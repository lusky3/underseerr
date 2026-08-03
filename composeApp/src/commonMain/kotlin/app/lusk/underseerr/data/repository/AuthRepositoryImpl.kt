package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.auth.SessionRefresher
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.api.AuthKtorService
import app.lusk.underseerr.data.remote.api.PlexKtorService
import app.lusk.underseerr.data.remote.api.PlexAuthRequest
import app.lusk.underseerr.data.mapper.toDomain
import app.lusk.underseerr.data.remote.safeApiCall
import app.lusk.underseerr.data.remote.toAppError
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.domain.model.UnderseerrSession
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.ServerInfo
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.AuthRepository
import io.ktor.client.statement.HttpResponse
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of AuthRepository for authentication operations.
 * Feature: underseerr
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6
 */
class AuthRepositoryImpl(
    private val authKtorService: AuthKtorService,
    private val plexKtorService: PlexKtorService,
    private val settingsKtorService: app.lusk.underseerr.data.remote.api.SettingsKtorService,
    private val securityManager: SecurityManager,
    private val preferencesManager: PreferencesManager,
    private val sessionCleaner: app.lusk.underseerr.data.auth.SessionCleaner
) : AuthRepository {
    
    companion object {
        private const val API_KEY_STORAGE_KEY = "underseerr_api_key"
        private const val SESSION_STORAGE_KEY = "underseerr_session"

        /**
         * Placeholder written by builds that predate session-cookie storage.
         * It is not a credential: a device carrying it has to sign in again.
         */
        const val LEGACY_NO_API_KEY_MARKER = "no_api_key"
    }

    /** Guards the one-shot legacy cleanup below. */
    private val legacyMarkerMutex = kotlinx.coroutines.sync.Mutex()
    private var legacyMarkerHandled = false
    
    override suspend fun validateServerUrl(url: String, allowHttp: Boolean): Result<ServerInfo> {
        val cleanUrl = url.trim()
        return try {
            // Validate URL format
            if (!isValidUrl(cleanUrl)) {
                return Result.error(
                    app.lusk.underseerr.domain.model.AppError.ValidationError(
                        "Invalid server URL format. Must be a valid HTTP/HTTPS URL."
                    )
                )
            }
            
            // Enforce HTTPS for security (except for localhost in debug or if explicitly allowed)
            val isLocalhost = cleanUrl.contains("localhost", ignoreCase = true) || cleanUrl.contains("127.0.0.1")
            if (!cleanUrl.startsWith("https://", ignoreCase = true) && !isLocalhost && !allowHttp) {
                return Result.error(
                    app.lusk.underseerr.domain.model.AppError.ValidationError(
                        "Server URL must use HTTPS for security."
                    )
                )
            }
            
            // Store server URL
            preferencesManager.setServerUrl(cleanUrl)
            
            // Try to fetch server info to validate connectivity
            val result = safeApiCall {
                // authKtorService.getServerInfo() // Deprecated: Use public settings to detect Jellyseerr
                settingsKtorService.getPublicSettings()
            }
            
            when (result) {
                is Result.Success -> {
                    val publicSettings = result.data
                    val isJellyseerr = publicSettings.mediaServerType == 4
                    
                    // Add to configured servers list
                    preferencesManager.addServer(
                        app.lusk.underseerr.domain.repository.ServerConfig(
                            url = cleanUrl,
                            name = "Server ${cleanUrl.replace("https://", "").replace("http://", "").substringBefore("/")}",
                            isActive = true,
                            isJellyseerr = isJellyseerr
                        )
                    )
                    
                    // Only persist isJellyseerr in a basic way for now (via preferences if we had a field, or inferred)
                    // Ideally, we should store server type in PreferencesManager.ServerConfig too.
                    // For now, key features will re-check or rely on this initial check.
                    
                    Result.success(
                        ServerInfo(
                            version = "Unknown", // Public settings might not return version, strictly speaking
                            initialized = publicSettings.initialized,
                            applicationUrl = publicSettings.applicationUrl,
                            isJellyseerr = isJellyseerr
                        )
                    )
                }
                is Result.Error -> {
                    // Fallback to old status endpoint if public settings fails (legacy support)
                     val fallbackResult = safeApiCall { authKtorService.getServerInfo() }
                     if (fallbackResult is Result.Success) {
                         val info = fallbackResult.data
                         preferencesManager.setServerUrl(cleanUrl)
                         Result.success(ServerInfo(info.version, info.initialized, info.applicationUrl, false))
                     } else {
                         result // Return original error
                     }
                }
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(
                app.lusk.underseerr.domain.model.AppError.NetworkError(
                    "Failed to connect to server: ${e.message}"
                )
            )
        }
    }
    
    /**
     * Exchanges a Plex token for an Overseerr session.
     *
     * Every token reaching here is untrusted input: `underseerr://auth?token=…`
     * is an exported, BROWSABLE deep link, so the string can come from any web
     * page or installed app. Only the server may decide whether a token is
     * valid — there is deliberately no locally recognised token.
     */
    override suspend fun authenticateWithPlex(plexToken: String): Result<UserProfile> {
        return try {
            // Call Plex authentication endpoint
            val result = safeApiCall {
                authKtorService.authenticateWithPlex(plexToken)
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val apiUserProfile: app.lusk.underseerr.data.remote.model.ApiUserProfile = response.body()
                    
                    // Store Plex token for direct Plex API access
                    securityManager.storeSecureData("plex_token", plexToken)
                    
                    // Extract session cookie (shared with SessionRefresher so login
                    // and refresh always store the cookie in the same shape)
                    val cookieValue = SessionRefresher.extractSessionCookie(response)
                    if (cookieValue != null) {
                        securityManager.storeSecureData("cookie_auth_token", cookieValue)
                    } else {
                        println("AuthRepositoryImpl: WARNING - No Set-Cookie header found!")
                    }
                    
                    // Overseerr typically returns session cookie, so we don't always have an API key
                    // We use a placeholder to indicate a valid session exists for getStoredSession()
                    val sessionMarker = "SESSION_COOKIE"
                    
                    // Store session marker
                    securityManager.storeSecureData(API_KEY_STORAGE_KEY, sessionMarker)
                    
                    // Store user ID
                    preferencesManager.setUserId(apiUserProfile.id)
                    
                    // Map to domain model
                    val userProfile = apiUserProfile.toDomain()
                    
                    Result.success(userProfile)
                }
                is Result.Error -> result
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(
                app.lusk.underseerr.domain.model.AppError.AuthError(
                    "Authentication failed: ${e.message}"
                )
            )
        }
    }

    override suspend fun authenticateLocal(username: String, password: String): Result<UserProfile> {
        return try {
            val result = safeApiCall {
                authKtorService.loginLocal(username, password)
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val apiUserProfile: app.lusk.underseerr.data.remote.model.ApiUserProfile = response.body()
                    
                    // Store session cookie
                    SessionRefresher.extractSessionCookie(response)?.let { cookieValue ->
                        securityManager.storeSecureData("cookie_auth_token", cookieValue)
                    }
                    
                    // Store session marker and user ID
                    val sessionMarker = "SESSION_COOKIE"
                    securityManager.storeSecureData(API_KEY_STORAGE_KEY, sessionMarker)
                    preferencesManager.setUserId(apiUserProfile.id)
                    
                    Result.success(apiUserProfile.toDomain())
                }
                is Result.Error -> result
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(app.lusk.underseerr.domain.model.AppError.AuthError("Local authentication failed: ${e.message}"))
        }
    }

    override suspend fun authenticateWithJellyfin(username: String, password: String, hostname: String): Result<UserProfile> {
        return try {
            val result = safeApiCall {
                authKtorService.authenticateWithJellyfin(username, password, hostname)
            }

            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val apiUserProfile: app.lusk.underseerr.data.remote.model.ApiUserProfile = response.body()

                    // Store session cookie
                    SessionRefresher.extractSessionCookie(response)?.let { cookieValue ->
                        securityManager.storeSecureData("cookie_auth_token", cookieValue)
                    }

                    // Store session marker and user ID
                    val sessionMarker = "SESSION_COOKIE"
                    securityManager.storeSecureData(API_KEY_STORAGE_KEY, sessionMarker)
                    preferencesManager.setUserId(apiUserProfile.id)

                    Result.success(apiUserProfile.toDomain())
                }
                is Result.Error -> result
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(app.lusk.underseerr.domain.model.AppError.AuthError("Jellyfin authentication failed: ${e.message}"))
        }
    }

    override suspend fun authenticateWithApiKey(apiKey: String): Result<UserProfile> {
        return try {
            // First store the API key so subsequent calls use it
            securityManager.storeSecureData(API_KEY_STORAGE_KEY, apiKey)
            
            // Validate the key by fetching current user
            val result = getCurrentUser()
            
            if (result is Result.Error) {
                // Clear if invalid
                securityManager.clearSecureData()
                preferencesManager.clearAuthData()
            }
            
            result
        } catch (e: Exception) {
            Result.error(app.lusk.underseerr.domain.model.AppError.AuthError("API key validation failed: ${e.message}"))
        }
    }
    
    override suspend fun initiatePlexLogin(): Result<Pair<Int, String>> {
        return try {
            var clientId = preferencesManager.getClientId()
            if (clientId == null) {
                clientId = "native-client-${kotlin.random.Random.nextInt(100000, 999999)}"
                preferencesManager.setClientId(clientId)
            }
            val response = plexKtorService.getPin(clientId = clientId)
            val product = "Underseerr"
            val authUrl = "https://app.plex.tv/auth#?clientID=$clientId&code=${response.code}&context%5Bdevice%5D%5Bproduct%5D=$product&context%5Bdevice%5D%5Bdevice%5D=iPhone&context%5Bdevice%5D%5Bplatform%5D=iOS"
            Result.success(response.id to authUrl)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }

    override suspend fun checkPlexLoginStatus(pinId: Int): Result<String?> {
        println("AuthRepositoryImpl: Checking Plex login status for PIN: $pinId")
        return try {
            val clientId = preferencesManager.getClientId() ?: "default-client-id"
            println("AuthRepositoryImpl: Using ClientID: $clientId")
            val response = plexKtorService.checkPin(id = pinId, clientId = clientId)
            println("AuthRepositoryImpl: CheckPin response: code=${response.code}, tokenPresent=${response.authToken != null}")
            Result.success(response.authToken)
        } catch (e: Exception) {
            println("AuthRepositoryImpl: Exception in checkPlexLoginStatus: ${e.message}")
            Result.error(e.toAppError())
        }
    }
    
    /**
     * The stored session, as a projection of what is on disk.
     *
     * Reading session state must stay free of side effects. This flow is cold
     * and collected by several screens at once (every `AuthViewModel` collects
     * `isAuthenticated()`, which maps over this), and it re-emits whenever the
     * stored user id changes — so anything destructive placed in the `map` would
     * run once per collector and again on every re-collection. The one piece of
     * cleanup that is genuinely needed runs exactly once, in [clearLegacySessionMarkerOnce].
     */
    override fun getStoredSession(): Flow<UnderseerrSession?> {
        return preferencesManager.getUserId()
            .onStart { clearLegacySessionMarkerOnce() }
            .map { userId ->
                if (userId == null) return@map null

                val apiKey = securityManager.retrieveSecureData(API_KEY_STORAGE_KEY)

                // The legacy placeholder is not a session: these devices have no
                // usable cookie, so they must sign in again. Reported as "no
                // session" whether or not the cleanup below has run yet.
                if (apiKey == null || apiKey == LEGACY_NO_API_KEY_MARKER) return@map null

                val serverUrl = preferencesManager.getServerUrl().first() ?: return@map null
                UnderseerrSession(
                    userId = userId,
                    apiKey = apiKey,
                    serverUrl = serverUrl,
                    expiresAt = null
                )
            }
    }

    /**
     * Migrates a device off the legacy `"no_api_key"` placeholder, at most once
     * per process.
     *
     * Such a device is signed out already as far as [getStoredSession] is
     * concerned; this drops the leftovers (credentials, auth preferences and the
     * account-scoped Room cache) so the next account to sign in on the device is
     * not served the previous one's data. It is not routed through [logout]
     * because there is no server session to end — the placeholder means no
     * cookie was ever stored — and a network call has no business running when a
     * screen merely reads the session state.
     */
    private suspend fun clearLegacySessionMarkerOnce() {
        if (legacyMarkerHandled) return
        legacyMarkerMutex.withLock {
            if (legacyMarkerHandled) return
            if (securityManager.retrieveSecureData(API_KEY_STORAGE_KEY) == LEGACY_NO_API_KEY_MARKER) {
                try {
                    sessionCleaner.clear()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Don't latch the flag: a cancelled wipe must be retried next launch.
                    throw e
                } catch (e: Exception) {
                    // Reading the session must not fail because the cleanup did.
                    // The projection above keeps this device signed out regardless.
                    println("AuthRepositoryImpl: legacy session cleanup failed: ${e.message}")
                }
            }
            legacyMarkerHandled = true
        }
    }
    
    override suspend fun getCurrentUser(): Result<UserProfile> {
        return try {
            // Ensure we have an API key
            val apiKey = securityManager.retrieveSecureData(API_KEY_STORAGE_KEY)
            if (apiKey == null) {
                return Result.error(
                    app.lusk.underseerr.domain.model.AppError.AuthError(
                        "Not authenticated. Please log in."
                    )
                )
            }
            
            // Fetch current user
            val result = safeApiCall {
                authKtorService.getCurrentUser()
            }
            
            when (result) {
                is Result.Success -> {
                    val apiUserProfile = result.data
                    val userProfile = apiUserProfile.toDomain()
                    Result.success(userProfile)
                }
                is Result.Error -> result
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(
                app.lusk.underseerr.domain.model.AppError.AuthError(
                    "Failed to get current user: ${e.message}"
                )
            )
        }
    }
    
    override suspend fun logout() {
        try {
            // Call logout endpoint (best effort)
            safeApiCall {
                authKtorService.logout()
            }
        } catch (e: Exception) {
            // Ignore errors during logout API call
        }
        
        // Credentials + local cache, in one place shared with the involuntary
        // sign-out in SessionRefresher so the two can't drift.
        sessionCleaner.clear()
    }
    
    override fun isAuthenticated(): Flow<Boolean> {
        return getStoredSession().map { it != null }
    }
    
    override suspend fun refreshSession(): Result<UserProfile> {
        // For now, just get current user
        // In a real implementation, this might refresh tokens
        return getCurrentUser()
    }

    override fun getServerUrl(): Flow<String?> {
        return preferencesManager.getServerUrl()
    }
    
    override fun getIsJellyseerr(): Flow<Boolean> {
        return kotlinx.coroutines.flow.combine(
            preferencesManager.getConfiguredServers(),
            preferencesManager.getServerUrl()
        ) { servers, currentUrl ->
            servers.find { it.url == currentUrl }?.isJellyseerr ?: false
        }
    }
    
    override suspend fun getPlexToken(): String? {
        return securityManager.retrieveSecureData("plex_token")
    }

    /**
     * Validate URL format.

     */
    private fun isValidUrl(url: String): Boolean {
        // Simple regex for URL validation in KMP
        val regex = "^(http|https)://.*".toRegex()
        return url.isNotBlank() && regex.matches(url)
    }
}
