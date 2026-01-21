package app.lusk.client.presentation.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
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
                        onReportIssueClick = { showReportIssueDialog = true }
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

@Composable
private fun MediaDetailsContent(
    details: MediaDetails,
    onRequestClick: () -> Unit,
    onReportIssueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Backdrop image
            details.backdropPath?.let { backdropPath ->
                BackdropImage(
                    backdropPath = backdropPath,
                    title = details.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                details.posterPath?.let { posterPath ->
                    PosterImage(
                        posterPath = posterPath,
                        title = details.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(180.dp)
                    )
                }

                // Title and basic info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = details.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    details.releaseDate?.let { releaseDate ->
                        Text(
                            text = releaseDate.take(4),
                            style = MaterialTheme.typography.bodyLarge,
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${(rating * 10.0).roundToInt() / 10.0}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Request button
                    val canModifyRequest = !details.isAvailable && 
                        (details.isRequested || details.isPartiallyAvailable) && 
                        details.isPartialRequestsEnabled && 
                        details.numberOfSeasons > 0 // Only for TV basically

                    Button(
                        onClick = onRequestClick,
                        enabled = (!details.isAvailable && !details.isRequested) || canModifyRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                details.isAvailable -> "Available"
                                canModifyRequest -> "Modify Request"
                                details.isRequested -> "Requested"
                                else -> "Request"
                            }
                        )
                    }
                    
                    // Report Issue button (only for available/requested media)
                    if (details.isAvailable || details.isRequested) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onReportIssueClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Report Issue")
                        }
                    }
                }
            }
        }

        item {
            // Overview
            details.overview?.let { overview ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            // Genres
            if (details.genres.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.titleLarge
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

        item {
            // Runtime/Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                details.runtime?.let { runtime ->
                    InfoItem(
                        label = "Runtime",
                        value = "$runtime min"
                    )
                }

                details.status?.let { status ->
                    InfoItem(
                        label = "Status",
                        value = status
                    )
                }
            }
        }
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
            style = MaterialTheme.typography.bodyLarge
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
