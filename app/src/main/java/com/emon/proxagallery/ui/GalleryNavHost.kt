package com.emon.proxagallery.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.util.ArrayList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems

private const val GALLERY_ROUTE = "gallery"
private const val PHOTO_VIEWER_ROUTE = "photo/{photoId}"
private const val PHOTO_ID_ARGUMENT = "photoId"
private const val RECENTLY_DELETED_ROUTE = "recently_deleted"
private const val HIDDEN_ALBUMS_ROUTE = "hidden_albums"
private const val FAVORITES_ROUTE = "favorites"

@Composable
fun GalleryNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val factory = remember(context) { GalleryViewModelFactory(context.applicationContext) }
    val viewModel: GalleryViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    val allPhotos = viewModel.allPhotosFlow.collectAsLazyPagingItems()
    val albumPhotos = viewModel.albumPhotosFlow.collectAsLazyPagingItems()
    val searchPhotos = viewModel.searchPhotosFlow.collectAsLazyPagingItems()

    val currentTheme by viewModel.themeMode
        .collectAsStateWithLifecycle(initialValue = com.emon.proxagallery.data.ThemeMode.SYSTEM_DEFAULT)
    val currentAccent by viewModel.accentColor
        .collectAsStateWithLifecycle(initialValue = com.emon.proxagallery.data.AccentColor.BLUE)

    val snackbarHostState = remember { SnackbarHostState() }

    // State that the delete permission launcher needs to communicate back to the ViewModel.
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteRetryUri by remember { mutableStateOf<Uri?>(null) }
    // Tracks whether the in-flight delete dialog is a bulk (multi-select) delete.
    var isBulkDeletePending by remember { mutableStateOf(false) }

    // Launcher for the OS-level delete-permission dialog (Android 11+).
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Only proceed if the user explicitly confirmed the system dialog.
        if (result.resultCode == Activity.RESULT_OK) {
            if (isBulkDeletePending) {
                viewModel.confirmBulkDeleteAfterPermission(pendingDeleteRetryUri)
                isBulkDeletePending = false
            } else {
                val id = pendingDeleteId
                if (id != null) {
                    viewModel.confirmDeleteAfterPermission(id, pendingDeleteRetryUri)
                }
            }
            pendingDeleteId = null
            pendingDeleteRetryUri = null
        } else {
            // User cancelled — roll back Room records and clean up copied files.
            if (isBulkDeletePending) {
                viewModel.cancelBulkDelete()
                isBulkDeletePending = false
            } else {
                val id = pendingDeleteId
                if (id != null) {
                    viewModel.cancelDelete(id)
                }
            }
            pendingDeleteId = null
            pendingDeleteRetryUri = null
        }
    }

    // Launcher for the album-cover photo picker.
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveAlbumCover(uri.toString())
        }
    }

    // Consume one-shot viewer effects.
    LaunchedEffect(Unit) {
        viewModel.viewerEffects.collect { effect ->
            when (effect) {
                is ViewerEffect.LaunchSystemDeleteDialog -> {
                    // Mark this as a bulk delete so the launcher callback routes to
                    // confirmBulkDeleteAfterPermission instead of the single-photo path.
                    isBulkDeletePending = true
                    pendingDeleteRetryUri = effect.retryUri
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
                }
                is ViewerEffect.SharePhotos -> {
                    if (effect.uris.isNotEmpty()) {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND_MULTIPLE
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(effect.uris))
                            type = "*/*"
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share photos")
                        )
                    }
                }
                is ViewerEffect.NavigateBack -> {
                    navController.popBackStack()
                    allPhotos.refresh()
                    albumPhotos.refresh()
                    searchPhotos.refresh()
                }
                is ViewerEffect.PhotoDeleted -> {
                    allPhotos.refresh()
                    albumPhotos.refresh()
                    searchPhotos.refresh()
                }
                is ViewerEffect.GalleryRefresh -> {
                    allPhotos.refresh()
                    albumPhotos.refresh()
                    searchPhotos.refresh()
                }
                is ViewerEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        NavHost(
            modifier = modifier.padding(paddingValues),
            navController = navController,
            startDestination = GALLERY_ROUTE,
        ) {
        composable(GALLERY_ROUTE) {
            HomeScreen(
                uiState = uiState,
                allPhotos = allPhotos,
                albumPhotos = albumPhotos,
                searchPhotos = searchPhotos,
                onPhotosAccessGranted = viewModel::loadPhotos,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onPhotoClick = { photo ->
                    viewModel.prepareViewer(photo.id)
                    navController.navigate(photoViewerRoute(photo.id))
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onAlbumClick = viewModel::selectAlbum,
                onTabClick = viewModel::selectTab,
                onAlbumSortChange = viewModel::setAlbumSortOption,
                onPhotoSortChange = viewModel::setPhotoSortOption,
                onRecentlyDeletedClick = {
                    navController.navigate(RECENTLY_DELETED_ROUTE)
                },
                onEnterSelectionMode = viewModel::enterSelectionMode,
                onTogglePhotoSelection = viewModel::togglePhotoSelection,
                onSelectPhoto = viewModel::selectPhoto,
                onExitSelectionMode = viewModel::exitSelectionMode,
                onSelectAllVisible = viewModel::selectAllOrNone,
                onShareSelected = viewModel::shareSelected,
                onFavoriteSelected = viewModel::bulkToggleFavorite,
                onRequestBulkDelete = viewModel::requestBulkDelete,
                onConfirmBulkDelete = viewModel::confirmBulkDelete,
                onCancelBulkDelete = viewModel::cancelBulkDelete,
                showDeleteConfirmation = uiState.showDeleteConfirmation,
                currentTheme = currentTheme,
                onThemeChange = viewModel::setThemeMode,
                currentAccent = currentAccent,
                onAccentChange = viewModel::setAccentColor,
                onEditAlbum = viewModel::onEditAlbum,
                onCancelEditAlbum = viewModel::onCancelEditAlbum,
                onSaveAlbumRename = viewModel::saveAlbumRename,
                onSaveAlbumCover = viewModel::saveAlbumCover,
                onResetAlbumCustomization = viewModel::resetAlbumCustomization,
                onToggleAlbumPinned = { uiState.editingAlbum?.id?.let { viewModel.toggleAlbumPinned(it) } },
                onToggleAlbumHidden = { uiState.editingAlbum?.id?.let { viewModel.toggleAlbumHidden(it) } },
                onRequestAlbumCover = {
                    coverPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onHiddenAlbumsClick = {
                    navController.navigate(HIDDEN_ALBUMS_ROUTE)
                },
                onFavoritesClick = {
                    navController.navigate(FAVORITES_ROUTE)
                }
            )
        }
        composable(
            route = PHOTO_VIEWER_ROUTE,
            arguments = listOf(
                navArgument(PHOTO_ID_ARGUMENT) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong(PHOTO_ID_ARGUMENT)
                ?: return@composable

            PhotoViewerScreen(
                itemIds = uiState.viewerPhotoIds,
                initialItemId = photoId,
                resolveViewerItem = { id -> viewModel.getMediaItem(id)?.toViewerItem() },
                onBackClick = { navController.popBackStack() },
                favoriteKeys = uiState.favoriteKeys,
                albums = uiState.albums,
                onToggleFavorite = viewModel::toggleFavorite,
                onDeletePhoto = { id, uri ->
                    pendingDeleteId = id
                    pendingDeleteRetryUri = null
                    viewModel.deleteCurrentPhoto(id, uri)
                },
                getMediaItem = viewModel::getMediaItem,
                getMediaDetails = viewModel::getMediaDetails
            )
        }
        composable(RECENTLY_DELETED_ROUTE) {
            RecentlyDeletedScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onItemRestored = { viewModel.refreshAfterExternalChange() }
            )
        }
        composable(HIDDEN_ALBUMS_ROUTE) {
            HiddenAlbumsScreen(
                hiddenAlbums = uiState.hiddenAlbums,
                albumCustomizations = uiState.albumCustomizations,
                editingAlbum = uiState.editingAlbum,
                editingAlbumStats = uiState.editingAlbumStats,
                showEditAlbumDialog = uiState.showEditAlbumDialog,
                onBackClick = { navController.popBackStack() },
                onUnhideAlbum = viewModel::unhideAlbum,
                onEditAlbum = viewModel::onEditHiddenAlbum,
                onCancelEditAlbum = viewModel::onCancelEditAlbum,
                onSaveAlbumRename = viewModel::saveAlbumRename,
                onSaveAlbumCover = viewModel::saveAlbumCover,
                onResetAlbumCustomization = viewModel::resetAlbumCustomization,
                onToggleAlbumPinned = { uiState.editingAlbum?.id?.let { viewModel.toggleAlbumPinned(it) } },
                onToggleAlbumHidden = { uiState.editingAlbum?.id?.let { viewModel.toggleAlbumHidden(it) } },
                onRequestAlbumCover = {
                    coverPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onPhotoClick = { photo ->
                    viewModel.prepareFavoritesViewer()
                    navController.navigate(photoViewerRoute(photo.id))
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onEnterSelectionMode = viewModel::enterSelectionMode,
                onTogglePhotoSelection = viewModel::togglePhotoSelection,
                onSelectPhoto = viewModel::selectPhoto,
                onExitSelectionMode = viewModel::exitSelectionMode,
                onSelectAllVisible = viewModel::selectAllFavoritesOrNone,
                onShareSelected = viewModel::shareSelected,
                onFavoriteSelected = viewModel::bulkToggleFavorite,
                onRequestBulkDelete = viewModel::requestBulkDelete,
                onConfirmBulkDelete = viewModel::confirmBulkDelete,
                onCancelBulkDelete = viewModel::cancelBulkDelete,
                showDeleteConfirmation = uiState.showDeleteConfirmation,
                onPhotoSortChange = viewModel::setPhotoSortOption
            )
        }
        }
    }
}

private fun photoViewerRoute(photoId: Long): String = "photo/$photoId"
