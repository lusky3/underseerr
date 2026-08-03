package app.lusk.underseerr.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Why the app dropped the user's session without them asking.
 */
enum class SessionExpiryReason {
    /** Overseerr refused the stored Plex token — it was revoked or expired at plex.tv. */
    PLEX_TOKEN_REVOKED,

    /** The session lapsed and nothing was stored that could re-establish it. */
    NO_CREDENTIALS_TO_REFRESH
}

val SessionExpiryReason.userMessage: String
    get() = when (this) {
        SessionExpiryReason.PLEX_TOKEN_REVOKED ->
            "Your Plex sign-in is no longer valid. Please sign in again."
        SessionExpiryReason.NO_CREDENTIALS_TO_REFRESH ->
            "Your session expired. Please sign in again."
    }

/**
 * Holds the reason for an involuntary sign-out so the UI can route to the login
 * screen and say why, instead of leaving the user on a screen whose every request
 * now fails.
 *
 * State rather than a one-shot event: the nav host reacts to it, and the sign-in
 * screen keeps showing the explanation until the user actually signs in again.
 */
class SessionExpiryNotifier {

    private val _reason = MutableStateFlow<SessionExpiryReason?>(null)
    val reason: StateFlow<SessionExpiryReason?> = _reason.asStateFlow()

    fun notifyExpired(reason: SessionExpiryReason) {
        _reason.value = reason
    }

    /** Clear once the user has signed in again. */
    fun consume() {
        _reason.value = null
    }
}
