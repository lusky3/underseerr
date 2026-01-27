package app.lusk.underseerr.domain.model

/**
 * Domain model representing a TV show.
 */
data class TvShow(
    val id: Int,
    val name: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String?,
    val voteAverage: Double,
    val numberOfSeasons: Int,
    val mediaInfo: MediaInfo?,
    val cast: List<CastMember> = emptyList()
)
