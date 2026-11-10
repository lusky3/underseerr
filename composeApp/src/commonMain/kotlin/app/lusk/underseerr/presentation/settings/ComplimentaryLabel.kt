package app.lusk.underseerr.presentation.settings

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * Subscription-row text while free access is granted to everyone. The server
 * sets the end date at the end of a UTC day, so it is shown in UTC to avoid
 * reading as the next day east of Greenwich.
 */
fun complimentarySubtitle(expiresAt: Long?): String {
    val until = expiresAt?.let {
        val date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
        " until ${MONTHS[date.month.ordinal]} ${date.day}, ${date.year}"
    } ?: ""
    return "Free during alpha$until*\n*May end sooner, subject to capacity"
}
