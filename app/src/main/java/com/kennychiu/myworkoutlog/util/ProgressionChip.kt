package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme

// Scheme-aware smart-pre-fill chip output. Weight / reps / rir are the values the
// chip applies to the set on tap; label is the text rendered on the chip. Label is
// intentionally self-contained so the UI doesn't have to know scheme specifics.
data class ChipSuggestion(
    val weight: Double? = null,
    val reps: Int? = null,
    val rir: Int? = null,
    val label: String,
)

// Pick a pre-fill suggestion based on the exercise's progression scheme and the
// representative set from the last session. Returns null when the scheme is unset
// or NONE (caller falls back to the legacy "maintain last" suggestion) and when
// there's nothing useful to seed from.
fun suggestForScheme(
    scheme: ProgressionScheme?,
    setNumber: Int = 1,
    lastWeight: Double? = null,
    lastReps: Int? = null,
    lastRir: Int? = null,
    increment: Double? = null,
    minReps: Int? = null,
    maxReps: Int? = null,
    targetRpe: String? = null,
    weightUnit: String = "kg",
    // 1-based cycle week, or null when the workout has no cycle context. Week 1 of a
    // fresh cycle re-introduces the exercise at its previous working weight rather than
    // stacking last-cycle's final bump — so the schemes that otherwise add weight each
    // session skip the bump on week 1.
    cycleWeekNumber: Int? = null,
): ChipSuggestion? {
    if (scheme == null || scheme == ProgressionScheme.NONE) return null
    if (lastWeight == null && lastReps == null) return null
    val isBaselineWeek = cycleWeekNumber == 1
    return when (scheme) {
        ProgressionScheme.LINEAR -> linearChip(lastWeight, lastReps, lastRir, increment, weightUnit, isBaselineWeek)
        ProgressionScheme.DOUBLE -> doubleChip(lastWeight, lastReps, lastRir, increment, minReps, maxReps, weightUnit, isBaselineWeek)
        ProgressionScheme.RPE -> rpeChip(lastWeight, lastReps, lastRir, targetRpe, weightUnit)
        ProgressionScheme.TOP_SET -> topSetChip(setNumber, lastWeight, lastReps, lastRir, increment, weightUnit, isBaselineWeek)
        ProgressionScheme.NONE -> null
    }
}

private fun linearChip(
    lastWeight: Double?,
    lastReps: Int?,
    lastRir: Int?,
    increment: Double?,
    weightUnit: String,
    isBaselineWeek: Boolean,
): ChipSuggestion {
    val inc = increment?.takeIf { it > 0.0 && !isBaselineWeek }
    val newWeight = if (lastWeight != null && inc != null) lastWeight + inc else lastWeight
    val suffix = if (isBaselineWeek) "baseline" else "next"
    return ChipSuggestion(
        weight = newWeight,
        reps = lastReps,
        rir = lastRir,
        label = chipLabel(newWeight, lastReps, weightUnit, suffix),
    )
}

private fun doubleChip(
    lastWeight: Double?,
    lastReps: Int?,
    lastRir: Int?,
    increment: Double?,
    minReps: Int?,
    maxReps: Int?,
    weightUnit: String,
    isBaselineWeek: Boolean,
): ChipSuggestion? {
    if (lastReps == null) return null
    val climbing = maxReps == null || lastReps < maxReps
    if (climbing) {
        val newReps = lastReps + 1
        return ChipSuggestion(
            weight = lastWeight,
            reps = newReps,
            rir = lastRir,
            label = chipLabel(lastWeight, newReps, weightUnit, "next"),
        )
    }
    // At or above max: bump weight, reset reps to the min. Skip the bump on week 1 of
    // the cycle — baseline re-introduction — and just hold at max reps / last weight.
    if (isBaselineWeek) {
        return ChipSuggestion(
            weight = lastWeight,
            reps = lastReps,
            rir = lastRir,
            label = chipLabel(lastWeight, lastReps, weightUnit, "baseline"),
        )
    }
    // Default increment is 2.5 (a common small-plate bump) when the user hasn't set one.
    val inc = increment?.takeIf { it > 0.0 } ?: 2.5
    val newWeight = lastWeight?.let { it + inc }
    val newReps = minReps ?: lastReps
    return ChipSuggestion(
        weight = newWeight,
        reps = newReps,
        rir = lastRir,
        label = chipLabel(newWeight, newReps, weightUnit, "next"),
    )
}

private fun rpeChip(
    lastWeight: Double?,
    lastReps: Int?,
    lastRir: Int?,
    targetRpe: String?,
    weightUnit: String,
): ChipSuggestion {
    val rpe = targetRpe?.takeIf { it.isNotBlank() }
    val rirFromRpe = rpe?.let { rirFromRpeString(it) }
    val base = chipBase(lastWeight, lastReps, weightUnit)
    val label = when {
        rpe == null -> chipLabel(lastWeight, lastReps, weightUnit, null)
        base.isEmpty() -> "@ RPE $rpe"
        else -> "$base @ RPE $rpe"
    }
    return ChipSuggestion(
        weight = lastWeight,
        reps = lastReps,
        rir = rirFromRpe ?: lastRir,
        label = label,
    )
}

private fun topSetChip(
    setNumber: Int,
    lastWeight: Double?,
    lastReps: Int?,
    lastRir: Int?,
    increment: Double?,
    weightUnit: String,
    isBaselineWeek: Boolean,
): ChipSuggestion {
    // Set 1 is the top set. Apply the increment bump so the user pushes harder than
    // last session — unless this is the cycle's baseline week, where we re-introduce
    // at the last working weight instead of stacking a bump on top of last cycle's peak.
    // Subsequent sets are backoffs and just copy the last performance.
    if (setNumber <= 1) {
        if (isBaselineWeek) {
            return ChipSuggestion(
                weight = lastWeight,
                reps = lastReps,
                rir = lastRir,
                label = chipLabel(lastWeight, lastReps, weightUnit, "top · baseline"),
            )
        }
        val inc = increment?.takeIf { it > 0.0 } ?: 2.5
        val newWeight = lastWeight?.let { it + inc }
        return ChipSuggestion(
            weight = newWeight,
            reps = lastReps,
            rir = lastRir,
            label = chipLabel(newWeight, lastReps, weightUnit, "top"),
        )
    }
    return ChipSuggestion(
        weight = lastWeight,
        reps = lastReps,
        rir = lastRir,
        label = chipLabel(lastWeight, lastReps, weightUnit, "backoff"),
    )
}

private fun chipLabel(weight: Double?, reps: Int?, weightUnit: String, suffix: String?): String {
    val base = chipBase(weight, reps, weightUnit)
    if (suffix == null) return base
    return if (base.isEmpty()) "($suffix)" else "$base ($suffix)"
}

private fun chipBase(weight: Double?, reps: Int?, weightUnit: String): String {
    val parts = mutableListOf<String>()
    weight?.let { parts.add("${formatWeight(it)}$weightUnit") }
    reps?.let { parts.add("${it}r") }
    return parts.joinToString(" ")
}

// Integer-valued weights render without the trailing ".0". Mirrors the rounding
// choice in LastPerformance.kt and ProgressionHint.kt so all three surfaces agree.
private fun formatWeight(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

// RIR is the flip side of RPE: RIR = 10 - RPE. "8" → 2. Range inputs like "7-8"
// use the lower bound (more reps-in-reserve, safer prefill).
private fun rirFromRpeString(rpe: String): Int? {
    val numbers = Regex("\\d+").findAll(rpe)
        .map { it.value.toIntOrNull() }
        .filterNotNull()
        .toList()
    val rpeValue = numbers.minOrNull() ?: return null
    return (10 - rpeValue).coerceIn(0, 10)
}
