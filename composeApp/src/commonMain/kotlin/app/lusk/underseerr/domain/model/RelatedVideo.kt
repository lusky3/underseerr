package app.lusk.underseerr.domain.model

/**
 * Represents a related video (trailer, featurette, etc.) for a movie or TV show.
 */
data class RelatedVideo(
    val url: String,
    val key: String,
    val name: String,
    val type: VideoType,
    val site: String
)

/**
 * Types of related videos.
 */
enum class VideoType {
    TRAILER,
    TEASER,
    CLIP,
    FEATURETTE,
    BEHIND_THE_SCENES,
    BLOOPERS,
    OPENING_CREDITS,
    OTHER
}
