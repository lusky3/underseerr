package app.lusk.underseerr.mock

/**
 * Mock image URL generator for debug builds.
 * 
 * This provides local asset URLs for fictional content used in
 * promotional screenshots. Each mock media item is mapped to
 * its corresponding generated poster/backdrop image.
 * 
 * Images are stored in:
 * - androidApp/src/debug/assets/mock_images/posters/
 * - androidApp/src/debug/assets/mock_images/backdrops/
 */
object MockImageUrls {
    
    // Map of media IDs to their asset file names
    // Movies: 1001-1012, TV Shows: 2001-2010
    private val assetMappings = mapOf(
        // Movies
        1001 to "poster_neon_horizons",
        1002 to "poster_last_cartographer",
        1003 to "poster_velvet_club",
        1004 to "poster_echoes_tomorrow",
        1005 to "poster_quantum_thief",
        1006 to "poster_garden_stars",
        1007 to "poster_crimson_protocol",
        1008 to "poster_whispers_aurora",
        1009 to "poster_memory_merchant",
        1010 to "poster_titanfall_legacy",
        1011 to "poster_copper_skies",
        1012 to "poster_infinite_library",
        // TV Shows (available ones)
        2001 to "poster_chronicles_void",
        2002 to "poster_alchemist_daughter",
        2003 to "poster_harbor_city",
        2004 to "poster_terraform_diaries", // Using terraform for dreamweavers placeholder
        2005 to "poster_eighth_realm"       // Using eighth_realm for stellar academy placeholder
    )
    
    // Fallback color palettes for placeholder generation (for items without generated images)
    private val movieColors = listOf(
        "1a1a2e" to "e94560", // Dark blue with pink
        "16213e" to "f0a500", // Navy with gold
        "0f3460" to "00fff5", // Deep blue with cyan
        "2d132c" to "ee4540", // Maroon with red
        "1c1c1c" to "f39c12", // Black with gold
        "2c2c54" to "706fd3", // Purple
        "1b262c" to "bbe1fa", // Navy with light blue
        "2c3e50" to "e74c3c", // Blue-gray with red
        "341f97" to "ff6b6b", // Purple with coral
        "222831" to "00adb5", // Charcoal with cyan
        "1a1a40" to "ff6b6b", // Dark blue with coral
        "0a3d62" to "f8c291"  // Ocean with peach
    )
    
    private val tvColors = listOf(
        "1a1a2e" to "4ecca3", // Dark with teal
        "393e46" to "00adb5", // Gray with cyan
        "222831" to "ffd460", // Dark with yellow
        "2d3436" to "74b9ff", // Dark with blue
        "1e272e" to "ff6348", // Dark with orange
        "2c3a47" to "1abc9c", // Gray with turquoise
        "1a1a2e" to "ff69b4", // Dark with pink
        "2f3640" to "e1b12c", // Gray with gold
        "192a56" to "f5f6fa", // Blue with white
        "273c75" to "00d8d8"  // Navy with aqua
    )
    
    /**
     * Generate a poster URL for mock content.
     * Returns local asset URL if available, otherwise falls back to placeholder.
     * 
     * Poster aspect ratio is 2:3 (500x750)
     */
    fun getPosterUrl(mediaId: Int, isMovie: Boolean): String {
        val assetName = assetMappings[mediaId]
        return if (assetName != null) {
            "file:///android_asset/mock_images/posters/${assetName}.jpg"
        } else {
            // Fallback to placeholder
            val colors = if (isMovie) movieColors else tvColors
            val colorPair = colors[mediaId % colors.size]
            val typeLabel = if (isMovie) "M" else "TV"
            "https://placehold.co/500x750/${colorPair.first}/${colorPair.second}?text=$typeLabel&font=roboto"
        }
    }
    
    /**
     * Generate a backdrop URL for mock content.
     * Returns local asset URL if available, otherwise falls back to placeholder.
     * 
     * Backdrop aspect ratio is 16:9 (1280x720)
     */
    fun getBackdropUrl(mediaId: Int, isMovie: Boolean): String {
        val assetName = assetMappings[mediaId]
        return if (assetName != null) {
            "file:///android_asset/mock_images/backdrops/${assetName}.jpg"
        } else {
            // Fallback to placeholder
            val colors = if (isMovie) movieColors else tvColors
            val colorPair = colors[mediaId % colors.size]
            val typeLabel = if (isMovie) "Movie" else "TV+Show"
            "https://placehold.co/1280x720/${colorPair.first}/${colorPair.second}?text=$typeLabel&font=roboto"
        }
    }
    
    /**
     * Generate an avatar URL for mock users.
     */
    fun getAvatarUrl(userId: Int): String {
        val colors = listOf(
            "3498db" to "fff", // Blue
            "e74c3c" to "fff", // Red
            "2ecc71" to "fff", // Green
            "9b59b6" to "fff", // Purple
            "f39c12" to "fff", // Orange
            "1abc9c" to "fff"  // Teal
        )
        val colorPair = colors[userId % colors.size]
        return "https://placehold.co/100x100/${colorPair.first}/${colorPair.second}?text=U$userId&font=roboto"
    }
    
    /**
     * Check if a media ID is a mock ID (1000+).
     */
    fun isMockId(mediaId: Int): Boolean = mediaId >= 1000
    
    /**
     * Check if a media ID has a local asset available.
     */
    fun hasLocalAsset(mediaId: Int): Boolean = assetMappings.containsKey(mediaId)
    
    /**
     * Convert a TMDB poster path to a mock placeholder if it's a mock ID.
     * This can be used in presentation layer to replace TMDB URLs.
     */
    fun resolveUrlFromPath(path: String?, isMovie: Boolean): String? {
        if (path == null) return null
        
        // Extract ID from path like "/poster/movie_1001.jpg"
        val regex = Regex("""/(poster|backdrop)/(movie|tv)_(\d+)\.jpg""")
        val match = regex.find(path)
        
        return if (match != null) {
            val type = match.groupValues[1]
            val mediaId = match.groupValues[3].toIntOrNull() ?: return null
            
            if (isMockId(mediaId)) {
                when (type) {
                    "poster" -> getPosterUrl(mediaId, isMovie)
                    "backdrop" -> getBackdropUrl(mediaId, isMovie)
                    else -> null
                }
            } else {
                // It's a real TMDB path, return as-is
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Get a list of all available mock poster asset paths.
     * Useful for preloading or debugging.
     */
    fun getAllPosterAssets(): List<String> {
        return assetMappings.values.map { "mock_images/posters/${it}.jpg" }
    }
    
    /**
     * Get a list of all available mock backdrop asset paths.
     * Useful for preloading or debugging.
     */
    fun getAllBackdropAssets(): List<String> {
        return assetMappings.values.map { "mock_images/backdrops/${it}.jpg" }
    }
}
