package com.emon.proxagallery.data

/**
 * Represents the available theme modes for the app.
 *
 * Stored as an int ordinal in DataStore via [SettingsKeys.THEME_MODE].
 */
enum class ThemeMode {
    SYSTEM_DEFAULT,
    DARK,
    LIGHT,
    BLOSSOM_PINK;

    /** Human-readable label shown in the theme picker UI. */
    fun displayName(): String = when (this) {
        SYSTEM_DEFAULT -> "System Default"
        DARK -> "Dark"
        LIGHT -> "Light"
        BLOSSOM_PINK -> "Blossom Pink"
    }
}
