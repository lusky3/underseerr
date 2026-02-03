package app.lusk.underseerr.presentation.issue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.IssueStatus
import app.lusk.underseerr.domain.model.IssueType
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.ui.components.PosterImage
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients
import org.koin.compose.viewmodel.koinViewModel

/**
 * Issues list screen displaying all reported issues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesListScreen(
    onIssueClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: IssueViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val issueCounts by viewModel.issueCounts.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Update issues data when entering the screen (background refresh if already loaded)
    LaunchedEffect(Unit) {
        viewModel.loadIssues()
        viewModel.loadIssueCounts()
    }
    
    val filterMap = mapOf("Open" to "open", "All" to "all", "Resolved" to "resolved")
    val inverseFilterMap = filterMap.entries.associate { it.value to it.key }
    val tabTitles = listOf("Open", "All", "Resolved")
    val selectedTabIndex = tabTitles.indexOf(inverseFilterMap[selectedFilter] ?: "Open")

    val gradients = LocalUnderseerrGradients.current
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Issues", color = gradients.onAppBar) },
                actions = {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    val tabs = listOf("Open", "All", "Resolved")
                    
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = if (selectedFilter != "open") MaterialTheme.colorScheme.primary else gradients.onAppBar
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            tabs.forEach { title ->
                                val filter = filterMap[title] ?: "all"
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            title,
                                            fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.loadIssues(filter)
                                        showFilterMenu = false
                                    },
                                    trailingIcon = {
                                        if (filter == selectedFilter) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = gradients.onAppBar)
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
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val hasCachedData = uiState is IssueListState.Success
                val isOffline = error != null
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Offline Banner pushes content down
                    app.lusk.underseerr.ui.components.OfflineBanner(
                        visible = isOffline && hasCachedData
                    )

                    // Issue counts summary (only if we have data or it's just a background refresh failure)
                    val counts = issueCounts
                    if (counts != null) {
                        IssueCountsCard(
                            counts = counts,
                            onCountClick = { filter ->
                                viewModel.loadIssues(filter)
                            }
                        )
                    }
                    
                    // Content
                    Box(modifier = Modifier.weight(1f)) {
                        when (val state = uiState) {
                            is IssueListState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is IssueListState.Empty -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    EmptyIssuesDisplay(
                                        filter = tabTitles[selectedTabIndex]
                                    )
                                }
                            }
                            is IssueListState.Success -> {
                                IssuesList(
                                    issues = state.issues,
                                    onIssueClick = onIssueClick,
                                    onResolve = { viewModel.resolveIssue(it) },
                                    onReopen = { viewModel.reopenIssue(it) },
                                    onDelete = { viewModel.deleteIssue(it) },
                                    // Add top padding if banner is visible is tricky here because banner is overlay
                                    // But list has its own padding.
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            is IssueListState.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    app.lusk.underseerr.ui.components.UnifiedErrorDisplay(
                                        message = state.message,
                                        onRetry = { viewModel.refresh() }
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

@Composable
private fun IssueCountsCard(
    counts: IssueCount,
    onCountClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradients = LocalUnderseerrGradients.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IssueCountItem(
            icon = Icons.Default.Warning,
            count = counts.open,
            label = "Open",
            color = MaterialTheme.colorScheme.error,
            onClick = { onCountClick("open") },
            modifier = Modifier.weight(1f),
            backgroundBrush = gradients.tertiary
        )
        IssueCountItem(
            icon = Icons.Default.CheckCircle,
            count = counts.closed,
            label = "Resolved",
            color = MaterialTheme.colorScheme.tertiary,
            onClick = { onCountClick("resolved") },
            modifier = Modifier.weight(1f),
            backgroundBrush = gradients.secondary
        )
        IssueCountItem(
            icon = Icons.AutoMirrored.Filled.List,
            count = counts.total,
            label = "Total",
            color = MaterialTheme.colorScheme.primary,
            onClick = { onCountClick("all") },
            modifier = Modifier.weight(1f),
            backgroundBrush = gradients.primary
        )
    }
}

@Composable
private fun IssueCountItem(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundBrush: Brush? = null
) {
    val gradients = LocalUnderseerrGradients.current
    Card(
        modifier = modifier
            .clip(gradients.statusBadgeShape)
            .clickable(onClick = onClick)
            .then(
                if (backgroundBrush != null && gradients.isVibrant) Modifier.background(backgroundBrush, shape = gradients.statusBadgeShape)
                else Modifier
            ),
        shape = gradients.statusBadgeShape,
        colors = CardDefaults.cardColors(
            containerColor = if (backgroundBrush != null && gradients.isVibrant) Color.Transparent else color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (backgroundBrush != null && gradients.isVibrant) Color.White else color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (backgroundBrush != null && gradients.isVibrant) Color.White else color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (backgroundBrush != null && gradients.isVibrant) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IssuesList(
    issues: List<Issue>,
    onIssueClick: (Int) -> Unit,
    onResolve: (Int) -> Unit,
    onReopen: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(issues, key = { it.id }) { issue ->
            IssueItem(
                issue = issue,
                onClick = { onIssueClick(issue.id) },
                onResolve = { onResolve(issue.id) },
                onReopen = { onReopen(issue.id) },
                onDelete = { onDelete(issue.id) }
            )
        }
    }
}

@Composable
private fun IssueItem(
    issue: Issue,
    onClick: () -> Unit,
    onResolve: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val gradients = LocalUnderseerrGradients.current
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
                // Poster with type badge
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(100.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PosterImage(
                            posterPath = issue.mediaPosterPath,
                            title = issue.mediaTitle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Issue type icon badge on posters
                    IssueTypeIcon(
                        issueType = issue.issueType,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(24.dp)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = issue.mediaTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Text(
                                    text = "${issue.issueType.displayName} Issue",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (issue.problemSeason != null) {
                                    Text(
                                        text = buildString {
                                            append("Season ${issue.problemSeason}")
                                            if (issue.problemEpisode != null) {
                                                append(", Episode ${issue.problemEpisode}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Actions menu
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (issue.status == IssueStatus.OPEN) {
                                        DropdownMenuItem(
                                            text = { Text("Resolve") },
                                            onClick = {
                                                showMenu = false
                                                onResolve()
                                            },
                                            leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Reopen") },
                                            onClick = {
                                                showMenu = false
                                                onReopen()
                                            },
                                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Larger colorful status badge
                        IssueStatusBadge(status = issue.status)
                        
                        // Comment count
                        if (issue.comments.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = issue.comments.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueTypeIcon(
    issueType: IssueType,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (issueType) {
        IssueType.VIDEO -> Icons.Default.Videocam to Color(0xFFE91E63) // Pink/Red
        IssueType.AUDIO -> Icons.AutoMirrored.Filled.VolumeUp to Color(0xFF2196F3) // Blue
        IssueType.SUBTITLES -> Icons.Default.Subtitles to Color(0xFFFFC107) // Amber
        IssueType.OTHER -> Icons.AutoMirrored.Filled.HelpOutline to Color(0xFF9C27B0) // Purple
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = issueType.displayName,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun IssueStatusBadge(
    status: IssueStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (status) {
        IssueStatus.OPEN -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "Open"
        )
        IssueStatus.RESOLVED -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "Resolved"
        )
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
private fun EmptyIssuesDisplay(
    filter: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (filter) {
                "Open" -> "No open issues"
                "Resolved" -> "No resolved issues"
                else -> "No issues found"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (filter) {
                "Open" -> "All issues have been resolved!"
                "Resolved" -> "No issues have been resolved yet"
                else -> "Issues reported for media will appear here"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
