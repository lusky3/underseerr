package app.lusk.client.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lusk.client.domain.repository.RequestRepository
import kotlinx.coroutines.flow.first

/**
 * Background worker to poll request status updates.
 * Feature: overseerr-android-client
 * Validates: Requirements 4.5
 * Property 17: Request Status Updates
 */
class RequestStatusWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val requestRepository: RequestRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get all user requests
            val requests = requestRepository.getUserRequests().first()
            
            // Poll status for each pending/approved request
            requests
                .filter { it.status.name in listOf("PENDING", "APPROVED") }
                .forEach { request ->
                    try {
                        requestRepository.getRequestStatus(request.id)
                    } catch (e: Exception) {
                        // Continue with other requests even if one fails
                    }
                }
            
            Result.success()
        } catch (e: Exception) {
            // Retry on failure
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "request_status_polling"
        const val POLLING_INTERVAL_MINUTES = 15L
    }
}
