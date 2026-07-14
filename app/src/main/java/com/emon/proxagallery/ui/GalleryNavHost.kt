package com.emon.proxagallery.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // State that the delete permission launcher needs to communicate back to the ViewModel.
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteRetryUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for the OS-level delete-permission dialog (Android 11+).
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Only proceed if the user explicitly confirmed the system dialog.
        if (result.resultCode == Activity.RESULT_OK) {
            val id = pendingDeleteId
            if (id != null) {
                viewModel.confirmDeleteAfterPermission(id, pendingDeleteRetryUri)
                pendingDeleteId = null
                pendingDeleteRetryUri = null
            }
        } else {
            // User cancelled — discard pending state, leave UI and Room unchanged.
            pendingDeleteId = null
            pendingDeleteRetryUri = null
        }
    }

    // Consume one-shot viewer effects.
    LaunchedEffect(Unit) {
        viewModel.viewerEffects.collect { effect ->
            when (effect) {
                is ViewerEffect.LaunchSystemDeleteDialog -> {
                    // pendingDeleteId is already set in onDeletePhoto before this effect fires.
                    // Only the retryUri (Android 10 path) comes from the effect itself.
                    pendingDeleteRetryUri = effect.retryUri
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(effect.intentSender).build()
                    )
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
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = GALLERY_ROUTE,
        modifier = modifier
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
                onTabClick = viewModel::selectTab
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
                photoIds = uiState.viewerPhotoIds,
                initialPhotoId = photoId,
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
    }
}

private fun photoViewerRoute(photoId: Long): String = "photo/$photoId"
