package app.lusk.underseerr.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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
    onNavigateToVibrantCustomization: () -> Unit,
    showPremiumPaywallOnStart: Boolean = false,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsState()
    
    val notificationServerType by viewModel.notificationServerType.collectAsState()
    val showTrialExpirationPopup by viewModel.showTrialExpirationPopup.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { message -> 
            snackbarHostState.showSnackbar(message)
        }
    }
    

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
    var showPaywallDialog by remember { mutableStateOf<String?>(null) }
    
    // Auto-open premium paywall if navigated from Profile for Vibrant theme
    LaunchedEffect(showPremiumPaywallOnStart) {
        if (showPremiumPaywallOnStart) {
            showPaywallDialog = "The Vibrant theme is a premium feature."
        }
    } // Context message
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val gradients = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current
            TopAppBar(
                title = { Text("Settings", color = gradients.onAppBar) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = gradients.onAppBar
                        )
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
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        val gradients = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current
        Box(modifier = Modifier.fillMaxSize().background(gradients.settings).padding(paddingValues)) {
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
                    ThemePreference.VIBRANT -> "Vibrant"
                },
                onClick = { showThemeDialog = true }
            )

            if (themePreference == ThemePreference.VIBRANT) {
                SettingsItem(
                    title = "Customize Vibrant Colors",
                    subtitle = "Adjust gradients and accents",
                    onClick = onNavigateToVibrantCustomization
                )
            }
            
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
                // Personal Request Notifications
                val isAdmin = viewModel.hasPermission(currentUser, app.lusk.underseerr.domain.model.AppPermissions.ADMIN)
                
                SettingsSwitchItem(
                    title = "Request Approved",
                    subtitle = if (isAdmin) "Get notified when requests are approved (including others)" else "Get notified when your request is approved",
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
                    subtitle = if (isAdmin) "Get notified when requests are declined (including others)" else "Get notified when your request is declined",
                    checked = notificationSettings.requestDeclined,
                    onCheckedChange = { checked ->
                        viewModel.updateNotificationSettings(notificationSettings.copy(requestDeclined = checked))
                    }
                )

                // Advanced Request Notifications (Permission Based)
                val canManageRequests = viewModel.hasPermission(currentUser, app.lusk.underseerr.domain.model.AppPermissions.MANAGE_REQUESTS)
                
                if (canManageRequests) {
                    SettingsSwitchItem(
                        title = "Request Pending Approval",
                        subtitle = "Get notified when other users submit new media requests which require approval",
                        checked = notificationSettings.requestPendingApproval,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(requestPendingApproval = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Request Automatically Approved",
                        subtitle = "Get notified when other users submit new media requests which are automatically approved",
                        checked = notificationSettings.requestAutoApproved,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(requestAutoApproved = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Media Auto-Requested",
                        subtitle = "Get notified when media is automatically requested by Overseerr (e.g. from watchlist sync)",
                        checked = notificationSettings.mediaAutoRequested,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(mediaAutoRequested = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Request Processing Failed",
                        subtitle = "Get notified when media requests fail to be added to Radarr or Sonarr",
                        checked = notificationSettings.requestProcessingFailed,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(requestProcessingFailed = checked))
                        }
                    )
                }

                // Issue Notifications
                val canManageIssues = viewModel.hasPermission(currentUser, app.lusk.underseerr.domain.model.AppPermissions.MANAGE_ISSUES)
                
                if (canManageIssues) {
                    HorizontalDivider()
                    SettingsSectionHeader(title = "Issue Notifications")

                    SettingsSwitchItem(
                        title = "Issue Reported",
                        subtitle = "Get notified when other users report issues",
                        checked = notificationSettings.issueReported,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(issueReported = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Issue Comment",
                        subtitle = "Get notified when other users comment on issues",
                        checked = notificationSettings.issueComment,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(issueComment = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Issue Resolved",
                        subtitle = "Get notified when issues are resolved by other users",
                        checked = notificationSettings.issueResolved,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(issueResolved = checked))
                        }
                    )

                    SettingsSwitchItem(
                        title = "Issue Reopened",
                        subtitle = "Get notified when issues are reopened by other users",
                        checked = notificationSettings.issueReopened,
                        onCheckedChange = { checked ->
                            viewModel.updateNotificationSettings(notificationSettings.copy(issueReopened = checked))
                        }
                    )
                }
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
            
            // Subscription Section
            SettingsSectionHeader(title = "Subscription")
            
            SettingsItem(
                title = "Subscription Status",
                subtitle = when (subscriptionStatus.tier) {
                    app.lusk.underseerr.domain.model.SubscriptionTier.PREMIUM -> "Premium Active"
                    app.lusk.underseerr.domain.model.SubscriptionTier.TRIAL -> "Trial Active (7 Days)"
                    app.lusk.underseerr.domain.model.SubscriptionTier.FREE -> "Free Version"
                },
                onClick = { viewModel.restorePurchases() }
            )

            HorizontalDivider()

            // Webhook Configuration (Auto-Setup)
            SettingsSectionHeader(title = "Advanced Integration")
            
            val isAdmin = viewModel.hasPermission(currentUser, app.lusk.underseerr.domain.model.AppPermissions.ADMIN)
            
            var showUrlDialog by remember { mutableStateOf(false) }
            val notificationServerUrl by viewModel.notificationServerUrl.collectAsState()

            if (showUrlDialog) {
                var isCustom by remember { mutableStateOf(notificationServerType == "CUSTOM") }
                var selectedType by remember { mutableStateOf(notificationServerType) }
                var urlInput by remember { mutableStateOf(notificationServerUrl ?: "") }
                
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Notification Server") },
                    text = {
                        Column {
                            // None option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedType = "NONE"
                                        isCustom = false 
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == "NONE",
                                    onClick = { 
                                        selectedType = "NONE"
                                        isCustom = false 
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("None")
                                    Text(
                                        "Disable push notifications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Hosted option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (subscriptionStatus.isPremium || subscriptionStatus.tier == app.lusk.underseerr.domain.model.SubscriptionTier.TRIAL) {
                                            selectedType = "HOSTED"
                                            isCustom = false 
                                        } else {
                                            showPaywallDialog = "Hosted notification server requires a subscription or trial."
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == "HOSTED",
                                    onClick = { 
                                        if (subscriptionStatus.isPremium || subscriptionStatus.tier == app.lusk.underseerr.domain.model.SubscriptionTier.TRIAL) {
                                            selectedType = "HOSTED"
                                            isCustom = false 
                                        } else {
                                            showPaywallDialog = "Hosted notification server requires a subscription or trial."
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Default (Hosted)")
                                        if (!subscriptionStatus.isPremium && subscriptionStatus.tier != app.lusk.underseerr.domain.model.SubscriptionTier.TRIAL) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Premium",
                                                modifier = Modifier.size(14.dp).padding(start = 4.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        "Secure relay for push notifications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Custom option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedType = "CUSTOM"
                                        isCustom = true 
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == "CUSTOM",
                                    onClick = { 
                                        selectedType = "CUSTOM"
                                        isCustom = true 
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Custom")
                                    Text(
                                        "Use your own Cloudflare Worker",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isCustom) {
                                OutlinedTextField(
                                    value = urlInput,
                                    onValueChange = { urlInput = it },
                                    label = { Text("https://...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.setNotificationServerType(selectedType)
                                if (selectedType == "CUSTOM" && urlInput.matches(Regex("^(https?://).+"))) {
                                    viewModel.setNotificationServerUrl(urlInput)
                                } else if (selectedType != "CUSTOM") {
                                    viewModel.setNotificationServerUrl("")
                                }
                                showUrlDialog = false
                            },
                            enabled = selectedType != "CUSTOM" || urlInput.matches(Regex("^(https?://).+"))
                        ) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
                    }
                )
            }

            SettingsItem(
                title = "Notification Server",
                subtitle = when (notificationServerType) {
                    "NONE" -> "None"
                    "HOSTED" -> "Default (Hosted)"
                    "CUSTOM" -> notificationServerUrl ?: "Custom URL"
                    else -> "Default (Hosted)"
                },
                onClick = { showUrlDialog = true }
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
            
            Spacer(modifier = Modifier.height(100.dp))
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
            subscriptionStatus = subscriptionStatus,
            onThemeSelected = { theme ->
                if (theme == ThemePreference.VIBRANT && !subscriptionStatus.isPremium) {
                    showPaywallDialog = "The Vibrant theme is a premium feature."
                } else {
                    viewModel.setThemePreference(theme)
                    showThemeDialog = false
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Paywall Dialog
    showPaywallDialog?.let { context ->
        SubscriptionPaywallDialog(
            message = context,
            onPurchase = { isYearly ->
                viewModel.purchasePremium(isYearly)
                showPaywallDialog = null
            },
            onUnlock = { key ->
                viewModel.unlockWithSerialKey(key)
                showPaywallDialog = null
            },
            onDismiss = { showPaywallDialog = null }
        )
    }

    // Trial Expiration Dialog
    if (showTrialExpirationPopup) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTrialPopup() },
            title = { Text("Trial Expired") },
            text = {
                Text("Your 7-day trial for the hosted notification server has expired. Please subscribe to continue using push notifications or host your own server.")
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.dismissTrialPopup()
                    showPaywallDialog = "Maintain your push notifications with Premium."
                }) {
                    Text("View Options")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTrialPopup() }) {
                    Text("Dismiss")
                }
            }
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
        color = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current.onSettings,
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
    subscriptionStatus: app.lusk.underseerr.domain.model.SubscriptionStatus,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (theme) {
                                ThemePreference.LIGHT -> "Light"
                                ThemePreference.DARK -> "Dark"
                                ThemePreference.SYSTEM -> "System default"
                                ThemePreference.VIBRANT -> "Vibrant"
                            }
                        )
                        // Show lock icon for Vibrant if not premium
                        if (theme == ThemePreference.VIBRANT && !subscriptionStatus.isPremium) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Premium",
                                modifier = Modifier.size(16.dp),
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

@Composable
private fun SubscriptionPaywallDialog(
    message: String,
    onPurchase: (isYearly: Boolean) -> Unit,
    onUnlock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var serialKey by remember { mutableStateOf("") }
    var showSerialInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Unlock Premium") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!showSerialInput) {
                    Text(
                        text = "Support development and unlock all features with a one-time purchase.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = serialKey,
                        onValueChange = { serialKey = it },
                        label = { Text("Enter Serial Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                TextButton(onClick = { showSerialInput = !showSerialInput }) {
                    Text(if (showSerialInput) "Back to purchase" else "Have a serial key?")
                }
            }
        },
        confirmButton = {
            if (!showSerialInput) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onPurchase(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Monthly Premium")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onPurchase(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Annual Premium (Best Value)")
                    }
                }
            } else {
                Button(
                    onClick = { onUnlock(serialKey) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serialKey.isNotBlank()
                ) {
                    Text("Unlock")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now")
            }
        }
    )
}
