package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiCredits(
    @SerialName("cast")
    val cast: List<ApiCastMember> = emptyList(),
    @SerialName("crew")
    val crew: List<ApiCrewMember> = emptyList()
)

@Serializable
data class ApiCastMember(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("character")
    val character: String? = null,
    @SerialName("profilePath")
    val profilePath: String? = null,
    @SerialName("order")
    val order: Int = 0
)

@Serializable
data class ApiCrewMember(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("job")
    val job: String? = null,
    @SerialName("profilePath")
    val profilePath: String? = null
)
