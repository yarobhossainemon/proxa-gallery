package com.emon.proxagallery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emon.proxagallery.data.GalleryRepository
import com.emon.proxagallery.data.TrashRepository

class TrashViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TrashViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        @Suppress("UNCHECKED_CAST")
        return TrashViewModel(
            trashRepository = TrashRepository(appContext),
            galleryRepository = GalleryRepository(appContext)
        ) as T
    }
}
