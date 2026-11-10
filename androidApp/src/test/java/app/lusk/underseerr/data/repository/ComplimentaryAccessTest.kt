package app.lusk.underseerr.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.remote.api.SubscriptionKtorService
import app.lusk.underseerr.data.remote.api.SubscriptionResponse
import app.lusk.underseerr.domain.billing.BillingManager
import app.lusk.underseerr.domain.model.Result
import app.lusk.underseerr.domain.model.SubscriptionTier
import app.lusk.underseerr.domain.model.UserProfile
import app.lusk.underseerr.domain.repository.AuthRepository
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.nowMillis
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The cached "free for everyone" window: how it becomes a tier, how it is
 * refreshed from the server, and that it never overrides a paid state.
 */
class ComplimentaryAccessTest {

    private val dataStoreFile = File.createTempFile("complimentary", ".preferences_pb")
        .also { it.delete() }
    private val preferences = PreferencesManager(
        PreferenceDataStoreFactory.create(produceFile = { dataStoreFile })
    )

    private val service = mockk<SubscriptionKtorService>()
    private val user = mockk<UserProfile> { every { id } returns USER_ID }
    private val auth = mockk<AuthRepository> {
        coEvery { getCurrentUser() } returns Result.Success(user)
    }
    private val billing = mockk<BillingManager> {
        every { isSubscribed } returns MutableStateFlow(false)
        every { purchaseDetails } returns MutableSharedFlow()
    }

    private val repository = SubscriptionRepositoryImpl(
        preferences, mockk<SecurityManager>(relaxed = true), service, auth, billing
    )

    @AfterEach
    fun tearDown() {
        dataStoreFile.delete()
    }

    private fun serverGrants(until: Long?) {
        coEvery { service.checkSubscriptionStatus(USER_ID.toString()) } returns SubscriptionResponse(
            isPremium = false,
            expiresAt = until,
            isComplimentary = until != null,
            complimentaryUntil = until
        )
    }

    private suspend fun tier() = repository.getSubscriptionStatus().first().tier

    @Test
    fun `a user with nothing is FREE`() = runBlocking {
        assertEquals(SubscriptionTier.FREE, tier())
    }

    @Test
    fun `a granted window becomes the COMPLIMENTARY tier, even for a user who never started a trial`() = runBlocking {
        assertNull(preferences.getTrialStartDate().first())
        val until = nowMillis() + DAY
        serverGrants(until)

        val granted = repository.syncComplimentaryAccess().getOrThrow()

        assertTrue(granted)
        val status = repository.getSubscriptionStatus().first()
        assertEquals(SubscriptionTier.COMPLIMENTARY, status.tier)
        assertEquals(until, status.expiresAt)
        assertTrue(status.hasHostedAccess)
    }

    @Test
    fun `a window that has passed no longer counts, even if still cached`() = runBlocking {
        preferences.setComplimentaryUntil(nowMillis() - 1)

        assertEquals(SubscriptionTier.FREE, tier())
    }

    @Test
    fun `the server turning it off clears the cached window`() = runBlocking {
        serverGrants(nowMillis() + DAY)
        repository.syncComplimentaryAccess()
        assertEquals(SubscriptionTier.COMPLIMENTARY, tier())

        serverGrants(null)
        val granted = repository.syncComplimentaryAccess().getOrThrow()

        assertFalse(granted)
        assertNull(preferences.getComplimentaryUntil().first())
        assertEquals(SubscriptionTier.FREE, tier())
    }

    @Test
    fun `an unreachable server keeps the cache and reports a failure`() = runBlocking {
        val until = nowMillis() + DAY
        preferences.setComplimentaryUntil(until)
        coEvery { service.checkSubscriptionStatus(any()) } throws java.io.IOException("offline")

        val result = repository.syncComplimentaryAccess()

        assertTrue(result.isFailure)
        assertEquals(until, preferences.getComplimentaryUntil().first())
        assertEquals(SubscriptionTier.COMPLIMENTARY, tier())
    }

    @Test
    fun `premium wins over the complimentary window and is not touched by the sync`() = runBlocking {
        preferences.setIsPremium(true)
        preferences.setPremiumExpiresAt(nowMillis() + 5 * DAY)
        serverGrants(nowMillis() + DAY)

        repository.syncComplimentaryAccess()

        assertEquals(SubscriptionTier.PREMIUM, tier())
        assertTrue(preferences.getIsPremium().first())
    }

    @Test
    fun `a collector that stays open sees access end at the deadline`() = runBlocking {
        preferences.setComplimentaryUntil(nowMillis() + 400L)

        val tiers = withTimeout(5_000L) {
            repository.getSubscriptionStatus().map { it.tier }.take(2).toList()
        }

        assertEquals(listOf(SubscriptionTier.COMPLIMENTARY, SubscriptionTier.FREE), tiers)
    }

    private companion object {
        const val USER_ID = 7
        const val DAY = 24L * 60 * 60 * 1000
    }
}
