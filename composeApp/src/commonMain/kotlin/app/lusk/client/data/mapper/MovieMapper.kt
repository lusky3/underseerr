package app.lusk.client.data.mapper

import app.lusk.client.data.local.entity.MovieEntity
import app.lusk.client.data.remote.model.ApiMovie
import app.lusk.client.domain.model.Movie
import app.lusk.client.domain.model.MediaInfo
import app.lusk.client.domain.model.MediaStatus

/**
 * Maps API movie model to domain movie model.
 */
fun ApiMovie.toDomain(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage ?: 0.0,
        mediaInfo = mediaInfo?.toDomain()
    )
}

/**
 * Maps database movie entity to domain movie model.
 */
fun MovieEntity.toDomain(): Movie {
    return Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        mediaInfo = if (mediaStatus != null) {
            MediaInfo(
                id = null, // Not stored in local database
                status = mediaStatus.toMediaStatus(),
                requestId = requestId,
                available = available
            )
        } else null
    )
}

/**
 * Maps domain movie model to database entity.
 */
fun Movie.toEntity(cachedAt: Long = System.currentTimeMillis()): MovieEntity {
    return MovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        mediaStatus = mediaInfo?.status?.ordinal,
        requestId = mediaInfo?.requestId,
        available = mediaInfo?.available ?: false,
        cachedAt = cachedAt
    )
}

/**
 * Converts integer status code to MediaStatus enum.
 */
private fun Int.toMediaStatus(): MediaStatus {
    return if (this in 0 until MediaStatus.values().size) {
        MediaStatus.values()[this]
    } else {
        MediaStatus.UNKNOWN
    }
}
