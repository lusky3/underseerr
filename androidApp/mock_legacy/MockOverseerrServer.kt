package app.lusk.underseerr.mock

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

/**
 * Mock Overseerr server for testing using MockWebServer.
 * Provides realistic API responses for all Overseerr endpoints.
 */
class MockOverseerrServer {
    private val server = MockWebServer()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    val baseUrl: String
        get() = server.url("/").toString()
    
    val port: Int
        get() = server.port
    
    /**
     * Start the mock server with default responses.
     */
    fun start() {
        server.dispatcher = OverseerrDispatcher()
        server.start()
    }
    
    /**
     * Start the mock server on a specific port.
     */
    fun start(port: Int) {
        server.dispatcher = OverseerrDispatcher()
        server.start(port)
    }
    
    /**
     * Shutdown the mock server.
     */
    fun shutdown() {
        server.shutdown()
    }
    
    /**
     * Enqueue a custom response.
     */
    fun enqueue(response: MockResponse) {
        server.enqueue(response)
    }
    
    /**
     * Get the last recorded request.
     */
    fun takeRequest(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS): RecordedRequest? {
        return server.takeRequest(timeout, unit)
    }
    
    /**
     * Set a custom dispatcher for advanced scenarios.
     */
    fun setDispatcher(dispatcher: Dispatcher) {
        server.dispatcher = dispatcher
    }
    
    /**
     * Dispatcher that handles all Overseerr API endpoints.
     */
    private inner class OverseerrDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path ?: return errorResponse(404, "Not Found")
            val method = request.method ?: "GET"
            
            return when {
                // Auth endpoints
                path == "/api/v1/auth/plex" && method == "POST" -> handlePlexAuth(request)
                path == "/api/v1/auth/me" && method == "GET" -> handleGetCurrentUser()
                path == "/api/v1/auth/logout" && method == "POST" -> handleLogout()
                path == "/api/v1/status" && method == "GET" -> handleServerInfo()
                
                // Discovery endpoints
                path.startsWith("/api/v1/discover/trending") -> handleGetTrending(request)
                path.startsWith("/api/v1/discover/movies") -> handleGetTrendingMovies(request)
                path.startsWith("/api/v1/discover/tv") -> handleGetTrendingTvShows(request)
                path.startsWith("/api/v1/search") -> handleSearch(request)
                path.matches(Regex("/api/v1/movie/\\d+")) -> handleGetMovieDetails(request)
                path.matches(Regex("/api/v1/tv/\\d+")) -> handleGetTvShowDetails(request)
                
                // Request endpoints
                path == "/api/v1/request" && method == "POST" -> handleSubmitRequest(request)
                path == "/api/v1/request" && method == "GET" -> handleGetRequests(request)
                path.matches(Regex("/api/v1/request/\\d+")) && method == "GET" -> handleGetRequest(request)
                path.matches(Regex("/api/v1/user/\\d+/requests")) -> handleGetUserRequests(request)
                path.matches(Regex("/api/v1/request/\\d+/status")) -> handleGetRequestStatus(request)
                path == "/api/v1/settings/radarr/profiles" -> handleGetQualityProfiles()
                path == "/api/v1/settings/radarr/folders" -> handleGetRootFolders()
                
                // User endpoints
                path.matches(Regex("/api/v1/user/\\d+")) -> handleGetUserProfile(request)
                path == "/api/v1/user" -> handleGetCurrentUser()
                path == "/api/v1/user/quota" -> handleGetUserQuota()
                path == "/api/v1/user/stats" -> handleGetUserStatistics()
                
                else -> errorResponse(404, "Endpoint not found: $path")
            }
        }
        
        private fun handlePlexAuth(request: RecordedRequest): MockResponse {
            return successResponse(MockResponses.authResponse())
        }
        
        private fun handleGetCurrentUser(): MockResponse {
            return successResponse(MockResponses.userProfile())
        }
        
        private fun handleLogout(): MockResponse {
            return MockResponse()
                .setResponseCode(204)
                .setHeader("Content-Type", "application/json")
        }
        
        private fun handleServerInfo(): MockResponse {
            return successResponse(MockResponses.serverInfo())
        }
        
        private fun handleGetTrending(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(MockResponses.searchResults(page))
        }
        
        private fun handleGetTrendingMovies(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(MockResponses.movieSearchResults(page))
        }
        
        private fun handleGetTrendingTvShows(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(MockResponses.tvShowSearchResults(page))
        }
        
        private fun handleSearch(request: RecordedRequest): MockResponse {
            val query = extractQueryParam(request, "query") ?: ""
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(MockResponses.searchResults(page, query))
        }
        
        private fun handleGetMovieDetails(request: RecordedRequest): MockResponse {
            val movieId = extractPathId(request.path!!)
            return successResponse(MockResponses.movieDetails(movieId))
        }
        
        private fun handleGetTvShowDetails(request: RecordedRequest): MockResponse {
            val tvId = extractPathId(request.path!!)
            return successResponse(MockResponses.tvShowDetails(tvId))
        }
        
        private fun handleSubmitRequest(request: RecordedRequest): MockResponse {
            return successResponse(MockResponses.requestResponse())
        }
        
        private fun handleGetRequests(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "skip")?.toIntOrNull()?.div(20) ?: 0
            return successResponse(MockResponses.requestsList(page + 1))
        }
        
        private fun handleGetRequest(request: RecordedRequest): MockResponse {
            val requestId = extractPathId(request.path!!)
            return successResponse(MockResponses.mediaRequest(requestId))
        }
        
        private fun handleGetUserRequests(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "skip")?.toIntOrNull()?.div(20) ?: 0
            return successResponse(MockResponses.requestsList(page + 1))
        }
        
        private fun handleGetRequestStatus(request: RecordedRequest): MockResponse {
            val requestId = extractPathId(request.path!!)
            return successResponse(MockResponses.requestStatus(requestId))
        }
        
        private fun handleGetQualityProfiles(): MockResponse {
            return successResponse(MockResponses.qualityProfiles())
        }
        
        private fun handleGetRootFolders(): MockResponse {
            return successResponse(MockResponses.rootFolders())
        }
        
        private fun handleGetUserProfile(request: RecordedRequest): MockResponse {
            return successResponse(MockResponses.userProfile())
        }
        
        private fun handleGetUserQuota(): MockResponse {
            return successResponse(MockResponses.userQuota())
        }
        
        private fun handleGetUserStatistics(): MockResponse {
            return successResponse(MockResponses.userStatistics())
        }
        
        private fun successResponse(body: Any): MockResponse {
            return MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json.encodeToString(body))
        }
        
        private fun errorResponse(code: Int, message: String): MockResponse {
            val error = mapOf("message" to message)
            return MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(json.encodeToString(error))
        }
        
        private fun extractQueryParam(request: RecordedRequest, param: String): String? {
            val url = request.requestUrl ?: return null
            return url.queryParameter(param)
        }
        
        private fun extractPathId(path: String): Int {
            return path.split("/").lastOrNull()?.toIntOrNull() ?: 1
        }
    }
}
