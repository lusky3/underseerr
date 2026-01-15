package app.lusk.client.di

import org.koin.dsl.module
import org.koin.androidx.workmanager.dsl.workerOf

import app.lusk.client.data.network.NetworkManager
import app.lusk.client.data.notification.NotificationHandler
import app.lusk.client.data.worker.WorkerScheduler
import app.lusk.client.error.GlobalErrorHandler
import app.lusk.client.util.MemoryOptimizer
import app.lusk.client.data.worker.RequestStatusWorker
import app.lusk.client.data.sync.SyncManager

val androidAppModule = module {
    // Note: ViewModels and some platform implementations are now in composeApp/commonMain and platformModule()
    
    single { GlobalErrorHandler(get()) }
    single { NotificationHandler(get()) }
    single { WorkerScheduler(get()) }
    single { NetworkManager(get()) }
    single { MemoryOptimizer(get()) }
    single { app.lusk.client.util.PerformanceMonitor() }
    single { SyncManager(get(), get()) }
    single { app.lusk.client.data.security.SecureLogger() }
    single { app.lusk.client.data.sync.OfflineQueue(get()) }
    single { app.lusk.client.data.security.CertificatePinningManager() }

    workerOf(::RequestStatusWorker)
}
