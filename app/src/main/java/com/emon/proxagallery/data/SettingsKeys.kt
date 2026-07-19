package com.emon.proxagallery.data

import androidx.datastore.preferences.core.intPreferencesKey

/**
 * Centralized definitions for all user settings stored in DataStore.
 *
 * To add a new setting:
 * 1. Add a key constant below.
 * 2. Add a getter Flow + setter in [SettingsRepository].
 * 3. Collect the flow in the relevant ViewModel.
 */
object SettingsKeys {

    val ALBUM_SORT_OPTION = intPreferencesKey("album_sort_option")

    val SELECTED_TAB = intPreferencesKey("selected_tab")

    val PHOTO_SORT_OPTION = intPreferencesKey("photo_sort_option")

    val THEME_MODE = intPreferencesKey("theme_mode")

    object Defaults {
        // NAME_ASC.ordinal is evaluated at runtime, so this cannot be `const`.
        // A plain `val` in an object is initialized once (singleton) and stays
        // in sync with NAME_ASC even if enum entries are reordered — no hardcoding.
        val ALBUM_SORT_OPTION = AlbumSortOption.NAME_ASC.ordinal
        const val SELECTED_TAB = 0
        val PHOTO_SORT_OPTION = PhotoSortOption.NEWEST.ordinal
        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM_DEFAULT.ordinal
    }
}
