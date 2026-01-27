package app.lusk.client.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.client.domain.model.MediaType
import app.lusk.client.presentation.auth.AuthViewModel
import app.lusk.client.presentation.auth.PlexAuthScreen
import app.lusk.client.presentation.discovery.DiscoveryViewModel
import app.lusk.client.presentation.discovery.MediaDetailsScreen
import app.lusk.client.presentation.discovery.CategoryResultsScreen
import app.lusk.client.presentation.discovery.HomeScreen
import app.lusk.client.presentation.discovery.SearchScreen
import app.lusk.client.presentation.issue.IssuesListScreen
import app.lusk.client.presentation.issue.IssueDetailsScreen
import app.lusk.client.presentation.profile.ProfileScreen
import app.lusk.client.presentation.request.RequestDetailsScreen
import app.lusk.client.presentation.request.RequestsListScreen
import app.lusk.client.presentation.request.RequestViewModel
import app.lusk.client.presentation.auth.ServerConfigScreen
import app.lusk.client.presentation.auth.SplashScreen
import app.lusk.client.presentation.settings.SettingsScreen
import app.lusk.client.presentation.settings.ServerManagementScreen

@Composable
fun UnderseerrNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Splash
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                onServerValidated = {
                    // Logic for what to do after validation (e.g. go to auth)
                    // If we need to go to PlexAuth:
                    navController.navigate(Screen.PlexAuth)
                },
                onAuthenticated = {
                   // Logic for already authenticated
                   navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                   }
                }
            )
        }
        
        composable<Screen.PlexAuth> {
            val viewModel: AuthViewModel = koinViewModel()
            PlexAuthScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                    }
                },
                onAuthError = { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar(error)
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
                    viewModel.handleAuthCallback(token)
                }
            }
            
            PlexAuthScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                    }
                },
                onAuthError = { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar(error)
                    }
                }
            )
        }
        
        composable<Screen.Home> {
            val viewModel: DiscoveryViewModel = koinViewModel()
            HomeScreen(
                viewModel = viewModel,
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
                    navController.navigate(Screen.CategoryResults(type.name, id, name))
                }
            )
        }
        
        composable<Screen.CategoryResults> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.CategoryResults>()
            val viewModel: DiscoveryViewModel = koinViewModel()

            // type is MediaType (enum), we convert it to safe lowercase string for display/logic if needed, 
            // but Screen.MediaDetails expects string type ("movie" or "tv")
            CategoryResultsScreen(
                categoryType = args.categoryType,
                categoryId = args.categoryId,
                categoryName = args.categoryName,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { type, id -> 
                    // type is MediaType enum
                    navController.navigate(Screen.MediaDetails(type.name.lowercase(), id))
                }
            )
        }

        composable<Screen.Search> {
            val viewModel: DiscoveryViewModel = koinViewModel()
            SearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { type, id ->
                    // type is MediaType enum
                    navController.navigate(Screen.MediaDetails(type.name.lowercase(), id))
                }
            )
        }
        
        composable<Screen.MediaDetails>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.MediaDetails>()
            val mediaType = if (args.mediaType == "tv") MediaType.TV else MediaType.MOVIE
            val viewModel: DiscoveryViewModel = koinViewModel()
            
            MediaDetailsScreen(
                mediaId = args.mediaId,
                mediaType = mediaType,
                openRequest = args.openRequest,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<Screen.Requests> {
            val viewModel: RequestViewModel = koinViewModel()
            RequestsListScreen(
                viewModel = viewModel,
                onRequestClick = { requestId ->
                    navController.navigate(Screen.RequestDetails(requestId))
                }
            )
        }
        
        composable<Screen.RequestDetails>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.RequestDetails>()
            val viewModel: RequestViewModel = koinViewModel()
            
            RequestDetailsScreen(
                requestId = args.requestId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onModifyRequest = { mediaId ->
                     navController.navigate(Screen.MediaDetails("tv", mediaId, openRequest = true)) 
                }
            )
        }
        
        composable<Screen.Issues> {
            IssuesListScreen(
                onIssueClick = { issueId ->
                    navController.navigate(Screen.IssueDetails(issueId))
                }
            )
        }
        
        composable<Screen.IssueDetails>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.IssueDetails>()
            
            IssueDetailsScreen(
                issueId = args.issueId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Profile> {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About)
                },
                onNavigateToRequests = { filter ->
                    // Navigate to requests tab - the filter will be applied in the screen
                    navController.navigate(Screen.Requests)
                },
                onLogout = {
                     navController.navigate(Screen.ServerConfig()) {
                        popUpTo<Screen.Home> { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.Settings>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            SettingsScreen(
                onNavigateToServerManagement = {
                    navController.navigate(Screen.ServerManagement)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.ServerManagement>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            ServerManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.About>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            app.lusk.client.presentation.settings.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
