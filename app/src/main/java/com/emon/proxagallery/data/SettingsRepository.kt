package com.emon.proxagallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Dedicated DataStore for user settings (separate file from favorites).
 *
 * Named [settingsDataStore] (not `dataStore`) because [FavoritesRepository]
 * already declares a top-level `Context.dataStore` in this package; reusing
 * that name would be a redeclaration conflict.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Single source of truth for persisted user settings. All DataStore access is
 * encapsulated here — ViewModels consume Flows and call suspend setters; the
 * UI never touches DataStore directly.
 *
 * Modelled on [FavoritesRepository]: takes a [Context], stores the application
 * context, and exposes a cold [Flow] per setting plus a suspend setter.
 */
class SettingsRepository(
    context: Context
) {
    private val appContext = context.applicationContext

    /**
     * Current album sort option, or [AlbumSortOption.NAME_ASC] when unset.
     * The stored int ordinal is mapped back to the enum safely so a corrupt
     * or out-of-range value can never crash the app.
     */
    val albumSortOption: Flow<AlbumSortOption> = appContext.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ordinal = preferences[SettingsKeys.ALBUM_SORT_OPTION]
                ?: SettingsKeys.Defaults.ALBUM_SORT_OPTION
            AlbumSortOption.entries.getOrElse(ordinal) { AlbumSortOption.NAME_ASC }
        }

    /**
     * Current photo sort option, or [PhotoSortOption.NEWEST] when unset.
     * The stored int ordinal is mapped back to the enum safely so a corrupt
     * or out-of-range value can never crash the app.
     */
    val photoSortOption: Flow<PhotoSortOption> = appContext.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ordinal = preferences[SettingsKeys.PHOTO_SORT_OPTION]
                ?: SettingsKeys.Defaults.PHOTO_SORT_OPTION
            PhotoSortOption.entries.getOrElse(ordinal) { PhotoSortOption.NEWEST }
        }

    /**
     * Currently selected home tab index, or `0` (All Photos) when unset.
     */
    val selectedTab: Flow<Int> = appContext.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.SELECTED_TAB] ?: SettingsKeys.Defaults.SELECTED_TAB
        }

    suspend fun setAlbumSortOption(option: AlbumSortOption) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.ALBUM_SORT_OPTION] = option.ordinal
        }
    }

    suspend fun setSelectedTab(tabIndex: Int) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.SELECTED_TAB] = tabIndex
        }
    }

    suspend fun setPhotoSortOption(option: PhotoSortOption) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.PHOTO_SORT_OPTION] = option.ordinal
        }
    }

    /**
     * Current theme mode, or [ThemeMode.SYSTEM_DEFAULT] when unset.
     */
    val themeMode: Flow<ThemeMode> = appContext.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ordinal = preferences[SettingsKeys.THEME_MODE]
                ?: SettingsKeys.Defaults.DEFAULT_THEME_MODE
            ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM_DEFAULT }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_MODE] = mode.ordinal
        }
    }

    /**
     * Currently selected accent color, or [AccentColor.BLUE] when unset.
     * The stored int ordinal is mapped back to the enum safely so a corrupt
     * or out-of-range value can never crash the app.
     */
    val accentColor: Flow<AccentColor> = appContext.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ordinal = preferences[SettingsKeys.ACCENT_COLOR]
                ?: SettingsKeys.Defaults.DEFAULT_ACCENT_COLOR
            AccentColor.entries.getOrElse(ordinal) { AccentColor.BLUE }
        }

    suspend fun setAccentColor(accent: AccentColor) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.ACCENT_COLOR] = accent.ordinal
        }
    }
}
