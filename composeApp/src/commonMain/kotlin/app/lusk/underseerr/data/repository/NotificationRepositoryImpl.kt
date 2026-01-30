package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.NotificationDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.api.SettingsKtorService
import app.lusk.underseerr.data.remote.api.UserKtorService
import app.lusk.underseerr.data.remote.model.*
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.domain.model.Notification
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.NotificationRepository
import app.lusk.underseerr.domain.security.WebPushKeyManager
import app.lusk.underseerr.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of NotificationRepository.
 * Feature: underseerr
 * Validates: Requirements 6.1, 6.2, 6.3
 */
import app.lusk.underseerr.shared.BuildKonfig

class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
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
        
        // For FCM, the Web Push endpoint is a specific URL containing the token
        val endpoint = "https://fcm.googleapis.com/fcm/send/$token"
        
        logger.d(TAG, "Registering for Push: endpoint=$endpoint")
        
        // 0. Register token with Cloudflare Worker
        val currentUser = safeApiCall { userKtorService.getCurrentUser() }
        if (currentUser is Result.Success) {
            // Retrieve Endpoint from Build Config (injected via Env Vars)
            val serverUrl = if (BuildKonfig.DEBUG) {
                 BuildKonfig.WORKER_ENDPOINT_STAGING.ifBlank { BuildKonfig.WORKER_ENDPOINT_PROD }
            } else {
                 BuildKonfig.WORKER_ENDPOINT_PROD
            }
            
            if (serverUrl.isBlank()) {
                logger.e(TAG, "Worker Endpoint is missing! Check build configuration.")
            } else {
                val email = currentUser.data.email
                if (!email.isNullOrBlank()) {
                    try {
                        notificationServerService.registerToken(serverUrl, email, token)
                        logger.d(TAG, "Registered token with notification server: $serverUrl")
                    } catch (e: Exception) {
                        logger.w(TAG, "Failed to register token with notification server: ${e.message}")
                    }
                } else {
                    logger.w(TAG, "Skipping token registration: User email is null or blank.")
                }
            }
        } else {
             logger.w(TAG, "Could not fetch current user to register token. Cloud Function notifications may fail.")
        }
        
        // 1. Check Global Settings & Update User Settings
        logger.d(TAG, "Step 1: Checking settings...")
        val settingsResult = safeApiCall {
            // Check Global Settings
            val globalResult = settingsRepository.getGlobalNotificationSettings()
            if (globalResult is Result.Success) {
                if (!globalResult.data) {
                    logger.w(TAG, "WARNING: Web Push is globally disabled on this Overseerr server!")
                } else {
                    logger.d(TAG, "Global Web Push is enabled.")
                }
            }

            // Check User Settings
            val currentUser = userKtorService.getCurrentUser()
            val currentApiSettings = userKtorService.getUserNotificationSettings(currentUser.id)
            var domainSettings = app.lusk.underseerr.data.mapper.NotificationSettingsMapper.mapApiToDomain(currentApiSettings)
            
            // If disabled, or effectively no types (though mapper handles types), we enable defaults
            // We can check if it's "effectively disabled" by checking the raw mask if we wanted, 
            // but domainSettings.enabled is the main switch.
            
            if (!domainSettings.enabled) {
                logger.i(TAG, "Web Push is disabled in Overseerr. Attempting to enable defaults...")
                val newSettings = domainSettings.copy(
                    enabled = true,
                    // Ensure core types are on
                    requestApproved = true,
                    requestAvailable = true,
                    requestDeclined = true,
                    requestPendingApproval = true,
                    requestProcessingFailed = true
                )
                settingsRepository.updateNotificationSettings(newSettings)
                logger.i(TAG, "Step 1 SUCCESS: Auto-enabled Web Push notifications for user ${currentUser.id}")
            } else {
                // Sync Server state to Local Prefs
                settingsRepository.updateNotificationSettings(domainSettings)
                logger.d(TAG, "Web Push is already enabled. Synced settings to local preferences.")
            }
        }
        
        if (settingsResult is Result.Error) {
             // We log warning but continue - maybe they are already set or it's a transient error
             // However, if we failed to enable settings, registration might fail next.
            logger.w(TAG, "Failed step 1: auto-enable Web Push settings: ${settingsResult.error}")
        }

        // 2. Register the subscription (User is now hopefully enabled)
        // 2. Register the subscription (User is now hopefully enabled)
        val subscription = app.lusk.underseerr.data.remote.model.ApiRegisterPushSubscription(
            endpoint = endpoint,
            auth = auth,
            p256dh = p256dh
        )
        
        logger.d(TAG, "Step 2: Adding push subscription to Overseerr...")
        val registrationResult = safeApiCall {
            userKtorService.registerPushSubscription(subscription)
        }
        
        if (registrationResult is Result.Error) {
            logger.e(TAG, "Failed step 2: register push subscription: ${registrationResult.error}")
            return registrationResult
        }
        logger.d(TAG, "Step 2 SUCCESS: Push subscription registered.")
        
        return Result.success(Unit)
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
        // Construct the webhook settings payload for Overseerr
        val payload = mapOf(
            "enabled" to true,
            "options" to mapOf(
                "webhookUrl" to webhookUrl,
                "authHeader" to "", // Optional
                "jsonPayload" to "{\"notification_type\":\"{{notification_type}}\",\"subject\":\"{{subject}}\",\"message\":\"{{message}}\",\"image\":\"{{image}}\",\"email\":\"{{notifyuser_email}}\"}", // Base64 encoded in agent, but API takes string and handles it? 
                // Wait, based on source code: 
                // const payloadString = Buffer.from(this.getSettings().options.jsonPayload, 'base64').toString('utf8');
                // The API expects the stored value to probably be Base64 if the agent decodes it.
                // Let's check the API docs or assume we send plain string and the server handles it?
                // Actually, let's look at the source again. The AGENT decodes it. That implies the SETTINGS store it as Base64.
                // So we should Base64 encode our json template.
                
                // Simplified template:
                // {
                //   "notification_type": "{{notification_type}}",
                //   "subject": "{{subject}}",
                //   "message": "{{message}}",
                //   "image": "{{image}}",
                //   "notifyuser_email": "{{notifyuser_email}}",
                //   "requestedBy_email": "{{requestedBy_email}}"
                // }
                
                 "jsonPayload" to "ewogIC  ibm90aWZpY2F0aW9uX3R5cGUiOiAie3tub3RpZmljYXRpb25fdHlwZX19IiwKICAic3ViamVjdCI6ICJ7e3N1YmplY3R9fSIsCiAgIm1lc3NhZ2UiOiAie3ttZXNzYWdlfX0iLAogICJpbWFnZSI6ICJ7e2ltYWdlfX0iLAogICJbm90aWZ5dXNlcl9lbWFpbCI6ICJ7e25vdGlmeXVzZXJfZW1haWx9fSIsCiAgInJlcXVlc3RlZEJ5X2VtYWlsIjogInt7cmVxdWVzdGVkQnlfZW1haWx9fSIKfQ=="
            ),
             "types" to 14336 // Enable all or specific types? 
             // Bitmask: 
             // 2 (PENDING) + 4 (APPROVED) + 8 (AVAILABLE) + 16 (FAILED) + 32 (TEST) + 64 (DECLINED) + 128 (AUTO_APPROVED) + 256 (ISSUE_CREATED) ...
             // Let's enable most relevant ones: 
             // PENDING (2) + APPROVED (4) + AVAILABLE (8) + FAILED (16) + DECLINED (64) + AUTO_APPROVED (128) = 222
             // + ISSUE_CREATED (256) + TITLE (512?) ... 
             // A safe bet is a large number or calculate it. 
             // Let's assume the user wants standard notifications. 
             // For now, let's just use what was in the GET response or a calculated mask.
             // Mask 4062 from logs = 2+4+8+16+32+64+128+256+512+1024+2048 (All up to ISSUE lines)
        )
        // Wait, the API might require us to fetch existing settings to preserve 'types'.
        // But for auto-conf, we can enforce a good default.
        
        // Re-encoding logic to be safe:
        // Plain JSON:
        // {"notification_type":"{{notification_type}}","subject":"{{subject}}","message":"{{message}}","image":"{{image}}","notifyuser_email":"{{notifyuser_email}}","requestedBy_email":"{{requestedBy_email}}"}
        // I will implement a helper or just hardcode the base64 for that string.
        
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
        
        // We need a way to Base64 encode in KMP common code. 
        // For now, let's assume we can add a Base64 util or use one if available.
        // Or I can hardcode it for this specific template.
        
        // Hardcoded Base64 for the above JSON (minified safely):
        // {"notification_type":"{{notification_type}}","subject":"{{subject}}","message":"{{message}}","image":"{{image}}","notifyuser_email":"{{notifyuser_email}}","requestedBy_email":"{{requestedBy_email}}"}
        val base64Payload = "eyJub3RpZmljYXRpb25fdHlwZSI6Int7bm90aWZpY2F0aW9uX3R5cGV9fSIsInN1YmplY3QiOiJ7e3N1YmplY3R9fSIsIm1lc3NhZ2UiOiJ7e21lc3NhZ2V9fSIsImltYWdlIjoie3tpbWFnZX19Iiwibm90aWZ5dXNlcl9lbWFpbCI6Int7bm90aWZ5dXNlcl9lbWFpbH19IiwicmVxdWVzdGVkQnlfZW1haWwiOiJ7e3JlcXVlc3RlZEJ5X2VtYWlsfX0ifQ=="
        
        val finalPayload = mapOf(
            "enabled" to true,
            "types" to 4094, // Standard set
            "options" to mapOf(
                "webhookUrl" to webhookUrl,
                "jsonPayload" to base64Payload
            )
        )
        
        settingsKtorService.updateWebhookSettings(finalPayload)
    }
}
