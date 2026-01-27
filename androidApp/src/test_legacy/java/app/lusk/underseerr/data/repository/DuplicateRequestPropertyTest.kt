package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.local.entity.MediaRequestEntity
import app.lusk.underseerr.data.remote.SafeApiCall
import app.lusk.underseerr.data.remote.api.RequestApiService
import app.lusk.underseerr.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Property-based tests for duplicate request prevention.
 * Feature: underseerr, Property 11: Duplicate Request Prevention
 * Validates: Requirements 3.4, 3.5
 */
class DuplicateRequestPropertyTest : StringSpec({
    
    "Property 11.1: isMediaRequested returns true for already requested media" {
        checkAll<Int>(100) { mediaId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            // Media is already requested
            val existingRequest = MediaRequestEntity(
                id = 1,
                mediaType = "movie",
                mediaId = mediaId,
                title = "Test Movie",
                posterPath = null,
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                seasons = null
            )
            
            coEvery { dao.getRequestByMediaId(mediaId) } returns existingRequest
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.isMediaRequested(mediaId)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<Boolean>>()
            (result as Result.Success).data shouldBe true
        }
    }
    
    "Property 11.2: isMediaRequested returns false for non-requested media" {
        checkAll<Int>(100) { mediaId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            // Media is not requested
            coEvery { dao.getRequestByMediaId(mediaId) } returns null
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.isMediaRequested(mediaId)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<Boolean>>()
            (result as Result.Success).data shouldBe false
        }
    }
    
    "Property 11.3: Different media IDs have independent request status" {
        checkAll<Int, Int>(100) { mediaId1, mediaId2 ->
            if (mediaId1 != mediaId2) {
                // Given
                val apiService = mockk<RequestApiService>()
                val dao = mockk<MediaRequestDao>()
                val safeApiCall = SafeApiCall()
                
                // Only mediaId1 is requested
                val existingRequest = MediaRequestEntity(
                    id = 1,
                    mediaType = "movie",
                    mediaId = mediaId1,
                    title = "Test Movie",
                    posterPath = null,
                    status = "pending",
                    requestedDate = System.currentTimeMillis(),
                    seasons = null
                )
                
                coEvery { dao.getRequestByMediaId(mediaId1) } returns existingRequest
                coEvery { dao.getRequestByMediaId(mediaId2) } returns null
                
                val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
                
                // When
                val result1 = repository.isMediaRequested(mediaId1)
                val result2 = repository.isMediaRequested(mediaId2)
                
                // Then
                result1.shouldBeInstanceOf<Result.Success<Boolean>>()
                result2.shouldBeInstanceOf<Result.Success<Boolean>>()
                
                (result1 as Result.Success).data shouldBe true
                (result2 as Result.Success).data shouldBe false
            }
        }
    }
    
    "Property 11.4: Request status check is consistent across multiple calls" {
        checkAll<Int>(100) { mediaId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val existingRequest = MediaRequestEntity(
                id = 1,
                mediaType = "movie",
                mediaId = mediaId,
                title = "Test Movie",
                posterPath = null,
                status = "pending",
                requestedDate = System.currentTimeMillis(),
                seasons = null
            )
            
            coEvery { dao.getRequestByMediaId(mediaId) } returns existingRequest
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When - check multiple times
            val result1 = repository.isMediaRequested(mediaId)
            val result2 = repository.isMediaRequested(mediaId)
            val result3 = repository.isMediaRequested(mediaId)
            
            // Then - all should return same result
            result1.shouldBeInstanceOf<Result.Success<Boolean>>()
            result2.shouldBeInstanceOf<Result.Success<Boolean>>()
            result3.shouldBeInstanceOf<Result.Success<Boolean>>()
            
            val status1 = (result1 as Result.Success).data
            val status2 = (result2 as Result.Success).data
            val status3 = (result3 as Result.Success).data
            
            status1 shouldBe status2
            status2 shouldBe status3
        }
    }
    
    "Property 11.5: Checking non-existent media ID returns false" {
        checkAll<Int>(100, Arb.int(Int.MIN_VALUE..Int.MAX_VALUE)) { mediaId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            coEvery { dao.getRequestByMediaId(any()) } returns null
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.isMediaRequested(mediaId)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<Boolean>>()
            (result as Result.Success).data shouldBe false
        }
    }
})
