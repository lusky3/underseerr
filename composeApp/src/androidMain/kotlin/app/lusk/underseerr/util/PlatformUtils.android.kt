package app.lusk.underseerr.util

import android.content.Intent
import android.net.Uri

/**
 * Opens a URL in the device's default browser on Android.
 */
actual fun openUrl(url: String) {
    val context = CurrentActivityHolder.get() ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Silently fail if no browser is available
    }
}

