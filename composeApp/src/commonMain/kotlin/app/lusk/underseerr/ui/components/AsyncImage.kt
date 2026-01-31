package app.lusk.underseerr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Async image component with progressive loading, placeholders, and error handling.
 * Feature: underseerr
 * Validates: Requirements 10.2
 * Property 36: Progressive Image Loading
 */

/**
 * Load and display an image asynchronously with Coil.
 * 
 * @param imageUrl URL of the image to load
 * @param contentDescription Accessibility description
 * @param modifier Modifier for the image
 * @param contentScale How to scale the image
 * @param showPlaceholder Whether to show placeholder while loading
 * @param showError Whether to show error icon on failure
 */
@Composable
fun AsyncImage(
    imageUrl: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showPlaceholder: Boolean = true,
    showError: Boolean = true
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            if (showPlaceholder) {
                ImagePlaceholder()
            }
        },
        error = {
            if (showError) {
                ImageError()
            }
        }
    )
}

/**
 * Placeholder shown while image is loading.
 */
@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Error icon shown when image fails to load.
 */
@Composable
fun ImageError(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = "Failed to load image",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * Simple placeholder icon for images.
 */
@Composable
fun SimpleImagePlaceholder(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "Image placeholder",
            modifier = Modifier.size(48.dp),
            tint = iconColor
        )
    }
}

/**
 * Poster image component for movies and TV shows.
 */
@Composable
fun PosterImage(
    posterPath: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    val imageUrl = posterPath?.let { 
        when {
            it.startsWith("http") -> it
            it.startsWith("/") -> "https://image.tmdb.org/t/p/w200$it"
            else -> "https://image.tmdb.org/t/p/w200/$it"
        }
    }
    
    AsyncImage(
        imageUrl = imageUrl,
        contentDescription = "Poster for $title",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * Backdrop image component for media details.
 */
@Composable
fun BackdropImage(
    backdropPath: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    val imageUrl = backdropPath?.let { 
        if (it.startsWith("http")) it else "https://image.tmdb.org/t/p/w1280$it" 
    }
    
    AsyncImage(
        imageUrl = imageUrl,
        contentDescription = "Backdrop for $title",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

/**
 * Avatar image component for user profiles.
 */
@Composable
fun AvatarImage(
    avatarUrl: String?,
    userName: String,
    modifier: Modifier = Modifier
) {
    val imageUrl = avatarUrl?.let {
        if (it.startsWith("http")) it else it // Avatar URLs are usually full or handled by Coil
    }
    
    AsyncImage(
        imageUrl = imageUrl ?: avatarUrl,
        contentDescription = "Avatar for $userName",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
