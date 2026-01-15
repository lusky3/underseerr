package app.lusk.client.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.client.domain.model.MediaType
import app.lusk.client.presentation.auth.AuthViewModel
import app.lusk.client.presentation.auth.PlexAuthScreen
import app.lusk.client.presentation.auth.ServerConfigScreen
import app.lusk.client.presentation.auth.SplashScreen
import app.lusk.client.presentation.discovery.DiscoveryViewModel
import app.lusk.client.presentation.discovery.HomeScreen
import app.lusk.client.presentation.discovery.MediaDetailsScreen
import app.lusk.client.presentation.discovery.SearchScreen
import app.lusk.client.presentation.profile.ProfileScreen
import app.lusk.client.presentation.request.RequestDetailsScreen
import app.lusk.client.presentation.request.RequestsListScreen
import app.lusk.client.presentation.settings.ServerManagementScreen
import app.lusk.client.presentation.settings.SettingsScreen
import app.lusk.client.ui.animation.backwardTransition
import app.lusk.client.ui.animation.forwardTransition
import app.lusk.client.ui.animation.popEnterTransition
import app.lusk.client.ui.animation.popExitTransition

/**
 * Main navigation host for the app.
 * Refactored for KMP in commonMain.
 */
@Composable
fun OverseerrNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Initialization
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToServerConfig = {
                    navController.navigate(Screen.ServerConfig.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Authentication Flow
        composable(
            route = Screen.ServerConfig.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            ServerConfigScreen(
                onServerValidated = {
                    navController.navigate(Screen.PlexAuth.route)
                },
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ServerConfig.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.PlexAuth.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            PlexAuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ServerConfig.route) { inclusive = true }
                    }
                },
                onAuthError = { error ->
                    // Handle auth error - could navigate to error screen or show snackbar
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.PlexAuthCallback.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val viewModel: AuthViewModel = koinViewModel()
            
            // Auto-trigger token exchange when arriving from deep link
            LaunchedEffect(token) {
                if (token.isNotEmpty()) {
                    viewModel.handleAuthCallback(token)
                }
            }
            
            PlexAuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ServerConfig.route) { inclusive = true }
                    }
                },
                onAuthError = { error ->
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }
        
        composable(
            route = Screen.Home.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val viewModel: DiscoveryViewModel = koinViewModel()
            HomeScreen(
                viewModel = viewModel,
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MediaDetails.createRoute("movie", movieId))
                },
                onTvShowClick = { tvShowId ->
                    navController.navigate(Screen.MediaDetails.createRoute("tv", tvShowId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onCategoryClick = { type, id, name ->
                    navController.navigate(Screen.CategoryResults.createRoute(type.name, id, name))
                }
            )
        }

        composable(
            route = Screen.CategoryResults.route,
            arguments = listOf(
                navArgument("categoryType") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.IntType },
                navArgument("categoryName") { type = NavType.StringType }
            ),
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val categoryType = backStackEntry.arguments?.getString("categoryType") ?: ""
            val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val viewModel: DiscoveryViewModel = koinViewModel()

            app.lusk.client.presentation.discovery.CategoryResultsScreen(
                viewModel = viewModel,
                categoryType = categoryType,
                categoryId = categoryId,
                categoryName = categoryName,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { mediaType, mediaId ->
                    navController.navigate(Screen.MediaDetails.createRoute(mediaType.name.lowercase(), mediaId))
                }
            )
        }
        
        composable(
            route = Screen.Search.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val viewModel: DiscoveryViewModel = koinViewModel()
            SearchScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onMediaClick = { mediaType, mediaId ->
                    navController.navigate(Screen.MediaDetails.createRoute(mediaType.name.lowercase(), mediaId))
                }
            )
        }
        
        composable(
            route = Screen.MediaDetails.route,
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.IntType },
                navArgument("openRequest") { 
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "lusk://media/{mediaType}/{mediaId}" }
            ),
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val mediaTypeString = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
            val openRequest = backStackEntry.arguments?.getBoolean("openRequest") ?: false
            val mediaType = if (mediaTypeString == "tv") MediaType.TV else MediaType.MOVIE
            val viewModel: DiscoveryViewModel = koinViewModel()
            
            MediaDetailsScreen(
                viewModel = viewModel,
                mediaType = mediaType,
                mediaId = mediaId,
                openRequest = openRequest,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Requests Flow
        composable(
            route = Screen.Requests.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val viewModel: app.lusk.client.presentation.request.RequestViewModel = koinViewModel()
            RequestsListScreen(
                viewModel = viewModel,
                onRequestClick = { requestId ->
                    navController.navigate(Screen.RequestDetails.createRoute(requestId))
                }
            )
        }
        
        // Issues Flow
        composable(
            route = Screen.Issues.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            app.lusk.client.presentation.issue.IssuesListScreen(
                onIssueClick = { issueId ->
                    navController.navigate(Screen.IssueDetails.createRoute(issueId))
                }
            )
        }
        
        composable(
            route = Screen.IssueDetails.route,
            arguments = listOf(
                navArgument("issueId") { type = NavType.IntType }
            ),
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val issueId = backStackEntry.arguments?.getInt("issueId") ?: return@composable
            
            app.lusk.client.presentation.issue.IssueDetailsScreen(
                issueId = issueId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.RequestDetails.route,
            arguments = listOf(
                navArgument("requestId") { type = NavType.IntType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "lusk://request/{requestId}" }
            ),
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getInt("requestId") ?: 0
            val viewModel: app.lusk.client.presentation.request.RequestViewModel = koinViewModel()
            
            RequestDetailsScreen(
                requestId = requestId,
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onModifyRequest = { mediaId ->
                    // Navigate to TV Show details with request dialog open
                    navController.navigate(
                        Screen.MediaDetails.createRoute("tv", mediaId, openRequest = true)
                    )
                }
            )
        }
        
        // Profile Flow
        composable(
            route = Screen.Profile.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.ServerConfig.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Settings.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            SettingsScreen(
                onNavigateToServerManagement = {
                    navController.navigate(Screen.ServerManagement.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ServerManagement.route,
            enterTransition = { forwardTransition() },
            exitTransition = { backwardTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            ServerManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
