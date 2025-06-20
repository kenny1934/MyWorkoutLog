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
    weightUnit: String
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
                text = set.weight?.toString() ?: "--",
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
    onNavigateToWorkout: (String) -> Unit
) {
    var viewMode by remember { mutableStateOf(HistoryViewMode.MESOCYCLES) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        // Header with view mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Workout History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            
            // View mode toggle buttons
            Row {
                FilterChip(
                    onClick = { viewMode = HistoryViewMode.MESOCYCLES },
                    label = { Text("Cycles") },
                    selected = viewMode == HistoryViewMode.MESOCYCLES
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    onClick = { viewMode = HistoryViewMode.CHRONOLOGICAL },
                    label = { Text("All") },
                    selected = viewMode == HistoryViewMode.CHRONOLOGICAL
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (viewMode) {
            HistoryViewMode.MESOCYCLES -> MesocycleHistoryView(viewModel, onNavigateToWorkout)
            HistoryViewMode.CHRONOLOGICAL -> ChronologicalHistoryView(viewModel, onNavigateToWorkout)
            HistoryViewMode.EXERCISE_FOCUSED -> ChronologicalHistoryView(viewModel, onNavigateToWorkout) // Placeholder
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
                
                // Show recent workouts
                workouts.takeLast(3).forEach { workout ->
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
            
            // Show all workouts from this cycle
            if (cycleWithWorkouts.workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                cycleWithWorkouts.workouts.forEach { workout ->
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
    showCycleInfo: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToWorkout(workout.id) },
        elevation = CardDefaults.cardElevation(2.dp)
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
fun HistoryDetailScreen(
    workoutId: String,
    viewModel: HistoryViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val workout by viewModel.getLoggedWorkoutById(workoutId).collectAsState(initial = null)

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
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                }

                // Overall Comments Section
                if (!currentWorkout.overallComments.isNullOrBlank()) {
                    item {
                        Text("Notes:", style = MaterialTheme.typography.titleMedium)
                        Text(currentWorkout.overallComments, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                items(currentWorkout.loggedExercises) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.exerciseName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            // Dynamic header based on what data is available
                            val hasReps = exercise.sets.any { it.reps != null }
                            val hasSecs = exercise.sets.any { it.secs != null }
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("Set", modifier = Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall)
                                Text("Weight (${workout!!.performedWeightUnit ?: "kg"})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                if (hasReps) {
                                    Text("Reps", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                }
                                if (hasSecs) {
                                    Text("Duration", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.width(32.dp)) // Space for expand button
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Display each logged set using the new component
                            exercise.sets.forEachIndexed { index, set ->
                                HistorySetRow(
                                    set = set,
                                    setNumber = index + 1,
                                    weightUnit = workout!!.performedWeightUnit ?: "kg"
                                )
                            }
                        }
                    }
                }
            }
        }
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