package app.lusk.underseerr.presentation.discovery

import app.lusk.underseerr.domain.model.AppError
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.repository.DiscoveryRepository
import app.lusk.underseerr.domain.repository.HomeScreenConfig
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.domain.repository.ProfileRepository
import app.lusk.underseerr.domain.repository.RequestRepository
import app.lusk.underseerr.domain.repository.SettingsRepository
import app.lusk.underseerr.domain.repository.WatchlistRepository
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * What the user is actually told when a watchlist mutation fails.
 *
 * Two separate defects meet here:
 *  - `AppError.message` is the *internal* string ("Client error: 500", "Could not
 *    find Plex ratingKey for TMDB ID 603"). Emitting it leaks implementation detail
 *    into a snackbar; `getUserMessage()` is the sanctioned accessor.
 *  - `PlexReauthRequired` is already a complete sentence written for the user, so
 *    the "Failed to add: " prefix turns it into a developer note stapled to a real
 *    message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistUiEventTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)

    private fun viewModel(): DiscoveryViewModel {
        val discoveryRepository = mockk<DiscoveryRepository>(relaxed = true)
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val issueRepository = mockk<IssueRepository>(relaxed = true)
        every { settingsRepository.getHomeScreenConfig() } returns flowOf(HomeScreenConfig())

        return DiscoveryViewModel(
            discoveryRepository,
            watchlistRepository,
            requestRepository,
            profileRepository,
            ProfileViewModel(profileRepository, requestRepository),
            IssueViewModel(issueRepository),
            RequestViewModel(requestRepository, settingsRepository, mockk(relaxed = true)),
            settingsRepository
        )
    }

    /** Collects everything [DiscoveryViewModel.uiEvent] emits while [block] runs. */
    private fun eventsFrom(block: suspend (DiscoveryViewModel) -> Unit): List<String> {
        val messages = mutableListOf<String>()
        runTest(testDispatcher) {
            val vm = viewModel()
            val collector = launch { vm.uiEvent.toList(messages) }
            runCurrent() // the SharedFlow has replay 0 — subscribe before emitting
            block(vm)
            advanceUntilIdle()
            collector.cancel()
        }
        return messages
    }

    @Test
    fun `a Plex re-link prompt is shown verbatim when adding`() {
        coEvery { watchlistRepository.addToWatchlist(any(), any(), any()) } returns
            Result.error(AppError.PlexReauthRequired())

        val messages = eventsFrom { it.addToWatchlist(603, MediaType.MOVIE) }

        assertEquals(listOf("Reconnect your Plex account to manage your watchlist."), messages)
    }

    @Test
    fun `a Plex re-link prompt is shown verbatim when removing`() {
        coEvery { watchlistRepository.removeFromWatchlist(any(), any(), any()) } returns
            Result.error(AppError.PlexReauthRequired())

        val messages = eventsFrom { it.removeFromWatchlist(603, MediaType.MOVIE, null) }

        assertEquals(listOf("Reconnect your Plex account to manage your watchlist."), messages)
    }

    @Test
    fun `other failures keep a prefix but never leak the internal message`() {
        coEvery { watchlistRepository.addToWatchlist(any(), any(), any()) } returns
            Result.error(AppError.HttpError(500, "Server error: 500 at /api/v1/watchlist"))

        val messages = eventsFrom { it.addToWatchlist(603, MediaType.MOVIE) }

        assertEquals(listOf("Failed to add: Server error. Please try again later."), messages)
        messages.forEach {
            assertFalse(it.contains("/api/v1"), "leaked internal detail: $it")
        }
    }

    @Test
    fun `an unknown error does not surface its raw text`() {
        coEvery { watchlistRepository.removeFromWatchlist(any(), any(), any()) } returns
            Result.error(AppError.UnknownError("Could not find Plex ratingKey for TMDB ID 603"))

        val messages = eventsFrom { it.removeFromWatchlist(603, MediaType.MOVIE, null) }

        assertEquals(listOf("Failed to remove: An unexpected error occurred. Please try again."), messages)
    }
}
