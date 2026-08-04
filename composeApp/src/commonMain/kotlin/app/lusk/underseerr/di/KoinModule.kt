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
    // Resolve platform config before anything reads AppConfig.isDebug.
    app.lusk.underseerr.util.initAppConfig(context)

    // Preferences
    single<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>> { createDataStore(context) }
    single { PreferencesManager(get()) }
    
    // Network
    // SessionRefresher resolves the HttpClient lazily to break the construction
    // cycle (HttpClient -> SessionRefresher -> HttpClient).
    single { app.lusk.underseerr.data.auth.SessionExpiryNotifier() }
    // One sign-out routine for both the explicit logout and the involuntary one.
    // Depends on the database (resolved lazily below) but never on the HttpClient,
    // so it adds no construction cycle.
    single {
        app.lusk.underseerr.data.auth.SessionCleaner(
            get(), get(), get(),
            app.lusk.underseerr.data.auth.RoomCacheCleaner(get<UnderseerrDatabase>())
        )
    }
    single {
        app.lusk.underseerr.data.auth.SessionRefresher(get(), get(), get(), get(), get()) {
            get<io.ktor.client.HttpClient>()
        }
    }
    single<io.ktor.client.HttpClient> { HttpClientFactory(get(), get(), get()).create() }
    single { AuthKtorService(get()) }
    single { DiscoveryKtorService(get()) }
    single { UserKtorService(get()) }
    single { PlexKtorService(get()) }
    single { JellyseerrKtorService(get()) }
    single<RequestKtorService> { RequestServiceImpl(get()) }
    single<IssueService> { IssueKtorService(get()) }
    single { SettingsKtorService(get()) }
    single { NotificationServerService(get()) }
    single { SubscriptionKtorService(get(), app.lusk.underseerr.util.defaultWorkerEndpoint) }
    
    // Database
    single { 
        app.lusk.underseerr.data.local.getDatabaseBuilder(context)
            .fallbackToDestructiveMigration(true)
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
    single { get<UnderseerrDatabase>().issueDao() }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<DiscoveryRepository> { DiscoveryRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<WatchlistRepository> { WatchlistRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<RequestRepository> { RequestRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get(), get(), get()) }
    single<CacheRepository> { CacheRepositoryImpl(get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<IssueRepository> { IssueRepositoryImpl(get(), get(), get()) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get(), get(), get(), get(), get()) }
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
