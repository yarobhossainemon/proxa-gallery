package com.emon.proxagallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String = "",
    val mimeType: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val dateTakenMs: Long? = null,
    val dateAddedMs: Long? = null,
    val bucketId: Long? = null,
    val bucketDisplayName: String? = null,
    val durationMs: Long? = null,
    val dateModifiedSec: Long? = null
) {
    val isVideo: Boolean = mimeType.startsWith("video/")
    val fileExtension: String = displayName.substringAfterLast('.', "").lowercase()
}

