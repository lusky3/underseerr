package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for media availability information.
 */
@Serializable
data class ApiMediaInfo(
    val id: Int? = null,
    val tmdbId: Int? = null,
    val mediaType: String? = null,
    val status: Int,
    val requestId: Int? = null,
    val available: Boolean = false,
    val requests: List<ApiMediaRequest>? = null
)
