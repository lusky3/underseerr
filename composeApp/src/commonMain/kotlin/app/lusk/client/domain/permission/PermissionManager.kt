package app.lusk.client.domain.permission

import kotlinx.coroutines.flow.Flow

interface PermissionManager {
    fun isPermissionGranted(permission: Permission): Boolean
    suspend fun requestPermission(permission: Permission): Boolean
    fun shouldShowRationale(permission: Permission): Boolean
    fun openAppSettings()
}

enum class Permission {
    NOTIFICATIONS,
    CAMERA
}
