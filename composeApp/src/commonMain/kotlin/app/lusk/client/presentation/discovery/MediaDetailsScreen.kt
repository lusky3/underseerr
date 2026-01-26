package app.lusk.client.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.repository.IssueRepository
import app.lusk.client.presentation.issue.ReportIssueDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import app.lusk.client.presentation.request.RequestDialog
import app.lusk.client.presentation.request.RequestViewModel

import app.lusk.client.ui.components.PosterImage
import app.lusk.client.ui.components.BackdropImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    viewModel: DiscoveryViewModel,
    mediaType: MediaType,
    mediaId: Int,
    openRequest: Boolean = false,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requestViewModel: RequestViewModel = koinViewModel()
    val issueRepository: IssueRepository = koinInject()
    val state by viewModel.mediaDetailsState.collectAsState()
    var showRequestDialog by remember { mutableStateOf(false) }
    var showReportIssueDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auto-open request dialog if requested
    LaunchedEffect(openRequest) {
        if (openRequest) {
            showRequestDialog = true
        }
    }
    
    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadMediaDetails(mediaType, mediaId)
    }

    var isRefreshing by remember { mutableStateOf(false) }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        containerColor = Color.Transparent
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadMediaDetails(mediaType, mediaId)
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val detailsState = state) {
                is MediaDetailsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is MediaDetailsState.Success -> {
                    val canModifyRequest = !detailsState.details.isAvailable && 
                        (detailsState.details.isRequested || detailsState.details.isPartiallyAvailable) && 
                        detailsState.details.isPartialRequestsEnabled && 
                        detailsState.details.numberOfSeasons > 0

                    MediaDetailsContent(
                        details = detailsState.details,
                        onRequestClick = { showRequestDialog = true },
                        onReportIssueClick = { showReportIssueDialog = true },
                        onBackClick = onBackClick
                    )
                    
                    if (showRequestDialog) {
                        RequestDialog(
                            mediaId = mediaId,
                            mediaType = mediaType,
                            mediaTitle = detailsState.details.title,
                            seasonCount = detailsState.details.numberOfSeasons,
                            partialRequestsEnabled = detailsState.details.isPartialRequestsEnabled,
                            isModify = canModifyRequest,
                            requestedSeasons = detailsState.details.requestedSeasons,
                            viewModel = requestViewModel,
                            onDismiss = { showRequestDialog = false },
                            onSuccess = {
                                showRequestDialog = false
                                viewModel.loadMediaDetails(mediaType, mediaId)
                            }
                        )
                    }
                    
                    // Report Issue Dialog
                    if (showReportIssueDialog) {
                        ReportIssueDialog(
                            mediaTitle = detailsState.details.title,
                            isTvShow = mediaType == MediaType.TV,
                            numberOfSeasons = detailsState.details.numberOfSeasons,
                            onDismiss = { showReportIssueDialog = false },
                            onSubmit = { issueType, message, season, episode ->
                                scope.launch {
                                    // Get the media ID from the details
                                    val mediaInfoId = detailsState.details.mediaInfoId
                                    if (mediaInfoId != null) {
                                        issueRepository.createIssue(
                                            issueType = issueType,
                                            message = message,
                                            mediaId = mediaInfoId,
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
                                    } else {
                                        showReportIssueDialog = false
                                        snackbarHostState.showSnackbar(
                                            "Cannot report issue: Media not available in library"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                is MediaDetailsState.Error -> {
                    ErrorDisplay(
                        message = detailsState.message,
                        onRetry = { viewModel.loadMediaDetails(mediaType, mediaId) }
                    )
                }
                MediaDetailsState.Idle -> {
                    // Initial state, loading will be triggered by LaunchedEffect
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaDetailsContent(
    details: MediaDetails,
    onRequestClick: () -> Unit,
    onReportIssueClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Backdrop with fade overlay
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    details.backdropPath?.let { backdropPath ->
                        BackdropImage(
                            backdropPath = backdropPath,
                            title = details.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    
                    // Bottom fade gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
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
                    
                    // Back button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Info Card overlapping backdrop
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-48).dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Title and Year row
                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Info chips row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            details.releaseDate?.let { releaseDate ->
                                Text(
                                    text = releaseDate.take(4),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            details.voteAverage?.let { rating ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${(rating * 10.0).roundToInt() / 10.0}/10",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            details.runtime?.let { runtime ->
                                val hours = runtime / 60
                                val mins = runtime % 60
                                Text(
                                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Overview
                        details.overview?.let { overview ->
                            Text(
                                text = overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Request button - prominent gradient style
                        val canModifyRequest = !details.isAvailable && 
                            (details.isRequested || details.isPartiallyAvailable) && 
                            details.isPartialRequestsEnabled && 
                            details.numberOfSeasons > 0

                        Button(
                            onClick = onRequestClick,
                            enabled = (!details.isAvailable && !details.isRequested) || canModifyRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = when {
                                    details.isAvailable -> "Available"
                                    canModifyRequest -> "Modify Request"
                                    details.isRequested -> "Requested"
                                    else -> "Request"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        // Report Issue button
                        if (details.isAvailable || details.isRequested) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onReportIssueClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Report Issue")
                            }
                        }
                    }
                }
            }

            // Genres section
            if (details.genres.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Genres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(details.genres) { genre ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(genre) }
                                )
                            }
                        }
                    }
                }
            }

            // Cast section (placeholder - would need cast data from API)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Placeholder cast members
                        items(6) { index ->
                            CastMember(
                                name = listOf("Lead Actor", "Supporting", "Director", "Producer", "Writer", "Composer")[index],
                                role = listOf("Main Character", "Side Role", "Direction", "Production", "Screenplay", "Music")[index]
                            )
                        }
                    }
                }
            }

            // Runtime/Status info
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    details.status?.let { status ->
                        InfoItem(
                            label = "Status",
                            value = status
                        )
                    }
                    
                    if (details.numberOfSeasons > 0) {
                        InfoItem(
                            label = "Seasons",
                            value = details.numberOfSeasons.toString()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CastMember(
    name: String,
    role: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.first().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = role,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
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
