package com.emon.proxagallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val id: Long,
    val displayName: String,
    val coverPhotoUri: Uri,
    val itemCount: Int
)

