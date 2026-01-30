package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.Notification
import app.lusk.underseerr.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification operations.
 * Feature: underseerr
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5
 */
interface NotificationRepository {
    
    /**
     * Register device for push notifications.
     * Property 24: Status Change Notifications
     */
    suspend fun registerForPushNotifications(token: String): Result<Unit>
    
    /**
     * Unregister device from push notifications.
     */
    suspend fun unregisterPushNotifications(): Result<Unit>
    
    /**
     * Get notification history.
     */
    fun getNotificationHistory(): Flow<List<Notification>>
    
    /**
     * Mark notification as read.
     */
    suspend fun markNotificationAsRead(notificationId: String)
    
    /**
     * Save notification to history.
     */
    suspend fun saveNotification(notification: Notification)
    
    /**
     * Clear all notifications.
     */
    suspend fun clearAllNotifications()
    
    /**
     * Delete specific notification.
     */
    suspend fun deleteNotification(notificationId: String)

    /**
     * Update webhook notification settings including the Cloud Function URL.
     */
    suspend fun updateWebhookSettings(webhookUrl: String)
}
