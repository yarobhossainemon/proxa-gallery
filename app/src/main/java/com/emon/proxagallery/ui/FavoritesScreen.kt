package com.emon.proxagallery.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emon.proxagallery.data.MediaItem

/**
 * Favorites screen — a thin host around the reusable [MediaGrid].
 *
 * The favorites data itself is owned by the shared [GalleryViewModel]
 * ([GalleryUiState.favoriteItems], kept in sync with the photo sort option by
 * the ViewModel's favorites observer). This screen only wires that data into
 * [MediaGrid] and forwards every selection / share / delete / favorite action
 * back to the same ViewModel — no business logic is duplicated, and selection
 * state is the same [GalleryUiState] used across the app.
 *
 * Opening the photo viewer from here calls [GalleryViewModel.prepareFavoritesViewer]
 * so the viewer's swipe ordering matches this grid.
 *
 * This is the template future photo-grid destinations (Videos, Screenshots,
 * Documents, Downloads, AI Collections, …) will follow: supply a list + a title +
 * empty-state copy, reuse [MediaGrid].
 */
@Composable
fun FavoritesScreen(
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
    onPhotoSortChange: (com.emon.proxagallery.data.PhotoSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    MediaGrid(
        title = "Favorites",
        photos = uiState.favoriteItems,
        uiState = uiState,
        onBackClick = onBackClick,
        onPhotoClick = onPhotoClick,
        onToggleFavorite = onToggleFavorite,
        onEnterSelectionMode = onEnterSelectionMode,
        onTogglePhotoSelection = onTogglePhotoSelection,
        onSelectPhoto = onSelectPhoto,
        onExitSelectionMode = onExitSelectionMode,
        onSelectAllVisible = onSelectAllVisible,
        onShareSelected = onShareSelected,
        onFavoriteSelected = onFavoriteSelected,
        onRequestBulkDelete = onRequestBulkDelete,
        onConfirmBulkDelete = onConfirmBulkDelete,
        onCancelBulkDelete = onCancelBulkDelete,
        showDeleteConfirmation = showDeleteConfirmation,
        onPhotoSortChange = onPhotoSortChange,
        emptyIcon = Icons.Rounded.Favorite,
        emptyTitle = "No favorites yet",
        emptySubtitle = "Tap the heart on any photo or video to add it here.",
        modifier = modifier
    )
}
