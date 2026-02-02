plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    id("io.sentry.android.gradle")
}

android {
    namespace = "app.lusk.underseerr"
    compileSdk = libs.versions.compileSdk.get().toInt()


    defaultConfig {
        applicationId = "app.lusk.underseerr"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable property-based testing
        testOptions {
            unitTests.isReturnDefaultValues = true
            unitTests.isIncludeAndroidResources = true
        }
    }

    signingConfigs {
        val keystorePath = System.getenv("KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
            // Sentry DSN - empty by default, set via SENTRY_DSN env var
            buildConfigField("String", "SENTRY_DSN", "\"${System.getenv("SENTRY_DSN") ?: ""}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            
            // Inclusion of native debug symbols for Google Play Console
            ndk {
                debugSymbolLevel = "FULL"
            }

            // Sentry DSN - empty by default, set via SENTRY_DSN env var
            buildConfigField("String", "SENTRY_DSN", "\"${System.getenv("SENTRY_DSN") ?: ""}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.workmanager)

    // Database (Room KMP in composeApp, but keeping runtime for safety if needed, can likely be removed if unused in androidApp sources)
    implementation(libs.androidx.work.runtime.ktx)

    // Image Loading
    implementation(libs.coil.compose)

    // Security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Other
    implementation(libs.androidx.splashscreen)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // Sentry - Error monitoring (optional, only active if DSN is configured)
    implementation("io.sentry:sentry-android:8.31.0")
    implementation("io.sentry:sentry-compose-android:8.31.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.framework.datatest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.okhttp.logging)
    debugImplementation(libs.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
}

// JaCoCo Configuration
apply(plugin = "jacoco")

configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val buildDir = layout.buildDirectory.get().asFile
    
    val classDirectoriesTree = fileTree("$buildDir/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*"
        )
    } + fileTree("${project(":composeApp").layout.buildDirectory.get().asFile}/intermediates/runtime_library_classes_dir/androidMain/bundleLibRuntimeToDirAndroidMain") {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "lusk/composeapp/generated/**/*.*"
        )
    }
    
    val sourceDirectoriesTree = files(
        "${project.projectDir}/src/main/java", 
        "${project.projectDir}/src/main/kotlin",
        "${project(":composeApp").projectDir}/src/commonMain/kotlin",
        "${project(":composeApp").projectDir}/src/androidMain/kotlin"
    )
    
    val executionDataTree = fileTree("$buildDir") {
        include(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            "jacoco/testDebugUnitTest.exec"
        )
    } 
    
    sourceDirectories.setFrom(sourceDirectoriesTree)
    classDirectories.setFrom(classDirectoriesTree)
    executionData.setFrom(executionDataTree)
}

// Sentry configuration for crash reporting
sentry {
    org.set(System.getenv("SENTRY_ORG") ?: "")
    projectName.set(System.getenv("SENTRY_PROJECT") ?: "")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN") ?: "")

    // Upload source code to Sentry for better stack traces
    // Only works when auth token is provided
    val hasAuthToken = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrBlank()
    includeSourceContext.set(hasAuthToken)
    
    // Auto-install Sentry SDK (we already added it manually)
    autoInstallation.enabled.set(false)
}

// Disable Sentry upload tasks if no auth token is present
tasks.configureEach {
    if (name.contains("uploadSentryProguardMappings") || name.contains("uploadSentryNativeSymbols")) {
        enabled = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrBlank()
    }
}
