package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

private const val BODYWEIGHT_DEFAULT_KG = 75.0
private val DECIMAL_INPUT_REGEX = Regex("^\\d*\\.?\\d*\$")

/**
 * Adaptive workout layout container that switches between single-column and master-detail layouts
 */
@Composable
fun AdaptiveWorkoutLayout(
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit
) {
    val useMasterDetail = shouldUseWorkoutMasterDetail()
    Box(modifier = modifier) {
        content(useMasterDetail)
    }
}

/**
 * Master-detail workout view for large screens
 */
@Composable
fun MasterDetailWorkoutView(
    activeWorkout: LoggedWorkout?,
    exerciseList: List<LoggedExercise>,
    selectedExerciseId: String?,
    onExerciseSelected: (String) -> Unit,
    sessionContent: @Composable () -> Unit,
    selectedExerciseContent: @Composable () -> Unit,
    navigationRail: @Composable () -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    // Optional cycle-context banner rendered full-width above both panels.
    // Mirrors the compact layout's top-of-LazyColumn banner so deload / target
    // RIR context is visible on the Z Fold's tablet layout too.
    contextBanner: (@Composable () -> Unit)? = null,
    lastPerformanceFor: (String) -> String? = { null },
    progressionHintFor: (String) -> String? = { null },
) {
    val masterWidth = workoutMasterPanelWidth()
    val spacing = workoutElementSpacing()

    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Navigation rail for quick actions - positioned with top padding only
        Box(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        ) {
            navigationRail()
        }

        // Content area with proper padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = spacing)
        ) {
            if (contextBanner != null) {
                Box(modifier = Modifier.padding(end = spacing, bottom = spacing)) {
                    contextBanner()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Master panel - Exercise list and session overview
                Card(
                    modifier = Modifier
                        .width(masterWidth)
                        .fillMaxHeight()
                        .padding(end = spacing),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Session overview section
                Text(
                    text = "Session Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Session content (bodyweight, notes, timer info)
                sessionContent()
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Exercise list
                Text(
                    text = "Exercises (${exerciseList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(exerciseList) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            isSelected = exercise.id == selectedExerciseId,
                            onSelected = { onExerciseSelected(exercise.id) },
                            lastPerformance = lastPerformanceFor(exercise.exerciseId),
                            progressionHint = progressionHintFor(exercise.exerciseId),
                        )
                    }
                }
            }
        }
        
            // Detail panel - Selected exercise details
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                selectedExerciseContent()
            }
            } // end inner master+detail Row
        }
    }
}

/**
 * Exercise list item for master panel
 */
@Composable
fun ExerciseListItem(
    exercise: LoggedExercise,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    lastPerformance: String? = null,
    progressionHint: String? = null,
) {
    val setsCompleted = exercise.sets.count { set -> 
        (set.weight != null && set.reps != null) || set.secs != null 
    }
    val totalSets = exercise.sets.size
    val completionPercentage = if (totalSets > 0) (setsCompleted.toFloat() / totalSets) else 0f
    
    Card(
        onClick = onSelected,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "$setsCompleted/$totalSets",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            lastPerformance?.let { performance ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last: $performance",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            progressionHint?.let { hint ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { completionPercentage },
                modifier = Modifier.fillMaxWidth(),
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Status indicator
            if (exercise.isSubstitute == true) {
                Spacer(modifier = Modifier.height(4.dp))
                val orig = exercise.originalExerciseName?.takeIf { it.isNotBlank() }
                Text(
                    text = if (orig != null) "Substituted from $orig" else "Substituted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/**
 * Workout navigation rail for large screens
 */
@Composable
fun WorkoutNavigationRail(
    onAddExercise: () -> Unit,
    onStartRestTimer: () -> Unit,
    onFinishWorkout: () -> Unit,
    timerIsRunning: Boolean,
    sessionElapsedTime: Int,
    allSetsComplete: Boolean,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${sessionElapsedTime / 60}:${String.format("%02d", sessionElapsedTime % 60)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        // Add Exercise
        NavigationRailItem(
            icon = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Exercise",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Add") },
            selected = false,
            onClick = onAddExercise
        )
        
        // Rest Timer
        NavigationRailItem(
            icon = {
                Icon(
                    if (timerIsRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                    contentDescription = "Rest Timer",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Rest") },
            selected = timerIsRunning,
            onClick = onStartRestTimer
        )
        
        Spacer(modifier = Modifier.weight(1f))

        if (allSetsComplete) {
            NavigationRailItem(
                icon = {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Finish",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Finish") },
                selected = false,
                onClick = onFinishWorkout,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Compact session info: bodyweight stepper + session notes, flat (no hero Card).
 */
@Composable
fun CompactSessionInfo(
    bodyweightText: String,
    onBodyweightChange: (String) -> Unit,
    sessionNotesText: String,
    onSessionNotesChange: (String) -> Unit,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val bwValue = bodyweightText.toDoubleOrNull()
    val step = 0.5
    val minBw = 30.0
    val maxBw = 300.0

    fun adjust(delta: Double) {
        // Seed from typical adult bodyweight when the field is empty so ± produce sane first values.
        val base = bwValue ?: BODYWEIGHT_DEFAULT_KG
        val next = (base + delta).coerceIn(minBw, maxBw)
        val formatted = "%.1f".format(next).trimEnd('0').trimEnd('.')
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onBodyweightChange(formatted)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { adjust(-step) },
                enabled = bwValue == null || bwValue > minBw,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease bodyweight",
                    modifier = Modifier.size(18.dp)
                )
            }
            OutlinedTextField(
                value = bodyweightText,
                onValueChange = { newText ->
                    if (newText.matches(DECIMAL_INPUT_REGEX)) onBodyweightChange(newText)
                },
                label = { Text("Bodyweight") },
                trailingIcon = {
                    Text(
                        text = weightUnit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = { adjust(step) },
                enabled = bwValue == null || bwValue < maxBw,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase bodyweight",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedTextField(
            value = sessionNotesText,
            onValueChange = onSessionNotesChange,
            label = { Text("Notes") },
            placeholder = { Text("How are you feeling? Any observations?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
    }
}

/**
 * Enhanced exercise detail panel for large screens
 */
@Composable
fun EnhancedExerciseDetailPanel(
    exercise: LoggedExercise?,
    weightUnit: String,
    onSetUpdate: (String, String, String, Double?, String, String?, String?, String?, String?, String?) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, String) -> Unit,
    onStartRest: (String, String) -> Unit,
    onSetRestTime: (String, String, Int?) -> Unit,
    performanceSuggestion: PerformanceSuggestion?,
    demoVideoLink: String? = null,
    modifier: Modifier = Modifier
) {
    if (exercise == null) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select an exercise to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Calculate completion metrics
        val setsCompleted = exercise.sets.count { set -> 
            (set.weight != null && set.reps != null) || set.secs != null 
        }
        val totalSets = exercise.sets.size
        val completionPercentage = if (totalSets > 0) setsCompleted.toFloat() / totalSets else 0f
        
        // Animation for completion progress
        val animatedProgress by animateFloatAsState(
            targetValue = completionPercentage,
            animationSpec = spring(dampingRatio = 0.8f),
            label = "completion_progress"
        )
        
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ExerciseDemoChip(videoLink = demoVideoLink)
                    }
                    if (exercise.isSubstitute == true) {
                        val orig = exercise.originalExerciseName?.takeIf { it.isNotBlank() }
                        Text(
                            text = if (orig != null) "Substituted from $orig" else "Substituted Exercise",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                FilledTonalIconButton(
                    onClick = { onAddSet(exercise.id) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Add Set",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Progress bar and completion text
            if (totalSets > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sets completion text
                Text(
                    text = "$setsCompleted/$totalSets sets completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        completionPercentage >= 1.0f -> MaterialTheme.colorScheme.tertiary
                        completionPercentage >= 0.8f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            val nextUnfilledIndex = exercise.sets.indexOfFirst { s ->
                val performanceIn = (s.weight != null && s.reps != null) || s.secs != null
                !(performanceIn && s.rir != null)
            }

            // Sets list with enhanced spacing for large screens
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(workoutElementSpacing()),
                modifier = Modifier.fillMaxSize()
            ) {
                items(exercise.sets.size) { index ->
                    val set = exercise.sets[index]
                    
                    EnhancedSetRow(
                        setNumber = index + 1,
                        weightValue = set.weight?.toString() ?: "",
                        repsValue = set.reps?.toString() ?: "",
                        secsValue = set.secs?.toString() ?: "",
                        rirValue = set.rir?.toString() ?: "",
                        bandsValue = set.bands ?: "",
                        notesValue = set.notes ?: "",
                        videoReference = set.videoReference,
                        videoMarks = set.videoMarks,
                        restTimeSeconds = set.restTimeSeconds,
                        weightUnit = weightUnit,
                        showWeightReps = !set.targetReps.isNullOrBlank(),
                        showSecs = !set.targetSecs.isNullOrBlank(),
                        showDeleteButton = exercise.sets.size > 1,
                        performanceSuggestion = performanceSuggestion,
                        isLargeScreen = true, // Enable large screen optimizations
                        onWeightChange = { newWeight ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", newWeight.toDoubleOrNull(), set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, set.videoReference, set.videoMarks)
                        },
                        onRepsChange = { newReps ->
                            onSetUpdate(exercise.id, set.id, newReps, set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, set.videoReference, set.videoMarks)
                        },
                        onSecsChange = { newSecs ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, newSecs, set.rir?.toString(), set.bands, set.notes, set.videoReference, set.videoMarks)
                        },
                        onRirChange = { newRir ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", newRir, set.bands, set.notes, set.videoReference, set.videoMarks)
                        },
                        onBandsChange = { newBands ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), newBands, set.notes, set.videoReference, set.videoMarks)
                        },
                        onNotesChange = { newNotes ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, newNotes, set.videoReference, set.videoMarks)
                        },
                        onVideoSelected = { videoPath, marks ->
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, videoPath, marks)
                        },
                        onVideoRemoved = {
                            onSetUpdate(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, null, null)
                        },
                        onStartRest = { onStartRest(exercise.id, set.id) },
                        onDeleteSet = { onRemoveSet(exercise.id, set.id) },
                        onApplySuggestion = {
                            performanceSuggestion?.let { suggestion ->
                                val weightText = suggestion.suggestedWeight?.toString() ?: ""
                                val repsText = suggestion.suggestedReps?.toString() ?: ""
                                val secsText = suggestion.suggestedSecs?.toString() ?: ""
                                val rirText = suggestion.suggestedRir?.toString() ?: ""
                                onSetUpdate(exercise.id, set.id, repsText, weightText.toDoubleOrNull(), secsText, rirText, set.bands, set.notes, set.videoReference, set.videoMarks)
                            }
                        },
                        onCopyPreviousSet = exercise.sets.getOrNull(index - 1)
                            ?.takeIf { it.weight != null || it.reps != null || it.secs != null }
                            ?.let { prev ->
                                {
                                    onSetUpdate(
                                        exercise.id,
                                        set.id,
                                        prev.reps?.toString() ?: "",
                                        prev.weight,
                                        prev.secs?.toString() ?: "",
                                        prev.rir?.toString(),
                                        set.bands,
                                        set.notes,
                                        set.videoReference,
                                        set.videoMarks
                                    )
                                }
                            },
                        onClearSet = {
                            onSetUpdate(
                                exercise.id,
                                set.id,
                                "",
                                null,
                                "",
                                null,
                                set.bands,
                                set.notes,
                                set.videoReference,
                                set.videoMarks
                            )
                        },
                        onEditRestTime = { seconds -> onSetRestTime(exercise.id, set.id, seconds) },
                        onClearRestTime = { onSetRestTime(exercise.id, set.id, null) },
                        isNextUnfilled = index == nextUnfilledIndex
                    )
                }
            }
        }
    }
}

