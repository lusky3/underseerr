package app.lusk.underseerr.util

import app.lusk.underseerr.shared.BuildKonfig

actual object AppConfig {
    actual val isDebug: Boolean = BuildKonfig.DEBUG
}
