@file:OptIn(ExperimentalLayoutApi::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ManageExercisesScreen(
    viewModel: ExerciseViewModel,
    onNavigateUp: (() -> Unit)? = null
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()

    if (layoutInfo.useMasterDetail) {
        // Large screen: Master-detail layout
        ExerciseManagementMasterDetailView(
            viewModel = viewModel,
            layoutInfo = layoutInfo,
            onNavigateUp = onNavigateUp
        )
    } else {
        // Small screen: Original single-column layout
        ExerciseManagementSingleColumnView(
            viewModel = viewModel,
            onNavigateUp = onNavigateUp
        )
    }
}

@Composable
private fun ExerciseManagementSingleColumnView(
    viewModel: ExerciseViewModel,
    onNavigateUp: (() -> Unit)?
) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()

    // State for managing the dialogs
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    ScreenScaffold(
        title = "Manage Exercises",
        onNavigateUp = onNavigateUp,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new exercise")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(Dimens.screenPadding)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                items(exercises, key = { it.id }) { exercise ->
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
private fun ExerciseManagementMasterDetailView(
    viewModel: ExerciseViewModel,
    layoutInfo: AdaptiveLayoutInfo,
    onNavigateUp: (() -> Unit)?
) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroupFilter by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedEquipmentFilter by remember { mutableStateOf<Equipment?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Filter exercises based on search and filters
    val filteredExercises = remember(exercises, searchQuery, selectedMuscleGroupFilter, selectedEquipmentFilter) {
        exercises.filter { exercise ->
            val matchesSearch = searchQuery.isBlank() ||
                exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesMuscleGroup = selectedMuscleGroupFilter == null ||
                exercise.targetMuscleGroups.contains(selectedMuscleGroupFilter)
            val matchesEquipment = selectedEquipmentFilter == null ||
                exercise.equipment.contains(selectedEquipmentFilter)

            matchesSearch && matchesMuscleGroup && matchesEquipment
        }
    }

    // Auto-select first exercise when filtered list changes or when selected exercise is no longer available
    LaunchedEffect(filteredExercises) {
        when {
            selectedExercise == null && filteredExercises.isNotEmpty() -> {
                selectedExercise = filteredExercises.first()
            }
            selectedExercise != null && !filteredExercises.contains(selectedExercise) -> {
                // Selected exercise no longer matches filters, select first available or clear selection
                selectedExercise = filteredExercises.firstOrNull()
            }
        }
    }

    ScreenScaffold(
        title = "Manage Exercises",
        onNavigateUp = onNavigateUp,
        actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Exercise"
                )
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(layoutInfo.contentPadding)
        ) {
            // Master Panel (Left side - 40%)
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .heightIn(min = 400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search exercises") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter chips
                    ExerciseFilterChips(
                        selectedMuscleGroup = selectedMuscleGroupFilter,
                        selectedEquipment = selectedEquipmentFilter,
                        onMuscleGroupSelected = { selectedMuscleGroupFilter = it },
                        onEquipmentSelected = { selectedEquipmentFilter = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Exercise count
                    Text(
                        text = "${filteredExercises.size} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exercise list
                    if (filteredExercises.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = "No exercises",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No exercises found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredExercises, key = { it.id }) { exercise ->
                                ExerciseListItem(
                                    exercise = exercise,
                                    isSelected = selectedExercise == exercise,
                                    onExerciseSelected = { selectedExercise = exercise }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detail Panel (Right side - 60%)
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .heightIn(min = 400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                ExerciseDetailPanel(
                    selectedExercise = selectedExercise,
                    onExerciseUpdated = { updatedExercise ->
                        viewModel.update(updatedExercise)
                        selectedExercise = updatedExercise
                    },
                    onExerciseDeleted = { exerciseToDelete ->
                        viewModel.delete(exerciseToDelete)
                        selectedExercise = null
                    },
                    onCreateNewExercise = { name, equipment, usesBodyweight, muscleGroups ->
                        viewModel.insert(name, equipment, usesBodyweight, muscleGroups)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, equipment, usesBodyweight, muscleGroups ->
                viewModel.insert(name, equipment, usesBodyweight, muscleGroups)
                showAddDialog = false
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
                Text(
                    text = "Target Muscles: ${selectedMuscleGroups.joinToString { it.name }}",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
                Text(
                    text = "Target Muscles: ${editedMuscleGroups.joinToString { it.name }}",
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    isSelected: Boolean,
    onExerciseSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExerciseSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Equipment and bodyweight indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = exercise.equipment.joinToString { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (exercise.usesBodyweight) {
                            Text(
                                text = "BW",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) 
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                        else 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Muscle group tags (limited to 3 with overflow indicator)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                exercise.targetMuscleGroups.take(3).forEach { group ->
                    Text(
                        text = group.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                if (exercise.targetMuscleGroups.size > 3) {
                    Text(
                        text = "+${exercise.targetMuscleGroups.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseFilterChips(
    selectedMuscleGroup: MuscleGroup?,
    selectedEquipment: Equipment?,
    onMuscleGroupSelected: (MuscleGroup?) -> Unit,
    onEquipmentSelected: (Equipment?) -> Unit
) {
    var showMuscleGroupMenu by remember { mutableStateOf(false) }
    var showEquipmentMenu by remember { mutableStateOf(false) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Muscle Group Filter
        FilterChip(
            onClick = { showMuscleGroupMenu = true },
            label = { 
                Text(
                    text = selectedMuscleGroup?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Muscle",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            selected = selectedMuscleGroup != null,
            leadingIcon = if (selectedMuscleGroup != null) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select muscle group",
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        
        // Equipment Filter
        FilterChip(
            onClick = { showEquipmentMenu = true },
            label = { 
                Text(
                    text = selectedEquipment?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Equipment",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            selected = selectedEquipment != null,
            leadingIcon = if (selectedEquipment != null) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select equipment",
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        
        // Clear filters button with proper spacing
        if (selectedMuscleGroup != null || selectedEquipment != null) {
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                onClick = {
                    onMuscleGroupSelected(null)
                    onEquipmentSelected(null)
                },
                label = { 
                    Text(
                        text = "Clear", 
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) 
                },
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear filters",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
    
    // Muscle Group Dropdown Menu
    DropdownMenu(
        expanded = showMuscleGroupMenu,
        onDismissRequest = { showMuscleGroupMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("All Muscle Groups") },
            onClick = {
                onMuscleGroupSelected(null)
                showMuscleGroupMenu = false
            },
            leadingIcon = {
                if (selectedMuscleGroup == null) {
                    Icon(Icons.Default.Check, contentDescription = "Selected")
                }
            }
        )
        
        MuscleGroup.entries.forEach { muscleGroup ->
            DropdownMenuItem(
                text = { 
                    Text(muscleGroup.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() })
                },
                onClick = {
                    onMuscleGroupSelected(muscleGroup)
                    showMuscleGroupMenu = false
                },
                leadingIcon = {
                    if (selectedMuscleGroup == muscleGroup) {
                        Icon(Icons.Default.Check, contentDescription = "Selected")
                    }
                }
            )
        }
    }
    
    // Equipment Dropdown Menu
    DropdownMenu(
        expanded = showEquipmentMenu,
        onDismissRequest = { showEquipmentMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("All Equipment") },
            onClick = {
                onEquipmentSelected(null)
                showEquipmentMenu = false
            },
            leadingIcon = {
                if (selectedEquipment == null) {
                    Icon(Icons.Default.Check, contentDescription = "Selected")
                }
            }
        )
        
        Equipment.entries.forEach { equipment ->
            DropdownMenuItem(
                text = { 
                    Text(equipment.name.lowercase().replaceFirstChar { it.titlecase() })
                },
                onClick = {
                    onEquipmentSelected(equipment)
                    showEquipmentMenu = false
                },
                leadingIcon = {
                    if (selectedEquipment == equipment) {
                        Icon(Icons.Default.Check, contentDescription = "Selected")
                    }
                }
            )
        }
    }
}

@Composable
private fun ExerciseDetailPanel(
    selectedExercise: Exercise?,
    onExerciseUpdated: (Exercise) -> Unit,
    onExerciseDeleted: (Exercise) -> Unit,
    onCreateNewExercise: (String, String, Boolean, List<MuscleGroup>) -> Unit
) {
    if (selectedExercise == null) {
        // Show placeholder encouraging exercise selection
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Select Exercise",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select an exercise to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "or click + to create a new exercise",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Show exercise details
        ExerciseDetailView(
            exercise = selectedExercise,
            onEdit = { updatedExercise ->
                onExerciseUpdated(updatedExercise)
            },
            onDelete = {
                onExerciseDeleted(selectedExercise)
            }
        )
    }
}

@Composable
private fun ExerciseDetailView(
    exercise: Exercise,
    onEdit: (Exercise) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (isEditing) {
        ExerciseCreateEditForm(
            exercise = exercise,
            onSave = { name, equipment, usesBodyweight, muscleGroups ->
                val updatedExercise = exercise.copy(
                    name = name,
                    equipment = try { listOf(Equipment.valueOf(equipment.uppercase().trim())) } catch (e: Exception) { exercise.equipment },
                    usesBodyweight = usesBodyweight,
                    targetMuscleGroups = muscleGroups
                )
                onEdit(updatedExercise)
                isEditing = false
            },
            onCancel = { isEditing = false }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with exercise name and action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exercise Details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit exercise",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete exercise",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                // Exercise Properties Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Properties",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Equipment
                        ExercisePropertyRow(
                            label = "Equipment",
                            value = exercise.equipment.joinToString { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } },
                            icon = Icons.Default.FitnessCenter
                        )
                        
                        // Uses Bodyweight
                        ExercisePropertyRow(
                            label = "Uses Bodyweight",
                            value = if (exercise.usesBodyweight) "Yes" else "No",
                            icon = Icons.Default.Person
                        )
                        
                        // Exercise ID (for reference)
                        ExercisePropertyRow(
                            label = "Exercise ID",
                            value = exercise.id,
                            icon = Icons.Default.Tag
                        )
                    }
                }
            }
            
            item {
                // Muscle Groups Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = "Muscle groups",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Target Muscle Groups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        if (exercise.targetMuscleGroups.isEmpty()) {
                            Text(
                                text = "No muscle groups specified",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                exercise.targetMuscleGroups.forEach { muscleGroup ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Text(
                                            text = muscleGroup.name.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.titlecase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise") },
            text = { Text("Are you sure you want to delete ${exercise.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExercisePropertyRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
private fun ExerciseCreateEditForm(
    exercise: Exercise?,
    onSave: (String, String, Boolean, List<MuscleGroup>) -> Unit,
    onCancel: () -> Unit
) {
    var exerciseName by remember { mutableStateOf(exercise?.name ?: "") }
    var exerciseEquipment by remember { mutableStateOf(exercise?.equipment?.joinToString() ?: "") }
    var exerciseUsesBodyweight by remember { mutableStateOf(exercise?.usesBodyweight ?: false) }
    var selectedMuscleGroups by remember { mutableStateOf(exercise?.targetMuscleGroups ?: emptyList()) }
    var exerciseNotes by remember { mutableStateOf(exercise?.notes ?: "") }
    var exerciseVideoLink by remember { mutableStateOf(exercise?.videoLink ?: "") }
    var showMuscleGroupDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (exercise == null) "Create Exercise" else "Edit Exercise",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter exercise details",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            // Basic Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = exerciseEquipment,
                        onValueChange = { exerciseEquipment = it },
                        label = { Text("Equipment (e.g., DUMBBELL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Uses Bodyweight")
                        Switch(
                            checked = exerciseUsesBodyweight,
                            onCheckedChange = { exerciseUsesBodyweight = it }
                        )
                    }
                }
            }
        }
        
        item {
            // Muscle Groups Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Target Muscle Groups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (selectedMuscleGroups.isEmpty()) {
                        Text(
                            text = "No muscle groups selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedMuscleGroups.forEach { muscleGroup ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Text(
                                        text = muscleGroup.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.titlecase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    Button(
                        onClick = { showMuscleGroupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Select muscle groups"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Muscle Groups")
                    }
                }
            }
        }
        
        item {
            // Optional Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Additional Information (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        value = exerciseNotes,
                        onValueChange = { exerciseNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    OutlinedTextField(
                        value = exerciseVideoLink,
                        onValueChange = { exerciseVideoLink = it },
                        label = { Text("Video Link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
        
        item {
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        if (exerciseName.isNotBlank()) {
                            onSave(exerciseName, exerciseEquipment, exerciseUsesBodyweight, selectedMuscleGroups)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = exerciseName.isNotBlank()
                ) {
                    Text(if (exercise == null) "Create" else "Save")
                }
            }
        }
    }
    
    // Muscle Group Selection Dialog
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
}
