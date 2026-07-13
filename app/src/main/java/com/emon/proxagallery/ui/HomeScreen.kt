package com.emon.proxagallery.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emon.proxagallery.util.PermissionHelper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.MediaItem
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onTabClick: (Int) -> Unit = {}
) {
    BackHandler(enabled = uiState.selectedAlbumId != null) {
        onAlbumClick(null)
    }

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        allPhotos = allPhotos,
        albumPhotos = albumPhotos,
        searchPhotos = searchPhotos,
        onPhotosAccessGranted = onPhotosAccessGranted,
        onSearchQueryChange = onSearchQueryChange,
        onPhotoClick = onPhotoClick,
        onToggleFavorite = onToggleFavorite,
        onAlbumClick = onAlbumClick,
        onTabClick = onTabClick
    )
}

@Composable
private fun HomeScreenContent(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onTabClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state as mutableState so recomposition triggers on change.
    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasPermission(context))
    }

    // Re-check permission when the app resumes (e.g. user granted it from Settings).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = PermissionHelper.hasPermission(context)
                if (nowGranted && !hasPermission) {
                    hasPermission = true
                    onPhotosAccessGranted()
                } else {
                    hasPermission = nowGranted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) {
            hasPermission = true
            onPhotosAccessGranted()
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onRequestPermission = {
                permissionLauncher.launch(PermissionHelper.requiredPermissions())
            }
        )
    } else {
        GalleryContainer(
            uiState = uiState,
            allPhotos = allPhotos,
            albumPhotos = albumPhotos,
            searchPhotos = searchPhotos,
            onSearchQueryChange = onSearchQueryChange,
            onPhotoClick = onPhotoClick,
            onToggleFavorite = onToggleFavorite,
            onAlbumClick = onAlbumClick,
            onAlbumClear = { onAlbumClick(null) },
            onTabClick = onTabClick
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Proxa Gallery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Your memories, beautifully organized.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Allow Photos Access",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun GalleryContainer(
    uiState: GalleryUiState,
    allPhotos: LazyPagingItems<MediaItem>,
    albumPhotos: LazyPagingItems<MediaItem>,
    searchPhotos: LazyPagingItems<MediaItem>,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onAlbumClick: (Long?) -> Unit,
    onAlbumClear: () -> Unit,
    onTabClick: (Int) -> Unit
) {
    val homeGridState = rememberLazyStaggeredGridState()
    val searchGridState = rememberLazyStaggeredGridState()
    val favoriteGridState = rememberLazyStaggeredGridState()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val currentDate = remember {
        val calendar = Calendar.getInstance()
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> ""
        }
        val month = when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "January"
            Calendar.FEBRUARY -> "February"
            Calendar.MARCH -> "March"
            Calendar.APRIL -> "April"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "June"
            Calendar.JULY -> "July"
            Calendar.AUGUST -> "August"
            Calendar.SEPTEMBER -> "September"
            Calendar.OCTOBER -> "October"
            Calendar.NOVEMBER -> "November"
            Calendar.DECEMBER -> "December"
            else -> ""
        }
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        "$dayOfWeek, $month $dayOfMonth"
    }

    val favoriteItems = uiState.favoriteItems

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.selectedAlbumId != null) {
                // Inside an album view
                val selectedAlbum = uiState.albums.find { it.id == uiState.selectedAlbumId }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onAlbumClear) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            text = selectedAlbum?.displayName ?: "Album",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedAlbum?.itemCount ?: 0} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                PagedPhotoGridTab(
                    pagingItems = albumPhotos,
                    uiState = uiState,
                    onPhotoClick = onPhotoClick,
                    onToggleFavorite = onToggleFavorite
                )
            } else {
                // Root tabs view
                when (uiState.selectedTab) {
                    0 -> {
                        // Gallery Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = greeting,
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Emon",
                                fontSize = 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentDate,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        PagedPhotoGridTab(
                            pagingItems = allPhotos,
                            uiState = uiState,
                            gridState = homeGridState,
                            onPhotoClick = onPhotoClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    1 -> {
                        // Search Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                        ) {
                            GlassSearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChange,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            QuickSearchChips(
                                onChipClick = onSearchQueryChange
                            )

                            Spacer(Modifier.height(8.dp))
                        }

                        PagedPhotoGridTab(
                            pagingItems = searchPhotos,
                            uiState = uiState,
                            gridState = searchGridState,
                            onPhotoClick = onPhotoClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    2 -> {
                        // Albums Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "My Albums",
                                fontSize = 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AlbumGridTab(
                            albums = uiState.albums,
                            onAlbumClick = onAlbumClick
                        )
                    }
                    3 -> {
                        // Favorites Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Favorites",
                                fontSize = 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        PhotoGridTab(
                            uiState = uiState,
                            photos = favoriteItems,
                            gridState = favoriteGridState,
                            onPhotoClick = onPhotoClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                    4 -> {
                        // Settings Tab
                        SettingsTab(
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                }
            }
        }

        // Floating Bottom Navigation Bar
        if (uiState.selectedAlbumId == null) {
            FloatingBottomNavigation(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PagedPhotoGridTab(
    pagingItems: LazyPagingItems<MediaItem>,
    uiState: GalleryUiState,
    gridState: LazyStaggeredGridState? = null,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    val actualGridState = gridState ?: rememberLazyStaggeredGridState()

    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append

    when {
        refreshState is LoadState.Loading && pagingItems.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = refreshState.error.localizedMessage ?: "Unable to load images.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { pagingItems.retry() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
        refreshState is LoadState.NotLoading && pagingItems.itemCount == 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        else -> {
            LazyVerticalStaggeredGrid(
                state = actualGridState,
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = if (uiState.selectedAlbumId == null) 96.dp else 16.dp
                )
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.id ?: index.toLong() },
                    contentType = { index -> 
                        val item = pagingItems[index]
                        if (item?.isVideo == true) "video" else "image"
                    }
                ) { index ->
                    val item = pagingItems[index] ?: return@items
                    val favKey = if (item.isVideo) "v:${item.id}" else "i:${item.id}"
                    val isFav = favKey in uiState.favoriteKeys
                    PhotoGridItem(
                        item = item,
                        isFavorite = isFav,
                        onPhotoClick = onPhotoClick,
                        onToggleFavorite = onToggleFavorite
                    )
                }

                if (appendState is LoadState.Loading) {
                    item(
                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine,
                        contentType = { "loading_indicator" }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (appendState is LoadState.Error) {
                    item(
                        span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine,
                        contentType = { "error_indicator" }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = appendState.error.localizedMessage ?: "Failed to load more items.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { pagingItems.retry() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoGridTab(
    uiState: GalleryUiState,
    photos: List<MediaItem>,
    gridState: LazyStaggeredGridState? = null,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onLoadMore: () -> Unit = {}
) {
    val actualGridState = gridState ?: rememberLazyStaggeredGridState()

    LaunchedEffect(actualGridState) {
        snapshotFlow {
            val layoutInfo = actualGridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 6
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }

    when {
        uiState.isLoading && photos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        photos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
        else -> {
            LazyVerticalStaggeredGrid(
                state = actualGridState,
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = if (uiState.selectedAlbumId == null) 96.dp else 16.dp
                )
            ) {
                items(
                    items = photos,
                    key = { item -> item.id },
                    contentType = { item -> if (item.isVideo) "video" else "image" }
                ) { item ->
                    val favKey = if (item.isVideo) "v:${item.id}" else "i:${item.id}"
                    val isFav = favKey in uiState.favoriteKeys
                    PhotoGridItem(
                        item = item,
                        isFavorite = isFav,
                        onPhotoClick = onPhotoClick,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    item: MediaItem,
    isFavorite: Boolean,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        label = "photo_press_scale"
    )

    Box(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(
                if (item.width != null && item.height != null && item.width > 0 && item.height > 0)
                    item.width.toFloat() / item.height.toFloat()
                else 1f
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161A22))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onPhotoClick(item) }
                )
            }
    ) {
        val context = LocalContext.current
        val imageRequest = remember(item.uri) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(200)
                .crossfade(false)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onToggleFavorite(item.id, item.isVideo) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = if (isFavorite) Color(0xFFFF1744) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AlbumGridTab(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit
) {
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 96.dp
        )
    ) {
        items(
            items = albums,
            key = { album -> album.id },
            contentType = { "album" }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "album_press_scale"
    )

    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(album.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161A22)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val coverRequest = remember(album.coverPhotoUri) {
                ImageRequest.Builder(context)
                    .data(album.coverPhotoUri)
                    .size(384)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xB3090B10))
                    .border(
                        width = 1.dp,
                        color = Color(0x1AFFFFFF),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = album.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Pair(Icons.Rounded.Image, "Gallery"),
        Pair(Icons.Rounded.Search, "Search"),
        Pair(Icons.Rounded.Folder, "Albums"),
        Pair(Icons.Rounded.Favorite, "Favorites"),
        Pair(Icons.Rounded.Settings, "Settings")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xD010141E))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = constraints.maxWidth.toFloat()
            val density = LocalDensity.current
            val tabWidthDp = with(density) { (width / 5).toDp() }

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidthDp * selectedTab,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "nav_indicator"
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(tabWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = with(density) { indicatorOffset.toPx() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(index) }
                            )
                    ) {
                        if (index == 2) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else 
                                            Color(0x0FFFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0x15FFFFFF),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.first,
                                    contentDescription = item.second,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.first,
                                contentDescription = item.second,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0x20FFFFFF),
        label = "search_border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xC0161A24))
            .border(1.dp, borderColor, RoundedCornerShape(30.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search your memories...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 15.sp
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            Spacer(Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "AI Search",
                tint = if (isFocused) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        onQueryChange("sunset")
                    }
            )
        }
    }
}

@Composable
fun QuickSearchChips(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        "People", "Flowers", "Cats", "Cars", "Beach", 
        "Food", "Videos", "Documents", "Screenshots", "Memes", "Favorites"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onChipClick(chip.lowercase()) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chip,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsTab(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(24.dp))
        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161A22)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsItem(
                    icon = Icons.Rounded.Settings,
                    title = "App Version",
                    subtitle = "v1.0.0 (Premium One UI)"
                )
                HorizontalDivider(
                    color = Color(0x1AFFFFFF),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                SettingsItem(
                    icon = Icons.Rounded.Image,
                    title = "Developer",
                    subtitle = "Emon & Pair Programmer"
                )
                HorizontalDivider(
                    color = Color(0x1AFFFFFF),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                SettingsItem(
                    icon = Icons.Rounded.Favorite,
                    title = "Theme",
                    subtitle = "Dark Mode (Aesthetic #090B10)"
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
