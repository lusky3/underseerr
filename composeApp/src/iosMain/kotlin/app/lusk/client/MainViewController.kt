package app.lusk.client

import androidx.compose.ui.window.ComposeUIViewController
import app.lusk.client.presentation.main.MainScreen
import app.lusk.client.di.initKoin
import app.lusk.client.util.PlatformContext
import platform.darwin.NSObject

fun MainViewController() = ComposeUIViewController {
    MainScreen()
}

fun initKoin() {
    initKoin(NSObject())
}
