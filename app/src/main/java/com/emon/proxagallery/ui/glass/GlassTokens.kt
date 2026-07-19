package com.emon.proxagallery.ui.glass

import androidx.compose.ui.unit.dp

/**
 * Single source of truth for every dimension the glass design system uses.
 *
 * Radii, paddings, border widths and the press-scale factor all live here so
 * that "make cards slightly rounder" is a one-line change instead of a
 * project-wide hunt for `18.dp` / `20.dp` / `22.dp` literals.
 *
 * Read these from [GlassModifiers] / the Glass* components; never hardcode a
 * radius inline.
 */
object GlassTokens {
    /** Standard card corner radius (album / library / AI / settings cards). */
    val CardRadius = 22.dp

    /** Smaller card corner radius (photo grid items, compact surfaces). */
    val SmallRadius = 16.dp

    /** Fully-rounded capsule radius (chips, pills). */
    val ChipRadius = 50.dp

    /** Floating bottom navigation pill — uses percent-based rounding. */
    val NavRadius = 50

    /** Search bar corner radius. */
    val SearchRadius = 32.dp

    /** Default interior padding inside a glass surface. */
    val GlassPadding = 14.dp

    /** 1px hairline border used on every glass surface. */
    val BorderWidth = 1.dp

    /** Scale factor applied to a card/button when pressed. */
    const val PressScale = 0.96f
}
