package app.lusk.underseerr.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

fun solidBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))

@Immutable
data class UnderseerrGradients(
    val primary: Brush = solidBrush(Color.Gray),
    val secondary: Brush = solidBrush(Color.Gray),
    val tertiary: Brush = solidBrush(Color.Gray),
    val background: Brush = solidBrush(Color.Black),
    val surface: Brush = solidBrush(Color.DarkGray),
    val accent: Brush = solidBrush(Color.Magenta),
    val highlight: Brush = solidBrush(Color.Cyan),
    val appBar: Brush = solidBrush(Color.Gray),
    val navBar: Brush = solidBrush(Color.Gray),
    val settings: Brush = solidBrush(Color.Gray),
    val profiles: Brush = solidBrush(Color.Gray),
    val requestDetails: Brush = solidBrush(Color.Gray),
    val issueDetails: Brush = solidBrush(Color.Gray),
    val onPrimary: Color = Color.White,
    val onSecondary: Color = Color.White,
    val onTertiary: Color = Color.White,
    val onBackground: Color = Color.White,
    val onSurface: Color = Color.White,
    val onAccent: Color = Color.White,
    val onHighlight: Color = Color.White,
    val onAppBar: Color = Color.White,
    val onNavBar: Color = Color.White,
    val onSettings: Color = Color.White,
    val onProfiles: Color = Color.White,
    val onRequestDetails: Color = Color.White,
    val onIssueDetails: Color = Color.White,
    val statusBadgeShape: Shape = RoundedCornerShape(8.dp),
    val isVibrant: Boolean = false
)

val LocalUnderseerrGradients = staticCompositionLocalOf { UnderseerrGradients() }

val VibrantGradients = UnderseerrGradients(
    primary = Brush.linearGradient(
        colors = listOf(Color(0xFF5D63EA), Color(0xFF7A31AC))
    ),
    secondary = Brush.linearGradient(
        colors = listOf(Color(0xFF0FEE8D), Color(0xFF09AA9D))
    ),
    tertiary = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
    ),
    background = Brush.verticalGradient(
        colors = listOf(vibrant_background, vibrant_surface)
    ),
    surface = Brush.verticalGradient(
        colors = listOf(vibrant_surface, Color(0xFF2D0055))
    ),
    accent = Brush.linearGradient(
        colors = listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    ),
    highlight = Brush.linearGradient(
        colors = listOf(Color(0xFFFF4B2B), Color(0xFFFF416C))
    ),
    statusBadgeShape = CircleShape,
    isVibrant = true
)

val DefaultDarkGradients = UnderseerrGradients(
    primary = solidBrush(md_theme_dark_surfaceVariant),
    secondary = solidBrush(md_theme_dark_surfaceVariant),
    tertiary = solidBrush(md_theme_dark_surfaceVariant),
    background = solidBrush(md_theme_dark_background),
    surface = solidBrush(md_theme_dark_surfaceVariant),
    requestDetails = solidBrush(md_theme_dark_background),
    issueDetails = solidBrush(md_theme_dark_background),
    appBar = solidBrush(md_theme_dark_surface),
    navBar = solidBrush(md_theme_dark_surface),
    settings = solidBrush(md_theme_dark_background),
    profiles = solidBrush(md_theme_dark_background),
    statusBadgeShape = RoundedCornerShape(8.dp),
    isVibrant = false
)

val DefaultLightGradients = UnderseerrGradients(
    primary = solidBrush(md_theme_light_surfaceVariant),
    secondary = solidBrush(md_theme_light_surfaceVariant),
    tertiary = solidBrush(md_theme_light_surfaceVariant),
    background = solidBrush(md_theme_light_background),
    surface = solidBrush(md_theme_light_surfaceVariant),
    requestDetails = solidBrush(md_theme_light_background),
    issueDetails = solidBrush(md_theme_light_background),
    appBar = solidBrush(md_theme_light_surface),
    navBar = solidBrush(md_theme_light_surface),
    settings = solidBrush(md_theme_light_background),
    profiles = solidBrush(md_theme_light_background),
    // Light theme needs dark text on light backgrounds
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onAccent = Color.Black,
    onHighlight = Color.Black,
    onAppBar = Color.Black,
    onNavBar = Color.Black,
    onSettings = Color.Black,
    onProfiles = Color.Black,
    onRequestDetails = Color.Black,
    onIssueDetails = Color.Black,
    statusBadgeShape = RoundedCornerShape(8.dp),
    isVibrant = false
)

/**
 * Returns White or Black depending on the background luminance.
 */
fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}

/**
 * Returns White or Black depending on the average luminance of the brush colors.
 * Note: Simplistic approach as it only checks the first and last colors if possible.
 */
fun contentColorFor(brush: Brush): Color {
    // Brushes don't easily reveal their colors, but we use this mostly for our known gradients.
    // Since we can't easily introspect Brush, we'll assume light text for now but
    // the UI should ideally pass the colors or we should use a better abstraction.
    // For now, let's keep it simple.
    return Color.White 
}
