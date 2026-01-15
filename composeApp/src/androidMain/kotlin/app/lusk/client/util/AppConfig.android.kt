package app.lusk.client.util

import app.lusk.client.shared.BuildConfig

actual object AppConfig {
    actual val isDebug: Boolean = BuildConfig.DEBUG
}
