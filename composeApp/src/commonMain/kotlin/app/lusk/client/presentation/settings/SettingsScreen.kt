package app.lusk.client.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.client.domain.repository.NotificationSettings
import app.lusk.client.domain.repository.ThemePreference

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
    val defaultQualityProfile by viewModel.defaultQualityProfile.collectAsState()
    val currentServerUrl by viewModel.currentServerUrl.collectAsState()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityProfileDialog by remember { mutableStateOf(false) }
    
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
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
            
            Divider()
            
            // Notifications Section
            SettingsSectionHeader(title = "Notifications")
            
            SettingsSwitchItem(
                title = "Enable Notifications",
                subtitle = "Receive push notifications",
                checked = notificationSettings.enabled,
                onCheckedChange = { enabled ->
                    viewModel.updateNotificationSettings(
                        notificationSettings.copy(enabled = enabled)
                    )
                }
            )
            
            if (notificationSettings.enabled) {
                SettingsSwitchItem(
                    title = "Request Approved",
                    subtitle = "Notify when request is approved",
                    checked = notificationSettings.requestApproved,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(
                            notificationSettings.copy(requestApproved = checked)
                        )
                    }
                )
                
                SettingsSwitchItem(
                    title = "Request Available",
                    subtitle = "Notify when media becomes available",
                    checked = notificationSettings.requestAvailable,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(
                            notificationSettings.copy(requestAvailable = checked)
                        )
                    }
                )
                
                SettingsSwitchItem(
                    title = "Request Declined",
                    subtitle = "Notify when request is declined",
                    checked = notificationSettings.requestDeclined,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(
                            notificationSettings.copy(requestDeclined = checked)
                        )
                    }
                )
            }
            
            Divider()
            
            // Security Section
            SettingsSectionHeader(title = "Security")
            
            SettingsSwitchItem(
                title = "Biometric Authentication",
                subtitle = "Require fingerprint or face unlock",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) }
            )
            
            Divider()
            
            // Requests Section
            SettingsSectionHeader(title = "Requests")
            
            SettingsItem(
                title = "Default Quality Profile",
                subtitle = defaultQualityProfile?.toString() ?: "Not set",
                onClick = { showQualityProfileDialog = true }
            )
            
            Divider()
            
            // Server Section
            SettingsSectionHeader(title = "Server")
            
            SettingsItem(
                title = "Manage Servers",
                subtitle = currentServerUrl ?: "No server configured",
                onClick = onNavigateToServerManagement
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
    
    // Quality Profile Dialog
    if (showQualityProfileDialog) {
        QualityProfileDialog(
            currentProfile = defaultQualityProfile,
            onProfileSelected = { profile ->
                viewModel.setDefaultQualityProfile(profile)
                showQualityProfileDialog = false
            },
            onDismiss = { showQualityProfileDialog = false }
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
    onCheckedChange: (Boolean) -> Unit
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
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
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
    currentProfile: Int?,
    onProfileSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Simplified quality profile selection
    // In a real app, this would fetch available profiles from the server
    val profiles = listOf(1, 2, 3, 4)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality Profile") },
        text = {
            Column {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(profile) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Profile $profile")
                        if (profile == currentProfile) {
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
