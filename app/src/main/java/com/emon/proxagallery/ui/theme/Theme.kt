package com.emon.proxagallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.emon.proxagallery.data.AccentColor
import com.emon.proxagallery.data.ThemeMode

@Immutable
data class ExtendedColors(
    val glassSurface: Color,
    val glassBorder: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val onNavUnselected: Color,
    val cardShadowPrimary: Color,
    val cardShadowSecondary: Color,
    val surfaceContainer: Color,
    // Glass-design-system extensions (Phase 1). Every theme populates these
    // explicitly so a missing token fails fast instead of rendering Unspecified.
    val glowColor: Color,
    val innerHighlight: Color,
    val gradientBorderStart: Color,
    val gradientBorderMid: Color,
    val gradientBorderEnd: Color,
    val glassSurfaceStrong: Color,
    val badgeText: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        glassSurface = Color.Unspecified,
        glassBorder = Color.Unspecified,
        gradientStart = Color.Unspecified,
        gradientEnd = Color.Unspecified,
        onNavUnselected = Color.Unspecified,
        cardShadowPrimary = Color.Unspecified,
        cardShadowSecondary = Color.Unspecified,
        surfaceContainer = Color.Unspecified,
        glowColor = Color.Unspecified,
        innerHighlight = Color.Unspecified,
        gradientBorderStart = Color.Unspecified,
        gradientBorderMid = Color.Unspecified,
        gradientBorderEnd = Color.Unspecified,
        glassSurfaceStrong = Color.Unspecified,
        badgeText = Color.Unspecified
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

// ── Dark ColorScheme (unchanged) ────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    onPrimary = Color.White,
    secondary = GeminiPurple,
    onSecondary = Color.White,
    tertiary = GeminiCyan,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = CardDark,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = Color.White.copy(alpha = 0.08f),
    outlineVariant = Color.White.copy(alpha = 0.04f),
    error = DeleteRed,
    onError = Color.White
)

private val DarkExtendedColors = ExtendedColors(
    glassSurface = Color(0x99090B10),
    glassBorder = Color(0x1EFFFFFF),
    gradientStart = GeminiBlue,
    gradientEnd = GeminiPurple,
    onNavUnselected = Color(0xFF8E919A),
    cardShadowPrimary = GeminiBlue.copy(alpha = 0.35f),
    cardShadowSecondary = GeminiPurple.copy(alpha = 0.15f),
    surfaceContainer = Color(0xFF161A22),
    glowColor = DarkGlowColor,
    innerHighlight = DarkInnerHighlight,
    gradientBorderStart = DarkGradientBorderStart,
    gradientBorderMid = DarkGradientBorderMid,
    gradientBorderEnd = DarkGradientBorderEnd,
    glassSurfaceStrong = DarkGlassSurfaceStrong,
    badgeText = Color.White
)

// ── Light ColorScheme (Samsung One UI) ──────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = LightTertiary,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = LightTextPrimary,
    surface = SurfaceLight,
    onSurface = LightTextPrimary,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = DeleteRed,
    onError = Color.White
)

private val LightExtendedColors = ExtendedColors(
    glassSurface = LightGlassSurface,
    glassBorder = LightGlassBorder,
    gradientStart = LightGradientStart,
    gradientEnd = LightGradientEnd,
    onNavUnselected = LightOnNavUnselected,
    cardShadowPrimary = LightCardShadowPrimary,
    cardShadowSecondary = LightCardShadowSecondary,
    surfaceContainer = SurfaceContainerLight,
    glowColor = LightGlowColor,
    innerHighlight = LightInnerHighlight,
    gradientBorderStart = LightGradientBorderStart,
    gradientBorderMid = LightGradientBorderMid,
    gradientBorderEnd = LightGradientBorderEnd,
    glassSurfaceStrong = LightGlassSurfaceStrong,
    badgeText = LightTextPrimary
)

// ── Accent Color system ─────────────────────────────────────────────
// The user-selected accent recolors only the accent-sensitive Material
// tokens (primary/secondary/tertiary and their containers) plus the
// accent-sensitive glass ExtendedColors (gradients, glow, gradient
// borders, card shadows, badge text). Neutral surfaces, backgrounds,
// glass blur, outlines, and shadows are left untouched so the glass
// design language is preserved.

/**
 * Applies [accent] to the resolved [colorScheme] / [extended] for the given
 * brightness. Returns a new pair; inputs are never mutated.
 */
private fun applyAccent(
    colorScheme: ColorScheme,
    extended: ExtendedColors,
    accent: AccentColor,
    isDark: Boolean
): Pair<ColorScheme, ExtendedColors> {
    val seed = if (isDark) accent.darkSeed else accent.lightSeed

    val recoloredScheme = colorScheme.copy(
        primary = seed.primary,
        onPrimary = Color.White,
        primaryContainer = seed.primaryContainer,
        onPrimaryContainer = Color.White,
        inversePrimary = seed.primary,
        secondary = seed.secondary,
        onSecondary = Color.White,
        secondaryContainer = seed.secondaryContainer,
        onSecondaryContainer = Color.White,
        tertiary = seed.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = seed.tertiaryContainer,
        onTertiaryContainer = Color.White
    )

    val recoloredExtended = extended.copy(
        gradientStart = seed.primary,
        gradientEnd = seed.secondary,
        glowColor = seed.primary.copy(alpha = if (isDark) 0.20f else 0.12f),
        gradientBorderStart = seed.primary,
        gradientBorderMid = seed.secondary,
        gradientBorderEnd = seed.tertiary,
        cardShadowPrimary = seed.primary.copy(alpha = if (isDark) 0.35f else 0.25f),
        cardShadowSecondary = seed.secondary.copy(alpha = if (isDark) 0.15f else 0.12f),
        badgeText = if (isDark) Color.White else seed.primary
    )

    return recoloredScheme to recoloredExtended
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxaGalleryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM_DEFAULT -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val (baseScheme, baseExtended) = when (themeMode) {
        ThemeMode.SYSTEM_DEFAULT -> if (isSystemDark)
            DarkColorScheme to DarkExtendedColors
        else
            LightColorScheme to LightExtendedColors
        ThemeMode.DARK -> DarkColorScheme to DarkExtendedColors
        ThemeMode.LIGHT -> LightColorScheme to LightExtendedColors
    }

    val (colorScheme, extended) = applyAccent(baseScheme, baseExtended, accentColor, isDark)

    val ripple = RippleConfiguration(color = colorScheme.primary)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides ripple,
            LocalExtendedColors provides extended,
            content = content
        )
    }
}
