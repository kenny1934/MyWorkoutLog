@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HistorySetRow(
    set: LoggedSet,
    setNumber: Int,
    weightUnit: String,
    exercise: LoggedExercise,
    workout: LoggedWorkout
) {
    // Determine what data is available for this set
    val hasWeightReps = set.weight != null || set.reps != null
    val hasSecs = set.secs != null
    val hasRir = set.rir != null
    val hasBands = !set.bands.isNullOrBlank()
    val hasNotes = !set.notes.isNullOrBlank()
    val hasAdditionalData = hasRir || hasBands || hasNotes

    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        // Primary row - always visible
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set number
            Text(
                text = setNumber.toString(),
                modifier = Modifier.width(40.dp),
                fontWeight = FontWeight.Medium
            )

            // Weight
            Text(
                text = if (set.weight != null) formatHistoryWeightDisplay(set, exercise, workout) else "--",
                modifier = Modifier.weight(1f)
            )

            // Reps or Secs (based on what's available)
            if (hasWeightReps) {
                Text(
                    text = set.reps?.toString() ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasSecs) {
                Text(
                    text = "${set.secs ?: "--"}s",
                    modifier = Modifier.weight(1f)
                )
            }

            // Expand/collapse button for additional data
            if (hasAdditionalData) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Hide Details" else "Show Details",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }
        }

        // Expandable section for additional data
        AnimatedVisibility(visible = isExpanded && hasAdditionalData) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp)
            ) {
                // RIR and Bands row
                if (hasRir || hasBands) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (hasRir) {
                            Text(
                                text = "RIR: ${set.rir}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (hasBands) {
                            Text(
                                text = "Bands: ${set.bands}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Notes section
                if (hasNotes) {
                    Text(
                        text = "Notes: ${set.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (hasRir || hasBands) 4.dp else 0.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutCard(
    workout: LoggedWorkout,
    onNavigateToWorkout: (String) -> Unit,
    showCycleInfo: Boolean = true,
    isSelected: Boolean = false,
    onWorkoutSelected: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (onWorkoutSelected != null) {
                    onWorkoutSelected(workout.id)
                } else {
                    onNavigateToWorkout(workout.id)
                }
            },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workout.name ?: "Workout",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Show indicator if workout has detailed set data
                val hasDetailedData = workout.loggedExercises.any { exercise ->
                    exercise.sets.any { set ->
                        set.rir != null ||
                        !set.bands.isNullOrBlank() ||
                        !set.notes.isNullOrBlank()
                    }
                }

                if (hasDetailedData) {
                    Icon(
                        imageVector = Icons.Filled.DataUsage,
                        contentDescription = "Contains detailed set data",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(workout.date, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Show time details directly in the list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val duration = calculateDurationMinutes(workout.startTimestamp, workout.endTimestamp)
                Text(
                    text = "Duration: ${duration?.let { "$it min" } ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = workout.startTimestamp?.let { formatTimestampToTime(it) } ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun WorkoutSummaryCard(workout: LoggedWorkout) {
    val totalVolume = calculateWorkoutVolume(workout)
    val totalSets = countTotalSets(workout)
    val totalExercises = countExercises(workout)
    val avgSetVolume = calculateAverageSetVolume(workout)
    val weightUnit = workout.performedWeightUnit ?: "kg"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = "Workout Summary",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Workout Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Total Volume
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Volume",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Volume", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "${totalVolume.toInt()} $weightUnit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Total Sets
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Numbers,
                            contentDescription = "Sets",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sets", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = totalSets.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Total Exercises
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Exercises",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exercises", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = totalExercises.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (avgSetVolume > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Average per set: ${avgSetVolume.toInt()} $weightUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProgramContextCard(workout: LoggedWorkout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = "Program Context",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Program Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cycle Name
            if (!workout.userCycleName.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cycle:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        workout.userCycleName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Program Template Information (if we have it)
            workout.workoutTemplateId?.let { templateId ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Template:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        workout.name ?: "Workout Template",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Program Week/Session info (if available)
            if (!workout.programWeekDefinitionId.isNullOrBlank() || !workout.programSessionDefinitionId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Session:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Program Session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteWorkoutConfirmationDialog(
    isVisible: Boolean,
    workout: LoggedWorkout,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete Workout")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this workout?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Show workout details
                Text(
                    text = "• ${workout.name ?: "Unnamed Workout"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "• ${workout.date}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (workout.activeProgramCycleId != null) {
                    Text(
                        text = "• Part of active program cycle",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---- Shared helpers ----

fun formatTimestampToTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

// Helper function to format weight with appropriate decimal precision for history
fun formatHistoryWeight(weight: Double): String {
    return when {
        weight % 1.0 == 0.0 -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

private fun calculateDurationMinutes(start: Long?, end: Long?): Long? {
    if (start == null || end == null) return null
    val diff = end - start
    return TimeUnit.MILLISECONDS.toMinutes(diff)
}

// Helper function to format weight display for bodyweight exercises in history
private fun formatHistoryWeightDisplay(set: LoggedSet, exercise: LoggedExercise, workout: LoggedWorkout): String {
    val weightUnit = workout.performedWeightUnit ?: "kg"
    val setWeight = set.weight ?: 0.0

    // Check if this is a bodyweight exercise based on name (more reliable than equipment)
    val isBodyweightExercise = exercise.exerciseName.contains("pull up", ignoreCase = true) ||
         exercise.exerciseName.contains("pullup", ignoreCase = true) ||
         exercise.exerciseName.contains("chin up", ignoreCase = true) ||
         exercise.exerciseName.contains("chinup", ignoreCase = true) ||
         exercise.exerciseName.contains("dip", ignoreCase = true) ||
         exercise.exerciseName.contains("push up", ignoreCase = true) ||
         exercise.exerciseName.contains("pushup", ignoreCase = true)

    // Handle bodyweight exercises: set.weight contains external weight only, not total
    if (isBodyweightExercise && workout.bodyweight != null) {
        val bodyweight = workout.bodyweight
        val externalWeight = setWeight // For bodyweight exercises, set.weight IS the external weight

        return when {
            externalWeight > 0.1 -> {
                val totalWeight = bodyweight + externalWeight
                "BW(${formatHistoryWeight(bodyweight)}$weightUnit) + ${formatHistoryWeight(externalWeight)}$weightUnit = ${formatHistoryWeight(totalWeight)}$weightUnit"
            }
            else -> "BW(${formatHistoryWeight(bodyweight)}$weightUnit)"
        }
    }

    // For bodyweight exercises without bodyweight data, show external weight only
    if (isBodyweightExercise && setWeight > 0) {
        return "+${formatHistoryWeight(setWeight)}$weightUnit"
    }

    // Regular exercise: show total weight only
    return "${formatHistoryWeight(setWeight)}$weightUnit"
}

// Workout calculation utilities for summary statistics
private fun calculateWorkoutVolume(workout: LoggedWorkout): Double {
    return workout.loggedExercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            (set.weight ?: 0.0) * (set.reps ?: 0)
        }
    }
}

private fun countTotalSets(workout: LoggedWorkout): Int {
    return workout.loggedExercises.sumOf { it.sets.size }
}

private fun countExercises(workout: LoggedWorkout): Int {
    return workout.loggedExercises.size
}

private fun calculateAverageSetVolume(workout: LoggedWorkout): Double {
    val totalVolume = calculateWorkoutVolume(workout)
    val totalSets = countTotalSets(workout)
    return if (totalSets > 0) totalVolume / totalSets else 0.0
}
