package app.lusk.underseerr.domain.permission

class IosPermissionManager : PermissionManager {
    override fun isPermissionGranted(permission: Permission): Boolean {
        // TODO: Implement actual permission check
        return false
    }

    override suspend fun requestPermission(permission: Permission): Boolean {
        // TODO: Implement actual permission request
        return false
    }

    override fun shouldShowRationale(permission: Permission): Boolean {
        return false
    }

    override fun openAppSettings() {
        // TODO: Open settings
    }
}
