package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.data.remote.model.ApiMediaRequest
import app.lusk.client.domain.model.MediaRequest
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Property-based tests for request confirmation display.
 * Feature: overseerr-android-client, Property 10: Request Confirmation Display
 * Validates: Requirements 3.3
 */
class RequestConfirmationPropertyTest : StringSpec({
    
    "Property 10.1: Successful request returns unique request ID" {
        checkAll<Int, Int>(100) { movieId, requestId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            val apiResponse = ApiMediaRequest(
                id = requestId,
                mediaId = movieId,
                mediaType = "movie",
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                title = "Test Movie",
                posterPath = null,
                seasons = null
            )
            
            coEvery { 
                apiService.requestMovie(any(), any(), any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestMovie(movieId, null, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.id shouldNotBe 0
            request.id shouldBe requestId
        }
    }
    
    "Property 10.2: Request ID is positive integer" {
        checkAll<Int>(100, Arb.int(1..Int.MAX_VALUE)) { requestId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            val apiResponse = ApiMediaRequest(
                id = requestId,
                mediaId = 1,
                mediaType = "movie",
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                title = "Test Movie",
                posterPath = null,
                seasons = null
            )
            
            coEvery { 
                apiService.requestMovie(any(), any(), any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestMovie(1, null, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.id shouldBe requestId
            (request.id > 0) shouldBe true
        }
    }
    
    "Property 10.3: Request confirmation includes media title" {
        checkAll<Int, String>(100, Arb.int(1..1000), Arb.string(1..100)) { movieId, title ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            val apiResponse = ApiMediaRequest(
                id = 1,
                mediaId = movieId,
                mediaType = "movie",
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                title = title,
                posterPath = null,
                seasons = null
            )
            
            coEvery { 
                apiService.requestMovie(any(), any(), any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestMovie(movieId, null, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.title shouldBe title
            request.title.isNotEmpty() shouldBe true
        }
    }
    
    "Property 10.4: Request confirmation includes request status" {
        checkAll<Int>(100) { movieId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            val apiResponse = ApiMediaRequest(
                id = 1,
                mediaId = movieId,
                mediaType = "movie",
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                title = "Test Movie",
                posterPath = null,
                seasons = null
            )
            
            coEvery { 
                apiService.requestMovie(any(), any(), any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestMovie(movieId, null, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.status shouldNotBe null
        }
    }
    
    "Property 10.5: Each request has unique ID across multiple submissions" {
        checkAll<Int, Int>(100) { movieId1, movieId2 ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            var requestIdCounter = 1
            coEvery { 
                apiService.requestMovie(any(), any(), any()) 
            } answers {
                ApiMediaRequest(
                    id = requestIdCounter++,
                    mediaId = firstArg(),
                    mediaType = "movie",
                    status = "pending",
                    requestedDate = System.currentTimeMillis(),
                    title = "Test Movie",
                    posterPath = null,
                    seasons = null
                )
            }
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result1 = repository.requestMovie(movieId1, null, null)
            val result2 = repository.requestMovie(movieId2, null, null)
            
            // Then
            result1.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            result2.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            
            val request1 = (result1 as Result.Success).data
            val request2 = (result2 as Result.Success).data
            
            request1.id shouldNotBe request2.id
        }
    }
})
