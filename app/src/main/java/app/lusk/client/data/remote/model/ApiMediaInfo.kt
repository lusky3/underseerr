package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for media availability information.
 */
@Serializable
data class ApiMediaInfo(
    val status: Int,
    val requestId: Int? = null,
    val available: Boolean = false,
    val requests: List<ApiMediaRequest>? = null
)
