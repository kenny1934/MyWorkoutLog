@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.myworkoutlog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // LaunchedEffect runs a coroutine when the composable first appears.
    LaunchedEffect(key1 = templateId) {
        viewModel.startWorkoutFromTemplate(templateId, cycleId, weekId, sessionId)
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
    
    // State for set removal confirmation
    var showRemoveSetConfirmation by remember { mutableStateOf(false) }
    var selectedSetForRemoval by remember { mutableStateOf<Pair<String, String>?>(null) } // exerciseId, setId
    
    // Coroutine scope for handling async save operations
    val coroutineScope = rememberCoroutineScope()
    
    // Function to save all pending field data before navigation/completion
    suspend fun saveAllPendingData() {
        // Save bodyweight if changed
        viewModel.updateBodyweight(bodyweightText)
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

    // Collect the active workout state from the ViewModel.
    val activeWorkout by viewModel.activeWorkoutState.collectAsStateWithLifecycle()
    val activeCycle by activeCycleViewModel.activeCycle.collectAsStateWithLifecycle()

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
                    if (!isEditMode) {
                        Text(
                            text = formatTime(sessionElapsedTime),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Exercise")
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
        }
    ) { paddingValues ->
        // If the workout is not loaded yet, show a loading indicator.
        if (activeWorkout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Once loaded, display the list of exercises.
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            EnhancedSetRow(
                                setNumber = index + 1,
                                weightValue = set.weight?.toString() ?: "",
                                repsValue = set.reps?.toString() ?: "",
                                secsValue = set.secs?.toString() ?: "",
                                rirValue = set.rir?.toString() ?: "",
                                weightUnit = weightUnit,
                                showWeightReps = !set.targetReps.isNullOrBlank(),
                                showSecs = !set.targetSecs.isNullOrBlank(),
                                onWeightChange = { newWeight ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", newWeight.toDoubleOrNull(), set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes)
                                },
                                onRepsChange = { newReps ->
                                    viewModel.updateSet(exercise.id, set.id, newReps, set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes)
                                },
                                onSecsChange = { newSecs ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, newSecs, set.rir?.toString(), set.bands, set.notes)
                                },
                                onRirChange = { newRir ->
                                    viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", newRir, set.bands, set.notes)
                                },
                                onStartRest = { viewModel.startRestTimer() },
                                onDeleteSet = {
                                    selectedSetForRemoval = Pair(exercise.id, set.id)
                                    showRemoveSetConfirmation = true
                                },
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
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
}

@Composable
fun LoggedSetRow(
    set: LoggedSet,
    setNumber: Int,
    weightUnit: String,
    showDeleteButton: Boolean = false,
    exerciseId: String,
    viewModel: WorkoutLoggerViewModel,
    onRepsChange: (String) -> Unit,
    onWeightChange: (Double?) -> Unit,
    onSecsChange: (String) -> Unit,
    onRirChange: (String) -> Unit = {},
    onBandsChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onStartRest: () -> Unit,
    onDeleteSet: () -> Unit = {},
    onSetUpdate: (String, Double?, String, String, String, String) -> Unit
) {
    // Determine which fields to show based on the template's targets for this set
    val showWeightReps = !set.targetReps.isNullOrBlank()
    val showSecs = !set.targetSecs.isNullOrBlank()
    
    // Get performance suggestion for this exercise
    val performanceSuggestion = viewModel.getPerformanceSuggestion(exerciseId)

    // Local state holds the text as the user types it.
    var weightText by remember { mutableStateOf(set.weight?.toString() ?: "") }
    var repsText by remember { mutableStateOf(set.reps?.toString() ?: "") }
    var secsText by remember { mutableStateOf(set.secs?.toString() ?: "") }
    var rirText by remember { mutableStateOf(set.rir?.toString() ?: "") }
    var bandsText by remember { mutableStateOf(set.bands ?: "") }
    var notesText by remember { mutableStateOf(set.notes ?: "") }
    var showNotes by remember { mutableStateOf(false) }
    
    // Debounced auto-save for critical fields to handle active typing scenario
    LaunchedEffect(notesText) {
        if (notesText != (set.notes ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onNotesChange(notesText)
        }
    }
    
    LaunchedEffect(bandsText) {
        if (bandsText != (set.bands ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onBandsChange(bandsText)
        }
    }
    
    LaunchedEffect(rirText) {
        if (rirText != (set.rir?.toString() ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onRirChange(rirText)
        }
    }
    
    // Debounced auto-save for core fields as well
    LaunchedEffect(weightText) {
        if (weightText != (set.weight?.toString() ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onWeightChange(weightText.toDoubleOrNull())
        }
    }
    
    LaunchedEffect(repsText) {
        if (repsText != (set.reps?.toString() ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onRepsChange(repsText)
        }
    }
    
    LaunchedEffect(secsText) {
        if (secsText != (set.secs?.toString() ?: "")) {
            kotlinx.coroutines.delay(1000) // 1 second debounce
            onSecsChange(secsText)
        }
    }

    // Function to save all current field values
    fun saveAllCurrentValues() {
        onSetUpdate(repsText, weightText.toDoubleOrNull(), secsText, rirText, bandsText, notesText)
    }

    // Save current values when component is disposed (e.g., when navigating away)
    DisposableEffect(Unit) {
        onDispose {
            saveAllCurrentValues()
        }
    }

    // This ensures if the underlying data changes from elsewhere, our text fields update.
    LaunchedEffect(set.weight) {
        val currentWeightString = set.weight?.toString() ?: ""
        if (weightText != currentWeightString) {
            weightText = currentWeightString
        }
    }
    LaunchedEffect(set.reps) {
        val currentRepsString = set.reps?.toString() ?: ""
        if (repsText != currentRepsString) {
            repsText = currentRepsString
        }
    }
    LaunchedEffect(set.secs) {
        val currentSecsString = set.secs?.toString() ?: ""
        if (secsText != currentSecsString) {
            secsText = currentSecsString
        }
    }
    LaunchedEffect(set.rir) {
        val currentRirString = set.rir?.toString() ?: ""
        if (rirText != currentRirString) {
            rirText = currentRirString
        }
    }
    LaunchedEffect(set.bands) {
        val currentBandsString = set.bands ?: ""
        if (bandsText != currentBandsString) {
            bandsText = currentBandsString
        }
    }
    LaunchedEffect(set.notes) {
        val currentNotesString = set.notes ?: ""
        if (notesText != currentNotesString) {
            notesText = currentNotesString
        }
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Set $setNumber",
                modifier = Modifier.width(60.dp),
                fontWeight = FontWeight.Bold
            )
            
            // Pre-fill suggestion chip
            if (performanceSuggestion != null && performanceSuggestion.confidence > 0.3f && 
                set.weight == null && set.reps == null) { // Only show for empty sets
                AssistChip(
                    onClick = {
                        // Apply suggestions: Update local state first, then trigger saves
                        performanceSuggestion.suggestedWeight?.let { weight ->
                            weightText = weight.toString()
                        }
                        performanceSuggestion.suggestedReps?.let { reps ->
                            repsText = reps.toString()
                        }
                        performanceSuggestion.suggestedRir?.let { rir ->
                            rirText = rir.toString()
                        }
                        
                        // Save all current values immediately after updating UI state
                        saveAllCurrentValues()
                    },
                    label = { 
                        Text(
                            text = buildString {
                                performanceSuggestion.suggestedWeight?.let { append("${it}kg ") }
                                performanceSuggestion.suggestedReps?.let { append("${it}r ") }
                                performanceSuggestion.daysAgo?.let { 
                                    append("(${it}d ago)")
                                }
                            }.trim(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Smart suggestion",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Row {
                // Delete button (only show if more than 1 set)
                if (showDeleteButton) {
                    IconButton(onClick = onDeleteSet) {
                        Icon(
                            Icons.Filled.Remove, 
                            contentDescription = "Remove Set",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                // Start rest timer button
                if (set.reps != null || set.secs != null) {
                    IconButton(onClick = { 
                        // Save current values when starting rest timer
                        saveAllCurrentValues()
                        onStartRest() 
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start Rest Timer")
                    }
                }
            }
        }

        // Enhanced input fields with better visual design
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Weight input field
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { newText ->
                        // Allow only digits and one decimal point
                        if (newText.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            weightText = newText
                        }
                    },
                    label = { Text("Weight ($weightUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                onWeightChange(weightText.toDoubleOrNull())
                            }
                        }
                )

                if (showWeightReps) {
                    // Reps input field
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { newText ->
                            if (newText.all { it.isDigit() }) {
                                repsText = newText
                            }
                        },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    onRepsChange(repsText)
                                }
                            }
                    )
                }

                // Time input for time-based exercises
                if (showSecs) {
                    OutlinedTextField(
                        value = secsText,
                        onValueChange = { newText ->
                            if (newText.all { it.isDigit() }) {
                                secsText = newText
                            }
                        },
                        label = { Text("Secs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    onSecsChange(secsText)
                                }
                            }
                    )
                }
            }
        }

        // RIR and Bands row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RIR field (0-10)
            OutlinedTextField(
                value = rirText,
                onValueChange = { newText ->
                    if (newText.isEmpty() || (newText.all { it.isDigit() } && newText.toIntOrNull()?.let { it <= 10 } == true)) {
                        rirText = newText
                    }
                },
                label = { Text("RIR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onRirChange(rirText)
                        }
                    }
            )

            // Bands field
            OutlinedTextField(
                value = bandsText,
                onValueChange = { bandsText = it },
                label = { Text("Bands") },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onBandsChange(bandsText)
                        }
                    }
            )
        }

        // Notes toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { 
                    // Save current values when toggling notes visibility
                    if (showNotes) {
                        // If collapsing notes, save all current data
                        saveAllCurrentValues()
                    }
                    showNotes = !showNotes 
                }
            ) {
                Icon(
                    imageVector = if (showNotes) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (showNotes) "Hide Notes" else "Show Notes"
                )
            }
        }

        // Expandable notes field
        AnimatedVisibility(visible = showNotes) {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onNotesChange(notesText)
                        }
                    },
                minLines = 2
            )
        }
    }
}

@Composable
fun TimerBar(
    isRunning: Boolean,
    currentTime: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit
) {
    AnimatedVisibility(visible = isRunning || currentTime > 0) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentTime),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Dynamic Play/Pause Button
                IconButton(onClick = { if (isRunning) onPause() else onResume() }) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.PauseCircle else Icons.Filled.PlayArrow,
                        contentDescription = if (isRunning) "Pause Timer" else "Resume Timer",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Button(onClick = onAddTime) { Text("+15s") }

                IconButton(onClick = onReset) {
                    Icon(Icons.Filled.Replay, contentDescription = "Reset Timer")
                }

                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop Timer")
                }
            }
        }
    }
}

@Composable
fun AddExerciseToWorkoutDialog(
    viewModel: WorkoutLoggerViewModel,
    onDismiss: () -> Unit,
    onExerciseAdded: () -> Unit
) {
    // State for exercise selection
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var numberOfSets by remember { mutableStateOf("3") }
    var showExerciseSelector by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (showExerciseSelector) "Select Exercise" else "Configure Sets")
        },
        text = {
            if (showExerciseSelector) {
                ExerciseSelectorContent(
                    viewModel = viewModel,
                    onExerciseSelected = { exercise ->
                        selectedExercise = exercise
                        showExerciseSelector = false
                    }
                )
            } else {
                Column {
                    Text("Exercise: ${selectedExercise?.name}")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = numberOfSets,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.isNotEmpty()) {
                                numberOfSets = newValue
                            }
                        },
                        label = { Text("Number of Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showExerciseSelector) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = {
                        selectedExercise?.let { exercise ->
                            val sets = numberOfSets.toIntOrNull() ?: 3
                            viewModel.addExerciseToWorkout(exercise.id, sets)
                            onExerciseAdded()
                        }
                    },
                    enabled = selectedExercise != null
                ) {
                    Text("Add Exercise")
                }
            }
        },
        dismissButton = {
            if (!showExerciseSelector) {
                TextButton(
                    onClick = { 
                        showExerciseSelector = true
                        selectedExercise = null
                    }
                ) {
                    Text("Back")
                }
            }
        }
    )
}

@Composable
fun ExerciseSelectorContent(
    viewModel: WorkoutLoggerViewModel,
    onExerciseSelected: (Exercise) -> Unit
) {
    val exercises by viewModel.allExercises.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search exercises...") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filteredExercises = exercises.filter { exercise ->
                exercise.name.contains(searchText, ignoreCase = true)
            }
            
            items(filteredExercises) { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = exercise.targetMuscleGroups.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseContextMenuDialog(
    exercise: LoggedExercise,
    onDismiss: () -> Unit,
    onSubstitute: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise Options") },
        text = { 
            Column {
                Text("Choose an action for \"${exercise.exerciseName}\"")
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onSubstitute,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Substitute Exercise")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Exercise")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        dismissButton = null
    )
}

@Composable
fun SubstituteExerciseDialog(
    viewModel: WorkoutLoggerViewModel,
    currentExercise: LoggedExercise,
    onDismiss: () -> Unit,
    onExerciseSubstituted: () -> Unit
) {
    // State for exercise selection
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExerciseSelector by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (showExerciseSelector) "Select Substitute" else "Confirm Substitution")
        },
        text = {
            if (showExerciseSelector) {
                Column {
                    Text("Substituting: ${currentExercise.exerciseName}")
                    Spacer(modifier = Modifier.height(16.dp))
                    ExerciseSelectorContent(
                        viewModel = viewModel,
                        onExerciseSelected = { exercise ->
                            selectedExercise = exercise
                            showExerciseSelector = false
                        }
                    )
                }
            } else {
                Column {
                    Text("Current: ${currentExercise.exerciseName}")
                    Text("Substitute with: ${selectedExercise?.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will replace the exercise while keeping all current set data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (showExerciseSelector) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = {
                        selectedExercise?.let { newExercise ->
                            viewModel.substituteExercise(currentExercise.id, newExercise.id)
                            onExerciseSubstituted()
                        }
                    },
                    enabled = selectedExercise != null
                ) {
                    Text("Substitute")
                }
            }
        },
        dismissButton = {
            if (!showExerciseSelector) {
                TextButton(
                    onClick = { 
                        showExerciseSelector = true
                        selectedExercise = null
                    }
                ) {
                    Text("Back")
                }
            }
        }
    )
}

// A helper function to format seconds into MM:SS format
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}