package app.lusk.underseerr.data.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `userMessage` is the only thing the user ever sees explaining why the app signed
 * them out, so it is a product surface, not a debug string.
 *
 * Every test here iterates [SessionExpiryReason.entries] rather than naming cases.
 * Combined with the exhaustive `when` in the production mapping (which fails to
 * compile if a new enum constant is unhandled), that means a newly added reason
 * cannot ship with a blank, duplicated, or internals-leaking message.
 */
class SessionExpiryUserMessageTest {

    /**
     * Words that mean something to us and nothing to a user, plus the HTTP codes
     * Overseerr answers with for a dead session. If one of these shows up in a
     * message, an implementation detail escaped into the UI.
     */
    private val leakedInternals = listOf(
        "token",
        "cookie",
        "connect.sid",
        "401",
        "403",
        "http",
        "null",
        "exception",
        "credential",
        "api",
        "endpoint",
        "refresh"
    )

    @Test
    fun `there is at least one reason to test`() {
        assertTrue(SessionExpiryReason.entries.isNotEmpty())
    }

    @Test
    fun `every reason has a non-blank message`() {
        SessionExpiryReason.entries.forEach { reason ->
            assertTrue(
                reason.userMessage.isNotBlank(),
                "$reason has a blank userMessage"
            )
        }
    }

    @Test
    fun `every reason has a distinct message`() {
        val messages = SessionExpiryReason.entries.map { it.userMessage }

        assertEquals(
            SessionExpiryReason.entries.size,
            messages.distinct().size,
            "Two reasons share a message, so the explanation cannot be right for both: $messages"
        )
    }

    @Test
    fun `no message leaks an implementation detail`() {
        SessionExpiryReason.entries.forEach { reason ->
            val lowered = reason.userMessage.lowercase()
            leakedInternals.forEach { term ->
                assertFalse(
                    lowered.contains(term),
                    "${reason.name} message leaks \"$term\": ${reason.userMessage}"
                )
            }
        }
    }

    @Test
    fun `no message exposes the enum constant name`() {
        SessionExpiryReason.entries.forEach { reason ->
            val message = reason.userMessage

            assertFalse(
                message.contains(reason.name, ignoreCase = true),
                "${reason.name} message echoes the enum name: $message"
            )
            assertFalse(
                message.contains('_'),
                "${reason.name} message looks like an identifier, not prose: $message"
            )
        }
    }

    @Test
    fun `every message tells the user to sign in again`() {
        SessionExpiryReason.entries.forEach { reason ->
            assertTrue(
                reason.userMessage.contains("sign in", ignoreCase = true),
                "${reason.name} message gives no next step: ${reason.userMessage}"
            )
        }
    }

    @Test
    fun `every message reads as a sentence`() {
        SessionExpiryReason.entries.forEach { reason ->
            val message = reason.userMessage

            assertEquals(
                message.trim(),
                message,
                "${reason.name} message has stray whitespace: \"$message\""
            )
            assertTrue(
                message.first().isUpperCase(),
                "${reason.name} message does not start with a capital: $message"
            )
            assertTrue(
                message.last() in charArrayOf('.', '!'),
                "${reason.name} message is not punctuated: $message"
            )
        }
    }

    /**
     * A banner, not a log line. Long enough to explain, short enough to read on a
     * phone without the card swallowing the sign-in button.
     */
    @Test
    fun `every message is banner-sized`() {
        SessionExpiryReason.entries.forEach { reason ->
            val length = reason.userMessage.length

            assertTrue(
                length in 20..160,
                "${reason.name} message is $length chars, outside 20..160: ${reason.userMessage}"
            )
        }
    }

    /**
     * A revoked Plex token is a different user problem from a plain lapsed session
     * (one needs re-authorising at plex.tv, the other just needs signing in), so
     * only that message should name Plex.
     */
    @Test
    fun `only the revoked-Plex-sign-in case names Plex`() {
        assertTrue(
            SessionExpiryReason.PLEX_TOKEN_REVOKED.userMessage.contains("Plex"),
            "The revoked case should say which sign-in went stale"
        )
        assertFalse(
            SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH.userMessage.contains("Plex"),
            "A generic lapsed session should not blame Plex"
        )
    }
}
