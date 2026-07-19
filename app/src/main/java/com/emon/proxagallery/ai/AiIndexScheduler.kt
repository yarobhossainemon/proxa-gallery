package com.emon.proxagallery.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiIndexScheduler(
    private val repository: AiIndexRepository,
    private val galleryRepository: com.emon.proxagallery.data.GalleryRepository,
    private val dao: AiIndexDao,
    private val aiVersion: Int = AiIndexRepository.CURRENT_AI_VERSION
) {
    suspend fun scheduleInitialIndexing() {
        repository.enqueueIndexing()
    }

    suspend fun scheduleChanges() {
        val currentIds = withContext(Dispatchers.IO) {
            galleryRepository.getAllPhotoIds().toSet()
        }
        val stored = withContext(Dispatchers.IO) {
            dao.getOutdatedIndexes(Int.MAX_VALUE).map { it.photoId }.toSet() +
                dao.getPendingIndexes(aiVersion, Int.MAX_VALUE).map { it.photoId }
        }
        val deletedIds = stored - currentIds
        deletedIds.forEach { photoId ->
            withContext(Dispatchers.IO) { dao.deleteIndex(photoId) }
        }
        val newOrOutdatedIds = withContext(Dispatchers.IO) {
            currentIds.filter { photoId ->
                val state = dao.getIndex(photoId)
                state == null ||
                    state.aiVersion < aiVersion ||
                    state.sourceModifiedAtMs != galleryRepository.getMediaDetails(photoId)?.dateModifiedSec?.times(1000L)
            }
        }
        if (newOrOutdatedIds.isNotEmpty()) {
            Log.d(TAG, "index started changes=${newOrOutdatedIds.size}")
            repository.enqueuePhotoIds(newOrOutdatedIds)
        }
    }

    suspend fun scheduleSingle(photoId: Long) {
        repository.enqueueSingleImage(photoId)
    }

    suspend fun scheduleReindex() {
        repository.enqueueReindex()
    }

    companion object {
        private const val TAG = "AiIndexScheduler"
    }
}
