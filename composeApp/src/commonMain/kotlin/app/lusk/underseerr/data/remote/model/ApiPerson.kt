package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.Serializable

/**
 * API model for person details from Overseerr.
 */
@Serializable
data class ApiPerson(
    val id: Int,
    val name: String,
    val biography: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val gender: Int? = null,
    val placeOfBirth: String? = null,
    val profilePath: String? = null,
    val knownForDepartment: String? = null,
    val combinedCredits: ApiPersonCredits? = null,
    val credits: ApiPersonCredits? = null
)

@Serializable
data class ApiPersonCredits(
    val cast: List<ApiPersonCredit>? = null,
    val crew: List<ApiPersonCredit>? = null
)

@Serializable
data class ApiPersonCredit(
    val id: Int,
    val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Double? = null,
    val character: String? = null,
    val department: String? = null,
    val job: String? = null
)
