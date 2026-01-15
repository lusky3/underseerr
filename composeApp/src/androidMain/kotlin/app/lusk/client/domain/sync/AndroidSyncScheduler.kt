package app.lusk.client.domain.sync

import android.content.Context
import androidx.work.*
import app.lusk.client.worker.OfflineRequestWorker

class AndroidSyncScheduler(private val context: Context) : SyncScheduler {
    override fun scheduleOfflineSync() {
        val workRequest = OneTimeWorkRequestBuilder<OfflineRequestWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_requests",
            ExistingWorkPolicy.APPEND,
            workRequest
        )
    }
}
