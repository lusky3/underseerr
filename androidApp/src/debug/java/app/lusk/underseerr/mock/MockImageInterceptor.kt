package app.lusk.underseerr.mock

import android.content.Context
import coil3.intercept.Interceptor
import coil3.request.ImageResult

/**
 * Coil interceptor for mock/debug builds that intercepts TMDB image requests
 * and replaces them with locally stored mock images or placeholder images.
 * 
 * This enables promotional screenshots with fictional content that doesn't
 * rely on TMDB's actual image servers.
 */
class MockImageInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        // Pattern to identify mock poster paths (our fictional content uses IDs 1000+)
        private val MOCK_POSTER_PATTERN = Regex("""/poster/(movie|tv)_(\d+)\.jpg""")
        private val MOCK_BACKDROP_PATTERN = Regex("""/backdrop/(movie|tv)_(\d+)\.jpg""")
        
        // Mock image IDs start from 1000
        private const val MOCK_ID_THRESHOLD = 1000
    }
    
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        
        // Only intercept string URLs
        if (data !is String) {
            return chain.proceed()
        }
        
        val url = data
        
        // Check if this is a TMDB URL with a mock image path
        if (url.contains("image.tmdb.org")) {
            val mockInfo = extractMockInfo(url)
            if (mockInfo != null) {
                val (isPoster, isMovie, mediaId) = mockInfo
                
                // Use MockImageUrls to get the appropriate URL (local asset or placeholder)
                val resolvedUrl = if (isPoster) {
                    MockImageUrls.getPosterUrl(mediaId, isMovie)
                } else {
                    MockImageUrls.getBackdropUrl(mediaId, isMovie)
                }
                
                val newRequest = request.newBuilder()
                    .data(resolvedUrl)
                    .build()
                return chain.withRequest(newRequest).proceed()
            }
        }
        
        return chain.proceed()
    }
    
    /**
     * Extracts mock info from URL.
     * @return Triple of (isPoster, isMovie, mediaId) or null if not a mock URL
     */
    private fun extractMockInfo(url: String): Triple<Boolean, Boolean, Int>? {
        // Check for poster pattern
        val posterMatch = MOCK_POSTER_PATTERN.find(url)
        if (posterMatch != null) {
            val type = posterMatch.groupValues[1]
            val id = posterMatch.groupValues[2].toIntOrNull() ?: return null
            if (id >= MOCK_ID_THRESHOLD) {
                return Triple(true, type == "movie", id)
            }
        }
        
        // Check for backdrop pattern
        val backdropMatch = MOCK_BACKDROP_PATTERN.find(url)
        if (backdropMatch != null) {
            val type = backdropMatch.groupValues[1]
            val id = backdropMatch.groupValues[2].toIntOrNull() ?: return null
            if (id >= MOCK_ID_THRESHOLD) {
                return Triple(false, type == "movie", id)
            }
        }
        
        return null
    }
}
