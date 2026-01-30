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
        
        logger.d(TAG, "onMessageReceived called. From: ${message.from}")
        
        val type = message.data["type"]
        if (type == "webpush_encrypted") {
            val payloadBase64 = message.data["payload"]
            if (payloadBase64 != null) {
                try {
                    val encryptedPayload = android.util.Base64.decode(payloadBase64, android.util.Base64.DEFAULT)
                    // Decryption involves crypto, doing it in runBlocking since this is a service callback
                    val decryptedBytes = runBlocking { webPushKeyManager.decrypt(encryptedPayload) }
                    val decryptedJson = String(decryptedBytes)
                    logger.d(TAG, "Successfully decrypted Web Push payload")
                    
                    val json = Json { ignoreUnknownKeys = true }
                    val data = json.decodeFromString<Map<String, String>>(decryptedJson)
                    
                    val title = data["subject"] ?: "Overseerr"
                    val body = data["message"] ?: ""
                    val image = data["image"]
                    val deepLink = "underseerr://request" // Default deep link for now
                    
                    processIncomingNotification(title, body, image, deepLink, data["notification_type"])
                    return
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to decrypt Web Push notification", e)
                }
            }
        }

        // Fallback or Standard Plaintext Data
        val title = message.notification?.title ?: message.data["title"] ?: "Overseerr"
        val body = message.notification?.body ?: message.data["message"] ?: "New notification"
        val imageUrl = message.notification?.imageUrl?.toString() ?: message.data["image"]
        val deepLink = message.data["url"]
        val notificationTypeString = message.data["type"]

        processIncomingNotification(title, body, imageUrl, deepLink, notificationTypeString)
    }

    private fun processIncomingNotification(
        title: String, 
        body: String, 
        imageUrl: String?, 
        deepLink: String?, 
        notificationTypeString: String?
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
        val lowerBody = body.lowercase()
        
        val shouldShow = when {
            notificationTypeString == "media_approved" || lowerTitle.contains("approved") -> settings.requestApproved
            notificationTypeString == "media_available" || lowerTitle.contains("available") -> settings.requestAvailable
            notificationTypeString == "media_declined" || lowerTitle.contains("declined") -> settings.requestDeclined
            notificationTypeString == "media_pending" || lowerTitle.contains("requested") -> settings.requestPendingApproval
            notificationTypeString == "media_failed" || lowerTitle.contains("failed") -> settings.requestProcessingFailed
            notificationTypeString == "issue_created" || lowerTitle.contains("reported") -> settings.issueReported
            notificationTypeString == "issue_comment" || lowerTitle.contains("comment") -> settings.issueComment
            notificationTypeString == "issue_resolved" || lowerTitle.contains("resolved") -> settings.issueResolved
            notificationTypeString == "issue_reopened" || lowerTitle.contains("reopened") -> settings.issueReopened
            else -> true
        }
        
        if (!shouldShow) {
            logger.d(TAG, "Dropping notification: $title (Filtered by local settings)")
            return
        }
        
        showNotification(title, body, imageUrl, deepLink)
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
