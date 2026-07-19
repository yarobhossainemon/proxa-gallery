package com.emon.proxagallery.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emon.proxagallery.ui.theme.extendedColors

/**
 * Small glass circle used for pin / favorite / count badges on cards.
 *
 * Never glows — badges appear on every card, so glowing them would cheapen the
 * UI. [gradient] toggles a filled gradient circle for emphasis (favorite, pin);
 * leave it false for plain count badges.
 *
 * @param size      diameter of the badge.
 * @param gradient  when true, fills with the theme gradient instead of glass.
 */
@Composable
fun GlassBadge(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    gradient: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.extendedColors
    val shape = circleShape()
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (gradient) {
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                        ),
                        shape = shape
                    )
                } else {
                    Modifier.glassBackground(shape = shape)
                }
            ),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Convenience count badge: a [GlassBadge] rendering a number in white.
 */
@Composable
fun GlassCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    gradient: Boolean = false
) {
    GlassBadge(modifier = modifier, size = size, gradient = gradient) {
        Text(
            text = count.toString(),
            color = MaterialTheme.extendedColors.badgeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
