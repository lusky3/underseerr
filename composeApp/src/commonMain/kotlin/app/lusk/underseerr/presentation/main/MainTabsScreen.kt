package app.lusk.underseerr.presentation.main

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.lusk.underseerr.presentation.discovery.DiscoveryViewModel
import app.lusk.underseerr.presentation.discovery.HomeScreen
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.issue.IssuesListScreen
import app.lusk.underseerr.presentation.profile.ProfileScreen
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import app.lusk.underseerr.presentation.request.RequestsListScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainTabsScreen(
    onNavigateToMediaDetails: (String, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategory: (app.lusk.underseerr.presentation.discovery.CategoryType, Int, String) -> Unit,
    onNavigateToRequestDetails: (Int) -> Unit,
    onNavigateToIssueDetails: (Int) -> Unit,
    onNavigateToSettings: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToRequestsFilter: (String?) -> Unit,
    onLogout: () -> Unit,
    mainViewModel: MainViewModel = koinViewModel()
) {
    val selectedTab by mainViewModel.selectedTab.collectAsState()
    val pagerState = rememberPagerState(initialPage = selectedTab) { 4 }

    // Sync Pager -> ViewModel only when settled to avoid jumpy animations
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != selectedTab) {
            mainViewModel.setSelectedTab(pagerState.settledPage)
        }
    }

    // Sync ViewModel -> Pager via explicit commands
    LaunchedEffect(Unit) {
        mainViewModel.navCommand.collect { index ->
            if (pagerState.currentPage != index || pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    // Handle Drag Events from Nav Bar
    LaunchedEffect(Unit) {
        mainViewModel.tabDragEvent.collect { delta ->
            // Use a coefficient to scale drag to scroll
            pagerState.scrollBy(-delta * 1.5f)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.tabDragEnd.collect {
            // Snap to nearest page when drag ends
            val nearestPage = pagerState.currentPage
            pagerState.animateScrollToPage(nearestPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 3 // Keep all main tabs alive for instant switching
    ) { page ->
        when (page) {
            0 -> {
                val viewModel: DiscoveryViewModel = koinViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onMovieClick = { onNavigateToMediaDetails("movie", it) },
                    onTvShowClick = { onNavigateToMediaDetails("tv", it) },
                    onSearchClick = onNavigateToSearch,
                    onCategoryClick = onNavigateToCategory
                )
            }
            1 -> {
                val viewModel: RequestViewModel = koinViewModel()
                RequestsListScreen(
                    viewModel = viewModel,
                    initialFilter = null, // Filter handled via state if needed, or we can pass it in
                    onRequestClick = onNavigateToRequestDetails
                )
            }
            2 -> {
                val viewModel: IssueViewModel = koinViewModel()
                IssuesListScreen(
                    viewModel = viewModel,
                    onIssueClick = onNavigateToIssueDetails
                )
            }
            3 -> {
                val viewModel: ProfileViewModel = koinViewModel()
                ProfileScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAbout = onNavigateToAbout,
                    onNavigateToRequests = onNavigateToRequestsFilter,
                    onLogout = onLogout,
                    viewModel = viewModel
                )
            }
        }
    }
}
