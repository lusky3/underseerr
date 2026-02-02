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
            // Read Base URL directly from preferences (suspend call is allowed here)
            var baseUrl = try {
                 preferencesManager.getServerUrl().first() ?: ""
            } catch (e: Exception) {
                 println("HttpClient: Error reading server URL: ${e.message}")
                 ""
            }
            
            // Fallback: check configured servers list if empty
            if (baseUrl.isEmpty()) {
                val configuredServers = try {
                    preferencesManager.getConfiguredServers().first()
                } catch (e: Exception) {
                    emptyList()
                }
                
                val firstServer = configuredServers.firstOrNull()
                if (firstServer != null) {
                    baseUrl = firstServer.url
                    // Also attempt to self-heal the preference? No, just use it for now.
                    println("HttpClient: Recovered Base URL from Configured Servers: $baseUrl")
                }
            }
            
            println("HttpClient: Intercepting request to ${context.url.buildString()}, resolved baseUrl: '$baseUrl'")
            
            if (baseUrl.isNotEmpty()) {
                try {
                    val url = Url(baseUrl)
                    context.url.protocol = url.protocol
                    context.url.host = url.host
                    context.url.port = url.port
                    // println("HttpClient: Applied Base URL: ${url.protocol}://${url.host}:${url.port}")
                } catch (e: Exception) {
                    println("HttpClient: Failed to parse/apply base URL '$baseUrl': ${e.message}")
                }
            } else {
                println("HttpClient: WARNING - Base URL is empty! Request may fail to localhost.")
            }
            
            // Apply Credentials - Read fresh from Secure Storage
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
        }
        
        return client
    }
}
