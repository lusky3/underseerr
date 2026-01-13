package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for user profile data.
 */
@Serializable
data class ApiUserProfile(
    val id: Int = -1,
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
    val requestCount: Int = 0,
    val permissions: Long = 0
)

