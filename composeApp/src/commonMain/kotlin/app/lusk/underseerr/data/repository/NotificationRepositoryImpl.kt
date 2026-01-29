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
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val userKtorService: UserKtorService,
    private val settingsRepository: app.lusk.underseerr.domain.repository.SettingsRepository,
    private val webPushKeyManager: WebPushKeyManager,
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
}
