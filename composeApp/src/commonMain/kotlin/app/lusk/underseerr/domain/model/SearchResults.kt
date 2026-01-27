package app.lusk.underseerr.domain.model

/**
 * Represents search results containing both movies and TV shows.
 */
data class SearchResults(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<SearchResult>
)

/**
 * Represents a single search result that can be either a movie or TV show.
 */
data class SearchResult(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double
)
