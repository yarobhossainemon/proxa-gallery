package com.emon.proxagallery.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark palette (Gemini — kept unchanged) ───────────────────────────
val BackgroundDark = Color(0xFF090B10)
val SurfaceDark = Color(0xFF10131A)
val CardDark = Color(0xFF131722)

val GeminiBlue = Color(0xFF6EA8FE)
val GeminiPurple = Color(0xFF9B7BFF)
val GeminiCyan = Color(0xFF5EE6FF)
val GeminiPink = Color(0xFFEFB8C8)

// Dark glass-system extensions: the blue→purple→cyan identity.
// glowColor is the ambient cyan halo used sparingly (AI button, selected
// nav item, active search, active chip). innerHighlight is the faint top
// white stroke that makes every glass surface read as "lit from above".
// gradientBorder* is the 3-stop animated border; glassSurfaceStrong is the
// higher-opacity variant for selection bars / footers.
val DarkGlowColor = GeminiCyan.copy(alpha = 0.20f)
val DarkInnerHighlight = Color.White.copy(alpha = 0.10f)
val DarkGradientBorderStart = GeminiBlue
val DarkGradientBorderMid = GeminiPurple
val DarkGradientBorderEnd = GeminiCyan
val DarkGlassSurfaceStrong = Color(0xCC10131A)

// ── Light palette (Samsung One UI) ──────────────────────────────────
val BackgroundLight = Color(0xFFF5F6F8)
val SurfaceLight = Color(0xFFFFFFFF)
val CardLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFEDEEF2)

val LightPrimary = Color(0xFF1A6FC4)
val LightSecondary = Color(0xFF7B5EA7)
val LightTertiary = Color(0xFF00A3A3)

val LightTextPrimary = Color(0xFF1A1C1E)
val LightTextSecondary = Color(0xFF6B7280)

val LightOutline = Color(0xFF1A1C1E).copy(alpha = 0.08f)
val LightOutlineVariant = Color(0xFF1A1C1E).copy(alpha = 0.04f)

val LightGlassSurface = Color(0xD9F5F6F8)
val LightGlassBorder = Color(0x14000000)
val LightGlassSurfaceStrong = Color(0xF2F5F6F8)

val LightGradientStart = Color(0xFF1A6FC4)
val LightGradientEnd = Color(0xFF7B5EA7)

val LightOnNavUnselected = Color(0xFF8E919A)

val LightCardShadowPrimary = Color(0xFF1A6FC4).copy(alpha = 0.25f)
val LightCardShadowSecondary = Color(0xFF7B5EA7).copy(alpha = 0.12f)

// Light glass-system extensions — softer alpha than dark so the identity
// stays tasteful on a bright background. Glow uses the primary blue,
// highlight is a faint top white stroke.
val LightGlowColor = LightPrimary.copy(alpha = 0.12f)
val LightInnerHighlight = Color.White.copy(alpha = 0.60f)
val LightGradientBorderStart = LightPrimary
val LightGradientBorderMid = LightSecondary
val LightGradientBorderEnd = LightTertiary

// ── Shared semantic colors ──────────────────────────────────────────
val FavoriteRed = Color(0xFFFF1744)
val DeleteRed = Color(0xFFFF5252)

val VideoBadgeBlue = Color(0xFF1565C0)
val PhotoBadgeGreen = Color(0xFF2E7D32)
val MapPinBlue = Color(0xFF4FC3F7)
val RestoreBlue = Color(0xFF1A73E8)
