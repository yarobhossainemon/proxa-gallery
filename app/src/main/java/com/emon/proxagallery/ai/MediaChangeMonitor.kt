package com.emon.proxagallery.ai

import com.emon.proxagallery.data.GalleryRepository
import kotlinx.coroutines.flow.Flow

class MediaChangeMonitor(
    galleryRepository: GalleryRepository
) {
    val changes: Flow<Long> = galleryRepository.mediaStoreChanges
}
