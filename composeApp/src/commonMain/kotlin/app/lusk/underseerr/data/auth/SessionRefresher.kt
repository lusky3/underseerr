package app.lusk.underseerr.data.auth

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.api.PlexAuthRequest
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Marks a request that must never trigger a session refresh. Without this the
 * refresh call itself would recurse when it fails with 401/403.
 */
val SkipSessionRefresh = AttributeKey<Boolean>("SkipSessionRefresh")

/**
 * Re-mints an expired Overseerr session from the stored Plex token.
 *
 * Overseerr authenticates via a `connect.sid` session cookie. That cookie is
 * captured once at login and has a finite server-side lifetime; when it lapses,
 * Overseerr answers protected endpoints with 403 (not 401 — see its
 * `isAuthenticated` middleware). Previously nothing in the app reacted to that,
 * so the session stayed "valid" locally forever.
 *
 * Recovery is three-tiered:
 *  1. Probe `/api/v1/auth/me` to find out whether the session is actually dead.
 *     Overseerr also answers 403 for *insufficient permissions* on a perfectly
 *     healthy session (`/api/v1/settings/main` is ADMIN-only and is hit on every
 *     app start; `/api/v1/service/radarr|sonarr` need MANAGE_REQUESTS), so the
 *     caller's 403 on its own says nothing about the session.
 *  2. POST the stored Plex token back to `/api/v1/auth/plex` for a fresh cookie.
 *  3. If that fails the Plex token itself is dead — clear credentials so the app
 *     drops back to the login screen instead of silently serving stale cache.
 */
class SessionRefresher(
    private val securityManager: SecurityManager,
    preferencesManager: PreferencesManager,
    private val logger: AppLogger,
    private val expiryNotifier: SessionExpiryNotifier,
    /**
     * Shared with the explicit logout so an involuntary sign-out wipes exactly as
     * much as a deliberate one. Deliberately has no default: a caller that forgot
     * to supply one would silently reinstate the credentials-only sign-out this
     * class exists to prevent.
     */
    private val sessionCleaner: SessionCleaner,
    private val clientProvider: () -> HttpClient
) {
    private val mutex = Mutex()
    private val timeSource: TimeSource = TimeSource.Monotonic

    /**
     * The cookie a `/api/v1/auth/me` probe most recently proved to be alive, and
     * when it was proved.
     *
     * Caching the *attempted* cookie would not be enough on its own: a successful
     * refresh mints a new cookie, so the next permission-403 would carry the new
     * value and re-auth again, and so on without bound. What has to be remembered
     * is the positive result — "this exact cookie works" — so a burst of
     * permission-403s carrying it costs one probe between them, not one each.
     *
     * The result expires after [VERIFICATION_TTL] because a session proved alive
     * at launch will eventually lapse while the process is still running, and a
     * permanently cached "it's fine" would leave that user with a dead session
     * nothing ever tries to refresh.
     *
     * Only ever touched under [mutex].
     */
    private var verifiedCookie: String? = null
    private var verifiedAt: TimeMark? = null

    /**
     * Whether a sign-out has already been performed and not yet acted on by the UI.
     *
     * Concurrent auth failures all reach [invalidate]; without this the whole
     * sign-out (including `clearAllTables()`) would run once per coroutine and the
     * *last* one to arrive would overwrite the reason — the user would be told
     * "your session expired" when the truth was "your Plex sign-in is no longer
     * valid". Only ever touched under [mutex].
     */
    private var invalidated = false

    suspend fun currentCookie(): String? = securityManager.retrieveSecureData(COOKIE_KEY)

    /**
     * Attempts to restore a usable session.
     *
     * @param staleCookie the cookie the caller sent when it received the auth
     *   failure. If storage already holds a different cookie, another coroutine
     *   refreshed while this one waited on the mutex and we reuse that result
     *   rather than burning a second login.
     * @return true when a fresh cookie is available for a retry.
     */
    suspend fun refresh(staleCookie: String?): Boolean = mutex.withLock {
        val stored = securityManager.retrieveSecureData(COOKIE_KEY)
        if (!stored.isNullOrEmpty() && stored != staleCookie) {
            logger.d(TAG, "Session already refreshed by a concurrent request; reusing it.")
            return@withLock true
        }

        // Establish that the session is genuinely dead before spending a full
        // re-auth (an outbound plex.tv call from the user's own server) on it.
        // Held inside the mutex so a burst of concurrent 403s shares one probe.
        if (!stored.isNullOrEmpty() && !isSessionDead(stored)) return@withLock false

        val plexToken = securityManager.retrieveSecureData(PLEX_TOKEN_KEY)
        if (plexToken.isNullOrEmpty()) {
            logger.w(TAG, "Session rejected and no Plex token stored — forcing re-login.")
            invalidate(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH)
            return@withLock false
        }

        val response = try {
            clientProvider().post("/api/v1/auth/plex") {
                attributes.put(SkipSessionRefresh, true)
                setBody(PlexAuthRequest(plexToken))
            }
        } catch (e: CancellationException) {
            // A cancelled coroutine is not a failed refresh. On the JVM
            // CancellationException is an IllegalStateException, so the broad
            // catch below would otherwise swallow it and turn a cancellation into
            // "refresh said no" — surfacing the original 403 to the caller.
            throw e
        } catch (e: Exception) {
            // Only give up the session when the server actively rejected the
            // Plex token. A network blip must not log the user out.
            val status = (e as? ResponseException)?.response?.status?.value
            if (status == 401 || status == 403) {
                logger.w(TAG, "Plex token rejected by Overseerr ($status) — forcing re-login.")
                invalidate(SessionExpiryReason.PLEX_TOKEN_REVOKED)
            } else {
                logger.e(TAG, "Session refresh failed transiently: ${e.message}", e)
            }
            return@withLock false
        }

        val cookie = extractSessionCookie(response)
        if (cookie == null) {
            logger.e(TAG, "Re-auth succeeded but returned no Set-Cookie; cannot refresh session.")
            return@withLock false
        }

        securityManager.storeSecureData(COOKIE_KEY, cookie)
        securityManager.storeSecureData(API_KEY_STORAGE_KEY, SESSION_MARKER)
        // The new cookie has not been probed, and the app is signed in again.
        verifiedCookie = null
        verifiedAt = null
        invalidated = false
        logger.d(TAG, "Overseerr session refreshed from stored Plex token.")
        true
    }

    /**
     * Answers "is [cookie] actually rejected by the server?" rather than assuming
     * it from the caller's 403.
     *
     * Anything other than a hard 401/403 from the probe is treated as "not dead":
     * a network failure must never escalate into a sign-out.
     */
    private suspend fun isSessionDead(cookie: String): Boolean {
        val provenRecently = cookie == verifiedCookie &&
            verifiedAt?.let { it.elapsedNow() < VERIFICATION_TTL } == true
        if (provenRecently) {
            logger.d(TAG, "Session already proven valid; treating this failure as a permission error.")
            return false
        }
        return when (probeSession()) {
            ProbeOutcome.DEAD -> true
            ProbeOutcome.ALIVE -> {
                verifiedCookie = cookie
                verifiedAt = timeSource.markNow()
                logger.d(TAG, "Session is valid — the rejection was a permission error, not an expiry.")
                false
            }
            ProbeOutcome.INDETERMINATE -> {
                logger.d(TAG, "Could not reach the session probe; assuming transient and leaving the session alone.")
                false
            }
        }
    }

    /**
     * Asks Overseerr whether the stored session cookie still identifies a user.
     *
     * `/api/v1/auth/me` is the one protected endpoint every authenticated user can
     * reach regardless of permissions, so its answer isolates "session dead" from
     * "not allowed". [SkipSessionRefresh] is mandatory: the refresh interceptor
     * would otherwise call back into [refresh] and deadlock on the non-reentrant
     * [mutex] this runs under.
     */
    private suspend fun probeSession(): ProbeOutcome = try {
        val response: HttpResponse = clientProvider().get(PROBE_PATH) {
            attributes.put(SkipSessionRefresh, true)
        }
        outcomeFor(response.status.value)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // expectSuccess = true turns a non-2xx into a ResponseException.
        val status = (e as? ResponseException)?.response?.status?.value
        if (status == null) {
            logger.d(TAG, "Session probe failed transiently: ${e.message}")
            ProbeOutcome.INDETERMINATE
        } else {
            outcomeFor(status)
        }
    }

    private fun outcomeFor(status: Int): ProbeOutcome = when {
        status in 200..299 -> ProbeOutcome.ALIVE
        status == 401 || status == 403 -> ProbeOutcome.DEAD
        else -> ProbeOutcome.INDETERMINATE
    }

    private enum class ProbeOutcome { ALIVE, DEAD, INDETERMINATE }

    /**
     * Drops the stored Plex token alone, leaving the Overseerr session intact.
     *
     * Plex Discover (watchlist) calls authenticate directly with this token, so a
     * 401 from plex.tv means the token was revoked there even though the Overseerr
     * session may still be perfectly healthy — logging the user out over it would
     * be wrong. Removing the key is what matters: WatchlistPagingSource and
     * WatchlistRepositoryImpl both fall back to Overseerr's own watchlist when no
     * Plex token is present, so the feature degrades instead of failing.
     */
    suspend fun invalidatePlexToken() = mutex.withLock {
        if (securityManager.retrieveSecureData(PLEX_TOKEN_KEY) == null) return@withLock
        logger.w(TAG, "Plex rejected the stored token — dropping it and falling back to Overseerr.")
        securityManager.removeSecureData(PLEX_TOKEN_KEY)
    }

    /**
     * Signs the user out so `isAuthenticated()` flips false and the UI returns to
     * login. Routed through [SessionCleaner] because this has to wipe the local
     * cache too — otherwise the next account to sign in on this device inherits
     * the previous one's requests, issues and notifications.
     *
     * Runs at most once per involuntary sign-out. The guard lifts again once the
     * UI has consumed the reason (the user reached the sign-in screen), so a later
     * expiry in the same process can still sign them out.
     */
    private suspend fun invalidate(reason: SessionExpiryReason) {
        if (invalidated && expiryNotifier.reason.value != null) {
            logger.d(TAG, "Already signed out for this expiry; keeping the original reason.")
            return
        }
        invalidated = true
        try {
            sessionCleaner.clear()
        } finally {
            // The UI has to learn *why* even if the wipe went wrong halfway;
            // without this it would sit on a screen whose every request fails and
            // never explain itself.
            expiryNotifier.notifyExpired(reason)
        }
    }

    companion object {
        private const val TAG = "SessionRefresher"
        const val COOKIE_KEY = "cookie_auth_token"
        const val PLEX_TOKEN_KEY = "plex_token"
        const val API_KEY_STORAGE_KEY = "underseerr_api_key"
        const val SESSION_MARKER = "SESSION_COOKIE"

        /** Where the session probe goes. Reachable by any signed-in user. */
        private const val PROBE_PATH = "/api/v1/auth/me"

        /**
         * How long a "this cookie is alive" result is trusted.
         *
         * Long enough that the permission-403s a screen fires off together share a
         * single probe; short enough that a session which lapses later in the same
         * process is still noticed and refreshed.
         */
        private val VERIFICATION_TTL = 5.minutes

        /** Express's default session cookie name — what Overseerr and Jellyseerr both use. */
        private const val SESSION_COOKIE_NAME = "connect.sid"

        /**
         * Picks the Overseerr session out of a response's `Set-Cookie` headers.
         *
         * A response commonly carries more than one: behind Cloudflare (`__cf_bm`),
         * oauth2-proxy or Authelia the session is not the first. Taking the first
         * blindly stored a proxy cookie as the session, after which every request
         * failed auth and re-authenticated forever. So all of them are considered
         * and `connect.sid` is preferred, with the first entry as a fallback so
         * deployments that rename the cookie still work.
         */
        fun extractSessionCookie(response: HttpResponse): String? {
            val pairs = response.headers.getAll(HttpHeaders.SetCookie)
                ?.mapNotNull { header ->
                    header.substringBefore(';').trim().takeIf { it.isNotEmpty() }
                }
                .orEmpty()
            if (pairs.isEmpty()) return null
            return pairs.firstOrNull { it.substringBefore('=').trim() == SESSION_COOKIE_NAME }
                ?: pairs.first()
        }
    }
}
