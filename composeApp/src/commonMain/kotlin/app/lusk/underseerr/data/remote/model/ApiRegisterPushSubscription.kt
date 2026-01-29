package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiRegisterPushSubscription(
    val endpoint: String,
    val auth: String,
    val p256dh: String,
    val userAgent: String = "Underseerr-Android"
)
