package com.emon.proxagallery.ui.glass

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emon.proxagallery.data.ThemeMode
import com.emon.proxagallery.ui.theme.ProxaGalleryTheme
import com.emon.proxagallery.ui.theme.extendedColors
import androidx.compose.ui.graphics.vector.ImageVector.Builder as VectorBuilder

// ───────────────────────────────────────────────────────────────────────────
//  SignatureGlassNav — the visual identity of Proxa Gallery.
//
//  One continuous glass surface. The dock's top edge rises in a gentle convex
//  "molten hill" that the AI button grows out of — it is NOT a floating action
//  button, and there is NO notch, hole, or cutout. ~30% of the AI button sits
//  above the dock; ~70% is visually embedded.
//
//  Four dock items:  Gallery · Albums   [AI]   Library · Settings
//  The AI exists only as the centerpiece — it is never a tab.
//
//  Built entirely on the existing glass design system (glassBackground, glow,
//  animatedGradientBorder, pressScale, GlassMotion, ExtendedColors) so the
//  signature surface stays consistent with every other glass component.
// ───────────────────────────────────────────────────────────────────────────

/**
 * Single source of truth for the signature nav's dimensions.
 *
 * Mirrors the [GlassTokens] convention: every radius, gap and size lives here
 * so tuning the silhouette is a one-line change instead of a magic-number hunt.
 */
private object SignatureNavTokens {
    /** Hero AI button diameter (brief: 74–78dp). */
    val AiButtonDiameter = 76.dp

    /** Fraction of the AI diameter that rises ABOVE the dock (brief: ~30%). */
    const val AiRiseFraction = 0.30f

    /** Fraction of the AI diameter the dock hill rises above the baseline top. */
    const val HillRiseFraction = 0.72f

    /**
     * How tightly the hill hugs the button silhouette. 0.46 → the glass
     * curves up to meet ~70% of the button's side; a smaller value hugs
     * tighter, a larger value lets the button float more.
     */
    const val HillHugFraction = 0.46f

    /** Dock body height, excluding the central hill rise. */
    val DockHeight = 64.dp

    /** Outer horizontal margin so the dock doesn't kiss the screen edges. */
    val DockHorizontalPadding = 16.dp

    /** Bottom gutter; clears the gesture-nav inset (we also add navigationBarsPadding). */
    val DockBottomGutter = 8.dp

    /** Corner radius of the dock's four outer corners (One UI soft square). */
    val DockCornerRadius = 30.dp

    /** Nav icon (Samsung One UI outline) size. */
    val NavIconSize = 24.dp

    /** Nav label text size — always visible per brief. */
    val NavLabelSp = 11.sp

    /** Glow halo radius under the AI button — restrained, not neon. */
    val AiGlowRadius = 22.dp

    /** Bézier horizontal control fraction (broadness of the bell at the base). */
    const val BellCtrlHorizFraction = 0.62f

    /** Bézier vertical control fraction (keeps the apex tangent flat). */
    const val BellCtrlVertFraction = 0.55f
}

/**
 * The molten-glass dock silhouette: a soft-cornered rectangle whose top edge
 * rises smoothly into a convex hill at horizontal center. There is no notch
 * and no hole — the hill is traced with two cubic Bézier curves whose
 * tangents are flat at both base and apex, producing a natural bell rather
 * than a mathematical semicircle.
 *
 * All geometry is resolved against the measured size in [createOutline]; the
 * hill is always horizontally centered.
 *
 * @param hillWidthPx   total width of the hill base, in pixels.
 * @param hillHeightPx  height the hill rises above the dock's flat top, in pixels.
 * @param cornerRadiusPx outer-corner radius, in pixels.
 */
private class SignatureDockShape(
    private val hillWidthPx: Float,
    private val hillHeightPx: Float,
    private val cornerRadiusPx: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val corner = cornerRadiusPx

        // The flat top of the dock sits at y = hillHeightPx; the bell peaks
        // at y = 0. The bottom of the dock is at y = h.
        val topFlatY = hillHeightPx
        val apexY = 0f

        val centerX = w / 2f
        val halfHill = hillWidthPx / 2f
        val hillLeft = (centerX - halfHill).coerceIn(corner, w - corner)
        val hillRight = (centerX + halfHill).coerceIn(corner, w - corner)
        val ctrlHoriz = halfHill * SignatureNavTokens.BellCtrlHorizFraction
        val ctrlVert = hillHeightPx * SignatureNavTokens.BellCtrlVertFraction

        val topLeftEnd = hillLeft - ctrlHoriz
        val topRightStart = hillRight + ctrlHoriz

        val path = Path().apply {
            // Top edge, starting just past the top-left corner.
            moveTo(corner, topFlatY)
            lineTo(topLeftEnd, topFlatY)
            // Left half of the bell: flat tangent at base, rising convex.
            cubicTo(
                hillLeft, topFlatY,
                hillLeft, apexY,
                centerX, apexY
            )
            // Right half: mirrors the left, returning to the flat top.
            cubicTo(
                hillRight, apexY,
                hillRight, topFlatY,
                topRightStart, topFlatY
            )
            lineTo(w - corner, topFlatY)
            // Top-right corner.
            quadraticTo(w, topFlatY, w, topFlatY + corner)
            lineTo(w, h - corner)
            // Bottom-right corner.
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            // Bottom-left corner.
            quadraticTo(0f, h, 0f, h - corner)
            lineTo(0f, topFlatY + corner)
            // Top-left corner.
            quadraticTo(0f, topFlatY, corner, topFlatY)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * The Proxa brand mark: an aperture whose blades resolve into a soft four-point
 * spark at the center. Original, restrained, and theme-agnostic (drawn white
 * over the AI button). NOT the word "AI" and NOT Material's AutoAwesome.
 *
 * Kept intentionally geometric and hairline so it reads as luxury glass rather
 * than a sticker.
 */
private val ProxaSymbol: ImageVector by lazy {
    VectorBuilder(
        name = "ProxaSymbol",
        defaultWidth = 28.dp,
        defaultHeight = 28.dp,
        viewportWidth = 28f,
        viewportHeight = 28f
    ).apply {
        // Four-point spark — the soft "light" core of the mark.
        path(fill = SolidColor(Color.White.copy(alpha = 0.95f))) {
            moveTo(14f, 4f)
            curveTo(14.6f, 9.2f, 18.8f, 13.4f, 24f, 14f)
            curveTo(18.8f, 14.6f, 14.6f, 18.8f, 14f, 24f)
            curveTo(13.4f, 18.8f, 9.2f, 14.6f, 4f, 14f)
            curveTo(9.2f, 13.4f, 13.4f, 9.2f, 14f, 4f)
            close()
        }
        // Inner frosted dot — the "lens" — slightly cooler so it reads as glass.
        // Drawn as two cubic Bézier curves approximating a circle (simpler
        // than matching PathBuilder.arcTo's exact parameter names).
        path(fill = SolidColor(Color.White.copy(alpha = 0.55f))) {
            moveTo(14f, 11.6f)
            curveTo(17.2f, 11.6f, 17.2f, 16.4f, 14f, 16.4f)
            curveTo(10.8f, 16.4f, 10.8f, 11.6f, 14f, 11.6f)
            close()
        }
    }.build()
}

/**
 * The hero. A single piece of premium glass the AI button grows out of:
 *
 * - ambient cyan halo via [glow] (restrained — uses [ExtendedColors.glowColor])
 * - slow 2.2s breathing pulse on scale (existing [GlassMotion.GlowPulse] cadence)
 * - frosted-glass body via [glassBackground] strong
 * - thin blue→purple→cyan animated gradient ring ([animatedGradientBorder])
 * - thin white top highlight (built into [glassBackground])
 * - press feedback via [pressScale]
 *
 * Nothing about this reads as a Material FAB: there is no elevated circular
 * shadow plate; the dock visually continues into it.
 */
@Composable
private fun AiGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Breathing: a subtle scale pulse. Uses the GlowPulse cadence (2.2s,
    // reverse) so it matches every other glowing accent in the system.
    val infinite = rememberInfiniteTransition(label = "ai_breath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_breath_value"
    )
    val breathScale = 1f + 0.035f * breath // 1.00 → 1.035

    Box(
        modifier = modifier
            .size(SignatureNavTokens.AiButtonDiameter)
            .pressScale(pressed = pressed, spec = GlassMotion.NavSpring)
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            }
            .glow(circleShape(), radius = SignatureNavTokens.AiGlowRadius)
            .glassBackground(shape = circleShape(), strong = true)
            .animatedGradientBorder(shape = circleShape(), active = true)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ProxaSymbol,
            contentDescription = "Proxa AI",
            tint = Color.White,
            modifier = Modifier.size(SignatureNavTokens.AiButtonDiameter * 0.42f)
        )
    }
}

/**
 * One of the four dock items — Samsung One UI outline icon + always-visible
 * label. Active item takes the blue accent and a spring scale pop; inactive
 * items use [ExtendedColors.onNavUnselected]. Glow is reserved for the AI
 * button and is never applied here.
 */
@Composable
private fun SignatureNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.extendedColors
    val tint = if (selected) colors.gradientStart else colors.onNavUnselected
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = GlassMotion.NavSpring,
        label = "nav_item_scale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(SignatureNavTokens.NavIconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = SignatureNavTokens.NavLabelSp,
            // Slightly heavier when active so the label participates in the
            // active state without adding glow.
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Navigation item descriptor for the signature dock.
 */
data class SignatureNavItemSpec(
    val icon: ImageVector,
    val label: String
)

/**
 * The default four dock items, in placement order (left pair, right pair):
 * Gallery · Albums  [AI]  Library · Settings.
 *
 * There is intentionally NO AI tab — the AI exists only as the centerpiece.
 */
val SignatureNavItems: List<SignatureNavItemSpec> = listOf(
    SignatureNavItemSpec(Icons.Rounded.Photo, "Gallery"),
    SignatureNavItemSpec(Icons.Rounded.Folder, "Albums"),
    SignatureNavItemSpec(Icons.Rounded.CollectionsBookmark, "Library"),
    SignatureNavItemSpec(Icons.Rounded.Settings, "Settings")
)

/**
 * The signature bottom navigation.
 *
 * @param selectedTab   index into [SignatureNavItems] (0..3).
 * @param onTabSelected invoked with the new index when a dock item is tapped.
 * @param onAiClick     invoked when the AI centerpiece is tapped.
 */
@Composable
fun SignatureGlassNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier,
    items: List<SignatureNavItemSpec> = SignatureNavItems
) {
    require(items.size == 4) {
        "SignatureGlassNav is designed for exactly four dock items (got ${items.size})."
    }

    val density = LocalDensity.current
    val aiDiameterPx = with(density) { SignatureNavTokens.AiButtonDiameter.toPx() }
    val hillWidthPx = aiDiameterPx * 2f * SignatureNavTokens.HillHugFraction
    val hillHeightPx = aiDiameterPx * SignatureNavTokens.HillRiseFraction
    val cornerPx = with(density) { SignatureNavTokens.DockCornerRadius.toPx() }
    val dockShape = remember(hillWidthPx, hillHeightPx, cornerPx) {
        SignatureDockShape(hillWidthPx, hillHeightPx, cornerPx)
    }

    // Total outer height = dock body + the hill rise. The AI button's vertical
    // offset (below) is computed so exactly AiRiseFraction of its diameter
    // protrudes above the hill apex (y = 0 of this Box).
    val totalHeight = SignatureNavTokens.DockHeight +
        (SignatureNavTokens.AiButtonDiameter * SignatureNavTokens.HillRiseFraction)

    // The AI button's top is offset DOWN from the Box top by
    // (diameter - rise), leaving `rise` of it above the hill apex. This is the
    // brief's 30/70 embedding: 30% floats above the dock, 70% is embedded.
    val aiTopOffset = SignatureNavTokens.AiButtonDiameter *
        (1f - SignatureNavTokens.AiRiseFraction)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        // ── The one continuous glass surface: dock body + molten hill. ──────
        // clip() first so the glassBackground fill follows the silhouette, then
        // glassBackground paints the frosted fill + hairline border + top
        // highlight inside that clip. One shape, one surface.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(dockShape)
                .glassBackground(shape = dockShape, strong = true)
        )

        // ── Nav items, pinned to the dock body (below the hill). ────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(SignatureNavTokens.DockHeight)
                .padding(horizontal = SignatureNavTokens.DockHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left pair: Gallery · Albums.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignatureNavItem(
                    icon = items[0].icon,
                    label = items[0].label,
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                SignatureNavItem(
                    icon = items[1].icon,
                    label = items[1].label,
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
            }

            // A gap roughly the AI button's width keeps the right pair clear of
            // the embedded portion of the centerpiece.
            Spacer(Modifier.width(SignatureNavTokens.AiButtonDiameter * 0.9f))

            // Right pair: Library · Settings.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignatureNavItem(
                    icon = items[2].icon,
                    label = items[2].label,
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                SignatureNavItem(
                    icon = items[3].icon,
                    label = items[3].label,
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }

        // ── The AI centerpiece, horizontally centered, vertically embedded. ─
        AiGlassButton(
            onClick = onAiClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = aiTopOffset)
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
//  Previews — Dark (flagship), Light.
//  Each renders the nav over a representative background so the glass reads
//  realistically, not on a flat void.
// ───────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SignatureGlassNavDarkPreview() {
    ProxaGalleryTheme(themeMode = ThemeMode.DARK) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            SignatureGlassNav(
                selectedTab = 0,
                onTabSelected = {},
                onAiClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SignatureNavTokens.DockBottomGutter)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SignatureGlassNavLightPreview() {
    ProxaGalleryTheme(themeMode = ThemeMode.LIGHT) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            SignatureGlassNav(
                selectedTab = 2,
                onTabSelected = {},
                onAiClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SignatureNavTokens.DockBottomGutter)
            )
        }
    }
}
