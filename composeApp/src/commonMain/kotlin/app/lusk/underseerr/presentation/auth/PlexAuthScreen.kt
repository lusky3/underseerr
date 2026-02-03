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
    
    // State for local auth forms - Hoisted to persist across AuthState changes
    var showLocalLoginForm by remember { mutableStateOf(false) }
    var showApiKeyForm by remember { mutableStateOf(false) }
    
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
                
                is AuthState.Authenticating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Authenticating...",
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (authState is AuthState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    Text(
                        text = "Other Options",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (!showLocalLoginForm && !showApiKeyForm) {
                        OutlinedButton(
                            onClick = { showLocalLoginForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in with Local Account")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { showApiKeyForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in with API Key")
                        }
                    }
                    
                    if (showLocalLoginForm) {
                        LocalLoginForm(
                            onLogin = { email, password -> viewModel.loginLocal(email, password) },
                            onCancel = { showLocalLoginForm = false }
                        )
                    }
                    
                    if (showApiKeyForm) {
                        ApiKeyForm(
                            onLogin = { key -> viewModel.loginWithApiKey(key) },
                            onCancel = { showApiKeyForm = false }
                        )
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

@Composable
private fun LocalLoginForm(
    onLogin: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            )
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        onLogin(email, password)
                    }
                }
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.weight(1f),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Login")
            }
        }
    }
}

@Composable
private fun ApiKeyForm(
    onLogin: (String) -> Unit,
    onCancel: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Overseerr API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Go
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onGo = {
                    if (apiKey.isNotBlank()) {
                        onLogin(apiKey)
                    }
                }
            )
        )
        Text(
            text = "You can find your API key in Settings > General",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Note: API key sessions are temporary and not stored locally.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { onLogin(apiKey) },
                modifier = Modifier.weight(1f),
                enabled = apiKey.isNotBlank()
            ) {
                Text("Verify Key")
            }
        }
    }
}
