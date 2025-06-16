@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

@Composable
fun ManageTemplatesScreen(
    viewModel: WorkoutTemplateViewModel,
    onNavigateToTemplate: (String) -> Unit,
    onStartWorkout: (String) -> Unit
) {
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new template")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Workout Templates", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (templates.isEmpty()) {
                Text(
                    "No templates yet. Click the '+' button to create one.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates) { template ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToTemplate(template.id) }
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text(template.name)
                                }
                                // START button
                                IconButton(
                                    onClick = { onStartWorkout(template.id) },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Start Workout"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("New Workout Template") },
                    text = {
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Template Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (templateName.isNotBlank()) {
                                    viewModel.insert(templateName, null)
                                    templateName = ""
                                    showDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TemplateDetailScreen(
    templateId: String,
    viewModel: WorkoutTemplateViewModel,
    onNavigateUp: () -> Unit
) {
    val templateFromDb by viewModel.getTemplateById(templateId).collectAsState(initial = null)
    val allExercises by viewModel.allMasterExercises.collectAsStateWithLifecycle()

    var editedName by remember { mutableStateOf("") }
    var editedExercises by remember { mutableStateOf<List<TemplateExercise>>(emptyList()) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(templateFromDb) {
        templateFromDb?.let {
            editedName = it.name
            editedExercises = it.templateExercises
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editedName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        templateFromDb?.let {
                            val updatedTemplate = it.copy(
                                name = editedName,
                                templateExercises = editedExercises
                            )
                            viewModel.update(updatedTemplate)
                        }
                        onNavigateUp()
                    }) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Filled.Add, "Add Exercise")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Template Name") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(editedExercises, key = { it.id }) { templateExercise ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(templateExercise.exerciseName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Loop through each set and make its targets editable
                            templateExercise.sets.forEachIndexed { setIndex, set ->
                                TemplateSetEditorRow(
                                    set = set,
                                    setNumber = setIndex + 1,
                                    onSetChange = { updatedSet ->
                                        val newSets = templateExercise.sets.toMutableList()
                                        newSets[setIndex] = updatedSet
                                        val newExercise = templateExercise.copy(sets = newSets)
                                        editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                    },
                                    onDelete = {
                                        val newSets = templateExercise.sets.toMutableList()
                                        newSets.removeAt(setIndex)
                                        val newExercise = templateExercise.copy(sets = newSets)
                                        editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                    }
                                )
                            }

                            TextButton(
                                onClick = {
                                    val newSet = TemplateExerciseSet(id = UUID.randomUUID().toString(), targetReps = null, targetSecs = null)
                                    val newExercise = templateExercise.copy(sets = templateExercise.sets + newSet)
                                    editedExercises = editedExercises.map { if (it.id == newExercise.id) newExercise else it }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Add Set") }
                        }
                    }
                }
            }
        }

        if (showAddExerciseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExerciseDialog = false },
                title = { Text("Add Exercise to Template") },
                text = {
                    LazyColumn {
                        items(allExercises) { exercise ->
                            Text(
                                text = exercise.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newTemplateExercise = TemplateExercise(
                                            id = UUID.randomUUID().toString(),
                                            exerciseId = exercise.id,
                                            exerciseName = exercise.name,
                                            targetMuscleGroups = exercise.targetMuscleGroups,
                                            equipment = exercise.equipment,
                                            order = (editedExercises.maxOfOrNull { it.order } ?: 0) + 1,
                                            sets = listOf(TemplateExerciseSet(id = UUID.randomUUID().toString()))
                                        )
                                        editedExercises = editedExercises + newTemplateExercise
                                        showAddExerciseDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showAddExerciseDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun TemplateSetEditorRow(
    set: TemplateExerciseSet,
    setNumber: Int,
    onSetChange: (TemplateExerciseSet) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Set $setNumber", modifier = Modifier.width(60.dp))
        OutlinedTextField(
            value = set.targetReps ?: "",
            onValueChange = { onSetChange(set.copy(targetReps = it, targetSecs = null)) },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = set.targetSecs ?: "",
            onValueChange = { onSetChange(set.copy(targetSecs = it, targetReps = null)) },
            label = { Text("Secs") },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Set")
        }
    }
}