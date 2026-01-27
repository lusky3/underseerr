package app.lusk.underseerr.domain.repository

import app.lusk.underseerr.domain.model.UnderseerrSession
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.ServerInfo
import app.lusk.underseerr.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Feature: underseerr
 * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6
 */
interface AuthRepository {
    
    /**
     * Validate server URL and check connectivity.
     * Property 1: URL Validation Correctness
     */
    suspend fun validateServerUrl(url: String, allowHttp: Boolean = false): Result<ServerInfo>
    
    /**
     * Authenticate with Plex token and exchange for Overseerr session.
     * Property 2: Token Exchange Integrity
     */
    suspend fun authenticateWithPlex(plexToken: String): Result<UserProfile>

    /**
     * Initiate Plex login by requesting a PIN.
     * @return Pair of PIN ID and Auth URL
     */
    suspend fun initiatePlexLogin(): Result<Pair<Int, String>>

    /**
     * Check the status of a Plex PIN.
     * @param pinId The ID of the PIN to check
     * @return The auth token if completed, or null if still waiting
     */
    suspend fun checkPlexLoginStatus(pinId: Int): Result<String?>
    
    /**
     * Get stored session as a Flow.
     */
    fun getStoredSession(): Flow<UnderseerrSession?>
    
    /**
     * Get current authenticated user.
     */
    suspend fun getCurrentUser(): Result<UserProfile>
    
    /**
     * Clear session and logout.
     * Property 21: Logout Cleanup
     */
    suspend fun logout()
    
    /**
     * Check if user is authenticated.
     */
    fun isAuthenticated(): Flow<Boolean>
    
    /**
     * Refresh session if needed.
     */
    suspend fun refreshSession(): Result<UserProfile>

    /**
     * Get the currently configured server URL.
     */
    fun getServerUrl(): Flow<String?>
}
