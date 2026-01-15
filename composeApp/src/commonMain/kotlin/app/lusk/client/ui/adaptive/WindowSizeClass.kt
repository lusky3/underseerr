package app.lusk.client.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size class based on Material Design 3 guidelines.
 */
enum class WindowSizeClass {
    COMPACT,    // Phone in portrait, width < 600dp
    MEDIUM,     // Tablet in portrait, foldable, 600dp <= width < 840dp
    EXPANDED    // Tablet in landscape, desktop, width >= 840dp
}

/**
 * Window height class for vertical responsiveness.
 */
enum class WindowHeightClass {
    COMPACT,    // height < 480dp
    MEDIUM,     // 480dp <= height < 900dp
    EXPANDED    // height >= 900dp
}

/**
 * Device posture for foldable devices.
 */
enum class DevicePosture {
    NORMAL,
    HALF_OPENED,
    FLAT,
    TENT
}

/**
 * Adaptive layout configuration based on window size.
 */
data class AdaptiveLayoutConfig(
    val windowSizeClass: WindowSizeClass,
    val windowHeightClass: WindowHeightClass,
    val columns: Int,
    val contentPadding: Dp,
    val itemSpacing: Dp,
    val useNavigationRail: Boolean,
    val showNavigationLabels: Boolean
)

/**
 * Calculate window size class based on width.
 */
fun calculateWindowSizeClass(width: Dp): WindowSizeClass {
    return when {
        width < 600.dp -> WindowSizeClass.COMPACT
        width < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * Calculate window height class based on height.
 */
fun calculateWindowHeightClass(height: Dp): WindowHeightClass {
    return when {
        height < 480.dp -> WindowHeightClass.COMPACT
        height < 900.dp -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }
}

/**
 * Calculate adaptive layout configuration.
 */
fun calculateAdaptiveLayoutConfig(width: Dp, height: Dp): AdaptiveLayoutConfig {
    val windowSizeClass = calculateWindowSizeClass(width)
    val windowHeightClass = calculateWindowHeightClass(height)
    
    return when (windowSizeClass) {
        WindowSizeClass.COMPACT -> AdaptiveLayoutConfig(
            windowSizeClass = windowSizeClass,
            windowHeightClass = windowHeightClass,
            columns = 2,
            contentPadding = 16.dp,
            itemSpacing = 8.dp,
            useNavigationRail = false,
            showNavigationLabels = true
        )
        WindowSizeClass.MEDIUM -> AdaptiveLayoutConfig(
            windowSizeClass = windowSizeClass,
            windowHeightClass = windowHeightClass,
            columns = 3,
            contentPadding = 24.dp,
            itemSpacing = 12.dp,
            useNavigationRail = true,
            showNavigationLabels = true
        )
        WindowSizeClass.EXPANDED -> AdaptiveLayoutConfig(
            windowSizeClass = windowSizeClass,
            windowHeightClass = windowHeightClass,
            columns = 4,
            contentPadding = 32.dp,
            itemSpacing = 16.dp,
            useNavigationRail = true,
            showNavigationLabels = false
        )
    }
}

/**
 * Extension function to check if device is in tablet mode.
 */
fun WindowSizeClass.isTablet(): Boolean {
    return this == WindowSizeClass.MEDIUM || this == WindowSizeClass.EXPANDED
}

/**
 * Extension function to check if device is in phone mode.
 */
fun WindowSizeClass.isPhone(): Boolean {
    return this == WindowSizeClass.COMPACT
}

/**
 * Extension function to get recommended grid columns.
 */
fun WindowSizeClass.getGridColumns(): Int {
    return when (this) {
        WindowSizeClass.COMPACT -> 2
        WindowSizeClass.MEDIUM -> 3
        WindowSizeClass.EXPANDED -> 4
    }
}
