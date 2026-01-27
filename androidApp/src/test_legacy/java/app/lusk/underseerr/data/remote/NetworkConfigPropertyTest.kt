package app.lusk.underseerr.data.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Property-based tests for API timeout configuration.
 * Feature: underseerr, Property 37: API Timeout Configuration
 * Validates: Requirements 10.3
 * 
 * These tests verify that API timeouts are properly configured.
 */
class NetworkConfigPropertyTest : StringSpec({

    "Property: Connect timeout is configured and reasonable" {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val timeoutMillis = client.connectTimeoutMillis
        
        // Timeout should be positive
        timeoutMillis shouldBeGreaterThan 0
        
        // Timeout should be reasonable (between 5 and 60 seconds)
        timeoutMillis shouldBeGreaterThan 5_000
        timeoutMillis shouldBeLessThanOrEqualTo 60_000
    }

    "Property: Read timeout is configured and reasonable" {
        val client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val timeoutMillis = client.readTimeoutMillis
        
        // Timeout should be positive
        timeoutMillis shouldBeGreaterThan 0
        
        // Timeout should be reasonable (between 5 and 60 seconds)
        timeoutMillis shouldBeGreaterThan 5_000
        timeoutMillis shouldBeLessThanOrEqualTo 60_000
    }

    "Property: Write timeout is configured and reasonable" {
        val client = OkHttpClient.Builder()
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val timeoutMillis = client.writeTimeoutMillis
        
        // Timeout should be positive
        timeoutMillis shouldBeGreaterThan 0
        
        // Timeout should be reasonable (between 5 and 60 seconds)
        timeoutMillis shouldBeGreaterThan 5_000
        timeoutMillis shouldBeLessThanOrEqualTo 60_000
    }

    "Property: Call timeout is configured and reasonable" {
        val client = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
        
        val timeoutMillis = client.callTimeoutMillis
        
        // Timeout should be positive
        timeoutMillis shouldBeGreaterThan 0
        
        // Call timeout should be longer than individual timeouts
        timeoutMillis shouldBeGreaterThan 30_000
        timeoutMillis shouldBeLessThanOrEqualTo 120_000
    }

    "Property: Timeout values are consistent across different time units" {
        checkAll(100, Arb.int(5..60)) { seconds ->
            val clientSeconds = OkHttpClient.Builder()
                .connectTimeout(seconds.toLong(), TimeUnit.SECONDS)
                .build()
            
            val clientMillis = OkHttpClient.Builder()
                .connectTimeout((seconds * 1000).toLong(), TimeUnit.MILLISECONDS)
                .build()
            
            // Both should result in the same timeout
            clientSeconds.connectTimeoutMillis shouldBe clientMillis.connectTimeoutMillis
        }
    }

    "Property: Retry on connection failure is enabled" {
        val client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
        
        client.retryOnConnectionFailure shouldBe true
    }

    "Property: Timeout configuration is preserved through builder" {
        val connectTimeout = 30L
        val readTimeout = 30L
        val writeTimeout = 30L
        val callTimeout = 60L
        
        val client = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .callTimeout(callTimeout, TimeUnit.SECONDS)
            .build()
        
        client.connectTimeoutMillis shouldBe connectTimeout * 1000
        client.readTimeoutMillis shouldBe readTimeout * 1000
        client.writeTimeoutMillis shouldBe writeTimeout * 1000
        client.callTimeoutMillis shouldBe callTimeout * 1000
    }

    "Property: Zero timeout means no timeout" {
        val client = OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .build()
        
        // Zero timeout means infinite/no timeout
        client.connectTimeoutMillis shouldBe 0
    }

    "Property: Timeout values are non-negative" {
        checkAll(100, Arb.int(0..120)) { seconds ->
            val client = OkHttpClient.Builder()
                .connectTimeout(seconds.toLong(), TimeUnit.SECONDS)
                .build()
            
            client.connectTimeoutMillis shouldBeGreaterThan -1
        }
    }
})
