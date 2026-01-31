package app.lusk.underseerr.di

import app.lusk.underseerr.data.local.UnderseerrDatabase
import app.lusk.underseerr.data.preferences.PreferencesManager
import app.lusk.underseerr.data.preferences.createDataStore
import app.lusk.underseerr.data.remote.HttpClientFactory
import app.lusk.underseerr.data.remote.api.*
import app.lusk.underseerr.data.repository.*
import app.lusk.underseerr.domain.repository.*
import app.lusk.underseerr.util.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.KoinAppDeclaration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun sharedModule(context: PlatformContext) = module {
    // Preferences
    single<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>> { createDataStore(context) }
    single { PreferencesManager(get()) }
    
    // Network
    single<io.ktor.client.HttpClient> { HttpClientFactory(get(), get()).create() }
    single { AuthKtorService(get()) }
    single { DiscoveryKtorService(get()) }
    single { UserKtorService(get()) }
    single { PlexKtorService(get()) }
    single<RequestKtorService> { RequestServiceImpl(get()) }
    single<IssueService> { IssueKtorService(get()) }
    single { SettingsKtorService(get()) }
    single { NotificationServerService(get()) }
    
    // Database
    single { 
        app.lusk.underseerr.data.local.getDatabaseBuilder(context)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build() 
    }
    single { get<UnderseerrDatabase>().movieDao() }
    single { get<UnderseerrDatabase>().tvShowDao() }
    single { get<UnderseerrDatabase>().mediaRequestDao() }
    single { get<UnderseerrDatabase>().notificationDao() }
    single { get<UnderseerrDatabase>().offlineRequestDao() }
    single { get<UnderseerrDatabase>().userDao() }
    single { get<UnderseerrDatabase>().discoveryDao() }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get(), get(), get(), get(), get()) }
    single<RequestRepository> { RequestRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get(), get(), get()) }
    single<CacheRepository> { CacheRepositoryImpl(get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<IssueRepository> { IssueRepositoryImpl(get(), get()) }
}


fun initKoin(context: PlatformContext, appDeclaration: KoinAppDeclaration = {}) = 
    org.koin.core.context.startKoin {
        appDeclaration()
        modules(sharedModule(context), platformModule(), presentationModule)
    }

/**
 * Expected platform-specific module.
 */
expect fun platformModule(): Module
