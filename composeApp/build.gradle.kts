import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library.kmp)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.buildkonfig)
    `maven-publish`
}

kotlin {
    // androidTarget() is created automatically by com.android.kotlin.multiplatform.library
    targets.configureEach {
        if (name == "android") {
             (this as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget)?.let { android ->
                 @OptIn(ExperimentalKotlinGradlePluginApi::class)
                 android.compilerOptions {
                     jvmTarget.set(JvmTarget.JVM_17)
                 }
             }
        }
    }

    androidLibrary {
        namespace = "app.lusk.underseerr.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildkonfig {
        packageName = "app.lusk.underseerr.shared"
        
        // Load .env properties
        val envProps = Properties()
        val envFile = rootProject.file(".env.local")
        if (envFile.exists()) {
             FileInputStream(envFile).use { stream ->
                 envProps.load(stream)
             }
        }

        fun getEnv(key: String): String {
            return System.getenv(key) ?: envProps.getProperty(key) ?: ""
        }

        defaultConfigs {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "DEBUG", "true")
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "WORKER_ENDPOINT_PROD", getEnv("CLOUDFLARE_WORKER_ENDPOINT_PROD"))
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "WORKER_ENDPOINT_STAGING", getEnv("CLOUDFLARE_WORKER_ENDPOINT_STAGING"))
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
        }
    }

    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // Room KMP
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.core.bundle)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.network.okhttp)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.room.ktx) // Room Android specific helpers if needed
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.firebase.messaging)
            implementation(libs.androidx.core.ktx)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.coil.network.ktor)
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}



room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

sonar {
    properties {
        property("sonar.sources", "src/commonMain/kotlin,src/androidMain/kotlin,src/iosMain/kotlin")
    }
}
