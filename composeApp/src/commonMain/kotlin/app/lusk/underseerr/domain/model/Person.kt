package app.lusk.underseerr.domain.model

/**
 * Domain model representing a person.
 */
data class Person(
    val id: Int,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val profilePath: String?,
    val knownForDepartment: String?,
    val credits: List<PersonCredit>
)

data class PersonCredit(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val character: String?,
    val job: String?
)
