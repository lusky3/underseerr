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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Approved", "Available", "Declined")
    var showFilterMenu by remember { mutableStateOf(false) }
    
    var pullRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLoading) {
        if (!isLoading && pullRefreshing) {
            pullRefreshing = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Requests") },
                actions = {
                    // Filter button
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = if (selectedFilter != "All") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            filters.forEach { filter ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            filter,
                                            fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedFilter = filter
                                        showFilterMenu = false
                                    },
                                    trailingIcon = if (filter == selectedFilter) {
                                        { 
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
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
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredRequests = when (selectedFilter) {
                    "All" -> userRequests
                    "Pending" -> userRequests.filter { it.status == RequestStatus.PENDING }
                    "Approved" -> userRequests.filter { it.status == RequestStatus.APPROVED }
                    "Available" -> userRequests.filter { it.status == RequestStatus.AVAILABLE }
                    "Declined" -> userRequests.filter { it.status == RequestStatus.DECLINED }
                    else -> userRequests
                }
                
                val hasCachedData = filteredRequests.isNotEmpty()
                val isOffline = error != null

                // 1. Content Layer
                when {
                    hasCachedData -> {
                        RequestsList(
                            requests = filteredRequests,
                            onRequestClick = onRequestClick,
                            contentPadding = PaddingValues(top = if (isOffline) 48.dp else 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                        )
                    }
                    isOffline -> {
                        ErrorDisplay(
                            message = error!!,
                            onRetry = { viewModel.refreshRequests() }
                        )
                    }
                    !isLoading -> {
                        EmptyRequestsDisplay(status = selectedFilter)
                    }
                }
                
                // 2. Overlays
                if (isLoading && !pullRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
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
                
                // Offline Banner
                app.lusk.underseerr.ui.components.OfflineBanner(
                    visible = isOffline && hasCachedData,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            }
        }
    }

@Composable
private fun RequestsList(
    requests: List<MediaRequest>,
    onRequestClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Poster with rounded corners
            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp)
            ) {
                AsyncImage(
                    imageUrl = request.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" },
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
                        overflow = TextOverflow.Ellipsis
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
                
                // Large colorful status badge
                StatusBadge(status = request.status)
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
        RequestStatus.PENDING -> Triple(
            Color(0xFFFFC107), // Amber
            Color.Black,
            "Pending"
        )
        RequestStatus.APPROVED -> Triple(
            Color(0xFF2196F3), // Blue
            Color.White,
            "Processing"
        )
        RequestStatus.AVAILABLE -> Triple(
            Color(0xFF4CAF50), // Green
            Color.White,
            "Available"
        )
        RequestStatus.DECLINED -> Triple(
            Color(0xFFF44336), // Red
            Color.White,
            "Declined"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
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
