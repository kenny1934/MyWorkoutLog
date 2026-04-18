@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun EditWorkoutScreen(
    workoutId: String,
    viewModel: WorkoutLoggerViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    weightUnit: String,
    onNavigateUp: () -> Unit
) {
    // LaunchedEffect to load workout for editing when the composable first appears
    LaunchedEffect(key1 = workoutId) {
        viewModel.loadWorkoutForEdit(workoutId)
    }

    // Reuse the WorkoutLoggerScreen UI but with edit mode context
    WorkoutLoggerScreenContent(
        viewModel = viewModel,
        activeCycleViewModel = activeCycleViewModel,
        weightUnit = weightUnit,
        onNavigateUp = onNavigateUp,
        isEditMode = true
    )
}

@Composable
fun WorkoutLoggerScreen(
    templateId: String,
    cycleId: String?,
    weekId: String?,
    sessionId: String?,
    viewModel: WorkoutLoggerViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    weightUnit: String,
    onNavigateUp: () -> Unit
) {
    // State for session choice dialog
    var showSessionDialog by remember { mutableStateOf(false) }
    var existingSession by remember { mutableStateOf<LoggedWorkout?>(null) }
    var sessionHoursAgo by remember { mutableStateOf(0) }

    // Check for existing session when screen opens
    LaunchedEffect(key1 = templateId) {
        try {
            val sessionStatus = viewModel.getSessionStatus(templateId)
            when (sessionStatus) {
                is WorkoutSessionStatus.None -> {
                    // No existing session, start new workout
                    viewModel.startWorkoutFromTemplate(templateId, cycleId, weekId, sessionId)
                }
                is WorkoutSessionStatus.InProgress -> {
                    // Found existing session, show choice dialog
                    existingSession = sessionStatus.workout
                    sessionHoursAgo = sessionStatus.hoursAgo
                    showSessionDialog = true
                }
            }
        } catch (e: Exception) {
            // If session check fails, fallback to starting new workout
            viewModel.startWorkoutFromTemplate(templateId, cycleId, weekId, sessionId)
        }
    }

    // Session choice dialog
    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = {
                showSessionDialog = false
                onNavigateUp() // Go back if user dismisses
            },
            title = { Text("Existing Workout Session") },
            text = {
                Text(
                    if (sessionHoursAgo == 0) {
                        "You have an in-progress workout session for this template from earlier today. Would you like to resume it or start fresh?"
                    } else {
                        "You have an in-progress workout session for this template from $sessionHoursAgo ${if (sessionHoursAgo == 1) "hour" else "hours"} ago. Would you like to resume it or start fresh?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSessionDialog = false
                        viewModel.resumeInProgressWorkout(templateId)
                    }
                ) {
                    Text("Resume Session")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSessionDialog = false
                        viewModel.startFreshWorkout(templateId, cycleId, weekId, sessionId)
                    }
                ) {
                    Text("Start Fresh")
                }
            }
        )
    }

    // Reuse the shared content with normal mode
    WorkoutLoggerScreenContent(
        viewModel = viewModel,
        activeCycleViewModel = activeCycleViewModel,
        weightUnit = weightUnit,
        onNavigateUp = onNavigateUp,
        isEditMode = false
    )
}

@Composable
private fun WorkoutLoggerScreenContent(
    viewModel: WorkoutLoggerViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    weightUnit: String,
    onNavigateUp: () -> Unit,
    isEditMode: Boolean
) {
    // Local state for the bodyweight text field
    var bodyweightText by remember { mutableStateOf("") }

    // Local state for session notes
    var sessionNotesText by remember { mutableStateOf("") }

    // State for exit confirmation dialog
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isFinishAction by remember { mutableStateOf(false) }

    // State for exercise addition dialog
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    // State for exercise context menu
    var showExerciseContextMenu by remember { mutableStateOf(false) }
    var selectedExerciseForMenu by remember { mutableStateOf<LoggedExercise?>(null) }

    // State for exercise substitution dialog
    var showSubstituteExerciseDialog by remember { mutableStateOf(false) }

    // State for exercise removal confirmation
    var showRemoveExerciseConfirmation by remember { mutableStateOf(false) }

    // State for duration edit dialog
    var showDurationEditDialog by remember { mutableStateOf(false) }

    // State for set removal confirmation
    var showRemoveSetConfirmation by remember { mutableStateOf(false) }
    var selectedSetForRemoval by remember { mutableStateOf<Pair<String, String>?>(null) } // exerciseId, setId

    // Coroutine scope for handling async save operations
    val coroutineScope = rememberCoroutineScope()

    // Function to save all pending field data before navigation/completion
    suspend fun saveAllPendingData() {
        // Save bodyweight if changed
        viewModel.updateBodyweight(bodyweightText)
        // Save session notes if changed
        viewModel.updateOverallComments(sessionNotesText)
        // Give a brief moment for any active typing to be captured by debounced saves
        kotlinx.coroutines.delay(100)
        // Note: Individual set data is automatically saved via DisposableEffect and debounced LaunchedEffect
    }

    // Function to show exit confirmation dialog
    fun showExitConfirmationDialog(action: () -> Unit, isFinish: Boolean = false) {
        exitAction = action
        isFinishAction = isFinish
        showExitConfirmation = true
    }

    // Debounced auto-save for bodyweight field
    LaunchedEffect(bodyweightText) {
        if (bodyweightText.isNotBlank()) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            viewModel.updateBodyweight(bodyweightText)
        }
    }

    // Debounced auto-save for session notes field
    LaunchedEffect(sessionNotesText) {
        if (sessionNotesText.isNotBlank()) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            viewModel.updateOverallComments(sessionNotesText)
        }
    }

    // Collect the active workout state from the ViewModel.
    val activeWorkout by viewModel.activeWorkoutState.collectAsStateWithLifecycle()
    val activeCycle by activeCycleViewModel.activeCycle.collectAsStateWithLifecycle()

    // Match this workout's week against the cycle snapshot so we can surface the
    // week's target RIR and deload flag at the top of the logger.
    val currentCycleWeek: ProgramWeekDefinition? = remember(
        activeWorkout?.programWeekDefinitionId,
        activeWorkout?.activeProgramCycleId,
        activeCycle?.cycleUuid,
    ) {
        val weekId = activeWorkout?.programWeekDefinitionId ?: return@remember null
        val workoutCycleId = activeWorkout?.activeProgramCycleId ?: return@remember null
        val cycle = activeCycle ?: return@remember null
        if (cycle.cycleUuid != workoutCycleId) return@remember null
        cycle.cycleProgram.weeks.firstOrNull { it.id == weekId }
    }

    // Get the timer state from the ViewModel
    val timerIsRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerValue by viewModel.timerValueSeconds.collectAsStateWithLifecycle()

    // Initialize bodyweight field from loaded workout data (for edit mode)
    LaunchedEffect(activeWorkout?.bodyweight) {
        val workout = activeWorkout
        if (workout?.bodyweight != null && bodyweightText.isEmpty()) {
            bodyweightText = workout.bodyweight.toString()
        }
    }

    // Initialize session notes field from loaded workout data (for edit mode)
    LaunchedEffect(activeWorkout?.overallComments) {
        val workout = activeWorkout
        if (workout?.overallComments != null && sessionNotesText.isEmpty()) {
            sessionNotesText = workout.overallComments
        }
    }
    val sessionElapsedTime by viewModel.sessionElapsedTime.collectAsStateWithLifecycle()

    // Handle system back gesture
    BackHandler {
        showExitConfirmationDialog({
            coroutineScope.launch {
                saveAllPendingData()
                viewModel.stopRestTimer()
                onNavigateUp()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Edit: ${activeWorkout?.name ?: "Workout"}" else activeWorkout?.name ?: "Log Workout")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        showExitConfirmationDialog({
                            coroutineScope.launch {
                                saveAllPendingData()
                                viewModel.stopRestTimer()
                                onNavigateUp()
                            }
                        })
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show timer in regular mode, show clickable duration in edit mode
                    if (!isEditMode) {
                        Text(
                            text = "${sessionElapsedTime / 60}:${String.format("%02d", sessionElapsedTime % 60)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    } else if (sessionElapsedTime >= 0) {
                        // In edit mode, show original duration and make it clickable
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showDurationEditDialog = true }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${sessionElapsedTime / 60}:${String.format("%02d", sessionElapsedTime % 60)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Duration",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(onClick = {
                        showExitConfirmationDialog({
                            coroutineScope.launch {
                                saveAllPendingData()
                                viewModel.finishWorkout(weightUnit, activeCycle)
                                onNavigateUp()
                            }
                        }, isFinish = true)
                    }) {
                        Text(if (isEditMode) "Save Changes" else "Finish")
                    }
                }
            )
        },

        floatingActionButton = {
            // Only show FAB for compact screens (large screens use navigation rail)
            if (!shouldUseWorkoutMasterDetail()) {
                FloatingActionButton(
                    onClick = { showAddExerciseDialog = true },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Exercise",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },

        bottomBar = {
            EnhancedTimerBar(
                isRunning = timerIsRunning,
                currentTime = timerValue,
                targetTime = 120, // 2 minutes default rest time
                onPause = { viewModel.pauseRestTimer() },
                onResume = { viewModel.resumeRestTimer() },
                onStop = { viewModel.stopRestTimer() },
                onReset = { viewModel.resetRestTimer() },
                onAddTime = { viewModel.addTimeToRestTimer(15) }
            )
        },
        // MainActivity's outer Scaffold already consumes the bottom system-nav inset
        // via AppBottomNavigationBar. Without zeroing here, this nested Scaffold
        // double-reserves that inset and clips the last set off the bottom of the
        // scroll area.
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        // If the workout is not loaded yet, show a loading indicator.
        if (activeWorkout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // State for master-detail navigation
            var selectedExerciseId by remember { mutableStateOf<String?>(activeWorkout!!.loggedExercises.firstOrNull()?.id) }

            // Use adaptive layout
            AdaptiveWorkoutLayout(
                modifier = Modifier.fillMaxSize()
            ) { useMasterDetail ->
                if (useMasterDetail) {
                    // Large screen: Master-detail layout
                    val masterDetailBannerWeek = currentCycleWeek?.takeIf { week ->
                        week.isDeloadWeek || !week.targetRir.isNullOrBlank()
                    }
                    MasterDetailWorkoutView(
                        activeWorkout = activeWorkout,
                        exerciseList = activeWorkout!!.loggedExercises,
                        selectedExerciseId = selectedExerciseId,
                        onExerciseSelected = { exerciseId -> selectedExerciseId = exerciseId },
                        contextBanner = masterDetailBannerWeek?.let { week ->
                            {
                                CycleContextBanner(
                                    weekLabel = week.weekLabel,
                                    isDeloadWeek = week.isDeloadWeek,
                                    targetRir = week.targetRir?.takeIf { it.isNotBlank() },
                                )
                            }
                        },
                        sessionContent = {
                            CompactSessionInfo(
                                bodyweightText = bodyweightText,
                                onBodyweightChange = { newText ->
                                    if (newText.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                        bodyweightText = newText
                                    }
                                },
                                sessionNotesText = sessionNotesText,
                                onSessionNotesChange = { sessionNotesText = it },
                                weightUnit = weightUnit
                            )
                        },
                        selectedExerciseContent = {
                            val selectedExercise = activeWorkout!!.loggedExercises.find { it.id == selectedExerciseId }
                            EnhancedExerciseDetailPanel(
                                exercise = selectedExercise,
                                weightUnit = weightUnit,
                                onSetUpdate = { exerciseId, setId, repsText, weight, secsText, rirText, bands, notes, videoPath ->
                                    viewModel.updateSet(exerciseId, setId, repsText, weight, secsText, rirText, bands, notes, videoPath)
                                },
                                onAddSet = { exerciseId -> viewModel.addSetToExercise(exerciseId) },
                                onRemoveSet = { exerciseId, setId -> viewModel.removeSetFromExercise(exerciseId, setId) },
                                onStartRest = { exerciseId, setId -> viewModel.startRestTimerForSet(exerciseId, setId) },
                                performanceSuggestion = selectedExercise?.let { viewModel.getPerformanceSuggestion(it.exerciseId) }
                            )
                        },
                        navigationRail = {
                            WorkoutNavigationRail(
                                onAddExercise = { showAddExerciseDialog = true },
                                onStartRestTimer = { viewModel.startRestTimerForSet("", "") },
                                onFinishWorkout = {
                                    showExitConfirmationDialog({
                                        coroutineScope.launch {
                                            saveAllPendingData()
                                            viewModel.finishWorkout(weightUnit, activeCycle)
                                            onNavigateUp()
                                        }
                                    }, isFinish = true)
                                },
                                timerIsRunning = timerIsRunning,
                                sessionElapsedTime = sessionElapsedTime
                            )
                        },
                        paddingValues = paddingValues
                    )
                } else {
                    // Compact screen: Traditional single-column layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // Cycle context banner (deload / target RIR) — only when this workout
                // belongs to an active cycle whose week has one of those fields set.
                currentCycleWeek?.let { week ->
                    val rir = week.targetRir?.takeIf { it.isNotBlank() }
                    if (week.isDeloadWeek || rir != null) {
                        item {
                            CycleContextBanner(
                                weekLabel = week.weekLabel,
                                isDeloadWeek = week.isDeloadWeek,
                                targetRir = rir,
                            )
                        }
                    }
                }
                // Enhanced bodyweight input section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Today's Session",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            EnhancedStepperInputField(
                                value = bodyweightText,
                                onValueChange = { newText ->
                                    if (newText.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                        bodyweightText = newText
                                    }
                                },
                                label = "Your Bodyweight",
                                unit = weightUnit,
                                step = 0.5,
                                minValue = 30.0,
                                maxValue = 300.0,
                                decimalPlaces = 1,
                                onFocusChanged = { isFocused ->
                                    if (!isFocused) {
                                        viewModel.updateBodyweight(bodyweightText)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Session notes input
                            EnhancedWorkoutInputField(
                                value = sessionNotesText,
                                onValueChange = { newText ->
                                    sessionNotesText = newText
                                },
                                label = "Session Notes",
                                placeholder = "How are you feeling today? Any observations?",
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                                onFocusChanged = { isFocused ->
                                    if (!isFocused) {
                                        viewModel.updateOverallComments(sessionNotesText)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                items(activeWorkout!!.loggedExercises) { exercise ->
                    // Calculate completion metrics for enhanced display
                    val setsCompleted = exercise.sets.count { set ->
                        (set.weight != null && set.reps != null) || set.secs != null
                    }
                    val totalSets = exercise.sets.size

                    // Get last performance info (placeholder for now)
                    val lastPerformance = null // TODO: Implement based on available data

                    EnhancedExerciseCard(
                        exerciseName = exercise.exerciseName,
                        isSubstitute = exercise.isSubstitute == true,
                        setsCompleted = setsCompleted,
                        totalSets = totalSets,
                        lastPerformance = lastPerformance,
                        onAddSet = { viewModel.addSetToExercise(exercise.id) },
                        onLongClick = {
                            selectedExerciseForMenu = exercise
                            showExerciseContextMenu = true
                        }
                    ) {
                        // Enhanced set rows
                        exercise.sets.forEachIndexed { index, set ->
                            // Get performance suggestion for this exercise
                            val performanceSuggestion = viewModel.getPerformanceSuggestion(exercise.exerciseId)

                            EnhancedSetRow(
                                setNumber = index + 1,
                                weightValue = set.weight?.toString() ?: "",
                                repsValue = set.reps?.toString() ?: "",
                                secsValue = set.secs?.toString() ?: "",
                                rirValue = set.rir?.toString() ?: "",
                                bandsValue = set.bands ?: "",
                                notesValue = set.notes ?: "",
                                videoReference = set.videoReference,
                                restTimeSeconds = set.restTimeSeconds,
                                weightUnit = weightUnit,
                                showWeightReps = !set.targetReps.isNullOrBlank(),
                                showSecs = !set.targetSecs.isNullOrBlank(),
                                showDeleteButton = exercise.sets.size > 1, // Only show delete if more than 1 set
                                performanceSuggestion = performanceSuggestion,
                                onWeightChange = { newWeight ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", newWeight.toDoubleOrNull(), set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, set.videoReference)
                                },
                                onRepsChange = { newReps ->
                                    viewModel.updateSet(exercise.id, set.id, newReps, set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, set.videoReference)
                                },
                                onSecsChange = { newSecs ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, newSecs, set.rir?.toString(), set.bands, set.notes, set.videoReference)
                                },
                                onRirChange = { newRir ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", newRir, set.bands, set.notes, set.videoReference)
                                },
                                onBandsChange = { newBands ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), newBands, set.notes, set.videoReference)
                                },
                                onNotesChange = { newNotes ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, newNotes, set.videoReference)
                                },
                                onVideoSelected = { videoPath ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, videoPath)
                                },
                                onVideoRemoved = {
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes, null)
                                },
                                onStartRest = { viewModel.startRestTimerForSet(exercise.id, set.id) },
                                onDeleteSet = {
                                    selectedSetForRemoval = Pair(exercise.id, set.id)
                                    showRemoveSetConfirmation = true
                                },
                                onApplySuggestion = {
                                    // Apply suggestions from performance data
                                    performanceSuggestion?.let { suggestion ->
                                        val weightText = suggestion.suggestedWeight?.toString() ?: ""
                                        val repsText = suggestion.suggestedReps?.toString() ?: ""
                                        val secsText = suggestion.suggestedSecs?.toString() ?: ""
                                        val rirText = suggestion.suggestedRir?.toString() ?: ""
                                        viewModel.updateSet(exercise.id, set.id, repsText, weightText.toDoubleOrNull(), secsText, rirText, set.bands, set.notes, set.videoReference)
                                    }
                                },
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
                    }
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Text(if (isFinishAction) {
                    if (isEditMode) "Confirm Save Changes" else "Confirm Finish"
                } else {
                    if (isEditMode) "Confirm Exit" else "Confirm Exit"
                })
            },
            text = {
                Text(if (isFinishAction) {
                    if (isEditMode) "Are you sure you want to save these changes to the workout?"
                    else "Are you sure you want to finish this workout? All progress will be saved."
                } else {
                    if (isEditMode) "Are you sure you want to exit without saving your changes?"
                    else "Are you sure you want to exit this workout? All progress will be saved."
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        exitAction?.invoke()
                    }
                ) {
                    Text(if (isFinishAction) {
                        if (isEditMode) "Save Changes" else "Finish"
                    } else {
                        "Exit"
                    })
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddExerciseToWorkoutDialog(
            viewModel = viewModel,
            onDismiss = { showAddExerciseDialog = false },
            onExerciseAdded = { showAddExerciseDialog = false }
        )
    }

    // Exercise Context Menu Dialog
    if (showExerciseContextMenu && selectedExerciseForMenu != null) {
        ExerciseContextMenuDialog(
            exercise = selectedExerciseForMenu!!,
            onDismiss = {
                showExerciseContextMenu = false
                selectedExerciseForMenu = null
            },
            onSubstitute = {
                showExerciseContextMenu = false
                showSubstituteExerciseDialog = true
            },
            onRemove = {
                showExerciseContextMenu = false
                showRemoveExerciseConfirmation = true
            }
        )
    }

    // Substitute Exercise Dialog
    if (showSubstituteExerciseDialog && selectedExerciseForMenu != null) {
        SubstituteExerciseDialog(
            viewModel = viewModel,
            currentExercise = selectedExerciseForMenu!!,
            onDismiss = {
                showSubstituteExerciseDialog = false
                selectedExerciseForMenu = null
            },
            onExerciseSubstituted = {
                showSubstituteExerciseDialog = false
                selectedExerciseForMenu = null
            }
        )
    }

    // Remove Exercise Confirmation Dialog
    if (showRemoveExerciseConfirmation && selectedExerciseForMenu != null) {
        AlertDialog(
            onDismissRequest = {
                showRemoveExerciseConfirmation = false
                selectedExerciseForMenu = null
            },
            title = { Text("Remove Exercise") },
            text = {
                Text("Are you sure you want to remove \"${selectedExerciseForMenu!!.exerciseName}\" from this workout? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeExerciseFromWorkout(selectedExerciseForMenu!!.id)
                        showRemoveExerciseConfirmation = false
                        selectedExerciseForMenu = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveExerciseConfirmation = false
                        selectedExerciseForMenu = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Set Confirmation Dialog
    if (showRemoveSetConfirmation && selectedSetForRemoval != null) {
        AlertDialog(
            onDismissRequest = {
                showRemoveSetConfirmation = false
                selectedSetForRemoval = null
            },
            title = { Text("Remove Set") },
            text = {
                Text("Are you sure you want to remove this set? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedSetForRemoval?.let { (exerciseId, setId) ->
                            viewModel.removeSetFromExercise(exerciseId, setId)
                        }
                        showRemoveSetConfirmation = false
                        selectedSetForRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveSetConfirmation = false
                        selectedSetForRemoval = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duration Edit Dialog
    DurationEditDialog(
        isVisible = showDurationEditDialog,
        currentDurationSeconds = if (sessionElapsedTime >= 0) sessionElapsedTime else 0,
        onDismiss = { showDurationEditDialog = false },
        onConfirm = { newDurationSeconds ->
            viewModel.updateWorkoutDuration(newDurationSeconds)
            showDurationEditDialog = false
        }
    )
}

@Composable
private fun CycleContextBanner(
    weekLabel: String,
    isDeloadWeek: Boolean,
    targetRir: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (isDeloadWeek) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = "Deload",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (targetRir != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "RIR $targetRir",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
