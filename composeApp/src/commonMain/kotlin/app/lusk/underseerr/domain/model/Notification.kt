package app.lusk.underseerr.domain.model

/**
 * Domain model representing a notification.
 */
data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false,
    val deepLink: String? = null,
    val mediaId: Int? = null,
    val requestId: Int? = null
)

/**
 * Types of notifications.
 */
enum class NotificationType {
    REQUEST_APPROVED,
    REQUEST_AVAILABLE,
    REQUEST_DECLINED,
    REQUEST_PENDING,
    SYSTEM
}
