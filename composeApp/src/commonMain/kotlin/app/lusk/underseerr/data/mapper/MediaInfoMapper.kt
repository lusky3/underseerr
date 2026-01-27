package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.remote.model.ApiMediaInfo
import app.lusk.underseerr.domain.model.MediaInfo
import app.lusk.underseerr.domain.model.MediaStatus

/**
 * Maps API media info model to domain media info model.
 */
fun ApiMediaInfo.toDomain(): MediaInfo {
    return MediaInfo(
        id = id,
        status = status.toMediaStatus(),
        requestId = requestId,
        available = available,
        requests = requests?.map { it.toDomain() } ?: emptyList()
    )
}

/**
 * Converts integer status code to MediaStatus enum.
 */
private fun Int.toMediaStatus(): MediaStatus {
    return when (this) {
        1 -> MediaStatus.UNKNOWN
        2 -> MediaStatus.PENDING
        3 -> MediaStatus.PROCESSING
        4 -> MediaStatus.PARTIALLY_AVAILABLE
        5 -> MediaStatus.AVAILABLE
        else -> MediaStatus.UNKNOWN
    }
}
