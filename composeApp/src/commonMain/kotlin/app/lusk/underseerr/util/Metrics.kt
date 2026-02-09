package app.lusk.underseerr.util

import io.sentry.Sentry

object Metrics {
    fun trackServerSetup() {
        Sentry.metrics().increment("server.setup")
    }

    fun trackLogin(method: String) {
        Sentry.metrics().increment("auth.login", tags = mapOf("method" to method))
    }

    fun trackMediaRequest(mediaType: String) {
        Sentry.metrics().increment("media.request", tags = mapOf("type" to mediaType))
    }

    fun trackSearch() {
        Sentry.metrics().increment("search.performed")
    }

    fun trackIssueReported() {
        Sentry.metrics().increment("issue.reported")
    }

    fun trackApiResponseTime(endpoint: String, durationMs: Long) {
        Sentry.metrics().distribution("api.response_time", durationMs.toDouble(), tags = mapOf("endpoint" to endpoint))
    }

    fun trackActiveRequests(count: Int) {
        Sentry.metrics().gauge("requests.active", count.toDouble())
    }
}
