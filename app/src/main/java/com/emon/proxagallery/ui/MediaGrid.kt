package com.emon.proxagallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.PhotoSortOption

/**
 * Reusable staggered photo-grid component used by every "list of photos"
 * destination that is NOT one of the root bottom-nav tabs.
 *
 * Hosts (each is a thin screen that supplies its own [photos]):
 * - [FavoritesScreen]
 * - future Videos / Screenshots / Documents / Downloads / AI Collections /
 *   Similar Photos / Smart Albums screens.
 *
 * Renders a Samsung One UI-style top bar (back + title + optional sort menu),
 * drag-to-select, the selection top/bottom bars, an empty state, and the bulk-
 * delete confirmation dialog. All selection/share/delete actions forward to the
 * shared [GalleryViewModel] through the supplied callbacks — no business logic
 * is duplicated, and selection state lives in the same [GalleryUiState] the rest
 * of the app uses.
 *
 * Not used for Hidden Albums or Recently Deleted: those render `Album`/`TrashItem`
 * (not [MediaItem]), expose domain-specific actions (unhide, restore, delete
 * forever), and use [ViewerMode.TRASH]. Forcing them in here would over-
 * parameterize this component.
 *
 * @param photos the synchronous, already-sorted list to display.
 * @param showSortMenu when false, hides the photo-sort dropdown (e.g. for a
 *   destination whose ordering is fixed).
 */
@Composable
fun MediaGrid(
    title: String,
    photos: List<MediaItem>,
    uiState: GalleryUiState,
    onBackClick: () -> Unit,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    onTogglePhotoSelection: (Long) -> Unit,
    onSelectPhoto: (Long) -> Unit,
    onExitSelectionMode: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onShareSelected: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onRequestBulkDelete: () -> Unit,
    onConfirmBulkDelete: () -> Unit,
    onCancelBulkDelete: () -> Unit,
    showDeleteConfirmation: Boolean,
    onPhotoSortChange: (PhotoSortOption) -> Unit,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String,
    modifier: Modifier = Modifier,
    showSortMenu: Boolean = true
) {
    // While a selection is active, the first Back press clears it without
    // navigating — mirrors the behavior of the root tabs.
    BackHandler(enabled = uiState.selectedPhotoIds.isNotEmpty()) {
        onExitSelectionMode()
    }

    val gridState = rememberLazyStaggeredGridState()
    val hasSelection = uiState.selectedPhotoIds.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            // Suppress the default status-bar inset; the header applies its own
            // .statusBarsPadding() so the inset is not doubled.
            contentWindowInsets = WindowInsets(0),
            // Selection action bar — same Samsung-style floating bar used by the
            // root tabs, shown only while a selection is active.
            bottomBar = {
                val allSelectedAreFav = remember(uiState.selectedPhotoIds, uiState.favoriteKeys) {
                    allSelectedAreFavorite(uiState)
                }
                AnimatedVisibility(
                    visible = hasSelection,
                    enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                        slideInVertically(animationSpec = tween(durationMillis = 280)) { it },
                    exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                        slideOutVertically(animationSpec = tween(durationMillis = 240)) { it }
                ) {
                    SelectionBottomBar(
                        selectedCount = uiState.selectedPhotoIds.size,
                        allSelectedAreFavorite = allSelectedAreFav,
                        onFavorite = onFavoriteSelected,
                        onShare = onShareSelected,
                        onDelete = onRequestBulkDelete
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // One top bar slot: title header in normal mode, selection bar
                // in selection mode — same position, no gap, no jump.
                TopBarSlot(
                    isSelectionMode = uiState.isSelectionMode,
                    normalHeader = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${photos.size} ${if (photos.size == 1) "item" else "items"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (showSortMenu) {
                                PhotoSortMenuButton(
                                    selectedOption = uiState.photoSortOption,
                                    onOptionSelected = onPhotoSortChange
                                )
                            }
                        }
                    },
                    selectionTopBar = {
                        SelectionTopBar(
                            selectedCount = uiState.selectedPhotoIds.size,
                            onClose = onExitSelectionMode
                        )
                    }
                )

                MediaGridContent(
                    photos = photos,
                    uiState = uiState,
                    gridState = gridState,
                    onPhotoClick = onPhotoClick,
                    onToggleFavorite = onToggleFavorite,
                    onEnterSelectionMode = onEnterSelectionMode,
                    onTogglePhotoSelection = onTogglePhotoSelection,
                    onSelectPhoto = onSelectPhoto,
                    emptyIcon = emptyIcon,
                    emptyTitle = emptyTitle,
                    emptySubtitle = emptySubtitle
                )
            }
        }

        // Bulk-delete confirmation — visibility driven by ViewModel state.
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = onCancelBulkDelete,
                title = {
                    Text(
                        text = "Delete ${uiState.selectedPhotoIds.size} photo" +
                            if (uiState.selectedPhotoIds.size == 1) "?" else "s?",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "This will move the selected photos to Recently Deleted.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmBulkDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelBulkDelete) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * The grid body of [MediaGrid]. Handles loading / error / empty states and the
 * staggered grid itself, reusing the shared [PhotoGridItem] and [GeminiEmptyState]
 * so every photo destination looks identical.
 */
@Composable
private fun MediaGridContent(
    photos: List<MediaItem>,
    uiState: GalleryUiState,
    gridState: LazyStaggeredGridState,
    onPhotoClick: (MediaItem) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onEnterSelectionMode: (Long) -> Unit,
    onTogglePhotoSelection: (Long) -> Unit,
    onSelectPhoto: (Long) -> Unit,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String
) {
    val currentOnPhotoClick by rememberUpdatedState(onPhotoClick)
    val currentOnToggleFavorite by rememberUpdatedState(onToggleFavorite)
    val currentOnEnterSelectionMode by rememberUpdatedState(onEnterSelectionMode)
    val currentOnTogglePhotoSelection by rememberUpdatedState(onTogglePhotoSelection)
    val currentOnSelectPhoto by rememberUpdatedState(onSelectPhoto)

    val handlePhotoClick = remember { { item: MediaItem -> currentOnPhotoClick(item) } }
    val handleToggleFavorite = remember { { id: Long, isFav: Boolean -> currentOnToggleFavorite(id, isFav) } }
    val handleEnterSelectionMode = remember { { id: Long -> currentOnEnterSelectionMode(id) } }
    val handleTogglePhotoSelection = remember { { id: Long -> currentOnTogglePhotoSelection(id) } }
    val handleSelectPhoto = remember { { id: Long -> currentOnSelectPhoto(id) } }

    val density = LocalDensity.current
    // Compute cell size once per composition — 3-column grid → screenWidth / 3.
    val configuration = LocalConfiguration.current
    val thumbnailSizePx = with(density) {
        (configuration.screenWidthDp.dp / 3).roundToPx().coerceIn(120, 600)
    }
    val dragModifier = Modifier.dragSelection(
        gridState = gridState,
        density = density,
        resolveId = { index -> photos.getOrNull(index)?.id },
        onSelectPhoto = handleSelectPhoto,
        onUnselectPhoto = handleTogglePhotoSelection
    )

    when {
        uiState.isLoading && photos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GeminiLoadingRing()
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
            GeminiEmptyState(
                icon = emptyIcon,
                title = emptyTitle,
                subtitle = emptySubtitle
            )
        }
        else -> {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().then(dragModifier),
                contentPadding = PaddingValues(
                    start = 4.dp,
                    end = 4.dp,
                    top = 4.dp,
                    bottom = 16.dp
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
                        thumbnailSizePx = thumbnailSizePx,
                        onPhotoClick = handlePhotoClick,
                        onToggleFavorite = handleToggleFavorite,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected = item.id in uiState.selectedPhotoIds,
                        onEnterSelectionMode = handleEnterSelectionMode,
                        onTogglePhotoSelection = handleTogglePhotoSelection
                    )
                }
            }
        }
    }
}
