package app.lusk.client.presentation.issue

import androidx.compose.foundation.background
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
import app.lusk.client.domain.model.Issue
import app.lusk.client.domain.model.IssueCount
import app.lusk.client.domain.model.IssueStatus
import app.lusk.client.domain.model.IssueType
import app.lusk.client.ui.components.AsyncImage
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
    
    val tabs = listOf("Open", "All", "Resolved")
    val filterMap = mapOf("Open" to "open", "All" to "all", "Resolved" to "resolved")
    var selectedTab by remember { mutableStateOf(0) }
    
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Issues") },
                actions = {
                    // Filter button
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                "Filter",
                                tint = if (selectedTab != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            title,
                                            fontWeight = if (index == selectedTab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedTab = index
                                        viewModel.loadIssues(filterMap[title] ?: "open")
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Issue counts summary
                issueCounts?.let { counts ->
                    IssueCountsCard(counts = counts)
                }
                
                // Content
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is IssueListState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is IssueListState.Empty -> {
                            EmptyIssuesDisplay(
                                filter = tabs[selectedTab],
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is IssueListState.Success -> {
                            IssuesList(
                                issues = state.issues,
                                onIssueClick = onIssueClick,
                                onResolve = { viewModel.resolveIssue(it) },
                                onReopen = { viewModel.reopenIssue(it) },
                                onDelete = { viewModel.deleteIssue(it) }
                            )
                        }
                        is IssueListState.Error -> {
                            app.lusk.client.ui.components.ErrorState(
                                message = state.message,
                                onRetry = { viewModel.refresh() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
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
private fun IssueCountsCard(
    counts: IssueCount,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IssueCountItem(
                icon = Icons.Default.Warning,
                count = counts.open,
                label = "Open",
                color = MaterialTheme.colorScheme.error
            )
            IssueCountItem(
                icon = Icons.Default.CheckCircle,
                count = counts.closed,
                label = "Resolved",
                color = MaterialTheme.colorScheme.tertiary
            )
            IssueCountItem(
                icon = Icons.AutoMirrored.Filled.List,
                count = counts.total,
                label = "Total",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun IssueCountItem(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
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
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Issue type icon
            IssueTypeIcon(
                issueType = issue.issueType,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Issue details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = issue.mediaTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IssueStatusChip(status = issue.status)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${issue.issueType.displayName} Issue",
                    style = MaterialTheme.typography.bodyMedium,
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User info
                    if (issue.createdByAvatar != null) {
                        AsyncImage(
                            imageUrl = issue.createdByAvatar,
                            contentDescription = "User avatar",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = issue.createdByName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Comment count
                    if (issue.comments.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = issue.comments.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
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
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, null)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Reopen") },
                            onClick = {
                                showMenu = false
                                onReopen()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
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
        IssueType.VIDEO -> Icons.Default.Videocam to MaterialTheme.colorScheme.primary
        IssueType.AUDIO -> Icons.AutoMirrored.Filled.VolumeUp to MaterialTheme.colorScheme.secondary
        IssueType.SUBTITLES -> Icons.Default.Subtitles to MaterialTheme.colorScheme.tertiary
        IssueType.OTHER -> Icons.AutoMirrored.Filled.HelpOutline to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier.size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = issueType.displayName,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun IssueStatusChip(
    status: IssueStatus,
    modifier: Modifier = Modifier
) {
    val (text, containerColor, contentColor) = when (status) {
        IssueStatus.OPEN -> Triple(
            "Open",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        IssueStatus.RESOLVED -> Triple(
            "Resolved",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
