package app.lusk.client.data.remote.model

import kotlinx.serialization.Serializable

/**
 * API model for request quota.
 */
@Serializable
data class ApiRequestQuota(
    val movie: ApiQuotaInfo? = null,
    val tv: ApiQuotaInfo? = null
)

/**
 * API model for quota information.
 */
@Serializable
data class ApiQuotaInfo(
    val limit: Int? = null,
    val remaining: Int? = null,
    val days: Int? = null
)
