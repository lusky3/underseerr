package app.lusk.underseerr.util

import android.content.pm.ApplicationInfo
import app.lusk.underseerr.shared.BuildKonfig

actual object AppConfig {
    /**
     * Defaults to false so a missed [initAppConfig] fails closed: production
     * behaviour (credential logging off, production endpoints) rather than a
     * release build quietly behaving like a debug one.
     *
     * There is deliberately no public mutator. This flag decides whether HTTP
     * logging runs and which Cloudflare Worker push notifications reach, and
     * `composeApp` is a published library — a `setDebuggableForTesting` here
     * would be shipped API that lets any caller flip a release build into debug
     * mode. [initAppConfig] is the only way in, for tests as well as production.
     */
    @Volatile
    internal var debuggable: Boolean = false

    actual val isDebug: Boolean get() = debuggable

    actual val versionCode: Int = BuildKonfig.VERSION_CODE
    actual val versionName: String = BuildKonfig.VERSION_NAME
}

/**
 * The APK carries `android:debuggable` only for the debug build type, so this
 * tracks the variant exactly — no build-script coordination required, and no way
 * for a release build to be mislabelled.
 */
internal fun isDebuggableApp(applicationInfoFlags: Int): Boolean =
    (applicationInfoFlags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

actual fun initAppConfig(context: PlatformContext) {
    AppConfig.debuggable = isDebuggableApp(context.context.applicationInfo.flags)
}
