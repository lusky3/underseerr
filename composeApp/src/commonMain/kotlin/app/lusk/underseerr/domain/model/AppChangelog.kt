package app.lusk.underseerr.domain.model

/**
 * Represents a single version's changelog entry.
 *
 * @param versionCode The build version code
 * @param versionName The user-visible version string (e.g. "1.0.4")
 * @param title Short title for this release
 * @param changes List of user-facing changes in this version
 */
data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val changes: List<String>
)

/**
 * Hardcoded changelog entries for the app.
 * 
 * Add new entries at the TOP of the list when releasing a new version.
 * The [versionCode] should match the one in androidApp/build.gradle.kts
 * and composeApp BuildKonfig.
 */
object AppChangelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            versionCode = 6,
            versionName = "1.0.4",
            title = "In-App Review & Polish",
            changes = listOf(
                "📝 Added in-app review prompt after successful requests",
                "📋 Added changelog popup on new version install",
                "🔔 Update available notice on Profile screen",
                "🐛 Bug fixes and performance improvements"
            )
        ),
        ChangelogEntry(
            versionCode = 5,
            versionName = "1.0.3",
            title = "Watchlist & Improvements",
            changes = listOf(
                "📺 Added watchlist functionality",
                "🎨 UI polish and improvements",
                "🐛 Fixed login connection errors"
            )
        ),
        ChangelogEntry(
            versionCode = 4,
            versionName = "1.0.2",
            title = "Premium Features",
            changes = listOf(
                "💎 Added premium subscription with Vibrant theme",
                "🔒 Biometric app lock",
                "🔔 Push notifications support"
            )
        )
    )

    /**
     * Returns changelog entries newer than the given version code.
     * If [lastSeenVersionCode] is 0, returns only the latest entry.
     */
    fun getEntriesSince(lastSeenVersionCode: Int): List<ChangelogEntry> {
        if (lastSeenVersionCode == 0) {
            return entries.take(1)
        }
        return entries.filter { it.versionCode > lastSeenVersionCode }
    }
}
