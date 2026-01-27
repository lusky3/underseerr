package app.lusk.underseerr.util

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Performance monitoring utility.
 * Feature: underseerr
 * Validates: Requirements 10.1, 10.6, 10.7
 */
class PerformanceMonitor {

    private val _metrics = MutableStateFlow<Map<String, PerformanceMetric>>(emptyMap())
    val metrics: StateFlow<Map<String, PerformanceMetric>> = _metrics.asStateFlow()

    /**
     * Start timing an operation.
     */
    fun startTiming(operationName: String): Long {
        return SystemClock.elapsedRealtime()
    }

    /**
     * End timing an operation and record the metric.
     */
    fun endTiming(operationName: String, startTime: Long) {
        val duration = SystemClock.elapsedRealtime() - startTime
        recordMetric(operationName, duration)
    }

    /**
     * Record a performance metric.
     */
    private fun recordMetric(operationName: String, durationMs: Long) {
        val currentMetrics = _metrics.value.toMutableMap()
        val existingMetric = currentMetrics[operationName]

        val newMetric = if (existingMetric != null) {
            existingMetric.copy(
                count = existingMetric.count + 1,
                totalDuration = existingMetric.totalDuration + durationMs,
                averageDuration = (existingMetric.totalDuration + durationMs) / (existingMetric.count + 1),
                minDuration = minOf(existingMetric.minDuration, durationMs),
                maxDuration = maxOf(existingMetric.maxDuration, durationMs)
            )
        } else {
            PerformanceMetric(
                operationName = operationName,
                count = 1,
                totalDuration = durationMs,
                averageDuration = durationMs,
                minDuration = durationMs,
                maxDuration = durationMs
            )
        }

        currentMetrics[operationName] = newMetric
        _metrics.value = currentMetrics
    }

    /**
     * Get metric for a specific operation.
     */
    fun getMetric(operationName: String): PerformanceMetric? {
        return _metrics.value[operationName]
    }

    /**
     * Clear all metrics.
     */
    fun clearMetrics() {
        _metrics.value = emptyMap()
    }

    /**
     * Get all operations that exceed a threshold.
     */
    fun getSlowOperations(thresholdMs: Long): List<PerformanceMetric> {
        return _metrics.value.values.filter { it.averageDuration > thresholdMs }
    }
}

/**
 * Performance metric data class.
 */
data class PerformanceMetric(
    val operationName: String,
    val count: Int,
    val totalDuration: Long,
    val averageDuration: Long,
    val minDuration: Long,
    val maxDuration: Long
)

/**
 * Inline function to measure execution time.
 */
inline fun <T> PerformanceMonitor.measure(operationName: String, block: () -> T): T {
    val startTime = startTiming(operationName)
    return try {
        block()
    } finally {
        endTiming(operationName, startTime)
    }
}
