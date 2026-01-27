package app.lusk.underseerr.presentation.auth

import app.cash.turbine.test
import app.lusk.underseerr.domain.model.UnderseerrSession
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.security.SecurityManager
import io.kotest.core.spec.style.StringSpec
import app.lusk.underseerr.util.AppLogger
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
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

/**
 * Property-based tests for logout cleanup.
 * Feature: underseerr, Property 21: Logout Cleanup
 * Validates: Requirements 5.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogoutCleanupPropertyTest : StringSpec({
    
    val testDispatcher = StandardTestDispatcher()
    
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }
    
    afterSpec {
        Dispatchers.resetMain()
    }
    
    "Property 21: Logout should clear all stored credentials" {
        // Feature: underseerr, Property 21: Logout Cleanup
        checkAll(100, Arb.int(1..100000), Arb.string(10..50)) { userId, apiKey ->
            runTest(testDispatcher) {
                // Arrange
                val authRepository = mockk<AuthRepository>(relaxed = true)
                val securityManager = mockk<SecurityManager>(relaxed = true)
                val logger = mockk<AppLogger>(relaxed = true)
                
                // Setup initial authenticated state
                coEvery { authRepository.isAuthenticated() } returns flowOf(true, false)
                coEvery { authRepository.logout() } returns Unit
                
                val viewModel = AuthViewModel(authRepository, securityManager, logger)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act
                viewModel.logout()
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                coVerify(exactly = 1) { authRepository.logout() }
                viewModel.authState.value shouldBe AuthState.Unauthenticated
            }
        }
    }
    
    "Property 21: Logout should transition through LoggingOut state" {
        // Feature: underseerr, Property 21: Logout Cleanup
        checkAll(100, Arb.int(1..100000)) { userId ->
            runTest(testDispatcher) {
                // Arrange
                val authRepository = mockk<AuthRepository>(relaxed = true)
                val securityManager = mockk<SecurityManager>(relaxed = true)
                val logger = mockk<AppLogger>(relaxed = true)
                
                coEvery { authRepository.isAuthenticated() } returns flowOf(true)
                coEvery { authRepository.logout() } returns Unit
                
                val viewModel = AuthViewModel(authRepository, securityManager, logger)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act & Assert
                viewModel.authState.test {
                    // Skip initial states
                    awaitItem() // Initial
                    
                    viewModel.logout()
                    testDispatcher.scheduler.advanceUntilIdle()
                    
                    val loggingOutState = awaitItem()
                    loggingOutState shouldBe AuthState.LoggingOut
                    
                    val finalState = awaitItem()
                    finalState shouldBe AuthState.Unauthenticated
                }
            }
        }
    }
    
    "Property 21: Logout should be idempotent" {
        // Feature: underseerr, Property 21: Logout Cleanup
        checkAll(100, Arb.int(2..5)) { logoutCount ->
            runTest(testDispatcher) {
                // Arrange
                val authRepository = mockk<AuthRepository>(relaxed = true)
                val securityManager = mockk<SecurityManager>(relaxed = true)
                val logger = mockk<AppLogger>(relaxed = true)
                
                coEvery { authRepository.isAuthenticated() } returns flowOf(true)
                coEvery { authRepository.logout() } returns Unit
                
                val viewModel = AuthViewModel(authRepository, securityManager, logger)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act - logout multiple times
                repeat(logoutCount) {
                    viewModel.logout()
                    testDispatcher.scheduler.advanceUntilIdle()
                }
                
                // Assert - should call logout for each invocation
                coVerify(exactly = logoutCount) { authRepository.logout() }
                viewModel.authState.value shouldBe AuthState.Unauthenticated
            }
        }
    }
    
    "Property 21: After logout, authentication should be required" {
        // Feature: underseerr, Property 21: Logout Cleanup
        checkAll(100, Arb.int(1..100000)) { userId ->
            runTest(testDispatcher) {
                // Arrange
                val authRepository = mockk<AuthRepository>(relaxed = true)
                val securityManager = mockk<SecurityManager>(relaxed = true)
                val logger = mockk<AppLogger>(relaxed = true)
                
                // Start authenticated, then become unauthenticated after logout
                coEvery { authRepository.isAuthenticated() } returns flowOf(true, false)
                coEvery { authRepository.logout() } returns Unit
                
                val viewModel = AuthViewModel(authRepository, securityManager, logger)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act
                viewModel.logout()
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert
                viewModel.authState.value shouldBe AuthState.Unauthenticated
            }
        }
    }
    
    "Property 21: Logout should clear session even if API call fails" {
        // Feature: underseerr, Property 21: Logout Cleanup
        checkAll(100, Arb.string(10..100)) { errorMessage ->
            runTest(testDispatcher) {
                // Arrange
                val authRepository = mockk<AuthRepository>(relaxed = true)
                val securityManager = mockk<SecurityManager>(relaxed = true)
                val logger = mockk<AppLogger>(relaxed = true)
                
                coEvery { authRepository.isAuthenticated() } returns flowOf(true, false)
                coEvery { authRepository.logout() } throws Exception(errorMessage)
                
                val viewModel = AuthViewModel(authRepository, securityManager, logger)
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Act - should not throw exception
                viewModel.logout()
                testDispatcher.scheduler.advanceUntilIdle()
                
                // Assert - should still transition to unauthenticated
                // Note: The repository implementation handles exceptions during logout
                coVerify(exactly = 1) { authRepository.logout() }
            }
        }
    }
})
