package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.shadow
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * Elevated glass card: [GlassSurface] + a soft colored shadow (spot color from
 * the theme gradient). This is the workhorse container for photo / album /
 * library / AI feature / settings cards.
 *
 * **Never glows** — cards are everywhere, and glow is reserved for the few
 * interactive accents (AI button, selected nav, active search/chip).
 *
 * @param shape      card corner shape.
 * @param pressed    drives the optional spring press-scale; pass null to disable.
 * @param elevation  shadow elevation; 0.dp disables the shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = cardShape(),
    pressed: Boolean? = null,
    elevation: Dp = 6.dp,
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.extendedColors
    val body = Modifier
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, spotColor = colors.cardShadowPrimary, ambientColor = colors.cardShadowSecondary) else Modifier)
        .then(if (pressed != null) Modifier.pressScale(pressed = pressed, spec = GlassMotion.CardSpring) else Modifier)
        .glassBackground(shape = shape)

    Box(
        modifier = modifier.then(body),
        contentAlignment = contentAlignment
    ) { content() }
}
