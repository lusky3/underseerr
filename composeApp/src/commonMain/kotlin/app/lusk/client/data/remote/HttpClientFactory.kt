package app.lusk.client.data.remote

import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.domain.security.SecurityManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    init {
        scope.launch {
            preferencesManager.getServerUrl().collectLatest { url ->
                currentBaseUrl = url ?: ""
            }
        }
        
        scope.launch {
            preferencesManager.getApiKey().collectLatest { key ->
                currentApiKey = key
            }
        }
        
        scope.launch {
            preferencesManager.getUserId().collectLatest { _ ->
                 // Reload cookie when user logs in/out
                 currentCookie = securityManager.retrieveSecureData("cookie_auth_token")
            }
        }
    }

    fun create(): HttpClient {
        return createHttpClient(
            json = json,
            baseUrlProvider = { currentBaseUrl },
            apiKeyProvider = { currentApiKey },
            cookieProvider = { currentCookie }
        )
    }
}

/**
 * Internal creation function.
 */
private fun createHttpClient(
    json: Json,
    baseUrlProvider: () -> String,
    apiKeyProvider: () -> String?,
    cookieProvider: () -> String?
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP: $message")
                }
            }
            level = LogLevel.ALL
        }

        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        // install(io.ktor.client.plugins.cookies.HttpCookies) {
        //    storage = io.ktor.client.plugins.cookies.AcceptAllCookiesStorage()
        // }
        
        defaultRequest {
            val baseUrl = baseUrlProvider()
            if (baseUrl.isNotEmpty()) {
                url(baseUrl)
            }
            
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            
            val apiKey = apiKeyProvider()
            if (!apiKey.isNullOrEmpty() && 
                apiKey != "SESSION_COOKIE" && 
                apiKey != "no_api_key" && 
                !apiKey.contains("@")
            ) {
                header("X-Api-Key", apiKey)
            } else {
                // If no API key, check for session cookie
                val cookie = cookieProvider()
                if (!cookie.isNullOrEmpty()) {
                     header("Cookie", cookie)
                }
            }
        }
    }
}
