package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Adaptive layout utilities for optimizing the app experience on large screens
 * including foldables like Galaxy Z Fold 6
 */

enum class ScreenSize {
    COMPACT,    // Phone portrait, narrow screens
    MEDIUM,     // Phone landscape, small tablets
    EXPANDED    // Large tablets, foldables unfolded
}

data class AdaptiveLayoutInfo(
    val screenSize: ScreenSize,
    val isLandscape: Boolean,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val useTwoColumns: Boolean,
    val useNavigationRail: Boolean,
    val useMasterDetail: Boolean,
    val contentPadding: Dp
)

@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = screenWidth > screenHeight
    
    // Define breakpoints based on Material 3 guidelines
    val screenSize = when {
        screenWidth < 600.dp -> ScreenSize.COMPACT
        screenWidth < 840.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
    
    return AdaptiveLayoutInfo(
        screenSize = screenSize,
        isLandscape = isLandscape,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        useTwoColumns = screenSize == ScreenSize.EXPANDED || (screenSize == ScreenSize.MEDIUM && isLandscape),
        useNavigationRail = screenSize == ScreenSize.EXPANDED,
        useMasterDetail = screenSize == ScreenSize.EXPANDED || (screenSize == ScreenSize.MEDIUM && isLandscape),
        contentPadding = when (screenSize) {
            ScreenSize.COMPACT -> 16.dp
            ScreenSize.MEDIUM -> 24.dp
            ScreenSize.EXPANDED -> 32.dp
        }
    )
}

/**
 * Adaptive grid column count based on screen size
 */
@Composable
fun adaptiveColumnCount(
    compact: Int = 1,
    medium: Int = 2,
    expanded: Int = 3
): Int {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> medium
        ScreenSize.EXPANDED -> expanded
    }
}

/**
 * Adaptive spacing based on screen size
 */
@Composable
fun adaptiveSpacing(
    compact: Dp = 8.dp,
    medium: Dp = 12.dp,
    expanded: Dp = 16.dp
): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> medium
        ScreenSize.EXPANDED -> expanded
    }
}

/**
 * Adaptive content width with maximum constraints for readability
 */
@Composable
fun adaptiveContentWidth(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> layoutInfo.screenWidth
        ScreenSize.MEDIUM -> minOf(layoutInfo.screenWidth, 720.dp)
        ScreenSize.EXPANDED -> minOf(layoutInfo.screenWidth * 0.8f, 1200.dp)
    }
}

/**
 * Helper to determine if we should use master-detail pattern
 */
@Composable
fun shouldUseMasterDetail(): Boolean {
    return rememberAdaptiveLayoutInfo().useMasterDetail
}

/**
 * Helper to determine if we should use two-column layout
 */
@Composable
fun shouldUseTwoColumns(): Boolean {
    return rememberAdaptiveLayoutInfo().useTwoColumns
}

/**
 * Adaptive text sizing based on layout context
 */
@Composable
fun adaptiveTextSize(
    baseSize: TextUnit,
    compactMultiplier: Float = 0.85f,
    mediumMultiplier: Float = 0.9f,
    expandedMultiplier: Float = 1f
): TextUnit {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val multiplier = when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> compactMultiplier
        ScreenSize.MEDIUM -> if (layoutInfo.useTwoColumns) mediumMultiplier else compactMultiplier
        ScreenSize.EXPANDED -> if (layoutInfo.useTwoColumns) expandedMultiplier * 0.8f else expandedMultiplier
    }
    return baseSize * multiplier
}

/**
 * Adaptive padding for widget internals
 */
@Composable
fun adaptivePadding(
    compact: Dp = 12.dp,
    medium: Dp = 16.dp,
    expanded: Dp = 20.dp
): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> compact
        ScreenSize.MEDIUM -> if (layoutInfo.useTwoColumns) compact else medium
        ScreenSize.EXPANDED -> if (layoutInfo.useTwoColumns) medium else expanded
    }
}

/**
 * Smart column count that considers minimum widget width
 */
@Composable
fun smartColumnCount(
    minWidgetWidth: Dp = 280.dp
): Int {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val availableWidth = layoutInfo.screenWidth - (layoutInfo.contentPadding * 2)
    val spacingNeeded = adaptiveSpacing() * 2 // Spacing between 3 columns
    
    return when {
        availableWidth < minWidgetWidth -> 1
        availableWidth < (minWidgetWidth * 2 + spacingNeeded) -> 2
        layoutInfo.screenSize == ScreenSize.EXPANDED -> 3
        else -> 2
    }
}

/**
 * Determine if widgets are in compact mode (narrow columns)
 */
@Composable
fun isCompactWidgetMode(): Boolean {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val columnCount = smartColumnCount()
    val availableWidthPerWidget = (layoutInfo.screenWidth - (layoutInfo.contentPadding * 2)) / columnCount
    // More lenient threshold for better large screen experience
    return availableWidthPerWidget < 200.dp
}

/**
 * Determine if difficulty badges should use compact mode
 */
@Composable
fun isCompactBadgeMode(): Boolean {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val columnCount = smartColumnCount()
    val availableWidthPerWidget = (layoutInfo.screenWidth - (layoutInfo.contentPadding * 2)) / columnCount
    // Only use compact badges on very small widgets (like phone portrait mode)
    return availableWidthPerWidget < 180.dp
}

/**
 * Get optimal widget height for consistent grid appearance
 */
@Composable
fun adaptiveWidgetHeight(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val isCompact = isCompactWidgetMode()
    
    return when {
        isCompact -> 180.dp
        layoutInfo.screenSize == ScreenSize.EXPANDED -> 220.dp
        else -> 200.dp
    }
}

/**
 * Get content-aware padding that scales with widget density
 */
@Composable
fun adaptiveContentPadding(
    compact: Dp = 8.dp,
    medium: Dp = 12.dp,
    expanded: Dp = 16.dp
): Dp {
    val isCompact = isCompactWidgetMode()
    val layoutInfo = rememberAdaptiveLayoutInfo()
    
    return when {
        isCompact -> compact
        layoutInfo.screenSize == ScreenSize.EXPANDED -> expanded
        else -> medium
    }
}

/**
 * Workout-specific adaptive layout utilities
 */

/**
 * Determine if workout should use master-detail layout
 */
@Composable
fun shouldUseWorkoutMasterDetail(): Boolean {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return layoutInfo.screenSize == ScreenSize.EXPANDED || 
           (layoutInfo.screenSize == ScreenSize.MEDIUM && layoutInfo.isLandscape)
}

/**
 * Get optimal master panel width for workout screens
 */
@Composable
fun workoutMasterPanelWidth(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val totalWidth = layoutInfo.screenWidth
    return when {
        totalWidth > 1000.dp -> 480.dp
        totalWidth > 800.dp -> 400.dp
        else -> totalWidth * 0.45f
    }
}

/**
 * Get workout touch target size optimized for gym usage
 */
@Composable
fun workoutTouchTargetSize(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> 44.dp
        ScreenSize.MEDIUM -> 48.dp
        ScreenSize.EXPANDED -> 56.dp
    }
}

/**
 * Get workout input field height for better gym usability
 */
@Composable
fun workoutInputHeight(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> 56.dp
        ScreenSize.MEDIUM -> 60.dp
        ScreenSize.EXPANDED -> 76.dp
    }
}

/**
 * Determine spacing between workout elements
 */
@Composable
fun workoutElementSpacing(): Dp {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    return when (layoutInfo.screenSize) {
        ScreenSize.COMPACT -> 12.dp
        ScreenSize.MEDIUM -> 16.dp
        ScreenSize.EXPANDED -> 20.dp
    }
}