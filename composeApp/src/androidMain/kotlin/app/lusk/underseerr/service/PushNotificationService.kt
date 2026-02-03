package app.lusk.underseerr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.lusk.underseerr.domain.repository.NotificationRepository
import app.lusk.underseerr.util.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import app.lusk.underseerr.domain.security.WebPushKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

class PushNotificationService : FirebaseMessagingService() {

    private val notificationRepository: NotificationRepository by inject()
    private val settingsRepository: app.lusk.underseerr.domain.repository.SettingsRepository by inject()
    private val webPushKeyManager: WebPushKeyManager by inject()
    private val logger: AppLogger by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "PushNotificationService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        logger.i(TAG, "New FCM Token received: $token")
        scope.launch {
            try {
                notificationRepository.registerForPushNotifications(token)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        logger.d(TAG, "Message Data: ${message.data}")
        
        val type = message.data["type"]
        val payloadBase64 = message.data["payload"]
        val headersJson = message.data["headers"]
        
        if (type == "webpush_encrypted") {
            if (payloadBase64 != null) {
                try {
                    val encryptedPayload = android.util.Base64.decode(payloadBase64, android.util.Base64.DEFAULT)
                    logger.d(TAG, "Decoding Base64 payload success. Size: ${encryptedPayload.size}")
                    
                    // Decryption involves crypto, doing it in runBlocking since this is a service callback
                    val decryptedJson = runBlocking { 
                        val headers = try {
                            headersJson?.let { Json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
                        } catch (e: Exception) {
                            logger.e(TAG, "Failed to parse headers JSON: ${e.message}", e)
                            emptyMap()
                        }
                        
                        logger.d(TAG, "Attempting decryption with headers: $headers")
                        webPushKeyManager.decrypt(encryptedPayload, headers) 
                    }
                    logger.d(TAG, "Successfully decrypted Web Push payload: $decryptedJson")
                    
                    val json = Json { ignoreUnknownKeys = true }
                    val data = json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(decryptedJson)
                    
                    fun kotlinx.serialization.json.JsonElement?.toCleanString(): String? {
                        return if (this is kotlinx.serialization.json.JsonPrimitive) {
                            this.content
                        } else {
                            this?.toString()?.removeSurrounding("\"")
                        }
                    }

                    val title = data["subject"].toCleanString() ?: "Underseerr"
                    var body = data["message"].toCleanString() ?: "New notification"
                    val image = data["image"].toCleanString()
                    val deepLink = data["actionUrl"].toCleanString() ?: "underseerr://request"
                    
                    val typeStr = (data["notificationType"] ?: data["notification_type"]).toCleanString()
                    
                    if (body.contains("undefined")) {
                        val mediaType = data["mediaType"].toCleanString() ?: "media"
                        body = body.replace("undefined", mediaType)
                    }
                    
                    // Convert JsonObject to Map<String, String?> for easier handling
                    val dataMap = data.mapValues { it.value.toCleanString() }
                    
                    processIncomingNotification(title, body, image, deepLink, typeStr, dataMap)
                    return
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to decrypt Web Push notification: ${e.message}", e)
                }
            } else {
                logger.w(TAG, "Received webpush_encrypted but payload is null")
            }
        }

        // Fallback or Standard Plaintext Data
        val title = message.notification?.title ?: message.data["title"] ?: "Underseerr"
        val body = message.notification?.body ?: message.data["message"] ?: "Notification received"
        val imageUrl = message.notification?.imageUrl?.toString() ?: message.data["image"]
        val deepLink = message.data["url"]
        val notificationTypeString = message.data["type"]

        processIncomingNotification(title, body, imageUrl, deepLink, notificationTypeString, message.data)
    }



    private fun constructDeepLinkFromData(type: String?, title: String, body: String, extraData: String?): String? {
        // Since we don't have the raw data map here easily without refactoring, 
        // we'll rely on what we can parse or what passed-in arguments allow.
        // ideally, processIncomingNotification would take the raw data map.
        return null 
    }

    // Refactored to pass raw data map
    private fun processIncomingNotification(
        title: String, 
        body: String, 
        imageUrl: String?, 
        deepLink: String?, 
        notificationTypeString: String?,
        dataMap: Map<String, String?>
    ) {
        // 1. Get current local settings
        val settings = runBlocking { 
            settingsRepository.getNotificationSettings().first()
        }
        
        // 2. Client-Side Filtering
        if (!settings.enabled) {
            logger.d(TAG, "Dropping notification: Notifications disabled locally.")
            return
        }
        
        val lowerTitle = title.lowercase()
        val type = notificationTypeString?.lowercase()
        logger.d(TAG, "Filtering Check: type=$type, title=$title")
        
        val shouldShow = when {
            type == "media_approved" || lowerTitle.contains(" approved") -> settings.requestApproved
            type == "media_auto_approved" || lowerTitle.contains("auto-approved") -> settings.requestAutoApproved
            type == "media_available" || lowerTitle.contains("available") -> settings.requestAvailable
            type == "media_declined" || lowerTitle.contains("declined") -> settings.requestDeclined
            type == "media_pending" || lowerTitle.contains("requested") -> settings.requestPendingApproval
            type == "media_failed" || lowerTitle.contains("failed") -> settings.requestProcessingFailed
            type == "issue_created" || lowerTitle.contains("reported") -> settings.issueReported
            type == "issue_comment" || lowerTitle.contains("comment") -> settings.issueComment
            type == "issue_resolved" || lowerTitle.contains("resolved") -> settings.issueResolved
            type == "issue_reopened" || lowerTitle.contains("reopened") -> settings.issueReopened
            type == "media_auto_requested" -> settings.mediaAutoRequested
            else -> {
                logger.d(TAG, "No specific filter match for type '$type', defaulting to show.")
                true
            }
        }
        
        if (!shouldShow) {
            logger.d(TAG, "NOT SHOWING: Notification for '$type' is disabled in your app settings.")
            return
        }
        
        // 3. Construct Deep Link dynamically based on identifiers in data
        // Priority: Provided Action URL -> Constructed ID-based Link -> Default Request Link
        var finalDeepLink = deepLink
        
        if (finalDeepLink.isNullOrEmpty() || finalDeepLink == "underseerr://request" || finalDeepLink.startsWith("/")) {
             // Try to find IDs
             val tmdbId = dataMap["tmdbId"]?.takeIf { it.isNotEmpty() }
             val mediaId = dataMap["mediaId"]?.takeIf { it.isNotEmpty() }
             val requestId = dataMap["requestId"]?.takeIf { it.isNotEmpty() }
             val mediaType = dataMap["mediaType"]?.takeIf { it.isNotEmpty() } ?: "movie"
             
             finalDeepLink = when {
                 // Issues usually attach a media ID
                 type?.contains("issue") == true && !mediaId.isNullOrEmpty() -> "underseerr://media/$mediaType/$mediaId"
                 
                 // Availability/Approval usually relates to a specific request
                 (type == "media_pending" || type == "media_approved" || type == "media_declined") && !requestId.isNullOrEmpty() -> "underseerr://request/$requestId"
                 
                 // If we have a direct TMDB ID (common in availability), go to media
                 !tmdbId.isNullOrEmpty() -> "underseerr://media/$mediaType/$tmdbId"
                 
                 // Fallback: if we only have a generic mediaId (could be TMDB or internal), try media route
                 !mediaId.isNullOrEmpty() -> "underseerr://media/$mediaType/$mediaId"
                 
                 else -> "underseerr://request"
             }
        }

        logger.d(TAG, "SHOWING notification: $title with deepLink: $finalDeepLink")
        showNotification(title, body, imageUrl, finalDeepLink)
    }

    private fun showNotification(title: String, body: String, imageUrl: String?, deepLink: String?) {
        val channelId = "underseerr_updates"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Overseerr Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for media request updates and approvals"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an Intent to open the app
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            deepLink?.let { data = android.net.Uri.parse(it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Find the best icon
        val smallIconResId = resources.getIdentifier("app_icon_transparent", "drawable", packageName).let {
            if (it != 0) {
                logger.d(TAG, "Using custom transparent icon: $it")
                it
            } else {
                logger.w(TAG, "Custom icon not found, falling back to app icon: ${applicationInfo.icon}")
                applicationInfo.icon
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(smallIconResId)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Handle image if present (note: simplified, usually requires loading into a Bitmap via Glide/Coil)
        if (imageUrl != null) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
