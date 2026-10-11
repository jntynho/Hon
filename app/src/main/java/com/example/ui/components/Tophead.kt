package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.LocalHazeState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalVaultPalette
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Configuration for application-wide liquid glass top bars / headers.
 */
data class TopheadGlassConfig(
    val isGlassEnabled: Boolean = false,
    val intensity: Float = 0.65f
)

val LocalTopheadGlassConfig = compositionLocalOf { TopheadGlassConfig() }

/**
 * Modifier extension to apply frosted liquid glass blur or solid surface to screen headers.
 */
@Composable
fun Modifier.glassTopheadBackground(
    glassConfig: TopheadGlassConfig = LocalTopheadGlassConfig.current,
    showBorder: Boolean = false
): Modifier {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val hazeState = LocalHazeState.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.45f
    val isDark = !isLight

    if (glassConfig.isGlassEnabled) {
        val intensity = glassConfig.intensity.coerceIn(0.2f, 1.0f)
        val tintAlpha = if (isDark) {
            (0.85f * (1f - intensity * 0.65f)).coerceIn(0.12f, 0.85f)
        } else {
            (0.88f * (1f - intensity * 0.65f)).coerceIn(0.15f, 0.88f)
        }
        val blurRadius = (28f * intensity).coerceIn(6f, 36f).dp

        val edgeLightDraw: DrawScope.() -> Unit = {
            // Draw specular bottom hairline edge light just like liquid glass
            val strokePx = 1.dp.toPx()
            val hairlineBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (isDark) 0.15f else 0.25f),
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.70f),
                    accent.copy(alpha = if (isDark) 0.35f else 0.50f),
                    Color.White.copy(alpha = if (isDark) 0.30f else 0.55f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = size.width
            )
            drawLine(
                brush = hairlineBrush,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = strokePx
            )
        }

        return if (hazeState != null) {
            val hazeStyle = HazeStyle(
                backgroundColor = Color.Transparent,
                tint = HazeTint(
                    color = if (isDark) {
                        Color(0xFF10121A).copy(alpha = tintAlpha)
                    } else {
                        Color.White.copy(alpha = tintAlpha)
                    }
                ),
                blurRadius = blurRadius,
                noiseFactor = 0.02f
            )
            this
                .hazeChild(
                    state = hazeState,
                    style = hazeStyle
                )
                .drawBehind(edgeLightDraw)
        } else {
            val fallbackBrush = Brush.verticalGradient(
                colors = listOf(
                    if (isDark) Color(0xFF161822).copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha),
                    if (isDark) Color(0xFF0F1018).copy(alpha = tintAlpha) else Color(0xFFF0F3F8).copy(alpha = tintAlpha)
                )
            )
            this
                .background(fallbackBrush)
                .drawBehind(edgeLightDraw)
        }
    } else {
        val borderMod = if (showBorder) {
            Modifier.drawBehind {
                drawLine(
                    color = palette.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        } else Modifier
        return this
            .background(palette.surface)
            .then(borderMod)
    }
}

/**
 * Types of standard navigation icons supported by AppTophead.
 */
enum class NavIconType {
    NONE,
    BACK,
    DRAWER,
    CLOSE
}

/**
 * AppTophead - Unified Master Top Bar Component for all screens.
 * 
 * Unifies header behavior across the entire app:
 * - Standard, Drawer, Close, or Custom navigation icons with haptic feedback.
 * - Standard title, subtitle, or custom animated/branded title composable.
 * - Integrated, smooth expandable search field with clear/close actions.
 * - Center-aligned mode (e.g. for Settings).
 * - Palette-aware container, content, and optional border colors.
 * - Flexible trailing action buttons slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTophead(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    centerTitle: Boolean = false,
    navIconType: NavIconType = NavIconType.BACK,
    onNavClick: (() -> Unit)? = null,
    navContentDescription: String? = null,
    navTestTag: String = "back_button",
    customNavigationIcon: (@Composable () -> Unit)? = null,
    // Search capability
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: ((Boolean) -> Unit)? = null,
    searchPlaceholder: String = "Search...",
    searchTestTag: String = "search_input",
    onSearchSubmit: (() -> Unit)? = null,
    // Title styling & custom slots
    titleColor: Color? = null,
    customTitleContent: (@Composable () -> Unit)? = null,
    showBorder: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalVaultPalette.current
    val accent = LocalAccentColor.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val primaryColor = titleColor ?: palette.textPrimary

    val borderModifier = if (showBorder) {
        Modifier.drawBehind {
            drawLine(
                color = palette.border,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    } else Modifier

    val titleContentComposable: @Composable () -> Unit = {
        if (isSearchExpanded && searchQuery != null && onSearchQueryChange != null) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = palette.textPrimary,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearchSubmit?.invoke()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(searchTestTag),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = searchPlaceholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = palette.textSecondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        } else if (customTitleContent != null) {
            customTitleContent()
        } else if (subtitle.isNullOrBlank()) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = primaryColor
            )
        } else {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = primaryColor
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }

    val navIconComposable: @Composable () -> Unit = {
        if (customNavigationIcon != null) {
            customNavigationIcon()
        } else if (isSearchExpanded && onSearchExpandedChange != null) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSearchExpandedChange(false)
                    onSearchQueryChange?.invoke("")
                },
                modifier = Modifier.testTag("close_search_nav_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close Search",
                    tint = palette.textPrimary
                )
            }
        } else {
            when (navIconType) {
                NavIconType.BACK -> {
                    if (onNavClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavClick()
                            },
                            modifier = Modifier.testTag(navTestTag)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = navContentDescription ?: "Back",
                                tint = primaryColor
                            )
                        }
                    }
                }
                NavIconType.DRAWER -> {
                    if (onNavClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavClick()
                            },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_app_menu),
                                contentDescription = navContentDescription ?: "Open Drawer",
                                tint = primaryColor
                            )
                        }
                    }
                }
                NavIconType.CLOSE -> {
                    if (onNavClick != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavClick()
                            },
                            modifier = Modifier.testTag(navTestTag.ifEmpty { "close_button" })
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = navContentDescription ?: "Close",
                                tint = primaryColor
                            )
                        }
                    }
                }
                NavIconType.NONE -> { /* No navigation icon */ }
            }
        }
    }

    val actionsComposable: @Composable RowScope.() -> Unit = {
        if (isSearchExpanded && searchQuery != null && onSearchQueryChange != null) {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchQueryChange("")
                    },
                    modifier = Modifier.testTag("clear_search_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_cancel),
                        contentDescription = "Clear Search",
                        tint = palette.textSecondary
                    )
                }
            } else if (onSearchExpandedChange != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSearchExpandedChange(false)
                        onSearchQueryChange("")
                    },
                    modifier = Modifier.testTag("close_search_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_cancel),
                        contentDescription = "Close Search",
                        tint = palette.textSecondary
                    )
                }
            }
        } else {
            actions()
        }
    }

    val glassConfig = LocalTopheadGlassConfig.current
    val containerColor = if (glassConfig.isGlassEnabled) Color.Transparent else palette.surface

    if (centerTitle) {
        CenterAlignedTopAppBar(
            modifier = modifier.glassTopheadBackground(glassConfig, showBorder),
            title = titleContentComposable,
            navigationIcon = navIconComposable,
            actions = actionsComposable,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = containerColor,
                titleContentColor = palette.textPrimary,
                navigationIconContentColor = palette.textPrimary,
                actionIconContentColor = palette.textPrimary
            )
        )
    } else {
        TopAppBar(
            modifier = modifier.glassTopheadBackground(glassConfig, showBorder),
            title = titleContentComposable,
            navigationIcon = navIconComposable,
            actions = actionsComposable,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = palette.textPrimary,
                navigationIconContentColor = palette.textPrimary,
                actionIconContentColor = palette.textPrimary
            )
        )
    }
}

/**
 * Tophead - Convenience helper for standard screens (Add/Edit Scene, Details, etc.)
 */
@Composable
fun Tophead(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    showBorder: Boolean = false,
    titleColor: Color? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    AppTophead(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navIconType = if (onBack != null) NavIconType.BACK else NavIconType.NONE,
        onNavClick = onBack,
        navContentDescription = backContentDescription,
        showBorder = showBorder,
        titleColor = titleColor,
        actions = actions
    )
}

/**
 * SearchableTophead - Convenience helper for searchable management screens (Actors, Studios, Bookmarks)
 */
@Composable
fun SearchableTophead(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    testTag: String = "search_input",
    onBack: (() -> Unit)? = null,
    showBorder: Boolean = false,
    customTitleContent: (@Composable () -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    AppTophead(
        title = title,
        modifier = modifier,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        isSearchExpanded = isSearchExpanded,
        onSearchExpandedChange = onSearchExpandedChange,
        searchPlaceholder = placeholder,
        searchTestTag = testTag,
        navIconType = if (onBack != null) NavIconType.BACK else NavIconType.NONE,
        onNavClick = onBack,
        showBorder = showBorder,
        customTitleContent = customTitleContent,
        actions = actions
    )
}
