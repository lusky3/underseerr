package app.lusk.client.presentation.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lusk.client.navigation.OverseerrNavHost
import app.lusk.client.navigation.Screen
import app.lusk.client.ui.adaptive.AdaptiveNavigation
import app.lusk.client.ui.adaptive.NavigationDestination
import app.lusk.client.ui.adaptive.calculateAdaptiveLayoutConfig
import app.lusk.client.ui.adaptive.defaultNavigationDestinations

/**
 * Main screen with bottom navigation and content.
 * Refactored for KMP in commonMain with Type Safe Navigation.
 */
@Composable
fun MainScreen(
    startDestination: Screen = Screen.Home,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show navigation using hierarchy and hasRoute
    val processingDestination = defaultNavigationDestinations.find { dest ->
         currentDestination?.hierarchy?.any { it.hasRoute(dest.screen::class) } == true
    }
    
    val showNavigation = processingDestination != null
    val currentScreen = processingDestination?.screen ?: Screen.Home
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutConfig = calculateAdaptiveLayoutConfig(maxWidth, maxHeight)
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNavigation && !layoutConfig.useNavigationRail) {
                    AdaptiveNavigation(
                        currentScreen = currentScreen,
                        layoutConfig = layoutConfig,
                        destinations = getNavigationDestinations(),
                        onNavigate = { screen ->
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                // Navigation rail for larger screens
                if (showNavigation && layoutConfig.useNavigationRail) {
                    AdaptiveNavigation(
                        currentScreen = currentScreen,
                        layoutConfig = layoutConfig,
                        destinations = getNavigationDestinations(),
                        onNavigate = { screen ->
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                
                // Main content
                OverseerrNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun getNavigationDestinations(): List<NavigationDestination> {
    return defaultNavigationDestinations
}
