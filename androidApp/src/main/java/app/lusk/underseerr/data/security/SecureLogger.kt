package app.lusk.underseerr.data.security

import android.util.Log


/**
 * Secure logging wrapper that redacts sensitive information from logs.
 * Feature: underseerr, Property 34: Log Redaction
 * Validates: Requirements 8.6
 * 
 * Automatically redacts tokens, passwords, API keys, and other sensitive data from logs.
 */
class SecureLogger {
    
    companion object {
        private const val REDACTED = "[REDACTED]"
        
        // Patterns for sensitive data
        private val SENSITIVE_PATTERNS = listOf(
            // API tokens and keys
            Regex("""(?i)(api[_-]?key|apikey|api[_-]?token|token|access[_-]?token|auth[_-]?token|bearer)\s*[:=]\s*['"]?([a-zA-Z0-9_\-\.]+)['"]?"""),
            // Passwords
            Regex("""(?i)(password|passwd|pwd|pass)\s*[:=]\s*['"]?([^\s'"]+)['"]?"""),
            // Authorization headers
            Regex("""(?i)(authorization)\s*:\s*['"]?(bearer\s+)?([a-zA-Z0-9_\-\.]+)['"]?"""),
            // Email addresses (partial redaction)
            Regex("""([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})"""),
            // Credit card numbers
            Regex("""\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b"""),
            // Social security numbers
            Regex("""\b\d{3}-\d{2}-\d{4}\b"""),
            // Phone numbers (US format)
            Regex("""\b\d{3}[-.]?\d{3}[-.]?\d{4}\b"""),
            // IP addresses (partial redaction)
            Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b"""),
            // Session IDs
            Regex("""(?i)(session[_-]?id|sessionid|sid)\s*[:=]\s*['"]?([a-zA-Z0-9_\-]+)['"]?"""),
            // Private keys
            Regex("""(?i)(private[_-]?key|privatekey)\s*[:=]\s*['"]?([^\s'"]+)['"]?"""),
            // Secrets
            Regex("""(?i)(secret|client[_-]?secret)\s*[:=]\s*['"]?([a-zA-Z0-9_\-\.]+)['"]?"""),
        )
        
        // Additional patterns for JSON/XML
        private val JSON_SENSITIVE_KEYS = setOf(
            "password", "token", "apiKey", "api_key", "apiToken", "api_token",
            "accessToken", "access_token", "authToken", "auth_token",
            "secret", "clientSecret", "client_secret", "privateKey", "private_key",
            "sessionId", "session_id", "authorization", "bearer"
        )
    }
    
    /**
     * Redact sensitive information from a message.
     * 
     * @param message The message to redact
     * @return Redacted message with sensitive data replaced
     */
    fun redact(message: String): String {
        var redacted = applySensitivePatterns(message)
        redacted = redactJsonSensitiveKeys(redacted)
        return redacted
    }
    
    /**
     * Apply all regex patterns to redact sensitive data.
     */
    private fun applySensitivePatterns(message: String): String {
        var redacted = message
        SENSITIVE_PATTERNS.forEach { pattern ->
            redacted = pattern.replace(redacted) { matchResult ->
                redactMatchResult(matchResult)
            }
        }
        return redacted
    }
    
    /**
     * Process a single regex match result and return the redacted string.
     */
    private fun redactMatchResult(matchResult: MatchResult): String {
        // For patterns with key-value pairs (3 groups), keep the key but redact the value
        if (matchResult.groupValues.size == 3) {
            return "${matchResult.groupValues[1]}=$REDACTED"
        }
        
        // For email addresses, partially redact
        if (matchResult.value.contains("@")) {
            return redactEmailAddress(matchResult.value)
        }
        
        return REDACTED
    }
    
    /**
     * Partially redact an email address, keeping first 2 characters of username.
     */
    private fun redactEmailAddress(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) {
            return REDACTED
        }
        
        val username = parts[0]
        val domain = parts[1]
        val redactedUsername = if (username.length > 2) {
            username.take(2) + "***"
        } else {
            "***"
        }
        return "$redactedUsername@$domain"
    }
    
    /**
     * Redact JSON-like sensitive keys in the message.
     */
    private fun redactJsonSensitiveKeys(message: String): String {
        var redacted = message
        JSON_SENSITIVE_KEYS.forEach { key ->
            // Match "key": "value" or 'key': 'value' or key=value
            val jsonPattern = Regex("""["']?$key["']?\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            redacted = jsonPattern.replace(redacted) { matchResult ->
                val prefix = matchResult.value.substringBefore(matchResult.groupValues[1])
                val suffix = matchResult.value.substringAfter(matchResult.groupValues[1])
                "$prefix$REDACTED$suffix"
            }
        }
        return redacted
    }
    
    /**
     * Log verbose message with redaction.
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (Log.isLoggable(tag, Log.VERBOSE)) {
            val redactedMessage = redact(message)
            if (throwable != null) {
                Log.v(tag, redactedMessage, throwable)
            } else {
                Log.v(tag, redactedMessage)
            }
        }
    }
    
    /**
     * Log debug message with redaction.
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            val redactedMessage = redact(message)
            if (throwable != null) {
                Log.d(tag, redactedMessage, throwable)
            } else {
                Log.d(tag, redactedMessage)
            }
        }
    }
    
    /**
     * Log info message with redaction.
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val redactedMessage = redact(message)
        if (throwable != null) {
            Log.i(tag, redactedMessage, throwable)
        } else {
            Log.i(tag, redactedMessage)
        }
    }
    
    /**
     * Log warning message with redaction.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val redactedMessage = redact(message)
        if (throwable != null) {
            Log.w(tag, redactedMessage, throwable)
        } else {
            Log.w(tag, redactedMessage)
        }
    }
    
    /**
     * Log error message with redaction.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val redactedMessage = redact(message)
        if (throwable != null) {
            Log.e(tag, redactedMessage, throwable)
        } else {
            Log.e(tag, redactedMessage)
        }
    }
    
    /**
     * Check if a string contains potentially sensitive data.
     * 
     * @param text The text to check
     * @return true if sensitive data patterns are detected
     */
    fun containsSensitiveData(text: String): Boolean {
        return SENSITIVE_PATTERNS.any { it.containsMatchIn(text) } ||
               JSON_SENSITIVE_KEYS.any { key ->
                   text.contains(key, ignoreCase = true)
               }
    }
}
