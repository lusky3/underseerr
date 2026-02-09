package app.lusk.underseerr.util

import io.sentry.Sentry

object Metrics {
    fun trackServerSetup() {
        try { Sentry.metrics().increment("server.setup", 1.0) } catch (_: Exception) {}
    }

    fun trackLogin(method: String) {
        try { Sentry.metrics().increment("auth.login.$method", 1.0) } catch (_: Exception) {}
    }

    fun trackMediaRequest(mediaType: String) {
        try { Sentry.metrics().increment("media.request.$mediaType", 1.0) } catch (_: Exception) {}
    }

    fun trackSearch() {
        try { Sentry.metrics().increment("search.performed", 1.0) } catch (_: Exception) {}
    }

    fun trackIssueReported() {
        try { Sentry.metrics().increment("issue.reported", 1.0) } catch (_: Exception) {}
    }

    fun trackApiResponseTime(endpoint: String, durationMs: Long) {
        try { Sentry.metrics().distribution("api.response_time", durationMs.toDouble()) } catch (_: Exception) {}
    }

    fun trackActiveRequests(count: Int) {
        try { Sentry.metrics().gauge("requests.active", count.toDouble()) } catch (_: Exception) {}
    }
}
