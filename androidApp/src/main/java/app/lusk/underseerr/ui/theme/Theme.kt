package app.lusk.underseerr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.domain.repository.ThemePreference

/**
 * Material You theme configuration for Underseerr.
 * Feature: underseerr
 * Validates: Requirements 9.1, 9.2, 9.5, 9.6
 */

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint
)

private val VibrantColorScheme = darkColorScheme(
    primary = vibrant_primary,
    onPrimary = vibrant_onPrimary,
    primaryContainer = vibrant_primaryContainer,
    onPrimaryContainer = vibrant_onPrimaryContainer,
    secondary = vibrant_secondary,
    onSecondary = vibrant_onSecondary,
    secondaryContainer = vibrant_secondaryContainer,
    onSecondaryContainer = vibrant_onSecondaryContainer,
    tertiary = vibrant_tertiary,
    onTertiary = vibrant_onTertiary,
    tertiaryContainer = vibrant_tertiaryContainer,
    onTertiaryContainer = vibrant_onTertiaryContainer,
    error = vibrant_error,
    onError = vibrant_onError,
    errorContainer = vibrant_errorContainer,
    onErrorContainer = vibrant_onErrorContainer,
    background = vibrant_background,
    onBackground = vibrant_onBackground,
    surface = vibrant_surface,
    onSurface = vibrant_onSurface,
    surfaceVariant = vibrant_surfaceVariant,
    onSurfaceVariant = vibrant_onSurfaceVariant,
    outline = vibrant_outline,
    outlineVariant = vibrant_outlineVariant,
    inverseSurface = vibrant_inverseSurface,
    inverseOnSurface = vibrant_inverseOnSurface,
    inversePrimary = vibrant_inversePrimary,
    surfaceTint = vibrant_surfaceTint
)

/**
 * Main theme composable with Material You dynamic theming support.
 * 
 * @param themeMode Theme mode (light, dark, or system)
 * @param dynamicColor Enable dynamic color theming (Android 12+)
 * @param content Content to be themed
 */
@Composable
fun UnderseerrTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    vibrantColors: app.lusk.underseerr.domain.repository.VibrantThemeColors = app.lusk.underseerr.domain.repository.VibrantThemeColors(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK, ThemePreference.VIBRANT -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        themePreference == ThemePreference.VIBRANT -> VibrantColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val gradients = when {
        themePreference == ThemePreference.VIBRANT -> {
            app.lusk.underseerr.ui.theme.UnderseerrGradients(
                primary = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.primaryStart), parseColor(vibrantColors.primaryEnd))
                ),
                secondary = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.secondaryStart), parseColor(vibrantColors.secondaryEnd))
                ),
                tertiary = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.tertiaryStart), parseColor(vibrantColors.tertiaryEnd))
                ),
                background = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.backgroundStart), parseColor(vibrantColors.backgroundEnd))
                ),
                surface = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.surfaceStart), parseColor(vibrantColors.surfaceEnd))
                ),
                accent = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.accentStart), parseColor(vibrantColors.accentEnd))
                ),
                highlight = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.highlightStart), parseColor(vibrantColors.highlightEnd))
                ),
                appBar = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(parseColor(vibrantColors.appBarStart), parseColor(vibrantColors.appBarEnd))
                ),
                navBar = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.navBarStart), parseColor(vibrantColors.navBarEnd))
                ),
                settings = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.settingsStart), parseColor(vibrantColors.settingsEnd))
                ),
                profiles = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.profilesStart), parseColor(vibrantColors.profilesEnd))
                ),
                requestDetails = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.requestDetailsStart), parseColor(vibrantColors.requestDetailsEnd))
                ),
                issueDetails = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(parseColor(vibrantColors.issueDetailsStart), parseColor(vibrantColors.issueDetailsEnd))
                ),
                onPrimary = contentColorFor(parseColor(vibrantColors.primaryStart)),
                onSecondary = contentColorFor(parseColor(vibrantColors.secondaryStart)),
                onTertiary = contentColorFor(parseColor(vibrantColors.tertiaryStart)),
                onBackground = contentColorFor(parseColor(vibrantColors.backgroundStart)),
                onSurface = contentColorFor(parseColor(vibrantColors.surfaceStart)),
                onAccent = contentColorFor(parseColor(vibrantColors.accentStart)),
                onHighlight = contentColorFor(parseColor(vibrantColors.highlightStart)),
                onAppBar = contentColorFor(parseColor(vibrantColors.appBarStart)),
                onNavBar = contentColorFor(parseColor(vibrantColors.navBarStart)),
                onSettings = contentColorFor(parseColor(vibrantColors.settingsStart)),
                onProfiles = contentColorFor(parseColor(vibrantColors.profilesStart)),
                onRequestDetails = contentColorFor(parseColor(vibrantColors.requestDetailsStart)),
                onIssueDetails = contentColorFor(parseColor(vibrantColors.issueDetailsStart)),
                statusBadgeShape = if (vibrantColors.usePillShape) 
                    androidx.compose.foundation.shape.CircleShape 
                else 
                    androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                isVibrant = true
            )
        }
        darkTheme -> DefaultDarkGradients.copy(
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onAccent = Color.White,
            onHighlight = Color.White,
            onAppBar = Color.White,
            onNavBar = Color.White,
            onSettings = Color.White,
            onProfiles = Color.White,
            onRequestDetails = Color.White,
            onIssueDetails = Color.White
        )
        else -> DefaultLightGradients.copy(
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onAccent = Color.White,
            onHighlight = Color.White,
            onAppBar = Color.White,
            onNavBar = Color.Black,
            onSettings = Color.Black,
            onProfiles = Color.Black,
            onRequestDetails = Color.Black,
            onIssueDetails = Color.Black
        )
    }

    CompositionLocalProvider(
        LocalUnderseerrGradients provides gradients
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length == 8) {
            val a = cleanHex.substring(0, 2).toInt(16)
            val r = cleanHex.substring(2, 4).toInt(16)
            val g = cleanHex.substring(4, 6).toInt(16)
            val b = cleanHex.substring(6, 8).toInt(16)
            Color(r, g, b, a)
        } else {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(r, g, b, 255)
        }
    } catch (e: Exception) {
        Color.White
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun contentColorFor(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}
