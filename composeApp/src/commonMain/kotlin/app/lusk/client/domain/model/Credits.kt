package app.lusk.client.domain.model

data class CastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profilePath: String?,
    val order: Int
)

data class CrewMember(
    val id: Int,
    val name: String,
    val job: String?,
    val profilePath: String?
)
