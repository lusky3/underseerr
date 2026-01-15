package app.lusk.client.mock

import com.squareup.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Helper class for setting up tests with MockOverseerrServer.
 * Provides utilities for creating Retrofit instances configured to use the mock server.
 */
object MockServerTestHelper {
    
    /**
     * Create a Retrofit instance configured to use the mock server.
     */
    fun createRetrofit(baseUrl: String): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    /**
     * Create an API service instance for testing.
     */
    inline fun <reified T> createApiService(mockServer: MockOverseerrServer): T {
        return createRetrofit(mockServer.baseUrl).create(T::class.java)
    }
}

/**
 * Base class for tests that use MockOverseerrServer.
 * Automatically starts and stops the server for each test.
 */
abstract class MockServerTest {
    protected lateinit var mockServer: MockOverseerrServer
    
    /**
     * Start the mock server before each test.
     */
    fun setupMockServer() {
        mockServer = MockOverseerrServer()
        mockServer.start()
    }
    
    /**
     * Stop the mock server after each test.
     */
    fun tearDownMockServer() {
        if (::mockServer.isInitialized) {
            mockServer.shutdown()
        }
    }
    
    /**
     * Create an API service for testing.
     */
    inline fun <reified T> createApiService(): T {
        return MockServerTestHelper.createApiService(mockServer)
    }
}
