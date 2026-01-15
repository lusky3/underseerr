package app.lusk.client.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatDate(timestamp: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter()
    formatter.setDateFormat("MMM dd, yyyy")
    formatter.setLocale(NSLocale.currentLocale)
    return formatter.stringFromDate(date)
}
