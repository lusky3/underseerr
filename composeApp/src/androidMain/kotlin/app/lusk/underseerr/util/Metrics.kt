package app.lusk.underseerr.util

import io.sentry.Sentry

object Metrics {
    fun trackServerSetup() {
        Sentry.metrics().counter("server.setup", 1.0)
    }

    fun trackLogin(method: String) {
        Sentry.metrics().counter("auth.login", 1.0, null, mapOf("method" to method))
    }

    fun trackMediaRequest(mediaType: String) {
        Sentry.metrics().counter("media.request", 1.0, null, mapOf("type" to mediaType))
    }

    fun trackSearch() {
        Sentry.metrics().counter("search.performed", 1.0)
    }

    fun trackIssueReported() {
        Sentry.metrics().counter("issue.reported", 1.0)
    }

    fun trackApiResponseTime(endpoint: String, durationMs: Long) {
        Sentry.metrics().distribution("api.response_time", durationMs.toDouble(), null, mapOf("endpoint" to endpoint))
    }

    fun trackActiveRequests(count: Int) {
        Sentry.metrics().gauge("requests.active", count.toDouble())
    }
}
