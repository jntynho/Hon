package com.example.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

import androidx.compose.ui.draw.blur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Pure Liquid Glass Selection Indicator with Gesture Motion Support & Active Tab Blur.
 *
 * Implements:
 * - Perfectly circular, harmonious pill shape (RoundedCornerShape(percent = 50)).
 * - Dynamic customizable backdrop blur & translucent glass gradient.
 * - Reactive liquid elastic pulse animation on gesture / long-press.
 * - Zero re-layout overhead (uses graphicsLayer translationX & scaleX).
 */
@Composable
fun LiquidGlassSelectionIndicator(
    targetCenterX: Float,
    itemWidth: Dp,
    height: Dp,
    accentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    pulseTrigger: Int = 0,
    hazeState: HazeState? = null,
    blurRadius: Dp = 16.dp,
    intensity: Float = 0.6f,
    dragOffset: Float = 0f,
    isDragging: Boolean = false,
    dragCenterX: Float? = null
) {
    val positionAnim = remember { Animatable(targetCenterX) }
    val stretchAnim = remember { Animatable(1.0f) }
    val alphaAnim = remember { Animatable(if (isVisible) 1f else 0f) }
    val pulseScale = remember { Animatable(1.0f) }

    // Synchronously track pointer drag position with 120Hz continuous responsiveness
    LaunchedEffect(dragCenterX, isDragging) {
        if (isDragging && dragCenterX != null) {
            positionAnim.snapTo(dragCenterX)
        }
    }

    LaunchedEffect(targetCenterX, isVisible, isDragging) {
        if (!isVisible) {
            alphaAnim.animateTo(0f, animationSpec = tween(150))
            return@LaunchedEffect
        }
        if (isDragging) {
            // Drag tracking is handled directly above
            return@LaunchedEffect
        }
        if (alphaAnim.value < 1f) {
            launch { alphaAnim.animateTo(1f, animationSpec = tween(150)) }
        }

        val distance = abs(targetCenterX - positionAnim.value)
        if (distance > 1.5f) {
            val targetStretch = 1.0f + (distance / 200f).coerceIn(0.08f, 0.22f)

            // GPU lateral stretch impulse
            launch {
                stretchAnim.animateTo(
                    targetValue = targetStretch,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                stretchAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }

            // Damped physical spring navigation without recoil
            positionAnim.animateTo(
                targetValue = targetCenterX,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 400f
                )
            )
        } else {
            positionAnim.snapTo(targetCenterX)
        }
    }

    // Trigger elastic liquid pulse when gesture is recognized
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            launch {
                pulseScale.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 600f
                    )
                )
                pulseScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 350f
                    )
                )
            }
        }
    }

    // Circular harmonious pill geometry
    val baseWidth = (itemWidth - 10.dp).coerceIn(44.dp, 72.dp)
    val pillShape = RoundedCornerShape(percent = 50)

    val clampedIntensity = intensity.coerceIn(0f, 1f)
    val factor = clampedIntensity / 0.6f

    // Pure translucent liquid glass gradient modulated by density/intensity
    val glassIndicatorGradient = if (isDarkTheme) {
        listOf(
            Color.White.copy(alpha = (0.16f * factor).coerceIn(0f, 0.45f)),
            Color.White.copy(alpha = (0.09f * factor).coerceIn(0f, 0.30f)),
            Color.White.copy(alpha = (0.06f * factor).coerceIn(0f, 0.22f))
        )
    } else {
        listOf(
            Color.White.copy(alpha = (0.80f * factor).coerceIn(0f, 0.98f)),
            Color.White.copy(alpha = (0.58f * factor).coerceIn(0f, 0.90f)),
            Color.White.copy(alpha = (0.40f * factor).coerceIn(0f, 0.80f))
        )
    }

    // Subtle, clean specular glass perimeter border modulated by intensity
    val glassBorderBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) {
            listOf(
                Color.White.copy(alpha = (0.40f * factor).coerceIn(0f, 0.85f)),
                Color.White.copy(alpha = (0.14f * factor).coerceIn(0f, 0.50f)),
                Color.White.copy(alpha = (0.06f * factor).coerceIn(0f, 0.30f))
            )
        } else {
            listOf(
                Color.White.copy(alpha = (0.85f * factor).coerceIn(0f, 1.0f)),
                Color.White.copy(alpha = (0.40f * factor).coerceIn(0f, 0.85f)),
                Color.White.copy(alpha = (0.18f * factor).coerceIn(0f, 0.60f))
            )
        }
    )

    val effectiveBlurRadius = blurRadius.coerceIn(0.dp, 40.dp)
    val indicatorHazeStyle = remember(isDarkTheme, effectiveBlurRadius, accentColor, factor) {
        HazeStyle(
            backgroundColor = if (isDarkTheme) {
                Color.Black.copy(alpha = (0.20f * factor).coerceIn(0f, 0.50f))
            } else {
                Color.White.copy(alpha = (0.35f * factor).coerceIn(0f, 0.70f))
            },
            tints = listOf(
                HazeTint(
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = (0.12f * factor).coerceIn(0f, 0.35f))
                    } else {
                        Color.White.copy(alpha = (0.45f * factor).coerceIn(0f, 0.85f))
                    }
                ),
                HazeTint(
                    color = accentColor.copy(alpha = (0.08f * factor).coerceIn(0f, 0.35f))
                )
            ),
            blurRadius = effectiveBlurRadius,
            noiseFactor = (0.02f * factor).coerceIn(0f, 0.05f)
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val interactiveStretch = if (isDragging && dragOffset != 0f) {
                    1.0f + (abs(dragOffset) / 320f).coerceIn(0f, 0.16f)
                } else 1.0f

                translationX = positionAnim.value - (baseWidth.toPx() / 2f)
                scaleX = stretchAnim.value * pulseScale.value * interactiveStretch
                scaleY = pulseScale.value
                alpha = alphaAnim.value
            }
            .width(baseWidth)
            .height(height)
            .clip(pillShape)
            .then(
                if (hazeState != null && effectiveBlurRadius > 0.dp) {
                    Modifier.hazeChild(
                        state = hazeState,
                        shape = pillShape,
                        style = indicatorHazeStyle
                    )
                } else if (effectiveBlurRadius > 0.dp) {
                    Modifier.blur(effectiveBlurRadius)
                } else {
                    Modifier
                }
            )
            .background(Brush.verticalGradient(glassIndicatorGradient))
            .border(
                width = 1.dp,
                brush = glassBorderBrush,
                shape = pillShape
            )
    )
}
