package com.emon.proxagallery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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

    NavHost(
        navController = navController,
        startDestination = GALLERY_ROUTE,
        modifier = modifier
    ) {
        composable(GALLERY_ROUTE) {
            HomeScreen(
                uiState = uiState,
                onPhotosAccessGranted = viewModel::loadPhotos,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onPhotoClick = { photo ->
                    navController.navigate(photoViewerRoute(photo.id))
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
                photos = uiState.allPhotos,
                initialPhotoId = photoId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun photoViewerRoute(photoId: Long): String = "photo/$photoId"
