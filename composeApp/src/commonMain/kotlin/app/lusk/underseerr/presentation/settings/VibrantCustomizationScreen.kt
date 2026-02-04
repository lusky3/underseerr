package app.lusk.underseerr.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.lusk.underseerr.domain.repository.VibrantThemeColors
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrantCustomizationScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val colors by viewModel.vibrantThemeColors.collectAsState()
    val gradients = LocalUnderseerrGradients.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Vibrant Theme") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.updateVibrantThemeColors(VibrantThemeColors()) }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to Default")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradients.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Adjust the gradients and colors for your personal Vibrant theme.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pill-shaped labels",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Use circular pill shapes for status badges instead of rounded rectangles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = colors.usePillShape,
                            onCheckedChange = { viewModel.updateVibrantThemeColors(colors.copy(usePillShape = it)) }
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Gradient Direction",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            app.lusk.underseerr.domain.repository.GradientDirection.values().forEach { direction ->
                                FilterChip(
                                    selected = colors.gradientDirection == direction,
                                    onClick = { 
                                        viewModel.updateVibrantThemeColors(colors.copy(gradientDirection = direction)) 
                                    },
                                    label = { 
                                        Text(
                                            when(direction) {
                                                app.lusk.underseerr.domain.repository.GradientDirection.VERTICAL -> "Vertical"
                                                app.lusk.underseerr.domain.repository.GradientDirection.HORIZONTAL -> "Horizontal"
                                                app.lusk.underseerr.domain.repository.GradientDirection.DIAGONAL_TL_BR -> "Diagonal ↘"
                                                app.lusk.underseerr.domain.repository.GradientDirection.DIAGONAL_TR_BL -> "Diagonal ↙"
                                            }
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                }

                CustomizationSection(
                    title = "Primary Gradient",
                    startColor = colors.primaryStart,
                    endColor = colors.primaryEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(primaryStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(primaryEnd = it)) }
                )

                CustomizationSection(
                    title = "Secondary Gradient (Available)",
                    startColor = colors.secondaryStart,
                    endColor = colors.secondaryEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(secondaryStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(secondaryEnd = it)) }
                )

                CustomizationSection(
                    title = "Tertiary Gradient (Pending)",
                    startColor = colors.tertiaryStart,
                    endColor = colors.tertiaryEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(tertiaryStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(tertiaryEnd = it)) }
                )

                CustomizationSection(
                    title = "Background Gradient",
                    startColor = colors.backgroundStart,
                    endColor = colors.backgroundEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(backgroundStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(backgroundEnd = it)) }
                )

                CustomizationSection(
                    title = "Surface Gradient",
                    startColor = colors.surfaceStart,
                    endColor = colors.surfaceEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(surfaceStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(surfaceEnd = it)) }
                )

                CustomizationSection(
                    title = "Accent Gradient",
                    startColor = colors.accentStart,
                    endColor = colors.accentEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(accentStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(accentEnd = it)) }
                )

                CustomizationSection(
                    title = "Highlight Gradient",
                    startColor = colors.highlightStart,
                    endColor = colors.highlightEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(highlightStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(highlightEnd = it)) }
                )

                CustomizationSection(
                    title = "UI Header (Top Bar)",
                    startColor = colors.appBarStart,
                    endColor = colors.appBarEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(appBarStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(appBarEnd = it)) }
                )

                CustomizationSection(
                    title = "Navigation Bar / Rail",
                    startColor = colors.navBarStart,
                    endColor = colors.navBarEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(navBarStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(navBarEnd = it)) }
                )

                CustomizationSection(
                    title = "Settings Page Background",
                    startColor = colors.settingsStart,
                    endColor = colors.settingsEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(settingsStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(settingsEnd = it)) }
                )

                CustomizationSection(
                    title = "Profile Page Background",
                    startColor = colors.profilesStart,
                    endColor = colors.profilesEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(profilesStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(profilesEnd = it)) }
                )

                CustomizationSection(
                    title = "Request Details Background",
                    startColor = colors.requestDetailsStart,
                    endColor = colors.requestDetailsEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(requestDetailsStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(requestDetailsEnd = it)) }
                )

                CustomizationSection(
                    title = "Issue Details Background",
                    startColor = colors.issueDetailsStart,
                    endColor = colors.issueDetailsEnd,
                    onStartChange = { viewModel.updateVibrantThemeColors(colors.copy(issueDetailsStart = it)) },
                    onEndChange = { viewModel.updateVibrantThemeColors(colors.copy(issueDetailsEnd = it)) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CustomizationSection(
    title: String,
    startColor: String,
    endColor: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    parseColor(startColor),
                                    parseColor(endColor)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorPickerButton(
                        label = "Start",
                        hex = startColor,
                        onHexChange = onStartChange,
                        modifier = Modifier.weight(1f)
                    )
                    ColorPickerButton(
                        label = "End",
                        hex = endColor,
                        onHexChange = onEndChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerButton(
    label: String,
    hex: String,
    onHexChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedCard(
        onClick = { showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Black.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(parseColor(hex))
            )
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall)
                Text(text = hex, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPicker) {
        ColorPickerDialog(
            initialHex = hex,
            onDismiss = { showPicker = false },
            onColorSelected = { 
                onHexChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialColor = parseColor(initialHex)
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    val currentColor = Color(red, green, blue, alpha)
    val currentHex = formatColor(currentColor)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Pick Color", style = MaterialTheme.typography.headlineSmall)

                // Large Preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                )

                Text(
                    text = currentHex,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSlider(label = "Red", value = red, color = Color.Red, onValueChange = { red = it })
                    ColorSlider(label = "Green", value = green, color = Color.Green, onValueChange = { green = it })
                    ColorSlider(label = "Blue", value = blue, color = Color.Blue, onValueChange = { blue = it })
                    ColorSlider(label = "Alpha", value = alpha, color = Color.Gray, onValueChange = { alpha = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(onClick = { onColorSelected(currentHex) }) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f)
            )
        )
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length == 8) {
            // ARGB
            val a = cleanHex.substring(0, 2).toInt(16)
            val r = cleanHex.substring(2, 4).toInt(16)
            val g = cleanHex.substring(4, 6).toInt(16)
            val b = cleanHex.substring(6, 8).toInt(16)
            Color(r, g, b, a)
        } else if (cleanHex.length == 6) {
            // RGB
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(r, g, b, 255)
        } else {
            Color.White
        }
    } catch (e: Exception) {
        Color.White
    }
}

private fun formatColor(color: Color): String {
    val argb = (color.alpha * 255).toInt() shl 24 or
               ((color.red * 255).toInt() shl 16) or
               ((color.green * 255).toInt() shl 8) or
               (color.blue * 255).toInt()
    return "#" + argb.toUInt().toString(16).uppercase().padStart(8, '0')
}
