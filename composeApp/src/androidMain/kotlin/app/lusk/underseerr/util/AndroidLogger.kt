package app.lusk.underseerr.util

import android.util.Log

/**
 * Android implementation of AppLogger using android.util.Log.
 */
class AndroidLogger : AppLogger {
    // d/i are gated: they carry routine diagnostics that must not reach a release
    // device's logcat. w/e stay unconditional — those are wanted in crash reports.
    override fun d(tag: String, message: String) {
        if (AppConfig.isDebug) Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }

    override fun i(tag: String, message: String) {
        if (AppConfig.isDebug) Log.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
}
