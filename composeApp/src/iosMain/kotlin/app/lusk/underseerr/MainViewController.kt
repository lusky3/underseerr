package app.lusk.underseerr

import androidx.compose.ui.window.ComposeUIViewController
import app.lusk.underseerr.presentation.main.MainScreen
import app.lusk.underseerr.di.initKoin
import app.lusk.underseerr.util.PlatformContext
import platform.darwin.NSObject

fun MainViewController() = ComposeUIViewController {
    MainScreen()
}

fun initKoin() {
    initKoin(PlatformContext())
}
