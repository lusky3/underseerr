package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiUserNotificationSettings(
    val emailEnabled: Boolean? = null,
    val webPushEnabled: Boolean? = null,
    val telegramEnabled: Boolean? = null,
    val discordEnabled: Boolean? = null,
    val notificationTypes: ApiNotificationTypes
)

@Serializable
data class ApiNotificationTypes(
    val webpush: Int? = null,
    val email: Int? = null,
    val discord: Int? = null,
    val telegram: Int? = null
)
