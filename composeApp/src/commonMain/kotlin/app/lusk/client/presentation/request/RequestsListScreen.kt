package app.lusk.client.presentation.request

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.*
import app.lusk.client.domain.model.MediaRequest
import app.lusk.client.domain.model.RequestStatus
import app.lusk.client.ui.components.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsListScreen(
    viewModel: RequestViewModel,
    onRequestClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val userRequests by viewModel.userRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Pending", "Approved", "Available", "Declined")
    
    var pullRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                actions = {
                    IconButton(onClick = { viewModel.refreshRequests() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                viewModel.refreshRequests()
                pullRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredRequests = when (selectedTab) {
                        0 -> userRequests
                        1 -> userRequests.filter { it.status == RequestStatus.PENDING }
                        2 -> userRequests.filter { it.status == RequestStatus.APPROVED }
                        3 -> userRequests.filter { it.status == RequestStatus.AVAILABLE }
                        4 -> userRequests.filter { it.status == RequestStatus.DECLINED }
                        else -> userRequests
                    }
                    
                    when {
                        error != null -> {
                            ErrorDisplay(
                                message = error!!,
                                onRetry = { viewModel.refreshRequests() }
                            )
                        }
                        filteredRequests.isEmpty() && !isLoading -> {
                            EmptyRequestsDisplay(status = tabs[selectedTab])
                        }
                        else -> {
                            RequestsList(
                                requests = filteredRequests,
                                onRequestClick = onRequestClick
                            )
                        }
                    }
                    
                    if (isLoading && !pullRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    // Top fade gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom fade gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
                }
            }
        }
    }

@Composable
private fun RequestsList(
    requests: List<MediaRequest>,
    onRequestClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            RequestItem(
                request = request,
                onClick = { onRequestClick(request.id) }
            )
        }
    }
}

@Composable
private fun RequestItem(
    request: MediaRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                imageUrl = request.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" },
                contentDescription = request.title,
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = request.mediaType.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val seasons = request.seasons
                    if (!seasons.isNullOrEmpty()) {
                        Text(
                            text = if (seasons.contains(0)) {
                                "All seasons"
                            } else {
                                "Seasons: ${seasons.joinToString(", ")}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                StatusChip(status = request.status)
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary to "Pending"
        RequestStatus.APPROVED -> MaterialTheme.colorScheme.primary to "Approved"
        RequestStatus.AVAILABLE -> MaterialTheme.colorScheme.secondary to "Available"
        RequestStatus.DECLINED -> MaterialTheme.colorScheme.error to "Declined"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
private fun ErrorDisplay(
    message: String,
    onRetry: () -> Unit,
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
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
