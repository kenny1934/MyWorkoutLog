@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

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
    onNavigateToDashboard: () -> Unit = {}
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val useMasterDetail = layoutInfo.useMasterDetail
    
    if (useMasterDetail) {
        ProgramMasterDetailLayout(
            programViewModel = programViewModel,
            activeCycleViewModel = activeCycleViewModel,
            templateViewModel = templateViewModel,
            onNavigateToDashboard = onNavigateToDashboard
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
                onNavigateToDashboard = onNavigateToDashboard
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
    onNavigateToDashboard: () -> Unit
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    
    var selectedProgramId by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
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
        // Header with search
        Text(
            text = "Program Blueprints",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
        CreateProgramDialog(
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
        StartCycleDialog(
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
}

@Composable
fun ManageProgramsScreen(
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    onNavigateToProgram: (String) -> Unit,
    onNavigateToDashboard: () -> Unit = {}
) {
    val programs by programViewModel.allPrograms.collectAsStateWithLifecycle()
    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var showStartCycleDialog by remember { mutableStateOf<ProgramTemplate?>(null) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateProgramDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new program")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Program Blueprints", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (programs.isEmpty()) {
                Text(
                    "No programs yet. Click the '+' button to create one.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programs) { program ->
                        Card(elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToProgram(program.id) }
                                        .padding(vertical = 16.dp)
                                ) { Text(program.name) }
                                // "Start Cycle" button
                                IconButton(onClick = { showStartCycleDialog = program }) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Start Cycle"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialog for creating a new program
            if (showCreateProgramDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateProgramDialog = false },
                    title = { Text("New Program Blueprint") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Program Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    programViewModel.insert(newName, null)
                                    newName = ""
                                    showCreateProgramDialog = false
                                }
                            }
                        ) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateProgramDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Dialog for starting a new cycle
            if (showStartCycleDialog != null) {
                val programToStart = showStartCycleDialog!!
                AlertDialog(
                    onDismissRequest = { showStartCycleDialog = null },
                    title = { Text("Start New Cycle") },
                    text = {
                        Column {
                            Text("You are about to start the program: ${programToStart.name}")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Give this cycle a name") },
                                placeholder = { Text(programToStart.name) }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val cycleName = newName.ifBlank { programToStart.name }
                            activeCycleViewModel.startCycle(programToStart, cycleName)
                            showStartCycleDialog = null
                            newName = ""
                            onNavigateToDashboard()
                        }) { Text("Start") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showStartCycleDialog = null
                        }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun ProgramEditorScreen(
    programId: String,
    programViewModel: ProgramViewModel,
    templateViewModel: WorkoutTemplateViewModel,
    onNavigateUp: () -> Unit
) {
    val programFromDb by programViewModel.getProgramById(programId).collectAsState(initial = null)
    val allWorkoutTemplates by templateViewModel.allTemplates.collectAsStateWithLifecycle()

    var editedName by remember { mutableStateOf("") }
    var editedWeeks by remember { mutableStateOf<List<ProgramWeekDefinition>>(emptyList()) }
    var showAddSessionDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(programFromDb) {
        programFromDb?.let {
            editedName = it.name
            editedWeeks = it.weeks
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
                        programFromDb?.let {
                            val updatedProgram = it.copy(name = editedName, weeks = editedWeeks)
                            programViewModel.update(updatedProgram)
                        }
                        onNavigateUp()
                    }) { Text("Save") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newWeek = ProgramWeekDefinition(
                    id = UUID.randomUUID().toString(),
                    weekLabel = "Week ${editedWeeks.size + 1}",
                    order = editedWeeks.size + 1,
                    sessions = emptyList()
                )
                editedWeeks = editedWeeks + newWeek
            }) {
                Icon(Icons.Filled.Add, "Add Week")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Program Name") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(editedWeeks) { week ->
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = week.weekLabel,
                                    onValueChange = { newLabel ->
                                        editedWeeks = editedWeeks.map {
                                            if (it.id == week.id) it.copy(weekLabel = newLabel) else it
                                        }
                                    },
                                    label = { Text("Week Label") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    editedWeeks = editedWeeks.filter { it.id != week.id }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Week")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Simple session cards with arrow button reordering
                            val sortedSessions = week.sessions.sortedBy { it.order }
                            
                            Column {
                                sortedSessions.forEachIndexed { index, session ->
                                    SessionCard(
                                        session = session,
                                        template = allWorkoutTemplates.find { it.id == session.workoutTemplateId },
                                        allTemplates = allWorkoutTemplates,
                                        canMoveUp = index > 0,
                                        canMoveDown = index < sortedSessions.size - 1,
                                        onMoveUp = {
                                            // Move session up in the order
                                            val reorderedSessions = sortedSessions.toMutableList()
                                            val temp = reorderedSessions[index]
                                            reorderedSessions[index] = reorderedSessions[index - 1]
                                            reorderedSessions[index - 1] = temp
                                            
                                            // Update orders
                                            val updatedSessions = reorderedSessions.mapIndexed { idx, s ->
                                                s.copy(order = idx + 1)
                                            }
                                            
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    w.copy(sessions = updatedSessions)
                                                } else w
                                            }
                                        },
                                        onMoveDown = {
                                            // Move session down in the order
                                            val reorderedSessions = sortedSessions.toMutableList()
                                            val temp = reorderedSessions[index]
                                            reorderedSessions[index] = reorderedSessions[index + 1]
                                            reorderedSessions[index + 1] = temp
                                            
                                            // Update orders
                                            val updatedSessions = reorderedSessions.mapIndexed { idx, s ->
                                                s.copy(order = idx + 1)
                                            }
                                            
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    w.copy(sessions = updatedSessions)
                                                } else w
                                            }
                                        },
                                        onSessionUpdated = { updatedSession ->
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    w.copy(sessions = w.sessions.map { s ->
                                                        if (s.id == session.id) updatedSession else s
                                                    })
                                                } else w
                                            }
                                        },
                                        onSessionDeleted = { sessionToDelete ->
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    // Remove session and reorder remaining sessions
                                                    val remainingSessions = w.sessions.filter { s -> s.id != sessionToDelete.id }
                                                    val reorderedSessions = remainingSessions.mapIndexed { idx, s ->
                                                        s.copy(order = idx + 1)
                                                    }
                                                    w.copy(sessions = reorderedSessions)
                                                } else w
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            TextButton(onClick = { showAddSessionDialog = week.id }) {
                                Text("Add Session to Week")
                            }
                        }
                    }
                }
            }
        }

        // Dialog for adding a session to a week
        if (showAddSessionDialog != null) {
            val weekIdToAddSessionTo = showAddSessionDialog
            var sessionName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddSessionDialog = null },
                title = { Text("Add Session") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = sessionName,
                            onValueChange = { sessionName = it },
                            label = { Text("Session Name (e.g., Day 1)") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Choose a template:", fontWeight = FontWeight.Bold)
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(allWorkoutTemplates) { template ->
                                Text(
                                    text = template.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val weekToUpdate = editedWeeks.find { it.id == weekIdToAddSessionTo }
                                            if (weekToUpdate != null) {
                                                val newSession = ProgramSessionDefinition(
                                                    id = UUID.randomUUID().toString(),
                                                    sessionName = sessionName.ifBlank { "Session ${weekToUpdate.sessions.size + 1}" },
                                                    workoutTemplateId = template.id,
                                                    order = weekToUpdate.sessions.size + 1
                                                )
                                                val updatedSessions = weekToUpdate.sessions + newSession
                                                val updatedWeek = weekToUpdate.copy(sessions = updatedSessions)
                                                editedWeeks = editedWeeks.map { if (it.id == weekIdToAddSessionTo) updatedWeek else it }
                                            }
                                            showAddSessionDialog = null
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddSessionDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun SessionCard(
    session: ProgramSessionDefinition,
    template: WorkoutTemplate?,
    allTemplates: List<WorkoutTemplate>,
    onSessionUpdated: (ProgramSessionDefinition) -> Unit,
    onSessionDeleted: (ProgramSessionDefinition) -> Unit,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showTemplateDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Arrow reordering buttons
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Session order number
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = session.order.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Session content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.sessionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Template name
                Text(
                    text = template?.name ?: "Unknown Template",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (template != null) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // Exercise count
                if (template != null) {
                    Text(
                        text = "${template.templateExercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            
            // Action buttons - simplified layout
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Edit button
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Edit Session Dialog
    if (showEditDialog) {
        var editedName by remember { mutableStateOf(session.sessionName) }
        var selectedTemplate by remember { mutableStateOf(template) }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Session") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Session Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Workout Template:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Template dropdown
                    ExposedDropdownMenuBox(
                        expanded = showTemplateDropdown,
                        onExpandedChange = { showTemplateDropdown = !showTemplateDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.name ?: "Select Template",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { 
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTemplateDropdown) 
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showTemplateDropdown,
                            onDismissRequest = { showTemplateDropdown = false }
                        ) {
                            allTemplates.forEach { template ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(template.name)
                                            Text(
                                                text = "${template.templateExercises.size} exercises",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedTemplate = template
                                        showTemplateDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedSession = session.copy(
                            sessionName = editedName.ifBlank { session.sessionName },
                            workoutTemplateId = selectedTemplate?.id ?: session.workoutTemplateId
                        )
                        onSessionUpdated(updatedSession)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Session") },
            text = { 
                Text("Are you sure you want to delete \"${session.sessionName}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSessionDeleted(session)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Enhanced Program Card with large screen optimizations
 */
@Composable
private fun EnhancedProgramCard(
    program: ProgramTemplate,
    isSelected: Boolean,
    onProgramClick: () -> Unit,
    onStartCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val touchTargetSize = workoutTouchTargetSize()
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    Card(
        modifier = modifier
            .clickable { onProgramClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = program.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (program.description?.isNotEmpty() == true) {
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onStartCycle,
                    modifier = Modifier.size(touchTargetSize)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Start Cycle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Program Statistics
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgramStatChip(
                    icon = Icons.Outlined.CalendarToday,
                    label = "${program.weeks.size} weeks"
                )
                
                val totalSessions = program.weeks.sumOf { it.sessions.size }
                ProgramStatChip(
                    icon = Icons.Outlined.FitnessCenter,
                    label = "$totalSessions sessions"
                )
            }
        }
    }
}

@Composable
private fun ProgramStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Empty State Components
 */
@Composable
private fun EmptyProgramsState(
    isSearching: Boolean,
    onCreateProgram: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Outlined.SearchOff else Icons.Outlined.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isSearching) "No programs found" else "No programs yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (isSearching) {
                "Try adjusting your search terms"
            } else {
                "Create your first program to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        if (!isSearching) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateProgram) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Program")
            }
        }
    }
}

@Composable
private fun ProgramDetailEmptyState(
    onCreateProgram: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Select a program",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Choose a program from the list to view or edit its details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Enhanced Dialogs
 */
@Composable
private fun CreateProgramDialog(
    newName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "New Program Blueprint",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    "Create a new workout program with custom weeks and sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = newName,
                    onValueChange = onNameChange,
                    label = { Text("Program Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = newName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StartCycleDialog(
    program: ProgramTemplate,
    cycleName: String,
    onCycleNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Start New Cycle",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    "You are about to start the program: ${program.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (program.weeks.isNotEmpty()) {
                    Text(
                        "This program contains ${program.weeks.size} weeks with ${program.weeks.sumOf { it.sessions.size }} total sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = cycleName,
                    onValueChange = onCycleNameChange,
                    label = { Text("Give this cycle a name") },
                    placeholder = { Text(program.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

@Composable
private fun ProgramDetailViewer(
    program: ProgramTemplate,
    allTemplates: List<WorkoutTemplate>,
    activeCycleViewModel: ActiveCycleViewModel,
    onStartEdit: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartCycleDialog by remember { mutableStateOf(false) }
    var cycleName by remember { mutableStateOf("") }
    
    Column(modifier = modifier) {
        // Title row with compact actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Icon-only Edit button for maximum space efficiency
                IconButton(onClick = onStartEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit Program",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Compact Start button with shorter text
                FilledTonalButton(
                    onClick = { showStartCycleDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", maxLines = 1)
                }
            }
        }
        
        // Description now spans full width below title row
        if (program.description?.isNotEmpty() == true) {
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Program Overview Stats - horizontal distribution
        ProgramOverviewStats(program = program)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Weeks Overview
        if (program.weeks.isNotEmpty()) {
            Text(
                text = "Program Structure",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(program.weeks) { week ->
                    WeekOverviewCard(
                        week = week,
                        allTemplates = allTemplates
                    )
                }
            }
        } else {
            EmptyProgramStructure(onStartEdit = onStartEdit)
        }
    }
    
    if (showStartCycleDialog) {
        StartCycleDialog(
            program = program,
            cycleName = cycleName,
            onCycleNameChange = { cycleName = it },
            onConfirm = {
                val finalCycleName = cycleName.ifBlank { program.name }
                activeCycleViewModel.startCycle(program, finalCycleName)
                showStartCycleDialog = false
                cycleName = ""
                onNavigateToDashboard()
            },
            onDismiss = {
                showStartCycleDialog = false
                cycleName = ""
            }
        )
    }
}

@Composable
private fun ProgramOverviewStats(program: ProgramTemplate) {
    val totalSessions = program.weeks.sumOf { it.sessions.size }
    val uniqueTemplates = program.weeks.flatMap { it.sessions.map { session -> session.workoutTemplateId } }.distinct().size
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OverviewStatCard(
            label = "Weeks",
            value = program.weeks.size.toString(),
            icon = Icons.Outlined.CalendarToday
        )
        
        OverviewStatCard(
            label = "Sessions",
            value = totalSessions.toString(),
            icon = Icons.Outlined.FitnessCenter
        )
        
        OverviewStatCard(
            label = "Templates",
            value = uniqueTemplates.toString(),
            icon = Icons.Outlined.Assignment
        )
    }
}


@Composable
private fun OverviewStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WeekOverviewCard(
    week: ProgramWeekDefinition,
    allTemplates: List<WorkoutTemplate>
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = week.weekLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            week.sessions.sortedBy { it.order }.forEach { session ->
                val template = allTemplates.find { it.id == session.workoutTemplateId }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${session.order}. ${session.sessionName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = template?.name ?: "Unknown Template",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (template != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    
                    if (template != null) {
                        Text(
                            text = "${template.templateExercises.size} exercises",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                if (session != week.sessions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramStructure(onStartEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.EditCalendar,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No program structure yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Start editing to add weeks and sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onStartEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Editing")
        }
    }
}

// Enhanced Program Editor - using existing logic with improved layout
@Composable
private fun EnhancedProgramEditor(
    program: ProgramTemplate,
    allTemplates: List<WorkoutTemplate>,
    onSave: (ProgramTemplate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedName by remember { mutableStateOf(program.name) }
    var editedWeeks by remember { mutableStateOf(program.weeks) }
    var showAddSessionDialog by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = modifier) {
        // Header with actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.requiredWidthIn(min = 64.dp)
                ) {
                    Text(
                        "Cancel", 
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                
                Button(
                    onClick = {
                        val updatedProgram = program.copy(
                            name = editedName,
                            weeks = editedWeeks
                        )
                        onSave(updatedProgram)
                    },
                    modifier = Modifier.requiredWidthIn(min = 64.dp)
                ) {
                    Text(
                        "Save", 
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Program Name Editor
        OutlinedTextField(
            value = editedName,
            onValueChange = { editedName = it },
            label = { Text("Program Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Weeks Editor - reusing existing SessionCard logic
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(workoutElementSpacing()),
            modifier = Modifier.weight(1f)
        ) {
            items(editedWeeks) { week ->
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = week.weekLabel,
                                onValueChange = { newLabel ->
                                    editedWeeks = editedWeeks.map {
                                        if (it.id == week.id) it.copy(weekLabel = newLabel) else it
                                    }
                                },
                                label = { Text("Week Label") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = {
                                    editedWeeks = editedWeeks.filter { it.id != week.id }
                                },
                                modifier = Modifier.size(workoutTouchTargetSize())
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Week")
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Enhanced session cards with better spacing
                        val sortedSessions = week.sessions.sortedBy { it.order }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sortedSessions.forEachIndexed { index, session ->
                                SessionCard(
                                    session = session,
                                    template = allTemplates.find { it.id == session.workoutTemplateId },
                                    allTemplates = allTemplates,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < sortedSessions.size - 1,
                                    onMoveUp = {
                                        // Move session up in the order
                                        val reorderedSessions = sortedSessions.toMutableList()
                                        val temp = reorderedSessions[index]
                                        reorderedSessions[index] = reorderedSessions[index - 1]
                                        reorderedSessions[index - 1] = temp
                                        
                                        // Update orders
                                        val updatedSessions = reorderedSessions.mapIndexed { idx, s ->
                                            s.copy(order = idx + 1)
                                        }
                                        
                                        editedWeeks = editedWeeks.map { w ->
                                            if (w.id == week.id) {
                                                w.copy(sessions = updatedSessions)
                                            } else w
                                        }
                                    },
                                    onMoveDown = {
                                        // Move session down in the order
                                        val reorderedSessions = sortedSessions.toMutableList()
                                        val temp = reorderedSessions[index]
                                        reorderedSessions[index] = reorderedSessions[index + 1]
                                        reorderedSessions[index + 1] = temp
                                        
                                        // Update orders
                                        val updatedSessions = reorderedSessions.mapIndexed { idx, s ->
                                            s.copy(order = idx + 1)
                                        }
                                        
                                        editedWeeks = editedWeeks.map { w ->
                                            if (w.id == week.id) {
                                                w.copy(sessions = updatedSessions)
                                            } else w
                                        }
                                    },
                                    onSessionUpdated = { updatedSession ->
                                        editedWeeks = editedWeeks.map { w ->
                                            if (w.id == week.id) {
                                                w.copy(sessions = w.sessions.map { s ->
                                                    if (s.id == session.id) updatedSession else s
                                                })
                                            } else w
                                        }
                                    },
                                    onSessionDeleted = { sessionToDelete ->
                                        editedWeeks = editedWeeks.map { w ->
                                            if (w.id == week.id) {
                                                // Remove session and reorder remaining sessions
                                                val remainingSessions = w.sessions.filter { s -> s.id != sessionToDelete.id }
                                                val reorderedSessions = remainingSessions.mapIndexed { idx, s ->
                                                    s.copy(order = idx + 1)
                                                }
                                                w.copy(sessions = reorderedSessions)
                                            } else w
                                        }
                                    }
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = { showAddSessionDialog = week.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Session to Week")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add Week Button
        OutlinedButton(
            onClick = {
                val newWeek = ProgramWeekDefinition(
                    id = UUID.randomUUID().toString(),
                    weekLabel = "Week ${editedWeeks.size + 1}",
                    order = editedWeeks.size + 1,
                    sessions = emptyList()
                )
                editedWeeks = editedWeeks + newWeek
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Week")
        }
    }
    
    // Add Session Dialog - reusing existing implementation
    if (showAddSessionDialog != null) {
        val weekIdToAddSessionTo = showAddSessionDialog
        var sessionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSessionDialog = null },
            title = { Text("Add Session") },
            text = {
                Column {
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Session Name (e.g., Day 1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Choose a template:", fontWeight = FontWeight.Bold)
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(allTemplates) { template ->
                            Text(
                                text = template.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val weekToUpdate = editedWeeks.find { it.id == weekIdToAddSessionTo }
                                        if (weekToUpdate != null) {
                                            val newSession = ProgramSessionDefinition(
                                                id = UUID.randomUUID().toString(),
                                                sessionName = sessionName.ifBlank { "Session ${weekToUpdate.sessions.size + 1}" },
                                                workoutTemplateId = template.id,
                                                order = weekToUpdate.sessions.size + 1
                                            )
                                            val updatedSessions = weekToUpdate.sessions + newSession
                                            val updatedWeek = weekToUpdate.copy(sessions = updatedSessions)
                                            editedWeeks = editedWeeks.map { if (it.id == weekIdToAddSessionTo) updatedWeek else it }
                                        }
                                        showAddSessionDialog = null
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddSessionDialog = null }) { Text("Cancel") }
            }
        )
    }
}