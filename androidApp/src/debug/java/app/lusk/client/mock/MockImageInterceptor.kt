package app.lusk.client.mock

import android.content.Context
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.decode.DataSource
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.Uri
import coil3.toUri

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
        
        // TMDB base URL patterns
        private const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w500"
        private const val TMDB_W200_BASE = "https://image.tmdb.org/t/p/w200"
        private const val TMDB_BACKDROP_BASE = "https://image.tmdb.org/t/p/w1280"
        
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
        
        val url = data.toString()
        
        // Check if this is a TMDB URL with a mock image path
        if (url.contains("image.tmdb.org")) {
            val path = extractMockPath(url)
            if (path != null) {
                val localAssetPath = getLocalAssetPath(path)
                if (assetExists(localAssetPath)) {
                    // Redirect to local asset
                    val newRequest = request.newBuilder()
                        .data("file:///android_asset/$localAssetPath")
                        .build()
                    return chain.withRequest(newRequest).proceed()
                } else {
                    // Use a colorful placeholder based on the media ID
                    val placeholderUrl = generatePlaceholderUrl(path)
                    val newRequest = request.newBuilder()
                        .data(placeholderUrl)
                        .build()
                    return chain.withRequest(newRequest).proceed()
                }
            }
        }
        
        return chain.proceed()
    }
    
    private fun extractMockPath(url: String): String? {
        // Extract the path portion after the TMDB base URL
        val posterMatch = MOCK_POSTER_PATTERN.find(url)
        if (posterMatch != null) {
            val id = posterMatch.groupValues[2].toIntOrNull() ?: return null
            if (id >= MOCK_ID_THRESHOLD) {
                return posterMatch.value
            }
        }
        
        val backdropMatch = MOCK_BACKDROP_PATTERN.find(url)
        if (backdropMatch != null) {
            val id = backdropMatch.groupValues[2].toIntOrNull() ?: return null
            if (id >= MOCK_ID_THRESHOLD) {
                return backdropMatch.value
            }
        }
        
        return null
    }
    
    private fun getLocalAssetPath(mockPath: String): String {
        return when {
            mockPath.contains("/poster/") -> "mock_images/posters${mockPath.substringAfter("/poster")}"
            mockPath.contains("/backdrop/") -> "mock_images/backdrops${mockPath.substringAfter("/backdrop")}"
            else -> "mock_images$mockPath"
        }
    }
    
    private fun assetExists(assetPath: String): Boolean {
        return try {
            context.assets.open(assetPath).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generatePlaceholderUrl(path: String): String {
        // Generate a colorful placeholder using placeholder.com or similar service
        // Extract info from path to generate unique colors
        val posterMatch = MOCK_POSTER_PATTERN.find(path)
        val backdropMatch = MOCK_BACKDROP_PATTERN.find(path)
        
        return when {
            posterMatch != null -> {
                val type = posterMatch.groupValues[1]
                val id = posterMatch.groupValues[2].toIntOrNull() ?: 0
                generatePosterPlaceholder(type, id)
            }
            backdropMatch != null -> {
                val type = backdropMatch.groupValues[1]
                val id = backdropMatch.groupValues[2].toIntOrNull() ?: 0
                generateBackdropPlaceholder(type, id)
            }
            else -> "https://placehold.co/300x450/1a1a2e/eaeaea?text=Poster"
        }
    }
    
    private fun generatePosterPlaceholder(type: String, id: Int): String {
        // Generate unique colors based on ID for visual variety
        val colors = listOf(
            "1a1a2e" to "eaeaea", // Dark blue
            "16213e" to "e94560", // Navy with pink accent
            "0f3460" to "e94560", // Deep blue
            "1a1a2e" to "4ecca3", // Dark with teal
            "2d132c" to "ee4540", // Maroon with red
            "222831" to "00adb5", // Charcoal with cyan
            "393e46" to "00adb5", // Gray with cyan
            "1b262c" to "bbe1fa", // Navy with light blue
            "2c3e50" to "e74c3c", // Blue-gray with red
            "1c1c1c" to "f39c12", // Black with gold
            "2c2c54" to "ffcd56", // Purple with yellow
            "1a1a40" to "ff6b6b"  // Dark blue with coral
        )
        
        val colorPair = colors[id % colors.size]
        val label = if (type == "movie") "M" else "TV"
        
        // Using placehold.co for reliable placeholder images
        return "https://placehold.co/300x450/${colorPair.first}/${colorPair.second}?text=$label"
    }
    
    private fun generateBackdropPlaceholder(type: String, id: Int): String {
        val colors = listOf(
            "1a1a2e" to "eaeaea",
            "16213e" to "e94560",
            "0f3460" to "e94560",
            "1a1a2e" to "4ecca3",
            "2d132c" to "ee4540",
            "222831" to "00adb5"
        )
        
        val colorPair = colors[id % colors.size]
        val label = if (type == "movie") "Movie" else "TV+Show"
        
        // Backdrop aspect ratio is typically 16:9
        return "https://placehold.co/1280x720/${colorPair.first}/${colorPair.second}?text=$label"
    }
}
