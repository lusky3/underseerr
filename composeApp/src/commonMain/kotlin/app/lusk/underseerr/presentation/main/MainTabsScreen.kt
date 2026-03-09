package app.lusk.underseerr.presentation.main

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.presentation.discovery.DiscoveryViewModel
import app.lusk.underseerr.presentation.discovery.HomeScreen
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.issue.IssuesListScreen
import app.lusk.underseerr.presentation.profile.ProfileScreen
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import app.lusk.underseerr.presentation.request.RequestsListScreen
import app.lusk.underseerr.presentation.settings.SettingsViewModel
import app.lusk.underseerr.presentation.settings.TrialPromptType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
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
    mainViewModel: MainViewModel
) {
    val selectedTab by mainViewModel.selectedTab.collectAsState()
    val pagerState = rememberPagerState(initialPage = selectedTab) { 4 }

    // Trial prompt — shown as overlay on main screen
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val trialPrompt by settingsViewModel.showTrialPrompt.collectAsState()

    // Sync Pager -> ViewModel only when settled to avoid jumpy animations
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
             if (page != selectedTab) {
                 mainViewModel.setSelectedTab(page)
             }
        }
    }

    // Sync ViewModel -> Pager via explicit commands
    LaunchedEffect(Unit) {
        mainViewModel.navCommand.collectLatest { index ->
            try {
                if (pagerState.currentPage != index) {
                    pagerState.animateScrollToPage(index)
                }
            } catch (e: CancellationException) {
                // Scroll cancelled by new navigation event, safe to ignore
            }
        }
    }

    // Handle Drag Events from Nav Bar
    LaunchedEffect(Unit) {
        mainViewModel.tabDragEvent.collect { delta ->
            pagerState.scrollBy(-delta * 1.5f)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.tabDragEnd.collect {
            val nearestPage = pagerState.currentPage
            pagerState.animateScrollToPage(nearestPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 3
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
                val filter by mainViewModel.requestsFilter.collectAsState()
                RequestsListScreen(
                    viewModel = viewModel,
                    initialFilter = filter,
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

    // Trial / Subscription Prompt — displayed as a dialog over the main content
    trialPrompt?.let { promptType ->
        when (promptType) {
            is TrialPromptType.Activate -> {
                AlertDialog(
                    onDismissRequest = { settingsViewModel.dismissTrialPrompt() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("Enable Push Notifications") },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Get instant push notifications when your requests are approved or become available.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    settingsViewModel.purchasePremiumWithTrial()
                                    settingsViewModel.dismissTrialPrompt()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Free Trial via Google Play")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { settingsViewModel.startTrial() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Try Without a Card (30 Days)")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { settingsViewModel.dismissTrialPrompt() }) {
                            Text("Not Now")
                        }
                    }
                )
            }
            is TrialPromptType.Reset -> {
                AlertDialog(
                    onDismissRequest = { settingsViewModel.dismissTrialPrompt() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = { Text("Trial Expired") },
                    text = {
                        Text(
                            "Your 30-day notification trial has ended. Reset it for another 30 days, or subscribe for unlimited access.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(onClick = { settingsViewModel.startTrial() }) {
                            Text("Reset Trial")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { settingsViewModel.dismissTrialPrompt() }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}
