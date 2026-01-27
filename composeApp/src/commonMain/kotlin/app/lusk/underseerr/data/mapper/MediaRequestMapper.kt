package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.local.entity.MediaRequestEntity
import app.lusk.underseerr.data.remote.model.ApiMediaRequest
import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.RequestStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Maps API media request model to domain media request model.
 */
fun ApiMediaRequest.toDomain(): MediaRequest {
    val finalMediaId = media?.tmdbId ?: media?.tvdbId ?: media?.id ?: id
    val finalMediaType = media?.mediaType ?: type
    
    return MediaRequest(
        id = id,
        mediaType = finalMediaType.toMediaType(),
        mediaId = finalMediaId,
        title = "Title Unavailable",
        posterPath = null,
        status = status.toRequestStatus(),
        requestedDate = createdAt?.toTimestamp() ?: app.lusk.underseerr.util.nowMillis(),
        seasons = seasons?.map { it.seasonNumber }
    )
}

/**
 * Maps database media request entity to domain media request model.
 */
fun MediaRequestEntity.toDomain(): MediaRequest {
    return MediaRequest(
        id = id,
        mediaType = mediaType.toMediaType(),
        mediaId = mediaId,
        title = title,
        posterPath = posterPath,
        status = status.toRequestStatus(),
        requestedDate = requestedDate,
        seasons = seasons
    )
}

/**
 * Maps domain media request model to database entity.
 */
fun MediaRequest.toEntity(cachedAt: Long = app.lusk.underseerr.util.nowMillis()): MediaRequestEntity {
    return MediaRequestEntity(
        id = id,
        mediaType = mediaType.name.lowercase(),
        mediaId = mediaId,
        title = title,
        posterPath = posterPath,
        status = when (status) {
            RequestStatus.PENDING -> 1
            RequestStatus.APPROVED -> 2
            RequestStatus.DECLINED -> 3
            RequestStatus.AVAILABLE -> 4
        },
        requestedDate = requestedDate,
        seasons = seasons,
        cachedAt = cachedAt
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

/**
 * Converts integer status code to RequestStatus enum.
 */
private fun Int.toRequestStatus(): RequestStatus {
    return when (this) {
        1 -> RequestStatus.PENDING
        2 -> RequestStatus.APPROVED
        3 -> RequestStatus.DECLINED
        4 -> RequestStatus.AVAILABLE
        5 -> RequestStatus.AVAILABLE // Also map 5 (media status available) to AVAILABLE
        else -> RequestStatus.PENDING
    }
}

/**
 * Converts ISO 8601 date string to timestamp.
 */
private fun String.toTimestamp(): Long {
    return try {
        Instant.parse(this).toEpochMilliseconds()
    } catch (e: Exception) {
        app.lusk.underseerr.util.nowMillis()
    }
}
