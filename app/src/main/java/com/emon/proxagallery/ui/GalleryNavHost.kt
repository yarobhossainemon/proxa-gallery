package com.emon.proxagallery.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emon.proxagallery.data.Photo

private const val GALLERY_ROUTE = "gallery"
private const val PHOTO_VIEWER_ROUTE = "photo/{photoId}/{photoUri}"
private const val PHOTO_ID_ARGUMENT = "photoId"
private const val PHOTO_URI_ARGUMENT = "photoUri"

@Composable
fun GalleryNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = GALLERY_ROUTE,
        modifier = modifier
    ) {
        composable(GALLERY_ROUTE) {
            HomeScreen(
                onPhotoClick = { photo ->
                    navController.navigate(photoViewerRoute(photo))
                }
            )
        }
        composable(
            route = PHOTO_VIEWER_ROUTE,
            arguments = listOf(
                navArgument(PHOTO_ID_ARGUMENT) { type = NavType.LongType },
                navArgument(PHOTO_URI_ARGUMENT) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong(PHOTO_ID_ARGUMENT)
                ?: return@composable
            val encodedUri = backStackEntry.arguments?.getString(PHOTO_URI_ARGUMENT)
                ?: return@composable
            val photo = Photo(
                id = photoId,
                uri = Uri.parse(Uri.decode(encodedUri))
            )

            PhotoViewerScreen(
                photo = photo,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun photoViewerRoute(photo: Photo): String =
    "photo/${photo.id}/${Uri.encode(photo.uri.toString())}"
