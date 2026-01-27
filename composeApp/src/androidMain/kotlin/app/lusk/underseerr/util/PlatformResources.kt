package app.lusk.underseerr.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getAppIcon(): Any? {
    val context = LocalContext.current
    return context.resources.getIdentifier("app_icon_transparent", "drawable", context.packageName)
        .takeIf { it != 0 }
}
