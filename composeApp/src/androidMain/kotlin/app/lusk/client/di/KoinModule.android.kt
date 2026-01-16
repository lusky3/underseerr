package app.lusk.client.di

import app.lusk.client.data.security.BiometricAuthenticator
import app.lusk.client.data.security.SecurityManagerImpl
import app.lusk.client.domain.security.SecurityManager
import app.lusk.client.domain.sync.AndroidSyncScheduler
import app.lusk.client.domain.sync.SyncScheduler
import app.lusk.client.util.AndroidLogger
import app.lusk.client.util.AppLogger
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SecurityManager> { SecurityManagerImpl(get()) }
    single<SyncScheduler> { AndroidSyncScheduler(get()) }
    single<AppLogger> { AndroidLogger() }
    single { app.lusk.client.data.security.BiometricAuthenticator(get()) }
    single<app.lusk.client.domain.security.BiometricManager> { get<app.lusk.client.data.security.BiometricAuthenticator>() }
    single<app.lusk.client.domain.permission.PermissionManager> { 
        app.lusk.client.domain.permission.AndroidPermissionManager(
            context = get(),
            activityProvider = { app.lusk.client.util.CurrentActivityHolder.get() }
        )
    }
}
