package app.lusk.client.data.repository

import android.util.Log
import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.data.remote.api.AuthApiService
import app.lusk.client.data.remote.api.PlexAuthRequest
import app.lusk.client.data.remote.interceptor.AuthInterceptor
import app.lusk.client.data.remote.PersistentCookieJar
import app.lusk.client.data.mapper.toDomain
import app.lusk.client.data.remote.safeApiCall
import app.lusk.client.data.remote.toAppError
import app.lusk.client.domain.security.SecurityManager
import app.lusk.client.domain.model.OverseerrSession
import app.lusk.client.domain.model.Result
import app.lusk.client.domain.model.ServerInfo
import app.lusk.client.domain.model.UserProfile
import app.lusk.client.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository for authentication operations.
 * Feature: overseerr-android-client
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val plexApiService: app.lusk.client.data.remote.api.PlexApiService,
    private val securityManager: SecurityManager,
    private val preferencesManager: PreferencesManager,
    private val authInterceptor: AuthInterceptor,
    private val cookieJar: PersistentCookieJar
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
            authInterceptor.setServerUrl(url)
            
            // Try to fetch server info to validate connectivity
            val result = safeApiCall {
                authApiService.getServerInfo()
            }
            
            when (result) {
                is Result.Success -> {
                    val apiServerInfo = result.data
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
        return try {
            // Call Plex authentication endpoint
            val result = safeApiCall {
                authApiService.authenticateWithPlex(PlexAuthRequest(plexToken))
            }
            
            when (result) {
                is Result.Success -> {
                    val apiUserProfile = result.data
                    
                    // Overseerr typically returns session cookie, so we don't always have an API key
                    // We use a placeholder to indicate a valid session exists for getStoredSession()
                    val sessionMarker = "SESSION_COOKIE"
                    
                    // Store session marker
                    securityManager.storeSecureData(API_KEY_STORAGE_KEY, sessionMarker)
                    
                    // Update auth interceptor with placeholder (it will know to use cookies only)
                    authInterceptor.setApiKey(sessionMarker)
                    preferencesManager.getServerUrl().first()?.let { authInterceptor.setServerUrl(it) }
                    
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
            val clientId = java.util.UUID.randomUUID().toString()
            preferencesManager.setClientId(clientId)
            val response = plexApiService.getPin(clientId = clientId)
            val authUrl = "https://app.plex.tv/auth/#!?clientID=${clientId}&code=${response.code}&context[device][product]=Lusk%20Overseerr%20Client"
            Result.success(response.id to authUrl)
        } catch (e: Exception) {
            Result.error(e.toAppError())
        }
    }

    override suspend fun checkPlexLoginStatus(pinId: Int): Result<String?> {
        return try {
            val clientId = preferencesManager.getClientId() ?: java.util.UUID.randomUUID().toString()
            val response = plexApiService.checkPin(id = pinId, clientId = clientId)
            Result.success(response.authToken)
        } catch (e: Exception) {
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
                    Log.d("AuthRepositoryImpl", "Found legacy 'no_api_key' placeholder. Forcing logout to refresh session.")
                    logout() 
                    return@map null
                }

                val serverUrl = preferencesManager.getServerUrl().first()
                if (apiKey != null && serverUrl != null) {
                    authInterceptor.setApiKey(apiKey)
                    authInterceptor.setServerUrl(serverUrl)
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
            
            // Update auth interceptor
            authInterceptor.setApiKey(apiKey)
            preferencesManager.getServerUrl().first()?.let { authInterceptor.setServerUrl(it) }
            
            // Fetch current user
            val result = safeApiCall {
                authApiService.getCurrentUser()
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
                authApiService.logout()
            }
        } catch (e: Exception) {
            // Ignore errors during logout API call
        }
        
        // Clear stored credentials
        securityManager.clearSecureData()
        preferencesManager.clearAuthData()
        
        // Clear auth interceptor
        authInterceptor.clearApiKey()
        
        // Clear cookies
        cookieJar.clear()
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
        return try {
            val parsedUrl = URL(url)
            val scheme = parsedUrl.protocol
            val host = parsedUrl.host
            
            // Must have http or https scheme
            if (scheme != "http" && scheme != "https") {
                return false
            }
            
            // Must have a host
            if (host.isNullOrBlank()) {
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}
