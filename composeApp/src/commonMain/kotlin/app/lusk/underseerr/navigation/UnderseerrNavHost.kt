package app.lusk.underseerr.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.underseerr.data.auth.SessionExpiryNotifier
import app.lusk.underseerr.data.auth.SessionExpiryReason
import kotlin.reflect.KClass
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.presentation.auth.AuthViewModel
import app.lusk.underseerr.presentation.auth.PlexAuthScreen
import app.lusk.underseerr.presentation.discovery.*
import app.lusk.underseerr.presentation.issue.*
import app.lusk.underseerr.presentation.profile.*
import app.lusk.underseerr.presentation.request.*
import app.lusk.underseerr.presentation.auth.*
import app.lusk.underseerr.presentation.settings.*
import app.lusk.underseerr.presentation.main.*

/**
 * Destinations that already *are* the sign-in flow. An involuntary sign-out that
 * arrives while the user is sitting on one of these must not re-navigate: doing so
 * would wipe the back stack out from under a sign-in attempt already in progress
 * (worst case, bouncing a user out of the Plex callback mid-exchange).
 *
 * Add any new pre-authentication destination here, or an expiry firing on it will
 * kick the user back to [Screen.PlexAuth].
 */
val SIGN_IN_DESTINATIONS: List<KClass<out Screen>> = listOf(
    Screen.PlexAuth::class,
    Screen.PlexAuthCallback::class,
    Screen.ServerConfig::class,
    Screen.Splash::class
)

/** True when [destination] is part of the sign-in flow (see [SIGN_IN_DESTINATIONS]). */
fun isSignInDestination(destination: NavDestination?): Boolean =
    destination != null && SIGN_IN_DESTINATIONS.any { destination.hasRoute(it) }

/**
 * The nav host's involuntary-sign-out guard, extracted so it can be tested without
 * a Compose runtime.
 *
 * A null [reason] means nothing expired. A null [currentDestination] means the graph
 * has not resolved a destination yet, which is treated as "not on sign-in" so a very
 * early expiry still routes rather than being swallowed.
 */
fun shouldRouteToSignIn(
    reason: SessionExpiryReason?,
    currentDestination: NavDestination?
): Boolean = reason != null && !isSignInDestination(currentDestination)

@Composable
fun UnderseerrNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Splash
) {
    // An expired or revoked sign-in clears credentials from under the UI. Without
    // this the user would be left on a screen whose every request now fails; route
    // them to sign-in instead (PlexAuthScreen explains why).
    val expiryNotifier: SessionExpiryNotifier = koinInject()
    LaunchedEffect(Unit) {
        expiryNotifier.reason.collect { reason ->
            if (!shouldRouteToSignIn(reason, navController.currentDestination)) return@collect
            navController.navigate(Screen.PlexAuth) {
                // popUpTo(0) — id 0 is the root graph entry, which is always on the
                // queue. The start destination (Splash) is NOT: it pops itself off on
                // its first navigation, and popBackStackInternal silently no-ops when
                // the target id is absent, leaving the signed-out screen reachable via
                // Back with every request 403ing and no second expiry emission to
                // rescue the user (reason is a conflated StateFlow).
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            val initialOrder = getOrder(initialState.destination)
            val targetOrder = getOrder(targetState.destination)
            if (initialOrder != -1 && targetOrder != -1 && initialOrder != targetOrder) {
                if (targetOrder > initialOrder) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn()
                }
            } else {
                fadeIn(animationSpec = tween(300))
            }
        },
        exitTransition = {
            val initialOrder = getOrder(initialState.destination)
            val targetOrder = getOrder(targetState.destination)
            if (initialOrder != -1 && targetOrder != -1 && initialOrder != targetOrder) {
                if (targetOrder > initialOrder) {
                    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
                } else {
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut()
                }
            } else {
                fadeOut(animationSpec = tween(300))
            }
        },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable<Screen.Splash> {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.MainTabs) {
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
                   expiryNotifier.consume()
                   // Logic for already authenticated
                   navController.navigate(Screen.MainTabs) {
                        popUpTo<Screen.ServerConfig> { inclusive = true }
                   }
                }
            )
        }
        
        composable<Screen.PlexAuth> {
            val viewModel: AuthViewModel = koinViewModel()
            PlexAuthScreen(
                // A forced sign-out clears the back stack, so this can be the only
                // destination — popping it would leave a blank NavHost.
                onBackClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                viewModel = viewModel,
                onAuthSuccess = {
                    expiryNotifier.consume()
                    // ServerConfig is frequently *not* on the stack here (an expiry
                    // routes straight to PlexAuth, and Splash -> MainTabs skips it
                    // entirely), and popUpTo no-ops on an absent id. Clearing from the
                    // root instead guarantees a single post-sign-in entry rather than a
                    // duplicate MainTabs plus a stale PlexAuth whose ViewModel still
                    // holds AuthState.Authenticated and re-fires this callback on Back.
                    navController.navigate(Screen.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                // PlexAuthScreen renders AuthState.Error inline (error Surface in its own
                // Scaffold body), so there is nothing for the host to display. Do not add a
                // snackbar here: it would duplicate the message the screen already shows.
                onAuthError = { }
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
                // A forced sign-out clears the back stack, so this can be the only
                // destination — popping it would leave a blank NavHost.
                onBackClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                viewModel = viewModel,
                onAuthSuccess = {
                    expiryNotifier.consume()
                    // See Screen.PlexAuth above for why this pops from the root.
                    navController.navigate(Screen.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                // See Screen.PlexAuth above: the error is already rendered by PlexAuthScreen.
                onAuthError = { }
            )
        }

        composable<Screen.MainTabs> {
            MainTabsScreen(
                onNavigateToMediaDetails = { type, id -> navController.navigate(Screen.MediaDetails(type, id)) },
                onNavigateToSearch = { navController.navigate(Screen.Search) },
                onNavigateToCategory = { type, id, name -> navController.navigate(Screen.CategoryResults(type.name, id, name)) },
                onNavigateToRequestDetails = { id -> navController.navigate(Screen.RequestDetails(id)) },
                onNavigateToIssueDetails = { id -> navController.navigate(Screen.IssueDetails(id)) },
                onNavigateToSettings = { showPaywall -> navController.navigate(Screen.Settings(showPaywall)) },
                onNavigateToAbout = { navController.navigate(Screen.About) },
                onNavigateToRequestsFilter = { filter ->
                    mainViewModel.setRequestsFilter(filter)
                    mainViewModel.navigateToTab(1)
                },
                onLogout = {
                    navController.navigate(Screen.ServerConfig()) {
                        popUpTo<Screen.MainTabs> { inclusive = true }
                    }
                },
                mainViewModel = mainViewModel
            )
        }

        // Redirect individual tab routes to MainTabs for deep links/compatibility
        composable<Screen.Home> {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.MainTabs) {
                    popUpTo<Screen.Home> { inclusive = true }
                }
                mainViewModel.navigateToTab(0)
            }
        }
        
        composable<Screen.Requests> {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.MainTabs) {
                    popUpTo<Screen.Requests> { inclusive = true }
                }
                mainViewModel.navigateToTab(1)
            }
        }
        
        composable<Screen.Issues> {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.MainTabs) {
                    popUpTo<Screen.Issues> { inclusive = true }
                }
                mainViewModel.navigateToTab(2)
            }
        }
        
        composable<Screen.Profile> {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.MainTabs) {
                    popUpTo<Screen.Profile> { inclusive = true }
                }
                mainViewModel.navigateToTab(3)
            }
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
                onBackClick = { navController.popBackStack() },
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetails(personId))
                },
                onMediaClick = { type, id ->
                    navController.navigate(Screen.MediaDetails(type.name.lowercase(), id))
                },
                onGenreClick = { genreId, genreName ->
                    navController.navigate(Screen.CategoryResults("genre", genreId, genreName))
                }
            )
        }

        composable<Screen.PersonDetails>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PersonDetails>()
            val viewModel: DiscoveryViewModel = koinViewModel()
            
            PersonDetailsScreen(
                personId = args.personId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMediaClick = { type, id -> 
                    navController.navigate(Screen.MediaDetails(type.name.lowercase(), id))
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
                },
                onViewDetails = { mediaId, mediaType ->
                    navController.navigate(Screen.MediaDetails(
                        mediaType = if (mediaType == MediaType.MOVIE) "movie" else "tv",
                        mediaId = mediaId
                    ))
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
        
        composable<Screen.Settings>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) { backStackEntry ->
            val settings = backStackEntry.toRoute<Screen.Settings>()
            SettingsScreen(
                showPremiumPaywallOnStart = settings.showPremiumPaywall,
                onNavigateToServerManagement = {
                    navController.navigate(Screen.ServerManagement)
                },
                onNavigateToVibrantCustomization = {
                    navController.navigate(Screen.VibrantCustomization)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.VibrantCustomization>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            VibrantCustomizationScreen(
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
            app.lusk.underseerr.presentation.settings.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun getOrder(destination: androidx.navigation.NavDestination): Int {
    return when {
        destination.hasRoute(Screen.MainTabs::class) -> 0
        destination.hasRoute(Screen.Home::class) -> 0
        destination.hasRoute(Screen.Requests::class) -> 1
        destination.hasRoute(Screen.Issues::class) -> 2
        destination.hasRoute(Screen.Profile::class) -> 3
        else -> -1
    }
}
