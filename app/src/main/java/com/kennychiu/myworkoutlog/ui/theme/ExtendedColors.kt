package com.kennychiu.myworkoutlog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that extend `MaterialTheme.colorScheme` — success / warning / info, and a
 * consistent chart palette. Held behind a CompositionLocal so dark vs light swaps automatically.
 *
 * Access via `MaterialTheme.extendedColors.success` inside any composable.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val chart: List<Color>
)

// Light variants — tuned to read legibly on the near-white LtBackground (#F9FAFB).
val LightExtendedColors = ExtendedColors(
    success = Color(0xFF15803D),
    onSuccess = Color.White,
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF052E16),
    warning = Color(0xFFB45309),
    onWarning = Color.White,
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF451A03),
    info = Color(0xFF1D4ED8),
    onInfo = Color.White,
    infoContainer = Color(0xFFDBEAFE),
    onInfoContainer = Color(0xFF172554),
    accent = Color(0xFFEA580C),
    onAccent = Color.White,
    chart = listOf(
        Color(0xFF0D9488), // teal — primary
        Color(0xFF7C3AED), // purple — secondary
        Color(0xFFEA580C), // orange — accent
        Color(0xFF15803D), // green — success
        Color(0xFF1D4ED8), // blue — info
        Color(0xFFBE185D), // pink
        Color(0xFFB45309), // amber
        Color(0xFF475569)  // slate
    )
)

// Dark variants — tuned against DkBackground (#111827).
val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF22C55E),
    onSuccess = Color(0xFF052E16),
    successContainer = Color(0xFF14532D),
    onSuccessContainer = Color(0xFFBBF7D0),
    warning = Color(0xFFF59E0B),
    onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFEF3C7),
    info = Color(0xFF60A5FA),
    onInfo = Color(0xFF172554),
    infoContainer = Color(0xFF1E3A8A),
    onInfoContainer = Color(0xFFDBEAFE),
    accent = Color(0xFFFB923C),
    onAccent = Color(0xFF431407),
    chart = listOf(
        Color(0xFF14B8A6), // teal — primary
        Color(0xFF8B5CF6), // purple — secondary
        Color(0xFFFB923C), // orange — accent
        Color(0xFF22C55E), // green — success
        Color(0xFF60A5FA), // blue — info
        Color(0xFFEC4899), // pink
        Color(0xFFF59E0B), // amber
        Color(0xFF94A3B8)  // slate
    )
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * Access extended semantic colors from any composable:
 * `MaterialTheme.extendedColors.success`.
 */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
