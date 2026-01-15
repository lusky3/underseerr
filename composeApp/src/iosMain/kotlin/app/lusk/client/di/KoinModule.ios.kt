package app.lusk.client.di

import app.lusk.client.data.security.MemorySecurityManager
import app.lusk.client.domain.security.SecurityManager
import app.lusk.client.domain.sync.SyncScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SecurityManager> { MemorySecurityManager() }
    single<SyncScheduler> { 
        object : SyncScheduler {
            override fun scheduleOfflineSync() {
                // TODO: Implement iOS background sync if possible, or just ignore for now
            }
        }
    }
    single<app.lusk.client.util.AppLogger> { app.lusk.client.util.ConsoleLogger() }
}
