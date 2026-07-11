package com.emon.proxagallery.data

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String = ""
)
