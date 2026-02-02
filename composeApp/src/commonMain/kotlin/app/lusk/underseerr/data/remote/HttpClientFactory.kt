package app.lusk.underseerr.data.remote

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.security.SecurityManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger

/**
 * Factory class to create and configure Ktor [HttpClient].
 * Manages dynamic configuration updates from [PreferencesManager].
 */
class HttpClientFactory(
    private val preferencesManager: PreferencesManager,
    private val securityManager: SecurityManager
) {
    // Removed local caching to prevent synchronization issues
    // private var currentBaseUrl: String = "" 
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    fun create(): HttpClient {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                level = LogLevel.ALL
            }
    
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 15000
            }
            
            // Base URL configuration
            expectSuccess = true
            defaultRequest {
                // We don't set URL here because we can't suspend to wait for initialization
                contentType(ContentType.Application.Json)
                header("Accept", "application/json")
                header("User-Agent", "Underseerr/1.0.0 (Android)")
            }
        }

        // Intercept requests to inject headers asynchronously
        client.requestPipeline.intercept(io.ktor.client.request.HttpRequestPipeline.State) {
            // 1. Initial resolution
            var baseUrl = try {
                 preferencesManager.getServerUrl().first()?.trim() ?: ""
            } catch (e: Exception) { "" }

            // 2. Determine if this is an API request that needs a server URL
            // If the URL is currently targeting localhost, it means we're making a relative API call
            val isApiRequest = context.url.host == "localhost" || context.url.host.isEmpty()
            
            // 3. Identify if we have ANY authentication data (API key or Session Cookie)
            val existingApiKey = securityManager.retrieveSecureData("underseerr_api_key")
            val existingCookie = securityManager.retrieveSecureData("cookie_auth_token")
            val hasAuthData = (!existingApiKey.isNullOrEmpty() && existingApiKey != "no_api_key") || 
                              !existingCookie.isNullOrEmpty()

            // 4. Retry Loop: ALWAYS wait for URL if making an API request and URL is empty
            // This handles the race condition during first login where ViewModels start before DataStore propagates
            if (baseUrl.isEmpty() && isApiRequest) {
                println("HttpClient: URL missing for API request (Auth present: $hasAuthData). Starting recovery efforts...")
                
                var retries = 0
                // Always wait up to 3 seconds for API requests (first login scenario)
                val maxRetries = 30
                
                while (baseUrl.isEmpty() && retries < maxRetries) {
                    kotlinx.coroutines.delay(100)
                    
                    // A. Try primary preference again
                    baseUrl = try { preferencesManager.getServerUrl().first()?.trim() ?: "" } catch (e: Exception) { "" }
                    
                    // B. Try configured servers list as fallback
                    if (baseUrl.isEmpty()) {
                        val servers = try { preferencesManager.getConfiguredServers().first() } catch (e: Exception) { emptyList() }
                        baseUrl = servers.firstOrNull { it.isActive }?.url?.trim() ?: servers.firstOrNull()?.url?.trim() ?: ""
                    }
                    
                    // C. Also check if auth data appeared (might help with timing)
                    if (baseUrl.isEmpty() && retries % 5 == 0) {
                        val newApiKey = securityManager.retrieveSecureData("underseerr_api_key")
                        val newCookie = securityManager.retrieveSecureData("cookie_auth_token")
                        if ((!newApiKey.isNullOrEmpty() && newApiKey != "no_api_key") || !newCookie.isNullOrEmpty()) {
                            println("HttpClient: Auth data appeared at retry $retries, continuing to wait for URL...")
                        }
                    }
                    
                    if (baseUrl.isNotEmpty()) {
                        println("HttpClient: Recovered Base URL after ${retries * 100}ms: $baseUrl")
                        break
                    }
                    retries++
                }
                
                if (baseUrl.isEmpty()) {
                    println("HttpClient: Failed to recover Base URL after 3s. Request will fail.")
                }
            }
            
            // 4. Apply Base URL if found
            if (baseUrl.isNotEmpty()) {
                val currentHost = context.url.host
                val isRelative = currentHost == "localhost" || 
                                currentHost == "127.0.0.1" || 
                                currentHost == "10.0.2.2" || 
                                currentHost.isEmpty()
                
                if (isRelative) {
                    try {
                        val newBase = io.ktor.http.Url(baseUrl)
                        
                        // Apply protocol, host, and port
                        context.url.protocol = newBase.protocol
                        context.url.host = newBase.host
                        context.url.port = newBase.port
                    
                        // Handle sub-paths in the base URL (e.g. https://domain.com/overseerr)
                        val baseSegments = newBase.pathSegments.filter { it.isNotEmpty() }
                        if (baseSegments.isNotEmpty()) {
                            val originalSegments = context.url.pathSegments.filter { it.isNotEmpty() }
                            context.url.pathSegments = baseSegments + originalSegments
                        }
                        
                        // Let Ktor handle Host header automatically unless we have a reason to force it
                        context.headers.remove("Host") 
                        
                        println("HttpClient: [SUCCESS] Request targeting: ${context.url.buildString()}")
                    } catch (e: Exception) {
                        println("HttpClient: [ERROR] Failed to apply Base URL '$baseUrl': ${e.message}")
                    }
                } else {
                     println("HttpClient: [PASS] Already targeting external host: ${context.url.host}")
                }
            } else {
                println("HttpClient: [CRITICAL] No Base URL found for ${context.url.buildString()}. This request will likely fail to localhost.")
            }
            
            // 5. Apply Credentials - Read fresh from Secure Storage
            // ONLY apply Overseerr credentials to requests targeting our server
            val isOverseerrRequest = !context.url.host.contains("plex.tv") && baseUrl.isNotEmpty() && context.url.host == io.ktor.http.Url(baseUrl).host
            
            if (isOverseerrRequest) {
                val apiKey = securityManager.retrieveSecureData("underseerr_api_key")
                
                if (!apiKey.isNullOrEmpty() && 
                    apiKey != "SESSION_COOKIE" && 
                    apiKey != "no_api_key" && 
                    !apiKey.contains("@")
                ) {
                    context.headers["X-Api-Key"] = apiKey
                } else {
                    // If no API key, check for session cookie from SecurityManager
                    val cookie = securityManager.retrieveSecureData("cookie_auth_token")
                    if (!cookie.isNullOrEmpty()) {
                         context.headers["Cookie"] = cookie
                    }
                }
            } else {
                // For non-Overseerr requests (like Plex), do NOT send our credentials
                // This prevents bleeding API keys or Overseerr cookies to external sites
            }
        }
        
        return client
    }
}
