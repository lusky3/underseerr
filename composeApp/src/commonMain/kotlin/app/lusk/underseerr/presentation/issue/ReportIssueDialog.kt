package app.lusk.underseerr.presentation.issue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import app.lusk.underseerr.domain.model.IssueType

import app.lusk.underseerr.ui.components.GradientButton
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients

/**
 * Dialog for reporting a new issue on media.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueDialog(
    mediaTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (issueType: Int, message: String, season: Int, episode: Int) -> Unit,
    modifier: Modifier = Modifier,
    isTvShow: Boolean = false,
    numberOfSeasons: Int = 0
) {
    var selectedIssueType by remember { mutableStateOf(IssueType.VIDEO) }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // TV Specific state
    var selectedSeason by remember { mutableStateOf(0) } // 0 = All/None
    var selectedEpisode by remember { mutableStateOf("") } // String input for flexibility
    var isSeasonDropdownExpanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        modifier = modifier,
        title = {
            Text("Report Issue")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Report a problem with \"$mediaTitle\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Issue type selection
                Text(
                    text = "Issue Type",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column(modifier = Modifier.selectableGroup()) {
                    IssueType.entries.forEach { issueType ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedIssueType == issueType,
                                    onClick = { selectedIssueType = issueType },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIssueType == issueType,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = issueType.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // TV Specific Options
                if (isTvShow && numberOfSeasons > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Affected Content",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        // Season Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isSeasonDropdownExpanded,
                            onExpandedChange = { isSeasonDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (selectedSeason == 0) "All Seasons / Global" else "Season $selectedSeason",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSeasonDropdownExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                label = { Text("Season") }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = isSeasonDropdownExpanded,
                                onDismissRequest = { isSeasonDropdownExpanded = false }
                            ) {
                                // Option for All/Global
                                DropdownMenuItem(
                                    text = { Text("All Seasons / Global") },
                                    onClick = {
                                        selectedSeason = 0
                                        isSeasonDropdownExpanded = false
                                    }
                                )
                                
                                // Options for each season
                                for (i in 1..numberOfSeasons) {
                                    DropdownMenuItem(
                                        text = { Text("Season $i") },
                                        onClick = {
                                            selectedSeason = i
                                            isSeasonDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Episode Input (only if specific season selected)
                        if (selectedSeason > 0) {
                            OutlinedTextField(
                                value = selectedEpisode,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        selectedEpisode = newValue
                                    }
                                },
                                label = { Text("Episode Number (Optional)") },
                                placeholder = { Text("e.g. 1") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
                
                // Message input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Describe the issue") },
                    placeholder = { Text("What's wrong with this media?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSubmitting
                )
            }
        },
        confirmButton = {
            GradientButton(
                onClick = {
                    if (message.isNotBlank()) {
                        isSubmitting = true
                        val episodeNum = selectedEpisode.toIntOrNull() ?: 0
                        onSubmit(selectedIssueType.value, message, selectedSeason, episodeNum)
                    }
                },
                enabled = message.isNotBlank() && !isSubmitting,
                brush = LocalUnderseerrGradients.current.primary
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Submit", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}
