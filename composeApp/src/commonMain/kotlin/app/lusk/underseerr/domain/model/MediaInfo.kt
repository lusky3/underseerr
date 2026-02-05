package app.lusk.underseerr.domain.model

/**
 * Contains information about media availability and request status.
 */
data class MediaInfo(
    val id: Int?,
    val status: MediaStatus,
    val requestId: Int?,
    val available: Boolean,
    val ratingKey: String? = null,
    val requests: List<MediaRequest> = emptyList()
)
