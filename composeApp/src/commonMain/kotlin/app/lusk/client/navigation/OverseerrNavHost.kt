package app.lusk.client.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.client.domain.model.MediaType
import app.lusk.client.presentation.auth.AuthViewModel
import app.lusk.client.presentation.auth.PlexAuthScreen
import app.lusk.client.presentation.discovery.DiscoveryViewModel
import app.lusk.client.presentation.discovery.MediaDetailsScreen
import app.lusk.client.presentation.discovery.CategoryResultsScreen
import app.lusk.client.presentation.home.HomeScreen
import app.lusk.client.presentation.issue.IssuesScreen
import app.lusk.client.presentation.issue.IssueDetailsScreen
import app.lusk.client.presentation.profile.ProfileScreen
import app.lusk.client.presentation.request.RequestDetailsScreen
import app.lusk.client.presentation.request.RequestsScreen
import app.lusk.client.presentation.request.RequestViewModel
import app.lusk.client.presentation.search.SearchScreen
import app.lusk.client.presentation.server.ServerConfigScreen
import app.lusk.client.presentation.splash.SplashScreen
import app.lusk.client.presentation.settings.SettingsScreen
import app.lusk.client.presentation.settings.ServerManagementScreen

@Composable
fun OverseerrNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Splash
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable<Screen.Splash> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.Splash> { inclusive = true }
                    }
                },
                onNavigateToServerConfig = {
                    navController.navigate(Screen.ServerConfig()) {
                        popUpTo<Screen.Splash> { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.ServerConfig> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.ServerConfig>()
            
            ServerConfigScreen(
                prefillServerUrl = args.serverUrl,
                onConfigSaved = {
                    navController.navigate(Screen.PlexAuth)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.PlexAuth> {
            val viewModel: AuthViewModel = koinViewModel()
            PlexAuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.PlexAuthCallback> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PlexAuthCallback>()
            val token = args.token
            val viewModel: AuthViewModel = koinViewModel()
            
            // Auto-trigger token exchange when arriving from deep link
            LaunchedEffect(token) {
                if (token.isNotEmpty()) {
                    viewModel.handleCallback(token)
                }
            }
            
            PlexAuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.Home> {
            HomeScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MediaDetails("movie", movieId))
                },
                onTvShowClick = { tvShowId ->
                    navController.navigate(Screen.MediaDetails("tv", tvShowId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search)
                },
                onCategoryClick = { type, id, name ->
                    navController.navigate(Screen.CategoryResults(type, id, name))
                }
            )
        }
        
        composable<Screen.CategoryResults> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.CategoryResults>()
            val viewModel: DiscoveryViewModel = koinViewModel()

            CategoryResultsScreen(
                categoryType = args.categoryType,
                categoryId = args.categoryId,
                categoryName = args.categoryName,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { type, id -> 
                    val mediaTypeEnum = if (type.lowercase() == "tv") MediaType.TV else MediaType.MOVIE
                    navController.navigate(Screen.MediaDetails(mediaTypeEnum.name.lowercase(), id))
                }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onMediaClick = { type, id ->
                    val mediaTypeEnum = if (type.lowercase() == "tv") MediaType.TV else MediaType.MOVIE
                    navController.navigate(Screen.MediaDetails(mediaTypeEnum.name.lowercase(), id))
                }
            )
        }
        
        composable<Screen.MediaDetails> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.MediaDetails>()
            val mediaType = if (args.mediaType == "tv") MediaType.TV else MediaType.MOVIE
            val viewModel: DiscoveryViewModel = koinViewModel()
            
            MediaDetailsScreen(
                mediaId = args.mediaId,
                mediaType = mediaType,
                openRequest = args.openRequest,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onRequestClick = {
                    // Navigate to request not supported directly here, maybe dialog?
                }
            )
        }

        composable<Screen.Requests> {
            RequestsScreen(
                onRequestClick = { requestId ->
                    navController.navigate(Screen.RequestDetails(requestId))
                }
            )
        }
        
        composable<Screen.RequestDetails> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.RequestDetails>()
            val viewModel: RequestViewModel = koinViewModel()
            
            RequestDetailsScreen(
                requestId = args.requestId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { type, id ->
                    val mediaType = if (type == MediaType.TV) "tv" else "movie"
                    navController.navigate(Screen.MediaDetails(mediaType, id, openRequest = true)) 
                }
            )
        }
        
        composable<Screen.Issues> {
            IssuesScreen(
                onIssueClick = { issueId ->
                    navController.navigate(Screen.IssueDetails(issueId))
                }
            )
        }
        
        composable<Screen.IssueDetails> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.IssueDetails>()
            
            IssueDetailsScreen(
                issueId = args.issueId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Profile> {
            ProfileScreen(
                onSettingsClick = {
                    navController.navigate(Screen.Settings)
                },
                onServerConfigClick = {
                    navController.navigate(Screen.ServerConfig()) {
                        // Clear stack
                    }
                }
            )
        }
        
        composable<Screen.Settings> {
            SettingsScreen(
                onServerManagementClick = {
                    navController.navigate(Screen.ServerManagement)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable<Screen.ServerManagement> {
            ServerManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
