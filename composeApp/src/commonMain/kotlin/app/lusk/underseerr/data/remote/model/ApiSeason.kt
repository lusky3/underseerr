package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

/**
 * API model for season data from Overseerr.
 */
@Serializable
data class ApiSeason(
    val id: Int,
    val seasonNumber: Int,
    val episodeCount: Int? = null,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val airDate: String? = null
)
