package app.lusk.client.data.mapper

import app.lusk.client.data.local.entity.TvShowEntity
import app.lusk.client.data.remote.model.ApiTvShow
import app.lusk.client.domain.model.TvShow
import app.lusk.client.domain.model.MediaInfo
import app.lusk.client.domain.model.MediaStatus

/**
 * Maps API TV show model to domain TV show model.
 */
fun ApiTvShow.toDomain(): TvShow {
    return TvShow(
        id = id,
        name = name,
        overview = overview ?: "",
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage ?: 0.0,
        numberOfSeasons = numberOfSeasons ?: 0,
        mediaInfo = mediaInfo?.toDomain()
    )
}

/**
 * Maps database TV show entity to domain TV show model.
 */
fun TvShowEntity.toDomain(): TvShow {
    return TvShow(
        id = id,
        name = name,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        numberOfSeasons = numberOfSeasons,
        mediaInfo = if (mediaStatus != null) {
            MediaInfo(
                status = mediaStatus.toMediaStatus(),
                requestId = requestId,
                available = available
            )
        } else null
    )
}

/**
 * Maps domain TV show model to database entity.
 */
fun TvShow.toEntity(cachedAt: Long = System.currentTimeMillis()): TvShowEntity {
    return TvShowEntity(
        id = id,
        name = name,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        firstAirDate = firstAirDate,
        voteAverage = voteAverage,
        numberOfSeasons = numberOfSeasons,
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
