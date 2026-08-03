package app.lusk.underseerr.util

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * `BuildKonfig.DEBUG` was hardcoded to `"true"` in `defaultConfigs` with no release
 * override, so `AppConfig.isDebug` reported true in **every** Android build. That
 * silently pointed release builds at the staging push-notification Worker and made
 * every `if (isDebug)` guard a no-op in production.
 *
 * Debug-ness now comes from the APK's own `android:debuggable` flag, which only the
 * debug build type carries.
 *
 * These tests go through [initAppConfig] rather than a test-only setter: that is the
 * one entry point production uses, and `composeApp` is a published library, so a
 * public mutator for the debug flag would ship to every consumer.
 */
class AppConfigTest {

    @AfterEach
    fun reset() = initAppConfig(appWithFlags(0))

    @Test
    fun `debuggable is read from the application info flag`() {
        initAppConfig(appWithFlags(ApplicationInfo.FLAG_DEBUGGABLE))
        assertTrue(AppConfig.isDebug)

        initAppConfig(appWithFlags(ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_INSTALLED))
        assertTrue(AppConfig.isDebug, "must isolate the debuggable bit, not compare the whole mask")
    }

    @Test
    fun `a release APK is not reported as debug`() {
        initAppConfig(appWithFlags(0))
        assertFalse(AppConfig.isDebug)

        initAppConfig(appWithFlags(ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_ALLOW_BACKUP))
        assertFalse(AppConfig.isDebug, "unrelated flags must not read as debuggable")
    }

    @Test
    fun `debug mode is never sticky`() {
        // The field starts false, so a missed initAppConfig fails closed: production
        // behaviour rather than a release build quietly logging credentials. Once set,
        // it must also be able to go back off — a latched flag would be the same bug
        // in a slower form.
        initAppConfig(appWithFlags(ApplicationInfo.FLAG_DEBUGGABLE))
        assertTrue(AppConfig.isDebug)

        initAppConfig(appWithFlags(0))
        assertFalse(AppConfig.isDebug)
    }

    @Test
    fun `worker endpoint follows the build instead of always being staging`() {
        initAppConfig(appWithFlags(ApplicationInfo.FLAG_DEBUGGABLE))
        val debugEndpoint = defaultWorkerEndpoint

        initAppConfig(appWithFlags(0))
        val releaseEndpoint = defaultWorkerEndpoint

        // The endpoints come from .env.local / CI secrets; nothing to compare if
        // this checkout has neither configured.
        assumeTrue(
            debugEndpoint.isNotEmpty() && releaseEndpoint.isNotEmpty(),
            "worker endpoints not configured in this environment"
        )
        assertNotEquals(
            debugEndpoint,
            releaseEndpoint,
            "release builds must reach the production Worker, not staging"
        )
    }

    /** A [PlatformContext] whose app reports exactly [flags] in its `ApplicationInfo`. */
    private fun appWithFlags(flags: Int): PlatformContext {
        val info = ApplicationInfo().also { it.flags = flags }
        val context = mockk<Context> { every { applicationInfo } returns info }
        return PlatformContext(context)
    }
}
