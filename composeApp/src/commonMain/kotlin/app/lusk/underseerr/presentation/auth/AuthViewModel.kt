package app.lusk.underseerr.presentation.auth

import app.lusk.underseerr.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.ServerInfo
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens.
 * Feature: underseerr
 * Validates: Requirements 1.1, 1.6, 5.4
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val securityManager: SecurityManager,
    private val logger: AppLogger
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _serverValidationState = MutableStateFlow<ServerValidationState>(ServerValidationState.Idle)
    val serverValidationState: StateFlow<ServerValidationState> = _serverValidationState.asStateFlow()

    private val _isCheckingStatus = MutableStateFlow(false)
    val isCheckingStatus: StateFlow<Boolean> = _isCheckingStatus.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    /**
     * Check if user is already authenticated.
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            authRepository.isAuthenticated().collect { isAuthenticated ->
                val currentState = _authState.value
                if (isAuthenticated) {
                    if (currentState != AuthState.Authenticated) {
                        _authState.value = AuthState.Authenticated
                    }
                } else {
                    // Only set to Unauthenticated if we aren't currently in a transitional state
                    if (currentState == AuthState.Initial || currentState == AuthState.Authenticated || currentState == AuthState.LoggingOut) {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }
    
    /**
     * Validate server URL.
     * Property 1: URL Validation Correctness
     */
    /**
     * Validate server URL.
     * Property 1: URL Validation Correctness
     */
    fun validateServer(url: String, allowHttp: Boolean = false) {
        viewModelScope.launch {
            if (url.contains("?apikey=")) {
                val cleanUrl = url.substringBefore("?apikey=")
                val apiKey = url.substringAfter("?apikey=")
                // Store API key securely (bypassing normal login flow for testing/manual override)
                securityManager.storeSecureData("underseerr_api_key", apiKey)
                // Set the clean URL for validation
                validateServerUrl(cleanUrl, allowHttp)
            } else {
                validateServerUrl(url, allowHttp)
            }
        }
    }

    private suspend fun validateServerUrl(url: String, allowHttp: Boolean) {
        _serverValidationState.value = ServerValidationState.Validating
        
        when (val result = authRepository.validateServerUrl(url, allowHttp)) {
            is Result.Success -> {
                // Check if we pre-injected an API key
                val apiKey = securityManager.retrieveSecureData("underseerr_api_key")
                if (apiKey != null) {
                    // Update interceptor with the key immediately so checkAuthStatus picks it up
                    // We need to re-fetch isAuthenticated or rely on checkAuthStatus loop
                    checkAuthStatus()
                }
                _serverValidationState.value = ServerValidationState.Valid(result.data)
            }
            is Result.Error -> {
                val rawMessage = result.error.message
                val userMessage = when {
                    rawMessage.contains("NSURLErrorDomain") || rawMessage.contains("ConnectException") -> 
                        "Unable to connect to server. Please check the URL and network."
                    rawMessage.contains("SSL") || rawMessage.contains("Certificate") -> 
                        "SSL Certificate error. Try using HTTP or check your certificate."
                    rawMessage.contains("401") || rawMessage.contains("403") ->
                        "Unauthorized. Please check your API key."
                    else -> rawMessage
                }
                _serverValidationState.value = ServerValidationState.Invalid(userMessage)
            }
            is Result.Loading -> {
                _serverValidationState.value = ServerValidationState.Validating
            }
        }
    }
    
    /**
     * Initiate Plex authentication and get redirect URL.
     */
    fun initiatePlexAuth() {
        viewModelScope.launch {
            logger.d("AuthViewModel", "Initiating Plex Auth...")
            _authState.value = AuthState.AuthenticatingWithPlex
            when (val result = authRepository.initiatePlexLogin()) {
                is Result.Success -> {
                    val (pinId, authUrl) = result.data
                    logger.d("AuthViewModel", "Plex PIN obtained: $pinId, URL: $authUrl")
                    _authState.value = AuthState.WaitingForPlex(pinId, authUrl)
                }
                is Result.Error -> {
                    logger.e("AuthViewModel", "Plex Login Init Error: ${result.error.message}")
                    _authState.value = AuthState.Error(result.error.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Check Plex PIN status.
     */
    fun checkPlexStatus(pinId: Int) {
        if (_isCheckingStatus.value) return
        
        viewModelScope.launch {
            _isCheckingStatus.value = true
            logger.d("AuthViewModel", "Checking Plex PIN status for ID: $pinId")
            when (val result = authRepository.checkPlexLoginStatus(pinId)) {
                is Result.Success -> {
                    result.data?.let { token ->
                        logger.d("AuthViewModel", "Plex token obtained from PIN!")
                        handleAuthCallback(token)
                    }
                }
                is Result.Error -> {
                    logger.e("AuthViewModel", "Error checking Plex PIN: ${result.error.message}")
                    // Don't show error to user during auto-polling, only if manual?
                    // For now, just log it.
                }
                else -> {}
            }
            _isCheckingStatus.value = false
        }
    }
    
    /**
     * Handle OAuth callback with Plex token.
     * Property 2: Token Exchange Integrity
     */
    fun handleAuthCallback(plexToken: String) {
        viewModelScope.launch {
            logger.d("AuthViewModel", "Exchanging Plex token for Overseerr session... Token length: ${plexToken.length}")
            _authState.value = AuthState.ExchangingToken
            
            when (val result = authRepository.authenticateWithPlex(plexToken)) {
                is Result.Success -> {
                    logger.d("AuthViewModel", "Authentication successful! Navigating to Home.")
                    _authState.value = AuthState.Authenticated
                }
                is Result.Error -> {
                    logger.e("AuthViewModel", "Authentication failed: ${result.error.message}")
                    _authState.value = AuthState.Error(result.error.message)
                }
                is Result.Loading -> {
                    _authState.value = AuthState.ExchangingToken
                }
            }
        }
    }

    /**
     * Login with local credentials.
     */
    fun loginLocal(username: String, password: String) {
        viewModelScope.launch {
            logger.d("AuthViewModel", "Logging in with local account...")
            _authState.value = AuthState.Authenticating
            when (val result = authRepository.authenticateLocal(username, password)) {
                is Result.Success -> {
                    logger.d("AuthViewModel", "Local login successful!")
                    _authState.value = AuthState.Authenticated
                }
                is Result.Error -> {
                    logger.e("AuthViewModel", "Local login failed: ${result.error.message}")
                    _authState.value = AuthState.Error(result.error.message)
                }
                else -> {}
            }
        }
    }

    /**
     * Login with API key.
     */
    fun loginWithApiKey(apiKey: String) {
        viewModelScope.launch {
            logger.d("AuthViewModel", "Verifying API key...")
            _authState.value = AuthState.Authenticating
            when (val result = authRepository.authenticateWithApiKey(apiKey)) {
                is Result.Success -> {
                    logger.d("AuthViewModel", "API key verification successful!")
                    _authState.value = AuthState.Authenticated
                }
                is Result.Error -> {
                    logger.e("AuthViewModel", "API key verification failed: ${result.error.message}")
                    _authState.value = AuthState.Error(result.error.message)
                }
                else -> {}
            }
        }
    }
    
    /**
     * Logout user.
     * Property 21: Logout Cleanup
     */
    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.LoggingOut
            try {
                authRepository.logout()
            } catch (e: Exception) {
                logger.e("AuthViewModel", "Logout error: ${e.message}")
            } finally {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
    
    /**
     * Retry authentication after error.
     */
    fun retryAuth() {
        _authState.value = AuthState.Unauthenticated
        _serverValidationState.value = ServerValidationState.Idle
    }
    
    /**
     * Get stored session information.
     */
    /**
     * Get the server URL.
     */
    fun getServerUrl() = authRepository.getServerUrl()

    /**
     * Clear server validation state.
     */
    fun clearServerValidation() {
        _serverValidationState.value = ServerValidationState.Idle
    }
    fun resetAuth() {
        _authState.value = AuthState.Unauthenticated
        _serverValidationState.value = ServerValidationState.Idle
    }
}

/**
 * Authentication state.
 */
sealed class AuthState {
    data object Initial : AuthState()
    data object Unauthenticated : AuthState()
    data object AuthenticatingWithPlex : AuthState()
    data object Authenticating : AuthState()
    data class WaitingForPlex(val pinId: Int, val authUrl: String) : AuthState()
    data object ExchangingToken : AuthState()
    data object Authenticated : AuthState()
    data object LoggingOut : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Server validation state.
 */
sealed class ServerValidationState {
    data object Idle : ServerValidationState()
    data object Validating : ServerValidationState()
    data class Valid(val serverInfo: ServerInfo) : ServerValidationState()
    data class Invalid(val message: String) : ServerValidationState()
}
