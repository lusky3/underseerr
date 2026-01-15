package app.lusk.client.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules background workers for the app.
 * Feature: overseerr-android-client
 * Validates: Requirements 4.5
 */
class WorkerScheduler(
    private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic request status polling.
     */
    fun scheduleRequestStatusPolling() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RequestStatusWorker>(
            RequestStatusWorker.POLLING_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            RequestStatusWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancel request status polling.
     */
    fun cancelRequestStatusPolling() {
        workManager.cancelUniqueWork(RequestStatusWorker.WORK_NAME)
    }
}
