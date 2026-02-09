package app.lusk.underseerr.util

actual object Metrics {
    actual fun trackServerSetup() {}
    actual fun trackLogin(method: String) {}
    actual fun trackMediaRequest(mediaType: String) {}
    actual fun trackSearch() {}
    actual fun trackIssueReported() {}
    actual fun trackApiResponseTime(endpoint: String, durationMs: Long) {}
    actual fun trackActiveRequests(count: Int) {}
}
