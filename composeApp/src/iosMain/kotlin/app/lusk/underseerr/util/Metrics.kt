package app.lusk.underseerr.util

object Metrics {
    fun trackServerSetup() {}
    fun trackLogin(method: String) {}
    fun trackMediaRequest(mediaType: String) {}
    fun trackSearch() {}
    fun trackIssueReported() {}
    fun trackApiResponseTime(endpoint: String, durationMs: Long) {}
    fun trackActiveRequests(count: Int) {}
}
