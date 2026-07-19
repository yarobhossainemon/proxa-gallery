package com.emon.proxagallery.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.emon.proxagallery.data.Album
import com.emon.proxagallery.data.AlbumCustomization
import com.emon.proxagallery.data.AlbumStats
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * Hidden Albums management screen (Samsung Gallery / Google Photos–style).
 *
 * Lists every album the user has hidden via [AlbumCustomization.isHidden].
 * It reuses the existing customization Flow from Room — no MediaStore scan,
 * no new tables. Each card shows the resolved cover/name/count and a small
 * "Hidden" badge, with an overflow menu for Unhide / Rename / Change Cover /
 * Reset Customization. All actions route through the shared [GalleryViewModel]
 * so they take effect instantly on both this screen and the Albums tab.
 */
@Composable
fun HiddenAlbumsScreen(
    hiddenAlbums: List<Album>,
    albumCustomizations: Map<Long, AlbumCustomization>,
    editingAlbum: Album?,
    editingAlbumStats: AlbumStats?,
    showEditAlbumDialog: Boolean,
    onBackClick: () -> Unit,
    onUnhideAlbum: (Long) -> Unit,
    onEditAlbum: (Album) -> Unit,
    onCancelEditAlbum: () -> Unit,
    onSaveAlbumRename: (String?) -> Unit,
    onSaveAlbumCover: (String) -> Unit,
    onResetAlbumCustomization: () -> Unit,
    onToggleAlbumPinned: () -> Unit,
    onToggleAlbumHidden: () -> Unit,
    onRequestAlbumCover: () -> Unit
) {
    Scaffold(
        topBar = {
            HiddenAlbumsTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hiddenAlbums.isEmpty()) {
                GeminiEmptyState(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "No Hidden Albums",
                    subtitle = "Albums you hide will appear here."
                )
            } else {
                HiddenAlbumsGrid(
                    albums = hiddenAlbums,
                    onUnhide = onUnhideAlbum,
                    onEditAlbum = onEditAlbum
                )
            }
        }
    }

    // Reuse the same Samsung One UI–style edit sheet used by the Albums tab.
    // Visibility is driven by the shared ViewModel state so the sheet behaves
    // identically regardless of which screen opened it.
    if (showEditAlbumDialog && editingAlbum != null) {
        EditAlbumBottomSheet(
            album = editingAlbum,
            customization = albumCustomizations[editingAlbum.id],
            stats = editingAlbumStats,
            onRename = onSaveAlbumRename,
            onChangeCover = { onRequestAlbumCover() },
            onTogglePinned = onToggleAlbumPinned,
            onToggleHidden = onToggleAlbumHidden,
            onResetCustomization = onResetAlbumCustomization,
            onDismiss = onCancelEditAlbum
        )
    }
}

@Composable
private fun HiddenAlbumsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Hidden Albums",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.weight(1f))
        // Balance the row so the title stays centered.
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun HiddenAlbumsGrid(
    albums: List<Album>,
    onUnhide: (Long) -> Unit,
    onEditAlbum: (Album) -> Unit
) {
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
            contentType = { "hidden_album" }
        ) { album ->
            HiddenAlbumCard(
                album = album,
                onUnhide = { onUnhide(album.id) },
                onEditAlbum = { onEditAlbum(album) }
            )
        }
    }
}

/**
 * A hidden album card. Mirrors the [AlbumCard] visual language (same shape,
 * overlay, press animation, themed shadow) and adds a small "Hidden" badge.
 */
@Composable
private fun HiddenAlbumCard(
    album: Album,
    onUnhide: () -> Unit,
    onEditAlbum: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hidden_album_press_scale"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 4.dp,
        label = "hidden_album_shadow_elevation"
    )

    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(20.dp),
                spotColor = MaterialTheme.extendedColors.cardShadowPrimary,
                ambientColor = MaterialTheme.extendedColors.cardShadowSecondary,
                clip = false
            )
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
                    onLongPress = { onEditAlbum() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val coverRequest = remember(album.coverUriToDisplay) {
                ImageRequest.Builder(context)
                    .data(album.coverUriToDisplay)
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

            // Dark overlay to match the Albums tab aesthetic.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // Small "Hidden" badge (top-start).
            HiddenBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )

            // Overflow menu (⋮) — top-end.
            var menuExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Album options",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    HiddenAlbumMenuItem(
                        icon = Icons.Rounded.Visibility,
                        label = "Unhide Album",
                        onClick = {
                            menuExpanded = false
                            onUnhide()
                        }
                    )
                    HiddenAlbumMenuItem(
                        icon = Icons.Rounded.Photo,
                        label = "Rename Album",
                        onClick = {
                            menuExpanded = false
                            onEditAlbum()
                        }
                    )
                    HiddenAlbumMenuItem(
                        icon = Icons.Rounded.Photo,
                        label = "Change Album Cover",
                        onClick = {
                            menuExpanded = false
                            onEditAlbum()
                        }
                    )
                    HiddenAlbumMenuItem(
                        icon = Icons.Rounded.Refresh,
                        label = "Reset Customization",
                        labelColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            menuExpanded = false
                            onEditAlbum()
                        }
                    )
                }
            }

            // Bottom label area.
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
                        text = album.displayNameToDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.itemCount} ${if (album.itemCount == 1) "item" else "items"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAlbumMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    color = labelColor
                )
            }
        },
        onClick = onClick
    )
}

/** Small pill-shaped badge that marks an album as hidden. */
@Composable
private fun HiddenBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.VisibilityOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Hidden",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
