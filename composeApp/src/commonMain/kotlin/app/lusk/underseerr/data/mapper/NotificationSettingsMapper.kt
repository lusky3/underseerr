package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.remote.model.ApiUserNotificationSettings
import app.lusk.underseerr.domain.model.NotificationTypeMask
import app.lusk.underseerr.domain.repository.NotificationSettings

/**
 * Maps between API notification settings and domain settings.
 */
object NotificationSettingsMapper {

    fun mapApiToDomain(apiSettings: ApiUserNotificationSettings): NotificationSettings {
        val webpushMask = apiSettings.notificationTypes.webpush ?: 0
        val isEnabled = apiSettings.webPushEnabled == true

        return NotificationSettings(
            enabled = isEnabled,
            requestPendingApproval = (webpushMask and NotificationTypeMask.MEDIA_PENDING) != 0,
            requestApproved = (webpushMask and NotificationTypeMask.MEDIA_APPROVED) != 0,
            requestAutoApproved = (webpushMask and NotificationTypeMask.MEDIA_AUTO_APPROVED) != 0,
            requestDeclined = (webpushMask and NotificationTypeMask.MEDIA_DECLINED) != 0,
            requestProcessingFailed = (webpushMask and NotificationTypeMask.MEDIA_FAILED) != 0,
            requestAvailable = (webpushMask and NotificationTypeMask.MEDIA_AVAILABLE) != 0,
            issueReported = (webpushMask and NotificationTypeMask.ISSUE_CREATED) != 0,
            issueComment = (webpushMask and NotificationTypeMask.ISSUE_COMMENT) != 0,
            issueResolved = (webpushMask and NotificationTypeMask.ISSUE_RESOLVED) != 0,
            issueReopened = (webpushMask and NotificationTypeMask.ISSUE_REOPENED) != 0,
            mediaAutoRequested = (webpushMask and NotificationTypeMask.MEDIA_AUTO_REQUESTED) != 0,
            syncEnabled = true // Default, will be overwritten by local prefs in repository if needed
        )
    }

    fun calculateBitmask(settings: NotificationSettings): Int {
        var mask = 0
        if (settings.requestPendingApproval) mask = mask or NotificationTypeMask.MEDIA_PENDING
        if (settings.requestApproved) mask = mask or NotificationTypeMask.MEDIA_APPROVED
        if (settings.requestAutoApproved) mask = mask or NotificationTypeMask.MEDIA_AUTO_APPROVED
        if (settings.requestDeclined) mask = mask or NotificationTypeMask.MEDIA_DECLINED
        if (settings.requestProcessingFailed) mask = mask or NotificationTypeMask.MEDIA_FAILED
        if (settings.requestAvailable) mask = mask or NotificationTypeMask.MEDIA_AVAILABLE
        if (settings.issueReported) mask = mask or NotificationTypeMask.ISSUE_CREATED
        if (settings.issueComment) mask = mask or NotificationTypeMask.ISSUE_COMMENT
        if (settings.issueResolved) mask = mask or NotificationTypeMask.ISSUE_RESOLVED
        if (settings.issueReopened) mask = mask or NotificationTypeMask.ISSUE_REOPENED
        if (settings.mediaAutoRequested) mask = mask or NotificationTypeMask.MEDIA_AUTO_REQUESTED
        // Always include TEST_NOTIFICATION bit to ensure connectivity tests work
        mask = mask or NotificationTypeMask.TEST_NOTIFICATION
        return mask
    }
}
