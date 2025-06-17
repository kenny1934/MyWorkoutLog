@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
    // Local state for the bodyweight text field
    var bodyweightText by remember { mutableStateOf("") }
    
    // State for exit confirmation dialog
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isFinishAction by remember { mutableStateOf(false) }
    
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

    // LaunchedEffect runs a coroutine when the composable first appears.
    LaunchedEffect(key1 = templateId) {
        viewModel.startWorkoutFromTemplate(templateId, cycleId, weekId, sessionId)
    }

    // Collect the active workout state from the ViewModel.
    val activeWorkout by viewModel.activeWorkoutState.collectAsStateWithLifecycle()
    val activeCycle by activeCycleViewModel.activeCycle.collectAsStateWithLifecycle()

    // Get the timer state from the ViewModel
    val timerIsRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerValue by viewModel.timerValueSeconds.collectAsStateWithLifecycle()
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
                title = { Text(activeWorkout?.name ?: "Log Workout") },
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
                    Text(
                        text = formatTime(sessionElapsedTime),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Button(onClick = {
                        showExitConfirmationDialog({
                            coroutineScope.launch {
                                saveAllPendingData()
                                viewModel.finishWorkout(weightUnit, activeCycle)
                                onNavigateUp()
                            }
                        }, isFinish = true)
                    }) {
                        Text("Finish")
                    }
                }
            )
        },

        bottomBar = {
            TimerBar(
                isRunning = timerIsRunning,
                currentTime = timerValue,
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
                // Add a bodyweight input field as the first item in the list
                item {
                    OutlinedTextField(
                        value = bodyweightText,
                        onValueChange = { newText ->
                            if (newText.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                bodyweightText = newText
                            }
                        },
                        label = { Text("Bodyweight ($weightUnit)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    viewModel.updateBodyweight(bodyweightText)
                                }
                            }
                    )
                }
                items(activeWorkout!!.loggedExercises) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(exercise.exerciseName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // For each exercise, display its sets
                            exercise.sets.forEachIndexed { index, set ->
                                LoggedSetRow(
                                    set = set,
                                    setNumber = index + 1,
                                    weightUnit = weightUnit,
                                    onRepsChange = { newReps ->
                                        viewModel.updateSet(exercise.id, set.id, newReps, set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes)
                                    },
                                    onWeightChange = { newWeight ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", newWeight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, set.notes)
                                    },
                                    onSecsChange = { newSecs ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, newSecs, set.rir?.toString(), set.bands, set.notes)
                                    },
                                    onRirChange = { newRir ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", newRir, set.bands, set.notes)
                                    },
                                    onBandsChange = { newBands ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), newBands, set.notes)
                                    },
                                    onNotesChange = { newNotes ->
                                        viewModel.updateSet(exercise.id, set.id, set.reps?.toString() ?: "", set.weight, set.secs?.toString() ?: "", set.rir?.toString(), set.bands, newNotes)
                                    },
                                    onStartRest = { viewModel.startRestTimer() }
                                )
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
                Text(if (isFinishAction) "Confirm Finish" else "Confirm Exit") 
            },
            text = { 
                Text(if (isFinishAction) 
                    "Are you sure you want to finish this workout? All progress will be saved." 
                else 
                    "Are you sure you want to exit this workout? All progress will be saved."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        exitAction?.invoke()
                    }
                ) {
                    Text(if (isFinishAction) "Finish" else "Exit")
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
}

@Composable
fun LoggedSetRow(
    set: LoggedSet,
    setNumber: Int,
    weightUnit: String,
    onRepsChange: (String) -> Unit,
    onWeightChange: (Double?) -> Unit,
    onSecsChange: (String) -> Unit,
    onRirChange: (String) -> Unit = {},
    onBandsChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onStartRest: () -> Unit
) {
    // Determine which fields to show based on the template's targets for this set
    val showWeightReps = !set.targetReps.isNullOrBlank()
    val showSecs = !set.targetSecs.isNullOrBlank()

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
        onRepsChange(repsText)
        onWeightChange(weightText.toDoubleOrNull())
        onSecsChange(secsText)
        onRirChange(rirText)
        onBandsChange(bandsText)
        onNotesChange(notesText)
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
            Spacer(modifier = Modifier.weight(1f))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text field for weight
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
                // Text field for reps
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

            // This TextField will only appear for time-based exercises
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

        // Second row for RIR and Bands
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

            // Notes toggle button
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

// A helper function to format seconds into MM:SS format
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}