package app.lusk.client.util

import kotlin.experimental.ExperimentalNativeApi

actual object AppConfig {
    @OptIn(ExperimentalNativeApi::class)
    actual val isDebug: Boolean = Platform.isDebugBinary
}
