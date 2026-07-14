package com.emon.proxagallery.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

class TrashRepository(
    context: Context
) {
    private val dao = TrashDatabase.getInstance(context).trashDao()

    suspend fun moveToTrash(
        mediaId: Long,
        uri: Uri,
        displayName: String,
        mimeType: String,
        originalAlbum: String?
    ) {
        val now = System.currentTimeMillis()
        val item = TrashItem(
            mediaId = mediaId,
            uri = uri.toString(),
            displayName = displayName,
            mimeType = mimeType,
            originalAlbum = originalAlbum,
            deletedAt = now,
            expiresAt = now + TrashItem.TRASH_RETENTION_MS,
            isVideo = mimeType.startsWith("video/"),
            thumbnailUri = uri.toString()
        )
        dao.insert(item)
    }

    suspend fun restoreFromTrash(mediaId: Long) {
        dao.deleteByMediaId(mediaId)
    }

    fun getTrashItems(): Flow<List<TrashItem>> = dao.getAllFlow()

    suspend fun deleteForever(mediaId: Long) {
        // TODO Phase 2: call MediaStore.delete() to physically remove the file
        dao.deleteByMediaId(mediaId)
    }

    suspend fun clearExpiredTrash() {
        val expired = dao.getExpired(System.currentTimeMillis())
        if (expired.isNotEmpty()) {
            // TODO Phase 2: call MediaStore.delete() on each expired item before removing
            dao.deleteByIds(expired.map { it.id })
        }
    }
}
