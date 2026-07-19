package com.emon.proxagallery.ui.glass

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * Navigation item descriptor.
 */
data class GlassNavItem(
    val icon: ImageVector,
    val label: String
)

/** The default tabs used by [GlassNavigationBar]. */
val DefaultGlassNavItems = listOf(
    GlassNavItem(Icons.Rounded.Photo, "Photos"),
    GlassNavItem(Icons.Rounded.Folder, "Albums"),
    GlassNavItem(Icons.Rounded.AutoAwesome, "AI"),
    GlassNavItem(Icons.Rounded.CollectionsBookmark, "Library"),
    GlassNavItem(Icons.Rounded.Settings, "Settings")
)

/**
 * Diameter of the premium center AI hero button. Embedded inside the nav bar
 * (not a floating FAB) — sized to feel like a signature element without
 * breaking the 72dp bar height or the equal-weight slot layout.
 */
private val AiButtonDiameter = 60.dp

/** Length of one breathing / ring-sweep cycle (~3.5s, within the 3–4s brief). */
private const val AiBreathDurationMs = 3_500

/**
 * Floating bottom navigation bar built from the glass design system.
 *
 * - Frosted glass pill background ([Modifier.glassBackground]).
 * - Selected item: gradient-filled circle + glow halo (one of the few glow
 *   spots in the system) + spring scale pop ([GlassMotion.NavSpring]).
 * - Unselected: translucent icon tinted [ExtendedColors.onNavUnselected].
 *
 * This is a drop-in replacement for the previous `FloatingBottomNavigation`.
 */
@Composable
fun GlassNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<GlassNavItem> = DefaultGlassNavItems,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.extendedColors
    val shape = navShape()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(72.dp)
            .glassBackground(shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index

                // The center AI tab is rendered as a premium glass hero button;
                // every other tab keeps the existing 48dp styling untouched.
                if (index == items.count() / 2) {
                    AiNavButton(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onTabSelected(index) },
                        // Keep the AI tab in the same equal-weight slot as every
                        // other item so the bar layout is unchanged.
                        slotModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    return@forEachIndexed
                }

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1.0f,
                    animationSpec = GlassMotion.NavSpring,
                    label = "nav_scale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        )
                ) {
                    val itemShape = circleShape()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .then(if (isSelected) Modifier.glow(itemShape, radius = 14.dp) else Modifier)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                                        ),
                                        shape = itemShape
                                    )
                                } else {
                                    Modifier.background(
                                        color = Color.Transparent,
                                        shape = itemShape
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color.White else colors.onNavUnselected,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The premium center AI hero button — Proxa Gallery's visual signature.
 *
 * A 60dp circular glass element embedded inside the nav bar (not a floating
 * FAB) built entirely from the glass design system:
 *
 * - Frosted [glassBackground] core (Samsung One UI 8 feel).
 * - A thin, always-animated blue → cyan gradient ring sweeping around it.
 * - A slow 3.5s breathing scale pulse on the inner core.
 * - Soft ambient [glow] when selected; a faint ring + minimal glow at rest so
 *   the inactive state still reads as premium.
 * - Spring selection pop ([GlassMotion.NavSpring]) and press feedback
 *   ([GlassMotion.PressSpring] via [pressScale]).
 *
 * The icon stays centered; it uses the current AI icon until a Proxa logo
 * lands. Navigation contract is unchanged — same `onClick` as every other tab.
 */
@Composable
private fun AiNavButton(
    item: GlassNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    slotModifier: Modifier
) {
    val colors = MaterialTheme.extendedColors
    val shape = circleShape()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Selection scale pop — same spring language as the other nav items.
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1.0f,
        animationSpec = GlassMotion.NavSpring,
        label = "ai_nav_selection_scale"
    )

    // Slow breathing pulse (3–4s cycle). Applied to an inner core so the
    // selection scale can layer on top without fighting it.
    val breath = rememberInfiniteTransition(label = "ai_breath")
    val breathFraction by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AiBreathDurationMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_breath_fraction"
    )
    val breathScale = 1f + 0.05f * breathFraction

    // Ring sweep — drives the diagonal endpoints of the blue → cyan gradient
    // so the ring appears to rotate around the button.
    val ringFraction by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AiBreathDurationMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_ring_sweep"
    )

    // Ring + glow intensity ease in/out with selection so the inactive state
    // still shows a faint ring and minimal glow.
    val ringAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.45f,
        animationSpec = GlassMotion.NavSpring,
        label = "ai_ring_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = slotModifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(AiButtonDiameter)
                .pressScale(pressed = isPressed, spec = GlassMotion.PressSpring)
                .graphicsLayer {
                    scaleX = selectionScale
                    scaleY = selectionScale
                }
                // Ambient glow — strong when selected, faint when at rest.
                .then(
                    if (isSelected) Modifier.glow(shape, radius = 18.dp)
                    else Modifier.glow(shape, radius = 8.dp)
                )
                .glassBackground(shape = shape)
                // Signature thin animated blue → cyan gradient ring, drawn as a
                // hairline stroke just inside the glass border.
                .drawWithCache {
                    val strokePath = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                left = size.width * 0.06f,
                                top = size.height * 0.06f,
                                right = size.width * 0.94f,
                                bottom = size.height * 0.94f
                            )
                        )
                    }
                    val sweep = size.minDimension
                    val ringBrush = Brush.linearGradient(
                        colors = listOf(
                            colors.gradientBorderStart, // blue
                            colors.gradientBorderEnd   // cyan
                        ),
                        start = Offset(sweep * ringFraction, 0f),
                        end = Offset(0f, sweep * ringFraction)
                    )
                    onDrawWithContent {
                        drawContent()
                        drawPath(
                            path = strokePath,
                            brush = ringBrush,
                            alpha = ringAlpha,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = GlassTokens.BorderWidth.toPx() * 1.4f
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                    }
            ) {
                // The AI hero keeps its frosted glass core in BOTH themes (unlike
                // the other nav items, which sit on a solid blue/purple gradient
                // when selected). A hardcoded white tint therefore vanishes on the
                // light glass in light theme. onSurface tracks the glass surface
                // contrast in every theme (white on dark glass, dark text on light
                // glass), while onNavUnselected still handles the idle state.
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else colors.onNavUnselected,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
