package app.lusk.underseerr.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.presentation.auth.AuthState
import app.lusk.underseerr.presentation.auth.AuthViewModel
import app.lusk.underseerr.ui.components.ErrorState
import app.lusk.underseerr.ui.components.LoadingState
import app.lusk.underseerr.presentation.settings.SettingsViewModel
import app.lusk.underseerr.domain.repository.ThemePreference
import app.lusk.underseerr.domain.repository.NotificationSettings
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush

/**
 * Masks an email address for privacy display.
 * Example: "example@email.com" -> "ex*****@e****.com"
 */
private fun maskEmail(email: String): String {
    if (!email.contains("@")) return email
    
    val parts = email.split("@")
    if (parts.size != 2) return email
    
    val localPart = parts[0]
    val domainPart = parts[1]
    
    // Mask local part: show first 2 chars, then asterisks
    val maskedLocal = if (localPart.length <= 2) {
        localPart
    } else {
        localPart.take(2) + "*".repeat(minOf(5, localPart.length - 2))
    }
    
    // Mask domain: show first char, asterisks, then TLD
    val domainParts = domainPart.split(".")
    val maskedDomain = if (domainParts.size >= 2) {
        val mainDomain = domainParts.dropLast(1).joinToString(".")
        val tld = domainParts.last()
        val maskedMain = if (mainDomain.isNotEmpty()) {
            mainDomain.first() + "*".repeat(minOf(4, mainDomain.length - 1))
        } else {
            mainDomain
        }
        "$maskedMain.$tld"
    } else {
        domainPart
    }
    
    return "$maskedLocal@$maskedDomain"
}

/**
 * Profile screen displaying user information, quota, and statistics.
 * Refactored for KMP in commonMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToRequests: (String?) -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val quota by viewModel.quota.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshingStats by viewModel.isRefreshingStats.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val authState by authViewModel.authState.collectAsState()
    val notificationSettings by settingsViewModel.notificationSettings.collectAsState()
    val themePreference by settingsViewModel.themePreference.collectAsState()

    // Handle logout navigation
    androidx.compose.runtime.LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            onLogout()
        }
    }
    
    var pullRefreshing by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    val hasData = profile != null && quota != null && statistics != null
                    IconButton(onClick = { 
                        if (hasData) {
                            viewModel.refreshStatistics()
                        } else {
                            viewModel.refresh()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
        ) { paddingValues ->
            val hasData = profile != null && quota != null && statistics != null
            val isOffline = error != null && hasData

            Box(modifier = Modifier.fillMaxSize()) {
                // Content Layer
                PullToRefreshBox(
                    isRefreshing = isLoading && pullRefreshing,
                    onRefresh = {
                        pullRefreshing = true
                        if (hasData) {
                            viewModel.refreshStatistics()
                        } else {
                            viewModel.refresh()
                        }
                        pullRefreshing = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = if (isOffline) 48.dp else 16.dp, bottom = 100.dp)
                    ) {
                        when {
                            isLoading && !hasData -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingState()
                                }
                            }
                            
                            error != null && !hasData -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(400.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        ErrorState(
                                            message = error!!,
                                            onRetry = { viewModel.refresh() },
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                        Button(onClick = onNavigateToSettings) {
                                            Text("Open Settings")
                                        }
                                    }
                                }
                            }
                            
                            hasData -> {
                                ProfileContent(
                                    profile = profile!!,
                                    quota = quota!!,
                                    statistics = statistics!!,
                                    isRefreshingStats = isRefreshingStats,
                                    notificationSettings = notificationSettings,
                                    themePreference = themePreference,
                                    onUpdateTheme = { settingsViewModel.updateTheme(it) },
                                    onUpdateNotificationSettings = { settingsViewModel.updateNotificationSettings(it) },
                                    onNotificationsClick = { showNotificationsDialog = true },
                                    onLanguageClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Help wanted!")
                                        }
                                    },
                                    onNavigateToSettings = onNavigateToSettings,
                                    onNavigateToAbout = onNavigateToAbout,
                                    onNavigateToRequests = onNavigateToRequests,
                                    onLogout = { authViewModel.logout() }
                                )
                            }
                        }
                    }

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

                // Banner Layer
                app.lusk.underseerr.ui.components.OfflineBanner(
                    visible = isOffline,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = paddingValues.calculateTopPadding())
                )
            }
        }
    
    // Notifications Dialog
    if (showNotificationsDialog) {
        NotificationsDialog(
            settings = notificationSettings,
            onUpdate = { settingsViewModel.updateNotificationSettings(it) },
            onDismiss = { showNotificationsDialog = false }
        )
    }
}

@Composable
private fun ProfileContent(
    profile: app.lusk.underseerr.domain.model.UserProfile,
    quota: app.lusk.underseerr.domain.repository.RequestQuota,
    statistics: app.lusk.underseerr.domain.model.UserStatistics,
    isRefreshingStats: Boolean,
    notificationSettings: NotificationSettings,
    themePreference: ThemePreference,
    onUpdateTheme: (ThemePreference) -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onNotificationsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToRequests: (String?) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageHelp by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        // User Info Card - Horizontal layout like the mockup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar on the left
                if (profile.avatar != null) {
                    AsyncImage(
                        imageUrl = profile.avatar,
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default avatar",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // User info on the right
                Column {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = maskEmail(profile.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Admin", // TODO: Get actual role
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Statistics Cards Row - Clickable to navigate to Requests with filter
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Schedule,
                    value = statistics.totalRequests.toString(),
                    label = "Requests",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateToRequests(null) }, // All requests
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.CheckCircle,
                    value = statistics.availableRequests.toString(),
                    label = "Available",
                    color = Color(0xFF4CAF50),
                    onClick = { onNavigateToRequests("Available") },
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Pending,
                    value = statistics.pendingRequests.toString(),
                    label = "Pending",
                    color = Color(0xFFFF9800),
                    onClick = { onNavigateToRequests("Pending") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (isRefreshingStats) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp)
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Request Quota Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Request Quota",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Movie Quota
                QuotaRow(
                    label = "Movies",
                    remaining = quota.movieRemaining,
                    limit = quota.movieLimit,
                    days = quota.movieDays
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // TV Quota
                QuotaRow(
                    label = "TV Shows",
                    remaining = quota.tvRemaining,
                    limit = quota.tvLimit,
                    days = quota.tvDays
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Settings Section
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column {
                // Notifications - clickable row that opens dialog
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { onNotificationsClick() },
                    trailing = {
                        Switch(
                            checked = notificationSettings.enabled,
                            onCheckedChange = { enabled ->
                                onUpdateNotificationSettings(
                                    notificationSettings.copy(enabled = enabled)
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Theme
                val currentTheme = when(themePreference) {
                    ThemePreference.LIGHT -> "Light"
                    ThemePreference.DARK -> "Dark"
                    ThemePreference.SYSTEM -> "System"
                    ThemePreference.VIBRANT -> "Vibrant"
                }

                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = currentTheme,
                    onClick = { showThemeDialog = true }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Language
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English",
                    onClick = {
                         // TODO: Use callback to show snackbar in parent
                         onLanguageClick()
                    }
                )

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Select Theme") },
                        text = {
                            Column {
                                ThemePreference.entries.forEach { theme ->
                                    val title = when (theme) {
                                        ThemePreference.LIGHT -> "Light"
                                        ThemePreference.DARK -> "Dark"
                                        ThemePreference.SYSTEM -> "System"
                                        ThemePreference.VIBRANT -> "Vibrant"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onUpdateTheme(theme)
                                                showThemeDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = theme == themePreference,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = title)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showLanguageHelp) {
                    AlertDialog(
                        onDismissRequest = { showLanguageHelp = false },
                        title = { Text("Language") },
                        text = { Text("Help wanted") },
                        confirmButton = {
                            TextButton(onClick = { showLanguageHelp = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // More Settings (Default Profiles, Server Management, etc.)
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "More Settings",
                    subtitle = "Default profiles, server management",
                    onClick = onNavigateToSettings
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // About
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "About",
                    onClick = onNavigateToAbout
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Sign Out
                SettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Sign Out",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
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
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
private fun NotificationsDialog(
    settings: NotificationSettings,
    onUpdate: (NotificationSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Request Approved", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Notify when request is approved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.requestApproved,
                        onCheckedChange = { 
                            onUpdate(settings.copy(requestApproved = it)) 
                        }
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Media Available", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Notify when media is available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.requestAvailable,
                        onCheckedChange = { 
                            onUpdate(settings.copy(requestAvailable = it)) 
                        }
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Request Declined", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Notify when request is declined",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.requestDeclined,
                        onCheckedChange = { 
                            onUpdate(settings.copy(requestDeclined = it)) 
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun QuotaRow(
    label: String,
    remaining: Int?,
    limit: Int?,
    days: Int?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (limit != null && remaining != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$remaining / $limit",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                if (days != null) {
                    Text(
                        text = "per $days days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Unlimited",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
