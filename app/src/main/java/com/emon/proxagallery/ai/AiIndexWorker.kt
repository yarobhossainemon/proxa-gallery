package com.emon.proxagallery.ai

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as com.emon.proxagallery.ProxaGalleryApplication
        val container = application.appContainer
        val repository = container.aiIndexRepository
        val galleryRepository = container.galleryRepository
        val photoIds = inputData.getLongArray(KEY_PHOTO_IDS)?.toList().orEmpty()
        if (photoIds.isEmpty()) return Result.success()

        Log.d(TAG, "index started batch=${inputData.getInt(KEY_BATCH_INDEX, 0)} size=${photoIds.size}")

        return try {
            withContext(Dispatchers.IO) {
                photoIds.chunked(PROGRESS_CHUNK_SIZE).forEach { chunk ->
                    if (isStopped) {
                        Log.d(TAG, "worker resumed after cancellation")
                        return@withContext
                    }
                    chunk.forEach { photoId ->
                        val details = galleryRepository.getMediaDetails(photoId)
                        if (details == null) {
                            repository.markFailed(photoId)
                        } else {
                            repository.markIndexed(
                                photoId = photoId,
                                uri = details.uri.toString(),
                                sourceModifiedAtMs = details.dateModifiedSec?.times(1000L)
                            )
                        }
                    }
                    setProgress(
                        androidx.work.workDataOf(
                            KEY_PROGRESS to chunk.size,
                            KEY_TOTAL to photoIds.size
                        )
                    )
                    Log.d(TAG, "batch completed size=${chunk.size}")
                }
            }
            Result.success()
        } catch (exception: Exception) {
            Log.e(TAG, "index failed", exception)
            Result.retry()
        }
    }

    companion object {
        const val KEY_PHOTO_IDS = "photo_ids"
        const val KEY_AI_VERSION = "ai_version"
        const val KEY_BATCH_INDEX = "batch_index"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        private const val PROGRESS_CHUNK_SIZE = 10
        private const val TAG = "AiIndexWorker"
    }
}
