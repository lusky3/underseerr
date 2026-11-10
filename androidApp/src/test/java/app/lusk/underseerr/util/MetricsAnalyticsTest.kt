package app.lusk.underseerr.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private class RecordingBackend : AnalyticsBackend {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    val screens = mutableListOf<String>()
    var enabled: Boolean? = null

    override fun logEvent(name: String, params: Map<String, String>) {
        events += name to params
    }

    override fun logScreen(screenName: String) {
        screens += screenName
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

private const val SEARCH = "search"

class MetricsAnalyticsTest : StringSpec({
    val backend = RecordingBackend()

    beforeTest {
        backend.events.clear()
        backend.screens.clear()
        Metrics.backend = backend
        Metrics.setEnabled(true)
    }

    afterSpec {
        Metrics.setEnabled(true)
    }

    "events reach the backend with only their category parameter" {
        Metrics.trackLogin("plex")
        Metrics.trackMediaRequest("movie")
        Metrics.trackMediaRequestFailed("tv")
        Metrics.trackLoginFailed("local")

        backend.events shouldBe listOf(
            "login" to mapOf("method" to "plex"),
            "media_request" to mapOf("media_type" to "movie"),
            "media_request_failed" to mapOf("media_type" to "tv"),
            "login_failed" to mapOf("method" to "local")
        )
    }

    "screens reach the backend" {
        Metrics.trackScreen(SEARCH)

        backend.screens shouldBe listOf(SEARCH)
    }

    "nothing is sent while analytics is disabled" {
        Metrics.setEnabled(false)

        Metrics.trackLogin("plex")
        Metrics.trackSearch()
        Metrics.trackScreen("home")

        backend.events.shouldBeEmpty()
        backend.screens.shouldBeEmpty()
    }

    "disabling tells the backend to stop collecting, and re-enabling resumes" {
        Metrics.setEnabled(false)
        backend.enabled shouldBe false

        Metrics.setEnabled(true)
        backend.enabled shouldBe true
        Metrics.trackSearch()
        backend.events shouldBe listOf(SEARCH to emptyMap<String, String>())
    }

})
