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
            versionCode = 10,
            versionName = "1.0.8",
            title = "Sign-In Reliability & Security",
            changes = listOf(
                "🔐 An expired Overseerr session now renews automatically instead of failing when you open an item",
                "🚪 If your Plex sign-in is no longer valid, the app explains why and takes you back to sign-in",
                "📺 Watchlist still loads when Plex access is revoked, and asks you to reconnect Plex before adding or removing titles",
                "🛡️ Account credentials are no longer written to device logs",
                "🔔 Fixed push notifications using the wrong server in release builds",
                "🐛 Bug fixes and performance improvements"
            )
        ),
        ChangelogEntry(
            versionCode = 9,
            versionName = "1.0.7",
            title = "Bug Fixes & Security Updates",
            changes = listOf(
                "🐛 Fixed advanced request root folder path bug",
                "🐛 Fixed issue where failed requests incorrectly appeared as Available",
                "🛡️ Resolved multiple internal security vulnerabilities via Snyk",
                "📦 Maintained and updated overall project dependencies"
            )
        ),
        ChangelogEntry(
            versionCode = 8,
            versionName = "1.0.6",
            title = "Performance Improvements",
            changes = listOf(
                "✨ Minor app refinements and optimizations",
                "🐛 Bug fixes and performance improvements"
            )
        ),
        ChangelogEntry(
            versionCode = 7,
            versionName = "1.0.5",
            title = "Push Notification Trial & Security",
            changes = listOf(
                "🔔 Extended trial from 7 days to 30 days",
                "✨ New users now get an explicit trial activation prompt",
                "🔄 Expired trial users can reset their trial from Settings",
                "⏱️ Live countdown shows days remaining in your trial",
                "📦 Updated dependencies",
                "🐛 Bug fixes and performance improvements"
            )
        ),
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
