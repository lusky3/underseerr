package app.lusk.underseerr.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class RegisterTokenRequest(
    val email: String,
    val token: String
)

class NotificationServerService(private val client: HttpClient) {

    /**
     * Registers the FCM token with the Cloudflare Worker (or other backend).
     */
    suspend fun registerToken(serverUrl: String, email: String, token: String) {
        // Construct the full URL: serverUrl + /register
        // Ensure serverUrl doesn't end with slash to avoid double slash
        val cleanUrl = serverUrl.trimEnd('/')
        val url = "$cleanUrl/register"

        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(RegisterTokenRequest(email, token))
        }
    }
}
