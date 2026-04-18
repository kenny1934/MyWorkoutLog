package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.LoggedExercise
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun summarizeLastPerformance(
    workout: LoggedWorkout,
    exercise: LoggedExercise,
    today: Date = Date()
): String? {
    val weightSets = exercise.sets.filter { it.reps != null && it.reps > 0 && it.weight != null }
    val secsSets = exercise.sets.filter { it.secs != null && it.secs > 0 }

    val core: String = when {
        weightSets.isNotEmpty() -> {
            val rep = weightSets.maxByOrNull { it.weight ?: 0.0 } ?: return null
            val unit = workout.performedWeightUnit ?: "kg"
            val weightStr = formatWeight(rep.weight ?: return null)
            "${weightSets.size} × ${rep.reps} @ $weightStr$unit"
        }
        secsSets.isNotEmpty() -> {
            val rep = secsSets.maxByOrNull { it.secs ?: 0 } ?: return null
            "${secsSets.size} × ${rep.secs}s"
        }
        else -> return null
    }

    return "$core${daysAgoSuffix(workout.date, today)}"
}

private fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) w.toLong().toString() else w.toString()

private fun daysAgoSuffix(dateString: String, today: Date): String {
    val days = daysBetween(dateString, today) ?: return ""
    val label = when {
        days <= 0 -> "today"
        days == 1 -> "yesterday"
        else -> "${days}d ago"
    }
    return " ($label)"
}

private fun daysBetween(dateString: String, today: Date): Int? {
    val parsed = try {
        DATE_FORMAT.parse(dateString) ?: return null
    } catch (e: Exception) {
        return null
    }
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val startOfDay = { d: Date ->
        cal.time = d
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val diff = startOfDay(today) - startOfDay(parsed)
    return (diff / (1000L * 60 * 60 * 24)).toInt()
}
