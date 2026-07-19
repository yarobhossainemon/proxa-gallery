package com.emon.proxagallery.ui.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * The shared motion language for every glass component.
 *
 * One file tunes the whole app's feel — this is what makes premium UIs
 * (Samsung Gallery, Pixel, Apple) read as cohesive. Every [GlassModifier] /
 * Glass* component pulls its spec from here instead of inventing its own.
 */
object GlassMotion {

    /** Generic press feedback for buttons / icons — noticeable but quick. */
    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Card press / hover scale — slightly bouncier for a tactile feel. */
    val CardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Chip selection scale — snappy, no bounce (chips should feel decisive). */
    val ChipSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Bottom-nav tab selection pop. */
    val NavSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Standard fade-in for glass surfaces appearing on screen. */
    val FadeIn: TweenSpec<Float> = tween(
        durationMillis = 220,
        easing = FastOutSlowInEasing
    )

    /** Slow ambient pulse used by the AI orb / glowing accents. */
    val GlowPulse: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(durationMillis = 2200, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
}
