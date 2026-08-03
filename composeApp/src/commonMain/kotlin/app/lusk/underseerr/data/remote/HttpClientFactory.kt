package app.lusk.underseerr.data.remote

import app.lusk.underseerr.data.auth.SessionRefresher
import app.lusk.underseerr.data.auth.SkipSessionRefresh
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.statement.discardRemaining
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
    private val securityManager: SecurityManager,
    private val sessionRefresher: SessionRefresher
) {
    private companion object {
        const val PLEX_TOKEN_HEADER = "X-Plex-Token"

        /** Headers whose values are credentials and must never reach the log. */
        val SENSITIVE_HEADERS = setOf(
            HttpHeaders.Authorization,
            HttpHeaders.Cookie,
            HttpHeaders.SetCookie,
            "X-Api-Key",
            PLEX_TOKEN_HEADER,
            "X-Plex-Client-Identifier"
        )
    }

    // Removed local caching to prevent synchronization issues
    // private var currentBaseUrl: String = ""

    /**
     * Diagnostics for base-URL resolution and session refresh.
     *
     * Gated on [AppConfig.isDebug] for the same reason the Ktor [Logging] plugin
     * below is: these lines print full request URLs, and a release build should
     * neither emit them nor pay to concatenate them on every request. The message
     * is a lambda so the string is only built when it will actually be printed.
     *
     * Not routed through [app.lusk.underseerr.util.AppLogger]: the factory is
     * constructed inside a Koin `single { }` that already carries a lazy
     * `() -> HttpClient` to break a DI cycle, and the Android AppLogger writes
     * via `Log.d`, which is *not* stripped in release — so injecting it would add
     * a constructor dependency without fixing the thing this item is about.
     */
    private inline fun debugLog(message: () -> String) {
        if (AppConfig.isDebug) println(message())
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = true
    }

    /**
     * @param engine overrides the platform default engine. Tests pass a MockEngine
     *   so the interceptors below are exercised as configured in production.
     */
    fun create(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
        val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(ContentNegotiation) {
                json(json)
            }
            
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                // HEADERS, never BODY: the auth request bodies carry the Plex token
                // and, for local/Jellyfin logins, the user's plaintext password.
                level = if (AppConfig.isDebug) LogLevel.HEADERS else LogLevel.NONE
                // Belt and braces — credential headers are masked in every build, so
                // this holds even if the debug flag is wrong.
                // Case-insensitive: HTTP field names are case-insensitive (RFC 9110 §5.1)
                // and OkHttp lowercases every RESPONSE header name, so an exact-match
                // set silently failed to mask "set-cookie" on device.
                sanitizeHeader { name -> SENSITIVE_HEADERS.any { it.equals(name, ignoreCase = true) } }
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

        val client = if (engine != null) HttpClient(engine, config) else HttpClient(config)

        // Intercept requests to inject headers asynchronously
        // Split out of create() so each concern reads on its own. Order is
        // load-bearing: the base URL and credentials must be applied before the
        // send phase can inspect a response or retry a request.
        installRequestDecoration(client)
        installSessionRefresh(client)

        return client
    }

    /** Resolves the configured base URL onto relative requests and attaches credentials. */
    private fun installRequestDecoration(client: HttpClient) {
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
                debugLog { "HttpClient: URL missing for API request (Auth present: $hasAuthData). Starting recovery efforts..." }
                
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
                            debugLog { "HttpClient: Auth data appeared at retry $retries, continuing to wait for URL..." }
                        }
                    }
                    
                    if (baseUrl.isNotEmpty()) {
                        debugLog { "HttpClient: Recovered Base URL after ${retries * 100}ms: $baseUrl" }
                        break
                    }
                    retries++
                }
                
                if (baseUrl.isEmpty()) {
                    debugLog { "HttpClient: Failed to recover Base URL after 3s. Request will fail." }
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
                        val baseSegments = newBase.segments.filter { it.isNotEmpty() }
                        if (baseSegments.isNotEmpty()) {
                            val originalSegments = context.url.pathSegments.filter { it.isNotEmpty() }
                            context.url.pathSegments = baseSegments + originalSegments
                        }
                        
                        // Let Ktor handle Host header automatically unless we have a reason to force it
                        context.headers.remove("Host") 
                        
                        debugLog { "HttpClient: [SUCCESS] Request targeting: ${context.url.buildString()}" }
                    } catch (e: Exception) {
                        debugLog { "HttpClient: [ERROR] Failed to apply Base URL '$baseUrl': ${e.message}" }
                    }
                } else {
                     debugLog { "HttpClient: [PASS] Already targeting external host: ${context.url.host}" }
                }
            } else {
                debugLog { "HttpClient: [CRITICAL] No Base URL found for ${context.url.buildString()}. This request will likely fail to localhost." }
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
    }

    /**
     * Recovers from an expired Overseerr session instead of surfacing a bare 403.
     * Overseerr answers protected endpoints with 403 (not 401) once the session
     * cookie lapses, so both codes are treated as a possible auth failure here.
     */
    private fun installSessionRefresh(client: HttpClient) {
        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)

            val status = call.response.status.value
            if (status != 401 && status != 403) return@intercept call

            // Never let the re-auth call trigger another re-auth.
            if (request.attributes.getOrNull(SkipSessionRefresh) == true) return@intercept call

            // A plex.tv rejection means the Plex token itself was revoked. It cannot
            // be refreshed without the user re-linking at plex.tv, so drop it rather
            // than logging anyone out — the Overseerr session is independent and may
            // still be fine. Watchlist *reads* then fall back to Overseerr. Adds and
            // removes cannot: Overseerr has no watchlist write endpoint, so they fail
            // with AppError.PlexReauthRequired asking the user to re-link Plex.
            if (request.headers[PLEX_TOKEN_HEADER] != null) {
                if (status == 401) sessionRefresher.invalidatePlexToken()
                return@intercept call
            }

            // Only cookie-authenticated Overseerr calls can be refreshed. API-key
            // requests use X-Api-Key — a 403 there is a genuine permission error,
            // not an expired session.
            val staleCookie = request.headers[HttpHeaders.Cookie] ?: return@intercept call

            if (!sessionRefresher.refresh(staleCookie)) return@intercept call
            val freshCookie = sessionRefresher.currentCookie() ?: return@intercept call

            // The request pipeline does not re-run on retry, so swap the cookie by hand.
            request.headers.remove(HttpHeaders.Cookie)
            request.headers.append(HttpHeaders.Cookie, freshCookie)
            debugLog { "HttpClient: Session refreshed after $status, retrying ${request.url.buildString()}" }
            // Drain the rejected response so its connection can be reused by the retry
            // instead of being held open by unread bytes.
            call.response.discardRemaining()
            execute(request)
        }
    }
}
