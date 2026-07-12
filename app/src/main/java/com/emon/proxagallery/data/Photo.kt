package com.emon.proxagallery.data

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val dateTakenMs: Long? = null,
    val bucketId: Long? = null
)
