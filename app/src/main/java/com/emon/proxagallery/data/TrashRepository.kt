package com.emon.proxagallery.data

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class TrashRepository(
    context: Context
) {
    private val dao = TrashDatabase.getInstance(context).trashDao()
    private val contentResolver = context.contentResolver
    private val trashDir = File(context.filesDir, "trash/media")
    private val thumbDir = File(context.filesDir, "trash/thumbnails")

    init {
        // Run mkdirs lazily inside background coroutines to avoid synchronous Disk I/O on main thread.
    }

    private fun extensionFromMimeType(mimeType: String): String {
        return when {
            mimeType.startsWith("image/jpeg") -> "jpg"
            mimeType.startsWith("image/png") -> "png"
            mimeType.startsWith("image/webp") -> "webp"
            mimeType.startsWith("image/gif") -> "gif"
            mimeType.startsWith("video/mp4") -> "mp4"
            mimeType.startsWith("video/3gpp") -> "3gp"
            mimeType.startsWith("video/webm") -> "webm"
            mimeType.startsWith("video/x-matroska") -> "mkv"
            else -> "bin"
        }
    }

    suspend fun copyToTrash(
        sourceUri: Uri,
        mimeType: String,
        fileSize: Long
    ): LocalCopyResult? = withContext(Dispatchers.IO) {
        trashDir.mkdirs()
        val ext = extensionFromMimeType(mimeType)
        val fileName = "${UUID.randomUUID()}.$ext"
        val destFile = File(trashDir, fileName)

        try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                    }
                }
            } ?: run {
                destFile.delete()
                return@withContext null
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                destFile.delete()
                return@withContext null
            }

            return@withContext LocalCopyResult(
                localFilePath = destFile.absolutePath,
                fileSize = destFile.length()
            )
        } catch (e: Exception) {
            destFile.delete()
            return@withContext null
        }
    }

    suspend fun generateThumbnail(
        localFilePath: String,
        isVideo: Boolean
    ): String? = withContext(Dispatchers.IO) {
        thumbDir.mkdirs()
        val thumbFile = File(thumbDir, "${UUID.randomUUID()}.jpg")

        try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(localFilePath)
                val bitmap = retriever.frameAtTime
                retriever.release()
                if (bitmap != null) {
                    FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    bitmap.recycle()
                    return@withContext thumbFile.absolutePath
                }
            } else {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(localFilePath, options)

                val maxDim = 512
                val scaleFactor = maxOf(
                    (options.outWidth + maxDim - 1) / maxDim,
                    (options.outHeight + maxDim - 1) / maxDim,
                    1
                )

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = scaleFactor
                }
                val bitmap = BitmapFactory.decodeFile(localFilePath, decodeOptions)
                if (bitmap != null) {
                    FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    bitmap.recycle()
                    return@withContext thumbFile.absolutePath
                }
            }
        } catch (_: Exception) {
        }
        return@withContext null
    }

    fun deleteLocalFiles(localFilePath: String?, localThumbnailPath: String?) {
        localFilePath?.let { File(it).delete() }
        localThumbnailPath?.let { File(it).delete() }
    }

    suspend fun moveToTrash(
        mediaId: Long,
        uri: Uri,
        displayName: String,
        mimeType: String,
        originalAlbum: String?,
        localFilePath: String? = null,
        localThumbnailPath: String? = null,
        fileSize: Long = 0,
        originalRelativePath: String? = null
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
            thumbnailUri = localThumbnailPath ?: uri.toString(),
            localFilePath = localFilePath,
            localThumbnailPath = localThumbnailPath,
            fileSize = fileSize,
            originalRelativePath = originalRelativePath
        )
        dao.insert(item)
    }

    suspend fun restoreItem(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val sourceFile = File(item.localFilePath ?: return@withContext false)
        if (!sourceFile.exists()) return@withContext false

        val collection = when {
            item.mimeType.startsWith("video/") ->
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            item.mimeType.startsWith("image/") ->
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> {
                val ext = item.localFilePath?.substringAfterLast('.', "")
                when (ext) {
                    "jpg", "jpeg", "png", "webp", "gif", "bmp" ->
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "mp4", "3gp", "webm", "mkv" ->
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> return@withContext false
                }
            }
        }

        val nowSec = System.currentTimeMillis() / 1000L
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.MediaColumns.DATE_ADDED, nowSec)
            put(MediaStore.MediaColumns.DATE_MODIFIED, item.deletedAt / 1000L)
            item.originalRelativePath?.let {
                put(MediaStore.MediaColumns.RELATIVE_PATH, it)
            }
        }

        val uri = contentResolver.insert(collection, values)
        if (uri == null) return@withContext false

        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: run {
                contentResolver.delete(uri, null, null)
                return@withContext false
            }
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            return@withContext false
        }

        try {
            val updateValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
                put(MediaStore.MediaColumns.SIZE, sourceFile.length())
            }
            contentResolver.update(uri, updateValues, null, null)
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            return@withContext false
        }

        deleteLocalFiles(item.localFilePath, item.localThumbnailPath)
        dao.deleteByMediaId(item.mediaId)

        true
    }

    fun getTrashItems(): Flow<List<TrashItem>> = dao.getAllFlow()

    suspend fun deleteForever(mediaId: Long) {
        val item = dao.getByMediaId(mediaId)
        if (item != null) {
            deleteLocalFiles(item.localFilePath, item.localThumbnailPath)
        }
        dao.deleteByMediaId(mediaId)
    }

    suspend fun deleteTrashRecords(ids: List<Long>) {
        val items = dao.getByIds(ids)
        items.forEach { deleteLocalFiles(it.localFilePath, it.localThumbnailPath) }
        dao.deleteByIds(ids)
    }

    suspend fun clearExpiredTrash() {
        val expired = dao.getExpired(System.currentTimeMillis())
        if (expired.isNotEmpty()) {
            expired.forEach { deleteLocalFiles(it.localFilePath, it.localThumbnailPath) }
            dao.deleteByIds(expired.map { it.id })
        }
    }

    suspend fun cancelPendingDelete(filePath: String?, thumbPath: String?) {
        deleteLocalFiles(filePath, thumbPath)
    }
}

data class LocalCopyResult(
    val localFilePath: String,
    val fileSize: Long
)