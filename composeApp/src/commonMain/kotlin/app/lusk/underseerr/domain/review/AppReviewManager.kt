package app.lusk.underseerr.domain.review

/**
 * Interface for managing in-app review prompts.
 * 
 * This provides a platform-agnostic way to prompt users for app
 * reviews after they've made a meaningful number of requests.
 * 
 * Android: Uses the Google Play In-App Review API.
 * iOS: Placeholder for SKStoreReviewController integration.
 */
interface AppReviewManager {

    /**
     * The number of successful requests required before prompting for review.
     * Default threshold is 3 requests.
     */
    val requestThreshold: Int
        get() = 3

    /**
     * Minimum time (in milliseconds) between review prompts.
     * Default is 30 days (Google Play enforces its own quota too).
     */
    val minimumPromptIntervalMs: Long
        get() = 30L * 24 * 60 * 60 * 1000 // 30 days

    /**
     * Checks whether conditions are met to prompt the user for a review.
     * 
     * Conditions:
     * - User has made at least [requestThreshold] successful requests
     * - User hasn't already completed a review
     * - Enough time has passed since the last prompt
     *
     * @return true if the app should prompt for a review
     */
    suspend fun shouldPromptForReview(): Boolean

    /**
     * Launches the in-app review flow.
     * 
     * On Android, this uses the Google Play In-App Review API.
     * The API handles quotas internally, so even if called, the dialog
     * may not be displayed if the user has been prompted recently.
     *
     * @return true if the review flow was launched successfully
     */
    suspend fun launchReviewFlow(): Boolean

    /**
     * Records that a successful request was made.
     * Increments the internal counter and returns the new count.
     *
     * @return the updated count of successful requests
     */
    suspend fun recordSuccessfulRequest(): Int

    /**
     * Marks the review as completed so the user isn't prompted again.
     */
    suspend fun markReviewCompleted()
}
