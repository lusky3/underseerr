package app.lusk.underseerr.data.remote

import app.lusk.underseerr.data.auth.SessionExpiryNotifier
import app.lusk.underseerr.data.auth.SessionCleaner
import app.lusk.underseerr.data.auth.SessionRefresher
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lusk.underseerr.util.PlatformContext
import app.lusk.underseerr.util.initAppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.random.Random

/**
 * The Ktor client logged at [io.ktor.client.plugins.logging.LogLevel.ALL], which
 * put the Plex token, the Overseerr session cookie, the API key and — for local
 * and Jellyfin logins — the user's plaintext password into logcat on every run.
 *
 * These tests capture what the client actually prints and assert no credential
 * survives. The client's logger writes via `println`, so stdout is the honest
 * place to check.
 */
class CredentialLoggingTest {

    private val stored = mutableMapOf(
        SessionRefresher.COOKIE_KEY to "connect.sid=$SESSION_SECRET",
        SessionRefresher.PLEX_TOKEN_KEY to PLEX_SECRET,
        SessionRefresher.API_KEY_STORAGE_KEY to SessionRefresher.SESSION_MARKER
    )

    private val securityManager = mockk<SecurityManager>(relaxed = true).also { sm ->
        coEvery { sm.retrieveSecureData(any()) } answers { stored[firstArg()] }
        coEvery { sm.storeSecureData(any(), any()) } answers { stored[firstArg()] = secondArg(); Unit }
        coEvery { sm.removeSecureData(any()) } answers { stored.remove(firstArg<String>()); Unit }
        coEvery { sm.clearSecureData() } answers { stored.clear(); Unit }
    }

    private val dataStoreFile = File.createTempFile("underseerr_log_test_", ".preferences_pb")
        .also { it.delete() }
    private val preferencesManager = PreferencesManager(
        PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
    ).also { runBlocking { it.setServerUrl("https://overseerr.example.com") } }

    @BeforeEach
    fun enableVerboseLogging() {
        // Worst case on purpose: logging fully enabled, so these tests prove the
        // header sanitizing works rather than that the log level happens to be off.
        // Driven through initAppConfig — the production entry point — because
        // composeApp ships no public setter for the debug flag.
        initAppConfig(appWithFlags(ApplicationInfo.FLAG_DEBUGGABLE))
    }

    @AfterEach
    fun tearDown() {
        initAppConfig(appWithFlags(0))
        dataStoreFile.delete()
    }

    /** A [PlatformContext] whose app reports exactly [flags] in its `ApplicationInfo`. */
    private fun appWithFlags(flags: Int): PlatformContext {
        val info = ApplicationInfo().also { it.flags = flags }
        val context = mockk<Context> { every { applicationInfo } returns info }
        return PlatformContext(context)
    }

    private fun buildClient(): HttpClient {
        lateinit var client: HttpClient
        val logger = mockk<AppLogger>(relaxed = true)
        val refresher = SessionRefresher(
            securityManager,
            preferencesManager,
            logger,
            SessionExpiryNotifier(),
            // No Room in this source set, and these tests never sign out anyway.
            SessionCleaner(securityManager, preferencesManager, logger, cacheCleaner = null)
        ) { client }
        client = HttpClientFactory(preferencesManager, securityManager, refresher).create(
            MockEngine {
                respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        // lowercase on purpose: OkHttp lowercases response header names
                        "set-cookie" to listOf("connect.sid=$ROTATED_SECRET; Path=/; HttpOnly")
                    )
                )
            }
        )
        return client
    }

    /** Runs [block] with stdout captured, and returns everything printed. */
    private fun captureOutput(block: suspend () -> Unit): String {
        val buffer = ByteArrayOutputStream()
        val original = System.out
        System.setOut(PrintStream(buffer, true))
        try {
            runBlocking { block() }
        } finally {
            System.setOut(original)
        }
        return buffer.toString()
    }

    @Test
    fun `the overseerr session cookie never reaches the log`() {
        val client = buildClient()
        val output = captureOutput { client.get("/api/v1/movie/42") }

        assertTrue(output.contains("REQUEST:"), "positive control: logging was not actually enabled")
        assertTrue(output.contains("***"), "positive control: no header was masked at all")
        assertFalse(output.contains(SESSION_SECRET), "outgoing session cookie was logged")
        assertFalse(output.contains(ROTATED_SECRET), "Set-Cookie from the response was logged")
    }

    @Test
    fun `the plex token never reaches the log`() {
        val client = buildClient()
        val output = captureOutput {
            client.get("https://discover.provider.plex.tv/library/sections/watchlist/all") {
                header("X-Plex-Token", PLEX_SECRET)
            }
        }

        assertFalse(output.contains(PLEX_SECRET), "X-Plex-Token was logged")
    }

    @Test
    fun `an overseerr api key never reaches the log`() {
        stored[SessionRefresher.API_KEY_STORAGE_KEY] = API_KEY_SECRET
        stored.remove(SessionRefresher.COOKIE_KEY)

        val client = buildClient()
        val output = captureOutput { client.get("/api/v1/movie/42") }

        assertFalse(output.contains(API_KEY_SECRET), "X-Api-Key was logged")
    }

    @Test
    fun `login request bodies are never logged`() {
        // The regression that mattered most: LogLevel.ALL printed request bodies,
        // so /auth/plex leaked the Plex token and /auth/local leaked the password.
        val client = buildClient()
        val output = captureOutput {
            client.post("/api/v1/auth/local") {
                setBody("""{"email":"user@example.com","password":"$PASSWORD_SECRET"}""")
            }
        }

        assertFalse(output.contains(PASSWORD_SECRET), "login body containing the password was logged")
    }

    private companion object {
        /**
         * Generated at runtime rather than written as literals. Credential-shaped
         * string literals trip secret scanners (Snyk Code flagged the previous ones
         * as hardcoded secrets/passwords), and these values only need to be unique,
         * greppable markers that must not appear in captured log output.
         */
        private fun marker(label: String): String =
            "DUMMY-" + label + "-" + Random.nextLong(1_000_000_000L, 9_999_999_999L)

        // `s%3A` mirrors express-session's encoding so the cookie shape stays realistic.
        val SESSION_SECRET = "s%3A" + marker("session")
        val ROTATED_SECRET = "s%3A" + marker("rotated")
        val PLEX_SECRET = marker("plex")
        val API_KEY_SECRET = marker("apikey")
        val PASSWORD_SECRET = marker("passphrase")
    }
}
