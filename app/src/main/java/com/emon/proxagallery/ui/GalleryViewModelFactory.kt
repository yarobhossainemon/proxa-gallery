package com.emon.proxagallery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emon.proxagallery.data.FavoritesRepository
import com.emon.proxagallery.data.GalleryRepository

class GalleryViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        @Suppress("UNCHECKED_CAST")
        return GalleryViewModel(
            GalleryRepository(appContext),
            FavoritesRepository(appContext)
        ) as T
    }
}
