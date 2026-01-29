package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiGlobalNotificationSettings(
    val webpush: ApiWebPushGlobalSettings? = null
)

@Serializable
data class ApiWebPushGlobalSettings(
    val enabled: Boolean = false,
    val types: Int = 0
)
