package app.lusk.underseerr.domain.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AndroidPermissionManager(private val context: Context, private val activityProvider: () -> Activity?) : PermissionManager {

    override fun isPermissionGranted(permission: Permission): Boolean {
        if (permission == Permission.NOTIFICATIONS) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        }

        val androidPermission = permission.toAndroidPermission() ?: return true
        return ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        if (isPermissionGranted(permission)) return true

        val androidPermission = permission.toAndroidPermission() ?: return true
        val activity = activityProvider() ?: return false
        
        ActivityCompat.requestPermissions(activity, arrayOf(androidPermission), 101)
        return false // We can't guarantee result immediately here without a complex callback setup
    }

    override fun shouldShowRationale(permission: Permission): Boolean {
        if (isPermissionGranted(permission)) return false
        val androidPermission = permission.toAndroidPermission() ?: return false
        val activity = activityProvider() ?: return false
        
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermission)
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun Permission.toAndroidPermission(): String? {
        return when (this) {
            Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            Permission.CAMERA -> Manifest.permission.CAMERA
        }
    }
}
