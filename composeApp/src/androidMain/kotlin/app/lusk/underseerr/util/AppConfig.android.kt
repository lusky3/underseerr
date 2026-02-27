package app.lusk.underseerr.util

import app.lusk.underseerr.shared.BuildKonfig

actual object AppConfig {
    actual val isDebug: Boolean = BuildKonfig.DEBUG
    actual val versionCode: Int = BuildKonfig.VERSION_CODE
    actual val versionName: String = BuildKonfig.VERSION_NAME
}
