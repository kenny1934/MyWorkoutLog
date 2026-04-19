@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddExerciseToWorkoutDialog(
    viewModel: WorkoutLoggerViewModel,
    onDismiss: () -> Unit,
    onExerciseAdded: () -> Unit
) {
    // State for exercise selection
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var numberOfSets by remember { mutableStateOf("3") }
    var showExerciseSelector by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (showExerciseSelector) "Select Exercise" else "Configure Sets")
        },
        text = {
            if (showExerciseSelector) {
                ExerciseSelectorContent(
                    viewModel = viewModel,
                    onExerciseSelected = { exercise: Exercise ->
                        selectedExercise = exercise
                        showExerciseSelector = false
                    }
                )
            } else {
                Column {
                    Text("Exercise: ${selectedExercise?.name}")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = numberOfSets,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.isNotEmpty()) {
                                numberOfSets = newValue
                            }
                        },
                        label = { Text("Number of Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showExerciseSelector) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = {
                        selectedExercise?.let { exercise ->
                            val sets = numberOfSets.toIntOrNull() ?: 3
                            viewModel.addExerciseToWorkout(exercise.id, sets)
                            onExerciseAdded()
                        }
                    },
                    enabled = selectedExercise != null
                ) {
                    Text("Add Exercise")
                }
            }
        },
        dismissButton = {
            if (!showExerciseSelector) {
                TextButton(
                    onClick = {
                        showExerciseSelector = true
                        selectedExercise = null
                    }
                ) {
                    Text("Back")
                }
            }
        }
    )
}

@Composable
fun ExerciseSelectorContent(
    viewModel: WorkoutLoggerViewModel,
    onExerciseSelected: (Exercise) -> Unit
) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search exercises...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val filteredExercises = remember(exercises, searchText) {
            exercises.filter { exercise ->
                exercise.name.contains(searchText, ignoreCase = true)
            }
        }

        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredExercises, key = { it.id }) { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exercise.targetMuscleGroups.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseContextMenuDialog(
    exercise: LoggedExercise,
    onDismiss: () -> Unit,
    onSubstitute: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise Options") },
        text = {
            Column {
                Text("Choose an action for \"${exercise.exerciseName}\"")
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSubstitute,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Substitute Exercise")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Exercise")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        dismissButton = null
    )
}

@Composable
fun SubstituteExerciseDialog(
    viewModel: WorkoutLoggerViewModel,
    currentExercise: LoggedExercise,
    onDismiss: () -> Unit,
    onExerciseSubstituted: () -> Unit
) {
    // State for exercise selection
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExerciseSelector by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (showExerciseSelector) "Select Substitute" else "Confirm Substitution")
        },
        text = {
            if (showExerciseSelector) {
                Column {
                    Text("Substituting: ${currentExercise.exerciseName}")
                    Spacer(modifier = Modifier.height(16.dp))
                    ExerciseSelectorContent(
                        viewModel = viewModel,
                        onExerciseSelected = { exercise: Exercise ->
                            selectedExercise = exercise
                            showExerciseSelector = false
                        }
                    )
                }
            } else {
                Column {
                    Text("Current: ${currentExercise.exerciseName}")
                    Text("Substitute with: ${selectedExercise?.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will replace the exercise while keeping all current set data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (showExerciseSelector) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = {
                        selectedExercise?.let { newExercise ->
                            viewModel.substituteExercise(currentExercise.id, newExercise.id)
                            onExerciseSubstituted()
                        }
                    },
                    enabled = selectedExercise != null
                ) {
                    Text("Substitute")
                }
            }
        },
        dismissButton = {
            if (!showExerciseSelector) {
                TextButton(
                    onClick = {
                        showExerciseSelector = true
                        selectedExercise = null
                    }
                ) {
                    Text("Back")
                }
            }
        }
    )
}

@Composable
fun DurationEditDialog(
    isVisible: Boolean,
    currentDurationSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (!isVisible) return

    var durationInput by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(true) }

    LaunchedEffect(currentDurationSeconds) {
        durationInput = formatSecondsToDisplay(currentDurationSeconds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Workout Duration")
        },
        text = {
            Column {
                Text(
                    text = "Original: ${formatSecondsToDisplay(currentDurationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Enter duration to correct faulty time records:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { input ->
                        durationInput = input
                        isValid = validateDurationInput(input)
                    },
                    label = { Text("Duration") },
                    placeholder = { Text("45:30 or 1:15:30") },
                    supportingText = {
                        if (!isValid && durationInput.isNotEmpty()) {
                            Text(
                                "Try: 45:30, 1:15:30, 45m 30s, or 2730s",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (isValid && durationInput.isNotEmpty()) {
                            val parsedSeconds = parseDurationToSeconds(durationInput)
                            if (parsedSeconds != null) {
                                Text(
                                    "= ${parsedSeconds}s (${formatSecondsToDisplay(parsedSeconds)})",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Text("Formats: MM:SS, H:MM:SS, 45m 30s, 2730s")
                        }
                    },
                    isError = !isValid && durationInput.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedSeconds = parseDurationToSeconds(durationInput)
                    if (parsedSeconds != null) {
                        onConfirm(parsedSeconds)
                    }
                },
                enabled = isValid && durationInput.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
