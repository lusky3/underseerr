package app.lusk.underseerr.domain.model

/**
 * Represents an authenticated session with Overseerr.
 */
data class UnderseerrSession(
    val apiKey: String,
    val userId: Int,
    val serverUrl: String,
    val expiresAt: Long?
)
