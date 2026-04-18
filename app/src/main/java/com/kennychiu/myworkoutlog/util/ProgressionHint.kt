package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme
import com.kennychiu.myworkoutlog.data.TemplateExercise

// Short single-line hint derived from an exercise's progression scheme and its params.
// Returns null when there's nothing useful to show (no scheme configured or NONE).
// Rendered under the exercise name in both the compact logger card and the master-detail
// row; format decisions live here so both surfaces stay in sync.
fun formatProgressionHint(
    scheme: ProgressionScheme?,
    increment: Double? = null,
    minReps: Int? = null,
    maxReps: Int? = null,
    targetRpe: String? = null,
    weightUnit: String = "kg",
): String? {
    if (scheme == null || scheme == ProgressionScheme.NONE) return null
    return when (scheme) {
        ProgressionScheme.LINEAR -> {
            val inc = increment
            if (inc != null && inc > 0.0) "Linear +${formatIncrement(inc)}$weightUnit/wk" else "Linear"
        }
        ProgressionScheme.DOUBLE -> {
            val min = minReps
            val max = maxReps
            when {
                min != null && max != null -> "Double $min–$max reps"
                min != null -> "Double ≥$min reps"
                max != null -> "Double ≤$max reps"
                else -> "Double progression"
            }
        }
        ProgressionScheme.RPE -> {
            val rpe = targetRpe?.takeIf { it.isNotBlank() }
            if (rpe != null) "RPE $rpe" else "RPE"
        }
        ProgressionScheme.TOP_SET -> "Top set + backoffs"
        ProgressionScheme.NONE -> null
    }
}

// Convenience overload that reads fields directly off a TemplateExercise.
fun formatProgressionHint(exercise: TemplateExercise, weightUnit: String = "kg"): String? {
    return formatProgressionHint(
        scheme = exercise.progressionScheme,
        increment = exercise.progressionIncrement,
        minReps = exercise.progressionMinReps,
        maxReps = exercise.progressionMaxReps,
        targetRpe = exercise.progressionTargetRpe,
        weightUnit = weightUnit,
    )
}

// Integer-valued increments render without a trailing ".0" (so +2.5kg and +5kg both
// look natural). Mirrors the same rounding choice in LastPerformance.kt.
private fun formatIncrement(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
