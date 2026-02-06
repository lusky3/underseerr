package app.lusk.underseerr.data.remote.model

import app.lusk.underseerr.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
        mediaInfo = mediaInfo?.toMediaInfo(),
        cast = credits?.cast?.map { it.toCastMember() } ?: emptyList(),
        relatedVideos = relatedVideos?.mapNotNull { it.toRelatedVideo() } ?: emptyList(),
        genres = genres?.map { it.toGenre() } ?: emptyList(),
        runtime = runtime,
        tagline = tagline,
        status = status,
        digitalReleaseDate = digitalReleaseDate,
        physicalReleaseDate = physicalReleaseDate
    )
}

fun ApiRelatedVideo.toRelatedVideo(): RelatedVideo? {
    val url = this.url ?: return null
    val key = this.key ?: return null
    val name = this.name ?: return null
    val site = this.site ?: return null
    
    val videoType = when (type?.lowercase()) {
        "trailer" -> VideoType.TRAILER
        "teaser" -> VideoType.TEASER
        "clip" -> VideoType.CLIP
        "featurette" -> VideoType.FEATURETTE
        "behind the scenes" -> VideoType.BEHIND_THE_SCENES
        "bloopers" -> VideoType.BLOOPERS
        "opening credits" -> VideoType.OPENING_CREDITS
        else -> VideoType.OTHER
    }
    
    return RelatedVideo(
        url = url,
        key = key,
        name = name,
        type = videoType,
        site = site
    )
}

fun ApiCastMember.toCastMember(): CastMember {
    return CastMember(
        id = id,
        name = name,
        character = character,
        profilePath = profilePath,
        order = order
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
        mediaInfo = mediaInfo?.toMediaInfo(),
        cast = credits?.cast?.map { it.toCastMember() } ?: emptyList(),
        relatedVideos = relatedVideos?.mapNotNull { it.toRelatedVideo() } ?: emptyList(),
        genres = genres?.map { it.toGenre() } ?: emptyList(),
        tagline = tagline,
        status = status,
        seasons = seasons?.map { it.toSeason() } ?: emptyList(),
        lastAirDate = lastAirDate
    )
}

fun ApiSeason.toSeason(): Season {
    return Season(
        id = id,
        seasonNumber = seasonNumber,
        episodeCount = episodeCount ?: 0,
        name = name ?: "Season $seasonNumber",
        overview = overview,
        posterPath = posterPath,
        airDate = airDate
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
        voteAverage = voteAverage ?: 0.0,
        mediaInfo = mediaInfo?.toMediaInfo(),
        ratingKey = ratingKey
    )
}

fun ApiMediaRequest.toMediaRequest(): MediaRequest {
    val requestedDate = try {
        if (createdAt != null) {
            Instant.parse(createdAt).toEpochMilliseconds()
        } else {
            app.lusk.underseerr.util.nowMillis()
        }
    } catch (e: Exception) {
        app.lusk.underseerr.util.nowMillis()
    }
    
    // Determine media ID (prefer tmdbId for movies, tvdbId for TV, fallback to media.id or top id)
    val finalMediaId = media?.tmdbId ?: media?.tvdbId ?: media?.id ?: id
    val finalMediaType = media?.mediaType ?: type
    
    // Attempt to extract title from media object
    val finalTitle = media?.title ?: media?.name ?: "Title Unavailable"
    val finalPosterPath = media?.posterPath
    
    return MediaRequest(
        id = id,
        mediaType = when (finalMediaType.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> MediaType.MOVIE
        },
        mediaId = finalMediaId,
        title = finalTitle,
        posterPath = finalPosterPath,
        status = when {
            media?.status == 5 -> RequestStatus.AVAILABLE
            status == 1 -> RequestStatus.PENDING
            status == 2 -> RequestStatus.APPROVED
            status == 3 -> RequestStatus.DECLINED
            status == 4 -> RequestStatus.AVAILABLE
            else -> RequestStatus.PENDING
        },
        requestedDate = requestedDate,
        seasons = seasons?.map { it.seasonNumber },
        ratingKey = media?.ratingKey
    )
}

fun ApiMediaInfo.toMediaInfo(): MediaInfo {
    return MediaInfo(
        id = id,
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
        ratingKey = ratingKey ?: ratingKey4k,
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

fun ApiGenre.toGenre(): Genre {
    return Genre(
        id = id,
        name = name
    )
}

fun ApiPerson.toPerson(): Person {
    val allCredits = mutableListOf<PersonCredit>()
    val apiCredits = combinedCredits ?: credits
    
    println("DEBUG toPerson: combinedCredits = $combinedCredits")
    println("DEBUG toPerson: credits = $credits")
    println("DEBUG toPerson: apiCredits = $apiCredits")
    println("DEBUG toPerson: cast size = ${apiCredits?.cast?.size}")
    println("DEBUG toPerson: crew size = ${apiCredits?.crew?.size}")
    
    apiCredits?.cast?.map { it.toPersonCredit() }?.let { allCredits.addAll(it) }
    apiCredits?.crew?.map { it.toPersonCredit() }?.let { allCredits.addAll(it) }
    
    println("DEBUG toPerson: total credits = ${allCredits.size}")
    
    return Person(
        id = id,
        name = name,
        biography = biography,
        birthday = birthday,
        deathday = deathday,
        placeOfBirth = placeOfBirth,
        profilePath = profilePath,
        knownForDepartment = knownForDepartment,
        credits = allCredits
    )
}

fun ApiPersonCredit.toPersonCredit(): PersonCredit {
    return PersonCredit(
        id = id,
        mediaType = when (mediaType.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> MediaType.MOVIE
        },
        title = title ?: name ?: "",
        overview = overview ?: "",
        posterPath = posterPath,
        releaseDate = releaseDate ?: firstAirDate,
        voteAverage = voteAverage ?: 0.0,
        character = character,
        job = job
    )
}
