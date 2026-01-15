package app.lusk.client.data.repository

import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.data.remote.api.AuthApiService
import app.lusk.client.data.remote.api.PlexAuthRequest
import app.lusk.client.data.remote.interceptor.AuthInterceptor
import app.lusk.client.data.remote.model.ApiServerInfo
import app.lusk.client.data.security.SecurityManager
import app.lusk.client.domain.model.Result
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Property-based tests for URL validation.
 * Feature: overseerr-android-client, Property 1: URL Validation Correctness
 * Validates: Requirements 1.2
 * 
 * These tests verify that URL validation correctly identifies valid and invalid URLs.
 */
class UrlValidationPropertyTest : StringSpec({

    "Property: Valid HTTPS URLs are accepted" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val validUrls = listOf(
            "https://overseerr.example.com",
            "https://overseerr.example.com:5055",
            "https://192.168.1.100:5055",
            "https://localhost:5055",
            "https://my-server.local"
        )
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        validUrls.forEach { url ->
            val result = repository.validateServerUrl(url)
            
            result.shouldBeInstanceOf<Result.Success<*>>()
            coVerify { preferencesManager.setServerUrl(url) }
        }
    }

    "Property: HTTP URLs are rejected for security" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val httpUrls = listOf(
            "http://overseerr.example.com",
            "http://192.168.1.100:5055",
            "http://localhost:5055"
        )
        
        httpUrls.forEach { url ->
            val result = repository.validateServerUrl(url)
            
            result.shouldBeInstanceOf<Result.Error>()
            val error = result.errorOrNull()
            error shouldNotBe null
        }
    }

    "Property: Malformed URLs are rejected" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val invalidUrls = listOf(
            "not a url",
            "://example.com",
            "",
            "ftp://example.com",
            "example.com",
            "https://",
            "https://"
        )
        
        invalidUrls.forEach { url ->
            val result = repository.validateServerUrl(url)
            
            result.shouldBeInstanceOf<Result.Error>()
        }
    }

    "Property: URL validation checks connectivity" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val validUrl = "https://overseerr.example.com"
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        val result = repository.validateServerUrl(validUrl)
        
        result.shouldBeInstanceOf<Result.Success<*>>()
        coVerify { authApiService.getServerInfo() }
    }

    "Property: URL with port numbers are handled correctly" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        checkAll(10, Arb.int(1000..9999)) { port ->
            val url = "https://overseerr.example.com:$port"
            
            coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
            
            val result = repository.validateServerUrl(url)
            
            result.shouldBeInstanceOf<Result.Success<*>>()
        }
    }

    "Property: URL validation stores valid URLs" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val validUrl = "https://overseerr.example.com:5055"
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        repository.validateServerUrl(validUrl)
        
        coVerify { preferencesManager.setServerUrl(validUrl) }
    }

    "Property: URL validation does not store invalid URLs" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val invalidUrl = "not a url"
        
        repository.validateServerUrl(invalidUrl)
        
        coVerify(exactly = 0) { preferencesManager.setServerUrl(any()) }
    }

    "Property: URL scheme is case-insensitive" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val urls = listOf(
            "HTTPS://overseerr.example.com",
            "Https://overseerr.example.com",
            "https://overseerr.example.com"
        )
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        urls.forEach { url ->
            val result = repository.validateServerUrl(url)
            result.shouldBeInstanceOf<Result.Success<*>>()
        }
    }

    "Property: URL with paths are handled correctly" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val urlsWithPaths = listOf(
            "https://overseerr.example.com/api",
            "https://overseerr.example.com/path/to/api"
        )
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        urlsWithPaths.forEach { url ->
            val result = repository.validateServerUrl(url)
            result.shouldBeInstanceOf<Result.Success<*>>()
        }
    }

    "Property: IPv4 addresses are valid hosts" {
        val authApiService = mockk<AuthApiService>()
        val securityManager = mockk<SecurityManager>(relaxed = true)
        val preferencesManager = mockk<PreferencesManager>(relaxed = true)
        val authInterceptor = mockk<AuthInterceptor>(relaxed = true)
        val repository = AuthRepositoryImpl(authApiService, securityManager, preferencesManager, authInterceptor)
        
        val ipUrls = listOf(
            "https://192.168.1.1",
            "https://10.0.0.1:5055",
            "https://172.16.0.1"
        )
        
        coEvery { authApiService.getServerInfo() } returns ApiServerInfo("1.0.0", "OK")
        
        ipUrls.forEach { url ->
            val result = repository.validateServerUrl(url)
            result.shouldBeInstanceOf<Result.Success<*>>()
        }
    }
})
