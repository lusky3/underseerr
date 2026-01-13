package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for media request data.
 */
@Serializable
data class ApiMediaRequest(
    val id: Int,
    @SerialName("type") val type: String = "movie",
    val status: Int,
    val createdAt: String? = null,
    val media: ApiRequestMedia? = null,
    val seasons: List<ApiRequestSeason>? = null
)

@Serializable
data class ApiRequestMedia(
    @SerialName("mediaType") val mediaType: String? = null,
    val tmdbId: Int? = null,
    val tvdbId: Int? = null,
    val status: Int? = null,
    val id: Int? = null
)

@Serializable
data class ApiRequestSeason(
    val id: Int,
    val seasonNumber: Int,
    val status: Int
)
