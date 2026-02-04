package app.lusk.underseerr.domain.model

/**
 * Represents information about an Overseerr server.
 */
data class ServerInfo(
    val version: String,
    val initialized: Boolean = true,
    val applicationUrl: String = "",
    val isJellyseerr: Boolean = false
)
