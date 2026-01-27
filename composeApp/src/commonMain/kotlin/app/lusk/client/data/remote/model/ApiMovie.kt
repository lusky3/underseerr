package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for movie data from Overseerr.
 */
@Serializable
data class ApiMovie(
    val id: Int,
    val title: String,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val voteAverage: Double? = null,
    val mediaInfo: ApiMediaInfo? = null,
    @SerialName("credits")
    val credits: ApiCredits? = null
)
