package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.local.entity.MediaRequestEntity
import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.domain.model.RequestStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for request grouping correctness.
 * Feature: overseerr-android-client, Property 14: Request Grouping Correctness
 * Validates: Requirements 4.2
 */
class RequestGroupingPropertyTest : StringSpec({
    
    "Property 14.1: Each request appears in exactly one status group" {
        checkAll<List<Pair<Int, String>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.enum<RequestStatus>().map { it.name.lowercase() },
            1..20
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, status) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = "Movie $id",
                    posterPath = null,
                    status = status,
                    requestedDate = System.currentTimeMillis(),
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            
            // Then - group by status
            val groupedByStatus = result.groupBy { it.status }
            
            // Each request should appear exactly once
            val allRequestsFromGroups = groupedByStatus.values.flatten()
            allRequestsFromGroups.size shouldBe result.size
            
            // No duplicates across groups
            val uniqueIds = allRequestsFromGroups.map { it.id }.toSet()
            uniqueIds.size shouldBe result.size
        }
    }
    
    "Property 14.2: Status groups contain only requests with matching status" {
        checkAll<List<Pair<Int, RequestStatus>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.enum<RequestStatus>(),
            1..20
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, status) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = "Movie $id",
                    posterPath = null,
                    status = status.name.lowercase(),
                    requestedDate = System.currentTimeMillis(),
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            val groupedByStatus = result.groupBy { it.status }
            
            // Then - each group should only contain requests with that status
            groupedByStatus.forEach { (status, requestsInGroup) ->
                requestsInGroup.forEach { request ->
                    request.status shouldBe status
                }
            }
        }
    }
    
    "Property 14.3: All status types are represented when present" {
        checkAll<Int>(100) { seed ->
            if (seed > 0) {
                // Given - create at least one request for each status
                val apiService = mockk<RequestApiService>()
                val dao = mockk<MediaRequestDao>()
                val safeApiCall = SafeApiCall()
                
                val allStatuses = RequestStatus.entries
                val entities = allStatuses.mapIndexed { index, status ->
                    MediaRequestEntity(
                        id = index + 1,
                        mediaType = "movie",
                        mediaId = index + 1,
                        title = "Movie ${index + 1}",
                        posterPath = null,
                        status = status.name.lowercase(),
                        requestedDate = System.currentTimeMillis(),
                        seasons = null
                    )
                }
                
                coEvery { dao.getAllRequests() } returns flowOf(entities)
                
                val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
                
                // When
                val result = repository.getUserRequests().first()
                val groupedByStatus = result.groupBy { it.status }
                
                // Then - all statuses should be present in groups
                val statusesInGroups = groupedByStatus.keys
                statusesInGroups shouldContainExactlyInAnyOrder allStatuses
            }
        }
    }
    
    "Property 14.4: Empty status groups are not created" {
        checkAll<List<Pair<Int, RequestStatus>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.enum<RequestStatus>(),
            1..10
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, status) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = "Movie $id",
                    posterPath = null,
                    status = status.name.lowercase(),
                    requestedDate = System.currentTimeMillis(),
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            val groupedByStatus = result.groupBy { it.status }
            
            // Then - no empty groups
            groupedByStatus.values.forEach { group ->
                group.isNotEmpty() shouldBe true
            }
        }
    }
    
    "Property 14.5: Grouping preserves request count" {
        checkAll<List<Pair<Int, RequestStatus>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.enum<RequestStatus>(),
            1..20
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, status) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = "Movie $id",
                    posterPath = null,
                    status = status.name.lowercase(),
                    requestedDate = System.currentTimeMillis(),
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            val groupedByStatus = result.groupBy { it.status }
            
            // Then - total count across all groups equals original count
            val totalInGroups = groupedByStatus.values.sumOf { it.size }
            totalInGroups shouldBe result.size
            totalInGroups shouldBe requests.size
        }
    }
})
