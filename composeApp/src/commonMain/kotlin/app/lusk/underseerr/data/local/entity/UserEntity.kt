package app.lusk.underseerr.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int,
    val email: String?,
    val displayName: String,
    val avatar: String?,
    val isPlexUser: Boolean,
    val permissions: Long,
    val requestCount: Int,
    // Quota info (nullable)
    val movieLimit: Int?,
    val movieRemaining: Int?,
    val movieDays: Int?,
    val tvLimit: Int?,
    val tvRemaining: Int?,
    val tvDays: Int?,
    val cachedAt: Long = app.lusk.underseerr.util.nowMillis()
)
