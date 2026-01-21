package app.lusk.client.data.repository

import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.data.remote.api.AuthKtorService
import app.lusk.client.data.remote.api.PlexKtorService
import app.lusk.client.data.remote.api.PlexAuthRequest
import app.lusk.client.data.mapper.toDomain
import app.lusk.client.data.remote.safeApiCall
import app.lusk.client.data.remote.toAppError
import app.lusk.client.domain.security.SecurityManager
import app.lusk.client.domain.model.OverseerrSession
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.ServerInfo
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.repository.AuthRepository
import io.ktor.client.statement.HttpResponse
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of AuthRepository for authentication operations.
 * Feature: overseerr-android-client
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6
 */
class AuthRepositoryImpl(
    private val authKtorService: AuthKtorService,
    private val plexKtorService: PlexKtorService,
    private val securityManager: SecurityManager,
    private val preferencesManager: PreferencesManager
) : AuthRepository {
    
    companion object {
        private const val API_KEY_STORAGE_KEY = "overseerr_api_key"
        private const val SESSION_STORAGE_KEY = "overseerr_session"
    }
    
    override suspend fun validateServerUrl(url: String, allowHttp: Boolean): Result<ServerInfo> {
        return try {
            // Validate URL format
            if (!isValidUrl(url)) {
                return Result.error(
                    app.lusk.client.domain.model.AppError.ValidationError(
                        "Invalid server URL format. Must be a valid HTTP/HTTPS URL."
                    )
                )
            }
            
            // Enforce HTTPS for security (except for localhost in debug or if explicitly allowed)
            val isLocalhost = url.contains("localhost", ignoreCase = true) || url.contains("127.0.0.1")
            if (!url.startsWith("https://", ignoreCase = true) && !isLocalhost && !allowHttp) {
                return Result.error(
                    app.lusk.client.domain.model.AppError.ValidationError(
                        "Server URL must use HTTPS for security."
                    )
                )
            }
            
            // Store server URL
            preferencesManager.setServerUrl(url)
            
            // Try to fetch server info to validate connectivity
            val result = safeApiCall {
                authKtorService.getServerInfo()
            }
            
            when (result) {
                is Result.Success -> {
                    val apiServerInfo = result.data
                    
                    // Add to configured servers list
                    preferencesManager.addServer(
                        app.lusk.client.domain.repository.ServerConfig(
                            url = url,
                            name = "Server ${url.replace("https://", "").replace("http://", "").substringBefore("/")}",
                            isActive = true
                        )
                    )
                    
                    Result.success(
                        ServerInfo(
                            version = apiServerInfo.version,
                            initialized = apiServerInfo.initialized,
                            applicationUrl = apiServerInfo.applicationUrl
                        )
                    )
                }
                is Result.Error -> result
                is Result.Loading -> Result.loading()
            }
        } catch (e: Exception) {
            Result.error(
                app.lusk.client.domain.model.AppError.NetworkError(
                    "Failed to connect to server: ${e.message}"
                )
            )
        }
    }
    
    override suspend fun authenticateWithPlex(plexToken: String): Result<UserProfile> {
        // Debug Bypass
        if (plexToken == "debug_token_12345") {
            println("AuthRepositoryImpl: Debug token detected. Bypassing authentication.")
            val dummyUser = UserProfile(
                id = 1,
                email = "debug@example.com",
                displayName = "Debug User",
                avatar = null,
                requestCount = 0,
                permissions = app.lusk.client.domain.model.Permissions(
                    canRequest = true,
                    canManageRequests = true,
                    canViewRequests = true,
                    isAdmin = true
                ),
                isPlexUser = true
            )
            val debugKey = "debug_session_key_12345"
            securityManager.storeSecureData(API_KEY_STORAGE_KEY, debugKey)
            preferencesManager.setApiKey(debugKey)
            preferencesManager.setUserId(1)
            println("AuthRepositoryImpl: Stored dummy user ID 1 and debug API key.")
            return Result.success(dummyUser)
        }

        return try {
            // Call Plex authentication endpoint
            val result = safeApiCall {
                authKtorService.authenticateWithPlex(plexToken)
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.data
                    val apiUserProfile: app.lusk.client.data.remote.model.ApiUserProfile = response.body()
                    
                    // Manually extract session cookie if present
                    val setCookieHeader = response.headers["Set-Cookie"]
                    println("AuthRepositoryImpl: Set-Cookie Header: $setCookieHeader")
                    
                    if (setCookieHeader != null) {
                        val cookieValue = setCookieHeader.split(";").firstOrNull()
                        if (cookieValue != null) {
                             println("AuthRepositoryImpl: Storing cookie: $cookieValue")
                             securityManager.storeSecureData("cookie_auth_token", cookieValue)
                        }
                    } else {
                        println("AuthRepositoryImpl: WARNING - No Set-Cookie header found!")
                        // Fallback: Check if we have a raw cookie list
                    }
                    
                    // Overseerr typically returns session cookie, so we don't always have an API key
                    // We use a placeholder to indicate a valid session exists for getStoredSession()
                    val sessionMarker = "SESSION_COOKIE"
                    
                    // Store session marker
                    securityManager.storeSecureData(API_KEY_STORAGE_KEY, sessionMarker)
                    preferencesManager.setApiKey(sessionMarker)
                    
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
                app.lusk.client.domain.model.AppError.AuthError(
                    "Authentication failed: ${e.message}"
                )
            )
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
    
    override fun getStoredSession(): Flow<OverseerrSession?> {
        return preferencesManager.getUserId().map { userId ->
            if (userId != null) {
                val apiKey = securityManager.retrieveSecureData(API_KEY_STORAGE_KEY)
                
                // If we have the legacy "no_api_key" placeholder, force a re-login
                // because we need to properly establish session cookies.
                if (apiKey == "no_api_key") {
                    // Log.d("AuthRepositoryImpl", "Found legacy 'no_api_key' placeholder. Forcing logout to refresh session.")
                    logout() 
                    return@map null
                }

                val serverUrl = preferencesManager.getServerUrl().first()
                if (apiKey != null && serverUrl != null) {
                    OverseerrSession(
                        userId = userId,
                        apiKey = apiKey,
                        serverUrl = serverUrl,
                        expiresAt = null
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
    
    override suspend fun getCurrentUser(): Result<UserProfile> {
        return try {
            // Ensure we have an API key
            val apiKey = securityManager.retrieveSecureData(API_KEY_STORAGE_KEY)
            if (apiKey == null) {
                return Result.error(
                    app.lusk.client.domain.model.AppError.AuthError(
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
                app.lusk.client.domain.model.AppError.AuthError(
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
        
        // Clear stored credentials
        securityManager.clearSecureData()
        preferencesManager.clearAuthData()
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
    
    /**
     * Validate URL format.
     */
    private fun isValidUrl(url: String): Boolean {
        // Simple regex for URL validation in KMP
        val regex = "^(http|https)://.*".toRegex()
        return url.isNotBlank() && regex.matches(url)
    }
}
