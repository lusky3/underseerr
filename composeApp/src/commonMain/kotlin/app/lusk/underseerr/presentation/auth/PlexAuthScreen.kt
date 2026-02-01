package app.lusk.underseerr.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons

/**
 * Plex authentication screen with OAuth flow.
 * Feature: underseerr
 * Validates: Requirements 1.1, 1.3, 1.4
 * Property 28: Koin Dependency Injection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlexAuthScreen(
    onBackClick: () -> Unit,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    
    // Handle WaitingForPlex - open browser and start polling
    val waitingForPlex = authState as? AuthState.WaitingForPlex
    LaunchedEffect(waitingForPlex) {
        waitingForPlex?.let { state ->
            // Open browser for Plex login
            try {
                uriHandler.openUri(state.authUrl)
            } catch (e: Exception) {
                // Ignore browser open errors
            }
            
            // Start polling for status
            while (viewModel.authState.value is AuthState.WaitingForPlex) {
                viewModel.checkPlexStatus(state.pinId)
                delay(3000) // Poll every 3 seconds
            }
        }
    }
    
    // Auto-check on Resume
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (viewModel.authState.value is AuthState.WaitingForPlex) {
             val state = viewModel.authState.value as AuthState.WaitingForPlex
             viewModel.checkPlexStatus(state.pinId)
        }
    }
    
    
    // Handle success/error states
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                onAuthSuccess()
            }
            is AuthState.Error -> {
                onAuthError((authState as AuthState.Error).message)
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authenticate with Plex") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetAuth()
                        onBackClick()
                    }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (authState) {
                is AuthState.AuthenticatingWithPlex -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Opening Plex authentication...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Please complete authentication in your browser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                is AuthState.WaitingForPlex -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Waiting for Plex authentication...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Please complete the login in the browser window that just opened.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { uriHandler.openUri(authState.let { (it as? AuthState.WaitingForPlex)?.authUrl ?: "" }) }) {
                        Text("Re-open Browser")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val isChecking by viewModel.isCheckingStatus.collectAsState()
                    OutlinedButton(
                        onClick = { 
                            val state = authState
                            if (state is AuthState.WaitingForPlex) {
                                viewModel.checkPlexStatus(state.pinId)
                            }
                        },
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking...")
                        } else {
                            Text("Check Status")
                        }
                    }
                }
                
                is AuthState.ExchangingToken -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Exchanging token...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Connecting to Overseerr server",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                else -> {
                    Text(
                        text = "Sign in with Plex",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Use your Plex account to access Overseerr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    
                    Button(
                        onClick = { viewModel.initiatePlexAuth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Plex")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You'll be redirected to Plex to authorize this app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
