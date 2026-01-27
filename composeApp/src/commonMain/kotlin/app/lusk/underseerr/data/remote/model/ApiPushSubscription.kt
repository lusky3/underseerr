package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiPushSubscription(
    val endpoint: String,
    val expirationTime: Long? = null,
    val keys: ApiPushKeys
)

@Serializable
data class ApiPushKeys(
    val p256dh: String,
    val auth: String
)
