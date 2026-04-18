@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.*

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
        },
        contentWindowInsets = WindowInsets(0),
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
                                    editedWeeks = duplicateWeekInto(editedWeeks, week)
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Week")
                                }
                                IconButton(onClick = {
                                    editedWeeks = editedWeeks.filter { it.id != week.id }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Week")
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = week.isDeloadWeek,
                                    onClick = {
                                        editedWeeks = editedWeeks.map {
                                            if (it.id == week.id) it.copy(isDeloadWeek = !it.isDeloadWeek) else it
                                        }
                                    },
                                    label = { Text("Deload week") },
                                    leadingIcon = if (week.isDeloadWeek) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                                Spacer(Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = week.targetRir.orEmpty(),
                                    onValueChange = { newValue ->
                                        editedWeeks = editedWeeks.map {
                                            if (it.id == week.id) it.copy(targetRir = newValue.takeIf { s -> s.isNotBlank() }) else it
                                        }
                                    },
                                    label = { Text("Target RIR") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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

// Enhanced Program Editor - improved layout for master-detail
@Composable
fun EnhancedProgramEditor(
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
                                    editedWeeks = duplicateWeekInto(editedWeeks, week)
                                },
                                modifier = Modifier.size(workoutTouchTargetSize())
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Week")
                            }
                            IconButton(
                                onClick = {
                                    editedWeeks = editedWeeks.filter { it.id != week.id }
                                },
                                modifier = Modifier.size(workoutTouchTargetSize())
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Week")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = week.isDeloadWeek,
                                onClick = {
                                    editedWeeks = editedWeeks.map {
                                        if (it.id == week.id) it.copy(isDeloadWeek = !it.isDeloadWeek) else it
                                    }
                                },
                                label = { Text("Deload week") },
                                leadingIcon = if (week.isDeloadWeek) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = week.targetRir.orEmpty(),
                                onValueChange = { newValue ->
                                    editedWeeks = editedWeeks.map {
                                        if (it.id == week.id) it.copy(targetRir = newValue.takeIf { s -> s.isNotBlank() }) else it
                                    }
                                },
                                label = { Text("Target RIR") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
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
