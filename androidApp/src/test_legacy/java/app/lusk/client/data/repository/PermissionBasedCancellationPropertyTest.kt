package app.lusk.client.data.repository

import app.lusk.client.data.local.dao.MediaRequestDao
import app.lusk.client.data.remote.SafeApiCall
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.data.remote.model.ApiRequestResponse
import app.lusk.client.domain.model.AppError
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * Property-based tests for permission-based cancellation.
 * Feature: overseerr-android-client, Property 16: Permission-Based Cancellation
 * Validates: Requirements 4.4
 */
class PermissionBasedCancellationPropertyTest : StringSpec({
    
    "Property 16.1: Users with permission can cancel their own requests" {
        checkAll<Int>(100, Arb.int(1..10000)) { requestId ->
            // Given - user has permission (successful API call)
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val successResponse = ApiRequestResponse(
                id = requestId,
                status = 5, // Cancelled status
                media = mockk()
            )
            
            coEvery { apiService.deleteRequest(requestId) } returns successResponse
            coEvery { dao.deleteRequest(requestId) } returns Unit
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.cancelRequest(requestId)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<Unit>>()
            coVerify { apiService.deleteRequest(requestId) }
            coVerify { dao.deleteRequest(requestId) }
        }
    }
    
    "Property 16.2: Users without permission cannot cancel requests" {
        checkAll<Int>(100, Arb.int(1..10000)) { requestId ->
            // Given - user lacks permission (403 Forbidden)
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val errorBody = """{"message":"Insufficient permissions"}"""
                .toResponseBody("application/json".toMediaType())
            
            coEvery { apiService.deleteRequest(requestId) } throws HttpException(
                Response.error<ApiRequestResponse>(403, errorBody)
            )
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.cancelRequest(requestId)
            
            // Then
            result.shouldBeInstanceOf<Result.Error>()
            val error = (result as Result.Error).error
            error.shouldBeInstanceOf<AppError.ServerError>()
            (error as AppError.ServerError).code shouldBe 403
            
            // Database should not be updated
            coVerify(exactly = 0) { dao.deleteRequest(any()) }
        }
    }
    
    "Property 16.3: Cancellation fails gracefully on network error" {
        checkAll<Int>(100, Arb.int(1..10000)) { requestId ->
            // Given - network error
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            coEvery { apiService.deleteRequest(requestId) } throws 
                java.net.UnknownHostException("Network unavailable")
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.cancelRequest(requestId)
            
            // Then
            result.shouldBeInstanceOf<Result.Error>()
            val error = (result as Result.Error).error
            error.shouldBeInstanceOf<AppError.NetworkError>()
            
            // Database should not be updated
            coVerify(exactly = 0) { dao.deleteRequest(any()) }
        }
    }
    
    "Property 16.4: Successful cancellation removes request from local database" {
        checkAll<Int>(100, Arb.int(1..10000)) { requestId ->
            // Given
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val successResponse = ApiRequestResponse(
                id = requestId,
                status = 5,
                media = mockk()
            )
            
            coEvery { apiService.deleteRequest(requestId) } returns successResponse
            coEvery { dao.deleteRequest(requestId) } returns Unit
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.cancelRequest(requestId)
            
            // Then
            result.shouldBeInstanceOf<Result.Success<Unit>>()
            coVerify(exactly = 1) { dao.deleteRequest(requestId) }
        }
    }
    
    "Property 16.5: Cancellation of non-existent request returns error" {
        checkAll<Int>(100, Arb.int(1..10000)) { requestId ->
            // Given - request doesn't exist (404 Not Found)
            val apiService = mockk<RequestApiService>()
            val dao = mockk<MediaRequestDao>()
            val safeApiCall = SafeApiCall()
            
            val errorBody = """{"message":"Request not found"}"""
                .toResponseBody("application/json".toMediaType())
            
            coEvery { apiService.deleteRequest(requestId) } throws HttpException(
                Response.error<ApiRequestResponse>(404, errorBody)
            )
            
            val repository = RequestRepositoryImpl(apiService, dao, safeApiCall)
            
            // When
            val result = repository.cancelRequest(requestId)
            
            // Then
            result.shouldBeInstanceOf<Result.Error>()
            val error = (result as Result.Error).error
            error.shouldBeInstanceOf<AppError.ServerError>()
            (error as AppError.ServerError).code shouldBe 404
        }
    }
})
