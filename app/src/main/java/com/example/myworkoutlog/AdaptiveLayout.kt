package com.example.myworkoutlog

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
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