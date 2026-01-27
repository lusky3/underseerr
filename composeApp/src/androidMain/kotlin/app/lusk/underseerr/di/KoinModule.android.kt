package app.lusk.underseerr.di

import app.lusk.underseerr.data.security.BiometricAuthenticator
import app.lusk.underseerr.data.security.SecurityManagerImpl
import app.lusk.underseerr.domain.security.SecurityManager
import app.lusk.underseerr.domain.sync.AndroidSyncScheduler
import app.lusk.underseerr.domain.sync.SyncScheduler
import app.lusk.underseerr.util.AndroidLogger
import app.lusk.underseerr.util.AppLogger
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SecurityManager> { SecurityManagerImpl(get()) }
    single<SyncScheduler> { AndroidSyncScheduler(get()) }
    single<AppLogger> { AndroidLogger() }
    single { app.lusk.underseerr.data.security.BiometricAuthenticator(get()) }
    single<app.lusk.underseerr.domain.security.BiometricManager> { get<app.lusk.underseerr.data.security.BiometricAuthenticator>() }
    single<app.lusk.underseerr.domain.permission.PermissionManager> { 
        app.lusk.underseerr.domain.permission.AndroidPermissionManager(
            context = get(),
            activityProvider = { app.lusk.underseerr.util.CurrentActivityHolder.get() }
        )
    }
}
