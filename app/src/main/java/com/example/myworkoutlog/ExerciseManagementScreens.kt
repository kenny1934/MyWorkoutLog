@file:OptIn(ExperimentalLayoutApi::class)

package com.example.myworkoutlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ManageExercisesScreen(viewModel: ExerciseViewModel) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()

    // State for managing the dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    // Scaffold provides the structure for the FloatingActionButton
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new exercise")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Manage Exercises", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(exercises) { exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onEditClick = { exerciseToEdit = exercise },
                        onDeleteClick = { exerciseToDelete = exercise }
                    )
                }
            }
        }
    }

    // Show the Add dialog when showAddDialog is true
    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, equipment, usesBodyweight, muscleGroups ->
                viewModel.insert(name, equipment, usesBodyweight, muscleGroups)
                showAddDialog = false
            }
        )
    }

    // Show the Edit dialog when exerciseToEdit is not null
    if (exerciseToEdit != null) {
        EditExerciseDialog(
            exercise = exerciseToEdit!!,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { updatedExercise ->
                viewModel.update(updatedExercise)
                exerciseToEdit = null
            }
        )
    }

    // Show the Delete confirmation dialog when exerciseToDelete is not null
    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Delete Exercise") },
            text = { Text("Are you sure you want to delete '${exerciseToDelete!!.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(exerciseToDelete!!)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, List<MuscleGroup>) -> Unit
) {
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseEquipment by remember { mutableStateOf("") }
    var newExerciseUsesBodyweight by remember { mutableStateOf(false) }
    var selectedMuscleGroups by remember { mutableStateOf<List<MuscleGroup>>(emptyList()) }
    var showMuscleGroupDialog by remember { mutableStateOf(false) }

    if (showMuscleGroupDialog) {
        MuscleGroupSelectionDialog(
            allMuscleGroups = MuscleGroup.entries.toList(),
            initialSelection = selectedMuscleGroups,
            onDismiss = { showMuscleGroupDialog = false },
            onConfirm = {
                selectedMuscleGroups = it
                showMuscleGroupDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newExerciseName,
                    onValueChange = { newExerciseName = it },
                    label = { Text("Exercise Name") }
                )
                OutlinedTextField(
                    value = newExerciseEquipment,
                    onValueChange = { newExerciseEquipment = it },
                    label = { Text("Equipment (e.g., DUMBBELL)") }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uses Bodyweight")
                    Switch(checked = newExerciseUsesBodyweight, onCheckedChange = { newExerciseUsesBodyweight = it })
                }
                Text("Target Muscles: ${selectedMuscleGroups.joinToString { it.name }}")
                Button(onClick = { showMuscleGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Muscle Groups")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newExerciseName.isNotBlank()) {
                    onConfirm(newExerciseName, newExerciseEquipment, newExerciseUsesBodyweight, selectedMuscleGroups)
                }
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditExerciseDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit
) {
    var editedName by remember { mutableStateOf(exercise.name) }
    var editedEquipment by remember { mutableStateOf(exercise.equipment.joinToString()) }
    var editedUsesBodyweight by remember { mutableStateOf(exercise.usesBodyweight) }
    var editedMuscleGroups by remember { mutableStateOf(exercise.targetMuscleGroups) }
    var showMuscleGroupDialog by remember { mutableStateOf(false) }

    if (showMuscleGroupDialog) {
        MuscleGroupSelectionDialog(
            allMuscleGroups = MuscleGroup.entries.toList(),
            initialSelection = editedMuscleGroups,
            onDismiss = { showMuscleGroupDialog = false },
            onConfirm = {
                editedMuscleGroups = it
                showMuscleGroupDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Exercise Name") }
                )
                OutlinedTextField(
                    value = editedEquipment,
                    onValueChange = { editedEquipment = it },
                    label = { Text("Equipment (e.g., DUMBBELL)") }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uses Bodyweight")
                    Switch(checked = editedUsesBodyweight, onCheckedChange = { editedUsesBodyweight = it })
                }
                Text("Target Muscles: ${editedMuscleGroups.joinToString { it.name }}")
                Button(onClick = { showMuscleGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Muscle Groups")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedExercise = exercise.copy(
                    name = editedName,
                    equipment = try { listOf(Equipment.valueOf(editedEquipment.uppercase().trim())) } catch (e: Exception) { exercise.equipment },
                    usesBodyweight = editedUsesBodyweight,
                    targetMuscleGroups = editedMuscleGroups
                )
                onConfirm(updatedExercise)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MuscleGroupSelectionDialog(
    allMuscleGroups: List<MuscleGroup>,
    initialSelection: List<MuscleGroup>,
    onDismiss: () -> Unit,
    onConfirm: (List<MuscleGroup>) -> Unit
) {
    var selectedMuscleGroups by remember { mutableStateOf(initialSelection.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Muscle Groups") },
        text = {
            LazyColumn {
                items(allMuscleGroups) { muscleGroup ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSelection = selectedMuscleGroups.toMutableSet()
                                if (selectedMuscleGroups.contains(muscleGroup)) {
                                    newSelection.remove(muscleGroup)
                                } else {
                                    newSelection.add(muscleGroup)
                                }
                                selectedMuscleGroups = newSelection
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedMuscleGroups.contains(muscleGroup),
                            onCheckedChange = { isChecked ->
                                val newSelection = selectedMuscleGroups.toMutableSet()
                                if (isChecked) {
                                    newSelection.add(muscleGroup)
                                } else {
                                    newSelection.remove(muscleGroup)
                                }
                                selectedMuscleGroups = newSelection
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(muscleGroup.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.titlecase() })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMuscleGroups.toList()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exercise Name
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Action Buttons
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Exercise")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Exercise")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Display Muscle Groups as wrapping tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                exercise.targetMuscleGroups.forEach { group ->
                    Text(
                        text = group.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}