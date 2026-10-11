package com.example.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Individual navigation tab item for Liquid Glass Bar with Gesture Support.
 *
 * Implements:
 * - Tap to switch tab.
 * - Short long-press on Active Tab to trigger liquid gesture recoil.
 * - Tight vertical centering without text shadows or icon glares.
 */
@Composable
fun LiquidGlassNavigationItem(
    item: LiquidGlassNavItem,
    isSelected: Boolean,
    itemWidth: Dp,
    accentColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isPressed: Boolean = false
) {
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isHighFontScale = density.fontScale > 1.25f

    // Immediate tactile feedback upon touch down
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = 800f),
        label = "glass_press_scale"
    )

    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "glass_selected_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else unselectedColor,
        animationSpec = spring(stiffness = 400f),
        label = "glass_icon_color"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.70f,
        animationSpec = spring(stiffness = 400f),
        label = "glass_label_alpha"
    )

    val titleText = if (item.titleRes != 0) stringResource(id = item.titleRes) else item.title

    // Proportional icon sizing for 70dp bar height (increased from 20dp to 24dp for crisp readability)
    val iconSize = if (isLandscape) 22.dp else if (isHighFontScale) 26.dp else 24.dp

    Box(
        modifier = modifier
            .width(itemWidth)
            .fillMaxHeight()
            .semantics {
                this.role = Role.Tab
                this.selected = isSelected
                this.contentDescription = titleText
                this.onClick {
                    onClick()
                    true
                }
            }
            .testTag("nav_item_${item.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(pressScale * selectedScale)
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            if (!isHighFontScale) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = titleText,
                    fontSize = if (isLandscape) 9.sp else 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    style = androidx.compose.material3.LocalTextStyle.current.copy(shadow = null),
                    color = contentColor.copy(alpha = labelAlpha),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
