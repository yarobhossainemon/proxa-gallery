package com.emon.proxagallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Merged UI model for an album.
 *
 * MediaStore provides the base values ([displayName], [coverPhotoUri],
 * [itemCount], [dateAdded]); the nullable customization fields overlay user
 * preferences stored in Room. The resolved, display-ready values are exposed by
 * [displayNameToDisplay] and [coverUriToDisplay] so the UI never has to repeat
 * the "customName ?: displayName" fallback.
 *
 * Future AI features (Smart Albums, Tags, Collections, Color/Icon, Description)
 * can extend this model or attach sibling data without touching MediaStore.
 */
@Immutable
data class Album(
    val id: Long,
    val displayName: String,
    val coverPhotoUri: Uri,
    val itemCount: Int,
    val dateAdded: Long = 0,
    val customName: String? = null,
    val customCoverUri: Uri? = null,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val colorTag: String? = null,
    val sortMode: String? = null
) {
    val displayNameToDisplay: String
        get() = customName?.takeIf { it.isNotBlank() } ?: displayName

    val coverUriToDisplay: Uri
        get() = customCoverUri ?: coverPhotoUri
}

