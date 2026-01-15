package app.lusk.client.data.remote.api

import app.lusk.client.data.remote.model.ApiAuthResponse
import app.lusk.client.data.remote.model.ApiServerInfo
import app.lusk.client.data.remote.model.ApiUserProfile
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ktor implementation of authentication endpoints.
 */
class AuthKtorService(private val client: HttpClient) {
    
    suspend fun authenticateWithPlex(authToken: String): io.ktor.client.statement.HttpResponse {
        return client.post("/api/v1/auth/plex") {
            setBody(PlexAuthRequest(authToken))
        }
    }
    
    suspend fun getCurrentUser(): ApiUserProfile {
        return client.get("/api/v1/auth/me").body()
    }
    
    suspend fun logout() {
        client.post("/api/v1/auth/logout")
    }
    
    suspend fun getServerInfo(): ApiServerInfo {
        return client.get("/api/v1/status").body()
    }
}

@Serializable
data class PlexAuthRequest(
    @SerialName("authToken")
    val authToken: String
)
