package app.lusk.underseerr.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.domain.model.TvShow
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun <T : Any> MediaSection(
    title: String,
    items: LazyPagingItems<T>,
    watchlistIds: Set<Int> = emptySet(),
    onItemClick: (Int) -> Unit = {},
    onItemClickWithType: (Int, MediaType) -> Unit = { _, _ -> },
    onItemLongClick: (T) -> Unit = {},
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = titleStyle,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val loadState = items.loadState.refresh
        val isListEmpty = items.itemCount == 0

        when {
            loadState is LoadState.Loading && isListEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            loadState is LoadState.Error && isListEmpty -> {
                val error = loadState.error
                UnifiedErrorDisplay(
                    message = error.message ?: "Failed to load content",
                    onRetry = { items.retry() },
                    modifier = Modifier.height(300.dp)
                )
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(items.itemCount) { index ->
                        items[index]?.let { item ->
                            val itemId = when (item) {
                                is Movie -> item.id
                                is TvShow -> item.id
                                is SearchResult -> item.id
                                else -> 0
                            }
                            MediaCard(
                                item = item,
                                isInWatchlist = watchlistIds.contains(itemId),
                                onClick = {
                                    when (item) {
                                        is Movie -> onItemClick(item.id)
                                        is TvShow -> onItemClick(item.id)
                                        is SearchResult -> {
                                            onItemClickWithType(item.id, item.mediaType)
                                            if (item.mediaType == MediaType.MOVIE) onItemClick(item.id)
                                        }
                                    }
                                },
                                onLongClick = { onItemLongClick(item) }
                            )
                        }
                    }

                    if (items.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(225.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    
                    if (items.loadState.append is LoadState.Error) {
                        item {
                            IconButton(onClick = { items.retry() }) {
                                Icon(Icons.Default.Refresh, "Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> MediaCard(
    item: T,
    isInWatchlist: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (posterPath, title) = when (item) {
        is Movie -> item.posterPath to item.title
        is TvShow -> item.posterPath to item.name
        is SearchResult -> item.posterPath to item.title
        else -> null to ""
    }

    Card(
        modifier = modifier
            .width(150.dp)
            .height(225.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!posterPath.isNullOrEmpty()) {
                PosterImage(
                    posterPath = posterPath,
                    title = title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (isInWatchlist) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "In Watchlist",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black
                            ),
                            startY = 100f
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}
