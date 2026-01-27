package app.lusk.underseerr.domain.model

/**
 * Represents quota information for a specific media type.
 */
data class QuotaInfo(
    val limit: Int?,
    val remaining: Int?,
    val days: Int?
)
