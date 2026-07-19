package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * Glass pill button with an optional leading icon and label. When [selected]
 * the pill fills with the theme gradient; otherwise it renders the standard
 * glass background.
 *
 * @param glow allow glow on the selected state (off by default — only the AI
 *             affordance should ever turn this on).
 */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    glow: Boolean = false,
    shape: Shape = chipShape()
) {
    val colors = MaterialTheme.extendedColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(40.dp)
            .pressScale(pressed = pressed, spec = GlassMotion.PressSpring)
            .then(if (glow && selected) Modifier.glow(shape) else Modifier)
            .then(
                if (selected) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                        ),
                        shape = shape
                    )
                } else {
                    Modifier.glassBackground(shape = shape)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Circular glass icon button with spring press feedback. This is the **only**
 * button that may glow — pass [glow] = true for the AI sparkle affordance.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    glow: Boolean = false
) {
    val shape = circleShape()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressed = pressed, spec = GlassMotion.PressSpring)
            .then(if (glow) Modifier.glow(shape, radius = 12.dp) else Modifier)
            .glassBackground(shape = shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size((size.value * 0.5f).dp)
        )
    }
}
