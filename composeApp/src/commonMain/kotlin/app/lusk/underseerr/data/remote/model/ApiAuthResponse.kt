package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for authentication response.
 */
@Serializable
data class ApiAuthResponse(
    @SerialName("api_key") val apiKey: String,
    @SerialName("user_id") val userId: Int
)
