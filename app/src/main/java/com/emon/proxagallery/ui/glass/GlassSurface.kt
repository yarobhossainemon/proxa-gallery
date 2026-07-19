package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Base glass container: a [Box] painted with [Modifier.glassBackground].
 *
 * No glow, no shadow, no motion — those are added by the caller (or by
 * [GlassCard] for the elevated variant). Use this directly when you need a
 * flat frosted surface such as an album footer, a selection bar, or a panel.
 *
 * @param shape     corner shape of the surface. Defaults to a card radius.
 * @param strong    pass true for selection bars / footers that need extra opacity.
 * @param padding   interior padding applied around [content].
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = cardShape(),
    strong: Boolean = false,
    padding: Dp = GlassTokens.GlassPadding,
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassBackground(shape = shape, strong = strong)
            .padding(padding),
        contentAlignment = contentAlignment
    ) { content() }
}
