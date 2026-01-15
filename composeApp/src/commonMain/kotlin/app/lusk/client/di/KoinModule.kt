package app.lusk.client.di

import app.lusk.client.data.local.OverseerrDatabase
import app.lusk.client.data.preferences.PreferencesManager
import app.lusk.client.data.preferences.createDataStore
import app.lusk.client.data.remote.HttpClientFactory
import app.lusk.client.data.remote.api.*
import app.lusk.client.data.repository.*
import app.lusk.client.domain.repository.*
import app.lusk.client.util.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.KoinAppDeclaration

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
    
    // Database
    single { app.lusk.client.data.local.getDatabaseBuilder(context).build() }
    single { get<OverseerrDatabase>().movieDao() }
    single { get<OverseerrDatabase>().tvShowDao() }
    single { get<OverseerrDatabase>().mediaRequestDao() }
    single { get<OverseerrDatabase>().notificationDao() }
    single { get<OverseerrDatabase>().offlineRequestDao() }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get(), get(), get(), get()) }
    single<RequestRepository> { RequestRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<CacheRepository> { CacheRepositoryImpl(get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
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
