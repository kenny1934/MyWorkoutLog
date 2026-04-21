package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme
import com.kennychiu.myworkoutlog.data.TemplateExercise

// One exercise's representative top set for a single moment (baseline snapshot before
// the cycle, or the top set that was actually logged during a given cycle week).
data class ExerciseTopSet(
    val weight: Double?,
    val reps: Int?,
)

// Per-week projection for one exercise across a cycle. `weight` and `reps` may be
// null when the scheme doesn't prescribe numeric targets (RPE, NONE) or when the
// anchor has no data yet. `isActual` is true when the row reflects a logged week
// rather than a forward projection.
data class ProjectedWeekSet(
    val weekNumber: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val label: String,
    val isActual: Boolean = false,
)

// Project an exercise's top-set target across every week of a cycle.
//
// Adaptive: for each week W, we anchor on the most recent logged actual in
// actualsByWeek at a week W' < W, or fall back to `baseline` (the last session
// before the cycle) when no earlier actuals exist. When actualsByWeek[W] is
// present, the row emits that actual directly (isActual = true) instead of a
// projection — so a missed or under-planned week re-baselines downstream.
//
// RPE is target-only by Kenny's decision: we surface "Target RPE {x}" with no
// weight number, even if we have a baseline. NONE renders "Freeform".
fun projectExerciseAcrossWeeks(
    exercise: TemplateExercise,
    cycleWeekCount: Int,
    baseline: ExerciseTopSet? = null,
    actualsByWeek: Map<Int, ExerciseTopSet?> = emptyMap(),
    weightUnit: String = "kg",
): List<ProjectedWeekSet> = projectExerciseAcrossWeeks(
    scheme = exercise.progressionScheme,
    increment = exercise.progressionIncrement,
    minReps = exercise.progressionMinReps,
    maxReps = exercise.progressionMaxReps,
    targetRpe = exercise.progressionTargetRpe,
    cycleWeekCount = cycleWeekCount,
    baseline = baseline,
    actualsByWeek = actualsByWeek,
    weightUnit = weightUnit,
)

fun projectExerciseAcrossWeeks(
    scheme: ProgressionScheme?,
    increment: Double? = null,
    minReps: Int? = null,
    maxReps: Int? = null,
    targetRpe: String? = null,
    cycleWeekCount: Int,
    baseline: ExerciseTopSet? = null,
    actualsByWeek: Map<Int, ExerciseTopSet?> = emptyMap(),
    weightUnit: String = "kg",
): List<ProjectedWeekSet> {
    if (cycleWeekCount <= 0) return emptyList()
    return (1..cycleWeekCount).map { week ->
        projectWeek(
            week = week,
            scheme = scheme,
            increment = increment,
            minReps = minReps,
            maxReps = maxReps,
            targetRpe = targetRpe,
            baseline = baseline,
            actualsByWeek = actualsByWeek,
            weightUnit = weightUnit,
        )
    }
}

private fun projectWeek(
    week: Int,
    scheme: ProgressionScheme?,
    increment: Double?,
    minReps: Int?,
    maxReps: Int?,
    targetRpe: String?,
    baseline: ExerciseTopSet?,
    actualsByWeek: Map<Int, ExerciseTopSet?>,
    weightUnit: String,
): ProjectedWeekSet {
    val actual = actualsByWeek[week]
    if (actual != null) {
        return ProjectedWeekSet(
            weekNumber = week,
            weight = actual.weight,
            reps = actual.reps,
            label = formatWeightReps(actual.weight, actual.reps, weightUnit),
            isActual = true,
        )
    }

    when (scheme) {
        null, ProgressionScheme.NONE -> return ProjectedWeekSet(week, label = "Freeform")
        ProgressionScheme.RPE -> {
            val rpe = targetRpe?.takeIf { it.isNotBlank() }
            val label = if (rpe != null) "Target RPE $rpe" else "RPE"
            return ProjectedWeekSet(week, label = label)
        }
        else -> Unit
    }

    // Find the anchor: most recent actual at a week < `week`, else baseline at week 1.
    val anchorFromActuals = (week - 1 downTo 1).firstNotNullOfOrNull { w ->
        actualsByWeek[w]?.let { w to it }
    }
    val (anchorWeek, anchor) = anchorFromActuals
        ?: (baseline?.let { 1 to it } ?: return ProjectedWeekSet(week, label = "—"))
    val offset = week - anchorWeek

    return when (scheme) {
        ProgressionScheme.LINEAR -> linearProjection(week, anchor, offset, increment, weightUnit)
        ProgressionScheme.TOP_SET -> topSetProjection(week, anchor, offset, increment, weightUnit)
        ProgressionScheme.DOUBLE -> doubleProjection(week, anchor, offset, increment, minReps, maxReps, weightUnit)
        else -> ProjectedWeekSet(week, label = "—")
    }
}

private fun linearProjection(
    week: Int,
    anchor: ExerciseTopSet,
    offset: Int,
    increment: Double?,
    weightUnit: String,
): ProjectedWeekSet {
    val inc = increment?.takeIf { it > 0.0 } ?: 0.0
    val weight = anchor.weight?.let { it + inc * offset }
    return ProjectedWeekSet(week, weight, anchor.reps, formatWeightReps(weight, anchor.reps, weightUnit))
}

private fun topSetProjection(
    week: Int,
    anchor: ExerciseTopSet,
    offset: Int,
    increment: Double?,
    weightUnit: String,
): ProjectedWeekSet {
    // Default small-plate bump when user hasn't set an increment — mirrors
    // topSetChip's ?: 2.5 choice in ProgressionChip.kt.
    val inc = increment?.takeIf { it > 0.0 } ?: 2.5
    val weight = anchor.weight?.let { it + inc * offset }
    return ProjectedWeekSet(week, weight, anchor.reps, formatWeightReps(weight, anchor.reps, weightUnit))
}

private fun doubleProjection(
    week: Int,
    anchor: ExerciseTopSet,
    offset: Int,
    increment: Double?,
    minReps: Int?,
    maxReps: Int?,
    weightUnit: String,
): ProjectedWeekSet {
    val inc = increment?.takeIf { it > 0.0 } ?: 2.5
    var w: Double? = anchor.weight
    var r: Int? = anchor.reps
    repeat(offset) {
        val currReps = r
        if (currReps != null) {
            if (maxReps != null && currReps >= maxReps) {
                w = w?.let { it + inc }
                r = minReps ?: currReps
            } else {
                r = currReps + 1
            }
        }
    }
    return ProjectedWeekSet(week, w, r, formatWeightReps(w, r, weightUnit))
}

private fun formatWeightReps(weight: Double?, reps: Int?, weightUnit: String): String {
    val w = weight?.let { formatWeight(it) }
    val r = reps?.toString()
    return when {
        w != null && r != null -> "$w$weightUnit × $r"
        w != null -> "$w$weightUnit"
        r != null -> "$r reps"
        else -> "—"
    }
}

private fun formatWeight(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
