package app.lusk.client.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that adds authentication headers to API requests.
 * Feature: overseerr-android-client
 * Validates: Requirements 1.4, 8.2
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    
    @Volatile
    private var apiKey: String? = null
    
    @Volatile
    private var currentUrl: String? = null
    
    /**
     * Set the API key to be used for authentication.
     * 
     * @param key The API key or session token
     */
    fun setApiKey(key: String?) {
        this.apiKey = key
    }
    
    /**
     * Set the current server URL to be used for requests.
     * 
     * @param url The server base URL
     */
    fun setServerUrl(url: String?) {
        this.currentUrl = url
    }
    
    /**
     * Clear the stored API key.
     */
    fun clearApiKey() {
        this.apiKey = null
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        // Handle dynamic base URL
        currentUrl?.let { baseUrl ->
            val newUrl = request.url.newBuilder()
                .scheme(if (baseUrl.startsWith("https")) "https" else "http")
                .host(baseUrl.substringAfter("://").substringBefore("/").substringBefore(":"))
                .apply {
                    val portIndex = baseUrl.substringAfter("://").substringBefore("/").indexOf(":")
                    if (portIndex != -1) {
                        port(baseUrl.substringAfter("://").substringBefore("/").substringAfter(":").toInt())
                    }
                }
                .build()
            request = request.newBuilder().url(newUrl).build()
        }
        
        // If no API key is set or it's a cookie session, proceed without X-Api-Key but with Accept header
        val apiKey = this.apiKey
        if (apiKey.isNullOrEmpty() || apiKey == "SESSION_COOKIE" || apiKey == "no_api_key" || apiKey.contains("@")) {
            android.util.Log.d("AuthInterceptor", "Using session cookies (API Key placeholder: $apiKey).")
            return chain.proceed(
                request.newBuilder()
                    .header("Accept", "application/json")
                    .build()
            )
        }
        
        android.util.Log.d("AuthInterceptor", "Adding X-Api-Key to request: ${request.url}")
        
        // Add API key to request headers
        val authenticatedRequest = request.newBuilder()
            .header("X-Api-Key", apiKey)
            .header("Accept", "application/json")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
