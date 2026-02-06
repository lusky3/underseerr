package app.lusk.underseerr.presentation.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.*
import androidx.paging.LoadState
import kotlinx.coroutines.delay
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.lusk.underseerr.domain.model.Movie
import app.lusk.underseerr.domain.model.TvShow
import app.lusk.underseerr.domain.model.SearchResult
import app.lusk.underseerr.domain.model.MediaType
import app.lusk.underseerr.domain.model.Genre
import app.lusk.underseerr.ui.components.PosterImage
import app.lusk.underseerr.ui.components.ImageError
import app.lusk.underseerr.ui.components.SimpleImagePlaceholder
import app.lusk.underseerr.ui.components.OfflineBanner
import app.lusk.underseerr.ui.theme.LocalUnderseerrGradients
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import app.lusk.underseerr.domain.model.MediaStatus
import app.lusk.underseerr.domain.model.MediaInfo
import app.lusk.underseerr.ui.components.MediaSection
import app.lusk.underseerr.ui.components.MediaCard
import app.lusk.underseerr.ui.components.ConfirmDialog
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
    val homeScreenConfig by viewModel.homeScreenConfig.collectAsState()
    val watchlistIds by viewModel.watchlistIds.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Auto-dismiss refresh indicator when any of the data sources finish loading
    LaunchedEffect(trending.loadState.refresh) {
        if (trending.loadState.refresh !is LoadState.Loading && isRefreshing) {
            isRefreshing = false
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    var longPressedItem by remember { mutableStateOf<Any?>(null) }
    var itemToRemoveFromWatchlist by remember { mutableStateOf<app.lusk.underseerr.domain.model.SearchResult?>(null) }

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

    val gradients = LocalUnderseerrGradients.current
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Discover", color = gradients.onAppBar) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = gradients.onAppBar
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = gradients.onAppBar,
                    actionIconContentColor = gradients.onAppBar
                ),
                modifier = Modifier.background(gradients.appBar)
            )
        },
        modifier = modifier.background(gradients.background),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                trending.refresh()
                popularMovies.refresh()
                popularTvShows.refresh()
                upcomingMovies.refresh()
                upcomingTvShows.refresh()
                watchlist.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val hasOfflineError = trending.loadState.refresh is LoadState.Error ||
                        popularMovies.loadState.refresh is LoadState.Error ||
                        popularTvShows.loadState.refresh is LoadState.Error ||
                        upcomingMovies.loadState.refresh is LoadState.Error ||
                        upcomingTvShows.loadState.refresh is LoadState.Error
                
                val hasCachedData = trending.itemCount > 0 || 
                        popularMovies.itemCount > 0 || 
                        popularTvShows.itemCount > 0 ||
                        upcomingMovies.itemCount > 0 ||
                        upcomingTvShows.itemCount > 0
                val showOfflineBanner = hasOfflineError && hasCachedData

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = if (showOfflineBanner) 48.dp else 16.dp, bottom = 100.dp)
                ) {



                if (homeScreenConfig.showTrending) {
                    item {
                        MediaSection(
                            title = "Trending",
                            items = trending,
                            watchlistIds = watchlistIds,
                            onItemClickWithType = { id, type -> 
                                if (type == MediaType.TV) onTvShowClick(id) else onMovieClick(id)
                            },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showWatchlist && watchlist.itemCount > 0) {
                    item {
                        MediaSection(
                            title = "Plex Watchlist",
                            items = watchlist,
                            watchlistIds = watchlistIds,
                            onItemClickWithType = { id, type -> 
                                if (type == MediaType.TV) onTvShowClick(id) else onMovieClick(id)
                            },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showPopularMovies) {
                    item {
                        MediaSection(
                            title = "Popular Movies",
                            items = popularMovies,
                            watchlistIds = watchlistIds,
                            onItemClick = { onMovieClick(it) },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showPopularTvShows) {
                    item {
                        MediaSection(
                            title = "Popular TV Shows",
                            items = popularTvShows,
                            watchlistIds = watchlistIds,
                            onItemClick = { onTvShowClick(it) },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showUpcomingMovies) {
                    item {
                        MediaSection(
                            title = "Upcoming Movies",
                            items = upcomingMovies,
                            watchlistIds = watchlistIds,
                            onItemClick = { onMovieClick(it) },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showUpcomingTvShows) {
                    item {
                        MediaSection(
                            title = "Upcoming TV Shows",
                            items = upcomingTvShows,
                            watchlistIds = watchlistIds,
                            onItemClick = { onTvShowClick(it) },
                            onItemLongClick = { longPressedItem = it }
                        )
                    }
                }

                if (homeScreenConfig.showMovieGenres && movieGenres.isNotEmpty()) {
                    item {
                        GenericGenreSection(
                            title = "Movie Genres",
                            genres = movieGenres,
                            onGenreClick = { genre -> onCategoryClick(CategoryType.MOVIE_GENRE, genre.id, genre.name) }
                        )
                    }
                }

                if (homeScreenConfig.showTvGenres && tvGenres.isNotEmpty()) {
                    item {
                        GenericGenreSection(
                            title = "TV Genres",
                            genres = tvGenres,
                            onGenreClick = { genre -> onCategoryClick(CategoryType.TV_GENRE, genre.id, genre.name) }
                        )
                    }
                }

                if (homeScreenConfig.showStudios) {
                    item {
                        ChipSection(
                            title = "Studios",
                            items = studios,
                            onItemClick = { id, name -> onCategoryClick(CategoryType.STUDIO, id, name) }
                        )
                    }
                }

                if (homeScreenConfig.showNetworks) {
                    item {
                        ChipSection(
                            title = "Networks",
                            items = networks,
                            onItemClick = { id, name -> onCategoryClick(CategoryType.NETWORK, id, name) }
                        )
                    }
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

            // Bottom fade gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
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

            OfflineBanner(
                visible = showOfflineBanner,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            }
        }
    }

    // Watchlist removal confirmation
    itemToRemoveFromWatchlist?.let { item ->
        ConfirmDialog(
            title = "Remove from Watchlist?",
            message = "This will remove '${item.title}' from your Plex watchlist across all your devices.",
            confirmText = "Remove",
            onConfirm = {
                viewModel.removeFromWatchlist(item.id, item.mediaType, item.ratingKey)
                itemToRemoveFromWatchlist = null
            },
            onDismiss = { itemToRemoveFromWatchlist = null }
        )
    }

    // Context Menu for Long Pressed Item
    longPressedItem?.let { item ->
        val itemData = when (item) {
            is Movie -> ContextMenuItemData(item.id, MediaType.MOVIE, item.title, item.mediaInfo, null)
            is TvShow -> ContextMenuItemData(item.id, MediaType.TV, item.name, item.mediaInfo, null)
            is SearchResult -> ContextMenuItemData(item.id, item.mediaType, item.title, item.mediaInfo, item.ratingKey)
            else -> ContextMenuItemData(0, MediaType.MOVIE, "", null, null)
        }

        if (itemData.id != 0) {
            val status = itemData.mediaInfo?.status
            val gradients = LocalUnderseerrGradients.current
            val isInWatchlist = watchlistIds.contains(itemData.id)
            ModalBottomSheet(
                onDismissRequest = { longPressedItem = null },
                containerColor = Color.Transparent, // We'll use a gradients.surface background
                dragHandle = { BottomSheetDefaults.DragHandle(color = gradients.onSurface.copy(alpha = 0.4f)) }
            ) {
                Box(modifier = Modifier.background(gradients.surface).padding(bottom = 32.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = itemData.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = gradients.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Status Indicator (if already processed)
                        if (status == MediaStatus.AVAILABLE || status == MediaStatus.PENDING || status == MediaStatus.PROCESSING) {
                            val statusTitle = if (status == MediaStatus.AVAILABLE) "Available" else "Pending"
                            val statusSub = if (status == MediaStatus.AVAILABLE) "This media is already available" else "Waiting for media to become available"
                            val statusIcon = if (status == MediaStatus.AVAILABLE) Icons.Default.CheckCircle else Icons.Default.Schedule
                            val statusColor = if (status == MediaStatus.AVAILABLE) Color(0xFF4CAF50) else Color(0xFFFFA000)
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().alpha(0.8f),
                                color = gradients.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                ListItem(
                                    headlineContent = { Text(statusTitle, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text(statusSub) },
                                    leadingContent = { Icon(statusIcon, contentDescription = null, tint = statusColor) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        headlineColor = gradients.onSurface,
                                        supportingColor = gradients.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        } else {
                            // Request Option
                            Surface(
                                onClick = {
                                    if (itemData.type == MediaType.MOVIE) {
                                        viewModel.quickRequest(itemData.id, MediaType.MOVIE)
                                    } else {
                                        viewModel.quickRequestTv(itemData.id)
                                    }
                                    longPressedItem = null
                                    // Refresh feeds to show new status
                                    trending.refresh()
                                    watchlist.refresh()
                                    popularMovies.refresh()
                                    popularTvShows.refresh()
                                    upcomingMovies.refresh()
                                    upcomingTvShows.refresh()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                ListItem(
                                    headlineContent = { Text("Request", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Using server defaults") },
                                    leadingContent = { Icon(Icons.Default.AddTask, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        headlineColor = gradients.onSurface,
                                        supportingColor = gradients.onSurface.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }

                        // Watchlist option (add or remove)
                        if (isInWatchlist) {
                            // Remove from Watchlist option
                            Surface(
                                onClick = {
                                    itemToRemoveFromWatchlist = when (item) {
                                        is SearchResult -> item
                                        is Movie -> SearchResult(
                                            id = item.id,
                                            mediaType = MediaType.MOVIE,
                                            title = item.title,
                                            overview = item.overview,
                                            posterPath = item.posterPath,
                                            releaseDate = item.releaseDate,
                                            voteAverage = item.voteAverage,
                                            mediaInfo = item.mediaInfo,
                                            ratingKey = null
                                        )
                                        is TvShow -> SearchResult(
                                            id = item.id,
                                            mediaType = MediaType.TV,
                                            title = item.name,
                                            overview = item.overview,
                                            posterPath = item.posterPath,
                                            releaseDate = item.firstAirDate,
                                            voteAverage = item.voteAverage,
                                            mediaInfo = item.mediaInfo,
                                            ratingKey = null
                                        )
                                        else -> null
                                    }
                                    longPressedItem = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                ListItem(
                                    headlineContent = { Text("Remove from Watchlist", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        headlineColor = gradients.onSurface
                                    )
                                )
                            }
                        } else {
                            // Add to Watchlist option
                            Surface(
                                onClick = {
                                    viewModel.addToWatchlist(itemData.id, itemData.type, null)
                                    longPressedItem = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                ListItem(
                                    headlineContent = { Text("Add to Watchlist", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Save to your Plex watchlist") },
                                    leadingContent = { Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent,
                                        headlineColor = gradients.onSurface,
                                        supportingColor = gradients.onSurface.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ContextMenuItemData(
    val id: Int,
    val type: MediaType,
    val title: String,
    val mediaInfo: MediaInfo?,
    val ratingKey: String?
)

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




