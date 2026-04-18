@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
