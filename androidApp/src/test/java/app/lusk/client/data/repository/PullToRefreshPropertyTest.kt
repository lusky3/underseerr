package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.local.dao.OfflineRequestDao
import app.lusk.client.data.local.entity.MediaRequestEntity
import app.lusk.client.data.remote.api.RequestKtorService
import app.lusk.client.data.remote.model.*
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.repository.DiscoveryRepository
import app.lusk.client.domain.sync.SyncScheduler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Property-based tests for pull-to-refresh data freshness.
 * Feature: overseerr-android-client, Property 18: Pull-to-Refresh Data Freshness
 * Validates: Requirements 4.6
 */
class PullToRefreshPropertyTest : StringSpec({
    
    val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()

    "Property 18.1: Pull-to-refresh fetches latest data from server" {
        val requestsArb = Arb.list(
            Arb.bind(Arb.int(1..1000), Arb.string(1..100), Arb.long(0..System.currentTimeMillis())) { id, title, timestamp ->
                Triple(id, title, timestamp)
            },
            1..20
        )
        checkAll(100, requestsArb) { requests ->
            runTest(testDispatcher) {
                val apiService = mockk<RequestKtorService>()
                val mediaDao = mockk<MediaRequestDao>(relaxed = true)
                val offlineDao = mockk<OfflineRequestDao>(relaxed = true)
                val discoveryRepo = mockk<DiscoveryRepository>(relaxed = true)
                val syncScheduler = mockk<SyncScheduler>(relaxed = true)
                
                val oldEntities = requests.map { (id, title, timestamp) ->
                    MediaRequestEntity(
                        id = id,
                        mediaType = "movie",
                        mediaId = id,
                        title = title,
                        posterPath = null,
                        status = 1,
                        requestedDate = timestamp,
                        seasons = null,
                        cachedAt = System.currentTimeMillis()
                    )
                }
                
                val newApiRequests = requests.map { (id, _, _) ->
                    ApiMediaRequest(
                        id = id,
                        type = "movie",
                        status = 2,
                        media = ApiRequestMedia(mediaType = "movie", tmdbId = id)
                    )
                }
                
                coEvery { mediaDao.getAllRequests() } returns flowOf(oldEntities)
                coEvery { apiService.getRequests(any(), any()) } returns RequestsResponse(
                    pageInfo = PageInfo(pages = 1, pageSize = 100, results = newApiRequests.size, page = 1),
                    results = newApiRequests
                )
                
                val repository = RequestRepositoryImpl(apiService, mediaDao, offlineDao, discoveryRepo, syncScheduler)
                repository.refreshRequests()
                
                coVerify { apiService.getRequests(any(), any()) }
                coVerify { mediaDao.insertAll(any()) }
            }
        }
    }
    
    "Property 18.2: Refreshed data replaces cached data" {
        val requestsArb = Arb.list(
            Arb.bind(Arb.int(1..1000), Arb.string(1..100)) { id, title -> id to title },
            1..10
        )
        checkAll(100, requestsArb) { requests ->
            runTest(testDispatcher) {
                val apiService = mockk<RequestKtorService>()
                val mediaDao = mockk<MediaRequestDao>(relaxed = true)
                val offlineDao = mockk<OfflineRequestDao>(relaxed = true)
                val discoveryRepo = mockk<DiscoveryRepository>(relaxed = true)
                val syncScheduler = mockk<SyncScheduler>(relaxed = true)
                
                val oldEntities = requests.map { (id, title) ->
                    MediaRequestEntity(
                        id = id,
                        mediaType = "movie",
                        mediaId = id,
                        title = title,
                        posterPath = null,
                        status = 1,
                        requestedDate = System.currentTimeMillis() - 86400000,
                        seasons = null,
                        cachedAt = System.currentTimeMillis()
                    )
                }
                
                val newApiRequests = requests.map { (id, _) ->
                    ApiMediaRequest(
                        id = id,
                        type = "movie",
                        status = 4,
                        media = ApiRequestMedia(mediaType = "movie", tmdbId = id)
                    )
                }
                
                val newEntities = newApiRequests.map { apiRequest ->
                    MediaRequestEntity(
                        id = apiRequest.id,
                        mediaType = "movie",
                        mediaId = apiRequest.media?.tmdbId ?: 0,
                        title = "Title",
                        posterPath = null,
                        status = apiRequest.status,
                        requestedDate = System.currentTimeMillis(),
                        seasons = null,
                        cachedAt = System.currentTimeMillis()
                    )
                }
                
                coEvery { mediaDao.getAllRequests() } returnsMany listOf(
                    flowOf(oldEntities),
                    flowOf(newEntities)
                )
                coEvery { apiService.getRequests(any(), any()) } returns RequestsResponse(
                    pageInfo = PageInfo(pages = 1, pageSize = 100, results = newApiRequests.size, page = 1),
                    results = newApiRequests
                )
                
                val repository = RequestRepositoryImpl(apiService, mediaDao, offlineDao, discoveryRepo, syncScheduler)
                val beforeRefresh = repository.getUserRequests().first()
                repository.refreshRequests()
                val afterRefresh = repository.getUserRequests().first()
                
                beforeRefresh.all { it.status.name == "PENDING" } shouldBe true
                afterRefresh.all { it.status.name == "AVAILABLE" } shouldBe true
            }
        }
    }
    
    "Property 18.3: Refresh handles network errors gracefully" {
        checkAll<Int>(100) { seed ->
            runTest(testDispatcher) {
                val apiService = mockk<RequestKtorService>()
                val mediaDao = mockk<MediaRequestDao>(relaxed = true)
                val offlineDao = mockk<OfflineRequestDao>(relaxed = true)
                val discoveryRepo = mockk<DiscoveryRepository>(relaxed = true)
                val syncScheduler = mockk<SyncScheduler>(relaxed = true)
                
                coEvery { apiService.getRequests(any(), any()) } throws Exception("Network unavailable")
                
                val repository = RequestRepositoryImpl(apiService, mediaDao, offlineDao, discoveryRepo, syncScheduler)
                val result = repository.refreshRequests()
                
                result.shouldBeInstanceOf<Result.Error>()
                coVerify(exactly = 0) { mediaDao.insertAll(any()) }
            }
        }
    }
    
    "Property 18.4: Refresh preserves request IDs" {
        val idsArb = Arb.list(Arb.int(1..10000), 1..15)
        checkAll(100, idsArb) { requestIds ->
            runTest(testDispatcher) {
                val apiService = mockk<RequestKtorService>()
                val mediaDao = mockk<MediaRequestDao>(relaxed = true)
                val offlineDao = mockk<OfflineRequestDao>(relaxed = true)
                val discoveryRepo = mockk<DiscoveryRepository>(relaxed = true)
                val syncScheduler = mockk<SyncScheduler>(relaxed = true)
                
                val apiRequests = requestIds.map { id ->
                    ApiMediaRequest(
                        id = id,
                        type = "movie",
                        status = 1,
                        media = ApiRequestMedia(mediaType = "movie", tmdbId = id)
                    )
                }
                
                val entities = apiRequests.map { apiRequest ->
                    MediaRequestEntity(
                        id = apiRequest.id,
                        mediaType = "movie",
                        mediaId = apiRequest.media?.tmdbId ?: 0,
                        title = "Movie ${apiRequest.id}",
                        posterPath = null,
                        status = apiRequest.status,
                        requestedDate = System.currentTimeMillis(),
                        seasons = null,
                        cachedAt = System.currentTimeMillis()
                    )
                }
                
                coEvery { mediaDao.getAllRequests() } returns flowOf(entities)
                coEvery { apiService.getRequests(any(), any()) } returns RequestsResponse(
                    pageInfo = PageInfo(pages = 1, pageSize = 100, results = apiRequests.size, page = 1),
                    results = apiRequests
                )
                
                val repository = RequestRepositoryImpl(apiService, mediaDao, offlineDao, discoveryRepo, syncScheduler)
                repository.refreshRequests()
                val result = repository.getUserRequests().first()
                
                val resultIds = result.map { it.id }
                resultIds shouldContainAll requestIds
            }
        }
    }
})
