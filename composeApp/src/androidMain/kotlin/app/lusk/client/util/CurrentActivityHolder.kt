package app.lusk.client.util

import android.app.Activity
import java.lang.ref.WeakReference

object CurrentActivityHolder {
    private var currentActivity: WeakReference<Activity?> = WeakReference(null)

    fun set(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun get(): Activity? {
        return currentActivity.get()
    }

    fun clear() {
        currentActivity = WeakReference(null)
    }
}
