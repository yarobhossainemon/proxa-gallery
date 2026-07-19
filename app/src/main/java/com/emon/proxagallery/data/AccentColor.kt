package com.emon.proxagallery.data

import androidx.compose.ui.graphics.Color

/**
 * The available accent colors. The selected accent recolors only the
 * accent-sensitive Material tokens (primary / secondary / tertiary and
 * their containers) plus the accent-sensitive glass ExtendedColors
 * (gradients, glow, gradient borders, card shadows, badge text). Neutral
 * surfaces, backgrounds, glass blur, outlines, and shadows are preserved
 * so the glass design language stays intact.
 *
 * Stored as an int ordinal in DataStore via [SettingsKeys.ACCENT_COLOR].
 *
 * Each entry exposes a [lightSeed] and a [darkSeed] holding the three
 * accent shades plus their containers. These seeds are the single source
 * of truth — no other file should hardcode accent hex values.
 */
enum class AccentColor {
    BLUE,
    PURPLE,
    GREEN,
    ORANGE,
    RED,
    TEAL,
    INDIGO,
    PINK;

    /** Human-readable label shown in the accent picker UI. */
    fun displayName(): String = when (this) {
        BLUE -> "Blue"
        PURPLE -> "Purple"
        GREEN -> "Green"
        ORANGE -> "Orange"
        RED -> "Red"
        TEAL -> "Teal"
        INDIGO -> "Indigo"
        PINK -> "Pink"
    }

    /**
     * Light-theme accent shades. Picked to keep sufficient contrast with
     * white foregrounds (`onPrimary = White`) on bright backgrounds.
     */
    val lightSeed: AccentSeed
        get() = when (this) {
            BLUE -> AccentSeed(
                primary = Color(0xFF1A6FC4),
                primaryContainer = Color(0xFF0E4F8F),
                secondary = Color(0xFF7B5EA7),
                secondaryContainer = Color(0xFF5A4480),
                tertiary = Color(0xFF00A3A3),
                tertiaryContainer = Color(0xFF007878)
            )
            PURPLE -> AccentSeed(
                primary = Color(0xFF7B3FE4),
                primaryContainer = Color(0xFF5A22B8),
                secondary = Color(0xFF9C5BFF),
                secondaryContainer = Color(0xFF6F35C9),
                tertiary = Color(0xFFC46FD8),
                tertiaryContainer = Color(0xFF974AAE)
            )
            GREEN -> AccentSeed(
                primary = Color(0xFF1E8E3E),
                primaryContainer = Color(0xFF136629),
                secondary = Color(0xFF3FA856),
                secondaryContainer = Color(0xFF2D7A3F),
                tertiary = Color(0xFF79A838),
                tertiaryContainer = Color(0xFF567D24)
            )
            ORANGE -> AccentSeed(
                primary = Color(0xFFE8731A),
                primaryContainer = Color(0xFFB85408),
                secondary = Color(0xFFE8A91A),
                secondaryContainer = Color(0xFFB8810A),
                tertiary = Color(0xFFE84E1A),
                tertiaryContainer = Color(0xFFB83608)
            )
            RED -> AccentSeed(
                primary = Color(0xFFD92D20),
                primaryContainer = Color(0xFFA11A14),
                secondary = Color(0xFFE8634A),
                secondaryContainer = Color(0xFFB8422F),
                tertiary = Color(0xFFE89C4A),
                tertiaryContainer = Color(0xFFB8712F)
            )
            TEAL -> AccentSeed(
                primary = Color(0xFF00838F),
                primaryContainer = Color(0xFF005C66),
                secondary = Color(0xFF1A8FA8),
                secondaryContainer = Color(0xFF0F6577),
                tertiary = Color(0xFF38A187),
                tertiaryContainer = Color(0xFF247660)
            )
            INDIGO -> AccentSeed(
                primary = Color(0xFF3D5AFE),
                primaryContainer = Color(0xFF2A3FD0),
                secondary = Color(0xFF5C6BC0),
                secondaryContainer = Color(0xFF3F4E94),
                tertiary = Color(0xFF7A8BFF),
                tertiaryContainer = Color(0xFF5563C9)
            )
            PINK -> AccentSeed(
                primary = Color(0xFFE040A0),
                primaryContainer = Color(0xFFB82D80),
                secondary = Color(0xFFD4648A),
                secondaryContainer = Color(0xFFA94870),
                tertiary = Color(0xFFE8789C),
                tertiaryContainer = Color(0xFFB8567A)
            )
        }

    /**
     * Dark-theme accent shades. Brighter than the light variants so they
     * remain vivid against the dark glass surfaces.
     */
    val darkSeed: AccentSeed
        get() = when (this) {
            BLUE -> AccentSeed(
                primary = Color(0xFF6EA8FE),
                primaryContainer = Color(0xFF3B7AE0),
                secondary = Color(0xFF9B7BFF),
                secondaryContainer = Color(0xFF6F4FCC),
                tertiary = Color(0xFF5EE6FF),
                tertiaryContainer = Color(0xFF2DB8D6)
            )
            PURPLE -> AccentSeed(
                primary = Color(0xFFB388FF),
                primaryContainer = Color(0xFF8A52E8),
                secondary = Color(0xFFC79BFF),
                secondaryContainer = Color(0xFF9466D6),
                tertiary = Color(0xFFD9A0E8),
                tertiaryContainer = Color(0xFFB070C0)
            )
            GREEN -> AccentSeed(
                primary = Color(0xFF66D97A),
                primaryContainer = Color(0xFF36A84B),
                secondary = Color(0xFF7FE08F),
                secondaryContainer = Color(0xFF4FA85E),
                tertiary = Color(0xFFA6D96A),
                tertiaryContainer = Color(0xFF7AAA42)
            )
            ORANGE -> AccentSeed(
                primary = Color(0xFFFFA84A),
                primaryContainer = Color(0xFFE07A14),
                secondary = Color(0xFFFFC56A),
                secondaryContainer = Color(0xFFD69024),
                tertiary = Color(0xFFFF8050),
                tertiaryContainer = Color(0xFFD6552A)
            )
            RED -> AccentSeed(
                primary = Color(0xFFFF6B6B),
                primaryContainer = Color(0xFFE03838),
                secondary = Color(0xFFFF8E72),
                secondaryContainer = Color(0xFFD85A40),
                tertiary = Color(0xFFFFAA66),
                tertiaryContainer = Color(0xFFD6793A)
            )
            TEAL -> AccentSeed(
                primary = Color(0xFF40C4D0),
                primaryContainer = Color(0xFF12959F),
                secondary = Color(0xFF5DC4D8),
                secondaryContainer = Color(0xFF2F95A8),
                tertiary = Color(0xFF66D4A6),
                tertiaryContainer = Color(0xFF38A57C)
            )
            INDIGO -> AccentSeed(
                primary = Color(0xFF7A8BFF),
                primaryContainer = Color(0xFF4453E8),
                secondary = Color(0xFF8E9CDB),
                secondaryContainer = Color(0xFF5C6BB0),
                tertiary = Color(0xFFA0AFFF),
                tertiaryContainer = Color(0xFF6E7DDB)
            )
            PINK -> AccentSeed(
                primary = Color(0xFFFF6EB4),
                primaryContainer = Color(0xFFE8408C),
                secondary = Color(0xFFFF90C8),
                secondaryContainer = Color(0xFFD86AA8),
                tertiary = Color(0xFFFFB0D4),
                tertiaryContainer = Color(0xFFD880AC)
            )
        }
}

/**
 * The three accent shades used to recolor a theme, plus their container
 * variants. Kept deliberately small so [com.emon.proxagallery.ui.theme]
 * can apply an accent with a single `copy(...)` call.
 */
data class AccentSeed(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color
)
