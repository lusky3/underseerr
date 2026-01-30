package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiUserNotificationSettings(
    val emailEnabled: Boolean? = null,
    val webPushEnabled: Boolean? = null,
    val telegramEnabled: Boolean? = null,
    val discordEnabled: Boolean? = null,
    val gotifyEnabled: Boolean? = null,
    val notificationTypes: ApiNotificationTypes,
    val gotify: ApiGotifySettings? = null
)

@Serializable
data class ApiNotificationTypes(
    val webpush: Int? = null,
    val email: Int? = null,
    val discord: Int? = null,
    val telegram: Int? = null,
    val gotify: Int? = null
)

@Serializable
data class ApiGotifySettings(
    val enabled: Boolean,
    val options: ApiGotifyOptions
)

@Serializable
data class ApiGotifyOptions(
    val url: String,
    val token: String
)
