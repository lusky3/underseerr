package app.lusk.client.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

/**
 * API service for Plex.tv authentication.
 */
interface PlexApiService {
    
    /**
     * Get a new PIN from Plex.
     */
    @Headers("Accept: application/json")
    @POST("api/v2/pins")
    suspend fun getPin(
        @Header("X-Plex-Product") product: String = "Lusk Overseerr Client",
        @Header("X-Plex-Client-Identifier") clientId: String,
        @Query("strong") strong: Boolean = true
    ): PlexPinResponse
    
    /**
     * Check PIN status.
     */
    @Headers("Accept: application/json")
    @GET("api/v2/pins/{id}")
    suspend fun checkPin(
        @Path("id") id: Int,
        @Header("X-Plex-Client-Identifier") clientId: String
    ): PlexPinResponse
}

@Serializable
data class PlexPinResponse(
    val id: Int,
    val code: String,
    @SerialName("clientIdentifier") val clientIdentifier: String? = null,
    @SerialName("authToken") val authToken: String? = null,
    @SerialName("expiresIn") val expiresIn: Int? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null
)
