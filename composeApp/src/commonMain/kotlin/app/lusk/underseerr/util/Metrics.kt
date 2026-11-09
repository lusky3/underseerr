package app.lusk.underseerr.util

/**
 * Where analytics events end up. Implemented per platform (Firebase Analytics on
 * Android); the default does nothing so tests, previews and builds without a
 * backend never send anything.
 */
interface AnalyticsBackend {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun logScreen(screenName: String)
    fun setEnabled(enabled: Boolean)
}

private object NoOpAnalyticsBackend : AnalyticsBackend {
    override fun logEvent(name: String, params: Map<String, String>) = Unit
    override fun logScreen(screenName: String) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

/**
 * Anonymous usage analytics. Events carry only a fixed name and, at most, a short
 * category such as a login method or media type. They never carry user IDs, server
 * URLs, search text or error messages.
 *
 * Nothing is sent while [isEnabled] is false, regardless of the backend.
 */
object Metrics {
    /** Set once at startup by the platform entry point. */
    var backend: AnalyticsBackend = NoOpAnalyticsBackend

    var isEnabled: Boolean = true
        private set

    /** Applies the user's choice, and tells the backend so it stops collecting. */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        backend.setEnabled(enabled)
    }

    private fun log(name: String, params: Map<String, String> = emptyMap()) {
        if (isEnabled) backend.logEvent(name, params)
    }

    fun trackScreen(screenName: String) {
        if (isEnabled) backend.logScreen(screenName)
    }

    fun trackServerSetup() = log("server_setup")
    fun trackLogin(method: String) = log("login", mapOf("method" to method))
    fun trackLoginFailed(method: String) = log("login_failed", mapOf("method" to method))
    fun trackMediaRequest(mediaType: String) = log("media_request", mapOf("media_type" to mediaType))
    fun trackMediaRequestFailed(mediaType: String) =
        log("media_request_failed", mapOf("media_type" to mediaType))
    fun trackSearch() = log("search")
    fun trackIssueReported() = log("issue_reported")

    // Not sent: per-call timing is high volume and belongs in Sentry performance traces.
    @Suppress("UNUSED_PARAMETER")
    fun trackApiResponseTime(endpoint: String, durationMs: Long) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun trackActiveRequests(count: Int) = Unit
}
