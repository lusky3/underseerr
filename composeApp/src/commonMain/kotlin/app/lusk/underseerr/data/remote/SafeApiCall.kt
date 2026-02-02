package app.lusk.underseerr.data.remote

import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.Result
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Extension functions for safe API calls with error handling.
 * Feature: underseerr, Property 4: Error Handling with Retry
 * Validates: Requirements 1.7, 3.7
 */

/**
 * Execute a suspend API call safely and wrap result in Result type.
 * 
 * @param apiCall The suspend function to execute
 * @return Result wrapping the API response or error
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        val response = apiCall()
        Result.success(response)
    } catch (e: Exception) {
        Result.error(e.toAppError())
    }
}

/**
 * Convert an exception to an AppError.
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is ResponseException -> {
            val code = response.status.value
            when (code) {
                401 -> AppError.AuthError(
                    message = "Authentication required",
                    cause = this
                )
                403 -> AppError.PermissionError(
                    message = "Access denied",
                    cause = this
                )
                404 -> AppError.NotFoundError(
                    message = "Resource not found",
                    cause = this
                )
                in 400..499 -> AppError.HttpError(
                    code = code,
                    message = "Client error: $code",
                    cause = this
                )
                in 500..599 -> AppError.ServerError(
                    message = "Server error: $code",
                    cause = this
                )
                else -> AppError.HttpError(
                    code = code,
                    message = "HTTP error: $code",
                    cause = this
                )
            }
        }
        is HttpRequestTimeoutException -> AppError.TimeoutError(
            message = "Request timed out. Please try again.",
            cause = this
        )
        is TimeoutCancellationException -> AppError.TimeoutError(
            message = "Request timed out. Please try again.",
            cause = this
        )
        is kotlinx.serialization.SerializationException -> AppError.ParseError(
            message = "Failed to parse server response.",
            cause = this
        )
        is io.ktor.client.call.NoTransformationFoundException -> AppError.ParseError(
            message = "Server returned an unexpected format. Please check the URL and ensure it points to an Overseerr instance.",
            cause = this
        )
        else -> AppError.UnknownError(
            message = message ?: "An unexpected error occurred",
            cause = this
        )
    }
}

/**
 * Execute an API call with retry logic.
 * 
 * @param maxRetries Maximum number of retry attempts
 * @param initialDelay Initial delay before first retry in milliseconds
 * @param maxDelay Maximum delay between retries in milliseconds
 * @param factor Multiplier for exponential backoff
 * @param apiCall The suspend function to execute
 * @return Result wrapping the API response or error
 */
suspend fun <T> safeApiCallWithRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 1000L,
    maxDelay: Long = 10000L,
    factor: Double = 2.0,
    apiCall: suspend () -> T
): Result<T> {
    var currentDelay = initialDelay
    var lastError: AppError? = null
    
    repeat(maxRetries) { attempt ->
        val result = safeApiCall(apiCall)
        
        when {
            result.isSuccess -> return result
            result.isError -> {
                val error = result.errorOrNull()!!
                lastError = error
                
                // If error is not retryable, return immediately
                if (!error.isRetryable()) {
                    return result
                }
                
                // If this is the last attempt, return the error
                if (attempt == maxRetries - 1) {
                    return result
                }
                
                // Wait before retrying
                kotlinx.coroutines.delay(currentDelay)
                
                // Increase delay for next attempt (exponential backoff)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
    
    // This should never be reached, but handle it just in case
    return Result.error(lastError ?: AppError.UnknownError("Request failed after $maxRetries retries"))
}

/**
 * Map a Result to another type, handling errors.
 */
inline fun <T, R> Result<T>.mapResult(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> try {
            Result.success(transform(data))
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
        is Result.Error -> Result.error(error)
        is Result.Loading -> Result.loading()
    }
}

/**
 * Flat map a Result to another Result, handling errors.
 */
inline fun <T, R> Result<T>.flatMapResult(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> try {
            transform(data)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
        is Result.Error -> Result.error(error)
        is Result.Loading -> Result.loading()
    }
}
