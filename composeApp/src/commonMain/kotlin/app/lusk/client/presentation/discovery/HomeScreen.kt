package app.lusk.client.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.*
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.lusk.client.domain.model.Movie
import app.lusk.client.domain.model.TvShow
import app.lusk.client.domain.model.SearchResult
import app.lusk.client.domain.model.MediaType
import app.lusk.client.domain.model.Genre
import app.lusk.client.ui.components.PosterImage
import app.lusk.client.ui.components.ImageError
import app.lusk.client.ui.components.SimpleImagePlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DiscoveryViewModel,
    onMovieClick: (Int) -> Unit,
    onTvShowClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onCategoryClick: (CategoryType, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trending = viewModel.trending.collectAsLazyPagingItems()
    
    val popularMovies = viewModel.popularMovies.collectAsLazyPagingItems()
    val popularTvShows = viewModel.popularTvShows.collectAsLazyPagingItems()
    
    val upcomingMovies = viewModel.upcomingMovies.collectAsLazyPagingItems()
    val upcomingTvShows = viewModel.upcomingTvShows.collectAsLazyPagingItems()
    
    val watchlist = viewModel.watchlist.collectAsLazyPagingItems()
    
    val movieGenres by viewModel.movieGenres.collectAsState()
    val tvGenres by viewModel.tvGenres.collectAsState()
    val isPlexUser by viewModel.isPlexUser.collectAsState()

    val studios = listOf(
        3 to "Pixar",
        420 to "Marvel Studios",
        2 to "Walt Disney Pictures",
        174 to "Warner Bros. Pictures",
        33 to "Universal Pictures",
        5 to "Columbia Pictures",
        521 to "DreamWorks Animation"
    )

    val networks = listOf(
        213 to "Netflix",
        49 to "HBO",
        1024 to "Amazon",
        2739 to "Disney+",
        2559 to "Apple TV+",
        67 to "Showtime",
        43 to "FOX"
    )

    var pullRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                trending.refresh()
                popularMovies.refresh()
                popularTvShows.refresh()
                upcomingMovies.refresh()
                upcomingTvShows.refresh()
                watchlist.refresh()
                viewModel.refresh()
                pullRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                item {
                    MediaSection(
                        title = "Trending",
                        items = trending,
                        onItemClickWithType = { id, type -> 
                            if (type == MediaType.TV) onTvShowClick(id) else onMovieClick(id)
                        }
                    )
                }

                if (isPlexUser && watchlist.itemCount > 0) {
                    item {
                        MediaSection(
                            title = "Your Watchlist",
                            items = watchlist,
                            onItemClickWithType = { id, type -> 
                                if (type == MediaType.TV) onTvShowClick(id) else onMovieClick(id)
                            }
                        )
                    }
                }

                item {
                    MediaSection(
                        title = "Popular Movies",
                        items = popularMovies,
                        onItemClick = { onMovieClick(it) }
                    )
                }

                item {
                    MediaSection(
                        title = "Popular TV Shows",
                        items = popularTvShows,
                        onItemClick = { onTvShowClick(it) }
                    )
                }

                item {
                    MediaSection(
                        title = "Upcoming Movies",
                        items = upcomingMovies,
                        onItemClick = { onMovieClick(it) }
                    )
                }

                item {
                    MediaSection(
                        title = "Upcoming TV Shows",
                        items = upcomingTvShows,
                        onItemClick = { onTvShowClick(it) }
                    )
                }

                if (movieGenres.isNotEmpty()) {
                    item {
                        GenericGenreSection(
                            title = "Movie Genres",
                            genres = movieGenres,
                            onGenreClick = { genre -> onCategoryClick(CategoryType.MOVIE_GENRE, genre.id, genre.name) }
                        )
                    }
                }

                if (tvGenres.isNotEmpty()) {
                    item {
                        GenericGenreSection(
                            title = "TV Genres",
                            genres = tvGenres,
                            onGenreClick = { genre -> onCategoryClick(CategoryType.TV_GENRE, genre.id, genre.name) }
                        )
                    }
                }

                item {
                    ChipSection(
                        title = "Studios",
                        items = studios,
                        onItemClick = { id, name -> onCategoryClick(CategoryType.STUDIO, id, name) }
                    )
                }

                item {
                    ChipSection(
                        title = "Networks",
                        items = networks,
                        onItemClick = { id, name -> onCategoryClick(CategoryType.NETWORK, id, name) }
                    )
                }
            }

            // Top fade gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        }
    }
}

@Composable
private fun GenericGenreSection(
    title: String,
    genres: List<Genre>,
    onGenreClick: (Genre) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(genres.size) { index ->
                SuggestionChip(
                    onClick = { onGenreClick(genres[index]) },
                    label = { Text(genres[index].name) }
                )
            }
        }
    }
}

@Composable
private fun ChipSection(
    title: String,
    items: List<Pair<Int, String>>,
    onItemClick: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                SuggestionChip(
                    onClick = { onItemClick(item.first, item.second) },
                    label = { Text(item.second) }
                )
            }
        }
    }
}

@Composable
private fun <T : Any> MediaSection(
    title: String,
    items: LazyPagingItems<T>,
    onItemClick: (Int) -> Unit = {},
    onItemClickWithType: (Int, MediaType) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (items.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LoadState.Error -> {
                val error = (items.loadState.refresh as LoadState.Error).error
                ErrorMessage(
                    message = error.message ?: "Failed to load content",
                    onRetry = { items.retry() }
                )
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(items.itemCount) { index ->
                        items[index]?.let { item ->
                            MediaCard(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is Movie -> onItemClick(item.id)
                                        is TvShow -> onItemClick(item.id)
                                        is SearchResult -> {
                                            onItemClickWithType(item.id, item.mediaType)
                                            // Backwards compat if only onItemClick provided
                                            if (item.mediaType == MediaType.MOVIE) onItemClick(item.id)
                                        }
                                    }
                                }
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
                }
            }
        }
    }
}

@Composable
private fun <T : Any> MediaCard(
    item: T,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick),
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
                SimpleImagePlaceholder()
            }

            // Gradient Scrim and Title
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

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
