package app.lusk.client.di

import app.lusk.client.data.remote.api.AuthApiService
import app.lusk.client.data.remote.api.DiscoveryApiService
import app.lusk.client.data.remote.api.PlexApiService
import app.lusk.client.data.remote.api.RequestApiService
import app.lusk.client.data.remote.api.UserApiService
import app.lusk.client.data.remote.interceptor.AuthInterceptor
import app.lusk.client.data.remote.interceptor.RetryInterceptor
import app.lusk.client.data.security.CertificatePinningManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import app.lusk.client.data.remote.PersistentCookieJar
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 * Feature: overseerr-android-client
 * Validates: Requirements 8.2, 10.3, 10.4
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Qualifier for base URL.
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class BaseUrl

    /**
     * Qualifier for Plex-specific OkHttpClient.
     */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class PlexClient
    
    /**
     * Provide base URL for API requests.
     * This will be updated dynamically when user configures server.
     */
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String {
        // Default placeholder - will be updated at runtime
        return "https://web.lusk.app"
    }
    
    /**
     * Provide JSON serializer configuration.
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            prettyPrint = false
            coerceInputValues = true
        }
    }
    
    /**
     * Provide HTTP logging interceptor for debugging.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (app.lusk.client.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Provide configured OkHttp client with interceptors and timeouts.
     * Feature: overseerr-android-client, Property 37: API Timeout Configuration
     * Validates: Requirements 8.2, 10.3, 10.4
     */
    @Provides
    @Singleton
    fun provideCookieJar(@ApplicationContext context: Context): PersistentCookieJar {
        return PersistentCookieJar(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        certificatePinningManager: CertificatePinningManager,
        cookieJar: PersistentCookieJar,
        @BaseUrl baseUrl: String
    ): OkHttpClient {
        return certificatePinningManager.createSecureClient(baseUrl)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(loggingInterceptor)
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    
    /**
     * Provide Retrofit instance.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @BaseUrl baseUrl: String
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    /**
     * Provide AuthApiService.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }
    
    /**
     * Provide DiscoveryApiService.
     */
    @Provides
    @Singleton
    fun provideDiscoveryApiService(retrofit: Retrofit): DiscoveryApiService {
        return retrofit.create(DiscoveryApiService::class.java)
    }
    
    /**
     * Provide RequestApiService.
     */
    @Provides
    @Singleton
    fun provideRequestApiService(retrofit: Retrofit): RequestApiService {
        return retrofit.create(RequestApiService::class.java)
    }
    
    /**
     * Provide UserApiService.
     */
    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    @PlexClient
    fun providePlexOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        cookieJar: PersistentCookieJar
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Provide PlexApiService.
     */
    @Provides
    @Singleton
    fun providePlexApiService(
        json: Json,
        @PlexClient plexOkHttpClient: OkHttpClient
    ): PlexApiService {
        val contentType = "application/json".toMediaType()
        
        val plexRetrofit = Retrofit.Builder()
            .baseUrl("https://plex.tv/")
            .client(plexOkHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return plexRetrofit.create(PlexApiService::class.java)
    }
}
