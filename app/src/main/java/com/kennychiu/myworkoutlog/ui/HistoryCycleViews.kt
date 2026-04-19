@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    var renameTarget by remember { mutableStateOf<CycleWithWorkouts?>(null) }

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

            items(completedCycles, key = { it.cycleId }) { cycleWithWorkouts ->
                CycleCardMaster(
                    cycleWithWorkouts = cycleWithWorkouts,
                    selectedWorkoutId = selectedWorkoutId,
                    onWorkoutSelected = onWorkoutSelected,
                    onRenameClick = { renameTarget = cycleWithWorkouts }
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

            items(orphanedWorkouts, key = { it.id }) { workout ->
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

    renameTarget?.let { target ->
        RenameCycleDialog(
            currentName = target.userCycleName.orEmpty(),
            onConfirm = { newName ->
                viewModel.renameCompletedCycle(target.cycleId, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
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
    var renameTarget by remember { mutableStateOf<CycleWithWorkouts?>(null) }

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

            items(completedCycles, key = { it.cycleId }) { cycleWithWorkouts ->
                CycleCard(
                    cycleWithWorkouts = cycleWithWorkouts,
                    onNavigateToWorkout = onNavigateToWorkout,
                    onRenameClick = { renameTarget = cycleWithWorkouts }
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

            items(orphanedWorkouts, key = { it.id }) { workout ->
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

    renameTarget?.let { target ->
        RenameCycleDialog(
            currentName = target.userCycleName.orEmpty(),
            onConfirm = { newName ->
                viewModel.renameCompletedCycle(target.cycleId, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
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
    onWorkoutSelected: (String) -> Unit,
    onRenameClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cycleWithWorkouts.userCycleName ?: "Cycle ${cycleWithWorkouts.cycleId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename cycle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
    onNavigateToWorkout: (String) -> Unit,
    onRenameClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cycleWithWorkouts.userCycleName ?: "Cycle ${cycleWithWorkouts.cycleId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename cycle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
            items(loggedWorkouts, key = { it.id }) { workout ->
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
            items(loggedWorkouts, key = { it.id }) { workout ->
                WorkoutCard(
                    workout = workout,
                    onNavigateToWorkout = onNavigateToWorkout,
                    showCycleInfo = true
                )
            }
        }
    }
}
