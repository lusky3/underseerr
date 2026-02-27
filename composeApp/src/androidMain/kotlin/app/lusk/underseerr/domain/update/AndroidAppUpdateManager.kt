package app.lusk.underseerr.domain.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [AppUpdateManager] using Google Play In-App Updates API.
 * 
 * Checks for available updates on the Play Store and can trigger the update flow.
 * Uses FLEXIBLE update type so the user isn't forced to update immediately.
 */
class AndroidAppUpdateManager(
    private val activityProvider: () -> Activity?
) : AppUpdateManager {

    companion object {
        private const val TAG = "AppUpdate"
        private const val UPDATE_REQUEST_CODE = 1001
    }

    override suspend fun isUpdateAvailable(): Boolean {
        val activity = activityProvider() ?: run {
            Log.w(TAG, "isUpdateAvailable: No activity available")
            return false
        }

        return try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)
            
            suspendCancellableCoroutine<Boolean> { continuation ->
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo
                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    val available = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    Log.d(TAG, "Update check: available=$available, availability=${appUpdateInfo.updateAvailability()}")
                    continuation.resume(available)
                }
                appUpdateInfoTask.addOnFailureListener { e ->
                    Log.e(TAG, "Update check failed", e)
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "isUpdateAvailable failed", e)
            false
        }
    }

    override suspend fun startUpdate() {
        val activity = activityProvider() ?: run {
            Log.w(TAG, "startUpdate: No activity available")
            return
        }

        try {
            val appUpdateManager = AppUpdateManagerFactory.create(activity)

            val appUpdateInfo = suspendCancellableCoroutine<AppUpdateInfo?> { continuation ->
                appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                    continuation.resume(info)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } ?: return

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                Log.d(TAG, "Starting flexible update flow")
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    UPDATE_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "Update not available or FLEXIBLE type not allowed, opening Play Store")
                // Fallback: open Play Store listing
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=${activity.packageName}")
                )
                activity.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startUpdate failed", e)
        }
    }
}
