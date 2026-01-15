package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.data.remote.model.ApiMediaRequest
import app.lusk.client.domain.model.MediaRequest
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.model.RequestStatus
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Property-based tests for request submission completeness.
 * Feature: overseerr-android-client, Property 9: Request Submission Completeness
 * Validates: Requirements 3.1, 3.2
 */
class RequestSubmissionPropertyTest : StringSpec({
    
    "Property 9.1: Movie request includes media ID, type, and quality profile" {
        checkAll<Int, Int?>(100) { movieId, qualityProfile ->
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
                apiService.requestMovie(movieId, qualityProfile, any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestMovie(movieId, qualityProfile, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.mediaId shouldBe movieId
            request.mediaType shouldBe MediaType.MOVIE
            
            // Verify API was called with correct parameters
            coVerify { apiService.requestMovie(movieId, qualityProfile, null) }
        }
    }
    
    "Property 9.2: TV show request includes media ID, seasons, and quality profile" {
        checkAll<Int, List<Int>, Int?>(100, Arb.int(1..1000), Arb.list(Arb.int(1..10), 1..5), Arb.int().orNull()) { tvShowId, seasons, qualityProfile ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>(relaxed = true)
            val safeApiCall = SafeApiCall()
            
            val apiResponse = ApiMediaRequest(
                id = 1,
                mediaId = tvShowId,
                mediaType = "tv",
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                title = "Test Show",
                posterPath = null,
                seasons = seasons
            )
            
            coEvery { 
                apiService.requestTvShow(tvShowId, seasons, qualityProfile, any()) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.requestTvShow(tvShowId, seasons, qualityProfile, null)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<MediaRequest>>()
            val request = (result as Result.Success).data
            request.mediaId shouldBe tvShowId
            request.mediaType shouldBe MediaType.TV
            request.seasons shouldBe seasons
            
            // Verify API was called with correct parameters
            coVerify { apiService.requestTvShow(tvShowId, seasons, qualityProfile, null) }
        }
    }
    
    "Property 9.3: Request submission returns request ID from server" {
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
            request.id shouldBe requestId
        }
    }
    
    "Property 9.4: Request submission caches request locally" {
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
            repository.requestMovie(movieId, null, null)
            
            // Then - verify request was cached
            coVerify { dao.insert(any()) }
        }
    }
    
    "Property 9.5: Request with root folder includes folder in submission" {
        checkAll<Int, String>(100, Arb.int(1..1000), Arb.string(1..50)) { movieId, rootFolder ->
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
                apiService.requestMovie(movieId, any(), rootFolder) 
            } returns apiResponse
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            repository.requestMovie(movieId, null, rootFolder)
            
            // Then - verify root folder was passed to API
            coVerify { apiService.requestMovie(movieId, any(), rootFolder) }
        }
    }
})
