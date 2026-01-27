package app.lusk.underseerr.presentation.auth

import app.cash.turbine.test
import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.ServerInfo
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for AuthViewModel.
 * Feature: underseerr
 * Validates: Requirements 1.1, 1.3, 1.7
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var securityManager: SecurityManager
    private lateinit var logger: AppLogger
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        securityManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        
        // Default: not authenticated
        coEvery { authRepository.isAuthenticated() } returns flowOf(false)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `when server URL is valid HTTPS, should validate successfully`() = runTest(testDispatcher) {
        // Arrange
        val serverUrl = "https://overseerr.example.com"
        val serverInfo = ServerInfo(version = "1.0.0")
        coEvery { authRepository.validateServerUrl(serverUrl) } returns Result.success(serverInfo)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.validateServer(serverUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        coVerify { authRepository.validateServerUrl(serverUrl) }
        viewModel.serverValidationState.value shouldBe ServerValidationState.Valid(serverInfo)
    }
    
    @Test
    fun `when server URL is HTTP, should reject for security`() = runTest(testDispatcher) {
        // Arrange
        val serverUrl = "http://overseerr.example.com"
        val error = AppError.ValidationError("Server URL must use HTTPS for security.")
        coEvery { authRepository.validateServerUrl(serverUrl) } returns Result.error(error)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.validateServer(serverUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        coVerify { authRepository.validateServerUrl(serverUrl) }
        val state = viewModel.serverValidationState.value
        state shouldBe ServerValidationState.Invalid(error.message)
    }
    
    @Test
    fun `when server URL is malformed, should reject`() = runTest(testDispatcher) {
        // Arrange
        val serverUrl = "not-a-valid-url"
        val error = AppError.ValidationError("Invalid server URL format. Must be a valid HTTP/HTTPS URL.")
        coEvery { authRepository.validateServerUrl(serverUrl) } returns Result.error(error)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.validateServer(serverUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        val state = viewModel.serverValidationState.value
        state shouldBe ServerValidationState.Invalid(error.message)
    }
    
    @Test
    fun `when initiating auth, should transition to AuthenticatingWithPlex state`() = runTest(testDispatcher) {
        // Arrange
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        coEvery { authRepository.initiatePlexLogin() } returns Result.success(1 to "ABCD")
        viewModel.initiatePlexAuth()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        val state = viewModel.authState.value
        state.shouldBeInstanceOf<AuthState.WaitingForPlex>()
    }
    
    @Test
    fun `when handling auth callback with valid token, should authenticate successfully`() = runTest(testDispatcher) {
        // Arrange
        val plexToken = "valid-plex-token"
        val userProfile = UserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = null,
            requestCount = 0,
            permissions = mockk(),
            isPlexUser = false
        )
        coEvery { authRepository.authenticateWithPlex(plexToken) } returns Result.success(userProfile)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.handleAuthCallback(plexToken)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        coVerify { authRepository.authenticateWithPlex(plexToken) }
        viewModel.authState.value shouldBe AuthState.Authenticated
    }
    
    @Test
    fun `when handling auth callback with invalid token, should show error`() = runTest(testDispatcher) {
        // Arrange
        val plexToken = "invalid-token"
        val errorMessage = "Invalid Plex token"
        val error = AppError.AuthError(errorMessage)
        coEvery { authRepository.authenticateWithPlex(plexToken) } returns Result.error(error)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.handleAuthCallback(plexToken)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        val state = viewModel.authState.value
        state shouldBe AuthState.Error(errorMessage)
    }
    
    @Test
    fun `when auth callback is processing, should transition through ExchangingToken state`() = runTest(testDispatcher) {
        // Arrange
        val plexToken = "valid-token"
        val userProfile = UserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = null,
            requestCount = 0,
            permissions = mockk(),
            isPlexUser = false
        )
        coEvery { authRepository.authenticateWithPlex(plexToken) } returns Result.success(userProfile)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act & Assert
        viewModel.authState.test {
            awaitItem() // Initial state
            
            viewModel.handleAuthCallback(plexToken)
            testDispatcher.scheduler.advanceUntilIdle()
            
            val exchangingState = awaitItem()
            exchangingState shouldBe AuthState.ExchangingToken
            
            val authenticatedState = awaitItem()
            authenticatedState shouldBe AuthState.Authenticated
        }
    }
    
    @Test
    fun `when retrying after error, should reset to unauthenticated state`() = runTest(testDispatcher) {
        // Arrange
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Set error state
        coEvery { authRepository.authenticateWithPlex(any()) } returns Result.Error(AppError.AuthError("Error"))
        viewModel.handleAuthCallback("invalid-token")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.retryAuth()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.authState.value shouldBe AuthState.Unauthenticated
        viewModel.serverValidationState.value shouldBe ServerValidationState.Idle
    }
    
    @Test
    fun `when clearing server validation, should reset to idle state`() = runTest(testDispatcher) {
        // Arrange
        val serverUrl = "https://overseerr.example.com"
        val serverInfo = ServerInfo(version = "1.0.0")
        coEvery { authRepository.validateServerUrl(serverUrl) } returns Result.success(serverInfo)
        
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.validateServer(serverUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.clearServerValidation()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.serverValidationState.value shouldBe ServerValidationState.Idle
    }
    
    @Test
    fun `when user is already authenticated, should start in authenticated state`() = runTest(testDispatcher) {
        // Arrange
        coEvery { authRepository.isAuthenticated() } returns flowOf(true)
        
        // Act
        viewModel = AuthViewModel(authRepository, securityManager, logger)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.authState.test {
            val state = awaitItem()
            state shouldBe AuthState.Authenticated
        }
    }
}
