package app.lusk.underseerr.domain.model

/**
 * Domain model representing a TV season.
 */
data class Season(
    val id: Int,
    val seasonNumber: Int,
    val episodeCount: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val airDate: String?
)
