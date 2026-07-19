package com.emon.proxagallery.data

import android.provider.MediaStore

/**
 * Sort options for photo grids.
 *
 * Each entry describes *what* to sort by ([column]) and *which direction*
 * ([ascending]). The enum carries no SQL syntax — the repository translates
 * `column + ascending` into whatever ORDER BY clause its data source needs.
 * This keeps the enum reusable across MediaStore, Room, or any future source.
 */
enum class PhotoSortOption(val label: String, val column: String, val ascending: Boolean) {
    NEWEST("Newest",     MediaStore.Files.FileColumns.DATE_ADDED,    false),
    OLDEST("Oldest",     MediaStore.Files.FileColumns.DATE_ADDED,    true),
    NAME_ASC("Name (A–Z)",  MediaStore.Files.FileColumns.DISPLAY_NAME, true),
    NAME_DESC("Name (Z–A)", MediaStore.Files.FileColumns.DISPLAY_NAME, false),
    LARGEST("Largest File",  MediaStore.Files.FileColumns.SIZE,       false),
    SMALLEST("Smallest File", MediaStore.Files.FileColumns.SIZE,      true);
}
