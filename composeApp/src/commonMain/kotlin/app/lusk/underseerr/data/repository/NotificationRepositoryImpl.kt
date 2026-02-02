package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.NotificationDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.api.SettingsKtorService
import app.lusk.underseerr.data.remote.api.UserKtorService
import app.lusk.underseerr.data.remote.model.*
import app.lusk.underseerr.data.remote.safeApiCall
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import app.lusk.underseerr.domain.model.Notification
import app.lusk.underseerr.domain.repository.NotificationSettings
import app.lusk.underseerr.domain.model.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.serializer
import app.lusk.underseerr.domain.repository.NotificationRepository
import app.lusk.underseerr.domain.security.WebPushKeyManager
import app.lusk.underseerr.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import app.lusk.underseerr.shared.BuildKonfig
import kotlinx.coroutines.flow.map

/**
 * Implementation of NotificationRepository.
 * Feature: underseerr
 * Validates: Requirements 6.1, 6.2, 6.3
 */
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val authRepository: app.lusk.underseerr.domain.repository.AuthRepository,
    private val userKtorService: UserKtorService,
    private val settingsKtorService: SettingsKtorService,
    private val settingsRepository: app.lusk.underseerr.domain.repository.SettingsRepository,
    private val webPushKeyManager: WebPushKeyManager,
    private val notificationServerService: app.lusk.underseerr.data.remote.api.NotificationServerService,
    private val logger: AppLogger
) : NotificationRepository {
    
    private companion object {
        const val TAG = "NotificationRepository"
    }
    
    override suspend fun registerForPushNotifications(token: String): Result<Unit> {
        val (p256dh, auth) = webPushKeyManager.getOrCreateWebPushKeys()
        
        // Determine Notification Server URL (Worker Proxy)
        val customServerUrl = settingsRepository.getNotificationServerUrl().first()
        val serverUrl = if (customServerUrl.isNullOrBlank()) {
             if (BuildKonfig.DEBUG) BuildKonfig.WORKER_ENDPOINT_STAGING else BuildKonfig.WORKER_ENDPOINT_PROD
        } else {
             customServerUrl
        }
        
        val cleanUrl = serverUrl.trimEnd('/')
        // Endpoint points to the Worker, which will proxy to FCM.
        val endpoint = "$cleanUrl/push/$token"
        
        logger.d(TAG, "Registering for Web Push Proxy: endpoint=$endpoint")
        
        // Register token with our Notification Worker/Proxy for subscription gating
        val userResult = authRepository.getCurrentUser()
        if (userResult is app.lusk.underseerr.domain.model.Result.Success) {
            val user = userResult.data
            try {
                val webhookSecret = preferencesManager.getWebhookSecret().first()
                notificationServerService.registerToken(
                    serverUrl = cleanUrl,
                    email = user.email ?: "",
                    token = token,
                    userId = user.id.toString(),
                    webhookSecret = webhookSecret
                )
            } catch (e: Exception) {
                logger.e(TAG, "Failed to register token with Underseerr Worker", e)
                // We continue anyway, as the Overseerr registration is main priority
            }
        }

        return safeApiCall {
            // Register the subscription on Overseerr
            val subscription = ApiRegisterPushSubscription(
                endpoint = endpoint,
                auth = auth,
                p256dh = p256dh,
                userAgent = "Mozilla/5.0 (Linux; Android 13; Underseerr) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
            )
            userKtorService.registerPushSubscription(subscription)
        }
    }
    
    override suspend fun unregisterPushNotifications(): Result<Unit> {
        return safeApiCall {
            // In a real implementation, this would call the Overseerr API
            // to unregister the FCM token
            Unit
        }
    }
    
    override fun getNotificationHistory(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun markNotificationAsRead(notificationId: String) {
        notificationDao.markAsRead(notificationId)
    }
    
    override suspend fun saveNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }
    
    override suspend fun clearAllNotifications() {
        notificationDao.deleteAllNotifications()
    }
    
    override suspend fun deleteNotification(notificationId: String) {
        notificationDao.deleteNotification(notificationId)
    }

    override suspend fun updateWebhookSettings(webhookUrl: String): Result<Unit> = safeApiCall {
        // JSON template for Overseerr
        val jsonTemplate = """
            {
                "notification_type": "{{notification_type}}",
                "subject": "{{subject}}",
                "message": "{{message}}",
                "image": "{{image}}",
                "notifyuser_email": "{{notifyuser_email}}",
                "requestedBy_email": "{{requestedBy_email}}"
            }
        """.trimIndent()
        
        // Overseerr server calls JSON.parse on the input string.
        // If we send raw JSON, it stores an Object, which crashes the WebUI (Ace Editor expects String).
        // We must send a JSON-encoded string so JSON.parse returns the String.
        // This effectively double-encodes the JSON.
        val safePayload = Json.encodeToString(String.serializer(), jsonTemplate)
        
        val payload = WebhookSettingsPayload(
            enabled = true,
            types = 8190, // Mask covering all types inclusive of MEDIA_AUTO_REQUESTED
            options = WebhookOptions(
                webhookUrl = webhookUrl,
                jsonPayload = safePayload
            )
        )
        
        settingsKtorService.updateWebhookSettings(payload)
    }

    override suspend fun fetchRemoteSettings(userId: Int): Result<NotificationSettings> = safeApiCall {
        val remote = settingsKtorService.getUserNotificationSettings(userId)
        val mask = remote.notificationTypes.webpush ?: 0
        val enabled = remote.webPushEnabled == true
        
        NotificationSettings(
            enabled = enabled,
            // Bitmask values based on Overseerr source
            requestPendingApproval = (mask and 2) != 0,
            requestApproved = (mask and 4) != 0,
            requestAvailable = (mask and 8) != 0,
            requestProcessingFailed = (mask and 16) != 0,
            requestDeclined = (mask and 64) != 0,
            requestAutoApproved = (mask and 128) != 0,
            issueReported = (mask and 256) != 0,
            issueComment = (mask and 512) != 0,
            issueResolved = (mask and 1024) != 0,
            issueReopened = (mask and 2048) != 0,
            mediaAutoRequested = (mask and 4096) != 0,
            syncEnabled = true
        )
    }
}

@Serializable
data class WebhookOptions(
    val webhookUrl: String,
    val authHeader: String? = null,
    val jsonPayload: String?
)

@Serializable
data class WebhookSettingsPayload(
    val enabled: Boolean,
    val types: Int,
    val options: WebhookOptions
)
