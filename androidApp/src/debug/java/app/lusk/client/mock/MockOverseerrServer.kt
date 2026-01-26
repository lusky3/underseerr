package app.lusk.client.mock

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
 * 
 * This mock server returns fictional content suitable for promotional
 * screenshots and app store listings.
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
                // ============================================================
                // Auth endpoints
                // ============================================================
                path == "/api/v1/auth/plex" && method == "POST" -> successResponse(json.encodeToString(MockResponses.userProfile()))
                path == "/api/v1/auth/me" && method == "GET" -> successResponse(json.encodeToString(MockResponses.userProfile()))
                path == "/api/v1/auth/logout" && method == "POST" -> handleLogout()
                path == "/api/v1/status" && method == "GET" -> successResponse(json.encodeToString(MockResponses.serverInfo()))
                
                // ============================================================
                // Discovery endpoints
                // ============================================================
                path.startsWith("/api/v1/discover/trending") -> handleGetTrending(request)
                path.startsWith("/api/v1/discover/movies") -> handleGetTrendingMovies(request)
                path.startsWith("/api/v1/discover/tv") -> handleGetTrendingTvShows(request)
                path.startsWith("/api/v1/search") -> handleSearch(request)
                path.startsWith("/api/v1/movie/") && !path.contains("/request") -> handleGetMovieDetails(request)
                path.startsWith("/api/v1/tv/") && !path.contains("/request") -> handleGetTvShowDetails(request)
                
                // ============================================================
                // Request endpoints
                // ============================================================
                path.startsWith("/api/v1/request") && method == "POST" -> {
                    if (path.contains("mediaId=")) {
                        // Return ApiMediaRequest for requestMovie/requestTvShow
                        successResponse(json.encodeToString(MockResponses.mediaRequest(1)))
                    } else {
                        // Return ApiRequestResponse for submitRequest(@Body)
                        successResponse(json.encodeToString(MockResponses.requestResponse()))
                    }
                }
                path == "/api/v1/request" && method == "GET" -> handleGetRequests(request)
                path.matches(Regex("/api/v1/request/\\d+")) && method == "GET" -> handleGetRequest(request)
                path.matches(Regex("/api/v1/request/\\d+")) && method == "DELETE" -> successResponse("", 204)
                path.contains("/requests") && path.contains("/user/") -> handleGetUserRequests(request)
                path.contains("/status") && path.contains("/request/") -> handleGetRequestStatus(request)
                path == "/api/v1/settings/radarr/profiles" -> successResponse(json.encodeToString(MockResponses.qualityProfiles()))
                path == "/api/v1/settings/radarr/folders" -> successResponse(json.encodeToString(MockResponses.rootFolders()))
                path == "/api/v1/settings/sonarr/profiles" -> successResponse(json.encodeToString(MockResponses.qualityProfiles()))
                path == "/api/v1/settings/sonarr/folders" -> successResponse(json.encodeToString(MockResponses.rootFolders()))
                
                // ============================================================
                // Issue endpoints
                // ============================================================
                path == "/api/v1/issue/count" && method == "GET" -> handleGetIssueCounts()
                path == "/api/v1/issue" && method == "GET" -> handleGetIssues(request)
                path == "/api/v1/issue" && method == "POST" -> handleCreateIssue(request)
                path.matches(Regex("/api/v1/issue/\\d+")) && method == "GET" -> handleGetIssue(request)
                path.matches(Regex("/api/v1/issue/\\d+")) && method == "DELETE" -> successResponse("", 204)
                path.matches(Regex("/api/v1/issue/\\d+/comment")) && method == "POST" -> handleAddComment(request)
                path.matches(Regex("/api/v1/issue/\\d+/(open|resolved)")) && method == "POST" -> handleUpdateIssueStatus(request)
                path.matches(Regex("/api/v1/issueComment/\\d+")) && method == "PUT" -> handleUpdateComment(request)
                
                // ============================================================
                // User endpoints
                // ============================================================
                path == "/api/v1/user/quota" -> successResponse(json.encodeToString(MockResponses.userQuota()))
                path == "/api/v1/user/stats" -> successResponse(json.encodeToString(MockResponses.userStatistics()))
                path == "/api/v1/user" -> successResponse(json.encodeToString(MockResponses.userProfile()))
                path.startsWith("/api/v1/user/") && !path.contains("/requests") -> {
                    val userId = extractPathId(path)
                    successResponse(json.encodeToString(MockResponses.userProfile(userId)))
                }
                
                else -> errorResponse(404, "Endpoint not found: $path")
            }
        }
        
        // ====================================================================
        // Discovery Handlers
        // ====================================================================
        
        private fun handleGetTrending(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(json.encodeToString(MockResponses.searchResults(page)))
        }
        
        private fun handleGetTrendingMovies(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(json.encodeToString(MockResponses.movieSearchResults(page)))
        }
        
        private fun handleGetTrendingTvShows(request: RecordedRequest): MockResponse {
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(json.encodeToString(MockResponses.tvShowSearchResults(page)))
        }
        
        private fun handleSearch(request: RecordedRequest): MockResponse {
            val query = extractQueryParam(request, "query") ?: ""
            val page = extractQueryParam(request, "page")?.toIntOrNull() ?: 1
            return successResponse(json.encodeToString(MockResponses.searchResults(page, query)))
        }
        
        private fun handleGetMovieDetails(request: RecordedRequest): MockResponse {
            val movieId = extractPathId(request.path!!)
            return successResponse(json.encodeToString(MockResponses.movieDetails(movieId)))
        }
        
        private fun handleGetTvShowDetails(request: RecordedRequest): MockResponse {
            val tvId = extractPathId(request.path!!)
            return successResponse(json.encodeToString(MockResponses.tvShowDetails(tvId)))
        }
        
        // ====================================================================
        // Request Handlers
        // ====================================================================
        
        private fun handleSubmitRequest(request: RecordedRequest): MockResponse {
            return successResponse(json.encodeToString(MockResponses.requestResponse()))
        }
        
        private fun handleGetRequests(request: RecordedRequest): MockResponse {
            val skip = extractQueryParam(request, "skip")?.toIntOrNull() ?: 0
            val page = (skip / 20) + 1
            return successResponse(json.encodeToString(MockResponses.requestsList(page)))
        }
        
        private fun handleGetRequest(request: RecordedRequest): MockResponse {
            val requestId = extractPathId(request.path!!)
            return successResponse(json.encodeToString(MockResponses.mediaRequest(requestId)))
        }
        
        private fun handleGetUserRequests(request: RecordedRequest): MockResponse {
            val skip = extractQueryParam(request, "skip")?.toIntOrNull() ?: 0
            val page = (skip / 20) + 1
            return successResponse(json.encodeToString(MockResponses.requestsList(page)))
        }
        
        private fun handleGetRequestStatus(request: RecordedRequest): MockResponse {
            val requestId = extractPathId(request.path!!)
            return successResponse(json.encodeToString(MockResponses.requestStatus(requestId)))
        }
        
        // ====================================================================
        // Issue Handlers
        // ====================================================================
        
        private fun handleGetIssues(request: RecordedRequest): MockResponse {
            val take = extractQueryParam(request, "take")?.toIntOrNull() ?: 20
            val skip = extractQueryParam(request, "skip")?.toIntOrNull() ?: 0
            val filter = extractQueryParam(request, "filter") ?: "all"
            return successResponse(json.encodeToString(MockResponses.issuesList(take, skip, filter)))
        }
        
        private fun handleGetIssueCounts(): MockResponse {
            return successResponse(json.encodeToString(MockResponses.issueCounts()))
        }
        
        private fun handleGetIssue(request: RecordedRequest): MockResponse {
            val issueId = extractPathId(request.path!!)
            return successResponse(json.encodeToString(MockResponses.issue(issueId)))
        }
        
        private fun handleCreateIssue(request: RecordedRequest): MockResponse {
            // Parse the request body to get issue details
            // For mock purposes, return a generic new issue
            return successResponse(json.encodeToString(
                MockResponses.createIssue(1, "Mock issue created", 1001)
            ))
        }
        
        private fun handleAddComment(request: RecordedRequest): MockResponse {
            val issueId = extractPathIdFromPattern(request.path!!, "/api/v1/issue/(\\d+)/comment")
            return successResponse(json.encodeToString(MockResponses.issue(issueId)))
        }
        
        private fun handleUpdateIssueStatus(request: RecordedRequest): MockResponse {
            val issueId = extractPathIdFromPattern(request.path!!, "/api/v1/issue/(\\d+)/")
            return successResponse(json.encodeToString(MockResponses.issue(issueId)))
        }
        
        private fun handleUpdateComment(request: RecordedRequest): MockResponse {
            val commentId = extractPathId(request.path!!)
            // Return a mock updated comment
            return successResponse(json.encodeToString(
                app.lusk.client.data.remote.model.ApiIssueComment(
                    id = commentId,
                    user = MockResponses.userProfile(1),
                    message = "Updated comment text",
                    createdAt = "2025-01-25T12:00:00.000Z",
                    updatedAt = "2025-01-25T13:00:00.000Z"
                )
            ))
        }
        
        // ====================================================================
        // Helper Methods
        // ====================================================================
        
        private fun successResponse(jsonBody: String, code: Int = 200): MockResponse {
            return MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody)
        }
        
        private fun errorResponse(code: Int, message: String): MockResponse {
            val error = mapOf("message" to message)
            return MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(json.encodeToString(error))
        }
        
        private fun handleLogout(): MockResponse {
            return MockResponse()
                .setResponseCode(204)
                .setHeader("Content-Type", "application/json")
        }
        
        private fun extractQueryParam(request: RecordedRequest, param: String): String? {
            val url = request.requestUrl ?: return null
            return url.queryParameter(param)
        }
        
        private fun extractPathId(path: String): Int {
            val pathWithoutQuery = path.split("?").first()
            return pathWithoutQuery.split("/").lastOrNull { it.isNotEmpty() && it.all { c -> c.isDigit() } }?.toIntOrNull() ?: 1
        }
        
        private fun extractPathIdFromPattern(path: String, pattern: String): Int {
            val regex = Regex(pattern)
            val match = regex.find(path)
            return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
        }
    }
}
