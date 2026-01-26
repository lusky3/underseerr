package app.lusk.client.presentation.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.model.SearchResult
import app.lusk.client.ui.components.PosterImage
import app.lusk.client.ui.components.SimpleImagePlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryResultsScreen(
    viewModel: DiscoveryViewModel,
    categoryType: String,
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit,
    onMediaClick: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagingItems = viewModel.categoryResults.collectAsLazyPagingItems()

    LaunchedEffect(categoryType, categoryId) {
        val type = try {
            CategoryType.valueOf(categoryType)
        } catch (e: Exception) {
            CategoryType.MOVIE_GENRE
        }
        viewModel.selectCategory(type, categoryId, categoryName)
    }

    var pullRefreshing by remember { mutableStateOf(false) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                pagingItems.refresh()
                pullRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pagingItems.loadState.refresh is LoadState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pagingItems.loadState.refresh is LoadState.Error) {
                val e = pagingItems.loadState.refresh as LoadState.Error
                ErrorDisplay(
                    message = e.error.message ?: "Failed to load results",
                    onRetry = { pagingItems.retry() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pagingItems.itemCount) { index ->
                        pagingItems[index]?.let { item ->
                            CategoryMediaCard(
                                item = item,
                                onClick = { onMediaClick(item.mediaType, item.id) }
                            )
                        }
                    }

                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    
                    if (pagingItems.loadState.append is LoadState.Error) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            val e = pagingItems.loadState.append as LoadState.Error
                            ErrorDisplay(
                                message = e.error.message ?: "Failed to load more",
                                onRetry = { pagingItems.retry() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMediaCard(
    item: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(2f / 3f)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (!item.posterPath.isNullOrEmpty()) {
                PosterImage(
                    posterPath = item.posterPath,
                    title = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SimpleImagePlaceholder()
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        val year = item.releaseDate?.take(4)
        if (!year.isNullOrEmpty()) {
            Text(
                text = year,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorDisplay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
