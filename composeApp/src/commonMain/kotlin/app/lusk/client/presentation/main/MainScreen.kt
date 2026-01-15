package app.lusk.client.presentation.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
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
 * Refactored for KMP in commonMain.
 */
@Composable
fun MainScreen(
    startDestination: String = Screen.Home.route,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determine if we should show navigation
    val showNavigation = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Requests.route,
        Screen.Issues.route,
        Screen.Profile.route
    )
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutConfig = calculateAdaptiveLayoutConfig(maxWidth, maxHeight)
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNavigation && !layoutConfig.useNavigationRail) {
                    AdaptiveNavigation(
                        currentRoute = currentDestination?.route ?: Screen.Home.route,
                        layoutConfig = layoutConfig,
                        destinations = getNavigationDestinations(),
                        onNavigate = { route ->
                            navController.navigate(route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
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
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // Navigation rail for larger screens
                if (showNavigation && layoutConfig.useNavigationRail) {
                    AdaptiveNavigation(
                        currentRoute = currentDestination?.route ?: Screen.Home.route,
                        layoutConfig = layoutConfig,
                        destinations = getNavigationDestinations(),
                        onNavigate = { route ->
                            navController.navigate(route) {
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
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Get navigation destinations for the app.
 */
private fun getNavigationDestinations(): List<NavigationDestination> {
    return defaultNavigationDestinations
}
