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
    id("org.sonarqube") version "7.2.2.6593"

    id("io.sentry.android.gradle") version "6.0.0" apply false
}

sonar {
    properties {
        property("sonar.projectKey", "lusky3_overseerr-requests")
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
        }
    }
}

// Force upgrade vulnerable transitive dependencies across all subprojects
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Netty vulnerabilities - upgrade to patched versions
            force("io.netty:netty-codec:4.2.9.Final")
            force("io.netty:netty-codec-http:4.2.9.Final")
            force("io.netty:netty-codec-http2:4.2.9.Final")
            force("io.netty:netty-common:4.2.9.Final")
            force("io.netty:netty-handler:4.2.9.Final")
            force("io.netty:netty-buffer:4.2.9.Final")
            force("io.netty:netty-transport:4.2.9.Final")
            force("io.netty:netty-resolver:4.2.9.Final")
            
            // Protobuf vulnerabilities - CVE for DoS
            force("com.google.protobuf:protobuf-java:4.29.3")
            force("com.google.protobuf:protobuf-kotlin:4.29.3")
            force("com.google.protobuf:protobuf-java-util:4.29.3")
            
            // JDOM2 XXE vulnerability
            force("org.jdom:jdom2:2.0.6.1")
            
            // jose4j DoS via compressed JWE
            force("org.bitbucket.b_c:jose4j:0.9.6")
            
            // Commons Lang3 uncontrolled recursion
            force("org.apache.commons:commons-lang3:3.17.0")

            // Play Services Basement - MAID vulnerability (CVE-2022-2390)
            force("com.google.android.gms:play-services-basement:18.5.0")
            
            // Kotlin stdlib - Information Exposure (SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744)
            // Android Test Platform pulls old kotlin-stdlib, force to project version
            force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0")
        }
    }
}
