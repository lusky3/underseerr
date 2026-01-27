package app.lusk.underseerr.data.mapper

import app.lusk.underseerr.data.remote.model.ApiServerInfo
import app.lusk.underseerr.domain.model.ServerInfo

/**
 * Maps API server info model to domain server info model.
 */
fun ApiServerInfo.toDomain(): ServerInfo {
    return ServerInfo(
        version = version,
        initialized = initialized,
        applicationUrl = applicationUrl
    )
}
