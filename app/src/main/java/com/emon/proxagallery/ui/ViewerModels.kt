package com.emon.proxagallery.ui

import android.net.Uri
import com.emon.proxagallery.data.MediaItem
import com.emon.proxagallery.data.TrashItem
import java.io.File

/**
 * Discriminator for the shared [PhotoViewerScreen]. The viewer engine (pager,
 * zoom/pan/gestures, top bar, animations) is identical for both modes; only the
 * action set and the sheets differ.
 *
 * - [GALLERY]: full action strip (Share/Edit/Favorite/Delete/More) + Details sheet.
 * - [TRASH]: Restore + Delete Forever only (no edit/share/wallpaper/more/details).
 */
enum class ViewerMode { GALLERY, TRASH }

/**
 * Uniform render model for a single page in the shared viewer.
 *
 * [imageData] is whatever Coil can decode: a MediaStore [Uri] in gallery mode, or a
 * local [File] (fallback to the original [Uri]) in trash mode. [videoUri] is the
 * MediaStore Uri used to launch the system video player; it is null in trash mode
 * because trashed files are no longer addressable via MediaStore.
 */
data class ViewerItem(
    val id: Long,
    val displayName: String,
    val imageData: Any,
    val isVideo: Boolean,
    val videoUri: Uri?
)

/** Gallery item → viewer item: renders from the MediaStore Uri; videos are playable. */
fun MediaItem.toViewerItem(): ViewerItem = ViewerItem(
    id = id,
    displayName = displayName,
    imageData = uri,
    isVideo = isVideo,
    videoUri = uri.takeIf { isVideo }
)

/**
 * Trash item → viewer item. Image source priority matches the previous trash viewer:
 * local full-size copy → local thumbnail → original (now-stale) Uri string. Videos are
 * not playable from trash (their MediaStore row is gone), so [videoUri] is null.
 */
fun TrashItem.toViewerItem(): ViewerItem {
    val imageSource = localFilePath?.let { File(it) }
        ?: localThumbnailPath?.let { File(it) }
        ?: uri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        ?: Uri.EMPTY

    return ViewerItem(
        id = id,
        displayName = displayName,
        imageData = imageSource,
        isVideo = isVideo,
        videoUri = null
    )
}
