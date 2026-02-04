package app.lusk.underseerr.presentation.issue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueComment
import app.lusk.underseerr.domain.model.IssueStatus
import app.lusk.underseerr.domain.model.IssueType
import app.lusk.underseerr.domain.repository.IssueRepository
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.ui.components.UnifiedErrorDisplay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Issue details screen showing issue information and comments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailsScreen(
    issueId: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    issueRepository: IssueRepository = koinInject()
) {
    var issue by remember { mutableStateOf<Issue?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newComment by remember { mutableStateOf("") }
    var isSendingComment by remember { mutableStateOf(false) }
    var editingComment by remember { mutableStateOf<IssueComment?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load issue details
    LaunchedEffect(issueId) {
        isLoading = true
        error = null
        issueRepository.getIssue(issueId)
            .onSuccess { 
                issue = it
                isLoading = false
            }
            .onError { 
                error = it.message
                isLoading = false
            }
    }
    
    Scaffold(
        topBar = {
            val gradients = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current
            TopAppBar(
                title = { Text("Issue Details", color = gradients.onAppBar) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = gradients.onAppBar)
                    }
                },
                actions = {
                    issue?.let { currentIssue ->
                        var showMenu by remember { mutableStateOf(false) }
                        
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More options", tint = gradients.onAppBar)
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (currentIssue.status == IssueStatus.OPEN) {
                                DropdownMenuItem(
                                    text = { Text("Resolve Issue") },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            issueRepository.resolveIssue(issueId)
                                                .onSuccess { updated ->
                                                    issue = updated
                                                    snackbarHostState.showSnackbar("Issue resolved")
                                                }
                                                .onError {
                                                    snackbarHostState.showSnackbar("Failed to resolve issue: ${it.message}")
                                                }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, null)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Reopen Issue") },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            issueRepository.reopenIssue(issueId)
                                                .onSuccess { updated ->
                                                    issue = updated
                                                    snackbarHostState.showSnackbar("Issue reopened")
                                                }
                                                .onError {
                                                    snackbarHostState.showSnackbar("Failed to reopen issue: ${it.message}")
                                                }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, null)
                                    }
                                )
                            }
                        }
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
        bottomBar = {
            // Comment input bar
            issue?.let {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            placeholder = { Text("Add a comment...") },
                            modifier = Modifier.weight(1f),
                            singleLine = false,
                            maxLines = 3,
                            enabled = !isSendingComment
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    scope.launch {
                                        isSendingComment = true
                                        issueRepository.addComment(issueId, newComment)
                                            .onSuccess { updated ->
                                                issue = updated
                                                newComment = ""
                                                snackbarHostState.showSnackbar("Comment added")
                                            }
                                            .onError {
                                                snackbarHostState.showSnackbar("Failed to add comment: ${it.message}")
                                            }
                                        isSendingComment = false
                                    }
                                }
                            },
                            enabled = newComment.isNotBlank() && !isSendingComment
                        ) {
                            if (isSendingComment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send comment",
                                    tint = if (newComment.isNotBlank()) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        val gradients = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current
        Box(modifier = Modifier.fillMaxSize().background(gradients.issueDetails)) {
        var pullRefreshing by remember { mutableStateOf(false) }
        
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                scope.launch {
                    issueRepository.getIssue(issueId)
                        .onSuccess { 
                            issue = it
                        }
                        .onError { /* Error already handled */ }
                    pullRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    UnifiedErrorDisplay(
                        message = error!!,
                        onRetry = {
                            scope.launch {
                                isLoading = true
                                error = null
                                issueRepository.getIssue(issueId)
                                    .onSuccess { 
                                        issue = it
                                        isLoading = false
                                    }
                                    .onError { 
                                        error = it.message
                                        isLoading = false
                                    }
                            }
                        }
                    )
                }
                issue != null -> {
                    IssueDetailsContent(
                        issue = issue!!,
                        onEditComment = { comment -> editingComment = comment }
                    )
                }
            }
            
            if (editingComment != null) {
                EditCommentDialog(
                    initialMessage = editingComment!!.message,
                    onDismiss = { editingComment = null },
                    onSubmit = { newMessage ->
                        scope.launch {
                        issueRepository.updateComment(editingComment!!.id, newMessage)
                            .onSuccess {
                                editingComment = null
                                snackbarHostState.showSnackbar("Comment updated")
                                // Reload issue to reflect changes
                                scope.launch {
                                    issueRepository.getIssue(issueId).onSuccess { issue = it }
                                }
                            }
                            .onError {
                                snackbarHostState.showSnackbar("Failed to update comment: ${it.message}")
                            }
                        }
                    }
                )
            }
        }
    }
}
}

@Composable
private fun IssueDetailsContent(
    issue: Issue,
    onEditComment: (IssueComment) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Issue header
        item {
            IssueHeaderCard(issue = issue)
        }
        
        // Comments section header
        item {
            Text(
                text = "Comments (${issue.comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Comments
        if (issue.comments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No comments yet. Be the first to comment!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(issue.comments, key = { it.id }) { comment ->
                CommentCard(
                    comment = comment,
                    onEditClick = { onEditComment(comment) }
                )
            }
        }
        
        // Bottom spacer for comment input
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IssueHeaderCard(
    issue: Issue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Status and type row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IssueTypeChip(issueType = issue.issueType)
                IssueStatusChip(status = issue.status)
            }
            
            // Media title
            Text(
                text = issue.mediaTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current.onIssueDetails
            )
            
            // Season/Episode info
            if (issue.problemSeason != null) {
                Text(
                    text = buildString {
                        append("Season ${issue.problemSeason}")
                        if (issue.problemEpisode != null) {
                            append(", Episode ${issue.problemEpisode}")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider()
            
            // Reporter info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (issue.createdByAvatar != null) {
                    AsyncImage(
                        imageUrl = issue.createdByAvatar,
                        contentDescription = "Reporter avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Column {
                    Text(
                        text = "Reported by ${issue.createdByName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    issue.createdAt?.let { createdAt ->
                        Text(
                            text = createdAt.take(10), // Simple date display
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueTypeChip(
    issueType: IssueType,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (issueType) {
        IssueType.VIDEO -> Icons.Default.Videocam to "Video"
        IssueType.AUDIO -> Icons.Default.MusicNote to "Audio"
        IssueType.SUBTITLES -> Icons.Default.Subtitles to "Subtitles"
        IssueType.OTHER -> Icons.AutoMirrored.Filled.Help to "Other"
    }
    
    AssistChip(
        onClick = { },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
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
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CommentCard(
    comment: IssueComment,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // User info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comment.userAvatar != null) {
                    AsyncImage(
                        imageUrl = comment.userAvatar,
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = comment.userName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                comment.createdAt?.let { createdAt ->
                    Text(
                        text = createdAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Comment message
            Text(
                text = comment.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
