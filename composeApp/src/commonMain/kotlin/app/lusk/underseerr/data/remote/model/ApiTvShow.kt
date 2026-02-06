package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for TV show data from Overseerr.
 */
@Serializable
data class ApiTvShow(
    val id: Int,
    val name: String,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
    val numberOfSeasons: Int? = null,
    val mediaInfo: ApiMediaInfo? = null,
    @SerialName("credits")
    val credits: ApiCredits? = null,
    val relatedVideos: List<ApiRelatedVideo>? = null,
    val genres: List<ApiGenre>? = null,
    val tagline: String? = null,
    val status: String? = null,
    val seasons: List<ApiSeason>? = null,
    val lastAirDate: String? = null
)

