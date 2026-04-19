@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
                },
                windowInsets = WindowInsets(0)
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
            val lazyListState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                editedWeeks = moveWeek(editedWeeks, from.index, to.index)
            }
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(editedWeeks, key = { _, w -> w.id }) { _, week ->
                    ReorderableItem(reorderState, key = week.id) { _ ->
                        Card(elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                WeekCardHeader(
                                    week = week,
                                    onLabelChange = { newLabel ->
                                        editedWeeks = editedWeeks.map {
                                            if (it.id == week.id) it.copy(weekLabel = newLabel) else it
                                        }
                                    },
                                    onDuplicate = {
                                        editedWeeks = duplicateWeekInto(editedWeeks, week)
                                    },
                                    onDelete = {
                                        editedWeeks = editedWeeks.filter { it.id != week.id }
                                    }
                                )
                                Spacer(Modifier.height(12.dp))
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
                                val sortedSessions = remember(week.sessions) { week.sessions.sortedBy { it.order } }

                                ReorderableColumn(
                                    list = sortedSessions,
                                    onSettle = { fromIndex, toIndex ->
                                        editedWeeks = moveSessionWithinWeek(editedWeeks, week.id, fromIndex, toIndex)
                                    },
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) { sIndex, session, _ ->
                                    key(session.id) {
                                        ReorderableItem {
                                            val sessionDragHandle: @Composable () -> Unit = {
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier.size(24.dp).draggableHandle()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DragHandle,
                                                        contentDescription = "Drag to reorder session",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
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
                                                            val remainingSessions = w.sessions.filter { s -> s.id != sessionToDelete.id }
                                                            val reorderedSessions = remainingSessions.mapIndexed { idx, s ->
                                                                s.copy(order = idx + 1)
                                                            }
                                                            w.copy(sessions = reorderedSessions)
                                                        } else w
                                                    }
                                                },
                                                otherWeeks = editedWeeks.filter { it.id != week.id },
                                                onMoveToWeek = { targetWeekId ->
                                                    editedWeeks = moveSessionToWeek(editedWeeks, week.id, session.id, targetWeekId)
                                                },
                                                dragHandle = sessionDragHandle
                                            )
                                        }
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
        }

        // Bottom sheet for adding a session to a week
        if (showAddSessionDialog != null) {
            val weekIdToAddSessionTo = showAddSessionDialog
            val weekToUpdate = editedWeeks.find { it.id == weekIdToAddSessionTo }
            AddSessionSheet(
                allTemplates = allWorkoutTemplates,
                currentSessionCount = weekToUpdate?.sessions?.size ?: 0,
                onAdd = { newSession ->
                    if (weekToUpdate != null) {
                        val updatedSessions = weekToUpdate.sessions + newSession
                        val updatedWeek = weekToUpdate.copy(sessions = updatedSessions)
                        editedWeeks = editedWeeks.map {
                            if (it.id == weekIdToAddSessionTo) updatedWeek else it
                        }
                    }
                    showAddSessionDialog = null
                },
                onDismiss = { showAddSessionDialog = null }
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
    otherWeeks: List<ProgramWeekDefinition> = emptyList(),
    onMoveToWeek: ((targetWeekId: String) -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showTemplateDropdown by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

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
            if (dragHandle != null) {
                dragHandle()
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Session order badge
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.sessionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = template?.name ?: "Unknown Template",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (template != null)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (template != null) {
                    Text(
                        text = "${template.templateExercises.size} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            // Single overflow menu collapses the old Edit + Move + Delete icon row.
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Session options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showOverflowMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        }
                    )
                    if (onMoveToWeek != null && otherWeeks.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Move to another week") },
                            onClick = {
                                showOverflowMenu = false
                                showMoveDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            showDeleteConfirmation = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

    // Move-to-Week Dialog
    if (showMoveDialog && onMoveToWeek != null) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to Week") },
            text = {
                Column {
                    Text(
                        text = "Move \"${session.sessionName}\" to:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    otherWeeks.forEach { targetWeek ->
                        Text(
                            text = targetWeek.weekLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMoveToWeek(targetWeek.id)
                                    showMoveDialog = false
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
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
        val enhancedLazyListState = rememberLazyListState()
        val enhancedReorderState = rememberReorderableLazyListState(enhancedLazyListState) { from, to ->
            editedWeeks = moveWeek(editedWeeks, from.index, to.index)
        }
        LazyColumn(
            state = enhancedLazyListState,
            verticalArrangement = Arrangement.spacedBy(workoutElementSpacing()),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(editedWeeks, key = { _, w -> w.id }) { _, week ->
                ReorderableItem(enhancedReorderState, key = week.id) { _ ->
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            WeekCardHeader(
                                week = week,
                                onLabelChange = { newLabel ->
                                    editedWeeks = editedWeeks.map {
                                        if (it.id == week.id) it.copy(weekLabel = newLabel) else it
                                    }
                                },
                                onDuplicate = {
                                    editedWeeks = duplicateWeekInto(editedWeeks, week)
                                },
                                onDelete = {
                                    editedWeeks = editedWeeks.filter { it.id != week.id }
                                }
                            )
                            Spacer(Modifier.height(12.dp))

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

                            val sortedSessions = remember(week.sessions) { week.sessions.sortedBy { it.order } }

                            ReorderableColumn(
                                list = sortedSessions,
                                onSettle = { fromIndex, toIndex ->
                                    editedWeeks = moveSessionWithinWeek(editedWeeks, week.id, fromIndex, toIndex)
                                },
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) { sIndex, session, _ ->
                                key(session.id) {
                                    ReorderableItem {
                                        val sessionDragHandle: @Composable () -> Unit = {
                                            IconButton(
                                                onClick = {},
                                                modifier = Modifier.size(24.dp).draggableHandle()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DragHandle,
                                                    contentDescription = "Drag to reorder session",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        SessionCard(
                                            session = session,
                                            template = allTemplates.find { it.id == session.workoutTemplateId },
                                            allTemplates = allTemplates,
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
                                                        val remainingSessions = w.sessions.filter { s -> s.id != sessionToDelete.id }
                                                        val reorderedSessions = remainingSessions.mapIndexed { idx, s ->
                                                            s.copy(order = idx + 1)
                                                        }
                                                        w.copy(sessions = reorderedSessions)
                                                    } else w
                                                }
                                            },
                                            otherWeeks = editedWeeks.filter { it.id != week.id },
                                            onMoveToWeek = { targetWeekId ->
                                                editedWeeks = moveSessionToWeek(editedWeeks, week.id, session.id, targetWeekId)
                                            },
                                            dragHandle = sessionDragHandle
                                        )
                                    }
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

    // Bottom sheet for adding a session to a week (master-detail editor)
    if (showAddSessionDialog != null) {
        val weekIdToAddSessionTo = showAddSessionDialog
        val weekToUpdate = editedWeeks.find { it.id == weekIdToAddSessionTo }
        AddSessionSheet(
            allTemplates = allTemplates,
            currentSessionCount = weekToUpdate?.sessions?.size ?: 0,
            onAdd = { newSession ->
                if (weekToUpdate != null) {
                    val updatedSessions = weekToUpdate.sessions + newSession
                    val updatedWeek = weekToUpdate.copy(sessions = updatedSessions)
                    editedWeeks = editedWeeks.map {
                        if (it.id == weekIdToAddSessionTo) updatedWeek else it
                    }
                }
                showAddSessionDialog = null
            },
            onDismiss = { showAddSessionDialog = null }
        )
    }
}

/**
 * Week-card header row: drag handle + label field + overflow menu (Duplicate / Delete).
 * Up/down arrow buttons removed — the drag handle covers ordering and kept alongside them was
 * just noise. Must be invoked inside a `ReorderableCollectionItemScope` so `draggableHandle()`
 * resolves.
 */
@Composable
private fun sh.calvin.reorderable.ReorderableCollectionItemScope.WeekCardHeader(
    week: ProgramWeekDefinition,
    onLabelChange: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {},
            modifier = Modifier.size(24.dp).draggableHandle()
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder week",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = week.weekLabel,
            onValueChange = onLabelChange,
            label = { Text("Week Label") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Week options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Duplicate Week") },
                    onClick = {
                        showOverflowMenu = false
                        onDuplicate()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Delete Week",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showOverflowMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}
