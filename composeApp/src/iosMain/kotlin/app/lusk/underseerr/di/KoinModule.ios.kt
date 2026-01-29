package app.lusk.underseerr.di

import app.lusk.underseerr.data.security.MemorySecurityManager
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.domain.security.WebPushKeyManager
import app.lusk.underseerr.domain.sync.SyncScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SecurityManager> { app.lusk.underseerr.domain.security.IosSecurityManager() }
    single<WebPushKeyManager> { 
        object : WebPushKeyManager {
            override suspend fun getOrCreateWebPushKeys(): Pair<String, String> = "" to ""
        }
    }
    single<SyncScheduler> { 
        object : SyncScheduler {
            override fun scheduleOfflineSync() {
                // TODO: Implement iOS background sync if possible, or just ignore for now
            }
        }
    }
    single<app.lusk.underseerr.util.AppLogger> { app.lusk.underseerr.util.ConsoleLogger() }
    single<app.lusk.underseerr.domain.security.BiometricManager> { app.lusk.underseerr.domain.security.IosBiometricManager() }
    single<app.lusk.underseerr.domain.permission.PermissionManager> { app.lusk.underseerr.domain.permission.IosPermissionManager() }
}
