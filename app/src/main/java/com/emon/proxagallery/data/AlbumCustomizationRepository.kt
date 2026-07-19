package com.emon.proxagallery.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository above MediaStore for [AlbumCustomization].
 *
 * The UI never touches Room directly — it goes through the ViewModel, which
 * goes through here. Every mutator routes through [mutate], so the read-merge-
 * upsert sequence for any field lives in exactly one place. Adding a future
 * field (e.g. description, icon) is one new mutator method, nothing else.
 */
class AlbumCustomizationRepository(
    context: Context
) {
    private val dao = TrashDatabase.getInstance(context).albumCustomizationDao()
    private val contentResolver = context.contentResolver

    fun observeAll(): Flow<List<AlbumCustomization>> = dao.getAllFlow()

    /** Reactive Flow of only the hidden album customizations. */
    fun observeHidden(): Flow<List<AlbumCustomization>> = dao.getHiddenFlow()

    suspend fun getAllOnce(): List<AlbumCustomization> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getByBucketId(bucketId: Long): AlbumCustomization? = withContext(Dispatchers.IO) {
        dao.getByBucketId(bucketId)
    }

    suspend fun updateCustomName(bucketId: Long, customName: String?) =
        mutate(bucketId) { it.copy(customName = customName?.takeIf { name -> name.isNotBlank() }) }

    suspend fun updateCustomCover(bucketId: Long, coverUri: String?) =
        mutate(bucketId) { it.copy(customCoverUri = coverUri) }

    suspend fun setPinned(bucketId: Long, isPinned: Boolean) =
        mutate(bucketId) { it.copy(isPinned = isPinned) }

    suspend fun setHidden(bucketId: Long, isHidden: Boolean) =
        mutate(bucketId) { it.copy(isHidden = isHidden) }

    /** Reserved for the future Album Color/Icon feature. */
    suspend fun updateColorTag(bucketId: Long, colorTag: String?) =
        mutate(bucketId) { it.copy(colorTag = colorTag) }

    /** Reserved for the future per-album sort preference feature. */
    suspend fun updateSortMode(bucketId: Long, sortMode: String?) =
        mutate(bucketId) { it.copy(sortMode = sortMode) }

    /** Clears every override for this album, restoring full MediaStore defaults. */
    suspend fun resetCustomization(bucketId: Long) = withContext(Dispatchers.IO) {
        dao.deleteByBucketId(bucketId)
    }

    /** Clears only the custom cover, restoring the default MediaStore cover. */
    suspend fun clearCustomCover(bucketId: Long) =
        mutate(bucketId) { it.copy(customCoverUri = null) }

    /** Drops customizations whose MediaStore albums no longer exist. */
    suspend fun deleteByBucketIds(bucketIds: List<Long>) = withContext(Dispatchers.IO) {
        if (bucketIds.isNotEmpty()) dao.deleteByBucketIds(bucketIds)
    }

    /**
     * Returns `true` if [uriString] points to a still-accessible image.
     * Used by the ViewModel to auto-fall-back to the default cover when the
     * custom cover photo has been deleted from the device.
     */
    fun isCoverUriAccessible(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
                ?.use { it.moveToFirst() }
                ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Applies [transform] to the existing row for [bucketId], creating one if
     * absent. Always refreshes [AlbumCustomization.updatedAt]. Single source of
     * truth for the read-modify-write so every mutator behaves identically.
     */
    private suspend fun mutate(
        bucketId: Long,
        transform: (AlbumCustomization) -> AlbumCustomization
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dao.getByBucketId(bucketId)
        val next = if (existing != null) {
            transform(existing).copy(updatedAt = now)
        } else {
            transform(AlbumCustomization(bucketId = bucketId, createdAt = now, updatedAt = now))
                .copy(updatedAt = now)
        }
        dao.upsert(next)
    }
}
