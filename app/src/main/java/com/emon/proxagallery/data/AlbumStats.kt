package com.emon.proxagallery.data

/**
 * Aggregate statistics for a single album, computed from MediaStore on demand.
 * Displayed in the Album Information section of the Edit Album bottom sheet.
 *
 * These are derived from MediaStore queries — never stored in Room.
 */
data class AlbumStats(
    val photoCount: Int,
    val videoCount: Int,
    val totalSizeBytes: Long
)
