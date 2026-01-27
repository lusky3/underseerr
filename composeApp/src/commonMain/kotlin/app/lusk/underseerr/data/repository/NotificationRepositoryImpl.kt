package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.NotificationDao
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.mapper.toEntity
import app.lusk.underseerr.data.remote.api.UserKtorService
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.domain.model.Notification
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of NotificationRepository.
 * Feature: underseerr
 * Validates: Requirements 6.1, 6.2, 6.3
 */
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val userKtorService: UserKtorService
) : NotificationRepository {
    
    override suspend fun registerForPushNotifications(token: String): Result<Unit> {
        return safeApiCall {
            // TODO: Properly implement Web Push crypto if targeting standard Overseerr
            // For now, we attempt to send the token as the endpoint, hoping for a custom handler or future support
            val subscription = app.lusk.underseerr.data.remote.model.ApiPushSubscription(
                endpoint = token,
                keys = app.lusk.underseerr.data.remote.model.ApiPushKeys(
                    p256dh = "", // crypto keys needed for real Web Push
                    auth = ""
                )
            )
            userKtorService.addPushSubscription(subscription)
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
}
