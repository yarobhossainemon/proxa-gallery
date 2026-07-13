package com.emon.proxagallery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.distinctUntilChanged
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.ui.theme.ProxaGalleryTheme
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: GalleryUiState,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onPhotosAccessGranted = onPhotosAccessGranted,
        onSearchQueryChange = onSearchQueryChange,
        onPhotoClick = onPhotoClick,
        onToggleFavorite = onToggleFavorite,
        onAlbumClick = onAlbumClick,
        onLoadMore = onLoadMore
    )
}

@Composable
private fun HomeScreenContent(
    uiState: GalleryUiState,
    onPhotosAccessGranted: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit = { _, _ -> },
    onAlbumClick: (Long?) -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.all { it.value }) {
            onPhotosAccessGranted()
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onRequestPermission = {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionLauncher.launch(permissions)
            }
        )
    } else {
        GalleryContainer(
            uiState = uiState,
            onSearchQueryChange = onSearchQueryChange,
            onPhotoClick = onPhotoClick,
            onToggleFavorite = onToggleFavorite,
            onAlbumClick = onAlbumClick,
            onAlbumClear = { onAlbumClick(null) },
            onLoadMore = onLoadMore
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Proxa Gallery",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Your memories, beautifully organized.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = onRequestPermission) {
            Text("Allow Photos Access")
        }
    }
}

@Composable
private fun GalleryContainer(
    uiState: GalleryUiState,
    onSearchQueryChange: (String) -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onAlbumClick: (Long?) -> Unit,
    onAlbumClear: () -> Unit,
    onLoadMore: () -> Unit = {}
) {
    var query by remember(uiState.searchQuery) { mutableStateOf(uiState.searchQuery) }
    val homeGridState = rememberLazyGridState()
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.selectedAlbumId == null) {
            Text(
                text = "Proxa Gallery",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )

            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))
        } else {
            val selectedAlbum = uiState.albums.find { it.id == uiState.selectedAlbumId }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAlbumClear) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to albums"
                    )
                }

                Text(
                    text = selectedAlbum?.displayName ?: "Album",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                onSearchQueryChange(newValue)
            },
            placeholder = { Text("Search photos...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        if (uiState.selectedAlbumId == null) {
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            val tabTitles = listOf("Photos", "Albums")

            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PhotoGridTab(
                    uiState = uiState,
                    gridState = homeGridState,
                    onPhotoClick = onPhotoClick,
                    onToggleFavorite = onToggleFavorite,
                    onLoadMore = onLoadMore
                )
                1 -> AlbumGridTab(
                    albums = uiState.albums,
                    onAlbumClick = onAlbumClick
                )
            }
        } else {
            PhotoGridTab(
                uiState = uiState,
                onPhotoClick = onPhotoClick,
                onToggleFavorite = onToggleFavorite,
                onLoadMore = onLoadMore
            )
        }
    }
}

@Composable
private fun PhotoGridTab(
    uiState: GalleryUiState,
    gridState: LazyGridState? = null,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onLoadMore: () -> Unit = {}
) {
    val actualGridState = gridState ?: rememberLazyGridState()

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
        uiState.isLoading && uiState.photos.isEmpty() -> {
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
        else -> {
            LazyVerticalGrid(
                state = actualGridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.photos, key = { item -> item.id }) { item ->
                    val favKey = if (item.isVideo) "v:${item.id}" else "i:${item.id}"

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.uri)
                                .crossfade(true)
                                .size(256)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPhotoClick(item) }
                        )

                        if (item.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = if (favKey in uiState.favoriteKeys) {
                                Color(0xFFFF1744)
                            } else {
                                Color.White.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clickable { onToggleFavorite(item.id, item.isVideo) }
                        )
                    }
                }
            }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(albums, key = { album -> album.id }) { album ->
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
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.coverPhotoUri)
                        .crossfade(true)
                        .size(256)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ProxaGalleryTheme {
        HomeScreenContent(
            uiState = GalleryUiState(isLoading = false),
            onPhotosAccessGranted = {},
            onSearchQueryChange = {},
            onPhotoClick = {}
        )
    }
}
