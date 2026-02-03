package app.lusk.underseerr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedErrorDisplay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    friendlyMessage: String? = null
) {
    var showDetails by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            val displayMessage = friendlyMessage ?: if (message.contains("Unable to resolve host") || message.contains("timeout") || message.contains("ConnectException")) {
                 "We're having trouble connecting to the server. Please check your internet connection."
            } else {
                 "Something went wrong while loading this content."
            }

            Text(
                text = displayMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Technical Details (Expandable)
            TextButton(
                onClick = { showDetails = !showDetails }
            ) {
                Text(if (showDetails) "Hide Technical Details" else "Show Technical Details")
            }

            AnimatedVisibility(visible = showDetails) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                     Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        // Ensure it doesn't overflow if really long
                        textAlign = TextAlign.Start
                     )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Text("Retry")
            }
        }
    }
}
