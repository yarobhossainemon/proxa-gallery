package com.emon.proxagallery.data

/**
 * Sort options for the Albums screen.
 *
 * This is the single source of truth for album ordering. Adding a new option
 * means adding one enum entry plus one [when] branch in [sort] — no other sort
 * logic exists anywhere in the app.
 *
 * Note: this affects ONLY the album list. Photo grids within an album are
 * always ordered by DATE_ADDED DESC and are untouched by this enum.
 */
enum class AlbumSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    MOST_PHOTOS("Most Photos"),
    LEAST_PHOTOS("Least Photos");

    /**
     * Returns a new list sorted by this option. Pure, allocation only on the
     * returned list; the input list is not mutated. Cheap to call in memory
     * (album counts are typically dozens to low hundreds).
     *
     * Pinned albums ([Album.isPinned]) always float above unpinned ones,
     * independent of the chosen ordering, so a user-pinned album stays on top
     * whether they sort by Newest, Name, or anything else.
     */
    fun sort(albums: List<Album>): List<Album> {
        val byOption = when (this) {
            NEWEST       -> albums.sortedByDescending { it.dateAdded }
            OLDEST       -> albums.sortedBy { it.dateAdded }
            NAME_ASC     -> albums.sortedBy { it.displayNameToDisplay.lowercase() }
            NAME_DESC    -> albums.sortedByDescending { it.displayNameToDisplay.lowercase() }
            MOST_PHOTOS  -> albums.sortedByDescending { it.itemCount }
            LEAST_PHOTOS -> albums.sortedBy { it.itemCount }
        }
        // Stable partition: preserves the per-option order inside each group.
        return byOption.sortedByDescending { it.isPinned }
    }
}
