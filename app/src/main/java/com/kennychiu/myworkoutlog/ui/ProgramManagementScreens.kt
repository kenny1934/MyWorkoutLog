@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Adaptive Program Management Screen - Master-Detail Layout for Large Screens
 * Optimized for Galaxy Z Fold 6 and other large screen devices
 */
@Composable
fun AdaptiveProgramManagementScreen(
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    templateViewModel: WorkoutTemplateViewModel,
    onNavigateToProgram: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateUp: (() -> Unit)? = null
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val useMasterDetail = layoutInfo.useMasterDetail

    if (useMasterDetail) {
        ProgramMasterDetailLayout(
            programViewModel = programViewModel,
            activeCycleViewModel = activeCycleViewModel,
            templateViewModel = templateViewModel,
            onNavigateToDashboard = onNavigateToDashboard,
            onNavigateUp = onNavigateUp
        )
    } else {
        // Use traditional navigation-based screens for compact devices
        var selectedProgramId by remember { mutableStateOf<String?>(null) }

        if (selectedProgramId != null) {
            ProgramEditorScreen(
                programId = selectedProgramId!!,
                programViewModel = programViewModel,
                templateViewModel = templateViewModel,
                onNavigateUp = { selectedProgramId = null }
            )
        } else {
            ManageProgramsScreen(
                programViewModel = programViewModel,
                activeCycleViewModel = activeCycleViewModel,
                onNavigateToProgram = { programId -> selectedProgramId = programId },
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateUp = onNavigateUp
            )
        }
    }
}

/**
 * Master-Detail Layout for Large Screens
 */
@Composable
private fun ProgramMasterDetailLayout(
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    templateViewModel: WorkoutTemplateViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateUp: (() -> Unit)?
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()

    var selectedProgramId by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var programIdToDelete by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(
        title = "Program Blueprints",
        onNavigateUp = onNavigateUp
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(layoutInfo.contentPadding)
        ) {
            // Master Panel - Program List (40% of width)
            Surface(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                ProgramMasterPanel(
                    programViewModel = programViewModel,
                    activeCycleViewModel = activeCycleViewModel,
                    selectedProgramId = selectedProgramId,
                    onProgramSelected = { programId ->
                        selectedProgramId = programId
                        isEditing = false
                    },
                    onNavigateToDashboard = onNavigateToDashboard,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detail Panel - Program Details/Editor (60% of width)
            Surface(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (selectedProgramId != null) {
                    ProgramDetailPanel(
                        programId = selectedProgramId!!,
                        programViewModel = programViewModel,
                        templateViewModel = templateViewModel,
                        activeCycleViewModel = activeCycleViewModel,
                        isEditing = isEditing,
                        onEditModeChanged = { isEditing = it },
                        onNavigateToDashboard = onNavigateToDashboard,
                        onDeleteProgram = { programId ->
                            programIdToDelete = programId
                            showDeleteConfirmation = true
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    ProgramDetailEmptyState(
                        onCreateProgram = {
                            // This will be handled by the master panel
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && programIdToDelete != null) {
        val programs by programViewModel.allPrograms.collectAsStateWithLifecycle()
        val programToDelete = programs.find { it.id == programIdToDelete }

        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                programIdToDelete = null
            },
            title = {
                Text(
                    text = "Delete Program",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete \"${programToDelete?.name}\"?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will permanently delete the program blueprint and cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        programIdToDelete?.let { programId ->
                            programViewModel.deleteById(programId)
                            // Clear selection if deleted program was selected
                            if (selectedProgramId == programId) {
                                selectedProgramId = null
                            }
                        }
                        showDeleteConfirmation = false
                        programIdToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        programIdToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Enhanced Master Panel for Program List
 */
@Composable
private fun ProgramMasterPanel(
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    selectedProgramId: String?,
    onProgramSelected: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val programs by programViewModel.allPrograms.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showStartCycleDialog by remember { mutableStateOf<ProgramTemplate?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var programToDelete by remember { mutableStateOf<ProgramTemplate?>(null) }
    var newName by remember { mutableStateOf("") }

    // Filter programs based on search
    val filteredPrograms = remember(programs, searchQuery) {
        if (searchQuery.isBlank()) {
            programs
        } else {
            programs.filter { program ->
                program.name.contains(searchQuery, ignoreCase = true) ||
                program.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Column(modifier = modifier) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search programs") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Programs List
        if (filteredPrograms.isEmpty()) {
            EmptyProgramsState(
                isSearching = searchQuery.isNotEmpty(),
                onCreateProgram = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredPrograms) { program ->
                    EnhancedProgramCard(
                        program = program,
                        isSelected = program.id == selectedProgramId,
                        onProgramClick = { onProgramSelected(program.id) },
                        onStartCycle = { showStartCycleDialog = program },
                        onDeleteProgram = {
                            programToDelete = program
                            showDeleteConfirmation = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Create Program Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Program")
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateProgramSheet(
            newName = newName,
            onNameChange = { newName = it },
            onConfirm = {
                if (newName.isNotBlank()) {
                    programViewModel.insert(newName, null)
                    newName = ""
                    showCreateDialog = false
                }
            },
            onDismiss = {
                showCreateDialog = false
                newName = ""
            }
        )
    }

    if (showStartCycleDialog != null) {
        StartCycleSheet(
            program = showStartCycleDialog!!,
            cycleName = newName,
            onCycleNameChange = { newName = it },
            onConfirm = {
                val cycleName = newName.ifBlank { showStartCycleDialog!!.name }
                activeCycleViewModel.startCycle(showStartCycleDialog!!, cycleName)
                showStartCycleDialog = null
                newName = ""
                onNavigateToDashboard()
            },
            onDismiss = {
                showStartCycleDialog = null
                newName = ""
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && programToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                programToDelete = null
            },
            title = {
                Text(
                    text = "Delete Program",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to delete \"${programToDelete?.name}\"?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will permanently delete the program blueprint and cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        programToDelete?.let { program ->
                            programViewModel.deleteById(program.id)
                        }
                        showDeleteConfirmation = false
                        programToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        programToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Program Detail Panel - View/Edit Mode
 */
@Composable
private fun ProgramDetailPanel(
    programId: String,
    programViewModel: ProgramViewModel,
    templateViewModel: WorkoutTemplateViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    isEditing: Boolean,
    onEditModeChanged: (Boolean) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onDeleteProgram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val programFromDb by programViewModel.getProgramById(programId).collectAsState(initial = null)
    val allWorkoutTemplates by templateViewModel.allTemplates.collectAsStateWithLifecycle()

    if (programFromDb == null) {
        ProgramDetailLoadingState()
        return
    }

    if (isEditing) {
        EnhancedProgramEditor(
            program = programFromDb!!,
            allTemplates = allWorkoutTemplates,
            onSave = { updatedProgram ->
                programViewModel.update(updatedProgram)
                onEditModeChanged(false)
            },
            onCancel = { onEditModeChanged(false) },
            modifier = modifier
        )
    } else {
        ProgramDetailViewer(
            program = programFromDb!!,
            allTemplates = allWorkoutTemplates,
            activeCycleViewModel = activeCycleViewModel,
            onStartEdit = { onEditModeChanged(true) },
            onNavigateToDashboard = onNavigateToDashboard,
            onDeleteProgram = onDeleteProgram,
            modifier = modifier
        )
    }
}

@Composable
private fun ProgramDetailLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
