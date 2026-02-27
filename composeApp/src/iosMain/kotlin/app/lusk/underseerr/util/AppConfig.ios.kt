package app.lusk.underseerr.util

import app.lusk.underseerr.shared.BuildKonfig
import kotlin.experimental.ExperimentalNativeApi

actual object AppConfig {
    @OptIn(ExperimentalNativeApi::class)
    actual val isDebug: Boolean = Platform.isDebugBinary
    actual val versionCode: Int = BuildKonfig.VERSION_CODE
    actual val versionName: String = BuildKonfig.VERSION_NAME
}
