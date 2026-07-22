// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library.kmp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.google.services) apply false
    id("org.sonarqube") version "7.2.3.7755"

    id("io.sentry.android.gradle") version "6.0.0" apply false
}

sonar {
    properties {
        property("sonar.projectKey", "lusky3_underseerr")
        property("sonar.organization", "lusk")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

buildscript {
    dependencies {
        constraints {
            classpath("org.apache.commons:commons-compress:1.27.1") {
                because("Fixes CVE-2024-25710 and CVE-2024-26308")
            }
            classpath("com.google.protobuf:protobuf-java:3.25.5") {
                because("Fixes Denial of Service vulnerability")
            }
            // Fix transitive vulnerabilities in AGP (Netty, JDOM, Jose4j, Guava)
            classpath("io.netty:netty-codec:4.2.13.Final") { because("Fixes security vulnerabilities") }
            classpath("io.netty:netty-codec-http2:4.2.13.Final") { because("Fixes security vulnerabilities") }
            classpath("io.netty:netty-handler:4.2.13.Final") { because("Fixes security vulnerabilities") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("Fixes XXE vulnerability") }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") { because("Fixes DoS vulnerability") }
            classpath("com.google.guava:guava:33.0.0-android") { because("Fixes insecure temp dir") }
        }
    }
}

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

// Fails the build if AppChangelog.kt's top entry doesn't match the single
// source of truth version in gradle/libs.versions.toml. androidApp's
// assembleRelease/bundleRelease depend on this, so a release can't ship
// with a stale in-app changelog entry.
tasks.register("verifyAppVersion") {
    group = "verification"
    description = "Checks AppChangelog.kt's top entry against libs.versions.toml's appVersionCode/appVersionName."

    val expectedCode = libs.versions.appVersionCode.get()
    val expectedName = libs.versions.appVersionName.get()
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

// Force upgrade vulnerable transitive dependencies across all subprojects
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Netty vulnerabilities - upgrade to patched versions
            force("io.netty:netty-codec:4.2.13.Final")
            force("io.netty:netty-codec-http:4.2.13.Final")
            force("io.netty:netty-codec-http2:4.2.13.Final")
            force("io.netty:netty-common:4.2.13.Final")
            force("io.netty:netty-handler:4.2.13.Final")
            force("io.netty:netty-buffer:4.2.13.Final")
            force("io.netty:netty-transport:4.2.13.Final")
            force("io.netty:netty-resolver:4.2.13.Final")
            
            // Protobuf vulnerabilities - CVE for DoS
            force("com.google.protobuf:protobuf-java:4.34.0")
            force("com.google.protobuf:protobuf-kotlin:4.34.0")
            force("com.google.protobuf:protobuf-java-util:4.34.0")
            
            // JDOM2 XXE vulnerability
            force("org.jdom:jdom2:2.0.6.1")
            
            // jose4j DoS via compressed JWE
            force("org.bitbucket.b_c:jose4j:0.9.6")
            
            // Commons Lang3 uncontrolled recursion
            force("org.apache.commons:commons-lang3:3.20.0")

            // Play Services Basement - MAID vulnerability (CVE-2022-2390)
            force("com.google.android.gms:play-services-basement:18.10.0")
            
            // Kotlin stdlib - Information Exposure (SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744)
            // Android Test Platform pulls old kotlin-stdlib, force to project version
            force("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.20")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.3.20")

            // Guava - Insecure use of temporary directory (CVE-2023-2976)
            force("com.google.guava:guava:33.0.0-android")

            // AndroidX Concurrent - resolve conflict between runtime (1.1.0) and test deps (1.2.0)
            force("androidx.concurrent:concurrent-futures:1.2.0")
            force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
        }
    }
}
