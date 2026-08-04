package app.lusk.underseerr.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lusk.underseerr.data.auth.LocalCacheCleaner
import app.lusk.underseerr.data.auth.SessionCleaner
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.api.AuthKtorService
import app.lusk.underseerr.data.remote.api.PlexKtorService
import app.lusk.underseerr.data.remote.api.SettingsKtorService
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Two properties of [AuthRepositoryImpl] that the app's security and cache
 * integrity rest on.
 *
 * 1. No token authenticates without the server. `authenticateWithPlex` used to
 *    recognise a hardcoded string ("debug_token_12345") and mint a local admin
 *    session with no network call, in every build type. The token is attacker
 *    supplied: `underseerr://auth?token=…` is an exported, BROWSABLE deep link
 *    that MainActivity feeds straight to `Screen.parseDeepLink` →
 *    `AuthViewModel.handleAuthCallback` → here.
 *
 * 2. Reading the session has no destructive side effects. The legacy
 *    `"no_api_key"` placeholder used to trigger a full sign-out (which now wipes
 *    the Room cache) from inside the flow's `map` — once per collector, again on
 *    every re-collection.
 */
class AuthSessionTest {

    private val stored = java.util.concurrent.ConcurrentHashMap<String, String>()

    private val securityManager = mockk<SecurityManager>(relaxed = true).also { sm ->
        coEvery { sm.retrieveSecureData(any()) } answers { stored[firstArg()] }
        coEvery { sm.storeSecureData(any(), any()) } answers {
            stored[firstArg()] = secondArg(); Unit
        }
        coEvery { sm.removeSecureData(any()) } answers { stored.remove(firstArg<String>()); Unit }
        coEvery { sm.clearSecureData() } answers { stored.clear(); Unit }
    }

    // A real PreferencesManager over a throwaway DataStore: the stored user id
    // is what actually decides whether the app considers itself signed in.
    private val dataStoreFile = File.createTempFile("underseerr_auth_test_", ".preferences_pb")
        .also { it.delete() }
    private val preferencesManager = PreferencesManager(
        PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
    )

    @AfterEach
    fun tearDown() {
        dataStoreFile.delete()
    }

    // Room is not on this source set's classpath, so the account-scoped cache is
    // stood in for by the same seam production wires Room into.
    private val cacheClears = java.util.concurrent.atomic.AtomicInteger(0)
    @Volatile private var cacheClearDelayMs = 0L
    private val cacheCleaner = LocalCacheCleaner {
        delay(cacheClearDelayMs)
        cacheClears.incrementAndGet()
    }
    private val logger = mockk<AppLogger>(relaxed = true)
    private val sessionCleaner =
        SessionCleaner(securityManager, preferencesManager, logger, cacheCleaner)

    private val authKtorService = mockk<AuthKtorService>(relaxed = true)
    private val plexKtorService = mockk<PlexKtorService>(relaxed = true)
    private val settingsKtorService = mockk<SettingsKtorService>(relaxed = true)

    private val repository = AuthRepositoryImpl(
        authKtorService = authKtorService,
        plexKtorService = plexKtorService,
        settingsKtorService = settingsKtorService,
        securityManager = securityManager,
        preferencesManager = preferencesManager,
        sessionCleaner = sessionCleaner
    )

    private suspend fun signedInWith(apiKey: String) {
        stored[API_KEY] = apiKey
        preferencesManager.setServerUrl(SERVER_URL)
        preferencesManager.setUserId(1)
    }

    // --- 1. no locally recognised token ------------------------------------

    @Test
    fun `the old debug token is just another token the server has to reject`() = runBlocking {
        coEvery { authKtorService.authenticateWithPlex(any()) } throws IOException("no server")

        val result = repository.authenticateWithPlex(DEBUG_TOKEN)

        assertInstanceOf(Result.Error::class.java, result, "must not mint a session locally")
        coVerify(exactly = 1) { authKtorService.authenticateWithPlex(DEBUG_TOKEN) }
        assertTrue(stored.isEmpty(), "nothing may be written to secure storage: $stored")
        assertNull(preferencesManager.getUserId().first(), "must not leave the app signed in")
        assertNull(repository.getStoredSession().first(), "must not produce a session")
        assertFalse(repository.isAuthenticated().first(), "a deep link must not sign anyone in")
    }

    @Test
    fun `a deep-link token is sent to the server verbatim`() = runBlocking {
        coEvery { authKtorService.authenticateWithPlex(any()) } throws IOException("no server")

        repository.authenticateWithPlex("attacker-supplied")

        // Every token takes the same path; there is no branch that skips the call.
        coVerify(exactly = 1) { authKtorService.authenticateWithPlex("attacker-supplied") }
        assertTrue(stored.isEmpty())
    }

    // --- 2. reading the session is a projection -----------------------------

    @Test
    fun `a legacy no_api_key device is reported signed out`() = runBlocking {
        signedInWith(LEGACY_MARKER)

        assertNull(repository.getStoredSession().first(), "the placeholder is not a session")
        assertFalse(repository.isAuthenticated().first(), "such users must be sent back to login")
    }

    @Test
    fun `a legacy no_api_key device is cleaned up, once`() = runBlocking {
        signedInWith(LEGACY_MARKER)

        repository.getStoredSession().first()

        assertTrue(stored.isEmpty(), "the stale placeholder must be dropped: $stored")
        assertNull(preferencesManager.getUserId().first())
        assertEquals(1, cacheClears.get(), "the previous account's cached rows must go too")

        // Collected again by another screen: nothing left to do, and nothing done.
        repository.isAuthenticated().first()
        repository.getStoredSession().first()
        assertEquals(1, cacheClears.get(), "cleanup must not repeat per collector")
    }

    @Test
    fun `concurrent collectors of a legacy device wipe the cache exactly once`() = runBlocking {
        signedInWith(LEGACY_MARKER)
        // Widen the window: several screens start collecting while the first
        // sign-out is still in flight. Firing a wipe per collector is the bug.
        cacheClearDelayMs = 50

        coroutineScope {
            (1..5).map { async { repository.getStoredSession().first() } }.map { it.await() }
        }

        assertEquals(1, cacheClears.get(), "one migration, however many collectors")
    }

    @Test
    fun `a real session is returned and nothing is cleaned up`() = runBlocking {
        signedInWith(SESSION_MARKER)

        repeat(3) {
            val session = repository.getStoredSession().first()
            assertNotNull(session, "a cookie-backed session must survive being read")
            assertEquals(1, session!!.userId)
            assertEquals(SESSION_MARKER, session.apiKey)
            assertEquals(SERVER_URL, session.serverUrl)
        }

        assertTrue(repository.isAuthenticated().first())
        assertEquals(0, cacheClears.get(), "reading the session must never wipe the cache")
        assertEquals(SESSION_MARKER, stored[API_KEY])
    }

    @Test
    fun `no stored user id means no session and no cleanup`() = runBlocking {
        preferencesManager.setServerUrl(SERVER_URL)

        assertNull(repository.getStoredSession().first())
        assertEquals(0, cacheClears.get())
    }

    @Test
    fun `an api-key user without a server url has no session`() = runBlocking {
        stored[API_KEY] = "real-api-key"
        preferencesManager.setUserId(1)

        assertNull(repository.getStoredSession().first())
        assertEquals(0, cacheClears.get())
        assertEquals("real-api-key", stored[API_KEY], "an unrelated read must not touch credentials")
    }

    private companion object {
        const val API_KEY = "underseerr_api_key"
        const val SESSION_MARKER = "SESSION_COOKIE"
        const val LEGACY_MARKER = AuthRepositoryImpl.LEGACY_NO_API_KEY_MARKER
        const val DEBUG_TOKEN = "debug_token_12345"
        const val SERVER_URL = "https://overseerr.example.com"
    }
}
