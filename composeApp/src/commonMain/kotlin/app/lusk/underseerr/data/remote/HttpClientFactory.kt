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
    private var currentBaseUrl: String = ""
    private var currentApiKey: String? = null
    private var currentCookie: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    private val initialUrlLoaded = kotlinx.coroutines.CompletableDeferred<Unit>()

    init {
        scope.launch {
            preferencesManager.getServerUrl().collectLatest { url ->
                currentBaseUrl = url ?: ""
                if (!initialUrlLoaded.isCompleted) {
                    initialUrlLoaded.complete(Unit)
                }
            }
        }
        // Removed credential caching flow to prevent race conditions
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
            // Wait for initial URL to be loaded from preferences
            if (!initialUrlLoaded.isCompleted) {
                try {
                    // Timeout after 2 seconds to avoid blocking forever if something is wrong
                    kotlinx.coroutines.withTimeout(2000) {
                        initialUrlLoaded.await()
                    }
                } catch (e: Exception) {
                    // Ignore timeout, proceed with (potentially empty) URL
                }
            }

            // Apply Base URL
            var baseUrl = currentBaseUrl
            
            // Fallback: If baseUrl is empty but we should be initialized, try reading flow directly
            // This handles the race condition where 'initialUrlLoaded' is true (default?) or flow is slow
            if (baseUrl.isEmpty()) {
                val directUrl = kotlinx.coroutines.runBlocking { 
                     // Best effort to get URL if missing
                     preferencesManager.getServerUrl().first() 
                }
                if (!directUrl.isNullOrEmpty()) {
                    currentBaseUrl = directUrl
                    baseUrl = directUrl
                    println("HttpClient: Recovered Base URL from direct read: $baseUrl")
                }
            }
            
            println("HttpClient: Intercepting request to ${context.url.buildString()}, currentBaseUrl: '$baseUrl', initial loaded: ${initialUrlLoaded.isCompleted}")
            
            if (baseUrl.isNotEmpty()) {
                try {
                    val url = Url(baseUrl)
                    context.url.protocol = url.protocol
                    context.url.host = url.host
                    context.url.port = url.port
                    println("HttpClient: Applied Base URL: ${url.protocol}://${url.host}:${url.port}")
                } catch (e: Exception) {
                    println("HttpClient: Failed to parse/apply base URL '$baseUrl': ${e.message}")
                }
            } else {
                println("HttpClient: WARNING - Base URL is empty! Request may fail to localhost.")
            }
            
            // Apply Credentials - Read fresh from Secure Storage to avoid race conditions
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
