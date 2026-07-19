package com.emon.proxagallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val uri: String = "",
    val displayName: String,
    val mimeType: String,
    val originalAlbum: String?,
    val deletedAt: Long,
    val expiresAt: Long,
    val isVideo: Boolean,
    val thumbnailUri: String? = null,
    val localFilePath: String? = null,
    val localThumbnailPath: String? = null,
    val fileSize: Long = 0,
    val originalRelativePath: String? = null
) {
    companion object {
        const val TRASH_RETENTION_MS: Long = 30L * 24L * 60L * 60L * 1000L
    }
}
