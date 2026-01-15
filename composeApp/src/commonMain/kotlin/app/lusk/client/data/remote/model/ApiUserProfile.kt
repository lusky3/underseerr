package app.lusk.client.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API model for user profile data.
 */
@Serializable
data class ApiUserProfile(
    @SerialName("id")
    val id: Int = -1,
    @SerialName("email")
    val email: String? = null,
    @SerialName("username")
    val username: String? = null,
    @SerialName("displayName")
    val displayName: String? = null,
    @SerialName("avatar")
    val avatar: String? = null,
    @SerialName("requestCount")
    val requestCount: Int = 0,
    @SerialName("permissions")
    val permissions: Long = 0,
    @SerialName("plexId")
    val plexId: Int? = null,
    @SerialName("plexUsername")
    val plexUsername: String? = null
)

