package com.emon.proxagallery.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.emon.proxagallery.data.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiIndexRepository(
    context: Context,
    private val galleryRepository: GalleryRepository,
    private val monitor: MediaChangeMonitor,
    private val aiDatabase: AiDatabase,
    private val aiVersion: Int = CURRENT_AI_VERSION
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val dao = aiDatabase.aiIndexDao()

    suspend fun enqueueIndexing() {
        val photoIds = withContext(Dispatchers.IO) {
            galleryRepository.getAllPhotoIds()
        }
        enqueuePhotoIds(photoIds)
    }

    suspend fun enqueueReindex() {
        val outdatedIds = withContext(Dispatchers.IO) {
            dao.getOutdatedIndexes(aiVersion).map { it.photoId }
        }
        enqueuePhotoIds(outdatedIds)
    }

    suspend fun enqueueSingleImage(photoId: Long) {
        enqueuePhotoIds(listOf(photoId))
    }

    suspend fun markIndexed(photoId: Long, uri: String, sourceModifiedAtMs: Long?) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            dao.upsertMediaIndex(
                AiMediaIndexEntity(
                    photoId = photoId,
                    uri = uri,
                    indexedAt = now,
                    aiVersion = aiVersion,
                    indexingStatus = AiIndexStatus.INDEXED,
                    sourceModifiedAtMs = sourceModifiedAtMs,
                    updatedAt = now
                )
            )
            Log.d(TAG, "index completed photoId=$photoId")
        }
    }

    suspend fun markFailed(photoId: Long) {
        withContext(Dispatchers.IO) {
            dao.upsertMediaIndex(
                AiMediaIndexEntity(
                    photoId = photoId,
                    uri = "",
                    aiVersion = aiVersion,
                    indexingStatus = AiIndexStatus.FAILED,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "index failed photoId=$photoId")
        }
    }

    suspend fun queryIndexState(photoId: Long): AiIndexState? {
        return withContext(Dispatchers.IO) {
            dao.getIndexState(photoId)
        }
    }

    suspend fun detectAndScheduleChanges() {
        val currentIds = withContext(Dispatchers.IO) {
            galleryRepository.getAllPhotoIds().toSet()
        }
        val cachedIds = withContext(Dispatchers.IO) {
            dao.getOutdatedIndexes(Int.MAX_VALUE).map { it.photoId }.toSet() + dao.getPendingIndexes(aiVersion, Int.MAX_VALUE).map { it.photoId }
        }
        val deletedIds = cachedIds - currentIds
        deletedIds.forEach { photoId ->
            withContext(Dispatchers.IO) { dao.deleteIndex(photoId) }
        }
        val newOrChangedIds = currentIds - cachedIds
        enqueuePhotoIds(newOrChangedIds.toList())
    }

    fun currentIndexingMonitor(): kotlinx.coroutines.flow.Flow<Long> = monitor.changes

    suspend fun enqueuePhotoIds(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            photoIds.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
                val request = OneTimeWorkRequestBuilder<AiIndexWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresCharging(true)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .setInputData(
                        workDataOf(
                            AiIndexWorker.KEY_PHOTO_IDS to batch.toLongArray(),
                            AiIndexWorker.KEY_AI_VERSION to aiVersion,
                            AiIndexWorker.KEY_BATCH_INDEX to batchIndex
                        )
                    )
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                workManager.enqueueUniqueWork(
                    workNameFor(batch),
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
            }
        }
    }

    companion object {
        const val CURRENT_AI_VERSION = 1
        private const val BATCH_SIZE = 50
        private const val TAG = "AiIndexRepository"
    }
}

private fun List<Long>.toLongArray(): LongArray = LongArray(size) { index -> get(index) }

private fun workNameFor(batch: List<Long>): String =
    "ai-index-" + batch.joinToString(separator = "_")
