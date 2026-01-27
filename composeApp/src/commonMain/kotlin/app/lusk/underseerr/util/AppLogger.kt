package app.lusk.underseerr.util

/**
 * Platform-agnostic logger for the application.
 */
interface AppLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
}

/**
 * A simple logger that prints to console for platforms without a specific implementation.
 */
class ConsoleLogger : AppLogger {
    override fun d(tag: String, message: String) {
        println("DEBUG: [$tag] $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR: [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun i(tag: String, message: String) {
        println("INFO: [$tag] $message")
    }

    override fun w(tag: String, message: String) {
        println("WARN: [$tag] $message")
    }
}
