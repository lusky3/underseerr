package app.lusk.client.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 * KMP Compatible.
 */
sealed class Screen(val route: String) {
    
    // Initialization
    data object Splash : Screen("splash")
    
    // Authentication
    data object ServerConfig : Screen("server_config")
    data object PlexAuth : Screen("plex_auth")
    data object PlexAuthCallback : Screen("plex_auth_callback/{token}") {
        fun createRoute(token: String) = "plex_auth_callback/$token"
    }
    
    // Main Navigation
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Requests : Screen("requests")
    data object Issues : Screen("issues")
    data object Profile : Screen("profile")
    
    // Details
    data object MediaDetails : Screen("media_details/{mediaType}/{mediaId}?openRequest={openRequest}") {
        fun createRoute(mediaType: String, mediaId: Int, openRequest: Boolean = false) = 
            "media_details/$mediaType/$mediaId?openRequest=$openRequest"
    }

    data object CategoryResults : Screen("category_results/{categoryType}/{categoryId}/{categoryName}") {
        fun createRoute(type: String, id: Int, name: String) = 
            "category_results/$type/$id/$name"
    }
    
    data object RequestDetails : Screen("request_details/{requestId}") {
        fun createRoute(requestId: Int) = "request_details/$requestId"
    }
    
    data object IssueDetails : Screen("issue_details/{issueId}") {
        fun createRoute(issueId: Int) = "issue_details/$issueId"
    }
    
    // Settings
    data object Settings : Screen("settings")
    data object ServerManagement : Screen("server_management")
    
    companion object {
        /**
         * Get screen from route string.
         */
        fun fromRoute(route: String?): Screen? {
            return when {
                route == null -> null
                route.startsWith("server_config") -> ServerConfig
                route.startsWith("plex_auth") -> PlexAuth
                route.startsWith("home") -> Home
                route.startsWith("search") -> Search
                route.startsWith("requests") -> Requests
                route.startsWith("issues") -> Issues
                route.startsWith("profile") -> Profile
                route.startsWith("media_details") -> MediaDetails
                route.startsWith("category_results") -> CategoryResults
                route.startsWith("request_details") -> RequestDetails
                route.startsWith("issue_details") -> IssueDetails
                route.startsWith("settings") -> Settings
                route.startsWith("server_management") -> ServerManagement
                else -> null
            }
        }
        
        /**
         * Parse deep link URI to screen route.
         */
        fun parseDeepLink(uri: String): String? {
            return when {
                uri.startsWith("lusk://media/") -> {
                    val parts = uri.removePrefix("lusk://media/").split("/")
                    if (parts.size == 2) {
                        MediaDetails.createRoute(parts[0], parts[1].toIntOrNull() ?: 0)
                    } else null
                }
                uri.startsWith("lusk://request/") -> {
                    val requestId = uri.removePrefix("lusk://request/").toIntOrNull()
                    requestId?.let { RequestDetails.createRoute(it) }
                }
                uri.startsWith("lusk://auth") -> {
                    // Extract token from query parameter: lusk://auth?token=XYZ
                    val token = uri.substringAfter("token=", "")
                    if (token.isNotEmpty()) {
                        "plex_auth_callback/$token"
                    } else null
                }
                else -> null
            }
        }
    }
}
