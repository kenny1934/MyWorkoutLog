@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.TimeUnit

@Composable
fun HistoryDetailScreen(
    workoutId: String,
    viewModel: HistoryViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val workout by viewModel.getLoggedWorkoutById(workoutId).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(workoutId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Workout")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Workout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        val currentWorkout = workout
        if (currentWorkout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Display the date prominently here
                    Text(currentWorkout.date, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Program Context Card
                    if (!currentWorkout.userCycleName.isNullOrBlank() || !currentWorkout.activeProgramCycleId.isNullOrBlank()) {
                        ProgramContextCard(workout = currentWorkout)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Start Time
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Start Time", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = currentWorkout.startTimestamp?.let { formatTimestampToTime(it) } ?: "--:--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // End Time
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("End Time", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = currentWorkout.endTimestamp?.let { formatTimestampToTime(it) } ?: "--:--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Duration
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duration", style = MaterialTheme.typography.labelMedium)
                            val durationMinutes = if (currentWorkout.startTimestamp != null && currentWorkout.endTimestamp != null) {
                                val diff = currentWorkout.endTimestamp - currentWorkout.startTimestamp
                                TimeUnit.MILLISECONDS.toMinutes(diff)
                            } else null
                            Text(
                                text = durationMinutes?.let { "$it min" } ?: "- min",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Bodyweight
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Bodyweight", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = currentWorkout.bodyweight?.let {
                                    "${formatHistoryWeight(it)} ${currentWorkout.performedWeightUnit ?: "kg"}"
                                } ?: "- ${currentWorkout.performedWeightUnit ?: "kg"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 20.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Overall Comments Section
                if (!currentWorkout.overallComments.isNullOrBlank()) {
                    item {
                        Text("Notes:", style = MaterialTheme.typography.titleMedium)
                        Text(currentWorkout.overallComments, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Workout Summary Statistics Card
                item {
                    WorkoutSummaryCard(workout = currentWorkout)
                }

                items(currentWorkout.loggedExercises, key = { it.id }) { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    exercise.exerciseName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                // Add bodyweight indicator icon for bodyweight exercises
                                val isBodyweightExercise = exercise.exerciseName.contains("pull up", ignoreCase = true) ||
                                    exercise.exerciseName.contains("pullup", ignoreCase = true) ||
                                    exercise.exerciseName.contains("chin up", ignoreCase = true) ||
                                    exercise.exerciseName.contains("chinup", ignoreCase = true) ||
                                    exercise.exerciseName.contains("dip", ignoreCase = true) ||
                                    exercise.exerciseName.contains("push up", ignoreCase = true) ||
                                    exercise.exerciseName.contains("pushup", ignoreCase = true)

                                if (isBodyweightExercise) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Bodyweight Exercise",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            // Dynamic header based on what data is available
                            val hasReps = exercise.sets.any { it.reps != null }
                            val hasSecs = exercise.sets.any { it.secs != null }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Set",
                                    modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Weight (${workout!!.performedWeightUnit ?: "kg"})",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                if (hasReps) {
                                    Text(
                                        "Reps",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (hasSecs) {
                                    Text(
                                        "Duration",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.width(32.dp)) // Space for expand button
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // Display each logged set using the new component
                            exercise.sets.forEachIndexed { index, set ->
                                HistorySetRow(
                                    set = set,
                                    setNumber = index + 1,
                                    weightUnit = workout!!.performedWeightUnit ?: "kg",
                                    exercise = exercise,
                                    workout = workout!!
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    val currentWorkout = workout
    if (currentWorkout != null) {
        DeleteWorkoutConfirmationDialog(
            isVisible = showDeleteDialog,
            workout = currentWorkout,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                viewModel.deleteWorkout(workoutId)
                showDeleteDialog = false
                onNavigateUp() // Navigate back after deletion
            }
        )
    }
}
