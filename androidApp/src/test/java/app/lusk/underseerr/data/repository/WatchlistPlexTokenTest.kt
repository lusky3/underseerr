package app.lusk.underseerr.data.repository

import app.lusk.underseerr.data.local.dao.MediaRequestDao
import app.lusk.underseerr.data.remote.api.DiscoveryKtorService
import app.lusk.underseerr.data.remote.api.JellyseerrKtorService
import app.lusk.underseerr.data.remote.api.PlexKtorService
import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.security.SecurityManager
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A revoked Plex token is deleted from secure storage by the HTTP client, so the
 * watchlist has to cope with `plex_token` simply being absent.
 *
 * Reads degrade to the Overseerr watchlist. Writes cannot: Overseerr exposes no
 * watchlist write endpoint (`/discover/watchlist` is a read-only mirror of Plex
 * Discover), so add/remove must fail with an actionable message telling the user
 * to re-link Plex — not with a developer-facing "Plex token not found" string.
 *
 * Jellyseerr is unaffected; it owns its watchlist and never needs a Plex token.
 */
class WatchlistPlexTokenTest {

    private val stored = mutableMapOf<String, String>()

    private val securityManager = mockk<SecurityManager>(relaxed = true).also { sm ->
        coEvery { sm.retrieveSecureData(any()) } answers { stored[firstArg()] }
    }

    private val plexKtorService = mockk<PlexKtorService>(relaxed = true)
    private val jellyseerrKtorService = mockk<JellyseerrKtorService>(relaxed = true)
    private val discoveryKtorService = mockk<DiscoveryKtorService>(relaxed = true)
    private val mediaRequestDao = mockk<MediaRequestDao>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)

    private fun repository(isJellyseerr: Boolean): WatchlistRepositoryImpl {
        every { authRepository.getIsJellyseerr() } returns flowOf(isJellyseerr)
        return WatchlistRepositoryImpl(
            discoveryKtorService = discoveryKtorService,
            plexKtorService = plexKtorService,
            jellyseerrKtorService = jellyseerrKtorService,
            authRepository = authRepository,
            securityManager = securityManager,
            mediaRequestDao = mediaRequestDao
        )
    }

    @Test
    fun `add without a Plex token asks the user to reconnect Plex`() = runBlocking {
        val result = repository(isJellyseerr = false)
            .addToWatchlist(603, MediaType.MOVIE, ratingKey = "5d77")

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertInstanceOf(AppError.PlexReauthRequired::class.java, error)
        assertEquals(REAUTH_MESSAGE, error.message)
        assertEquals(REAUTH_MESSAGE, error.getUserMessage())
        assertFalse(error.isRetryable())

        // Nothing should have been sent anywhere without a token.
        coVerify(exactly = 0) { plexKtorService.addToWatchlist(any(), any()) }
        coVerify(exactly = 0) { jellyseerrKtorService.addToWatchlist(any()) }
    }

    @Test
    fun `remove without a Plex token asks the user to reconnect Plex`() = runBlocking {
        val result = repository(isJellyseerr = false)
            .removeFromWatchlist(1399, MediaType.TV, ratingKey = "5d9c")

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertInstanceOf(AppError.PlexReauthRequired::class.java, error)
        assertEquals(REAUTH_MESSAGE, error.message)
        assertEquals(REAUTH_MESSAGE, error.getUserMessage())

        coVerify(exactly = 0) { plexKtorService.removeFromWatchlist(any(), any()) }
        coVerify(exactly = 0) { jellyseerrKtorService.removeFromWatchlist(any()) }
    }

    @Test
    fun `the message never leaks developer wording`() = runBlocking {
        val repo = repository(isJellyseerr = false)
        val messages = listOf(
            repo.addToWatchlist(603, MediaType.MOVIE, "5d77"),
            repo.removeFromWatchlist(603, MediaType.MOVIE, "5d77")
        ).map { (it as Result.Error).error.getUserMessage() }

        messages.forEach { message ->
            assertFalse(message.contains(PLEX_TOKEN_KEY), "leaked storage key: $message")
            assertFalse(message.contains("token not found", ignoreCase = true), "developer wording: $message")
            assertFalse(message.contains("unexpected error", ignoreCase = true), "unhelpful wording: $message")
        }
    }

    @Test
    fun `Jellyseerr writes still work without a Plex token`() = runBlocking {
        val repo = repository(isJellyseerr = true)

        assertTrue(repo.addToWatchlist(603, MediaType.MOVIE, null).isSuccess)
        assertTrue(repo.removeFromWatchlist(603, MediaType.MOVIE, null).isSuccess)

        coVerify(exactly = 1) { jellyseerrKtorService.addToWatchlist(any()) }
        coVerify(exactly = 1) { jellyseerrKtorService.removeFromWatchlist(603) }
    }

    @Test
    fun `Plex writes still work when the token is present`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = "a-live-token"
        val repo = repository(isJellyseerr = false)

        assertTrue(repo.addToWatchlist(603, MediaType.MOVIE, "5d77").isSuccess)
        assertTrue(repo.removeFromWatchlist(603, MediaType.MOVIE, "5d77").isSuccess)

        coVerify(exactly = 1) { plexKtorService.addToWatchlist("a-live-token", "5d77") }
        coVerify(exactly = 1) { plexKtorService.removeFromWatchlist("a-live-token", "5d77") }
    }

    // --- the revocation the user actually hits first --------------------------
    //
    // On the request that *discovers* a revocation the token is still in storage:
    // plex.tv answers 401 and only afterwards does the HTTP client delete the key.
    // Without mapping that 401 the first attempt shows a raw HTTP error and only a
    // second attempt gets the actionable message.

    private fun unauthorized(): ClientRequestException {
        val response = mockk<HttpResponse>(relaxed = true)
        every { response.status } returns HttpStatusCode.Unauthorized
        return ClientRequestException(response, "Unauthorized")
    }

    @Test
    fun `a 401 from plex on add asks the user to reconnect, not for a raw HTTP error`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = "a-revoked-token"
        coEvery { plexKtorService.addToWatchlist(any(), any()) } throws unauthorized()

        val result = repository(isJellyseerr = false).addToWatchlist(603, MediaType.MOVIE, "5d77")

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertInstanceOf(AppError.PlexReauthRequired::class.java, error)
        assertEquals(REAUTH_MESSAGE, error.getUserMessage())
    }

    @Test
    fun `a 401 from plex on remove asks the user to reconnect`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = "a-revoked-token"
        coEvery { plexKtorService.removeFromWatchlist(any(), any()) } throws unauthorized()

        val result = repository(isJellyseerr = false).removeFromWatchlist(1399, MediaType.TV, "5d9c")

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertInstanceOf(AppError.PlexReauthRequired::class.java, error)
        coVerify(exactly = 1) { plexKtorService.removeFromWatchlist("a-revoked-token", "5d9c") }
    }

    /**
     * MediaDetailsScreen always passes `ratingKey = null`, so the revocation is met
     * inside the Plex Discover lookup — whose blanket `catch (Exception)` would
     * otherwise flatten it into "Could not find Plex ratingKey for TMDB ID …".
     */
    @Test
    fun `a 401 while looking up the ratingKey is not flattened into a not-found error`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = "a-revoked-token"
        coEvery { plexKtorService.searchDiscover(any(), any(), any()) } throws unauthorized()

        val result = repository(isJellyseerr = false).addToWatchlist(603, MediaType.MOVIE, ratingKey = null)

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertInstanceOf(AppError.PlexReauthRequired::class.java, error)
        assertFalse(error.getUserMessage().contains("ratingKey"), "leaked internals: ${error.getUserMessage()}")
        coVerify(exactly = 0) { plexKtorService.addToWatchlist(any(), any()) }
    }

    /**
     * A non-401 failure must keep its own error; the re-link prompt is only correct
     * when Plex actually rejected the token.
     */
    @Test
    fun `a non-401 plex failure is not reported as a Plex re-link`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = "a-live-token"
        coEvery { plexKtorService.addToWatchlist(any(), any()) } throws RuntimeException("boom")

        val result = repository(isJellyseerr = false).addToWatchlist(603, MediaType.MOVIE, "5d77")

        val error = assertInstanceOf(Result.Error::class.java, result).error
        assertFalse(
            error is AppError.PlexReauthRequired,
            "an unrelated failure must not tell the user to re-link Plex"
        )
    }

    // --- an empty token is not a token ----------------------------------------

    @Test
    fun `an empty stored token is treated as absent`() = runBlocking {
        stored[PLEX_TOKEN_KEY] = ""
        val repo = repository(isJellyseerr = false)

        assertInstanceOf(
            AppError.PlexReauthRequired::class.java,
            assertInstanceOf(Result.Error::class.java, repo.addToWatchlist(603, MediaType.MOVIE, "5d77")).error
        )
        assertInstanceOf(
            AppError.PlexReauthRequired::class.java,
            assertInstanceOf(Result.Error::class.java, repo.removeFromWatchlist(603, MediaType.MOVIE, "5d77")).error
        )

        coVerify(exactly = 0) { plexKtorService.addToWatchlist(any(), any()) }
        coVerify(exactly = 0) { plexKtorService.removeFromWatchlist(any(), any()) }
    }

    private companion object {
        const val PLEX_TOKEN_KEY = "plex_token"
        const val REAUTH_MESSAGE = "Reconnect your Plex account to manage your watchlist."
    }
}
