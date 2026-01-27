package app.lusk.underseerr.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatDate(timestamp: Long): String {
    // 978307200.0 is the number of seconds between 1970-01-01 and 2001-01-01 (Reference Date)
    val date = NSDate(timeIntervalSinceReferenceDate = (timestamp / 1000.0) - 978307200.0)
    val formatter = NSDateFormatter()
    formatter.setDateFormat("MMM dd, yyyy")
    formatter.setLocale(NSLocale.currentLocale)
    return formatter.stringFromDate(date)
}
