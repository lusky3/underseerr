package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing notification history.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val deepLink: String? = null,
    val mediaId: Int? = null,
    val requestId: Int? = null
)
