package app.lusk.underseerr.domain.model

/**
 * Domain model representing a user profile.
 */
data class UserProfile(
    val id: Int,
    val email: String?,
    val displayName: String,
    val avatar: String?,
    val requestCount: Int,
    val permissions: Permissions,
    val rawPermissions: Long,
    val isPlexUser: Boolean
)

/**
 * User statistics for profile display.
 */
data class UserStatistics(
    val totalRequests: Int,
    val approvedRequests: Int,
    val declinedRequests: Int,
    val pendingRequests: Int,
    val availableRequests: Int
)
