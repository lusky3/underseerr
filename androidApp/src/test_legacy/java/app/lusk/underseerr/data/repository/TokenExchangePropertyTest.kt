package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.api.AuthApiService
import app.lusk.underseerr.data.remote.api.PlexAuthRequest
import app.lusk.underseerr.data.remote.interceptor.AuthInterceptor
import app.lusk.underseerr.data.remote.model.ApiPermissions
import app.lusk.underseerr.data.remote.model.ApiUserProfile
import app.lusk.underseerr.data.security.SecurityManager
import app.lusk.underseerr.domain.model.Permissions
import app.lusk.underseerr.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Property-based tests for token exchange integrity.
 * Feature: underseerr, Property 2: Token Exchange Integrity
 * Validates: Requirements 1.4
 * 
 * These tests verify that Plex token exchange works correctly.
 */
class TokenExchangePropertyTest : StringSpec({

    "Property: Valid Plex tokens are exchanged successfully" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        checkAll(100, Arb.string(20..50)) { plexToken ->
            val mockUserProfile = ApiUserProfile(
                id = 1,
                email = "user@example.com",
                displayName = "Test User",
                avatar = null,
                requestCount = 0,
                permissions = ApiPermissions(0)
            )
            
            coEvery { authApiService.authenticateWithPlex(PlexAuthRequest(plexToken)) } returns mockUserProfile
            coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
            
            val result = repository.authenticateWithPlex(plexToken)
            
            result.shouldBeInstanceOf<Result.Success<*>>()
            coVerify { authApiService.authenticateWithPlex(PlexAuthRequest(plexToken)) }
        }
    }

    "Property: Token exchange stores credentials securely" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val plexToken = "valid_plex_token_12345"
        val mockUserProfile = ApiUserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = null,
            requestCount = 0,
            permissions = ApiPermissions(0)
        )
        
        coEvery { authApiService.authenticateWithPlex(any()) } returns mockUserProfile
        coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
        
        repository.authenticateWithPlex(plexToken)
        
        // Verify that credentials were stored securely
        coVerify { securityManager.storeSecureData(any(), any()) }
    }

    "Property: Token exchange updates auth interceptor" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val plexToken = "valid_plex_token_12345"
        val mockUserProfile = ApiUserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = null,
            requestCount = 0,
            permissions = ApiPermissions(0)
        )
        
        coEvery { authApiService.authenticateWithPlex(any()) } returns mockUserProfile
        coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
        
        repository.authenticateWithPlex(plexToken)
        
        // Verify that auth interceptor was updated
        coVerify { authInterceptor.setApiKey(any()) }
    }

    "Property: Token exchange stores user ID" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        checkAll(100, Arb.int(1..10000)) { userId ->
            val mockUserProfile = ApiUserProfile(
                id = userId,
                email = "user@example.com",
                displayName = "Test User",
                avatar = null,
                requestCount = 0,
                permissions = ApiPermissions(0)
            )
            
            coEvery { authApiService.authenticateWithPlex(any()) } returns mockUserProfile
            coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
            
            repository.authenticateWithPlex("token")
            
            coVerify { preferencesManager.setUserId(userId) }
        }
    }

    "Property: Token exchange returns user profile" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val plexToken = "valid_plex_token"
        val mockUserProfile = ApiUserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = "https://example.com/avatar.jpg",
            requestCount = 5,
            permissions = ApiPermissions(0)
        )
        
        coEvery { authApiService.authenticateWithPlex(any()) } returns mockUserProfile
        coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
        
        val result = repository.authenticateWithPlex(plexToken)
        
        result.shouldBeInstanceOf<Result.Success<*>>()
        val userProfile = result.getOrNull()
        userProfile shouldNotBe null
        userProfile?.id shouldBe 1
        userProfile?.email shouldBe "user@example.com"
        userProfile?.displayName shouldBe "Test User"
    }

    "Property: Failed token exchange returns error" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val invalidToken = "invalid_token"
        
        coEvery { authApiService.authenticateWithPlex(any()) } throws Exception("Invalid token")
        
        val result = repository.authenticateWithPlex(invalidToken)
        
        result.shouldBeInstanceOf<Result.Error>()
    }

    "Property: Token exchange is idempotent for same token" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val plexToken = "same_token"
        val mockUserProfile = ApiUserProfile(
            id = 1,
            email = "user@example.com",
            displayName = "Test User",
            avatar = null,
            requestCount = 0,
            permissions = ApiPermissions(0)
        )
        
        coEvery { authApiService.authenticateWithPlex(any()) } returns mockUserProfile
        coEvery { securityManager.storeSecureData(any(), any()) } returns Unit
        
        val result1 = repository.authenticateWithPlex(plexToken)
        val result2 = repository.authenticateWithPlex(plexToken)
        
        result1.shouldBeInstanceOf<Result.Success<*>>()
        result2.shouldBeInstanceOf<Result.Success<*>>()
        
        result1.getOrNull()?.id shouldBe result2.getOrNull()?.id
    }

    "Property: Empty token is rejected" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        coEvery { authApiService.authenticateWithPlex(any()) } throws Exception("Empty token")
        
        val result = repository.authenticateWithPlex("")
        
        result.shouldBeInstanceOf<Result.Error>()
    }
})
