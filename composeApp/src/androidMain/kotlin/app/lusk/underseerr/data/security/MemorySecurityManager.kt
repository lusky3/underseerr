package app.lusk.underseerr.data.security

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages memory security by clearing sensitive data after inactivity timeout.
 * Feature: underseerr, Property 33: Memory Security Timeout
 * Validates: Requirements 8.5
 * 
 * Clears sensitive data from memory after 5 minutes of app being backgrounded.
 */
class MemorySecurityManager(
    private val context: android.content.Context
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TIMEOUT_MILLIS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timeoutJob: Job? = null
    private val sensitiveDataStore = ConcurrentHashMap<String, Any>()
    private val clearCallbacks = mutableListOf<() -> Unit>()
    
    private var isAppInBackground = false
    private var backgroundTimestamp = 0L
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    /**
     * Store sensitive data that should be cleared on timeout.
     * 
     * @param key Unique identifier for the data
     * @param data The sensitive data to store
     */
    fun storeSensitiveData(key: String, data: Any) {
        sensitiveDataStore[key] = data
    }
    
    /**
     * Retrieve sensitive data if still available.
     * 
     * @param key Unique identifier for the data
     * @return The data if available, null if cleared or not found
     */
    fun <T> getSensitiveData(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return sensitiveDataStore[key] as? T
    }
    
    /**
     * Remove specific sensitive data immediately.
     * 
     * @param key Unique identifier for the data to remove
     */
    fun removeSensitiveData(key: String) {
        sensitiveDataStore.remove(key)
    }
    
    /**
     * Register a callback to be invoked when sensitive data is cleared.
     * 
     * @param callback Function to call when data is cleared
     */
    fun registerClearCallback(callback: () -> Unit) {
        clearCallbacks.add(callback)
    }
    
    /**
     * Unregister a previously registered callback.
     * 
     * @param callback Function to unregister
     */
    fun unregisterClearCallback(callback: () -> Unit) {
        clearCallbacks.remove(callback)
    }
    
    /**
     * Clear all sensitive data immediately.
     */
    fun clearAllSensitiveData() {
        sensitiveDataStore.clear()
        
        // Invoke all registered callbacks
        clearCallbacks.forEach { callback ->
            try {
                callback()
            } catch (e: Exception) {
                // Log but don't crash if callback fails
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Check if app has been in background longer than timeout.
     * 
     * @return true if timeout has been exceeded, false otherwise
     */
    fun isTimeoutExceeded(): Boolean {
        if (!isAppInBackground) return false
        val elapsedTime = System.currentTimeMillis() - backgroundTimestamp
        return elapsedTime >= TIMEOUT_MILLIS
    }
    
    /**
     * Get remaining time before timeout in milliseconds.
     * 
     * @return Remaining time in milliseconds, 0 if not in background or timeout exceeded
     */
    fun getRemainingTimeoutMillis(): Long {
        if (!isAppInBackground) return 0L
        val elapsedTime = System.currentTimeMillis() - backgroundTimestamp
        val remaining = TIMEOUT_MILLIS - elapsedTime
        return if (remaining > 0) remaining else 0L
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App moved to background
        isAppInBackground = true
        backgroundTimestamp = System.currentTimeMillis()
        startTimeoutTimer()
    }
    
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App moved to foreground
        isAppInBackground = false
        cancelTimeoutTimer()
        
        // If timeout was exceeded while in background, clear data
        if (isTimeoutExceeded()) {
            clearAllSensitiveData()
        }
    }
    
    private fun startTimeoutTimer() {
        cancelTimeoutTimer()
        
        timeoutJob = scope.launch {
            delay(TIMEOUT_MILLIS)
            
            // After timeout, clear all sensitive data
            if (isAppInBackground) {
                clearAllSensitiveData()
            }
        }
    }
    
    private fun cancelTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = null
    }
    
    /**
     * Clean up resources when manager is no longer needed.
     */
    fun cleanup() {
        cancelTimeoutTimer()
        clearAllSensitiveData()
        clearCallbacks.clear()
        scope.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}
