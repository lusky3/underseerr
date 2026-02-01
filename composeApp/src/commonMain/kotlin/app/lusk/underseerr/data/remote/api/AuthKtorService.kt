package app.lusk.underseerr.data.remote.api

import app.lusk.underseerr.data.remote.model.ApiAuthResponse
import app.lusk.underseerr.data.remote.model.ApiServerInfo
import app.lusk.underseerr.data.remote.model.ApiUserProfile
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

    suspend fun loginLocal(username: String, password: String): io.ktor.client.statement.HttpResponse {
        return client.post("/api/v1/auth/local") {
            setBody(LocalAuthRequest(username, password))
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

@Serializable
data class LocalAuthRequest(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String
)
