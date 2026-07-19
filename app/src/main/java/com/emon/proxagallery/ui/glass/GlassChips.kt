package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glass capsule chip. Used for both the Photos quick-search chips and the AI
 * example chips, deduplicating the two previous hand-rolled chip styles.
 *
 * - When [active], the chip gets the animated 3-stop gradient border + a soft
 *   scale-down on press (one of the few glow-adjacent accents in the system).
 * - When inactive, it renders the standard glass capsule.
 *
 * Pass [onClick] = null for a non-interactive chip (AI examples).
 */
@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = chipShape()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val clickable = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(pressed, spec = GlassMotion.ChipSpring) else Modifier)
            .animatedGradientBorder(shape, active)
            .glassBackground(shape)
            .then(clickable)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
