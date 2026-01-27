package app.lusk.underseerr.domain.model

/**
 * Represents the availability status of media content.
 */
enum class MediaStatus {
    AVAILABLE,
    PENDING,
    PROCESSING,
    PARTIALLY_AVAILABLE,
    UNKNOWN
}
