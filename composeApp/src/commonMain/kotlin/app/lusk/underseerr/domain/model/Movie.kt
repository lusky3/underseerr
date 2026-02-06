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
    val cast: List<CastMember> = emptyList(),
    val relatedVideos: List<RelatedVideo> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val runtime: Int? = null,
    val tagline: String? = null,
    val status: String? = null,
    val digitalReleaseDate: String? = null,
    val physicalReleaseDate: String? = null
)

