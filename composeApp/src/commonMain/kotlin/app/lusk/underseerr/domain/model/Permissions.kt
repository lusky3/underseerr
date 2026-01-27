package app.lusk.underseerr.domain.model

/**
 * Represents user permissions in the system.
 */
data class Permissions(
    val canRequest: Boolean,
    val canManageRequests: Boolean,
    val canViewRequests: Boolean,
    val isAdmin: Boolean
)
