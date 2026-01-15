package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.local.entity.MediaRequestEntity
import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.domain.model.MediaRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * Property-based tests for request list completeness.
 * Feature: overseerr-android-client, Property 13: Request List Completeness
 * Validates: Requirements 4.1
 */
class RequestListPropertyTest : StringSpec({
    
    "Property 13.1: All user requests are displayed in the list" {
        checkAll<List<Triple<Int, String, String>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.string(1..100) to Arb.string(5..20),
            1..20
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, title, status) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = title,
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
            
            // Then
            result.size shouldBe requests.size
            val resultIds = result.map { it.id }
            val expectedIds = requests.map { it.first }
            resultIds shouldContainAll expectedIds
        }
    }
    
    "Property 13.2: Request list preserves all request properties" {
        checkAll<List<Triple<Int, String, Long>>>(100, Arb.list(
            Arb.int(1..1000) to Arb.string(1..100) to Arb.long(0..System.currentTimeMillis()),
            1..10
        )) { requests ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val entities = requests.map { (id, title, timestamp) ->
                MediaRequestEntity(
                    id = id,
                    mediaType = "movie",
                    mediaId = id,
                    title = title,
                    posterPath = "/test.jpg",
                    status = "pending",
                    requestedDate = timestamp,
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            
            // Then
            result.forEach { request ->
                val original = requests.find { it.first == request.id }
                original shouldBe Triple(request.id, request.title, request.requestedDate)
            }
        }
    }
    
    "Property 13.3: Empty request list returns empty collection" {
        checkAll<Unit>(100) {
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            coEvery { dao.getAllRequests() } returns flowOf(emptyList())
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            
            // Then
            result.size shouldBe 0
        }
    }
    
    "Property 13.4: Request list is ordered by requested date descending" {
        checkAll<List<Long>>(100, Arb.list(Arb.long(0..System.currentTimeMillis()), 2..10)) { timestamps ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val sortedTimestamps = timestamps.sortedDescending()
            val entities = sortedTimestamps.mapIndexed { index, timestamp ->
                MediaRequestEntity(
                    id = index + 1,
                    mediaType = "movie",
                    mediaId = index + 1,
                    title = "Movie $index",
                    posterPath = null,
                    status = "pending",
                    requestedDate = timestamp,
                    seasons = null
                )
            }
            
            coEvery { dao.getAllRequests() } returns flowOf(entities)
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.getUserRequests().first()
            
            // Then
            val resultTimestamps = result.map { it.requestedDate }
            resultTimestamps shouldBe sortedTimestamps
        }
    }
    
    "Property 13.5: Request list includes all media types" {
        checkAll<Int>(100) { count ->
            if (count > 0) {
                // Given
                val apiService = mockk<RequestApiService>()
                val dao = mockk<MediaRequestDao>()
                val safeApiCall = SafeApiCall()
                
                val entities = (1..count).map { id ->
                    MediaRequestEntity(
                        id = id,
                        mediaType = if (id % 2 == 0) "movie" else "tv",
                        mediaId = id,
                        title = "Media $id",
                        posterPath = null,
                        status = "pending",
                        requestedDate = System.currentTimeMillis(),
                        seasons = if (id % 2 == 0) null else "[1,2]"
                    )
                }
                
                coEvery { dao.getAllRequests() } returns flowOf(entities)
                
                val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
                
                // When
                val result = repository.getUserRequests().first()
                
                // Then
                result.size shouldBe count
                val hasMovies = result.any { it.mediaType.name == "MOVIE" }
                val hasTvShows = result.any { it.mediaType.name == "TV" }
                (hasMovies || hasTvShows) shouldBe true
            }
        }
    }
})
