package app.lusk.underseerr.data.security

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for secure logging with redaction.
 * Feature: underseerr, Property 34: Log Redaction
 * Validates: Requirements 8.6
 * 
 * These tests verify that sensitive information is properly redacted from logs.
 */
class SecureLoggerPropertyTest : StringSpec({

    val secureLogger = SecureLogger()

    "Property: API tokens are redacted from logs" {
        val testCases = listOf(
            "api_key=abc123def456" to "api_key=[REDACTED]",
            "apiKey: xyz789" to "apiKey=[REDACTED]",
            "API_TOKEN=secret123" to "API_TOKEN=[REDACTED]",
            "token: bearer_token_here" to "token=[REDACTED]",
            "access_token=\"my_secret_token\"" to "access_token=[REDACTED]"
        )
        
        testCases.forEach { (input, expected) ->
            val redacted = secureLogger.redact(input)
            redacted shouldContain "[REDACTED]"
            // Original token should not be in redacted output
            val tokenValue = input.substringAfter("=").substringAfter(":").trim().trim('"')
            if (tokenValue.isNotEmpty() && tokenValue != input) {
                redacted shouldNotContain tokenValue
            }
        }
    }

    "Property: Passwords are redacted from logs" {
        val testCases = listOf(
            "password=mySecretPass123" to "password=[REDACTED]",
            "passwd: admin123" to "passwd=[REDACTED]",
            "pwd=\"P@ssw0rd\"" to "pwd=[REDACTED]",
            "pass=secret" to "pass=[REDACTED]"
        )
        
        testCases.forEach { (input, _) ->
            val redacted = secureLogger.redact(input)
            redacted shouldContain "[REDACTED]"
            // Original password should not be in redacted output
            val passwordValue = input.substringAfter("=").substringAfter(":").trim().trim('"')
            if (passwordValue.isNotEmpty() && passwordValue != input) {
                redacted shouldNotContain passwordValue
            }
        }
    }

    "Property: Authorization headers are redacted" {
        val testCases = listOf(
            "Authorization: Bearer abc123xyz789" to "[REDACTED]",
            "authorization: bearer token123" to "[REDACTED]",
            "Authorization: abc123" to "[REDACTED]"
        )
        
        testCases.forEach { (input, _) ->
            val redacted = secureLogger.redact(input)
            redacted shouldContain "[REDACTED]"
            // Token part should be redacted
            if (input.contains("Bearer", ignoreCase = true)) {
                val token = input.substringAfter("Bearer", "").trim()
                if (token.isNotEmpty()) {
                    redacted shouldNotContain token
                }
            }
        }
    }

    "Property: Email addresses are partially redacted" {
        val testCases = listOf(
            "user@example.com",
            "john.doe@company.org",
            "admin@test.co.uk"
        )
        
        testCases.forEach { email ->
            val redacted = secureLogger.redact("Email: $email")
            // Should contain the domain but not the full username
            val domain = email.substringAfter("@")
            redacted shouldContain domain
            // Full email should not be present
            redacted shouldNotContain email
        }
    }

    "Property: Credit card numbers are redacted" {
        val testCases = listOf(
            "4532-1234-5678-9010",
            "4532 1234 5678 9010",
            "4532123456789010"
        )
        
        testCases.forEach { ccNumber ->
            val redacted = secureLogger.redact("Card: $ccNumber")
            redacted shouldContain "[REDACTED]"
            redacted shouldNotContain ccNumber
        }
    }

    "Property: Session IDs are redacted" {
        val testCases = listOf(
            "session_id=abc123xyz789",
            "sessionId: def456",
            "sid=\"ghi789\""
        )
        
        testCases.forEach { input ->
            val redacted = secureLogger.redact(input)
            redacted shouldContain "[REDACTED]"
            val sessionValue = input.substringAfter("=").substringAfter(":").trim().trim('"')
            if (sessionValue.isNotEmpty() && sessionValue != input) {
                redacted shouldNotContain sessionValue
            }
        }
    }

    "Property: Secrets are redacted" {
        val testCases = listOf(
            "secret=mySecret123",
            "client_secret: xyz789",
            "clientSecret=\"abc123\""
        )
        
        testCases.forEach { input ->
            val redacted = secureLogger.redact(input)
            redacted shouldContain "[REDACTED]"
            val secretValue = input.substringAfter("=").substringAfter(":").trim().trim('"')
            if (secretValue.isNotEmpty() && secretValue != input) {
                redacted shouldNotContain secretValue
            }
        }
    }

    "Property: JSON sensitive keys are redacted" {
        val jsonExamples = listOf(
            """{"password": "secret123"}""",
            """{"apiKey": "abc123"}""",
            """{"token": "xyz789"}""",
            """{'accessToken': 'bearer123'}"""
        )
        
        jsonExamples.forEach { json ->
            val redacted = secureLogger.redact(json)
            redacted shouldContain "[REDACTED]"
            // Extract the value and ensure it's not in the redacted output
            val valuePattern = Regex("""["']([^"']+)["']""")
            val matches = valuePattern.findAll(json).toList()
            if (matches.size >= 2) {
                val value = matches[1].groupValues[1]
                if (value.isNotEmpty()) {
                    redacted shouldNotContain value
                }
            }
        }
    }

    "Property: Non-sensitive data is not redacted" {
        val safeLogs = listOf(
            "User logged in successfully",
            "Fetching movie list from API",
            "Request completed in 250ms",
            "Cache hit for key: movies_trending",
            "Navigation to detail screen"
        )
        
        safeLogs.forEach { log ->
            val redacted = secureLogger.redact(log)
            // Non-sensitive logs should remain unchanged
            redacted shouldBe log
        }
    }

    "Property: Redaction is idempotent" {
        checkAll(100, Arb.string(10..100)) { message ->
            val redacted1 = secureLogger.redact(message)
            val redacted2 = secureLogger.redact(redacted1)
            
            // Redacting twice should produce the same result
            redacted1 shouldBe redacted2
        }
    }

    "Property: Sensitive data detection works correctly" {
        val sensitiveInputs = listOf(
            "password=secret",
            "api_key=abc123",
            "token: xyz789",
            "Authorization: Bearer token123"
        )
        
        sensitiveInputs.forEach { input ->
            secureLogger.containsSensitiveData(input) shouldBe true
        }
        
        val nonSensitiveInputs = listOf(
            "User logged in",
            "Fetching data",
            "Request completed"
        )
        
        nonSensitiveInputs.forEach { input ->
            secureLogger.containsSensitiveData(input) shouldBe false
        }
    }

    "Property: Multiple sensitive values in same message are all redacted" {
        val message = "Login with password=secret123 and api_key=abc456 and token=xyz789"
        val redacted = secureLogger.redact(message)
        
        // All sensitive values should be redacted
        redacted shouldNotContain "secret123"
        redacted shouldNotContain "abc456"
        redacted shouldNotContain "xyz789"
        
        // Should contain multiple [REDACTED] markers
        val redactedCount = redacted.split("[REDACTED]").size - 1
        redactedCount shouldBe 3
    }

    "Property: Redaction preserves message structure" {
        val message = "User authentication failed: password=wrong_password, attempts=3"
        val redacted = secureLogger.redact(message)
        
        // Should preserve non-sensitive parts
        redacted shouldContain "User authentication failed"
        redacted shouldContain "attempts=3"
        
        // Should redact sensitive parts
        redacted shouldContain "[REDACTED]"
        redacted shouldNotContain "wrong_password"
    }

    "Property: Empty and null-like strings are handled safely" {
        val edgeCases = listOf(
            "",
            " ",
            "null",
            "undefined"
        )
        
        edgeCases.forEach { input ->
            val redacted = secureLogger.redact(input)
            // Should not throw exception
            redacted shouldNotBe null
        }
    }
})
