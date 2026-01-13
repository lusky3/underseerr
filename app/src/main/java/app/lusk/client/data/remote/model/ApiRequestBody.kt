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
    val is4k: Boolean = false,
    val serverId: Int? = null,
    val profileId: Int? = null,
    val rootFolder: String? = null
)
