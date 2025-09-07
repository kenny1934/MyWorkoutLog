@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HistorySetRow(
    set: LoggedSet,
    setNumber: Int,
    weightUnit: String,
    exercise: LoggedExercise,
    workout: LoggedWorkout
) {
    // Determine what data is available for this set
    val hasWeightReps = set.weight != null || set.reps != null
    val hasSecs = set.secs != null
    val hasRir = set.rir != null
    val hasBands = !set.bands.isNullOrBlank()
    val hasNotes = !set.notes.isNullOrBlank()
    val hasAdditionalData = hasRir || hasBands || hasNotes
    
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        // Primary row - always visible
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set number
            Text(
                text = setNumber.toString(),
                modifier = Modifier.width(40.dp),
                fontWeight = FontWeight.Medium
            )
            
            // Weight
            Text(
                text = if (set.weight != null) formatHistoryWeightDisplay(set, exercise, workout) else "--",
                modifier = Modifier.weight(1f)
            )
            
            // Reps or Secs (based on what's available)
            if (hasWeightReps) {
                Text(
                    text = set.reps?.toString() ?: "--",
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (hasSecs) {
                Text(
                    text = "${set.secs ?: "--"}s",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Expand/collapse button for additional data
            if (hasAdditionalData) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Hide Details" else "Show Details",
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }
        }
        
        // Expandable section for additional data
        AnimatedVisibility(visible = isExpanded && hasAdditionalData) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp)
            ) {
                // RIR and Bands row
                if (hasRir || hasBands) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (hasRir) {
                            Text(
                                text = "RIR: ${set.rir}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (hasBands) {
                            Text(
                                text = "Bands: ${set.bands}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Notes section
                if (hasNotes) {
                    Text(
                        text = "Notes: ${set.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (hasRir || hasBands) 4.dp else 0.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit = onNavigateToWorkout // Default to existing behavior for backward compatibility
) {
    var viewMode by remember { mutableStateOf(HistoryViewMode.MESOCYCLES) }
    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    val layoutInfo = rememberAdaptiveLayoutInfo()

    if (layoutInfo.useMasterDetail) {
        // Large screen: Master-detail layout
        HistoryMasterDetailView(
            layoutInfo = layoutInfo,
            viewModel = viewModel,
            viewMode = viewMode,
            onViewModeChanged = { viewMode = it },
            selectedWorkoutId = selectedWorkoutId,
            onWorkoutSelected = { selectedWorkoutId = it },
            onNavigateToWorkout = onNavigateToWorkout,
            onNavigateToEdit = onNavigateToEdit
        )
    } else {
        // Small screen: Original single-column layout
        HistorySingleColumnView(
            viewModel = viewModel,
            viewMode = viewMode,
            onViewModeChanged = { viewMode = it },
            onNavigateToWorkout = onNavigateToWorkout
        )
    }
}

@Composable
private fun HistorySingleColumnView(
    viewModel: HistoryViewModel,
    viewMode: HistoryViewMode,
    onViewModeChanged: (HistoryViewMode) -> Unit,
    onNavigateToWorkout: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        HistoryHeader(
            viewMode = viewMode,
            onViewModeChanged = onViewModeChanged
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (viewMode) {
            HistoryViewMode.MESOCYCLES -> MesocycleHistoryView(viewModel, onNavigateToWorkout)
            HistoryViewMode.CHRONOLOGICAL -> ChronologicalHistoryView(viewModel, onNavigateToWorkout)
            HistoryViewMode.EXERCISE_FOCUSED -> ChronologicalHistoryView(viewModel, onNavigateToWorkout) // Placeholder
        }
    }
}

@Composable
private fun HistoryHeader(
    viewMode: HistoryViewMode,
    onViewModeChanged: (HistoryViewMode) -> Unit
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    
    // Use Column layout when in master panel (constrained width)
    if (layoutInfo.useMasterDetail) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Workout History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // View mode toggle buttons - stacked vertically for better visibility
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onViewModeChanged(HistoryViewMode.MESOCYCLES) },
                    label = { Text("Cycles") },
                    selected = viewMode == HistoryViewMode.MESOCYCLES,
                    modifier = Modifier.fillMaxWidth()
                )
                FilterChip(
                    onClick = { onViewModeChanged(HistoryViewMode.CHRONOLOGICAL) },
                    label = { Text("All") },
                    selected = viewMode == HistoryViewMode.CHRONOLOGICAL,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        // Original Row layout for single-column mode (full width available)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Workout History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            
            // View mode toggle buttons
            Row {
                FilterChip(
                    onClick = { onViewModeChanged(HistoryViewMode.MESOCYCLES) },
                    label = { Text("Cycles") },
                    selected = viewMode == HistoryViewMode.MESOCYCLES
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    onClick = { onViewModeChanged(HistoryViewMode.CHRONOLOGICAL) },
                    label = { Text("All") },
                    selected = viewMode == HistoryViewMode.CHRONOLOGICAL
                )
            }
        }
    }
}

@Composable
private fun HistoryMasterDetailView(
    layoutInfo: AdaptiveLayoutInfo,
    viewModel: HistoryViewModel,
    viewMode: HistoryViewMode,
    onViewModeChanged: (HistoryViewMode) -> Unit,
    selectedWorkoutId: String?,
    onWorkoutSelected: (String?) -> Unit,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(layoutInfo.contentPadding)
    ) {
        // Master Panel (Left side - 40%)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                HistoryHeader(
                    viewMode = viewMode,
                    onViewModeChanged = onViewModeChanged
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Master panel content with selection
                when (viewMode) {
                    HistoryViewMode.MESOCYCLES -> MesocycleHistoryMasterView(
                        viewModel = viewModel,
                        selectedWorkoutId = selectedWorkoutId,
                        onWorkoutSelected = onWorkoutSelected
                    )
                    HistoryViewMode.CHRONOLOGICAL -> ChronologicalHistoryMasterView(
                        viewModel = viewModel,
                        selectedWorkoutId = selectedWorkoutId,
                        onWorkoutSelected = onWorkoutSelected
                    )
                    HistoryViewMode.EXERCISE_FOCUSED -> ChronologicalHistoryMasterView(
                        viewModel = viewModel,
                        selectedWorkoutId = selectedWorkoutId,
                        onWorkoutSelected = onWorkoutSelected
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Detail Panel (Right side - 60%)
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            HistoryDetailPanel(
                selectedWorkoutId = selectedWorkoutId,
                viewModel = viewModel,
                onNavigateToWorkout = onNavigateToWorkout,
                onNavigateToEdit = onNavigateToEdit,
                onWorkoutDeleted = { onWorkoutSelected(null) } // Clear selection after deletion
            )
        }
    }
}

@Composable
fun HistoryDetailPanel(
    selectedWorkoutId: String?,
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onWorkoutDeleted: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (selectedWorkoutId == null) {
        // No workout selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = "Select Workout",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a workout to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Workout selected - show details
        val workout by viewModel.getLoggedWorkoutById(selectedWorkoutId).collectAsState(initial = null)
        
        val currentWorkout = workout
        if (currentWorkout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header with edit button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentWorkout.name ?: "Workout Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = { onNavigateToEdit(selectedWorkoutId) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Workout")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Workout",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Display the date prominently
                    Text(currentWorkout.date, style = MaterialTheme.typography.titleMedium)
                    
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

                items(currentWorkout.loggedExercises) { exercise ->
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
                                    "Weight (${currentWorkout.performedWeightUnit ?: "kg"})", 
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

                            // Display each logged set using the existing component
                            exercise.sets.forEachIndexed { index, set ->
                                HistorySetRow(
                                    set = set,
                                    setNumber = index + 1,
                                    weightUnit = currentWorkout.performedWeightUnit ?: "kg",
                                    exercise = exercise,
                                    workout = currentWorkout
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (currentWorkout != null) {
        DeleteWorkoutConfirmationDialog(
            isVisible = showDeleteDialog,
            workout = currentWorkout,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                viewModel.deleteWorkout(selectedWorkoutId)
                showDeleteDialog = false
                onWorkoutDeleted()
            }
        )
    }
}

@Composable
fun MesocycleHistoryMasterView(
    viewModel: HistoryViewModel,
    selectedWorkoutId: String?,
    onWorkoutSelected: (String) -> Unit
) {
    val activeCycle by viewModel.activeCycle.collectAsStateWithLifecycle()
    val activeCycleWorkouts by viewModel.activeCycleWorkouts.collectAsStateWithLifecycle()
    val completedCycles by viewModel.completedCycles.collectAsStateWithLifecycle()
    val orphanedWorkouts by viewModel.orphanedWorkouts.collectAsStateWithLifecycle()
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active cycle section
        if (activeCycle != null) {
            item {
                ActiveCycleSectionMaster(
                    cycle = activeCycle!!,
                    workouts = activeCycleWorkouts,
                    selectedWorkoutId = selectedWorkoutId,
                    onWorkoutSelected = onWorkoutSelected
                )
            }
        }
        
        // Completed cycles
        if (completedCycles.isNotEmpty()) {
            item {
                Text(
                    text = "Completed Cycles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(completedCycles) { cycleWithWorkouts ->
                CycleCardMaster(
                    cycleWithWorkouts = cycleWithWorkouts,
                    selectedWorkoutId = selectedWorkoutId,
                    onWorkoutSelected = onWorkoutSelected
                )
            }
        }
        
        // Orphaned workouts section
        if (orphanedWorkouts.isNotEmpty()) {
            item {
                Text(
                    text = "Individual Workouts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(orphanedWorkouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onNavigateToWorkout = { }, // Empty since we handle selection
                    showCycleInfo = false,
                    isSelected = selectedWorkoutId == workout.id,
                    onWorkoutSelected = onWorkoutSelected
                )
            }
        }
        
        // Empty state
        if (activeCycle == null && completedCycles.isEmpty() && orphanedWorkouts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No workouts logged yet.")
                }
            }
        }
    }
}

@Composable
fun MesocycleHistoryView(
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit
) {
    val activeCycle by viewModel.activeCycle.collectAsStateWithLifecycle()
    val activeCycleWorkouts by viewModel.activeCycleWorkouts.collectAsStateWithLifecycle()
    val completedCycles by viewModel.completedCycles.collectAsStateWithLifecycle()
    val orphanedWorkouts by viewModel.orphanedWorkouts.collectAsStateWithLifecycle()
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active cycle section
        if (activeCycle != null) {
            item {
                ActiveCycleSection(
                    cycle = activeCycle!!,
                    workouts = activeCycleWorkouts,
                    onNavigateToWorkout = onNavigateToWorkout
                )
            }
        }
        
        // Completed cycles
        if (completedCycles.isNotEmpty()) {
            item {
                Text(
                    text = "Completed Cycles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(completedCycles) { cycleWithWorkouts ->
                CycleCard(
                    cycleWithWorkouts = cycleWithWorkouts,
                    onNavigateToWorkout = onNavigateToWorkout
                )
            }
        }
        
        // Orphaned workouts section
        if (orphanedWorkouts.isNotEmpty()) {
            item {
                Text(
                    text = "Individual Workouts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(orphanedWorkouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onNavigateToWorkout = onNavigateToWorkout,
                    showCycleInfo = false
                )
            }
        }
        
        // Empty state
        if (activeCycle == null && completedCycles.isEmpty() && orphanedWorkouts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No workouts logged yet.")
                }
            }
        }
    }
}

@Composable
fun ActiveCycleSectionMaster(
    cycle: ActiveProgramCycle,
    workouts: List<LoggedWorkout>,
    selectedWorkoutId: String?,
    onWorkoutSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Active Cycle",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active: ${cycle.userCycleName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = cycle.programTemplateName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Started: ${cycle.startDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            if (workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${workouts.size} workouts completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Show recent workouts (take first 3 since list is already DESC sorted)
                workouts.take(3).forEach { workout ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkoutSelected(workout.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${workout.name ?: "Workout"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedWorkoutId == workout.id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = if (selectedWorkoutId == workout.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = workout.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedWorkoutId == workout.id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveCycleSection(
    cycle: ActiveProgramCycle,
    workouts: List<LoggedWorkout>,
    onNavigateToWorkout: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Active Cycle",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active: ${cycle.userCycleName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = cycle.programTemplateName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Started: ${cycle.startDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            if (workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${workouts.size} workouts completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Show recent workouts (take first 3 since list is already DESC sorted)
                workouts.take(3).forEach { workout ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWorkout(workout.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${workout.name ?: "Workout"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = workout.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CycleCardMaster(
    cycleWithWorkouts: CycleWithWorkouts,
    selectedWorkoutId: String?,
    onWorkoutSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cycleWithWorkouts.userCycleName ?: "Cycle ${cycleWithWorkouts.cycleId}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (cycleWithWorkouts.startDate != null) {
                Text(
                    text = "Started: ${cycleWithWorkouts.startDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cycleWithWorkouts.totalWorkouts} workouts",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(cycleWithWorkouts.completionRate * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Show all workouts from this cycle (sorted chronologically - oldest first)
            if (cycleWithWorkouts.workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                cycleWithWorkouts.workouts.sortedWith(
                    compareBy<LoggedWorkout> { it.date }.thenBy { it.startTimestamp ?: 0L }
                ).forEach { workout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWorkoutSelected(workout.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${workout.name ?: "Workout"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedWorkoutId == workout.id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selectedWorkoutId == workout.id) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = workout.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedWorkoutId == workout.id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CycleCard(
    cycleWithWorkouts: CycleWithWorkouts,
    onNavigateToWorkout: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = cycleWithWorkouts.userCycleName ?: "Cycle ${cycleWithWorkouts.cycleId}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (cycleWithWorkouts.startDate != null) {
                Text(
                    text = "Started: ${cycleWithWorkouts.startDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cycleWithWorkouts.totalWorkouts} workouts",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${(cycleWithWorkouts.completionRate * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Show all workouts from this cycle (sorted chronologically - oldest first)
            if (cycleWithWorkouts.workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                cycleWithWorkouts.workouts.sortedWith(
                    compareBy<LoggedWorkout> { it.date }.thenBy { it.startTimestamp ?: 0L }
                ).forEach { workout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWorkout(workout.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${workout.name ?: "Workout"}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = workout.date,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCard(
    workout: LoggedWorkout,
    onNavigateToWorkout: (String) -> Unit,
    showCycleInfo: Boolean = true,
    isSelected: Boolean = false,
    onWorkoutSelected: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (onWorkoutSelected != null) {
                    onWorkoutSelected(workout.id)
                } else {
                    onNavigateToWorkout(workout.id)
                }
            },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workout.name ?: "Workout", 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Show indicator if workout has detailed set data
                val hasDetailedData = workout.loggedExercises.any { exercise ->
                    exercise.sets.any { set ->
                        set.rir != null || 
                        !set.bands.isNullOrBlank() || 
                        !set.notes.isNullOrBlank()
                    }
                }
                
                if (hasDetailedData) {
                    Icon(
                        imageVector = Icons.Filled.DataUsage,
                        contentDescription = "Contains detailed set data",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Text(workout.date, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show time details directly in the list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val duration = calculateDurationMinutes(workout.startTimestamp, workout.endTimestamp)
                Text(
                    text = "Duration: ${duration?.let { "$it min" } ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = workout.startTimestamp?.let { formatTimestampToTime(it) } ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ChronologicalHistoryMasterView(
    viewModel: HistoryViewModel,
    selectedWorkoutId: String?,
    onWorkoutSelected: (String) -> Unit
) {
    val loggedWorkouts by viewModel.allLoggedWorkouts.collectAsStateWithLifecycle()

    if (loggedWorkouts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No workouts logged yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(loggedWorkouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onNavigateToWorkout = { }, // Empty since we handle selection
                    showCycleInfo = true,
                    isSelected = selectedWorkoutId == workout.id,
                    onWorkoutSelected = onWorkoutSelected
                )
            }
        }
    }
}

@Composable
fun ChronologicalHistoryView(
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit
) {
    val loggedWorkouts by viewModel.allLoggedWorkouts.collectAsStateWithLifecycle()

    if (loggedWorkouts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No workouts logged yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(loggedWorkouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onNavigateToWorkout = onNavigateToWorkout,
                    showCycleInfo = true
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(workout: LoggedWorkout) {
    val totalVolume = calculateWorkoutVolume(workout)
    val totalSets = countTotalSets(workout)
    val totalExercises = countExercises(workout)
    val avgSetVolume = calculateAverageSetVolume(workout)
    val weightUnit = workout.performedWeightUnit ?: "kg"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = "Workout Summary",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Workout Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Total Volume
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Volume",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Volume", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = "${totalVolume.toInt()} $weightUnit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Total Sets
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Numbers,
                            contentDescription = "Sets",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sets", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = totalSets.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Total Exercises
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Exercises",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exercises", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        text = totalExercises.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (avgSetVolume > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Average per set: ${avgSetVolume.toInt()} $weightUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

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
                }
            )
        }
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

                items(currentWorkout.loggedExercises) { exercise ->
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
    if (workout != null) {
        DeleteWorkoutConfirmationDialog(
            isVisible = showDeleteDialog,
            workout = workout,
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                viewModel.deleteWorkout(workoutId)
                showDeleteDialog = false
                onNavigateUp() // Navigate back after deletion
            }
        )
    }
}

private fun calculateDurationMinutes(start: Long?, end: Long?): Long? {
    if (start == null || end == null) return null
    val diff = end - start
    return TimeUnit.MILLISECONDS.toMinutes(diff)
}

private fun formatTimestampToTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

// Helper function to format weight display for bodyweight exercises in history
private fun formatHistoryWeightDisplay(set: LoggedSet, exercise: LoggedExercise, workout: LoggedWorkout): String {
    val weightUnit = workout.performedWeightUnit ?: "kg"
    val setWeight = set.weight ?: 0.0
    
    // Check if this is a bodyweight exercise based on name (more reliable than equipment)
    val isBodyweightExercise = exercise.exerciseName.contains("pull up", ignoreCase = true) ||
         exercise.exerciseName.contains("pullup", ignoreCase = true) ||
         exercise.exerciseName.contains("chin up", ignoreCase = true) ||
         exercise.exerciseName.contains("chinup", ignoreCase = true) ||
         exercise.exerciseName.contains("dip", ignoreCase = true) ||
         exercise.exerciseName.contains("push up", ignoreCase = true) ||
         exercise.exerciseName.contains("pushup", ignoreCase = true)
    
    // Handle bodyweight exercises: set.weight contains external weight only, not total
    if (isBodyweightExercise && workout.bodyweight != null) {
        val bodyweight = workout.bodyweight
        val externalWeight = setWeight // For bodyweight exercises, set.weight IS the external weight
        
        return when {
            externalWeight > 0.1 -> {
                val totalWeight = bodyweight + externalWeight
                "BW(${formatHistoryWeight(bodyweight)}$weightUnit) + ${formatHistoryWeight(externalWeight)}$weightUnit = ${formatHistoryWeight(totalWeight)}$weightUnit"
            }
            else -> "BW(${formatHistoryWeight(bodyweight)}$weightUnit)"
        }
    }
    
    // For bodyweight exercises without bodyweight data, show external weight only  
    if (isBodyweightExercise && setWeight > 0) {
        return "+${formatHistoryWeight(setWeight)}$weightUnit"
    }
    
    // Regular exercise: show total weight only
    return "${formatHistoryWeight(setWeight)}$weightUnit"
}

// Helper function to format weight with appropriate decimal precision for history
private fun formatHistoryWeight(weight: Double): String {
    return when {
        weight % 1.0 == 0.0 -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

// Workout calculation utilities for summary statistics
private fun calculateWorkoutVolume(workout: LoggedWorkout): Double {
    return workout.loggedExercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            (set.weight ?: 0.0) * (set.reps ?: 0)
        }
    }
}

private fun countTotalSets(workout: LoggedWorkout): Int {
    return workout.loggedExercises.sumOf { it.sets.size }
}

private fun countExercises(workout: LoggedWorkout): Int {
    return workout.loggedExercises.size
}

private fun calculateAverageSetVolume(workout: LoggedWorkout): Double {
    val totalVolume = calculateWorkoutVolume(workout)
    val totalSets = countTotalSets(workout)
    return if (totalSets > 0) totalVolume / totalSets else 0.0
}

@Composable
private fun ProgramContextCard(workout: LoggedWorkout) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timeline,
                    contentDescription = "Program Context",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Program Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cycle Name
            if (!workout.userCycleName.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cycle:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        workout.userCycleName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Program Template Information (if we have it)
            workout.workoutTemplateId?.let { templateId ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Template:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        workout.name ?: "Workout Template",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Program Week/Session info (if available)
            if (!workout.programWeekDefinitionId.isNullOrBlank() || !workout.programSessionDefinitionId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Session:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Program Session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteWorkoutConfirmationDialog(
    isVisible: Boolean,
    workout: LoggedWorkout,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (!isVisible) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete Workout")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this workout?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Show workout details
                Text(
                    text = "• ${workout.name ?: "Unnamed Workout"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "• ${workout.date}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (workout.activeProgramCycleId != null) {
                    Text(
                        text = "• Part of active program cycle",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}