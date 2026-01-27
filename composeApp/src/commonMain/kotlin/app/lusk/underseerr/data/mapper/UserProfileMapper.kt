package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.remote.model.ApiUserProfile
import app.lusk.underseerr.domain.model.Permissions
import app.lusk.underseerr.domain.model.UserProfile

/**
 * Maps API user profile model to domain user profile model.
 */
fun ApiUserProfile.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        email = email ?: "",
        displayName = displayName ?: username ?: "User $id",
        avatar = avatar,
        requestCount = requestCount,
        permissions = decodePermissions(permissions),
        isPlexUser = plexId != null
    )
}

private fun decodePermissions(bitmask: Long): Permissions {
    // Overseerr Permission masks
    val ADMIN = 2L
    val MANAGE_REQUESTS = 16L
    val REQUEST = 32L
    
    return Permissions(
        canRequest = (bitmask and REQUEST) != 0L || (bitmask and ADMIN) != 0L,
        canManageRequests = (bitmask and MANAGE_REQUESTS) != 0L || (bitmask and ADMIN) != 0L,
        canViewRequests = true,
        isAdmin = (bitmask and ADMIN) != 0L
    )
}
