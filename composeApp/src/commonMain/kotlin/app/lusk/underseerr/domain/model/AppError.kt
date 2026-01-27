package app.lusk.underseerr.domain.model

/**
 * Sealed class representing different types of application errors.
 * Feature: underseerr, Property 4: Error Handling with Retry
 * Validates: Requirements 1.7, 3.7
 */
sealed class AppError {
    abstract val message: String
    abstract val cause: Throwable?
    
    /**
     * Network-related errors.
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * HTTP errors with status codes.
     */
    data class HttpError(
        val code: Int,
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Authentication/authorization errors.
     */
    data class AuthError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Data parsing/serialization errors.
     */
    data class ParseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Server-side errors.
     */
    data class ServerError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Client-side validation errors.
     */
    data class ValidationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Resource not found errors.
     */
    data class NotFoundError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Permission/access denied errors.
     */
    data class PermissionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Timeout errors.
     */
    data class TimeoutError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Unknown/unexpected errors.
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    /**
     * Get a user-friendly error message.
     */
    fun getUserMessage(): String {
        return when (this) {
            is NetworkError -> "Network connection error. Please check your internet connection."
            is HttpError -> when (code) {
                400 -> "Invalid request. Please try again."
                401 -> "Authentication required. Please log in again."
                403 -> "Access denied. You don't have permission to perform this action."
                404 -> "The requested resource was not found."
                429 -> "Too many requests. Please try again later."
                500, 502, 503, 504 -> "Server error. Please try again later."
                else -> "An error occurred (HTTP $code). Please try again."
            }
            is AuthError -> "Authentication failed. Please log in again."
            is ParseError -> "Failed to process server response. Please try again."
            is ServerError -> "Server error. Please try again later."
            is ValidationError -> message
            is NotFoundError -> "The requested item was not found."
            is PermissionError -> "You don't have permission to perform this action."
            is TimeoutError -> "Request timed out. Please check your connection and try again."
            is UnknownError -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Check if this error is retryable.
     */
    fun isRetryable(): Boolean {
        return when (this) {
            is NetworkError -> true
            is HttpError -> code in listOf(408, 429, 500, 502, 503, 504)
            is TimeoutError -> true
            is ServerError -> true
            else -> false
        }
    }
}
