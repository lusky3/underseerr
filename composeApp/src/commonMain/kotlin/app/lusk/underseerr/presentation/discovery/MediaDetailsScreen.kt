package app.lusk.underseerr.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.ui.components.MediaSection
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
import app.lusk.underseerr.domain.model.CastMember
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.presentation.issue.ReportIssueDialog
import app.lusk.underseerr.domain.model.Season
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import app.lusk.underseerr.presentation.request.RequestDialog
import app.lusk.underseerr.presentation.request.RequestViewModel

import app.lusk.underseerr.ui.components.PosterImage
import app.lusk.underseerr.ui.components.BackdropImage
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients
import app.lusk.underseerr.domain.model.RelatedVideo
import app.lusk.underseerr.domain.model.VideoType
import app.lusk.underseerr.util.openUrl
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
    onPersonClick: (Int) -> Unit,
    onMediaClick: (MediaType, Int) -> Unit,
    onGenreClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val requestViewModel: RequestViewModel = koinViewModel()
    val issueRepository: IssueRepository = koinInject()
    val state by viewModel.mediaDetailsState.collectAsState()
    val watchlistIds by viewModel.watchlistIds.collectAsState()
    val recommendations = viewModel.recommendations.collectAsLazyPagingItems()
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
    // Auto-dismiss refresh indicator when data finishes loading
    LaunchedEffect(state) {
        if (state !is MediaDetailsState.Loading && isRefreshing) {
            isRefreshing = false
        }
    }
    
    val gradients = LocalUnderseerrGradients.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.background(gradients.background),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadMediaDetails(mediaType, mediaId, isRefresh = true)
                recommendations.refresh()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val detailsState = state) {
                is MediaDetailsState.Loading -> {
                    // Only show centralized spinner if NOT refreshing
                    if (!isRefreshing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is MediaDetailsState.Success -> {
                    val canModifyRequest = !detailsState.details.isAvailable && 
                        (detailsState.details.isRequested || detailsState.details.isPartiallyAvailable) && 
                        detailsState.details.isPartialRequestsEnabled && 
                        detailsState.details.numberOfSeasons > 0

                    MediaDetailsContent(
                        details = detailsState.details,
                        mediaId = mediaId,
                        mediaType = mediaType,
                        isInWatchlist = watchlistIds.contains(mediaId),
                        recommendations = recommendations,
                        viewModel = viewModel,
                        onRequestClick = { showRequestDialog = true },
                        onReportIssueClick = { showReportIssueDialog = true },
                        onBackClick = onBackClick,
                        onPersonClick = onPersonClick,
                        onMediaClick = onMediaClick,
                        onGenreClick = onGenreClick
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
                                        ).onSuccess {
                                            showReportIssueDialog = false
                                            snackbarHostState.showSnackbar("Issue reported successfully")
                                        }.onError { error ->
                                            showReportIssueDialog = false
                                            snackbarHostState.showSnackbar(
                                                "Failed to report issue: ${error.message}"
                                            )
                                        }
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
                    app.lusk.underseerr.ui.components.UnifiedErrorDisplay(
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
    mediaId: Int,
    mediaType: MediaType,
    isInWatchlist: Boolean,
    recommendations: LazyPagingItems<SearchResult>,
    viewModel: DiscoveryViewModel,
    onRequestClick: () -> Unit,
    onReportIssueClick: () -> Unit,
    onBackClick: () -> Unit,
    onPersonClick: (Int) -> Unit,
    onMediaClick: (MediaType, Int) -> Unit,
    onGenreClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        var isInfoExpanded by remember { mutableStateOf(false) }
        
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
                            .statusBarsPadding()
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
                val gradients = LocalUnderseerrGradients.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-48).dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.background(gradients.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                        // Title and Year row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = details.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            
                            details.status?.let { status ->
                                StatusBadge(status = status)
                            }
                        }
                        
                        details.tagline?.let { tagline ->
                            if (tagline.isNotBlank()) {
                                Text(
                                    text = tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Info chips row - Clickable to expand
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isInfoExpanded = !isInfoExpanded }
                                .padding(vertical = 4.dp)
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

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                imageVector = if (isInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isInfoExpanded) "Show Less" else "Show More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isInfoExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    val primaryLabel = if (mediaType == MediaType.MOVIE) "Theatre Release" else "First Aired"
                                    details.releaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                                        InfoItem(label = primaryLabel, value = date, modifier = Modifier.weight(1f))
                                    }
                                    
                                    details.digitalReleaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                                        InfoItem(label = "Digital Release", value = date, modifier = Modifier.weight(1f))
                                    }
                                }

                                if (!details.physicalReleaseDate.isNullOrEmpty() || !details.lastAirDate.isNullOrEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                        details.physicalReleaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                                            InfoItem(label = "Physical Release", value = date, modifier = Modifier.weight(1f))
                                        }
                                        details.lastAirDate?.takeIf { it.isNotBlank() }?.let { date ->
                                            InfoItem(label = "Latest Air Date", value = date, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
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

                        val gradients = LocalUnderseerrGradients.current
                        val isEnabled = (!details.isAvailable && !details.isRequested) || canModifyRequest

                        Button(
                            onClick = onRequestClick,
                            enabled = isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .then(
                                    if (isEnabled) Modifier.background(gradients.primary, shape = RoundedCornerShape(24.dp))
                                    else Modifier
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnabled) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            contentPadding = PaddingValues()
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                if (isInWatchlist) {
                                    // Pass null for ratingKey - details.ratingKey is the Plex Library key from Overseerr,
                                    // but watchlist operations need the Plex Discover key which is different.
                                    // findPlexRatingKey() will lookup the correct one.
                                    viewModel.removeFromWatchlist(mediaId, mediaType, null)
                                } else {
                                    // For adding, also let it lookup the correct key
                                    viewModel.addToWatchlist(mediaId, mediaType, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = if (isInWatchlist) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Delete else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist")
                        }
                        
                        // Watch in Plex button (only if available)
                        if (details.isAvailable) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { /* TODO: Open in Plex */ },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch in Plex")
                            }
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
        }
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
                                    onClick = { onGenreClick(genre.id, genre.name) },
                                    label = { Text(genre.name) }
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
                        items(details.cast) { castMember ->
                            CastMemberItem(
                                name = castMember.name,
                                role = castMember.character,
                                profilePath = castMember.profilePath,
                                onClick = { onPersonClick(castMember.id) }
                            )
                        }
                    }
                }
            }
            
            // Seasons section
            if (details.seasons.isNotEmpty()) {
                item {
                    val gradients = LocalUnderseerrGradients.current
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = gradients.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(details.seasons) { season ->
                                SeasonCard(season = season)
                            }
                        }
                    }
                }
            }

            // Videos section (Trailers, Featurettes, etc.)
            if (details.relatedVideos.isNotEmpty()) {
                item {
                    val gradients = LocalUnderseerrGradients.current
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Videos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = gradients.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Group videos by type and display each group
                val groupedVideos = details.relatedVideos.groupBy { it.type }
                val videoOrder = listOf(
                    VideoType.TRAILER,
                    VideoType.TEASER,
                    VideoType.CLIP,
                    VideoType.FEATURETTE,
                    VideoType.BEHIND_THE_SCENES,
                    VideoType.BLOOPERS,
                    VideoType.OPENING_CREDITS,
                    VideoType.OTHER
                )
                
                videoOrder.forEach { videoType ->
                    groupedVideos[videoType]?.let { videos ->
                        item {
                            val gradients = LocalUnderseerrGradients.current
                            Text(
                                text = when (videoType) {
                                    VideoType.TRAILER -> "Trailers"
                                    VideoType.TEASER -> "Teasers"
                                    VideoType.CLIP -> "Clips"
                                    VideoType.FEATURETTE -> "Featurettes"
                                    VideoType.BEHIND_THE_SCENES -> "Behind the Scenes"
                                    VideoType.BLOOPERS -> "Bloopers"
                                    VideoType.OPENING_CREDITS -> "Opening Credits"
                                    VideoType.OTHER -> "Other Videos"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = gradients.onSurface.copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(videos) { video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { openUrl(video.url) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Recommendations section
            if (recommendations.itemCount > 0) {
                item {
                    val gradients = LocalUnderseerrGradients.current
                    Spacer(modifier = Modifier.height(24.dp))
                    MediaSection(
                        title = "Recommendations",
                        items = recommendations,
                        watchlistIds = emptySet(), // We don't have this info here easily
                        onItemClickWithType = { id, type ->
                            onMediaClick(type, id)
                        },
                        titleStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = gradients.onSurface
                        )
                    )
                }
            }
            
            // Bottom spacing to match recommendations spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CastMemberItem(
    name: String,
    role: String?,
    profilePath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (profilePath != null) {
                AsyncImage(
                    imageUrl = "https://image.tmdb.org/t/p/w200$profilePath",
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.first().toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        if (role != null) {
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

/**
 * Video card component for displaying video thumbnails with play button overlay.
 */
@Composable
private fun VideoCard(
    video: RelatedVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradients = LocalUnderseerrGradients.current
    
    Column(
        modifier = modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video thumbnail with play button overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(gradients.surface),
            contentAlignment = Alignment.Center
        ) {
            // YouTube thumbnail
            if (video.site == "YouTube") {
                AsyncImage(
                    imageUrl = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg",
                    contentDescription = video.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            // Play button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Video type badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (video.type) {
                    VideoType.TRAILER -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    VideoType.TEASER -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                    VideoType.FEATURETTE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = when (video.type) {
                        VideoType.TRAILER -> "Trailer"
                        VideoType.TEASER -> "Teaser"
                        VideoType.CLIP -> "Clip"
                        VideoType.FEATURETTE -> "Featurette"
                        VideoType.BEHIND_THE_SCENES -> "BTS"
                        VideoType.BLOOPERS -> "Bloopers"
                        VideoType.OPENING_CREDITS -> "Opening"
                        VideoType.OTHER -> "Video"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Video name
        Text(
            text = video.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = gradients.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun SeasonCard(
    season: Season,
    modifier: Modifier = Modifier
) {
    val gradients = LocalUnderseerrGradients.current
    
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2/3f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!season.posterPath.isNullOrEmpty()) {
                    PosterImage(
                        posterPath = season.posterPath,
                        title = season.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = season.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = gradients.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        if (season.episodeCount > 0) {
            Text(
                text = "${season.episodeCount} Episodes",
                style = MaterialTheme.typography.labelSmall,
                color = gradients.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A colorful badge indicating the status of a movie or TV show.
 */
@Composable
private fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status.lowercase()) {
        "returning series" -> Color(0xFF4CAF50) // Green
        "ended" -> Color(0xFFF44336) // Red
        "canceled" -> Color(0xFF9E9E9E) // Grey
        "released" -> Color(0xFF2196F3) // Blue
        "in production" -> Color(0xFFFF9800) // Orange
        "planned" -> Color(0xFF673AB7) // Purple
        "pilot" -> Color(0xFFE91E63) // Pink
        "post production" -> Color(0xFF00BCD4) // Cyan
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = backgroundColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
