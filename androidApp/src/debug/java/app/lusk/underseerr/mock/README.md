# Mock Overseerr Server for Testing

This package provides a complete mock implementation of the Overseerr API server for testing purposes. It uses OkHttp's MockWebServer to simulate realistic API responses without requiring a real Overseerr instance.

## Features

- ✅ Complete API coverage for all Overseerr endpoints
- ✅ Realistic mock data with proper serialization
- ✅ Pagination support
- ✅ Configurable responses
- ✅ Request recording for verification
- ✅ Easy integration with existing tests

## Components

### 1. MockOverseerrServer

The main server class that handles all API endpoints.

```kotlin
val mockServer = MockOverseerrServer()
mockServer.start()

// Use the server
val baseUrl = mockServer.baseUrl

// Cleanup
mockServer.shutdown()
```

### 2. MockResponses

Provides realistic mock data for all API responses.

```kotlin
// Get mock user profile
val user = MockResponses.userProfile(userId = 1)

// Get mock search results
val results = MockResponses.searchResults(page = 1, query = "test")

// Get mock movie details
val movie = MockResponses.movieDetails(movieId = 123)
```

### 3. MockServerTestHelper

Utilities for creating Retrofit instances and API services configured to use the mock server.

```kotlin
// Create a Retrofit instance
val retrofit = MockServerTestHelper.createRetrofit(mockServer.baseUrl)

// Create an API service
val authApi = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
```

### 4. MockServerTest

Base class for tests that automatically manages server lifecycle.

```kotlin
class MyApiTest : MockServerTest() {
    @BeforeEach
    fun setup() {
        setupMockServer()
    }
    
    @AfterEach
    fun tearDown() {
        tearDownMockServer()
    }
    
    @Test
    fun testApi() {
        val api = createApiService<AuthApiService>()
        // Test your API calls
    }
}
```

## Supported Endpoints

### Authentication

- `POST /api/v1/auth/plex` - Authenticate with Plex
- `GET /api/v1/auth/me` - Get current user
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/status` - Get server info

### Discovery

- `GET /api/v1/discover/trending` - Get trending media
- `GET /api/v1/discover/movies` - Get trending movies
- `GET /api/v1/discover/tv` - Get trending TV shows
- `GET /api/v1/search` - Search for media
- `GET /api/v1/movie/{id}` - Get movie details
- `GET /api/v1/tv/{id}` - Get TV show details

### Requests

- `POST /api/v1/request` - Submit a request
- `GET /api/v1/request` - Get requests list
- `GET /api/v1/request/{id}` - Get request details
- `GET /api/v1/user/{id}/requests` - Get user requests
- `GET /api/v1/request/{id}/status` - Get request status
- `GET /api/v1/settings/radarr/profiles` - Get quality profiles
- `GET /api/v1/settings/radarr/folders` - Get root folders

### User

- `GET /api/v1/user/{id}` - Get user profile
- `GET /api/v1/user` - Get current user
- `GET /api/v1/user/quota` - Get user quota
- `GET /api/v1/user/stats` - Get user statistics

### Issues

- `GET /api/v1/issue` - Get issues list with filtering
- `GET /api/v1/issue/count` - Get issue counts by type/status
- `GET /api/v1/issue/{id}` - Get issue details
- `POST /api/v1/issue` - Create a new issue
- `DELETE /api/v1/issue/{id}` - Delete an issue
- `POST /api/v1/issue/{id}/comment` - Add comment to issue
- `POST /api/v1/issue/{id}/open` - Reopen an issue
- `POST /api/v1/issue/{id}/resolved` - Mark issue as resolved
- `PUT /api/v1/issueComment/{id}` - Update a comment

## Usage Examples

### Basic Test

```kotlin
@Test
fun testAuthentication() = runTest {
    val mockServer = MockOverseerrServer()
    mockServer.start()
    
    val authApi = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
    
    val response = authApi.authenticateWithPlex(
        PlexAuthRequest(authToken = "test-token")
    )
    
    assertEquals("test-api-key-12345", response.apiKey)
    assertEquals(1, response.userId)
    
    mockServer.shutdown()
}
```

### Testing with Kotest

```kotlin
class MyApiTest : FunSpec({
    lateinit var mockServer: MockOverseerrServer
    
    beforeTest {
        mockServer = MockOverseerrServer()
        mockServer.start()
    }
    
    afterTest {
        mockServer.shutdown()
    }
    
    test("should get trending movies") {
        runTest {
            val api = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
            
            val results = api.getTrendingMovies(page = 1)
            
            results.page shouldBe 1
            results.results.size shouldBe 20
        }
    }
})
```

### Custom Responses

```kotlin
@Test
fun testCustomResponse() {
    val mockServer = MockOverseerrServer()
    mockServer.start()
    
    // Enqueue a custom response
    mockServer.enqueue(
        MockResponse()
            .setResponseCode(500)
            .setBody("""{"message": "Server error"}""")
    )
    
    // Your test code that expects an error
    
    mockServer.shutdown()
}
```

### Verifying Requests

```kotlin
@Test
fun testRequestWasMade() = runTest {
    val mockServer = MockOverseerrServer()
    mockServer.start()
    
    val api = MockServerTestHelper.createApiService<AuthApiService>(mockServer)
    api.getServerInfo()
    
    // Verify the request
    val request = mockServer.takeRequest()
    assertEquals("/api/v1/status", request.path)
    assertEquals("GET", request.method)
    
    mockServer.shutdown()
}
```

### Testing Pagination

```kotlin
@Test
fun testPagination() = runTest {
    val mockServer = MockOverseerrServer()
    mockServer.start()
    
    val api = MockServerTestHelper.createApiService<DiscoveryApiService>(mockServer)
    
    val page1 = api.getTrending(page = 1)
    val page2 = api.getTrending(page = 2)
    
    assertEquals(1, page1.page)
    assertEquals(2, page2.page)
    assertNotEquals(page1.results[0].id, page2.results[0].id)
    
    mockServer.shutdown()
}
```

### Integration with Repository Tests

```kotlin
class MediaRepositoryTest : FunSpec({
    lateinit var mockServer: MockOverseerrServer
    lateinit var repository: MediaRepository
    
    beforeTest {
        mockServer = MockOverseerrServer()
        mockServer.start()
        
        val retrofit = MockServerTestHelper.createRetrofit(mockServer.baseUrl)
        val api = retrofit.create(DiscoveryApiService::class.java)
        
        repository = MediaRepositoryImpl(api)
    }
    
    afterTest {
        mockServer.shutdown()
    }
    
    test("repository should fetch trending movies") {
        runTest {
            val result = repository.getTrendingMovies(page = 1)
            
            result.isSuccess shouldBe true
            result.getOrNull()?.size shouldBe 20
        }
    }
})
```

## Mock Data Characteristics

### User Data

- User IDs: Sequential (1, 2, 3, ...)
- Emails: `user{id}@example.com`
- Display Names: `User {id}`
- Avatars: `/avatar/{id}.jpg`
- Permissions: User 1 is admin, others are regular users

### Media Data

- Movie IDs: Even numbers
- TV Show IDs: Odd numbers
- Titles: `Movie {id}` or `TV Show {id}`
- Posters: `/poster/{type}_{id}.jpg`
- Vote Averages: 6.5 - 9.0 range

### Request Data

- Request IDs: Sequential
- Status: Rotates through Pending (1), Approved (2), Declined (3), Available (5)
- Created dates: Distributed across January 2024

### Pagination

- Page size: 20 items per page
- Total pages: 10
- Total results: 200

## Advanced Usage

### Custom Dispatcher

For complex test scenarios, you can provide a custom dispatcher:

```kotlin
val mockServer = MockOverseerrServer()
mockServer.setDispatcher(object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.path) {
            "/api/v1/custom" -> MockResponse()
                .setResponseCode(200)
                .setBody("""{"custom": "response"}""")
            else -> MockResponse().setResponseCode(404)
        }
    }
})
mockServer.start()
```

### Simulating Network Delays

```kotlin
mockServer.enqueue(
    MockResponse()
        .setResponseCode(200)
        .setBody("""{"data": "value"}""")
        .setBodyDelay(2, TimeUnit.SECONDS)
)
```

### Simulating Errors

```kotlin
// 404 Not Found
mockServer.enqueue(
    MockResponse()
        .setResponseCode(404)
        .setBody("""{"message": "Not found"}""")
)

// 500 Server Error
mockServer.enqueue(
    MockResponse()
        .setResponseCode(500)
        .setBody("""{"message": "Internal server error"}""")
)

// Network timeout
mockServer.enqueue(
    MockResponse()
        .setSocketPolicy(SocketPolicy.NO_RESPONSE)
)
```

## Running the Tests

```bash
# Run all mock server tests
./gradlew test --tests "*MockOverseerrServerTest"

# Run with verbose output
./gradlew test --tests "*MockOverseerrServerTest" --info
```

## Benefits

1. **No External Dependencies**: Tests run without requiring a real Overseerr server
2. **Fast Execution**: Mock responses are instant
3. **Deterministic**: Same inputs always produce same outputs
4. **Offline Testing**: Works without internet connection
5. **Easy Debugging**: Full control over responses and timing
6. **CI/CD Friendly**: No need to set up test infrastructure

## Best Practices

1. **Always cleanup**: Call `mockServer.shutdown()` in `@AfterEach` or `afterTest`
2. **Use realistic data**: The mock responses are designed to be realistic
3. **Test error cases**: Use custom responses to test error handling
4. **Verify requests**: Use `takeRequest()` to verify your code makes correct API calls
5. **Isolate tests**: Each test should start with a fresh server instance

## Troubleshooting

### Port Already in Use

```kotlin
// Let the system assign a random port
mockServer.start() // Uses random available port

// Or specify a port
mockServer.start(8080)
```

### Timeout Issues

```kotlin
// Increase timeout in OkHttpClient
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()
```

### Serialization Errors

Ensure your models match the mock response structure. Check the `MockResponses` object for the exact format.

## Contributing

When adding new endpoints:

1. Add the endpoint handler in `MockOverseerrServer.OverseerrDispatcher`
2. Add mock data in `MockResponses`
3. Add tests in `MockOverseerrServerTest`
4. Update this README

## See Also

- [MockWebServer Documentation](https://github.com/square/okhttp/tree/master/mockwebserver)
- [Overseerr API Documentation](https://api-docs.overseerr.dev/)
- [Kotest Documentation](https://kotest.io/)

---

## Promotional Screenshots Mode

The mock system also provides **fictional content** for promotional screenshots and app store listings. All titles are original and copyright-free.

### Fictional Movies (IDs 1001-1012)

| ID | Title | Genre | Status |
|----|-------|-------|--------|
| 1001 | Neon Horizons | Sci-Fi, AI consciousness | Available |
| 1002 | The Last Cartographer | Adventure, parallel dimensions | Available |
| 1003 | Midnight at the Velvet Club | 1940s Noir Thriller | Pending |
| 1004 | Echoes of Tomorrow | Post-apocalyptic Drama | Available |
| 1005 | The Quantum Thief | Heist, Sci-Fi | Pending |
| 1006 | Garden of Stars | Space botanist drama | Requested |
| 1007 | The Crimson Protocol | Cyber thriller | Available |
| 1008 | Whispers in the Aurora | Iceland Sci-Fi | Available |
| 1009 | The Memory Merchant | Memory trading drama | Pending |
| 1010 | Titanfall Legacy | Mech action | Requested |
| 1011 | Beneath Copper Skies | Mars detective story | Available |
| 1012 | The Infinite Library | Fantasy mystery | Pending |

### Fictional TV Shows (IDs 2001-2010)

| ID | Title | Genre | Seasons | Status |
|----|-------|-------|---------|--------|
| 2001 | Chronicles of the Void | Space Opera | 3 | Available |
| 2002 | The Alchemist's Daughter | Victorian Fantasy | 2 | Available |
| 2003 | Harbor City | Crime Drama | 4 | Available |
| 2004 | The Dreamweavers | Psychological Thriller | 1 | Pending |
| 2005 | Stellar Academy | Sci-Fi Academy | 2 | Available |
| 2006 | The Cipher | Historical Conspiracy | 1 | Available |
| 2007 | Frostbound | Survival Drama | 2 | Pending |
| 2008 | The Syndicate Files | Tech Thriller | 1 | Requested |
| 2009 | Legends of Ashenvale | Epic Fantasy | 3 | Available |
| 2010 | Pulse | Medical Drama | 2 | Available |

### Fake Users

| ID | Name | Role |
|----|------|------|
| 1 | Admin User | Admin |
| 2 | Sarah Chen | User |
| 3 | Marcus Johnson | User |
| 4 | Emily Rodriguez | User |
| 5 | James Wilson | User |
| 6 | Olivia Park | User |

### Sample Issues

8 pre-configured issues with various types (Video, Audio, Subtitles, Other) and statuses (Open, Resolved) with realistic comment threads.

### Additional Files

| File | Purpose |
|------|---------|
| `MockImageUrls.kt` | Maps media IDs to local asset paths |
| `MockImageInterceptor.kt` | Coil interceptor that redirects TMDB URLs to local assets |
| `IMAGE_PROMPTS.md` | AI image generation prompts for all titles |

### Mock Image Assets

Generated poster and backdrop images are stored in:

```
androidApp/src/debug/assets/mock_images/
├── posters/       # 500x750 (2:3 ratio) JPEG images
│   ├── poster_neon_horizons.jpg
│   ├── poster_last_cartographer.jpg
│   └── ... (17 total)
└── backdrops/     # 1280x720 (16:9 ratio) JPEG images
    ├── poster_neon_horizons.jpg
    ├── poster_last_cartographer.jpg
    └── ... (17 total)
```

**Content Coverage:**

- 12 Movie posters (IDs 1001-1012)
- 5 TV Show posters (IDs 2001-2005)
- Falls back to colorful placehold.co images for unmapped IDs

The `MockImageInterceptor` intercepts TMDB image URLs and redirects them to:

1. Local assets via `file:///android_asset/` if available
2. Placeholder service URLs as fallback
