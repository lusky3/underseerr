package app.lusk.underseerr.navigation

import kotlinx.serialization.Serializable
import io.ktor.http.decodeURLQueryComponent

@Serializable
sealed class Screen {
    
    @Serializable
    data object Splash : Screen()
    
    @Serializable
    data class ServerConfig(val serverUrl: String? = null) : Screen()
    
    @Serializable
    data object PlexAuth : Screen()
    
    @Serializable
    data class PlexAuthCallback(val token: String) : Screen()
    
    @Serializable
    data object Home : Screen()

    @Serializable
    data object MainTabs : Screen()
    
    @Serializable
    data object Search : Screen()
    
    @Serializable
    data class Requests(val filter: String? = null) : Screen()
    
    @Serializable
    data object Issues : Screen()
    
    @Serializable
    data object Profile : Screen()
    
    @Serializable
    data class MediaDetails(val mediaType: String, val mediaId: Int, val openRequest: Boolean = false) : Screen()

    @Serializable
    data class CategoryResults(val categoryType: String, val categoryId: Int, val categoryName: String) : Screen()
    
    @Serializable
    data class RequestDetails(val requestId: Int) : Screen()
    
    @Serializable
    data class IssueDetails(val issueId: Int) : Screen()
    
    @Serializable
    data class Settings(val showPremiumPaywall: Boolean = false) : Screen()
    
    @Serializable
    data object ServerManagement : Screen()
    
    @Serializable
    data object About : Screen()

    @Serializable
    data object VibrantCustomization : Screen()

    @Serializable
    data class PersonDetails(val personId: Int) : Screen()
    
    companion object {
        fun parseDeepLink(uri: String): Screen? {
             return when {
                uri.startsWith("lusk://setup") || uri.startsWith("underseerr://setup") -> {
                    val serverUrl = uri.substringAfter("server=", "")
                        .takeIf { it.isNotEmpty() }
                        ?.let { it.decodeURLQueryComponent() }
                    ServerConfig(serverUrl)
                }
                uri.startsWith("lusk://media/") || uri.startsWith("underseerr://media/") -> {
                    val parts = uri.replace("lusk://media/", "").replace("underseerr://media/", "").split("/")
                    if (parts.size >= 2) {
                        MediaDetails(parts[0], parts[1].toIntOrNull() ?: 0)
                    } else null
                }
                uri.startsWith("lusk://request/") || uri.startsWith("underseerr://request/") -> {
                    val requestId = uri.replace("lusk://request/", "").replace("underseerr://request/", "").toIntOrNull()
                    requestId?.let { RequestDetails(it) }
                }
                uri == "lusk://request" || uri == "underseerr://request" -> {
                    Requests()
                }
                uri.startsWith("lusk://auth") || uri.startsWith("underseerr://auth") -> {
                    val token = uri.substringAfter("token=", "")
                    if (token.isNotEmpty()) {
                        PlexAuthCallback(token)
                    } else null
                }
                else -> null
            }
        }
    }
}
