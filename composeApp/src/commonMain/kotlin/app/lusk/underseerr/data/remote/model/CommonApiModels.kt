package app.lusk.underseerr.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMediaServer(
    val id: Int,
    val name: String,
    val isDefault: Boolean = false
)

@Serializable
data class ApiServiceSettings(
    val profiles: List<ApiQualityProfile>,
    val rootFolders: List<ApiRootFolder>
)

@Serializable
data class ApiRequestStatus(
    @SerialName("status")
    val status: String
)

@Serializable
data class ApiQualityProfile(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String
)

@Serializable
data class ApiRootFolder(
    @SerialName("id")
    val id: Int,
    @SerialName("path")
    val path: String
)

@Serializable
data class RequestsResponse(
    @SerialName("pageInfo")
    val pageInfo: PageInfo = PageInfo(),
    @SerialName("results")
    val results: List<ApiMediaRequest> = emptyList()
)

@Serializable
data class PageInfo(
    @SerialName("pages")
    val pages: Int = 1,
    @SerialName("pageSize")
    val pageSize: Int = 20,
    @SerialName("results")
    val results: Int = 0,
    @SerialName("page")
    val page: Int = 1
)

@Serializable
data class ApiGenre(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String
)
