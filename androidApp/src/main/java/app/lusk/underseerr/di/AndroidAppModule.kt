package app.lusk.underseerr.di

import org.koin.dsl.module
import org.koin.androidx.workmanager.dsl.workerOf

import app.lusk.underseerr.data.network.NetworkManager
import app.lusk.underseerr.data.notification.NotificationHandler
import app.lusk.underseerr.data.worker.WorkerScheduler
import app.lusk.underseerr.error.GlobalErrorHandler
import app.lusk.underseerr.util.MemoryOptimizer
import app.lusk.underseerr.data.worker.RequestStatusWorker
import app.lusk.underseerr.data.sync.SyncManager

val androidAppModule = module {
    // Note: ViewModels and some platform implementations are now in composeApp/commonMain and platformModule()
    
    single { GlobalErrorHandler(get()) }
    single { NotificationHandler(get()) }
    single { WorkerScheduler(get()) }
    single { NetworkManager(get()) }
    single { MemoryOptimizer(get()) }
    single { app.lusk.underseerr.util.PerformanceMonitor() }
    single { SyncManager(get(), get()) }
    single { app.lusk.underseerr.data.security.SecureLogger() }
    single { app.lusk.underseerr.data.sync.OfflineQueue(get()) }
    single { app.lusk.underseerr.data.security.CertificatePinningManager() }

    workerOf(::RequestStatusWorker)
}
