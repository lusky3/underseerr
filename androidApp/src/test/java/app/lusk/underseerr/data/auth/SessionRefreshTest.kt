package app.lusk.underseerr.data.auth

import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.HttpClientFactory
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.ktor.client.HttpClient
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Reproduces the reported failure and pins the fix.
 *
 * Symptom: once the Overseerr session lapsed the app kept reporting itself as
 * logged in (home screen served cached content) and only broke when a media item
 * was opened, surfacing a bare 403. Nothing ever tried to re-mint the session
 * from the Plex token the app already had on hand.
 *
 * Overseerr's `isAuthenticated` middleware answers 403 — not 401 — for a missing
 * or expired session, so both codes are exercised. It also answers 403 for a
 * *permission* failure on a perfectly healthy session, which is why the refresher
 * probes `/api/v1/auth/me` before spending a re-auth; that split is exercised too.
 *
 * These tests drive the real [HttpClientFactory] over a MockEngine so the
 * interceptor wiring (including Ktor's send-pipeline ordering against
 * `expectSuccess = true`) is what's under test, not a copy of it.
 *
 * State shared with the engine is concurrent-safe throughout: the MockEngine
 * handler and the refresher both run on the IO pool, so plain maps and Ints would
 * be mutated off-thread by the single-flight tests.
 */
class SessionRefreshTest {

    private val stored = ConcurrentHashMap<String, String>()

    private val securityManager = mockk<SecurityManager>(relaxed = true).also { sm ->
        coEvery { sm.retrieveSecureData(any()) } answers { stored[firstArg()] }
        coEvery { sm.storeSecureData(any(), any()) } answers {
            stored[firstArg()] = secondArg(); Unit
        }
        coEvery { sm.removeSecureData(any()) } answers { stored.remove(firstArg<String>()); Unit }
        coEvery { sm.clearSecureData() } answers { stored.clear(); Unit }
    }

    // A real PreferencesManager over a throwaway DataStore. Mocking it is not an
    // option (final class, no inline instrumentation on this JVM) and using the
    // real thing lets the tests assert the outcome that actually matters:
    // the stored user id disappearing, which is what flips isAuthenticated() false.
    private val dataStoreFile = File.createTempFile("underseerr_test_", ".preferences_pb")
        .also { it.delete() }
    private val preferencesManager = PreferencesManager(
        PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
    ).also {
        runBlocking {
            it.setServerUrl(SERVER_URL)
            it.setUserId(1) // a logged-in session
        }
    }

    @AfterEach
    fun tearDown() {
        dataStoreFile.delete()
    }

    private val logger = mockk<AppLogger>(relaxed = true)
    private val expiryNotifier = SessionExpiryNotifier()

    // Room holds account-scoped rows (requests, issues, notifications, users…),
    // so an involuntary sign-out has to wipe them exactly like the explicit
    // logout does — otherwise the next Plex account on this device is served the
    // previous one's cache. Room itself isn't on this source set's classpath, so
    // the cache is stood in for by the same seam production wires Room into.
    private val cacheClears = AtomicInteger()
    @Volatile private var cacheClearFails = false
    private val cacheCleaner = LocalCacheCleaner {
        cacheClears.incrementAndGet()
        if (cacheClearFails) throw IllegalStateException("database is closed")
    }
    private val sessionCleaner = SessionCleaner(securityManager, preferencesManager, logger, cacheCleaner)

    /** Cookies seen by the server on protected endpoints, in order. */
    private val sentCookies: MutableList<String?> = Collections.synchronizedList(mutableListOf())
    private val reauthAttempts = AtomicInteger()
    @Volatile private var reauthBody: String? = null

    /** Hits on `/api/v1/auth/me` — the "is this session actually dead?" probe. */
    private val probeAttempts = AtomicInteger()

    /**
     * What the probe endpoint says. Dead by default so the expiry tests below read
     * as they always did; the permission-error tests flip it.
     */
    @Volatile private var sessionIsAlive = false

    /** Makes the probe fail the way a dropped connection would. */
    @Volatile private var probeUnreachable = false

    /** `Set-Cookie` headers the re-auth responds with, in wire order. */
    @Volatile private var reauthSetCookies = listOf("$FRESH; Path=/; HttpOnly; Max-Age=2592000")

    /** The refresher under test, so tests can drive it without going via a request. */
    private lateinit var refresher: SessionRefresher

    private fun buildClient(handler: MockEngine.Companion.() -> MockEngine): HttpClient {
        lateinit var client: HttpClient
        refresher =
            SessionRefresher(securityManager, preferencesManager, logger, expiryNotifier, sessionCleaner) { client }
        client = HttpClientFactory(preferencesManager, securityManager, refresher)
            .create(MockEngine.handler())
        return client
    }

    private fun MockEngine.Companion.engine(
        onProtected: MockRequestHandleScope.(cookie: String?) -> HttpResponseData
    ) = MockEngine { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith(REAUTH_PATH) -> {
                reauthAttempts.incrementAndGet()
                reauthBody = (request.body as? io.ktor.http.content.TextContent)?.text
                reauthResponse()
            }
            path.endsWith(PROBE_PATH) -> {
                probeAttempts.incrementAndGet()
                probeResponse()
            }
            else -> {
                sentCookies += request.headers[HttpHeaders.Cookie]
                onProtected(request.headers[HttpHeaders.Cookie])
            }
        }
    }

    private fun MockRequestHandleScope.reauthResponse() =
        if (stored[SessionRefresher.PLEX_TOKEN_KEY] == GOOD_TOKEN) {
            respond(
                content = """{"id":1,"email":"a@b.c","displayName":"A"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "Set-Cookie" to reauthSetCookies
                )
            )
        } else {
            // Overseerr rejects a revoked Plex token.
            respond(
                content = """{"status":403,"message":"Unable to authenticate"}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

    private fun MockRequestHandleScope.probeResponse(): HttpResponseData {
        if (probeUnreachable) throw IOException("connection reset")
        return if (sessionIsAlive) {
            respond(
                content = """{"id":1,"email":"a@b.c","displayName":"A"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        } else {
            forbidden()
        }
    }

    private fun MockRequestHandleScope.ok() = respond(
        content = """{"id":42,"title":"Dune"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    private fun MockRequestHandleScope.forbidden() = respond(
        content = """{"status":403,"message":"You do not have permission"}""",
        status = HttpStatusCode.Forbidden,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    @Test
    fun `expired session is re-minted from the stored plex token and the request retried`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = SessionRefresher.SESSION_MARKER

        val client = buildClient { engine { cookie -> if (cookie == FRESH) ok() else forbidden() } }

        // Opening a media item — the exact action that produced the 403.
        val response: HttpResponse = client.get("/api/v1/movie/42")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf(STALE, FRESH), sentCookies, "stale attempt then fresh retry")
        assertTrue(reauthBody!!.contains(GOOD_TOKEN), "re-auth must send the stored plex token")
        assertEquals(FRESH, stored[SessionRefresher.COOKIE_KEY])
        assertEquals(SessionRefresher.SESSION_MARKER, stored[SessionRefresher.API_KEY_STORAGE_KEY])
    }

    @Test
    fun `401 is recovered the same way as 403`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN

        val client = buildClient {
            engine { cookie ->
                if (cookie == FRESH) ok() else respond(content = "", status = HttpStatusCode.Unauthorized)
            }
        }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/movie/42").status)
        assertEquals(1, reauthAttempts.get())
    }

    @Test
    fun `revoked plex token clears credentials so the app returns to login`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = SessionRefresher.SESSION_MARKER

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertTrue(stored.isEmpty(), "secure credentials must be dropped: $stored")
        assertNull(preferencesManager.getUserId().first(), "user id must be cleared so the app returns to login")
        assertEquals(
            SessionExpiryReason.PLEX_TOKEN_REVOKED,
            expiryNotifier.reason.value,
            "the sign-in screen needs to explain why the user was signed out"
        )
        assertEquals(1, cacheClears.get(), "the local cache must be wiped too")
    }

    @Test
    fun `an involuntary sign-out wipes the cache so the next account sees nothing of this one`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        // The explicit logout has always cleared Room; being forced out must not
        // be the weaker of the two, or the previous user's requests, issues and
        // notifications stay readable to whoever signs in next.
        assertEquals(1, cacheClears.get(), "involuntary sign-out must clear the local database")
    }

    @Test
    fun `a failure to clear the cache still drops the credentials`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN
        cacheClearFails = true

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        // Half a sign-out is worse than none: the app would keep believing it is
        // logged in with a session the server has already rejected.
        assertTrue(stored.isEmpty(), "secure credentials must still be dropped: $stored")
        assertNull(preferencesManager.getUserId().first())
        assertEquals(SessionExpiryReason.PLEX_TOKEN_REVOKED, expiryNotifier.reason.value)
    }

    @Test
    fun `a failure early in the wipe still finishes the rest and reports why`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN
        // Secure storage is the first step of the wipe. Preferences (a DataStore
        // write) can fail the same way; either one escaping used to abort every
        // later step, including the notification that tells the UI what happened.
        coEvery { securityManager.clearSecureData() } throws IOException("keystore unavailable")

        buildClient { engine { forbidden() } }
        assertFalse(refresher.refresh(STALE))

        assertNull(
            preferencesManager.getUserId().first(),
            "a failed first step must not stop the auth preferences being cleared"
        )
        assertEquals(1, cacheClears.get(), "…nor the local cache")
        assertEquals(
            SessionExpiryReason.PLEX_TOKEN_REVOKED,
            expiryNotifier.reason.value,
            "the UI must still be told why it is about to be thrown back to login"
        )
    }

    @Test
    fun `the re-auth call itself never triggers another refresh`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertEquals(1, reauthAttempts.get(), "re-auth must be attempted exactly once")
    }

    @Test
    fun `transient failure during refresh does not log the user out`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN

        val client = buildClient {
            MockEngine { request ->
                if (request.url.encodedPath.endsWith(REAUTH_PATH)) throw IOException("connection reset")
                forbidden()
            }
        }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertEquals(GOOD_TOKEN, stored[SessionRefresher.PLEX_TOKEN_KEY], "token must survive a network blip")
        assertNotNull(preferencesManager.getUserId().first(), "a network blip must not log the user out")
        assertNull(expiryNotifier.reason.value, "a network blip must not claim the session expired")
        assertEquals(0, cacheClears.get(), "a network blip must not wipe the cache")
    }

    @Test
    fun `no stored plex token means straight to login`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = SessionRefresher.SESSION_MARKER

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertEquals(0, reauthAttempts.get(), "nothing to refresh with, so no call")
        assertTrue(stored.isEmpty())
        assertNull(preferencesManager.getUserId().first())
        assertEquals(SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH, expiryNotifier.reason.value)
        assertEquals(1, cacheClears.get(), "the local cache must be wiped too")
    }

    @Test
    fun `api key auth is left alone because its 403 is a real permission error`() = runBlocking {
        // No cookie stored, so HttpClientFactory sends X-Api-Key instead.
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = "real-api-key"
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertEquals(0, reauthAttempts.get(), "must not re-auth an API-key user")
        assertEquals("real-api-key", stored[SessionRefresher.API_KEY_STORAGE_KEY])
    }

    @Test
    fun `plex revoking the token drops it without signing the user out of overseerr`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = SessionRefresher.SESSION_MARKER

        // Plex Discover rejects a revoked token with 401.
        val client = buildClient {
            MockEngine { respond(content = "", status = HttpStatusCode.Unauthorized) }
        }

        assertThrows(ClientRequestException::class.java) {
            runBlocking {
                client.get("https://discover.provider.plex.tv/library/sections/watchlist/all") {
                    header("X-Plex-Token", GOOD_TOKEN)
                }
            }
        }

        assertEquals(0, reauthAttempts.get(), "a plex.tv failure must not hit Overseerr's auth endpoint")
        assertNull(
            stored[SessionRefresher.PLEX_TOKEN_KEY],
            "the dead token must be dropped so the watchlist falls back to Overseerr"
        )
        // The Overseerr session is independent and was never in question.
        assertEquals(STALE, stored[SessionRefresher.COOKIE_KEY])
        assertNotNull(preferencesManager.getUserId().first(), "must not sign out of Overseerr")
        assertNull(expiryNotifier.reason.value)
    }

    @Test
    fun `a plex tv permission error other than 401 leaves the token alone`() = runBlocking {
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN

        val client = buildClient { MockEngine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking {
                client.get("https://discover.provider.plex.tv/library/sections/watchlist/all") {
                    header("X-Plex-Token", GOOD_TOKEN)
                }
            }
        }

        assertEquals(GOOD_TOKEN, stored[SessionRefresher.PLEX_TOKEN_KEY])
    }

    @Test
    fun `concurrent auth failures re-authenticate only once`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN

        val client = buildClient { engine { cookie -> if (cookie == FRESH) ok() else forbidden() } }

        // Several screens loading at once, all holding the same stale cookie.
        val statuses = coroutineScope {
            (1..5).map { async { client.get("/api/v1/movie/$it").status } }.map { it.await() }
        }

        assertTrue(statuses.all { it == HttpStatusCode.OK }, "all should recover: $statuses")
        assertEquals(1, reauthAttempts.get(), "single-flight refresh expected")
        assertEquals(1, probeAttempts.get(), "the probe is single-flight too")
    }

    // ---------------------------------------------------------------------
    // Overseerr answers 403 for "not allowed" as well as "session expired".
    // ---------------------------------------------------------------------

    @Test
    fun `a permission error on a live session never re-authenticates or signs the user out`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = SessionRefresher.SESSION_MARKER
        sessionIsAlive = true

        // /api/v1/settings/main is ADMIN-only and DiscoveryViewModel asks for it on
        // every app start, so every non-admin user hits this on launch.
        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/settings/main") }
        }

        assertEquals(1, probeAttempts.get(), "the session must be checked, not assumed dead")
        assertEquals(0, reauthAttempts.get(), "a permission error must not cost a round trip to plex.tv")
        assertEquals(STALE, stored[SessionRefresher.COOKIE_KEY], "the working session must be left alone")
        assertNotNull(preferencesManager.getUserId().first(), "a permission error must not sign anyone out")
        assertNull(expiryNotifier.reason.value)
        assertEquals(0, cacheClears.get())
    }

    @Test
    fun `a revoked plex token behind a permission error does not force a wrongful sign-out`() = runBlocking {
        // The nastiest combination: the user is simply not an admin, and their
        // stored Plex token happens to be dead. Re-authing would have "proved" the
        // session unrecoverable and wiped a perfectly good login.
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN
        sessionIsAlive = true

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/settings/main") }
        }

        assertEquals(0, reauthAttempts.get())
        assertEquals(REVOKED_TOKEN, stored[SessionRefresher.PLEX_TOKEN_KEY])
        assertNotNull(preferencesManager.getUserId().first())
        assertNull(expiryNotifier.reason.value)
        assertEquals(0, cacheClears.get())
    }

    @Test
    fun `repeated permission errors on the same cookie only probe once`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        sessionIsAlive = true

        val client = buildClient { engine { forbidden() } }

        repeat(3) {
            assertThrows(ClientRequestException::class.java) {
                runBlocking { client.get("/api/v1/settings/main") }
            }
        }

        assertEquals(1, probeAttempts.get(), "a cookie proven alive must not be re-probed on every 403")
        assertEquals(0, reauthAttempts.get())
    }

    @Test
    fun `a probe that cannot reach the server is treated as transient`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN
        probeUnreachable = true

        val client = buildClient { engine { forbidden() } }

        assertThrows(ClientRequestException::class.java) {
            runBlocking { client.get("/api/v1/movie/42") }
        }

        assertEquals(0, reauthAttempts.get(), "an unanswered probe proves nothing")
        assertEquals(REVOKED_TOKEN, stored[SessionRefresher.PLEX_TOKEN_KEY])
        assertNotNull(preferencesManager.getUserId().first(), "a network blip must not log the user out")
        assertNull(expiryNotifier.reason.value)
        assertEquals(0, cacheClears.get())
    }

    // ---------------------------------------------------------------------
    // One sign-out, one reason, however many coroutines arrive at once.
    // ---------------------------------------------------------------------

    @Test
    fun `concurrent failures against a revoked token sign out once and keep the true reason`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = REVOKED_TOKEN

        buildClient { engine { forbidden() } }

        val results = coroutineScope {
            (1..5).map { async { refresher.refresh(STALE) } }.map { it.await() }
        }

        assertTrue(results.none { it }, "nothing can be refreshed with a revoked token")
        assertEquals(1, reauthAttempts.get())
        assertEquals(1, cacheClears.get(), "the wipe must not run once per in-flight 403")
        assertEquals(
            SessionExpiryReason.PLEX_TOKEN_REVOKED,
            expiryNotifier.reason.value,
            "the later coroutines find no credentials at all; reporting that would hide the real cause"
        )
    }

    // ---------------------------------------------------------------------
    // Reverse proxies put their own cookies on the response first.
    // ---------------------------------------------------------------------

    @Test
    fun `the session is picked out from behind a proxy's own set-cookie`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        reauthSetCookies = listOf(
            "__cf_bm=cloudflare-bot-management; Path=/; HttpOnly; SameSite=None",
            "$FRESH; Path=/; HttpOnly; Max-Age=2592000"
        )

        val client = buildClient { engine { cookie -> if (cookie == FRESH) ok() else forbidden() } }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/movie/42").status)
        assertEquals(
            FRESH,
            stored[SessionRefresher.COOKIE_KEY],
            "storing Cloudflare's cookie as the session would re-auth on every single request"
        )
    }

    @Test
    fun `a deployment that does not use connect_sid still gets its cookie stored`() = runBlocking {
        stored[SessionRefresher.COOKIE_KEY] = STALE
        stored[SessionRefresher.PLEX_TOKEN_KEY] = GOOD_TOKEN
        reauthSetCookies = listOf("$RENAMED; Path=/; HttpOnly")

        val client = buildClient { engine { cookie -> if (cookie == RENAMED) ok() else forbidden() } }

        assertEquals(HttpStatusCode.OK, client.get("/api/v1/movie/42").status)
        assertEquals(RENAMED, stored[SessionRefresher.COOKIE_KEY])
    }

    private companion object {
        const val SERVER_URL = "https://overseerr.example.com"
        const val REAUTH_PATH = "/api/v1/auth/plex"
        const val PROBE_PATH = "/api/v1/auth/me"
        const val STALE = "connect.sid=stale"
        const val FRESH = "connect.sid=fresh"
        const val RENAMED = "overseerr.sid=fresh"
        const val GOOD_TOKEN = "plex-token-abc"
        const val REVOKED_TOKEN = "revoked-token"
    }
}
