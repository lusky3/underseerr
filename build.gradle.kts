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
    // Netty reaches the build twice: here on the buildscript classpath (AGP pulls it
    // in) and again for the subprojects below. Both pins move together.
    val nettyVersion = "4.2.17.Final"
    val nettyReason = "Fixes security vulnerabilities"
    dependencies {
        constraints {
            classpath("org.apache.commons:commons-compress:1.27.1") {
                because("Fixes CVE-2024-25710 and CVE-2024-26308")
            }
            classpath("com.google.protobuf:protobuf-java:3.25.5") {
                because("Fixes Denial of Service vulnerability")
            }
            // Fix transitive vulnerabilities in AGP (Netty, JDOM, Jose4j, Guava)
            classpath("io.netty:netty-codec:$nettyVersion") { because(nettyReason) }
            classpath("io.netty:netty-codec-http2:$nettyVersion") { because(nettyReason) }
            classpath("io.netty:netty-handler:$nettyVersion") { because(nettyReason) }
            classpath("org.jdom:jdom2:2.0.6.1") { because("Fixes XXE vulnerability") }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") { because("Fixes DoS vulnerability") }
            classpath("com.google.guava:guava:33.0.0-android") { because("Fixes insecure temp dir") }
        }
    }
}

// Type-safe version catalog accessors (libs.versions.x) aren't available in
// scripts loaded via apply(from = ...), so pass the expected values through
// as extra properties instead.
extra["expectedAppVersionCode"] = libs.versions.appVersionCode.get()
extra["expectedAppVersionName"] = libs.versions.appVersionName.get()
apply(from = "gradle/verify-app-version.gradle.kts")

// Force upgrade vulnerable transitive dependencies across all subprojects
subprojects {
    val nettyVersion = "4.2.17.Final"

    // AGP resolves Lint itself through `androidLintTool`, and lint ships its own Kotlin.
    // Forcing the project's kotlin-stdlib onto that classpath makes AGP 9.3 fail to
    // construct detectors ("Can't initialize detector InferredThreadDetector"). Nothing
    // on lint's own classpaths is shipped in the app, so leave them unpinned.
    configurations
        .matching { it.name != "androidLintTool" && !it.name.endsWith("LintChecksClasspath") }
        .configureEach {
            resolutionStrategy {
                // Netty vulnerabilities - upgrade to patched versions
                force("io.netty:netty-codec:$nettyVersion")
                force("io.netty:netty-codec-http:$nettyVersion")
                force("io.netty:netty-codec-http2:$nettyVersion")
                force("io.netty:netty-common:$nettyVersion")
                force("io.netty:netty-handler:$nettyVersion")
                force("io.netty:netty-buffer:$nettyVersion")
                force("io.netty:netty-transport:$nettyVersion")
                force("io.netty:netty-resolver:$nettyVersion")
            
                // Protobuf vulnerabilities - CVE for DoS
                force("com.google.protobuf:protobuf-java:4.36.2")
                force("com.google.protobuf:protobuf-kotlin:4.36.2")
                force("com.google.protobuf:protobuf-java-util:4.36.2")
            
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
                force("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.4.20")
                force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.4.20")

                // Guava - Insecure use of temporary directory (CVE-2023-2976)
                force("com.google.guava:guava:33.0.0-android")

                // AndroidX Concurrent - resolve conflict between runtime (1.1.0) and test deps (1.2.0)
                force("androidx.concurrent:concurrent-futures:1.2.0")
                force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
            }
        }
}
