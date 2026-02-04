package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for public server settings.
 */
@Serializable
data class ApiServerPublicSettings(
    @SerialName("initialized") val initialized: Boolean = true,
    @SerialName("applicationTitle") val applicationTitle: String = "",
    @SerialName("applicationUrl") val applicationUrl: String = "",
    @SerialName("localLogin") val localLogin: Boolean = true,
    @SerialName("mediaServerLogin") val mediaServerLogin: Boolean = true, // Jellyseerr specific
    @SerialName("newPlexLogin") val newPlexLogin: Boolean = true,
    @SerialName("mediaServerType") val mediaServerType: Int = 1, // 1 = Overseerr (default implied), 4 = Jellyseerr
    @SerialName("vapidPublic") val vapidPublic: String = ""
)

fun ApiServerPublicSettings.isJellyseerr(): Boolean {
    return mediaServerType == 4
}
