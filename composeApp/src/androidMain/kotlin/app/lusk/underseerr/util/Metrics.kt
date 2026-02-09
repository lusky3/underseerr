package app.lusk.underseerr.util

// Metrics tracking - currently disabled as Sentry metrics API is not available in this version
actual object Metrics {
    actual fun trackServerSetup() {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackLogin(method: String) {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackMediaRequest(mediaType: String) {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackSearch() {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackIssueReported() {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackApiResponseTime(endpoint: String, durationMs: Long) {
        // TODO: Implement when Sentry metrics API is available
    }

    actual fun trackActiveRequests(count: Int) {
        // TODO: Implement when Sentry metrics API is available
    }
}
