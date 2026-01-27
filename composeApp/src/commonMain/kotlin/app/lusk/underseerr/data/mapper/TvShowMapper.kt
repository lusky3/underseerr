package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.local.entity.TvShowEntity
import app.lusk.underseerr.data.remote.model.ApiTvShow
import app.lusk.underseerr.domain.model.TvShow
import app.lusk.underseerr.domain.model.MediaInfo
import app.lusk.underseerr.domain.model.MediaStatus

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
                id = null, // Not stored in local database
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
fun TvShow.toEntity(cachedAt: Long = app.lusk.underseerr.util.nowMillis()): TvShowEntity {
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
