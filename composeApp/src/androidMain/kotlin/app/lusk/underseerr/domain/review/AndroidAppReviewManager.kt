package app.lusk.underseerr.domain.review

import android.app.Activity
import android.util.Log
import android.widget.Toast
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.util.AppConfig
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [AppReviewManager] using Google Play In-App Review API.
 *
 * In debug builds, uses [FakeReviewManager] which simulates the review flow
 * without requiring a Play Store listing, and shows a Toast to confirm it triggered.
 *
 * In release builds, uses [ReviewManagerFactory] which connects to the real
 * Play Store. The Play API manages its own internal quota (~monthly), so even
 * if [launchReviewFlow] is called, the dialog may not always appear.
 *
 * Additional safeguards on top of Play's quota:
 * - Request count threshold (default 3)
 * - Minimum 30-day interval between prompts
 * - "Completed" flag to avoid re-prompting after successful review
 */
class AndroidAppReviewManager(
    private val preferencesManager: PreferencesManager,
    private val activityProvider: () -> Activity?
) : AppReviewManager {

    companion object {
        private const val TAG = "AppReview"
    }

    override suspend fun shouldPromptForReview(): Boolean {
        val requestCount = preferencesManager.getSuccessfulRequestCount().first()
        val hasCompleted = preferencesManager.getHasCompletedReview().first()
        val lastPromptTime = preferencesManager.getLastReviewPromptTime().first()

        Log.d(TAG, "shouldPromptForReview check: count=$requestCount, completed=$hasCompleted, lastPrompt=$lastPromptTime")

        if (hasCompleted) {
            Log.d(TAG, "→ Skipping: user already completed review")
            return false
        }
        if (requestCount < requestThreshold) {
            Log.d(TAG, "→ Skipping: count $requestCount < threshold $requestThreshold")
            return false
        }

        // In debug builds, skip the time-based throttle so we can test easily
        if (!AppConfig.isDebug) {
            val now = System.currentTimeMillis()
            if (lastPromptTime > 0 && (now - lastPromptTime) < minimumPromptIntervalMs) {
                Log.d(TAG, "→ Skipping: too soon since last prompt")
                return false
            }
        }

        // Only prompt at multiples of the threshold (3, 6, 9, ...)
        // so we don't spam on every single request after the threshold
        val isMultiple = requestCount % requestThreshold == 0
        Log.d(TAG, "→ Multiple of threshold? $isMultiple (count=$requestCount, threshold=$requestThreshold)")
        return isMultiple
    }

    override suspend fun launchReviewFlow(): Boolean {
        val activity = activityProvider() ?: run {
            Log.w(TAG, "launchReviewFlow: No activity available")
            return false
        }

        return try {
            // Record prompt time before launching
            preferencesManager.setLastReviewPromptTime(System.currentTimeMillis())

            // Use FakeReviewManager in debug builds since the real API
            // requires the app to be installed from the Play Store
            val reviewManager: ReviewManager = if (AppConfig.isDebug) {
                Log.d(TAG, "Using FakeReviewManager (debug build)")
                FakeReviewManager(activity)
            } else {
                Log.d(TAG, "Using real ReviewManager (release build)")
                ReviewManagerFactory.create(activity)
            }

            // Request the ReviewInfo object
            Log.d(TAG, "Requesting ReviewInfo...")
            val reviewInfo = suspendCancellableCoroutine<ReviewInfo?> { continuation ->
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "ReviewInfo obtained successfully")
                        continuation.resume(task.result)
                    } else {
                        Log.e(TAG, "Failed to get ReviewInfo", task.exception)
                        continuation.resume(null)
                    }
                }
            } ?: return false

            // Launch the review flow
            Log.d(TAG, "Launching review flow dialog...")
            val result = suspendCancellableCoroutine { continuation ->
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    Log.d(TAG, "Review flow completed")
                    continuation.resume(true)
                }
            }

            // In debug, show a Toast so the developer knows it triggered
            if (AppConfig.isDebug) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "✅ In-App Review flow triggered! (FakeReviewManager)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "launchReviewFlow failed", e)
            false
        }
    }

    override suspend fun recordSuccessfulRequest(): Int {
        val count = preferencesManager.incrementSuccessfulRequestCount()
        Log.d(TAG, "recordSuccessfulRequest: new count = $count")
        return count
    }

    override suspend fun markReviewCompleted() {
        preferencesManager.setHasCompletedReview(true)
        Log.d(TAG, "Review marked as completed — will not prompt again")
    }
}
