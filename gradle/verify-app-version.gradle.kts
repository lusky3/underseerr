// Fails the build if AppChangelog.kt's top entry doesn't match the single
// source of truth version in gradle/libs.versions.toml. androidApp's
// assembleRelease/bundleRelease depend on this, so a release can't ship
// with a stale in-app changelog entry.
//
// Kept in its own applied script (rather than inline in build.gradle.kts)
// so Codacy's Lizard complexity scan, which measures Kotlin script bodies
// crudely, scopes it to just this file.

// A plain object (not a top-level script function) so calling it from a
// task's doLast doesn't capture an unserializable reference to the build
// script itself, which would break the configuration cache.
object ChangelogVersionCheck {
    // Returns human-readable mismatches between AppChangelog.kt's top entry
    // and the expected app version, or an empty list if they match.
    fun findMismatches(changelogText: String, expectedCode: String, expectedName: String): List<String> {
        val actualCode = Regex("""versionCode\s*=\s*(\d+)""").find(changelogText)?.groupValues?.get(1)
        val actualName = Regex("""versionName\s*=\s*"([^"]+)"""").find(changelogText)?.groupValues?.get(1)

        val problems = mutableListOf<String>()
        if (actualCode != expectedCode) {
            problems += "top entry's versionCode is $actualCode, expected $expectedCode"
        }
        if (actualName != expectedName) {
            problems += "top entry's versionName is $actualName, expected $expectedName"
        }
        return problems
    }
}

tasks.register("verifyAppVersion") {
    group = "verification"
    description = "Checks AppChangelog.kt's top entry against libs.versions.toml's appVersionCode/appVersionName."

    val expectedCode = project.extra["expectedAppVersionCode"] as String
    val expectedName = project.extra["expectedAppVersionName"] as String
    val changelogFile = file("composeApp/src/commonMain/kotlin/app/lusk/underseerr/domain/model/AppChangelog.kt")

    inputs.file(changelogFile)
    inputs.property("expectedCode", expectedCode)
    inputs.property("expectedName", expectedName)

    doLast {
        val problems = ChangelogVersionCheck.findMismatches(changelogFile.readText(), expectedCode, expectedName)
        if (problems.isNotEmpty()) {
            throw GradleException(
                "AppChangelog.kt is out of sync with gradle/libs.versions.toml " +
                    "(appVersionCode=$expectedCode, appVersionName=$expectedName): " +
                    "${problems.joinToString("; ")}. " +
                    "Add a new top entry to AppChangelog.kt matching the bumped version."
            )
        }
    }
}
