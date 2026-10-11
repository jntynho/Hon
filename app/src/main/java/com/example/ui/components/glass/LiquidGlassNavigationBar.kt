package com.example.ui.components.glass

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.ui.components.glass.backdrop.GlassSurface
import com.example.ui.screens.LocalHazeState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating Liquid Glass Navigation Bar with Gesture Interactions.
 *
 * Implements:
 * 1. Deep translucent floating glass capsule with bidirectional tapering edge light.
 * 2. Diffused ambient floating shadow.
 * 3. Liquid Glass Selection Indicator with physical spring glide & long-press gesture pulse.
 * 4. Micro-haptic tactile feedback on active tab gesture.
 * 5. Adaptive landscape optimization and strict boundary clipping.
 */
@Composable
fun LiquidGlassNavigationBar(
    items: List<LiquidGlassNavItem> = LiquidGlassNavItem.defaultNavItems,
    selectedId: String,
    onItemSelected: (LiquidGlassNavItem) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: dev.chrisbanes.haze.HazeState? = LocalHazeState.current,
    onItemLongPressed: ((LiquidGlassNavItem) -> Unit)? = null,
    barHeightDp: Int = 70,
    transparency: Float = 0.65f,
    blurRadiusDp: Int = 24,
    activeTabBlurDp: Int = 16,
    activeTabIntensity: Float = 0.6f
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var pulseTrigger by remember { mutableIntStateOf(0) }

    // Precise luminance calculation using Compose's native luminance()
    val isLight = remember(palette.bg) {
        palette.bg.luminance() > 0.45f
    }
    val isDark = !isLight

    val selectedIndex by remember(selectedId, items) {
        derivedStateOf {
            items.indexOfFirst { it.id.equals(selectedId, ignoreCase = true) }
        }
    }
    val hasValidSelection = selectedIndex >= 0

    val safeHeight = barHeightDp.coerceIn(48, 80)
    val barHeight = if (isLandscape) (safeHeight * 0.82f).dp else safeHeight.dp
    val barCornerRadius = barHeight / 2
    val barShape = RoundedCornerShape(barCornerRadius)

    val unselectedItemColor = if (isDark) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color(0xFF3C404E).copy(alpha = 0.80f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = if (isLandscape) 32.dp else 16.dp,
                vertical = if (isLandscape) 6.dp else 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = if (isDark) 14.dp else 18.dp,
                    shape = barShape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color(0x241E2235),
                    spotColor = if (isDark) accent.copy(alpha = 0.22f) else Color(0x1E000000)
                )
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val itemCount = items.size.coerceAtLeast(1)
            val itemWidthPx = totalWidthPx / itemCount
            val itemWidthDp = with(density) { itemWidthPx.toDp() }

            var visualSelectedIndex by remember(selectedIndex) { mutableStateOf(selectedIndex) }
            LaunchedEffect(selectedIndex) {
                visualSelectedIndex = selectedIndex
            }

            val effectiveIndex = if (hasValidSelection) visualSelectedIndex.coerceIn(0, items.lastIndex) else 0
            val targetCenterX = (effectiveIndex + 0.5f) * itemWidthPx
            val indicatorHeight = barHeight - if (isLandscape) 8.dp else 12.dp

            var isDragging by remember { mutableStateOf(false) }
            var dragCurrentCenterX by remember { mutableStateOf<Float?>(null) }
            var dragOffsetValue by remember { mutableStateOf(0f) }
            var pressedIndex by remember { mutableStateOf<Int?>(null) }
            var previewIndex by remember { mutableStateOf<Int?>(null) }

            // LAYER 1: Deep Translucent Glass Surface with Tapering Edge Light
            GlassSurface(
                shape = barShape,
                isDark = isDark,
                accentColor = accent,
                hazeState = hazeState,
                blurRadius = blurRadiusDp.coerceIn(0, 40).dp,
                transparency = transparency.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxSize()
            )

            // LAYER 2: Moving Glass Selection Indicator (with Liquid Gesture Pulse and iOS Slide Tracking)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(barShape)
                    .padding(vertical = if (isLandscape) 4.dp else 6.dp)
            ) {
                LiquidGlassSelectionIndicator(
                    targetCenterX = targetCenterX,
                    itemWidth = itemWidthDp,
                    height = indicatorHeight,
                    accentColor = accent,
                    isDarkTheme = isDark,
                    isVisible = hasValidSelection,
                    pulseTrigger = pulseTrigger,
                    hazeState = hazeState,
                    blurRadius = activeTabBlurDp.dp,
                    intensity = activeTabIntensity,
                    dragOffset = dragOffsetValue,
                    isDragging = isDragging,
                    dragCenterX = dragCurrentCenterX,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            // LAYER 3: 100% Crisp Interactive Navigation Items with iOS Fluid Gesture Recognition
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items.size, totalWidthPx) {
                        val touchSlop = viewConfiguration.touchSlop
                        val minCenterX = 0.5f * itemWidthPx
                        val maxCenterX = (itemCount - 0.5f) * itemWidthPx

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downX = down.position.x
                            val downTime = System.currentTimeMillis()
                            val startIndex = (downX / itemWidthPx).toInt().coerceIn(0, items.lastIndex)

                            pressedIndex = startIndex
                            var hasDragStarted = false
                            var isLongPressTriggered = false
                            var lastHapticIndex = startIndex
                            val gestureStartCenterX = (visualSelectedIndex.coerceIn(0, items.lastIndex) + 0.5f) * itemWidthPx

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    break
                                }

                                val dx = change.position.x - downX

                                if (!hasDragStarted) {
                                    if (abs(dx) > touchSlop) {
                                        hasDragStarted = true
                                        isDragging = true
                                        pressedIndex = null
                                        change.consume()
                                    } else if (!isLongPressTriggered && startIndex == visualSelectedIndex && (System.currentTimeMillis() - downTime) > 400L) {
                                        isLongPressTriggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pulseTrigger++
                                        onItemLongPressed?.invoke(items[visualSelectedIndex])
                                    }
                                } else {
                                    change.consume()
                                    val currentTotalDragX = change.position.x
                                    val rawCenterX = gestureStartCenterX + (currentTotalDragX - downX)
                                    val clampedCenterX = if (rawCenterX < minCenterX) {
                                        minCenterX - (minCenterX - rawCenterX) * 0.35f
                                    } else if (rawCenterX > maxCenterX) {
                                        maxCenterX + (rawCenterX - maxCenterX) * 0.35f
                                    } else {
                                        rawCenterX
                                    }

                                    dragCurrentCenterX = clampedCenterX
                                    dragOffsetValue = clampedCenterX - gestureStartCenterX

                                    val currentHoverIndex = (clampedCenterX / itemWidthPx).toInt().coerceIn(0, items.lastIndex)
                                    if (currentHoverIndex != lastHapticIndex) {
                                        lastHapticIndex = currentHoverIndex
                                        previewIndex = currentHoverIndex
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }

                            // Pointer lifted (Up) or cancelled
                            pressedIndex = null
                            if (hasDragStarted) {
                                val releasedX = dragCurrentCenterX ?: gestureStartCenterX
                                val finalIndex = (releasedX / itemWidthPx).roundToInt().coerceIn(0, items.lastIndex)

                                previewIndex = null
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                // Immediately commit visual selection so indicator springs smoothly to target with zero recoil
                                visualSelectedIndex = finalIndex
                                isDragging = false
                                dragCurrentCenterX = null
                                dragOffsetValue = 0f

                                if (finalIndex != selectedIndex) {
                                    onItemSelected(items[finalIndex])
                                }
                            } else if (!isLongPressTriggered) {
                                val elapsed = System.currentTimeMillis() - downTime
                                if (elapsed < 600L) {
                                    val tappedIndex = (downX / itemWidthPx).toInt().coerceIn(0, items.lastIndex)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    visualSelectedIndex = tappedIndex
                                    onItemSelected(items[tappedIndex])
                                }
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isItemSelected = if (isDragging && previewIndex != null) index == previewIndex else index == visualSelectedIndex
                    val isItemPressed = pressedIndex == index
                    LiquidGlassNavigationItem(
                        item = item,
                        isSelected = isItemSelected,
                        itemWidth = itemWidthDp,
                        accentColor = accent,
                        unselectedColor = unselectedItemColor,
                        onClick = {
                            visualSelectedIndex = index
                            onItemSelected(item)
                        },
                        onLongClick = {
                            if (index == visualSelectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pulseTrigger++
                                onItemLongPressed?.invoke(item)
                            }
                        },
                        isPressed = isItemPressed
                    )
                }
            }
        }
    }
}
