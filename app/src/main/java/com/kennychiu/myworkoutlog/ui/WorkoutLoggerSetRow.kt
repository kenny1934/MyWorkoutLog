@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    // Get performance suggestion for this exercise (scheme-aware when the template has
    // a progression scheme configured; falls back to legacy "maintain last" otherwise).
    val performanceSuggestion = viewModel.getChipSuggestion(exerciseId, setNumber)

    // Keyboard flow: Weight → Reps → Secs → RIR → Done.
    val focusManager = LocalFocusManager.current

    // Each field is backed by a debounced text state: local edits flush to the
    // callback after 1s of quiet, and external changes to the underlying set
    // sync back into the local text. See rememberDebouncedField below.
    var weightText by rememberDebouncedField(
        source = set.weight?.toString() ?: "",
        onDebouncedChange = { onWeightChange(it.toDoubleOrNull()) },
    )
    var repsText by rememberDebouncedField(
        source = set.reps?.toString() ?: "",
        onDebouncedChange = onRepsChange,
    )
    var secsText by rememberDebouncedField(
        source = set.secs?.toString() ?: "",
        onDebouncedChange = onSecsChange,
    )
    var rirText by rememberDebouncedField(
        source = set.rir?.toString() ?: "",
        onDebouncedChange = onRirChange,
    )
    var bandsText by rememberDebouncedField(
        source = set.bands ?: "",
        onDebouncedChange = onBandsChange,
    )
    var notesText by rememberDebouncedField(
        source = set.notes ?: "",
        onDebouncedChange = onNotesChange,
    )
    var showNotes by remember { mutableStateOf(false) }

    fun saveAllCurrentValues() {
        onSetUpdate(repsText, weightText.toDoubleOrNull(), secsText, rirText, bandsText, notesText)
    }

    DisposableEffect(Unit) {
        onDispose {
            saveAllCurrentValues()
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

            // Pre-fill suggestion chip — slot height is reserved at
            // AssistChipDefaults.Height (32dp) even when the chip is hidden,
            // so the header row doesn't reflow as the user types and the chip
            // vanishes (which would otherwise shift the trailing Delete /
            // Start-Rest buttons under the user's finger).
            Box(
                modifier = Modifier
                    .heightIn(min = AssistChipDefaults.Height)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
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
                                text = performanceSuggestion.suggestionLabel ?: buildString {
                                    performanceSuggestion.suggestedWeight?.let { append("${it}$weightUnit ") }
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
                    )
                }
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
                val targetWeightHint = set.targetWeight?.takeIf { it.isNotBlank() }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { newText ->
                        // Allow only digits and one decimal point
                        if (newText.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            weightText = newText
                        }
                    },
                    label = { Text("Weight ($weightUnit)") },
                    placeholder = if (targetWeightHint != null) {
                        { Text("→ $targetWeightHint") }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true,
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true,
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
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
private fun rememberDebouncedField(
    source: String,
    onDebouncedChange: (String) -> Unit,
): MutableState<String> {
    val state = remember { mutableStateOf(source) }
    LaunchedEffect(state.value) {
        if (state.value != source) {
            kotlinx.coroutines.delay(1000)
            onDebouncedChange(state.value)
        }
    }
    LaunchedEffect(source) {
        if (state.value != source) {
            state.value = source
        }
    }
    return state
}
