package app.lusk.underseerr.presentation.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import app.lusk.underseerr.navigation.UnderseerrNavHost
import app.lusk.underseerr.navigation.Screen
import app.lusk.underseerr.presentation.discovery.DiscoveryViewModel
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.underseerr.ui.adaptive.*

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
    
    // Shared ViewModels for main tabs to prevent recreation/refreshing
    val discoveryViewModel: DiscoveryViewModel = koinViewModel()
    val requestViewModel: RequestViewModel = koinViewModel()
    val issueViewModel: IssueViewModel = koinViewModel()
    val profileViewModel: ProfileViewModel = koinViewModel()
    
    // Simplified navigation visibility - just check if it's a main tab
    val showNavigation = currentDestination?.let { dest ->
        dest.hasRoute(Screen.Home::class) ||
        dest.hasRoute(Screen.Requests::class) ||
        dest.hasRoute(Screen.Issues::class) ||
        dest.hasRoute(Screen.Profile::class)
    } ?: false
    
    // Get current screen for highlighting nav item
    val currentScreen = when {
        currentDestination?.hasRoute(Screen.Home::class) == true -> Screen.Home
        currentDestination?.hasRoute(Screen.Requests::class) == true -> Screen.Requests()
        currentDestination?.hasRoute(Screen.Issues::class) == true -> Screen.Issues
        currentDestination?.hasRoute(Screen.Profile::class) == true -> Screen.Profile
        else -> Screen.Home
    }
    
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutConfig = calculateAdaptiveLayoutConfig(maxWidth, maxHeight)
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNavigation && !layoutConfig.useNavigationRail) {
                    AdaptiveNavigation(
                        currentScreen = currentScreen,
                        layoutConfig = layoutConfig,
                        destinations = defaultNavigationDestinations,
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
                        destinations = defaultNavigationDestinations,
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
                UnderseerrNavHost(
                    navController = navController,
                    discoveryViewModel = discoveryViewModel,
                    requestViewModel = requestViewModel,
                    issueViewModel = issueViewModel,
                    profileViewModel = profileViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun getNavigationDestinations(): List<NavigationDestination> {
    return defaultNavigationDestinations
}
