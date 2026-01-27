package app.lusk.underseerr.util

import android.app.ActivityManager
import android.content.Context

/**
 * Memory optimization utility.
 * Feature: underseerr
 * Validates: Requirements 10.6, 10.7
 */
class MemoryOptimizer(
    private val context: Context
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * Get current memory usage information.
     */
    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory

        return MemoryInfo(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            availableMemory = availableMemory,
            percentUsed = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt(),
            isLowMemory = memoryInfo.lowMemory
        )
    }

    /**
     * Check if memory usage is high.
     */
    fun isMemoryHigh(): Boolean {
        val memoryInfo = getMemoryInfo()
        return memoryInfo.percentUsed > 80 || memoryInfo.isLowMemory
    }

    /**
     * Suggest garbage collection if memory is high.
     */
    fun suggestGarbageCollection() {
        if (isMemoryHigh()) {
            System.gc()
        }
    }

    /**
     * Get recommended image cache size based on available memory.
     */
    fun getRecommendedImageCacheSize(): Long {
        val memoryInfo = getMemoryInfo()
        // Use 25% of available memory for image cache
        return (memoryInfo.availableMemory * 0.25).toLong()
    }

    /**
     * Get recommended database cache size.
     */
    fun getRecommendedDatabaseCacheSize(): Long {
        // 100 MB for database cache
        return 100 * 1024 * 1024L
    }
}

/**
 * Memory information data class.
 */
data class MemoryInfo(
    val usedMemory: Long,
    val maxMemory: Long,
    val availableMemory: Long,
    val percentUsed: Int,
    val isLowMemory: Boolean
)
