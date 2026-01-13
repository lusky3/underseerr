package app.lusk.client.data.mapper

import app.lusk.client.data.remote.model.ApiSearchResult
import app.lusk.client.data.remote.model.ApiSearchResults
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.model.SearchResult
import app.lusk.client.domain.model.SearchResults

/**
 * Maps API search results model to domain search results model.
 */
fun ApiSearchResults.toDomain(): SearchResults {
    return SearchResults(
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toDomain() }
    )
}

/**
 * Maps API search result model to domain search result model.
 */
fun ApiSearchResult.toDomain(): SearchResult {
    return SearchResult(
        id = id,
        mediaType = mediaType?.toMediaType() ?: MediaType.MOVIE,
        title = title ?: name ?: "",
        overview = overview ?: "",
        posterPath = posterPath,
        releaseDate = releaseDate ?: firstAirDate,
        voteAverage = voteAverage ?: 0.0
    )
}

/**
 * Converts string media type to MediaType enum.
 */
private fun String.toMediaType(): MediaType {
    return when (this.lowercase()) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        else -> MediaType.MOVIE
    }
}
