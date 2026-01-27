package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiSystemSettings(
    val partialRequestsEnabled: Boolean = false
)
