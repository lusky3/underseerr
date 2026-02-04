package app.lusk.underseerr.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class JellyseerrKtorService(private val client: HttpClient) {

    suspend fun addToWatchlist(request: JellyseerrWatchlistRequest) {
        client.post("/api/v1/watchlist") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun removeFromWatchlist(tmdbId: Int) {
        client.delete("/api/v1/watchlist/$tmdbId")
    }
}

@Serializable
data class JellyseerrWatchlistRequest(
    val tmdbId: Int,
    val mediaType: String // "movie" or "tv"
)
