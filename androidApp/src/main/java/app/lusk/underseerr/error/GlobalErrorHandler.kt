package app.lusk.underseerr.error

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global error handler for uncaught exceptions and crash reporting.
 * Feature: underseerr
 * Validates: Requirements 10.5
 * Property 39: Crash Logging and Recovery
 */
class GlobalErrorHandler(
    private val context: Context
) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()
    
    private val errorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "GlobalErrorHandler"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_CRASH_LOGS = 10
    }
    
    /**
     * Initialize the global error handler.
     */
    fun initialize() {
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.d(TAG, "Global error handler initialized")
    }
    
    /**
     * Handle uncaught exceptions.
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Log the crash
            logCrash(thread, throwable)
            
            // Attempt graceful recovery
            attemptRecovery(throwable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in crash handler", e)
        } finally {
            // Call default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * Log crash details to file.
     */
    private fun logCrash(thread: Thread, throwable: Throwable) {
        errorScope.launch {
            try {
                val crashLog = buildCrashLog(thread, throwable)
                saveCrashLog(crashLog)
                Log.e(TAG, "Crash logged: ${throwable.message}", throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }
        }
    }
    
    /**
     * Build crash log with device info and stack trace.
     */
    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stackTrace = getStackTraceString(throwable)
        
        return buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine()
            appendLine("=== DEVICE INFO ===")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App Version: ${getAppVersion()}")
            appendLine()
            appendLine("=== THREAD INFO ===")
            appendLine("Thread: ${thread.name}")
            appendLine("Thread ID: ${thread.id}")
            appendLine()
            appendLine("=== EXCEPTION ===")
            appendLine("Type: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("=== STACK TRACE ===")
            appendLine(stackTrace)
            appendLine()
            appendLine("=== CAUSED BY ===")
            var cause = throwable.cause
            while (cause != null) {
                appendLine("Type: ${cause.javaClass.name}")
                appendLine("Message: ${cause.message}")
                appendLine(getStackTraceString(cause))
                appendLine()
                cause = cause.cause
            }
        }
    }
    
    /**
     * Get stack trace as string.
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
    
    /**
     * Save crash log to file.
     */
    private fun saveCrashLog(crashLog: String) {
        try {
            val crashDir = File(context.filesDir, CRASH_LOG_DIR)
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.log")
            
            crashFile.writeText(crashLog)
            
            // Clean up old crash logs
            cleanupOldCrashLogs(crashDir)
            
            Log.d(TAG, "Crash log saved: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }
    
    /**
     * Clean up old crash logs, keeping only the most recent ones.
     */
    private fun cleanupOldCrashLogs(crashDir: File) {
        try {
            val crashFiles = crashDir.listFiles()?.sortedByDescending { it.lastModified() }
            crashFiles?.drop(MAX_CRASH_LOGS)?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old crash logs", e)
        }
    }
    
    /**
     * Attempt graceful recovery from crash.
     */
    private fun attemptRecovery(throwable: Throwable) {
        try {
            when (throwable) {
                is OutOfMemoryError -> {
                    Log.w(TAG, "OutOfMemoryError detected, attempting memory cleanup")
                    System.gc()
                }
                is StackOverflowError -> {
                    Log.w(TAG, "StackOverflowError detected, cannot recover")
                }
                else -> {
                    Log.w(TAG, "Attempting graceful recovery from ${throwable.javaClass.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recovery attempt failed", e)
        }
    }
    
    /**
     * Get app version name.
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get all crash logs.
     */
    fun getCrashLogs(): List<File> {
        val crashDir = File(context.filesDir, CRASH_LOG_DIR)
        return crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Clear all crash logs.
     */
    fun clearCrashLogs() {
        val crashDir = File(context.filesDir, CRASH_LOG_DIR)
        crashDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "All crash logs cleared")
    }
}
