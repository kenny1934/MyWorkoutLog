@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit = onNavigateToWorkout, // Default to existing behavior for backward compatibility
    onOpenAllClips: () -> Unit = {}
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
            onNavigateToEdit = onNavigateToEdit,
            onOpenAllClips = onOpenAllClips
        )
    } else {
        // Small screen: Original single-column layout
        HistorySingleColumnView(
            viewModel = viewModel,
            viewMode = viewMode,
            onViewModeChanged = { viewMode = it },
            onNavigateToWorkout = onNavigateToWorkout,
            onOpenAllClips = onOpenAllClips
        )
    }
}

@Composable
private fun HistorySingleColumnView(
    viewModel: HistoryViewModel,
    viewMode: HistoryViewMode,
    onViewModeChanged: (HistoryViewMode) -> Unit,
    onNavigateToWorkout: (String) -> Unit,
    onOpenAllClips: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        HistoryHeader(
            viewMode = viewMode,
            onViewModeChanged = onViewModeChanged,
            onOpenAllClips = onOpenAllClips
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
    onViewModeChanged: (HistoryViewMode) -> Unit,
    onOpenAllClips: () -> Unit
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()

    // Use Column layout when in master panel (constrained width)
    if (layoutInfo.useMasterDetail) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                AllClipsIconButton(onClick = onOpenAllClips)
            }
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
        // Row layout for single-column mode. Title flexes with weight + ellipsis
        // so it shrinks before the Videocam + FilterChips can get squashed on the
        // Z-Fold cover width.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            AllClipsIconButton(onClick = onOpenAllClips)

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
private fun AllClipsIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = "All Clips",
            tint = MaterialTheme.colorScheme.primary
        )
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
    onNavigateToEdit: (String) -> Unit,
    onOpenAllClips: () -> Unit
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
                    onViewModeChanged = onViewModeChanged,
                    onOpenAllClips = onOpenAllClips
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
          Column(modifier = Modifier.fillMaxSize()) {
            val hasAnyClip = currentWorkout.loggedExercises.any { ex ->
                ex.sets.any { !it.videoReference.isNullOrBlank() }
            }
            if (hasAnyClip) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Clips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                WorkoutVideoGalleryStrip(
                    workout = currentWorkout,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

                items(currentWorkout.loggedExercises, key = { it.id }) { exercise ->
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

            // Delete confirmation dialog
            DeleteWorkoutConfirmationDialog(
                isVisible = showDeleteDialog,
                workout = currentWorkout,
                onDismiss = { showDeleteDialog = false },
                onConfirmDelete = {
                    selectedWorkoutId?.let { id ->
                        viewModel.deleteWorkout(id)
                        showDeleteDialog = false
                        onWorkoutDeleted()
                    }
                }
            )
        }
    }
}
