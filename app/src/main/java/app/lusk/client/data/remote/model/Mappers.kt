package app.lusk.client.data.remote.model

import app.lusk.client.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mapper functions to convert API models to domain models.
 */

fun ApiMovie.toMovie(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage ?: 0.0,
        mediaInfo = mediaInfo?.toMediaInfo()
    )
}

fun ApiTvShow.toTvShow(): TvShow {
    return TvShow(
        id = id,
        name = name,
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage ?: 0.0,
        numberOfSeasons = numberOfSeasons ?: 0,
        mediaInfo = mediaInfo?.toMediaInfo()
    )
}

fun ApiSearchResults.toSearchResults(): SearchResults {
    return SearchResults(
        page = page,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results.map { it.toSearchResult() }
    )
}

fun ApiSearchResult.toSearchResult(): SearchResult {
    return SearchResult(
        id = id,
        mediaType = when (mediaType?.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> MediaType.MOVIE
        },
        title = title ?: name ?: "",
        overview = overview ?: "",
        posterPath = posterPath,
        releaseDate = releaseDate ?: firstAirDate,
        voteAverage = voteAverage ?: 0.0
    )
}

fun ApiMediaRequest.toMediaRequest(): MediaRequest {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    
    val requestedDate = try {
        if (createdAt != null) {
            dateFormat.parse(createdAt)?.time ?: System.currentTimeMillis()
        } else {
            System.currentTimeMillis()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
    
    // Determine media ID (prefer tmdbId for movies, tvdbId for TV, fallback to media.id or top id)
    val finalMediaId = media?.tmdbId ?: media?.tvdbId ?: media?.id ?: id
    val finalMediaType = media?.mediaType ?: type
    
    return MediaRequest(
        id = id,
        mediaType = when (finalMediaType.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> MediaType.MOVIE
        },
        mediaId = finalMediaId,
        title = "Title Unavailable", // API response in this env is missing title
        posterPath = null,
        status = when {
            media?.status == 5 -> RequestStatus.AVAILABLE
            status == 1 -> RequestStatus.PENDING
            status == 2 -> RequestStatus.APPROVED
            status == 3 -> RequestStatus.DECLINED
            status == 4 -> RequestStatus.AVAILABLE
            else -> RequestStatus.PENDING
        },
        requestedDate = requestedDate,
        seasons = seasons?.map { it.seasonNumber }
    )
}

fun ApiMediaInfo.toMediaInfo(): MediaInfo {
    return MediaInfo(
        status = when (status) {
            1 -> MediaStatus.UNKNOWN
            2 -> MediaStatus.PENDING
            3 -> MediaStatus.PROCESSING
            4 -> MediaStatus.PARTIALLY_AVAILABLE
            5 -> MediaStatus.AVAILABLE
            else -> MediaStatus.UNKNOWN
        },
        requestId = requestId,
        available = available,
        requests = requests?.map { it.toMediaRequest() } ?: emptyList()
    )
}


fun ApiSearchResult.toMovie(): Movie {
    return Movie(
        id = id,
        title = title ?: name ?: "",
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = null,
        releaseDate = releaseDate ?: firstAirDate,
        voteAverage = voteAverage ?: 0.0,
        mediaInfo = null
    )
}

fun ApiSearchResult.toTvShow(): TvShow {
    return TvShow(
        id = id,
        name = name ?: title ?: "",
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = null,
        firstAirDate = firstAirDate ?: releaseDate,
        voteAverage = voteAverage ?: 0.0,
        numberOfSeasons = 0, // Not available in search result
        mediaInfo = null
    )
}
