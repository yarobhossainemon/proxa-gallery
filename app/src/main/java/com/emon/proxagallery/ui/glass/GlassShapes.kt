package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

/**
 * Shared shape factories so components never construct a [RoundedCornerShape]
 * inline (which would scatter magic numbers and defeat [GlassTokens]).
 *
 * These are plain functions rather than vals because [RoundedCornerShape] is
 * cheap to construct and callers sometimes need to pass them as composable
 * parameters.
 */

/** Standard card corner shape (album / library / AI / settings cards). */
fun cardShape(): Shape = RoundedCornerShape(GlassTokens.CardRadius)

/** Compact card corner shape (photo grid items). */
fun smallCardShape(): Shape = RoundedCornerShape(GlassTokens.SmallRadius)

/** Capsule shape for chips / pills. */
fun chipShape(): Shape = RoundedCornerShape(GlassTokens.ChipRadius)

/** Floating bottom navigation pill shape — full capsule. */
fun navShape(): Shape = RoundedCornerShape(percent = GlassTokens.NavRadius)

/** Search bar corner shape. */
fun searchShape(): Shape = RoundedCornerShape(GlassTokens.SearchRadius)

/** Fully circular shape (badges, icon buttons). */
fun circleShape(): Shape = CircleShape
