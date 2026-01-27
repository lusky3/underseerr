package app.lusk.underseerr.domain.model

/**
 * A generic wrapper for operation results that can either succeed or fail.
 * Feature: underseerr, Property 4: Error Handling with Retry
 * Validates: Requirements 1.7, 3.7
 */
sealed class Result<out T> {
    /**
     * Successful result with data.
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Failed result with error.
     */
    data class Error(val error: AppError) : Result<Nothing>()
    
    /**
     * Loading state (optional, for UI).
     */
    data object Loading : Result<Nothing>()
    
    /**
     * Check if result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if result is an error.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Check if result is loading.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Get data if successful, null otherwise.
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }
    
    /**
     * Get error if failed, null otherwise.
     */
    fun errorOrNull(): AppError? {
        return when (this) {
            is Error -> error
            else -> null
        }
    }
    
    /**
     * Get data or throw exception.
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw Exception(error.message, error.cause)
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }
    
    /**
     * Get data or default value.
     */
    fun getOrDefault(default: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            else -> default
        }
    }
    
    /**
     * Map successful result to another type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(error)
            is Loading -> Loading
        }
    }
    
    /**
     * Flat map successful result to another Result.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> Error(error)
            is Loading -> Loading
        }
    }
    
    /**
     * Execute action if successful.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Execute action if failed.
     */
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }
    
    /**
     * Execute action if loading.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
    
    companion object {
        /**
         * Create a successful result.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Create an error result.
         */
        fun <T> error(error: AppError): Result<T> = Error(error)
        
        /**
         * Create a loading result.
         */
        fun <T> loading(): Result<T> = Loading
    }
}
