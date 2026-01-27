package app.lusk.underseerr.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.local.dao.OfflineRequestDao
import app.lusk.underseerr.data.remote.api.RequestKtorService
import app.lusk.underseerr.data.remote.model.ApiRequestBody
import app.lusk.underseerr.data.remote.model.toMediaRequest
import app.lusk.underseerr.data.mapper.toEntity
import io.ktor.client.plugins.ResponseException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OfflineRequestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val offlineRequestDao: OfflineRequestDao by inject()
    private val requestKtorService: RequestKtorService by inject()
    private val mediaRequestDao: MediaRequestDao by inject()

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) return Result.failure()

        val offlineRequests = offlineRequestDao.getAll()
        if (offlineRequests.isEmpty()) return Result.success()

        var allSuccess = true

        offlineRequests.forEach { request ->
            try {
                val response = if (request.mediaType == "movie") {
                    requestKtorService.submitRequest(
                        ApiRequestBody(
                            mediaId = request.mediaId, 
                            mediaType = "movie",
                            profileId = request.qualityProfile, 
                            rootFolder = request.rootFolder
                        )
                    )
                } else {
                    val seasons = request.seasons?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                    requestKtorService.submitRequest(
                        ApiRequestBody(
                            mediaId = request.mediaId, 
                            mediaType = "tv",
                            seasons = seasons,
                            profileId = request.qualityProfile, 
                            rootFolder = request.rootFolder
                        )
                    )
                }
                 
                // Success - Save true request to DB
                val mediaRequest = response.toMediaRequest()
                mediaRequestDao.insert(mediaRequest.toEntity())
                 
                // Remove offline request
                offlineRequestDao.delete(request)
                 
            } catch (e: Exception) {
                if (e is ResponseException && e.response.status.value in 400..499) {
                    // Fatal error (e.g. already requested, 403, etc), don't retry, just delete
                    offlineRequestDao.delete(request)
                } else {
                    // Start next cycle effectively
                    allSuccess = false
                }
            }
        }
        
        return if (allSuccess) Result.success() else Result.retry()
    }
}
