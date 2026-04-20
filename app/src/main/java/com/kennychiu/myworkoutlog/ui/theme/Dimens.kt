package com.kennychiu.myworkoutlog.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, icon size, and elevation tokens.
 *
 * Numeric names track pixel values (`spacing16` = 16dp) so call sites stay self-documenting.
 * Prefer these over raw `.dp` literals in new code.
 */
object Dimens {
    // Spacing scale — matches the 4dp base grid used across Material 3.
    val spacing2 = 2.dp
    val spacing4 = 4.dp
    val spacing6 = 6.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing20 = 20.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing40 = 40.dp
    val spacing48 = 48.dp
    val spacing64 = 64.dp

    // Icon sizes.
    val iconXSmall = 12.dp
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconXLarge = 48.dp

    // Card / surface elevations.
    val elevationNone = 0.dp
    val elevationCard = 2.dp
    val elevationCardRaised = 4.dp
    val elevationModal = 8.dp

    // Minimum touch target (Material accessibility guideline).
    val touchTarget = 48.dp

    // Standard screen edge padding.
    val screenPadding = 16.dp

    // Badge padding (compact inset for Surface-wrapped labels like Deload / RIR chips).
    val badgePaddingHorizontal = 10.dp
    val badgePaddingVertical = 4.dp
}
