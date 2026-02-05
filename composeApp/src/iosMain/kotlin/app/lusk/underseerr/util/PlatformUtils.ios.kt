package app.lusk.underseerr.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens a URL in the device's default browser on iOS.
 */
actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
