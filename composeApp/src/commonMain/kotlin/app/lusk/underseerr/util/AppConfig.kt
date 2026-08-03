package app.lusk.underseerr.util

import app.lusk.underseerr.shared.BuildKonfig

expect object AppConfig {
    /**
     * True only for a genuinely debuggable build.
     *
     * This deliberately does not come from a BuildKonfig constant: BuildKonfig
     * generates one value for the whole compilation and cannot see the Android
     * debug/release variant, so a `DEBUG` constant there silently reported
     * "debug" in release builds. Each platform now resolves this from something
     * that actually tracks the build being run.
     */
    val isDebug: Boolean

    val versionCode: Int
    val versionName: String
}

/**
 * Resolves the platform values that need a context.
 *
 * Must be called once during DI setup, before anything reads [AppConfig]. Every
 * current reader is lazily evaluated (inside a function or a Koin `single { }`
 * block), so calling this at the top of `sharedModule` is enough.
 */
expect fun initAppConfig(context: PlatformContext)

/**
 * Push notifications proxy through a Cloudflare Worker, and debug builds must not
 * talk to the production one. Defined once so the call sites that need it cannot
 * drift apart.
 */
val defaultWorkerEndpoint: String
    get() = if (AppConfig.isDebug) {
        BuildKonfig.WORKER_ENDPOINT_STAGING
    } else {
        BuildKonfig.WORKER_ENDPOINT_PROD
    }
