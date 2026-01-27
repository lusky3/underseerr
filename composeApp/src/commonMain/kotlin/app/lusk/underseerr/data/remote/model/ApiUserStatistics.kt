package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for user statistics.
 */
@Serializable
data class ApiUserStatistics(
    @SerialName("total") val totalRequests: Int = 0,
    @SerialName("approved") val approvedRequests: Int = 0,
    @SerialName("declined") val declinedRequests: Int = 0,
    @SerialName("pending") val pendingRequests: Int = 0,
    @SerialName("available") val availableRequests: Int = 0,
    @SerialName("movie") val movie: Int = 0,
    @SerialName("tv") val tv: Int = 0,
    @SerialName("music") val music: Int = 0
)
