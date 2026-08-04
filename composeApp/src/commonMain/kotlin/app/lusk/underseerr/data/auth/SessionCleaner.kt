package app.lusk.underseerr.data.auth

import app.lusk.underseerr.data.local.UnderseerrDatabase
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.util.AppLogger

/**
 * The account-scoped local cache, as far as signing out is concerned.
 *
 * Exists so [SessionCleaner] states what it needs without dragging Room into
 * every call site (the androidApp unit-test source set has no Room on its
 * compile classpath).
 */
fun interface LocalCacheCleaner {
    suspend fun clearAll()
}

/** The real cache: every Room table. */
class RoomCacheCleaner(private val database: UnderseerrDatabase) : LocalCacheCleaner {
    override suspend fun clearAll() = database.clearAllTables()
}

/**
 * The single definition of "sign this account out of this device".
 *
 * Two paths sign a user out: the explicit one (`AuthRepositoryImpl.logout()`)
 * and the involuntary one ([SessionRefresher] giving up on a dead session). Both
 * must leave the device in the same state, because Room holds account-scoped
 * rows — requests, issues, notifications, the user table, cached discovery — and
 * repositories read them back directly. If only the credentials were dropped,
 * the next Plex account to sign in on the device would be served the previous
 * account's cache. Keeping the sequence in one place is what stops the two paths
 * from drifting apart again.
 */
class SessionCleaner(
    private val securityManager: SecurityManager,
    private val preferencesManager: PreferencesManager,
    private val logger: AppLogger,
    /**
     * Nullable only so unit tests that never stand up a cache can still exercise
     * the credential half of a sign-out. Production wiring (KoinModule) always
     * supplies the Room-backed [RoomCacheCleaner].
     */
    private val cacheCleaner: LocalCacheCleaner?
) {
    /**
     * Drops every trace of the signed-in account: secure storage, auth
     * preferences, and the local cache.
     *
     * Every step is best effort and independent. Guarding only the cache was not
     * enough: `clearAuthData()` is a DataStore write and can fail with an
     * IOException, and letting that escape aborted the rest of the sign-out —
     * including the notification that tells the UI *why* the user was signed out,
     * leaving them on a screen whose every request fails with no explanation.
     * The cache is deliberately last, so a failure there cannot leave the
     * credentials behind and strand the app believing it is still signed in with
     * a session the server has already rejected.
     */
    suspend fun clear() {
        step("secure storage") { securityManager.clearSecureData() }
        step("auth preferences") { preferencesManager.clearAuthData() }

        val cache = cacheCleaner
        if (cache == null) {
            logger.w(TAG, "No local cache wired; leaving it in place.")
            return
        }
        step("the local cache") { cache.clearAll() }
    }

    /**
     * Runs one stage of the sign-out, logging rather than propagating a failure so
     * the remaining stages still run.
     *
     * Cancellation is rethrown: it is not a failed wipe, and on the JVM
     * `CancellationException` is an `IllegalStateException`, so a bare
     * `catch (e: Exception)` would silently absorb it.
     */
    private suspend fun step(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "Failed to clear $what on sign-out: ${e.message}", e)
        }
    }

    private companion object {
        const val TAG = "SessionCleaner"
    }
}
