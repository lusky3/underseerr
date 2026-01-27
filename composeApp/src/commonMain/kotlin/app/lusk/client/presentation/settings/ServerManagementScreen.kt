package app.lusk.client.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import app.lusk.client.domain.repository.ServerConfig

/**
 * Server management screen for multi-server configuration.
 * Refactored for KMP in commonMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val configuredServers by viewModel.configuredServers.collectAsState()
    val currentServerUrl by viewModel.currentServerUrl.collectAsState()
    
    var showAddServerDialog by remember { mutableStateOf(false) }
    var serverToDelete by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Servers") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddServerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Server"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (configuredServers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No servers configured",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showAddServerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Server")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(configuredServers) { server ->
                    val isPrimary = server.url == currentServerUrl
                    val isOnlyServer = configuredServers.size <= 1
                    
                    ServerItem(
                        server = server,
                        isActive = isPrimary,
                        canDelete = !isPrimary && !isOnlyServer,
                        onServerClick = {
                            if (!isPrimary) {
                                viewModel.switchServer(server.url)
                            }
                        },
                        onDeleteClick = { serverToDelete = server.url }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    
    // Add Server Dialog
    if (showAddServerDialog) {
        AddServerDialog(
            onServerAdded = { config ->
                viewModel.addServer(config)
                showAddServerDialog = false
            },
            onDismiss = { showAddServerDialog = false }
        )
    }
    
    // Delete Confirmation Dialog
    serverToDelete?.let { url ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server") },
            text = { Text("Are you sure you want to remove this server configuration?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeServer(url)
                        serverToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServerItem(
    server: ServerConfig,
    isActive: Boolean,
    canDelete: Boolean,
    onServerClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onServerClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = server.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active server",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDeleteClick,
                enabled = canDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete server",
                    tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun AddServerDialog(
    onServerAdded: (ServerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var serverName by remember { mutableStateOf(TextFieldValue("")) }
    var serverUrl by remember { mutableStateOf(TextFieldValue("")) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Server") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = {
                        serverName = it
                        nameError = null
                    },
                    label = { Text("Server Name") },
                    placeholder = { Text("My Overseerr Server") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        urlError = null
                    },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://overseerr.example.com") },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var hasError = false
                    
                    if (serverName.text.isBlank()) {
                        nameError = "Server name is required"
                        hasError = true
                    }
                    
                    if (serverUrl.text.isBlank()) {
                        urlError = "Server URL is required"
                        hasError = true
                    } else if (!serverUrl.text.startsWith("http://") && 
                               !serverUrl.text.startsWith("https://")) {
                        urlError = "URL must start with http:// or https://"
                        hasError = true
                    } else if (serverUrl.text.contains(" ")) {
                        urlError = "URL must not contain spaces"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        onServerAdded(
                            ServerConfig(
                                url = serverUrl.text.trim(),
                                name = serverName.text.trim(),
                                isActive = false
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
