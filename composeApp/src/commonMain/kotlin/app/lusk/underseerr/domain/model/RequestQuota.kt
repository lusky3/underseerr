package app.lusk.underseerr.domain.model

/**
 * Represents request quotas for movies and TV shows.
 */
data class RequestQuota(
    val movie: QuotaInfo,
    val tv: QuotaInfo
)
