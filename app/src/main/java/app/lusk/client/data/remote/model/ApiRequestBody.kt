package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for submitting a media request.
 */
@Serializable
data class ApiRequestBody(
    val mediaId: Int,
    val mediaType: String,
    val seasons: List<Int>? = null,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("serverId") val serverId: Int? = null,
    @SerialName("profileId") val qualityProfile: Int? = null,
    @SerialName("rootFolder") val rootFolder: String? = null
)
