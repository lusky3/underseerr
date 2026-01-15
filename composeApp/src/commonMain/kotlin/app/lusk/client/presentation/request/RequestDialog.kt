package app.lusk.client.presentation.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.repository.QualityProfile
import app.lusk.client.domain.repository.RootFolder

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Composable
fun RequestDialog(
    mediaId: Int,
    mediaType: MediaType,
    mediaTitle: String,
    seasonCount: Int = 0,
    partialRequestsEnabled: Boolean = true,
    isModify: Boolean = false,
    requestedSeasons: List<Int> = emptyList(),
    viewModel: RequestViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requestState by viewModel.requestState.collectAsState()
    val qualityProfiles by viewModel.qualityProfiles.collectAsState()
    val rootFolders by viewModel.rootFolders.collectAsState()

    println("UI_DEBUG: mediaId=$mediaId, profiles=${qualityProfiles.size}, folders=${rootFolders.size}")
    qualityProfiles.forEachIndexed { i, p -> println("UI_DEBUG: Profile[$i]: id=${p.id}, name='${p.name}'") }
    rootFolders.forEachIndexed { i, f -> println("UI_DEBUG: Folder[$i]: id=${f.id}, path='${f.path}'") }
    
    // If partial requests disabled, auto-select all
    var selectedSeasons by remember { 
        mutableStateOf<List<Int>>(
            if (!partialRequestsEnabled) {
                if (seasonCount > 0) (1..seasonCount).toList() else listOf(0)
            } else {
                emptyList()
            }
        ) 
    }
    
    var selectedQualityProfile by remember { mutableStateOf<Int?>(null) }
    var selectedRootFolder by remember { mutableStateOf<String?>(null) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val isMovie = mediaType == MediaType.MOVIE
        viewModel.loadQualityProfiles(isMovie)
        viewModel.loadRootFolders(isMovie)
    }
    
    LaunchedEffect(requestState) {
        if (requestState is RequestState.Success) {
            onSuccess()
            viewModel.clearRequestState()
        }
    }

    LaunchedEffect(mediaId) {
        selectedQualityProfile = null
        selectedRootFolder = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isModify) "Modify Request" else "Request $mediaTitle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (mediaType == MediaType.TV) {
                    SeasonSelector(
                        seasonCount = seasonCount,
                        selectedSeasons = selectedSeasons,
                        partialRequestsEnabled = partialRequestsEnabled,
                        requestedSeasons = requestedSeasons,
                        onSeasonsChanged = { selectedSeasons = it }
                    )
                }
                
                if (!isModify) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Advanced Options")
                        Switch(
                            checked = showAdvancedOptions,
                            onCheckedChange = { showAdvancedOptions = it }
                        )
                    }
                    
                    if (showAdvancedOptions) {
                        val isOptionsLoading by viewModel.isOptionsLoading.collectAsState()
                        
                        if (isOptionsLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            if (qualityProfiles.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Quality Profile (${qualityProfiles.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    qualityProfiles.forEach { profile ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = selectedQualityProfile == profile.id,
                                                    onClick = { selectedQualityProfile = profile.id }
                                                )
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedQualityProfile == profile.id,
                                                onClick = { selectedQualityProfile = profile.id }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (rootFolders.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Root Folder (${rootFolders.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    rootFolders.forEach { folder ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = selectedRootFolder == folder.id,
                                                    onClick = { selectedRootFolder = folder.id }
                                                )
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedRootFolder == folder.id,
                                                onClick = { selectedRootFolder = folder.id }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = folder.path,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (qualityProfiles.isEmpty() && rootFolders.isEmpty()) {
                                val error by viewModel.error.collectAsState()
                                Text(
                                    text = error ?: "No advanced options available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                when (requestState) {
                    is RequestState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    is RequestState.Error -> {
                        Text(
                            text = (requestState as RequestState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mediaType) {
                        MediaType.MOVIE -> {
                            viewModel.submitMovieRequest(
                                movieId = mediaId,
                                qualityProfile = selectedQualityProfile,
                                rootFolder = selectedRootFolder
                            )
                        }
                        MediaType.TV -> {
                            if (selectedSeasons.isNotEmpty()) {
                                viewModel.submitTvShowRequest(
                                    tvShowId = mediaId,
                                    seasons = selectedSeasons,
                                    qualityProfile = selectedQualityProfile,
                                    rootFolder = selectedRootFolder
                                )
                            }
                        }
                    }
                },
                enabled = requestState !is RequestState.Loading &&
                        (mediaType == MediaType.MOVIE || selectedSeasons.isNotEmpty())
            ) {
                Text(if (isModify) "Modify Request" else "Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SeasonSelector(
    seasonCount: Int,
    selectedSeasons: List<Int>,
    partialRequestsEnabled: Boolean,
    requestedSeasons: List<Int> = emptyList(),
    onSeasonsChanged: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Select Seasons",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (!partialRequestsEnabled) {
            Text(
                "Partial series requests are disabled. Requesting all seasons.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        if (seasonCount > 0) {
                            // Select all valid seasons (not already requested)
                            val allValidSeasons = (1..seasonCount).filter { !requestedSeasons.contains(it) }
                            onSeasonsChanged(allValidSeasons)
                        } else {
                            // Fallback behavior if count unknown
                            onSeasonsChanged(listOf(0)) 
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("All Seasons")
                }
                
                Button(
                    onClick = { onSeasonsChanged(emptyList()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Individual Season Selection
            if (seasonCount > 0) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(seasonCount) { index ->
                        val seasonNumber = index + 1
                        val isAlreadyRequested = requestedSeasons.contains(seasonNumber)
                        
                        FilterChip(
                            selected = selectedSeasons.contains(seasonNumber) || isAlreadyRequested,
                            onClick = {
                                if (!isAlreadyRequested) {
                                    val currentSelections = selectedSeasons.toMutableList()
                                    if (currentSelections.contains(seasonNumber)) {
                                        currentSelections.remove(seasonNumber)
                                    } else {
                                        if (currentSelections.contains(0)) currentSelections.clear()
                                        currentSelections.add(seasonNumber)
                                    }
                                    onSeasonsChanged(currentSelections.sorted())
                                }
                            },
                            label = { Text("S$seasonNumber") },
                            enabled = !isAlreadyRequested,
                            colors = FilterChipDefaults.filterChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                disabledSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val totalSelectedAndRequested = selectedSeasons.size + requestedSeasons.size
        if (selectedSeasons.isNotEmpty()) {
            Text(
                text = if (selectedSeasons.contains(0)) {
                    "All seasons selected"
                } else if (totalSelectedAndRequested >= seasonCount && seasonCount > 0) {
                   "All seasons selected"
                } else {
                    "Seasons: ${selectedSeasons.joinToString(", ")}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

