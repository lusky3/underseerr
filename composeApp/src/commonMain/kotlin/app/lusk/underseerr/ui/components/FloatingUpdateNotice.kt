package app.lusk.underseerr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Floating update notice that appears on the bottom-right of the Profile screen.
 * 
 * Starts as a small pulsing FAB with an update icon. When tapped, it smoothly
 * expands into a card showing "Update Available" with an "Update" button.
 * Tapping "Update" triggers the update flow; tapping the close icon collapses it.
 */
@Composable
fun FloatingUpdateNotice(
    isUpdateAvailable: Boolean,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isUpdateAvailable) return

    var isExpanded by remember { mutableStateOf(false) }

    // Subtle scale pulse for the collapsed FAB
    val pulseScale by animateFloatAsState(
        targetValue = if (!isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pulse"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 20.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "corner"
    )

    Surface(
        onClick = { if (!isExpanded) isExpanded = true },
        modifier = modifier.then(
            if (!isExpanded) Modifier.scale(pulseScale) else Modifier
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (isExpanded) 16.dp else 14.dp,
                end = if (isExpanded) 8.dp else 14.dp,
                top = if (isExpanded) 10.dp else 14.dp,
                bottom = if (isExpanded) 10.dp else 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Update icon (always visible)
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Update available",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (isExpanded) 20.dp else 24.dp)
            )

            // Animated expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Update Available",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    FilledTonalButton(
                        onClick = onUpdateClick,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Update",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Close button to collapse
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
