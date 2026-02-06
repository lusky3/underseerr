package app.lusk.underseerr.data.remote.model

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
    @SerialName("tmdbId") val tmdbId: Int? = null,
    @SerialName("tvdbId") val tvdbId: Int? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("id") val id: Int? = null,
    @SerialName("status4k") val status4k: Int? = null,
    @SerialName("imdbId") val imdbId: String? = null,
    // Metadata often included in expanded responses
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("ratingKey") val ratingKey: String? = null
)

@Serializable
data class ApiRequestSeason(
    val id: Int,
    val seasonNumber: Int,
    val status: Int
)
