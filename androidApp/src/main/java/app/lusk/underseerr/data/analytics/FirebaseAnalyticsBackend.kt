package app.lusk.underseerr.data.analytics

import android.content.Context
import android.os.Bundle
import app.lusk.underseerr.util.AnalyticsBackend
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Sends [app.lusk.underseerr.util.Metrics] events to Firebase Analytics.
 *
 * [setEnabled] maps to setAnalyticsCollectionEnabled, which Firebase persists across
 * launches, so an opt-out holds even before the app has read its own preferences.
 */
class FirebaseAnalyticsBackend(context: Context) : AnalyticsBackend {

    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

    override fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
        analytics.logEvent(name, bundle)
    }

    override fun logScreen(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, SCREEN_CLASS)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun setEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    private companion object {
        // The app is a single Compose activity; real destinations go in screen_name.
        const val SCREEN_CLASS = "MainActivity"
    }
}
