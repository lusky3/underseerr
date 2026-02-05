package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

/**
 * API model for related videos (trailers, featurettes, etc.) from Overseerr.
 */
@Serializable
data class ApiRelatedVideo(
    val url: String? = null,
    val key: String? = null,
    val name: String? = null,
    val size: Int? = null,
    val type: String? = null, // Clip, Teaser, Trailer, Featurette, Opening Credits, Behind the Scenes, Bloopers
    val site: String? = null  // YouTube
)
