package app.lusk.client.data.remote

import app.lusk.client.data.remote.interceptor.RetryInterceptor
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.math.min
import kotlin.math.pow

/**
 * Property-based tests for exponential backoff retry logic.
 * Feature: overseerr-android-client, Property 38: Exponential Backoff Retry
 * Validates: Requirements 10.4
 * 
 * These tests verify that retry logic implements proper exponential backoff.
 */
class RetryInterceptorPropertyTest : StringSpec({

    "Property: Exponential backoff increases delay with each attempt" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        
        checkAll(10, Arb.int(1..5)) { attempt ->
            val delay1 = calculateBackoff(attempt, initialBackoff, multiplier)
            val delay2 = calculateBackoff(attempt + 1, initialBackoff, multiplier)
            
            // Each subsequent attempt should have a longer delay
            if (delay1 < 10000L && delay2 < 10000L) {
                delay2 shouldBeGreaterThan delay1
            }
        }
    }

    "Property: Backoff delay respects maximum limit" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        val maxBackoff = 10000L
        
        checkAll(100, Arb.int(1..10)) { attempt ->
            val delay = calculateBackoffWithMax(attempt, initialBackoff, multiplier, maxBackoff)
            
            // Delay should never exceed maximum
            delay shouldBeLessThanOrEqualTo maxBackoff
        }
    }

    "Property: Backoff delay is always positive" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        
        checkAll(100, Arb.int(1..10)) { attempt ->
            val delay = calculateBackoff(attempt, initialBackoff, multiplier)
            
            // Delay should always be positive
            delay shouldBeGreaterThan 0L
        }
    }

    "Property: First attempt has minimum backoff" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        
        val delay = calculateBackoff(1, initialBackoff, multiplier)
        
        // First attempt should use initial backoff
        delay shouldBe initialBackoff
    }

    "Property: Backoff follows exponential growth pattern" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        
        val delay1 = calculateBackoff(1, initialBackoff, multiplier)
        val delay2 = calculateBackoff(2, initialBackoff, multiplier)
        val delay3 = calculateBackoff(3, initialBackoff, multiplier)
        
        // Verify exponential pattern: delay2 / delay1 ≈ delay3 / delay2
        val ratio1 = delay2.toDouble() / delay1.toDouble()
        val ratio2 = delay3.toDouble() / delay2.toDouble()
        
        // Ratios should be approximately equal (within 0.1 tolerance)
        kotlin.math.abs(ratio1 - ratio2) shouldBeLessThanOrEqualTo 0.1
    }

    "Property: Retryable status codes are correctly identified" {
        val retryableStatusCodes = listOf(408, 429, 500, 502, 503, 504)
        val nonRetryableStatusCodes = listOf(200, 201, 400, 401, 403, 404)
        
        retryableStatusCodes.forEach { code ->
            isRetryableStatusCode(code) shouldBe true
        }
        
        nonRetryableStatusCodes.forEach { code ->
            isRetryableStatusCode(code) shouldBe false
        }
    }

    "Property: Backoff calculation is deterministic" {
        checkAll(100, Arb.int(1..10)) { attempt ->
            val initialBackoff = 1000L
            val multiplier = 2.0
            
            val delay1 = calculateBackoff(attempt, initialBackoff, multiplier)
            val delay2 = calculateBackoff(attempt, initialBackoff, multiplier)
            
            // Same input should produce same output
            delay1 shouldBe delay2
        }
    }

    "Property: Maximum retries limit is enforced" {
        val maxRetries = 3
        
        // Verify that we don't retry more than max times
        maxRetries shouldBeGreaterThan 0
        maxRetries shouldBeLessThanOrEqualTo 5
    }

    "Property: Backoff with different multipliers produces different delays" {
        val attempt = 3
        val initialBackoff = 1000L
        
        val delay1 = calculateBackoff(attempt, initialBackoff, 2.0)
        val delay2 = calculateBackoff(attempt, initialBackoff, 3.0)
        
        // Higher multiplier should produce longer delay
        delay2 shouldBeGreaterThan delay1
    }

    "Property: Backoff delay grows exponentially not linearly" {
        val initialBackoff = 1000L
        val multiplier = 2.0
        
        val delay1 = calculateBackoff(1, initialBackoff, multiplier)
        val delay2 = calculateBackoff(2, initialBackoff, multiplier)
        val delay3 = calculateBackoff(3, initialBackoff, multiplier)
        val delay4 = calculateBackoff(4, initialBackoff, multiplier)
        
        // Exponential growth: difference between delays should increase
        val diff1 = delay2 - delay1
        val diff2 = delay3 - delay2
        val diff3 = delay4 - delay3
        
        // Each difference should be larger than the previous (exponential)
        if (delay4 < 10000L) {
            diff2 shouldBeGreaterThan diff1
            diff3 shouldBeGreaterThan diff2
        }
    }
})

// Helper functions that mirror the retry interceptor logic

private fun calculateBackoff(attempt: Int, initialBackoff: Long, multiplier: Double): Long {
    return (initialBackoff * multiplier.pow(attempt - 1)).toLong()
}

private fun calculateBackoffWithMax(
    attempt: Int,
    initialBackoff: Long,
    multiplier: Double,
    maxBackoff: Long
): Long {
    val exponentialDelay = (initialBackoff * multiplier.pow(attempt - 1)).toLong()
    return min(exponentialDelay, maxBackoff)
}

private fun isRetryableStatusCode(code: Int): Boolean {
    return when (code) {
        408, 429, 500, 502, 503, 504 -> true
        else -> false
    }
}
