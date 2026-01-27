package app.lusk.underseerr.data.remote

import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Property-based tests for error handling with retry logic.
 * Feature: underseerr, Property 4: Error Handling with Retry
 * Validates: Requirements 1.7, 3.7
 * 
 * These tests verify that errors are properly handled and retried when appropriate.
 */
class ErrorHandlingPropertyTest : StringSpec({

    "Property: Successful API calls return Success result" {
        checkAll(100, Arb.string(1..50)) { data ->
            val result = safeApiCall {
                data
            }
            
            result.shouldBeInstanceOf<Result.Success<String>>()
            result.getOrNull() shouldBe data
        }
    }

    "Property: Failed API calls return Error result" {
        val result = safeApiCall<String> {
            throw IOException("Network error")
        }
        
        result.shouldBeInstanceOf<Result.Error>()
        result.errorOrNull() shouldNotBe null
    }

    "Property: HTTP 401 errors are mapped to AuthError" {
        val exception = HttpException(Response.error<Any>(401, okhttp3.ResponseBody.create(null, "")))
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.AuthError>()
    }

    "Property: HTTP 403 errors are mapped to PermissionError" {
        val exception = HttpException(Response.error<Any>(403, okhttp3.ResponseBody.create(null, "")))
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.PermissionError>()
    }

    "Property: HTTP 404 errors are mapped to NotFoundError" {
        val exception = HttpException(Response.error<Any>(404, okhttp3.ResponseBody.create(null, "")))
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.NotFoundError>()
    }

    "Property: HTTP 5xx errors are mapped to ServerError" {
        val serverErrorCodes = listOf(500, 502, 503, 504)
        
        serverErrorCodes.forEach { code ->
            val exception = HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))
            val error = exception.toAppError()
            
            error.shouldBeInstanceOf<AppError.ServerError>()
        }
    }

    "Property: UnknownHostException is mapped to NetworkError" {
        val exception = UnknownHostException("Unable to resolve host")
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.NetworkError>()
    }

    "Property: SocketTimeoutException is mapped to TimeoutError" {
        val exception = SocketTimeoutException("Connection timed out")
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.TimeoutError>()
    }

    "Property: IOException is mapped to NetworkError" {
        val exception = IOException("Network error")
        val error = exception.toAppError()
        
        error.shouldBeInstanceOf<AppError.NetworkError>()
    }

    "Property: Retryable errors are correctly identified" {
        val retryableErrors = listOf(
            AppError.NetworkError("Network error"),
            AppError.TimeoutError("Timeout"),
            AppError.ServerError("Server error"),
            AppError.HttpError(500, "Internal server error"),
            AppError.HttpError(502, "Bad gateway"),
            AppError.HttpError(503, "Service unavailable"),
            AppError.HttpError(504, "Gateway timeout")
        )
        
        retryableErrors.forEach { error ->
            error.isRetryable() shouldBe true
        }
    }

    "Property: Non-retryable errors are correctly identified" {
        val nonRetryableErrors = listOf(
            AppError.AuthError("Auth failed"),
            AppError.PermissionError("Access denied"),
            AppError.NotFoundError("Not found"),
            AppError.ValidationError("Invalid input"),
            AppError.HttpError(400, "Bad request"),
            AppError.HttpError(401, "Unauthorized"),
            AppError.HttpError(403, "Forbidden"),
            AppError.HttpError(404, "Not found")
        )
        
        nonRetryableErrors.forEach { error ->
            error.isRetryable() shouldBe false
        }
    }

    "Property: Result.map transforms successful results" {
        checkAll(100, Arb.int(1..100)) { value ->
            val result = Result.success(value)
            val mapped = result.map { it * 2 }
            
            mapped.shouldBeInstanceOf<Result.Success<Int>>()
            mapped.getOrNull() shouldBe value * 2
        }
    }

    "Property: Result.map preserves errors" {
        val error = AppError.NetworkError("Network error")
        val result = Result.error<Int>(error)
        val mapped = result.map { it * 2 }
        
        mapped.shouldBeInstanceOf<Result.Error>()
        mapped.errorOrNull() shouldBe error
    }

    "Property: Result.flatMap chains successful results" {
        checkAll(100, Arb.int(1..100)) { value ->
            val result = Result.success(value)
            val flatMapped = result.flatMap { Result.success(it * 2) }
            
            flatMapped.shouldBeInstanceOf<Result.Success<Int>>()
            flatMapped.getOrNull() shouldBe value * 2
        }
    }

    "Property: Result.flatMap propagates errors" {
        val error = AppError.NetworkError("Network error")
        val result = Result.error<Int>(error)
        val flatMapped = result.flatMap { Result.success(it * 2) }
        
        flatMapped.shouldBeInstanceOf<Result.Error>()
        flatMapped.errorOrNull() shouldBe error
    }

    "Property: Result.getOrDefault returns data on success" {
        checkAll(100, Arb.int(1..100)) { value ->
            val result = Result.success(value)
            result.getOrDefault(0) shouldBe value
        }
    }

    "Property: Result.getOrDefault returns default on error" {
        val result = Result.error<Int>(AppError.NetworkError("Error"))
        result.getOrDefault(42) shouldBe 42
    }

    "Property: User messages are non-empty and descriptive" {
        val errors = listOf(
            AppError.NetworkError("Network error"),
            AppError.HttpError(404, "Not found"),
            AppError.AuthError("Auth failed"),
            AppError.TimeoutError("Timeout"),
            AppError.ServerError("Server error"),
            AppError.ValidationError("Invalid input"),
            AppError.PermissionError("Access denied"),
            AppError.NotFoundError("Not found"),
            AppError.ParseError("Parse error"),
            AppError.UnknownError("Unknown error")
        )
        
        errors.forEach { error ->
            val userMessage = error.getUserMessage()
            userMessage.length shouldBe userMessage.length.coerceAtLeast(10)
            userMessage.isNotBlank() shouldBe true
        }
    }

    "Property: Result callbacks are executed correctly" {
        var successCalled = false
        var errorCalled = false
        
        val successResult = Result.success(42)
        successResult
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }
        
        successCalled shouldBe true
        errorCalled shouldBe false
        
        successCalled = false
        errorCalled = false
        
        val errorResult = Result.error<Int>(AppError.NetworkError("Error"))
        errorResult
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }
        
        successCalled shouldBe false
        errorCalled shouldBe true
    }
})
