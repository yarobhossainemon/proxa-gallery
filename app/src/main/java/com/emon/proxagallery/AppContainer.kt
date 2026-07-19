package com.emon.proxagallery

import android.content.Context
import com.emon.proxagallery.ai.AiDatabase
import com.emon.proxagallery.ai.AiIndexRepository
import com.emon.proxagallery.ai.AiIndexScheduler
import com.emon.proxagallery.ai.EmptyAiSearchRepository
import com.emon.proxagallery.ai.MediaChangeMonitor
import com.emon.proxagallery.data.GalleryRepository

class AppContainer(context: Context) {
    val galleryRepository: GalleryRepository = GalleryRepository(context)
    val aiDatabase: AiDatabase = AiDatabase.getInstance(context)
    val mediaChangeMonitor: MediaChangeMonitor = MediaChangeMonitor(galleryRepository)
    val aiIndexRepository: AiIndexRepository = AiIndexRepository(
        context = context,
        galleryRepository = galleryRepository,
        monitor = mediaChangeMonitor,
        aiDatabase = aiDatabase
    )
    val aiIndexScheduler: AiIndexScheduler = AiIndexScheduler(
        repository = aiIndexRepository,
        galleryRepository = galleryRepository,
        dao = aiDatabase.aiIndexDao()
    )
    val aiSearchRepository = EmptyAiSearchRepository()
}
