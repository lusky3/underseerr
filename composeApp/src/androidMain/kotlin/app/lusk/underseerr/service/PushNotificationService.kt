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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PushNotificationService : FirebaseMessagingService() {

    private val notificationRepository: NotificationRepository by inject()
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
        
        val title = message.notification?.title ?: message.data["title"] ?: "Overseerr"
        val body = message.notification?.body ?: message.data["message"] ?: "New notification"
        val imageUrl = message.notification?.imageUrl?.toString() ?: message.data["image"]
        val deepLink = message.data["url"]
        
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
