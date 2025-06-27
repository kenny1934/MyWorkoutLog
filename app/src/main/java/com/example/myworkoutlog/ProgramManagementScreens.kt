@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

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
                            // Enhanced session cards
                            val sortedSessions = week.sessions.sortedBy { it.order }
                            sortedSessions.forEachIndexed { index, session ->
                                SessionCard(
                                    session = session,
                                    template = allWorkoutTemplates.find { it.id == session.workoutTemplateId },
                                    allTemplates = allWorkoutTemplates,
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
                                    },
                                    onMoveUp = if (index > 0) {
                                        {
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    val sessions = w.sessions.toMutableList()
                                                    val currentIndex = sessions.indexOfFirst { it.id == session.id }
                                                    if (currentIndex > 0) {
                                                        // Swap with previous session
                                                        val temp = sessions[currentIndex].copy(order = currentIndex)
                                                        sessions[currentIndex] = sessions[currentIndex - 1].copy(order = currentIndex + 1)
                                                        sessions[currentIndex - 1] = temp
                                                    }
                                                    w.copy(sessions = sessions)
                                                } else w
                                            }
                                        }
                                    } else null,
                                    onMoveDown = if (index < sortedSessions.size - 1) {
                                        {
                                            editedWeeks = editedWeeks.map { w ->
                                                if (w.id == week.id) {
                                                    val sessions = w.sessions.toMutableList()
                                                    val currentIndex = sessions.indexOfFirst { it.id == session.id }
                                                    if (currentIndex < sessions.size - 1) {
                                                        // Swap with next session
                                                        val temp = sessions[currentIndex].copy(order = currentIndex + 2)
                                                        sessions[currentIndex] = sessions[currentIndex + 1].copy(order = currentIndex + 1)
                                                        sessions[currentIndex + 1] = temp
                                                    }
                                                    w.copy(sessions = sessions)
                                                } else w
                                            }
                                        }
                                    } else null,
                                    isFirst = index == 0,
                                    isLast = index == sortedSessions.size - 1
                                )
                                Spacer(Modifier.height(8.dp))
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
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false
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
            // Drag handle (for future drag & drop)
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
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
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template?.name ?: "Unknown Template",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (template != null) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.error
                    )
                    
                    // Template info
                    if (template != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${template.templateExercises.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Action buttons
            Row {
                // Reorder buttons
                Column {
                    // Move up button
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = !isFirst && onMoveUp != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (!isFirst && onMoveUp != null) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Move down button
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = !isLast && onMoveDown != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (!isLast && onMoveDown != null) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Edit button
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Session",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
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