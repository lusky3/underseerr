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
}

buildscript {
    dependencies {
        constraints {
            classpath("org.apache.commons:commons-compress:1.27.1") {
                because("Fixes CVE-2024-25710 and CVE-2024-26308")
            }
        }
    }
}
