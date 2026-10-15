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
    // in) and again for the subprojects below. Both read `netty` from the version
    // catalog, so there is one value for Dependabot to move.
    val nettyVersion = libs.versions.netty.get()
    val nettyReason = "Fixes security vulnerabilities"
    // AGP itself pulls BouncyCastle 1.79-1.83; same catalog entry as the
    // subprojects pin further down.
    val bouncyCastleVersion = libs.versions.bouncycastle.get()
    val bouncyCastleReason = "Fixes GOST-28147 and timing-channel advisories"
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
            classpath("org.bitbucket.b_c:jose4j:0.9.7") { because("Fixes DoS vulnerability") }
            classpath("com.google.guava:guava:33.0.0-android") { because("Fixes insecure temp dir") }
            classpath("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion") { because(bouncyCastleReason) }
            classpath("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion") { because(bouncyCastleReason) }
            classpath("org.bouncycastle:bcutil-jdk18on:$bouncyCastleVersion") { because(bouncyCastleReason) }
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
    // Same catalog entry as the buildscript pin above.
    val nettyVersion = rootProject.libs.versions.netty.get()
    val bouncyCastleVersion = rootProject.libs.versions.bouncycastle.get()

    // Pins that are safe on every configuration, lint's own classpaths included
    // (lintDebug passes with them). Lint and the Unified Test Platform pull older
    // copies of each, which is where the remaining tooling advisories came from.
    configurations.configureEach {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
            force("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
            force("org.bouncycastle:bcutil-jdk18on:$bouncyCastleVersion")
            // Commons Lang3 uncontrolled recursion
            force("org.apache.commons:commons-lang3:3.20.0")
            // HttpClient XSS in URI normalization (CVE-2020-13956)
            force("org.apache.httpcomponents:httpclient:4.5.14")
        }
    }

    // Lint-check jars bring guava 31.1-android, below the CVE-2023-2976 fix. A floor
    // rather than a force: one of these classpaths already resolves 33.6.0-jre, and
    // lint's own classpath runs the -jre flavour, so pinning one exact guava here
    // would downgrade or mismatch it.
    configurations
        .matching { it.name.endsWith("LintChecksClasspath") }
        .configureEach {
            dependencyConstraints.add(
                project.dependencies.constraints.create("com.google.guava:guava:32.0.0-android")
            )
        }

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
                // gRPC (via the Unified Test Platform) pulls these at 4.1.x; without a pin
                // they stay a major behind the rest of netty on the same classpath.
                force("io.netty:netty-codec-socks:$nettyVersion")
                force("io.netty:netty-handler-proxy:$nettyVersion")
            
                // Protobuf vulnerabilities - CVE for DoS
                force("com.google.protobuf:protobuf-java:4.36.2")
                force("com.google.protobuf:protobuf-kotlin:4.36.2")
                force("com.google.protobuf:protobuf-java-util:4.36.2")
            
                // JDOM2 XXE vulnerability
                force("org.jdom:jdom2:2.0.6.1")
            
                // jose4j DoS via compressed JWE
                force("org.bitbucket.b_c:jose4j:0.9.7")
            
                // Play Services Basement - MAID vulnerability (CVE-2022-2390)
                force("com.google.android.gms:play-services-basement:18.12.0")
            
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
