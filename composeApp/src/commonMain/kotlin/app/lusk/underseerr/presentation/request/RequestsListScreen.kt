package app.lusk.underseerr.presentation.request

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.*
import app.lusk.underseerr.domain.model.MediaRequest
import app.lusk.underseerr.domain.model.RequestStatus
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.underseerr.presentation.auth.AuthViewModel
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.presentation.issue.ReportIssueDialog
import app.lusk.underseerr.domain.model.MediaType
import org.koin.compose.koinInject
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Bookmark
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    viewModel: RequestViewModel,
    discoveryViewModel: app.lusk.underseerr.presentation.discovery.DiscoveryViewModel = koinViewModel(),
    onRequestClick: (Int) -> Unit,
    initialFilter: String? = null,
    modifier: Modifier = Modifier
) {
    val userRequests by viewModel.userRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val issueRepository: IssueRepository = koinInject()
    
    var showReportIssueDialog by remember { mutableStateOf<MediaRequest?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val filters = listOf("All", "Pending", "Approved", "Available", "Declined")
    // Case-insensitive matching for initial filter
    var selectedFilter by remember(initialFilter) { 
        mutableStateOf(filters.find { it.equals(initialFilter, ignoreCase = true) } ?: "All") 
    }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedFilter) {
        if (selectedFilter != "All") {
             snackbarHostState.showSnackbar("Showing $selectedFilter requests")
        }
    }
    
    var pullRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLoading) {
        if (!isLoading && pullRefreshing) {
            pullRefreshing = false
        }
    }

    // Update requests data when entering the screen (background refresh if already loaded)
    LaunchedEffect(Unit) {
        viewModel.refreshRequests()
    }

    val gradients = LocalUnderseerrGradients.current
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Requests", color = gradients.onAppBar) },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = if (selectedFilter != "All") MaterialTheme.colorScheme.primary else gradients.onAppBar
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            filters.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter) },
                                    onClick = {
                                        selectedFilter = filter
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { 
                        pullRefreshing = true
                        viewModel.refreshRequests() 
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = gradients.onAppBar)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = gradients.onAppBar,
                    actionIconContentColor = gradients.onAppBar
                ),
                modifier = Modifier.background(gradients.appBar)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.background(gradients.background),
        containerColor = Color.Transparent
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                viewModel.refreshRequests(isPullToRefresh = true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredRequests = when (selectedFilter) {
                    "All" -> userRequests.distinctBy { it.id }
                    "Pending" -> userRequests.distinctBy { it.id }.filter { it.status == RequestStatus.PENDING }
                    "Approved" -> userRequests.distinctBy { it.id }.filter { it.status == RequestStatus.APPROVED }
                    "Available" -> userRequests.distinctBy { it.id }.filter { it.status == RequestStatus.AVAILABLE }
                    "Declined" -> userRequests.distinctBy { it.id }.filter { it.status == RequestStatus.DECLINED }
                    else -> userRequests.distinctBy { it.id }
                }
                
                val hasCachedData = filteredRequests.isNotEmpty()
                val isOffline = error != null

                val authViewModel = koinViewModel<AuthViewModel>()
                val serverUrl by authViewModel.getServerUrl().collectAsState("")
                val currentUser by authViewModel.currentUser.collectAsState()
                
                // Determine permissions
                val canManageRequests = remember(currentUser) {
                    val user = currentUser
                    if (user != null) {
                        user.permissions.isAdmin || user.permissions.canManageRequests
                    } else {
                        false
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Offline Banner pushes content down
                    app.lusk.underseerr.ui.components.OfflineBanner(
                        visible = isOffline && hasCachedData
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (hasCachedData) {
                            RequestsList(
                                requests = filteredRequests,
                                onRequestClick = onRequestClick,
                                contentPadding = PaddingValues(16.dp),
                                onLoadMore = { viewModel.loadMoreRequests() },
                                isLoadingMore = isLoading && !pullRefreshing,
                                serverUrl = serverUrl ?: "",
                                canManageRequests = canManageRequests,
                                onApprove = { viewModel.approveRequest(it) },
                                onDecline = { viewModel.declineRequest(it) },
                                onReportIssue = { showReportIssueDialog = it },
                                onRepair = { viewModel.repairRequest(it) },
                                onAddToWatchlist = { request ->
                                    discoveryViewModel.addToWatchlist(request.mediaId, request.mediaType, null)
                                },
                                error = error
                            )
                        } 
                        
                        if (isOffline && !hasCachedData) {
                            app.lusk.underseerr.ui.components.UnifiedErrorDisplay(
                                message = error ?: "Unknown error",
                                onRetry = { viewModel.refreshRequests() }
                            )
                        } else if (!isLoading && !hasCachedData) {
                            EmptyRequestsDisplay(status = selectedFilter)
                        }

                        // 2. Overlays
                        if (isLoading && !pullRefreshing && !hasCachedData) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }

    // Report Issue Dialog
    showReportIssueDialog?.let { request ->
        ReportIssueDialog(
            mediaTitle = request.title,
            isTvShow = request.mediaType == MediaType.TV,
            numberOfSeasons = 0,
            onDismiss = { showReportIssueDialog = null },
            onSubmit = { issueType, message, season, episode ->
                scope.launch {
                    val result = issueRepository.createIssue(
                        issueType = issueType,
                        message = message,
                        mediaId = request.mediaId,
                        problemSeason = season,
                        problemEpisode = episode
                    )
                    
                    showReportIssueDialog = null
                    
                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("Issue reported successfully")
                    } else {
                        val errorMsg = result.errorOrNull()?.message ?: "Unknown error"
                        snackbarHostState.showSnackbar("Failed to report issue: $errorMsg")
                    }
                }
            }
        )
    }
}

@Composable
private fun RequestsList(
    requests: List<MediaRequest>,
    onRequestClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    serverUrl: String,
    canManageRequests: Boolean = false,
    onApprove: (Int) -> Unit = {},
    onDecline: (Int) -> Unit = {},
    onReportIssue: (MediaRequest) -> Unit = {},
    onRepair: (MediaRequest) -> Unit = {},
    onAddToWatchlist: (MediaRequest) -> Unit = {},
    error: String? = null
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(listState, requests.size, isLoadingMore, error) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= requests.size - 5 && !isLoadingMore && requests.isNotEmpty() && error == null) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = requests,
            key = { request -> request.id }
        ) { request ->
            RequestItem(
                request = request,
                serverUrl = serverUrl,
                onClick = { onRequestClick(request.id) },
                canManageRequests = canManageRequests,
                onApprove = onApprove,
                onDecline = onDecline,
                onReportIssue = onReportIssue,
                onRepair = onRepair,
                onAddToWatchlist = onAddToWatchlist
            )
        }
        
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RequestItem(
    request: MediaRequest,
    serverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    canManageRequests: Boolean = false,
    onApprove: (Int) -> Unit = {},
    onDecline: (Int) -> Unit = {},
    onReportIssue: (MediaRequest) -> Unit = {},
    onRepair: (MediaRequest) -> Unit = {},
    onAddToWatchlist: (MediaRequest) -> Unit = {}
) {
    LaunchedEffect(request.title, request.posterPath) {
        if (request.title == "Title Unavailable" || request.posterPath == null) {
            onRepair(request)
        }
    }

    val gradients = LocalUnderseerrGradients.current
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.background(gradients.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .width(70.dp)
                        .height(100.dp)
                ) {
                    val fullImageUrl = remember(request.posterPath, serverUrl) {
                        val path = request.posterPath
                        when {
                            path == null -> null
                            path.startsWith("http") -> path
                            path.startsWith("/api/v1/proxy") -> {
                                if (serverUrl.isNotEmpty()) "${serverUrl.trimEnd('/')}$path" else null
                            }
                            path.startsWith("/") -> "https://image.tmdb.org/t/p/w200$path"
                            else -> path
                        }
                    }

                    AsyncImage(
                        imageUrl = fullImageUrl,
                        contentDescription = request.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 24.dp) // Space for menu
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Requested ${request.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val seasons = request.seasons
                        if (!seasons.isNullOrEmpty()) {
                            Text(
                                text = if (seasons.contains(0)) "All seasons" else "Seasons: ${seasons.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    StatusBadge(status = request.status)
                }
            }
            
            // Quick Actions Menu
            if (canManageRequests || request.status == RequestStatus.APPROVED || request.status == RequestStatus.AVAILABLE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Quick Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (showMenu) {
                        val gradients = LocalUnderseerrGradients.current
                        ModalBottomSheet(
                            onDismissRequest = { showMenu = false },
                            containerColor = Color.Transparent,
                            dragHandle = { BottomSheetDefaults.DragHandle(color = gradients.onSurface.copy(alpha = 0.4f)) }
                        ) {
                            Box(modifier = Modifier.background(gradients.surface).padding(bottom = 32.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = request.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = gradients.onSurface,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    if (canManageRequests && request.status == RequestStatus.PENDING) {
                                        // Approve
                                        Surface(
                                            onClick = {
                                                onApprove(request.id)
                                                showMenu = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            ListItem(
                                                headlineContent = { Text("Approve", fontWeight = FontWeight.SemiBold) },
                                                leadingContent = { Icon(Icons.Default.ThumbUp, null, tint = MaterialTheme.colorScheme.primary) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = gradients.onSurface)
                                            )
                                        }
                                        
                                        // Decline
                                        Surface(
                                            onClick = {
                                                onDecline(request.id)
                                                showMenu = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                        ) {
                                            ListItem(
                                                headlineContent = { Text("Decline", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                                                leadingContent = { Icon(Icons.Default.ThumbDown, null, tint = MaterialTheme.colorScheme.error) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = gradients.onSurface)
                                            )
                                        }
                                    }
                                    
                                    // Add to Watchlist
                                    Surface(
                                        onClick = {
                                            onAddToWatchlist(request)
                                            showMenu = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        ListItem(
                                            headlineContent = { Text("Add to Watchlist", fontWeight = FontWeight.SemiBold) },
                                            leadingContent = { Icon(androidx.compose.material.icons.Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary) },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = gradients.onSurface)
                                        )
                                    }
                                    
                                    if (request.status == RequestStatus.APPROVED || request.status == RequestStatus.AVAILABLE) {
                                        // Report Issue
                                        Surface(
                                            onClick = {
                                                onReportIssue(request)
                                                showMenu = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                        ) {
                                            ListItem(
                                                headlineContent = { Text("Report Issue", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                                                leadingContent = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = gradients.onSurface)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (status) {
        RequestStatus.PENDING -> Triple(Color(0xFFFFC107), Color.Black, "Pending")
        RequestStatus.APPROVED -> Triple(Color(0xFF2196F3), Color.White, "Processing")
        RequestStatus.AVAILABLE -> Triple(Color(0xFF4CAF50), Color.White, "Available")
        RequestStatus.DECLINED -> Triple(Color(0xFFF44336), Color.White, "Declined")
    }
    
    val gradients = LocalUnderseerrGradients.current
    Surface(
        color = backgroundColor,
        shape = gradients.statusBadgeShape,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyRequestsDisplay(
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No $status requests",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Your requests will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


