package app.lusk.client.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.flow.first

/**
 * Server configuration screen for entering Overseerr server URL.
 * Feature: overseerr-android-client
 * Validates: Requirements 1.1, 1.2
 * Property 28: Koin Dependency Injection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(
    onServerValidated: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val serverValidationState by viewModel.serverValidationState.collectAsState()
    var serverUrl by remember { mutableStateOf("") }
    var allowHttp by remember { mutableStateOf(false) }
    
    // Prefill server URL if already stored
    LaunchedEffect(Unit) {
        viewModel.getServerUrl().first()?.let {
            serverUrl = it
        }
    }
    
    // Navigate when already authenticated (Session Persistence)
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }
    
    // Navigate when server is validated
    LaunchedEffect(serverValidationState) {
        if (serverValidationState is ServerValidationState.Valid) {
            onServerValidated()
        }
    }
    
    // Reset allowHttp when URL changes
    LaunchedEffect(serverUrl) {
        allowHttp = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Server") }
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
            Text(
                text = "Welcome to Overseerr",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Enter your Overseerr server URL to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://overseerr.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (serverUrl.isNotBlank()) {
                            viewModel.validateServer(serverUrl, allowHttp)
                        }
                    }
                ),
                trailingIcon = {
                    when (serverValidationState) {
                        is ServerValidationState.Valid -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        is ServerValidationState.Invalid -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Invalid",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                },
                isError = serverValidationState is ServerValidationState.Invalid,
                supportingText = {
                    when (val state = serverValidationState) {
                        is ServerValidationState.Invalid -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is ServerValidationState.Valid -> {
                            Text(
                                text = "Server validated successfully",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Show secure risk option if HTTPS error or HTTP URL
            val showRiskOption = (serverValidationState as? ServerValidationState.Invalid)?.message?.contains("HTTPS", ignoreCase = true) == true || 
                               (serverUrl.startsWith("http://") && !serverUrl.contains("localhost") && !serverUrl.contains("127.0.0.1"))
            
            if (showRiskOption) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowHttp,
                        onCheckedChange = { allowHttp = it }
                    )
                    Text(
                        text = "I accept the risks of using an insecure connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Button(
                onClick = {
                    if (serverUrl.isNotBlank()) {
                        viewModel.validateServer(serverUrl, allowHttp)
                    }
                },
                enabled = serverUrl.isNotBlank() && 
                         serverValidationState !is ServerValidationState.Validating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (serverValidationState is ServerValidationState.Validating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when (serverValidationState) {
                        is ServerValidationState.Validating -> "Validating..."
                        else -> "Continue"
                    }
                )
            }
            
            // Mock server button disabled
            // if (app.lusk.client.util.AppConfig.isDebug) {
            //    Spacer(modifier = Modifier.height(8.dp))
            //    OutlinedButton(
            //        onClick = { 
            //            serverUrl = "http://localhost:5055"
            //            viewModel.validateServer(serverUrl, true)
            //        },
            //        modifier = Modifier.fillMaxWidth()
            //    ) {
            //        Text("Debug: Use Mock Server")
            //    }
            // }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Make sure your server URL uses HTTPS for security",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
