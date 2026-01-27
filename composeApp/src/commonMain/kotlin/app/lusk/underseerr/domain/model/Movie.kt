package app.lusk.underseerr.domain.model

/**
 * Domain model representing a movie.
 */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val mediaInfo: MediaInfo?,
    val cast: List<CastMember> = emptyList()
)
