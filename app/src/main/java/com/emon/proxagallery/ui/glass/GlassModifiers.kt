package com.emon.proxagallery.ui.glass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * The base glass look every glass surface shares: a frosted translucent fill,
 * a 1px hairline border, and a faint top inner-highlight stroke that makes the
 * surface read as "lit from above". Deliberately has **no glow** — glow is an
 * opt-in accent added separately via [glow] so it can be used sparingly.
 *
 * Pass [strong] = true for bars / selection chrome that need more opacity than
 * a regular card (uses [com.emon.proxagallery.ui.theme.ExtendedColors.glassSurfaceStrong]).
 */
fun Modifier.glassBackground(
    shape: Shape,
    strong: Boolean = false
): Modifier = composed {
    val colors = MaterialTheme.extendedColors
    val surface = if (strong) colors.glassSurfaceStrong else colors.glassSurface
    val highlight = colors.innerHighlight

    this
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = when (outline) {
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Generic -> outline.path
                else -> Path().apply { addRect(outline.bounds) }
            }
            val highlightBrush = Brush.verticalGradient(
                colors = listOf(highlight, Color.Transparent)
            )
            onDrawWithContent {
                drawPath(path, color = surface)
                drawPath(path, brush = highlightBrush)
                drawContent()
            }
        }
        .border(
            width = GlassTokens.BorderWidth,
            color = colors.glassBorder,
            shape = shape
        )
}

/**
 * Ambient glow halo drawn underneath the surface via an elevated shadow whose
 * spot/ambient colors come from the theme gradient. **Use sparingly** — only
 * on the AI button, selected nav item, active search field, active chip and
 * the AI orb. Cards and badges must not glow.
 */
fun Modifier.glow(
    shape: Shape,
    radius: Dp = 16.dp
): Modifier = composed {
    val colors = MaterialTheme.extendedColors
    this.shadow(
        elevation = radius,
        shape = shape,
        clip = false,
        spotColor = colors.glowColor,
        ambientColor = colors.cardShadowSecondary
    )
}

/**
 * Spring scale feedback driven by [pressed]. Defaults to [GlassMotion.PressSpring];
 * pass [spec] to use [GlassMotion.CardSpring] / [GlassMotion.ChipSpring] /
 * [GlassMotion.NavSpring] for component-specific feel.
 */
fun Modifier.pressScale(
    pressed: Boolean,
    scaleDown: Float = GlassTokens.PressScale,
    spec: SpringSpec<Float> = GlassMotion.PressSpring
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spec,
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Moving 3-stop gradient border for the active / focused accent. When [active]
 * is false this draws the static hairline border (matching [glassBackground]),
 * so a surface can toggle between the two states without a layout jump.
 */
fun Modifier.animatedGradientBorder(
    shape: Shape,
    active: Boolean
): Modifier = composed {
    val colors = MaterialTheme.extendedColors

    if (!active) {
        this.border(GlassTokens.BorderWidth, colors.glassBorder, shape)
    } else {
        val transition = rememberInfiniteTransition(label = "glass_border")
        val fraction by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "border_offset"
        )
        this.border(
            width = GlassTokens.BorderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.gradientBorderStart,
                    colors.gradientBorderMid,
                    colors.gradientBorderEnd,
                    colors.gradientBorderStart
                ),
                start = Offset(0f, 0f),
                end = Offset(800f * fraction, 800f)
            ),
            shape = shape
        )
    }
}
