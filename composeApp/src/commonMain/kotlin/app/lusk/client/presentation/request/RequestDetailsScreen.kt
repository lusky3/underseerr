package app.lusk.client.presentation.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.lusk.client.domain.model.MediaRequest
import app.lusk.client.domain.model.RequestStatus
import app.lusk.client.domain.repository.IssueRepository
import app.lusk.client.presentation.issue.ReportIssueDialog
import app.lusk.client.ui.components.AsyncImage
import app.lusk.client.util.formatDate
import app.lusk.client.domain.model.MediaType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: Int,
    viewModel: RequestViewModel,
    onBackClick: () -> Unit,
    onModifyRequest: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    issueRepository: IssueRepository = koinInject()
) {
    val userRequests by viewModel.userRequests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val partialRequestsEnabled by viewModel.partialRequestsEnabled.collectAsState()
    
    val request = userRequests.find { it.id == requestId }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showReportIssueDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (request?.status == RequestStatus.PENDING) {
                        IconButton(onClick = { showCancelDialog = true }) {
                            Icon(Icons.Default.Delete, "Cancel Request")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                viewModel.refreshRequests()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                request == null && !isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Request not found")
                    }
                }
                request != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RequestDetailsContent(
                            request = request,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (partialRequestsEnabled && request.mediaType == MediaType.TV) {
                            Button(
                                onClick = { onModifyRequest(request.mediaId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Request More Seasons")
                            }
                        }
                        
                        // Show Report Issue button for approved or available requests
                        if (request.status == RequestStatus.APPROVED || 
                            request.status == RequestStatus.AVAILABLE) {
                            OutlinedButton(
                                onClick = { showReportIssueDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Report Issue")
                            }
                        }
                    }
                }
                // ... (loading state)
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            if (error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error!!)
                }
            }
        }
    }
    
    if (showCancelDialog && request != null) {
        CancelRequestDialog(
            requestTitle = request.title,
            onConfirm = {
                viewModel.cancelRequest(requestId)
                showCancelDialog = false
                onBackClick()
            },
            onDismiss = { showCancelDialog = false }
        )
    }
    
    // Report Issue Dialog
    if (showReportIssueDialog && request != null) {
        ReportIssueDialog(
            mediaTitle = request.title,
            isTvShow = request.mediaType == MediaType.TV,
            numberOfSeasons = 0, // Request details doesn't have total seasons count
            onDismiss = { showReportIssueDialog = false },
            onSubmit = { issueType, message, season, episode ->
                scope.launch {
                    // Use the media ID from the request
                    // Note: This might need adjustment based on how mediaInfoId is obtained
                    issueRepository.createIssue(
                        issueType = issueType,
                        message = message,
                        mediaId = request.mediaId,
                        problemSeason = season,
                        problemEpisode = episode
                    ).fold(
                        onSuccess = {
                            showReportIssueDialog = false
                            snackbarHostState.showSnackbar("Issue reported successfully")
                        },
                        onFailure = { error ->
                            showReportIssueDialog = false
                            snackbarHostState.showSnackbar(
                                "Failed to report issue: ${error.message}"
                            )
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun RequestDetailsContent(
    request: MediaRequest,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    imageUrl = request.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    contentDescription = request.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    StatusChip(status = request.status)
                    
                    Text(
                        text = request.mediaType.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Request Information",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    InfoRow(
                        label = "Request ID",
                        value = request.id.toString()
                    )
                    
                    InfoRow(
                        label = "Status",
                        value = request.status.name
                    )
                    
                    InfoRow(
                        label = "Requested Date",
                        value = formatDate(request.requestedDate)
                    )
                    
                    InfoRow(
                        label = "Media Type",
                        value = request.mediaType.name
                    )
                    
                    val seasons = request.seasons
                    if (!seasons.isNullOrEmpty()) {
                        InfoRow(
                            label = "Seasons",
                            value = if (seasons.contains(0)) {
                                "All seasons"
                            } else {
                                seasons.joinToString(", ")
                            }
                        )
                    }
                }
            }
        }
        
        item {
            when (request.status) {
                RequestStatus.PENDING -> {
                    InfoCard(
                        title = "Pending Approval",
                        message = "Your request is waiting for approval from an administrator."
                    )
                }
                RequestStatus.APPROVED -> {
                    InfoCard(
                        title = "Approved",
                        message = "Your request has been approved and is being processed."
                    )
                }
                RequestStatus.AVAILABLE -> {
                    InfoCard(
                        title = "Available",
                        message = "The requested media is now available in your Plex library!"
                    )
                }
                RequestStatus.DECLINED -> {
                    InfoCard(
                        title = "Declined",
                        message = "Your request was declined by an administrator."
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CancelRequestDialog(
    requestTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Request") },
        text = { Text("Are you sure you want to cancel the request for \"$requestTitle\"?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Request")
            }
        }
    )
}
