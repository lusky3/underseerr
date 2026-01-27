package app.lusk.underseerr.data.notification

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.lusk.underseerr.MainActivity

/**
 * Handler for notification-related operations.
 * Feature: underseerr
 * Validates: Requirements 6.4
 * Property 25: Notification Deep Link Navigation
 */
class NotificationHandler(
    private val context: Context
) {
    
    /**
     * Parse and handle deep link from notification.
     * Property 25: Notification Deep Link Navigation
     */
    fun handleDeepLink(deepLink: String): Intent {
        val uri = deepLink.toUri()
        return Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
    
    /**
     * Create deep link for media details.
     */
    fun createMediaDeepLink(mediaId: Int, mediaType: String): String {
        return "lusk://media/$mediaType/$mediaId"
    }
    
    /**
     * Create deep link for request details.
     */
    fun createRequestDeepLink(requestId: Int): String {
        return "lusk://request/$requestId"
    }
    
    /**
     * Parse deep link to extract navigation information.
     */
    fun parseDeepLink(deepLink: String): DeepLinkInfo? {
        val uri = deepLink.toUri()
        
        return when (uri.host) {
            "media" -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.size >= 2) {
                    DeepLinkInfo.MediaDetails(
                        mediaType = pathSegments[0],
                        mediaId = pathSegments[1].toIntOrNull() ?: return null
                    )
                } else null
            }
            "request" -> {
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) {
                    DeepLinkInfo.RequestDetails(
                        requestId = pathSegments[0].toIntOrNull() ?: return null
                    )
                } else null
            }
            else -> null
        }
    }
}

/**
 * Sealed class representing deep link information.
 */
sealed class DeepLinkInfo {
    data class MediaDetails(val mediaType: String, val mediaId: Int) : DeepLinkInfo()
    data class RequestDetails(val requestId: Int) : DeepLinkInfo()
}
