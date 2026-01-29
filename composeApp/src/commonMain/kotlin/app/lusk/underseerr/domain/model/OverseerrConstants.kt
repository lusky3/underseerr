package app.lusk.underseerr.domain.model

object AppPermissions {
    const val ADMIN = 2
    const val MANAGE_SETTINGS = 4
    const val MANAGE_USERS = 8
    const val MANAGE_REQUESTS = 16
    const val MANAGE_ISSUES = 32 // Check this value
    // ... others
}

object NotificationTypeMask {
    const val MEDIA_PENDING = 2
    const val MEDIA_APPROVED = 4
    const val MEDIA_AVAILABLE = 8
    const val MEDIA_FAILED = 16
    const val TEST_NOTIFICATION = 32
    const val MEDIA_DECLINED = 64
    const val MEDIA_AUTO_APPROVED = 128
    const val ISSUE_CREATED = 256
    const val ISSUE_COMMENT = 512
    const val ISSUE_RESOLVED = 1024
    const val ISSUE_REOPENED = 2048
}
