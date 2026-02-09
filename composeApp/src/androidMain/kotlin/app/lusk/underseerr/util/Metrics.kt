package app.lusk.underseerr.util

// Metrics tracking - currently disabled as Sentry metrics API is not available in this version
object Metrics {
    fun trackServerSetup() {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackLogin(method: String) {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackMediaRequest(mediaType: String) {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackSearch() {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackIssueReported() {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackApiResponseTime(endpoint: String, durationMs: Long) {
        // TODO: Implement when Sentry metrics API is available
    }

    fun trackActiveRequests(count: Int) {
        // TODO: Implement when Sentry metrics API is available
    }
}
