package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiPushKeys(
    val auth: String,
    val p256dh: String
)

@Serializable
data class ApiRegisterPushSubscription(
    val endpoint: String,
    val keys: ApiPushKeys,
    val userAgent: String = "Underseerr-Android"
)
