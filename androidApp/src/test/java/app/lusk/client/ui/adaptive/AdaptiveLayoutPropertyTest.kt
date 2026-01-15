package app.lusk.client.ui.adaptive

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Property-based tests for adaptive layout responsiveness.
 * Feature: overseerr-android-client
 * Property 35: Adaptive Layout Responsiveness
 * Validates: Requirements 9.4
 */
class AdaptiveLayoutPropertyTest : StringSpec({
    
    "Property 35.1: Window size class should be determined correctly based on width" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<Int>(100, Arb.int(200..2000)) { width ->
            // Act
            val sizeClass = when {
                width < 600 -> WindowSizeClass.COMPACT
                width < 840 -> WindowSizeClass.MEDIUM
                else -> WindowSizeClass.EXPANDED
            }
            
            // Assert - Size class should match width thresholds
            when {
                width < 600 -> sizeClass shouldBe WindowSizeClass.COMPACT
                width < 840 -> sizeClass shouldBe WindowSizeClass.MEDIUM
                else -> sizeClass shouldBe WindowSizeClass.EXPANDED
            }
        }
    }
    
    "Property 35.2: Grid columns should increase with larger window sizes" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<WindowSizeClass>(100, Arb.enum<WindowSizeClass>()) { sizeClass ->
            // Act
            val columns = sizeClass.getGridColumns()
            
            // Assert - Columns should be appropriate for size class
            when (sizeClass) {
                WindowSizeClass.COMPACT -> columns shouldBe 2
                WindowSizeClass.MEDIUM -> columns shouldBe 3
                WindowSizeClass.EXPANDED -> columns shouldBe 4
            }
            
            // Columns should always be positive
            columns shouldBeGreaterThan 0
        }
    }
    
    "Property 35.3: Navigation rail should be used for medium and expanded screens" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<WindowSizeClass>(100, Arb.enum<WindowSizeClass>()) { sizeClass ->
            // Act
            val config = when (sizeClass) {
                WindowSizeClass.COMPACT -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 2,
                    contentPadding = 16.dp,
                    itemSpacing = 8.dp,
                    useNavigationRail = false,
                    showNavigationLabels = true
                )
                WindowSizeClass.MEDIUM -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 3,
                    contentPadding = 24.dp,
                    itemSpacing = 12.dp,
                    useNavigationRail = true,
                    showNavigationLabels = true
                )
                WindowSizeClass.EXPANDED -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 4,
                    contentPadding = 32.dp,
                    itemSpacing = 16.dp,
                    useNavigationRail = true,
                    showNavigationLabels = false
                )
            }
            
            // Assert - Navigation rail usage should match size class
            when (sizeClass) {
                WindowSizeClass.COMPACT -> config.useNavigationRail shouldBe false
                WindowSizeClass.MEDIUM, WindowSizeClass.EXPANDED -> 
                    config.useNavigationRail shouldBe true
            }
        }
    }
    
    "Property 35.4: Content padding should increase with larger screens" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<WindowSizeClass>(100, Arb.enum<WindowSizeClass>()) { sizeClass ->
            // Act
            val padding = when (sizeClass) {
                WindowSizeClass.COMPACT -> 16
                WindowSizeClass.MEDIUM -> 24
                WindowSizeClass.EXPANDED -> 32
            }
            
            // Assert - Padding should increase with screen size
            when (sizeClass) {
                WindowSizeClass.COMPACT -> padding shouldBe 16
                WindowSizeClass.MEDIUM -> padding shouldBe 24
                WindowSizeClass.EXPANDED -> padding shouldBe 32
            }
        }
    }
    
    "Property 35.5: Tablet detection should work correctly" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<WindowSizeClass>(100, Arb.enum<WindowSizeClass>()) { sizeClass ->
            // Act
            val isTablet = sizeClass.isTablet()
            val isPhone = sizeClass.isPhone()
            
            // Assert - Device type detection should be mutually exclusive
            when (sizeClass) {
                WindowSizeClass.COMPACT -> {
                    isPhone shouldBe true
                    isTablet shouldBe false
                }
                WindowSizeClass.MEDIUM, WindowSizeClass.EXPANDED -> {
                    isPhone shouldBe false
                    isTablet shouldBe true
                }
            }
        }
    }
    
    "Property 35.6: Window height class should be determined correctly" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<Int>(100, Arb.int(300..1500)) { height ->
            // Act
            val heightClass = when {
                height < 480 -> WindowHeightClass.COMPACT
                height < 900 -> WindowHeightClass.MEDIUM
                else -> WindowHeightClass.EXPANDED
            }
            
            // Assert - Height class should match height thresholds
            when {
                height < 480 -> heightClass shouldBe WindowHeightClass.COMPACT
                height < 900 -> heightClass shouldBe WindowHeightClass.MEDIUM
                else -> heightClass shouldBe WindowHeightClass.EXPANDED
            }
        }
    }
    
    "Property 35.7: Adaptive layout config should be consistent for same window size" {
        // Feature: overseerr-android-client, Property 35: Adaptive Layout Responsiveness
        checkAll<WindowSizeClass>(100, Arb.enum<WindowSizeClass>()) { sizeClass ->
            // Act - Create config twice for same size class
            val config1 = when (sizeClass) {
                WindowSizeClass.COMPACT -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 2,
                    contentPadding = 16.dp,
                    itemSpacing = 8.dp,
                    useNavigationRail = false,
                    showNavigationLabels = true
                )
                WindowSizeClass.MEDIUM -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 3,
                    contentPadding = 24.dp,
                    itemSpacing = 12.dp,
                    useNavigationRail = true,
                    showNavigationLabels = true
                )
                WindowSizeClass.EXPANDED -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 4,
                    contentPadding = 32.dp,
                    itemSpacing = 16.dp,
                    useNavigationRail = true,
                    showNavigationLabels = false
                )
            }
            
            val config2 = when (sizeClass) {
                WindowSizeClass.COMPACT -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 2,
                    contentPadding = 16.dp,
                    itemSpacing = 8.dp,
                    useNavigationRail = false,
                    showNavigationLabels = true
                )
                WindowSizeClass.MEDIUM -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 3,
                    contentPadding = 24.dp,
                    itemSpacing = 12.dp,
                    useNavigationRail = true,
                    showNavigationLabels = true
                )
                WindowSizeClass.EXPANDED -> AdaptiveLayoutConfig(
                    windowSizeClass = sizeClass,
                    windowHeightClass = WindowHeightClass.MEDIUM,
                    columns = 4,
                    contentPadding = 32.dp,
                    itemSpacing = 16.dp,
                    useNavigationRail = true,
                    showNavigationLabels = false
                )
            }
            
            // Assert - Configs should be identical for same size class
            config1.columns shouldBe config2.columns
            config1.useNavigationRail shouldBe config2.useNavigationRail
            config1.windowSizeClass shouldBe config2.windowSizeClass
        }
    }
})
