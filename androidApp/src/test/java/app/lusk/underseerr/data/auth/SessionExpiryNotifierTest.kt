package app.lusk.underseerr.data.auth

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The involuntary-sign-out flow is a state machine shared between
 * [SessionRefresher] (the producer) and two consumers that cannot see each other:
 * the nav host, which routes to sign-in, and PlexAuthScreen, which explains why.
 *
 * The producer side is covered by SessionRefreshTest. These tests pin the
 * contract the *consumers* depend on — specifically that this is a `StateFlow`
 * with a retained current value, not a one-shot event. The nav host subscribes
 * from a `LaunchedEffect`, i.e. only once composition runs; if the expiry fired
 * before that (it can — the refresh attempt that clears credentials is triggered
 * by a background request), a non-replaying stream would drop it and strand the
 * user on a screen whose every request now 403s.
 */
class SessionExpiryNotifierTest {

    @Test
    fun `starts with no expiry so a fresh launch does not look signed out`() {
        assertNull(SessionExpiryNotifier().reason.value)
    }

    @Test
    fun `notifyExpired publishes the reason`() {
        val notifier = SessionExpiryNotifier()

        notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)

        assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, notifier.reason.value)
    }

    @Test
    fun `consume clears the reason so the banner disappears after signing back in`() {
        val notifier = SessionExpiryNotifier()
        notifier.notifyExpired(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH)

        notifier.consume()

        assertNull(notifier.reason.value)
    }

    @Test
    fun `consume on a clean notifier is a no-op`() {
        val notifier = SessionExpiryNotifier()

        notifier.consume()
        notifier.consume()

        assertNull(notifier.reason.value)
    }

    /**
     * The load-bearing replay guarantee. The nav host's collector starts late; it
     * must still observe an expiry that happened before composition.
     */
    @Test
    fun `a collector that subscribes after the expiry still receives it`() = runTest {
        val notifier = SessionExpiryNotifier()
        notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)

        notifier.reason.test {
            assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * PlexAuthScreen and the nav host collect independently. Neither may consume
     * the value out from under the other.
     */
    @Test
    fun `two independent collectors both see the same live reason`() = runTest {
        val notifier = SessionExpiryNotifier()
        notifier.notifyExpired(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH)

        notifier.reason.test {
            assertEquals(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        notifier.reason.test {
            assertEquals(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an already-subscribed collector observes expiry then consume`() = runTest {
        val notifier = SessionExpiryNotifier()

        notifier.reason.test {
            assertNull(awaitItem())

            notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, awaitItem())

            notifier.consume()
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a later reason replaces an earlier one`() {
        val notifier = SessionExpiryNotifier()

        notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
        notifier.notifyExpired(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH)

        assertEquals(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH, notifier.reason.value)
    }

    /**
     * Documented conflation: repeating the *same* reason does not re-emit, so the
     * nav guard will not fire twice for a duplicated notification. That is the
     * desired behaviour (the user is already being routed to sign-in), but it also
     * means a re-expiry after a `consume()` DOES re-emit — checked below.
     */
    @Test
    fun `repeating the same reason does not re-emit`() = runTest {
        val notifier = SessionExpiryNotifier()

        notifier.reason.test {
            assertNull(awaitItem())

            notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, awaitItem())

            notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expiring again after consume re-emits so a second sign-out still routes`() = runTest {
        val notifier = SessionExpiryNotifier()

        notifier.reason.test {
            assertNull(awaitItem())

            notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, awaitItem())

            notifier.consume()
            assertNull(awaitItem())

            notifier.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The exposed flow must stay read-only: a consumer that could write to it
     * could clear the reason without the user having actually signed in.
     */
    @Test
    fun `the exposed flow cannot be cast back to a mutable flow`() {
        val exposed: StateFlow<SessionExpiryReason?> = SessionExpiryNotifier().reason

        assertFalse(exposed is MutableStateFlow<*>)
    }

    @Test
    fun `notifiers do not share state`() {
        val a = SessionExpiryNotifier()
        val b = SessionExpiryNotifier()

        a.notifyExpired(SessionExpiryReason.PLEX_TOKEN_REVOKED)

        assertTrue(a.reason.value != null && b.reason.value == null)
    }
}
