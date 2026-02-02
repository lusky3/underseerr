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
    single<app.lusk.underseerr.domain.repository.FirestoreService> { app.lusk.underseerr.data.repository.IosFirestoreService() }
    single<app.lusk.underseerr.domain.billing.BillingManager> {
        object : app.lusk.underseerr.domain.billing.BillingManager {
            override fun startConnection() {}
            override suspend fun purchaseProduct(productId: String): Result<Unit> = Result.failure(Exception("iOS Billing not implemented"))
            override suspend fun isSubscribed(productId: String): Boolean = false
            override val isSubscribed: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
            override val purchaseDetails: kotlinx.coroutines.flow.SharedFlow<app.lusk.underseerr.domain.billing.PurchaseDetails> = kotlinx.coroutines.flow.MutableSharedFlow()
        }
    }
}
