package com.emon.proxagallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Virtual metadata layer stored ABOVE MediaStore.
 *
 * MediaStore remains the source of truth for album existence, item count, cover
 * and date. This row only carries user overrides for a given [bucketId]. A row
 * only exists once the user has customized something; an uncustomized album has
 * no row at all and falls back to MediaStore values.
 *
 * The nullable fields are deliberately permissive so future AI features (Smart
 * Albums, Tags, Collections, Color/Icon, Description, Sort preferences) can be
 * added by introducing new columns or companion tables that reference [bucketId]
 * — without redesigning this one.
 *
 * Fields:
 * - [customName]: overrides BUCKET_DISPLAY_NAME when non-blank.
 * - [customCoverUri]: overrides the MediaStore cover Uri when present.
 * - [isPinned]: floats the album above all unpinned albums regardless of sort.
 * - [isHidden]: excludes the album from the normal album list.
 * - [colorTag]: reserved for the future Album Color/Icon feature; stored now so
 *   a later migration is not required.
 * - [sortMode]: reserved for per-album photo sort preferences.
 * - [createdAt] / [updatedAt]: audit timestamps.
 */
@Entity(tableName = "album_customizations")
data class AlbumCustomization(
    @PrimaryKey
    val bucketId: Long,
    val customName: String? = null,
    val customCoverUri: String? = null,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val colorTag: String? = null,
    val sortMode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
