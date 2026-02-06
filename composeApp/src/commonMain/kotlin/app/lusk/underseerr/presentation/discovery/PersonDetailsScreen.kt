package app.lusk.underseerr.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Person
import app.lusk.underseerr.domain.model.PersonCredit
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.ui.components.AsyncImage
import app.lusk.underseerr.ui.components.LoadingState
import app.lusk.underseerr.ui.components.PosterImage
import app.lusk.underseerr.ui.components.BackdropImage
import app.lusk.underseerr.ui.components.MediaCard
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsScreen(
    personId: Int,
    viewModel: DiscoveryViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.personDetailsState.collectAsState()
    val gradients = LocalUnderseerrGradients.current
    
    LaunchedEffect(personId) {
        viewModel.loadPersonDetails(personId)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPersonDetails()
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.background(gradients.background)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val currentState = state) {
                is PersonDetailsState.Loading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                is PersonDetailsState.Error -> {
                    app.lusk.underseerr.ui.components.UnifiedErrorDisplay(
                        message = currentState.message,
                        onRetry = { viewModel.loadPersonDetails(personId) }
                    )
                }
                is PersonDetailsState.Success -> {
                    PersonDetailsContent(
                        person = currentState.person,
                        onBackClick = onBackClick,
                        onMediaClick = onMediaClick
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun PersonDetailsContent(
    person: Person,
    onBackClick: () -> Unit,
    onMediaClick: (MediaType, Int) -> Unit
) {
    val gradients = LocalUnderseerrGradients.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Backdrop with profile image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (!person.profilePath.isNullOrEmpty()) {
                        AsyncImage(
                            imageUrl = "https://image.tmdb.org/t/p/w780${person.profilePath}",
                            contentDescription = person.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradients.surface)
                        )
                    }
                    
                    // Bottom fade gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                    
                    // Back button
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Info Card overlapping backdrop
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp)
                        .offset(y = (-64).dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.background(gradients.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = gradients.onSurface
                        )
                        
                        person.knownForDepartment?.takeIf { it.isNotBlank() }?.let { dept ->
                            Text(
                                text = dept,
                                style = MaterialTheme.typography.titleMedium,
                                color = gradients.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        if (!person.birthday.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Born",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = person.birthday,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = gradients.onSurface
                                )
                            }
                        }
                        
                        if (!person.placeOfBirth.isNullOrEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "From",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = person.placeOfBirth,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = gradients.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (!person.biography.isNullOrEmpty()) {
                            Text(
                                text = "Biography",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = gradients.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = person.biography,
                                style = MaterialTheme.typography.bodyMedium,
                                color = gradients.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                }
            }
            
            // Known For section
            if (person.credits.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .offset(y = (-32).dp)
                    ) {
                        Text(
                            text = "Known For",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = gradients.onSurface,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(person.credits) { credit ->
                                MediaCard(
                                    item = SearchResult(
                                        id = credit.id,
                                        mediaType = credit.mediaType,
                                        title = credit.title,
                                        overview = credit.overview,
                                        posterPath = credit.posterPath,
                                        releaseDate = credit.releaseDate,
                                        voteAverage = credit.voteAverage,
                                        mediaInfo = null
                                    ),
                                    onClick = { onMediaClick(credit.mediaType, credit.id) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Interaction Spacer for bottom navigation / gesture area
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
