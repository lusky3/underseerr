package app.lusk.underseerr.presentation.request

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    viewModel: RequestViewModel,
    onRequestClick: (Int) -> Unit,
    initialFilter: String? = null,
    modifier: Modifier = Modifier
) {
    val userRequests by viewModel.userRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(initialFilter ?: "All") }
    val filters = listOf("All", "Pending", "Approved", "Available", "Declined")
    var showFilterMenu by remember { mutableStateOf(false) }
    
    var pullRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLoading) {
        if (!isLoading && pullRefreshing) {
            pullRefreshing = false
        }
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
                                onDecline = { viewModel.declineRequest(it) }
                            )
                        } 
                        
                        if (isOffline && !hasCachedData) {
                            app.lusk.underseerr.ui.components.UnifiedErrorDisplay(
                                message = error!!,
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
    onDecline: (Int) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(listState, requests.size, isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= requests.size - 5 && !isLoadingMore && requests.isNotEmpty()) {
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
                onDecline = onDecline
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

@Composable
private fun RequestItem(
    request: MediaRequest,
    serverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    canManageRequests: Boolean = false,
    onApprove: (Int) -> Unit = {},
    onDecline: (Int) -> Unit = {}
) {
    val gradients = LocalUnderseerrGradients.current
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            if (canManageRequests && request.status == RequestStatus.PENDING) {
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
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Approve") },
                            onClick = {
                                onApprove(request.id)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Decline") },
                            onClick = {
                                onDecline(request.id)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
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


