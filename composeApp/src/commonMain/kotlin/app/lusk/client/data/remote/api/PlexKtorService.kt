package app.lusk.client.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ktor implementation for Plex.tv authentication.
 */
class PlexKtorService(private val client: HttpClient) {
    
    private val plexBaseUrl = "https://plex.tv"

    suspend fun getPin(product: String = "Lusk Overseerr Client", clientId: String): PlexPinResponse {
        return client.post("$plexBaseUrl/api/v2/pins") {
            header("X-Plex-Product", product)
            header("X-Plex-Client-Identifier", clientId)
            parameter("strong", true)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
        }.body()
    }
    
    suspend fun checkPin(id: Int, clientId: String): PlexPinResponse {
        return client.get("$plexBaseUrl/api/v2/pins/$id") {
            header("X-Plex-Client-Identifier", clientId)
            header("Accept", "application/json")
        }.body()
    }
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
