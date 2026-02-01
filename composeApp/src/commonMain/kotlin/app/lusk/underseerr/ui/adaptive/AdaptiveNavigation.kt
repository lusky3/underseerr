package app.lusk.underseerr.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.navigation.Screen

/**
 * Adaptive navigation components that adjust based on screen size.
 * KMP Compatible.
 */

/**
 * Navigation destination data class.
 */
data class NavigationDestination(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

/**
 * Default navigation destinations.
 */
val defaultNavigationDestinations = listOf(
    NavigationDestination(
        screen = Screen.Home,
        icon = Icons.Default.Home,
        label = "Home"
    ),
    NavigationDestination(
        screen = Screen.Requests(),
        icon = Icons.Default.PlayArrow,
        label = "Requests"
    ),
    NavigationDestination(
        screen = Screen.Issues,
        icon = Icons.Default.Info,
        label = "Issues"
    ),
    NavigationDestination(
        screen = Screen.Profile,
        icon = Icons.Default.Person,
        label = "Profile"
    )
)

/**
 * Adaptive navigation that switches between bottom bar and navigation rail.
 */
@Composable
fun AdaptiveNavigation(
    currentScreen: Screen,
    layoutConfig: AdaptiveLayoutConfig,
    destinations: List<NavigationDestination> = defaultNavigationDestinations,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradients = app.lusk.underseerr.ui.theme.LocalUnderseerrGradients.current
    if (layoutConfig.useNavigationRail) {
        NavigationRail(
            modifier = modifier.background(gradients.navBar),
            containerColor = Color.Transparent,
            contentColor = gradients.onNavBar
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            destinations.forEach { destination ->
                NavigationRailItem(
                    selected = currentScreen == destination.screen,
                    onClick = { onNavigate(destination.screen) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = if (layoutConfig.showNavigationLabels) {
                        { Text(destination.label) }
                    } else null
                )
            }
        }
    } else {
        Column(modifier = modifier) {

            // Solid navigation bar
            NavigationBar(
                modifier = Modifier.background(gradients.navBar),
                containerColor = Color.Transparent,
                contentColor = gradients.onNavBar,
                tonalElevation = 0.dp
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentScreen == destination.screen,
                        onClick = { onNavigate(destination.screen) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = if (currentScreen == destination.screen) MaterialTheme.colorScheme.primary else gradients.onNavBar.copy(alpha = 0.6f)
                            )
                        },
                        label = if (layoutConfig.showNavigationLabels) {
                            { 
                                Text(
                                    destination.label, 
                                    color = if (currentScreen == destination.screen) MaterialTheme.colorScheme.primary else gradients.onNavBar.copy(alpha = 0.6f)
                                ) 
                            }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation bar for compact screens.
 */
@Composable
fun CompactNavigation(
    currentScreen: Screen,
    destinations: List<NavigationDestination> = defaultNavigationDestinations,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentScreen == destination.screen,
                onClick = { onNavigate(destination.screen) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

/**
 * Navigation rail for medium and expanded screens.
 */
@Composable
fun ExpandedNavigation(
    currentScreen: Screen,
    destinations: List<NavigationDestination> = defaultNavigationDestinations,
    onNavigate: (Screen) -> Unit,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = currentScreen == destination.screen,
                onClick = { onNavigate(destination.screen) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = if (showLabels) {
                    { Text(destination.label) }
                } else null
            )
        }
    }
}
