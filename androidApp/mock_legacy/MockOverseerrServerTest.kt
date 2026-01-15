package app.lusk.client.mock

import app.lusk.client.data.remote.api.AuthApiService
import app.lusk.client.data.remote.api.DiscoveryApiService
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.data.remote.api.UserApiService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest

/**
 * Example test demonstrating how to use MockOverseerrServer for testing API calls.
 */
class MockOverseerrServerTest : FunSpec({
    lateinit var mockServer: MockOverseerrServer
    
    beforeTest {
        mockServer = MockOverseerrServer()
        mockServer.start()
    }
    
    afterTest {
        mockServer.shutdown()
    }
    
    context("Auth API") {
        test("should authenticate with Plex") {
            runTest {
                val authApi = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
                
                val response = authApi.authenticateWithPlex(
                    app.lusk.client.data.remote.model.PlexAuthRequest(
                        authToken = "test-token"
                    )
                )
                
                response.apiKey shouldBe "test-api-key-12345"
                response.userId shouldBe 1
            }
        }
        
        test("should get current user") {
            runTest {
                val authApi = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
                
                val user = authApi.getCurrentUser()
                
                user.id shouldBe 1
                user.email shouldContain "@example.com"
                user.displayName shouldNotBe null
            }
        }
        
        test("should get server info") {
            runTest {
                val authApi = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
                
                val serverInfo = authApi.getServerInfo()
                
                serverInfo.version shouldBe "1.33.2"
                serverInfo.initialized shouldBe true
            }
        }
    }
    
    context("Discovery API") {
        test("should get trending media") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val results = discoveryApi.getTrending(page = 1)
                
                results.page shouldBe 1
                results.results.size shouldBe 20
                results.totalPages shouldBe 10
            }
        }
        
        test("should get trending movies") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val results = discoveryApi.getTrendingMovies(page = 1)
                
                results.page shouldBe 1
                results.results.size shouldBe 20
                results.results.all { it.mediaType == "movie" } shouldBe true
            }
        }
        
        test("should search for media") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val results = discoveryApi.search(query = "test", page = 1)
                
                results.page shouldBe 1
                results.results.size shouldBe 20
            }
        }
        
        test("should get movie details") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val movie = discoveryApi.getMovieDetails(movieId = 123)
                
                movie.id shouldBe 123
                movie.title shouldContain "Movie"
                movie.overview shouldNotBe null
            }
        }
        
        test("should get TV show details") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val tvShow = discoveryApi.getTvShowDetails(tvId = 456)
                
                tvShow.id shouldBe 456
                tvShow.name shouldContain "TV Show"
                tvShow.numberOfSeasons shouldBe 3
            }
        }
    }
    
    context("Request API") {
        test("should submit a request") {
            runTest {
                val requestApi = MockServerTestHelper.createApiService<RequestApiService>(mockServer)
                
                val response = requestApi.submitRequest(
                    app.lusk.client.data.remote.model.ApiRequestBody(
                        mediaId = 123,
                        mediaType = "movie"
                    )
                )
                
                response.id shouldNotBe null
                response.status shouldBe 1
            }
        }
        
        test("should get requests list") {
            runTest {
                val requestApi = MockServerTestHelper.createApiService<RequestApiService>(mockServer)
                
                val requests = requestApi.getRequests(take = 20, skip = 0)
                
                requests.results.size shouldBe 20
            }
        }
        
        test("should get request details") {
            runTest {
                val requestApi = MockServerTestHelper.createApiService<RequestApiService>(mockServer)
                
                val request = requestApi.getRequest(requestId = 1)
                
                request.id shouldBe 1
                request.title shouldNotBe null
            }
        }
        
        test("should get quality profiles") {
            runTest {
                val requestApi = MockServerTestHelper.createApiService<RequestApiService>(mockServer)
                
                val profiles = requestApi.getQualityProfiles()
                
                profiles.size shouldBe 4
                profiles.any { it.name == "HD-1080p" } shouldBe true
            }
        }
        
        test("should get root folders") {
            runTest {
                val requestApi = MockServerTestHelper.createApiService<RequestApiService>(mockServer)
                
                val folders = requestApi.getRootFolders()
                
                folders.size shouldBe 4
                folders.any { it.path == "/movies" } shouldBe true
            }
        }
    }
    
    context("User API") {
        test("should get user profile") {
            runTest {
                val userApi = MockServerTestHelper.createApiService<UserApiService>(mockServer)
                
                val user = userApi.getUserProfile(userId = 1)
                
                user.id shouldBe 1
                user.email shouldNotBe null
                user.permissions shouldNotBe null
            }
        }
        
        test("should get user quota") {
            runTest {
                val userApi = MockServerTestHelper.createApiService<UserApiService>(mockServer)
                
                val quota = userApi.getUserQuota()
                
                quota.movie shouldNotBe null
                quota.tv shouldNotBe null
                quota.movie?.limit shouldBe 10
            }
        }
        
        test("should get user statistics") {
            runTest {
                val userApi = MockServerTestHelper.createApiService<UserApiService>(mockServer)
                
                val stats = userApi.getUserStatistics()
                
                stats.totalRequests shouldBe 25
                stats.approvedRequests shouldBe 15
            }
        }
    }
    
    context("Pagination") {
        test("should handle multiple pages") {
            runTest {
                val discoveryApi = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
                
                val page1 = discoveryApi.getTrending(page = 1)
                val page2 = discoveryApi.getTrending(page = 2)
                
                page1.page shouldBe 1
                page2.page shouldBe 2
                page1.results[0].id shouldNotBe page2.results[0].id
            }
        }
    }
})
