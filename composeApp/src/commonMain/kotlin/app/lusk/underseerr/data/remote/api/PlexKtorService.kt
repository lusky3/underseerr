package app.lusk.underseerr.data.remote.api

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

    suspend fun getPin(product: String = "Underseerr", clientId: String): PlexPinResponse {
        return client.post("$plexBaseUrl/api/v2/pins") {
            header("X-Plex-Product", product)
            header("X-Plex-Client-Identifier", clientId)
            header("X-Plex-Device", "iPhone")
            header("X-Plex-Platform", "iOS")
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

    suspend fun getWatchlist(plexToken: String, page: Int = 1): PlexWatchlistResponse {
        return client.get("https://discover.provider.plex.tv/library/sections/watchlist/all") {
            header("X-Plex-Token", plexToken)
            header("Accept", "application/json")
            parameter("X-Plex-Container-Start", (page - 1) * 20)
            parameter("X-Plex-Container-Size", 20)
            parameter("includeGuids", 1)
        }.body()
    }

    suspend fun removeFromWatchlist(plexToken: String, ratingKey: String) {
        client.delete("https://discover.provider.plex.tv/library/sections/watchlist/all") {
            header("X-Plex-Token", plexToken)
            parameter("ratingKey", ratingKey)
        }
    }

    /**
     * Add to watchlist.
     * Use PUT for Plex Discover watchlist.
     */
    suspend fun addToWatchlist(plexToken: String, ratingKey: String) {
        client.put("https://discover.provider.plex.tv/library/sections/watchlist/all") {
            header("X-Plex-Token", plexToken)
            parameter("ratingKey", ratingKey)
        }
    }

    /**
     * Find a Plex item by its TMDB ID.
     */
    suspend fun searchDiscover(plexToken: String, query: String, mediaType: String): PlexWatchlistResponse {
        val searchType = if (mediaType == "movie") "movies" else "tv"
        
        return client.get("https://discover.provider.plex.tv/library/search") {
            header("X-Plex-Token", plexToken)
            header("Accept", "application/json")
            parameter("query", query)
            parameter("limit", 5)
            parameter("searchTypes", searchType)
            parameter("searchProviders", "discover")
        }.body()
    }
}

@Serializable
data class PlexWatchlistResponse(
    @SerialName("MediaContainer") val mediaContainer: PlexMediaContainer
)

@Serializable
data class PlexMediaContainer(
    val size: Int,
    val totalSize: Int? = null,
    val offset: Int? = null,
    @SerialName("Metadata") val metadata: List<PlexMetadata> = emptyList(),
    @SerialName("SearchResults") val searchResults: List<PlexSearchResults> = emptyList()
)

@Serializable
data class PlexSearchResults(
    @SerialName("SearchResult") val searchResult: List<PlexSearchResult> = emptyList()
)

@Serializable
data class PlexSearchResult(
    @SerialName("Metadata") val metadata: PlexMetadata
)

@Serializable
data class PlexMetadata(
    val ratingKey: String,
    val guid: String,
    val type: String, // movie or show
    val title: String,
    val summary: String? = null,
    val thumb: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val leafCount: Int? = null,
    val tmdbId: String? = null,
    val imdbId: String? = null,
    @SerialName("Guid") val externalGuids: List<PlexGuid> = emptyList()
)

@Serializable
data class PlexGuid(
    val id: String
)

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
