package app.lusk.underseerr.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.underseerr.domain.repository.NotificationSettings
import app.lusk.underseerr.domain.repository.ThemePreference
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Settings screen for app configuration.
 * Refactored for KMP in commonMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToServerManagement: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val defaultMovieProfile by viewModel.defaultMovieProfile.collectAsState()
    val defaultTvProfile by viewModel.defaultTvProfile.collectAsState()
    val movieProfiles by viewModel.movieProfiles.collectAsState()
    val tvProfiles by viewModel.tvProfiles.collectAsState()
    
    val currentServerUrl by viewModel.currentServerUrl.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showMovieProfileDialog by remember { mutableStateOf(false) }
    var showTvProfileDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            // Appearance Section
            SettingsSectionHeader(title = "Appearance")
            
            SettingsItem(
                title = "Theme",
                subtitle = when (themePreference) {
                    ThemePreference.LIGHT -> "Light"
                    ThemePreference.DARK -> "Dark"
                    ThemePreference.SYSTEM -> "System default"
                },
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider()
            
            // Notifications Section
            SettingsSectionHeader(title = "Notifications")
            
            val globalWebPushEnabled by viewModel.globalWebPushEnabled.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()

            if (!globalWebPushEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                         containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Web Push is globally disabled on the server. Contact your admin to enable it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            SettingsSwitchItem(
                title = "Enable Notifications",
                subtitle = "Receive push notifications",
                checked = notificationSettings.enabled,
                onCheckedChange = { enabled ->
                    viewModel.updateNotificationSettings(
                        notificationSettings.copy(enabled = enabled)
                    )
                },
                enabled = globalWebPushEnabled
            )

            SettingsSwitchItem(
                title = "Sync Notification Settings",
                subtitle = "Sync enabled types with Overseerr server. Disabling supports local-only filtering.",
                checked = notificationSettings.syncEnabled,
                onCheckedChange = { checked ->
                    viewModel.updateNotificationSettings(
                        notificationSettings.copy(syncEnabled = checked)
                    )
                },
                enabled = globalWebPushEnabled
            )
            
            if (notificationSettings.enabled && globalWebPushEnabled) {
                // Request Notifications
                SettingsSwitchItem(
                    title = "Request Approved",
                    subtitle = "Get notified when request is approved",
                    checked = notificationSettings.requestApproved,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestApproved = checked))
                    }
                )
                
                SettingsSwitchItem(
                    title = "Request Available",
                    subtitle = "Get notified when your media requests become available",
                    checked = notificationSettings.requestAvailable,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestAvailable = checked))
                    }
                )
                
                SettingsSwitchItem(
                    title = "Request Declined",
                    subtitle = "Get notified when request is declined",
                    checked = notificationSettings.requestDeclined,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestDeclined = checked))
                    }
                )

                // Advanced Request Notifications (Permission Based)
                val canManageRequests = viewModel.hasPermission(app.lusk.underseerr.domain.model.AppPermissions.MANAGE_REQUESTS)
                
                SettingsSwitchItem(
                    title = "Request Pending Approval",
                    subtitle = "Get notified when other users submit new media requests which require approval",
                    checked = notificationSettings.requestPendingApproval,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestPendingApproval = checked))
                    },
                    enabled = canManageRequests
                )

                SettingsSwitchItem(
                    title = "Request Automatically Approved",
                    subtitle = "Get notified when other users submit new media requests which are automatically approved",
                    checked = notificationSettings.requestAutoApproved,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestAutoApproved = checked))
                    },
                    enabled = canManageRequests
                )

                SettingsSwitchItem(
                    title = "Request Processing Failed",
                    subtitle = "Get notified when media requests fail to be added to Radarr or Sonarr",
                    checked = notificationSettings.requestProcessingFailed,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestProcessingFailed = checked))
                    },
                    enabled = canManageRequests
                )

                // Issue Notifications
                HorizontalDivider()
                SettingsSectionHeader(title = "Issue Notifications")

                val canManageIssues = viewModel.hasPermission(app.lusk.underseerr.domain.model.AppPermissions.MANAGE_ISSUES)

                SettingsSwitchItem(
                    title = "Issue Reported",
                    subtitle = "Get notified when other users report issues",
                    checked = notificationSettings.issueReported,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(issueReported = checked))
                    },
                    enabled = canManageIssues
                )

                SettingsSwitchItem(
                    title = "Issue Comment",
                    subtitle = "Get notified when other users comment on issues",
                    checked = notificationSettings.issueComment,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(issueComment = checked))
                    },
                    enabled = canManageIssues // Assuming permission needed
                )

                SettingsSwitchItem(
                    title = "Issue Resolved",
                    subtitle = "Get notified when issues are resolved by other users",
                    checked = notificationSettings.issueResolved,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(issueResolved = checked))
                    },
                    enabled = canManageIssues // Assuming permission needed
                )

                SettingsSwitchItem(
                    title = "Issue Reopened",
                    subtitle = "Get notified when issues are reopened by other users",
                    checked = notificationSettings.issueReopened,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(issueReopened = checked))
                    },
                    enabled = canManageIssues // Assuming permission needed
                )
            }
            
            HorizontalDivider()
            
            // Security Section
            SettingsSectionHeader(title = "Security")
            
            val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsState()

            SettingsSwitchItem(
                title = "Biometric Authentication",
                subtitle = if (isBiometricAvailable) "Require fingerprint or face unlock" else "Biometrics not available on this device",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                enabled = isBiometricAvailable
            )
            
            HorizontalDivider()
            
            // Requests Section
            SettingsSectionHeader(title = "Requests")
            
            SettingsItem(
                title = "Default Movie Profile",
                subtitle = defaultMovieProfile?.let { id -> 
                    movieProfiles.find { it.id == id }?.name
                } ?: "Use server setting",
                onClick = { showMovieProfileDialog = true }
            )

            SettingsItem(
                title = "Default TV Profile",
                subtitle = defaultTvProfile?.let { id -> 
                    tvProfiles.find { it.id == id }?.name
                } ?: "Use server setting",
                onClick = { showTvProfileDialog = true }
            )
            
            HorizontalDivider()
            
            // Server Section
            SettingsSectionHeader(title = "Server")
            
            SettingsItem(
                title = "Manage Servers",
                subtitle = currentServerUrl ?: "No server configured",
                onClick = onNavigateToServerManagement
            )
        }
        
        // Bottom fade gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
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
    
    // Theme Selection Dialog
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themePreference,
            onThemeSelected = { theme ->
                viewModel.setThemePreference(theme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // Movie Profile Dialog
    if (showMovieProfileDialog) {
        QualityProfileDialog(
            title = "Select Movie Profile",
            currentProfileId = defaultMovieProfile,
            profiles = movieProfiles,
            onProfileSelected = { profileId ->
                viewModel.setDefaultMovieProfile(profileId)
                showMovieProfileDialog = false
            },
            onDismiss = { showMovieProfileDialog = false }
        )
    }

    // TV Profile Dialog
    if (showTvProfileDialog) {
        QualityProfileDialog(
            title = "Select TV Profile",
            currentProfileId = defaultTvProfile,
            profiles = tvProfiles,
            onProfileSelected = { profileId ->
                viewModel.setDefaultTvProfile(profileId)
                showTvProfileDialog = false
            },
            onDismiss = { showTvProfileDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                ThemePreference.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (theme) {
                                ThemePreference.LIGHT -> "Light"
                                ThemePreference.DARK -> "Dark"
                                ThemePreference.SYSTEM -> "System default"
                            }
                        )
                        if (theme == currentTheme) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QualityProfileDialog(
    title: String,
    currentProfileId: Int?,
    profiles: List<app.lusk.underseerr.domain.repository.QualityProfile>,
    onProfileSelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                 modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // "Use server setting" option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileSelected(null) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Use server setting", style = MaterialTheme.typography.bodyLarge)
                    if (currentProfileId == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                if (profiles.isEmpty()) {
                     Text(
                        text = "No profiles found or loading...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(profile.id) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = profile.name, style = MaterialTheme.typography.bodyLarge)
                        if (profile.id == currentProfileId) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
