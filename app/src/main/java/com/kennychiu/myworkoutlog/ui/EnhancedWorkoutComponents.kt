package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.extendedColors
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.content.Intent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact input field for RIR (Reps in Reserve) — stepper layout, range 0..10.
 */
@Composable
fun RirInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val hasValue = value.isNotEmpty()
    val currentRir = value.toIntOrNull() ?: 0
    val descriptor = if (!hasValue) "Not logged" else when (currentRir) {
        0 -> "Failure"
        1, 2 -> "Very Hard"
        3, 4 -> "Hard"
        5, 6 -> "Moderate"
        7, 8 -> "Easy"
        else -> "Very Easy"
    }

    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = MaterialTheme.extendedColors.warning
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.extendedColors.success
    val intensityColor = when (currentRir) {
        0 -> errorColor
        1, 2 -> errorColor
        3, 4 -> warningColor
        5, 6 -> primaryColor
        7, 8 -> successColor
        else -> successColor
    }
    val animatedRir by animateIntAsState(targetValue = currentRir, label = "rirValue")
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val gradientBrush = remember(intensityColor, surfaceVariant) {
        Brush.horizontalGradient(
            listOf(
                surfaceVariant.copy(alpha = 0.45f),
                intensityColor.copy(alpha = 0.10f),
                surfaceVariant.copy(alpha = 0.45f)
            )
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RIR",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = descriptor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = intensityColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(gradientBrush)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!hasValue) onValueChange("0")
                    else onValueChange((currentRir - 1).coerceAtLeast(0).toString())
                },
                enabled = !hasValue || currentRir > 0,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = intensityColor.copy(alpha = 0.14f),
                    contentColor = intensityColor
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease RIR",
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = if (hasValue) animatedRir.toString() else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = intensityColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!hasValue) onValueChange("0")
                    else onValueChange((currentRir + 1).coerceAtMost(10).toString())
                },
                enabled = !hasValue || currentRir < 10,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = intensityColor.copy(alpha = 0.14f),
                    contentColor = intensityColor
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase RIR",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Enhanced exercise card with better visual hierarchy and modern design
 * Replaces the basic Card layout with a beautiful, fitness-focused component
 */
@Composable
fun EnhancedExerciseCard(
    exerciseName: String,
    isSubstitute: Boolean = false,
    originalExerciseName: String? = null,
    setsCompleted: Int = 0,
    totalSets: Int = 0,
    lastPerformance: String? = null,
    progressionHint: String? = null,
    modifier: Modifier = Modifier,
    onAddSet: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val completionPercentage = if (totalSets > 0) setsCompleted.toFloat() / totalSets else 0f
    
    // Animation for completion progress
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "completion_progress"
    )
    
    // Card elevation based on completion
    val animatedElevation by animateDpAsState(
        targetValue = if (completionPercentage > 0.8f) 12.dp else 6.dp,
        animationSpec = tween(300),
        label = "card_elevation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Regular click - no action needed */ },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completionPercentage > 0.8f) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (completionPercentage > 0.8f) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            ),
                            startY = 0f,
                            endY = 200f
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
                .padding(20.dp)
        ) {
            // Header section with exercise name and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exercise info column
                Column(modifier = Modifier.weight(1f)) {
                    // Exercise name
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Status indicators row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Substitute indicator
                        if (isSubstitute) {
                            val badgeLabel = originalExerciseName
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "from $it" }
                                ?: "Substituted"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = "Substituted",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Sets completion indicator
                        if (totalSets > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    completionPercentage >= 1.0f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    completionPercentage > 0.5f -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$setsCompleted/$totalSets sets",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = when {
                                            completionPercentage >= 1.0f -> MaterialTheme.colorScheme.primary
                                            completionPercentage > 0.5f -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Last performance indicator
                    lastPerformance?.let { performance ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last: $performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Progression scheme hint (Linear +2.5kg/wk, Double 8–12 reps, …)
                    progressionHint?.let { hint ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Action button
                FilledTonalIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddSet()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Set",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Progress bar for completion
            if (totalSets > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when {
                        completionPercentage >= 1.0f -> MaterialTheme.colorScheme.primary
                        completionPercentage > 0.5f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    },
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sets content
            content()
        }
    }
}

/**
 * Flat set row — collapsible summary when filled, OutlinedTextField inputs when expanded.
 */
@Composable
fun EnhancedSetRow(
    setNumber: Int,
    weightValue: String,
    repsValue: String,
    secsValue: String = "",
    rirValue: String = "",
    bandsValue: String = "",
    notesValue: String = "",
    videoReference: String? = null,
    restTimeSeconds: Int? = null,
    weightUnit: String,
    showWeightReps: Boolean = true,
    showSecs: Boolean = false,
    showDeleteButton: Boolean = false,
    performanceSuggestion: PerformanceSuggestion? = null,
    isLargeScreen: Boolean = false,
    onWeightChange: (String) -> Unit = {},
    onRepsChange: (String) -> Unit = {},
    onSecsChange: (String) -> Unit = {},
    onRirChange: (String) -> Unit = {},
    onBandsChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onVideoSelected: (String) -> Unit = {},
    onVideoRemoved: () -> Unit = {},
    onStartRest: () -> Unit = {},
    onDeleteSet: () -> Unit = {},
    onApplySuggestion: () -> Unit = {},
    onCopyPreviousSet: (() -> Unit)? = null,
    onClearSet: (() -> Unit)? = null,
    onEditRestTime: ((Int) -> Unit)? = null,
    onClearRestTime: (() -> Unit)? = null,
    isNextUnfilled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showExpandedOptions by rememberSaveable { mutableStateOf(false) }
    var showRestTimeEditDialog by rememberSaveable { mutableStateOf(false) }

    val weightRepsDone = weightValue.isNotEmpty() && repsValue.isNotEmpty()
    val secsDone = secsValue.isNotEmpty()
    val rirDone = rirValue.isNotEmpty()
    val isSetCompleted = (showWeightReps || showSecs) &&
        (!showWeightReps || weightRepsDone) &&
        (!showSecs || secsDone) &&
        rirDone
    val hasAnyValue = weightValue.isNotEmpty() || repsValue.isNotEmpty() || secsValue.isNotEmpty()
    var isCollapsed by rememberSaveable {
        mutableStateOf(isSetCompleted || (!hasAnyValue && !isNextUnfilled))
    }

    // Reactive collapse driver. Debounces on completion so the user can dial in RIR
    // without the row collapsing under them on the first +/- tap. Any field change
    // (including RIR) cancels and restarts the delay.
    LaunchedEffect(isSetCompleted, isNextUnfilled, hasAnyValue, rirValue, weightValue, repsValue, secsValue) {
        if (isSetCompleted) {
            delay(1500)
            isCollapsed = true
        } else {
            isCollapsed = !isNextUnfilled && !hasAnyValue
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isCollapsed = !isCollapsed
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Set $setNumber",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (isCollapsed) {
                        Text(
                            text = buildSetSummary(weightValue, repsValue, secsValue, rirValue, weightUnit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        restTimeSeconds?.let { restTime ->
                            // Tap opens DurationEditDialog to correct the value; long-press
                            // clears it. Both affordances gated on the callbacks being wired.
                            val badgeInteractive = onEditRestTime != null || onClearRestTime != null
                            val badgeModifier = if (badgeInteractive) {
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (onEditRestTime != null) {
                                                showRestTimeEditDialog = true
                                            }
                                        },
                                        onLongClick = onClearRestTime?.let { clear ->
                                            {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clear()
                                            }
                                        },
                                        onLongClickLabel = if (onClearRestTime != null) "Clear Rest Time" else null
                                    )
                            } else Modifier
                            Surface(
                                modifier = badgeModifier,
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Rest Time",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = formatTime(restTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isCollapsed = !isCollapsed
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = if (isCollapsed) "Expand Set" else "Collapse Set",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showDeleteButton) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteSet()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Set",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!isCollapsed) {
                        if (onCopyPreviousSet != null && !hasAnyValue) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCopyPreviousSet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Previous Set",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (onClearSet != null && hasAnyValue) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCollapsed = false
                                    onClearSet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Backspace,
                                    contentDescription = "Clear Set",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartRest()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Start Rest Timer",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !isCollapsed) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (performanceSuggestion != null && weightValue.isEmpty() && repsValue.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AssistChip(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onApplySuggestion()
                            },
                            label = {
                                Text(
                                    text = buildString {
                                        performanceSuggestion.suggestedWeight?.let { append("${it}kg ") }
                                        performanceSuggestion.suggestedReps?.let { append("${it}r ") }
                                        performanceSuggestion.suggestedSecs?.let { append("${it}s ") }
                                        performanceSuggestion.daysAgo?.let { append("(${it}d ago)") }
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (showWeightReps) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Weight",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Reps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightValue,
                                onValueChange = onWeightChange,
                                trailingIcon = { Text(weightUnit, style = MaterialTheme.typography.bodyMedium) },
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Weight in $weightUnit" }
                            )
                            OutlinedTextField(
                                value = repsValue,
                                onValueChange = onRepsChange,
                                placeholder = { Text("0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Reps" }
                            )
                        }
                    }

                    if (showSecs) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = secsValue,
                            onValueChange = onSecsChange,
                            trailingIcon = { Text("sec", style = MaterialTheme.typography.bodyMedium) },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Duration in seconds" }
                        )
                    }

                    if (showWeightReps) {
                        Spacer(modifier = Modifier.height(12.dp))
                        RirInputField(
                            value = rirValue,
                            onValueChange = onRirChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Additional Options",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showExpandedOptions = !showExpandedOptions
                            }
                        ) {
                            Icon(
                                imageVector = if (showExpandedOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (showExpandedOptions) "Hide Options" else "Show Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = showExpandedOptions) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = bandsValue,
                                onValueChange = onBandsChange,
                                label = { Text("Resistance Bands") },
                                placeholder = { Text("e.g., Red, Blue") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = notesValue,
                                onValueChange = onNotesChange,
                                label = { Text("Notes") },
                                placeholder = { Text("Personal notes…") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Column {
                                Text(
                                    text = "Form Reference",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                VideoReferenceSelector(
                                    currentVideoPath = videoReference,
                                    onVideoSelected = onVideoSelected,
                                    onVideoRemoved = onVideoRemoved,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (onEditRestTime != null) {
        DurationEditDialog(
            isVisible = showRestTimeEditDialog,
            currentDurationSeconds = restTimeSeconds ?: 0,
            onDismiss = { showRestTimeEditDialog = false },
            onConfirm = { newRestSeconds ->
                onEditRestTime(newRestSeconds)
                showRestTimeEditDialog = false
            },
            title = "Edit Rest Time",
            instruction = "Adjust the recorded rest time for this set:",
            onClear = onClearRestTime?.let { clear ->
                {
                    clear()
                    showRestTimeEditDialog = false
                }
            }
        )
    }
}

private fun buildSetSummary(
    weight: String,
    reps: String,
    secs: String,
    rir: String,
    weightUnit: String
): String {
    val weightReps = listOfNotNull(
        weight.takeIf { it.isNotEmpty() }?.let { "$it $weightUnit" },
        reps.takeIf { it.isNotEmpty() }
    ).joinToString(" × ")
    val parts = listOfNotNull(
        weightReps.takeIf { it.isNotEmpty() },
        secs.takeIf { it.isNotEmpty() }?.let { "${it}s" },
        rir.takeIf { it.isNotEmpty() }?.let { "RIR $it" }
    )
    return if (parts.isEmpty()) "Empty" else parts.joinToString(" · ")
}

/**
 * Enhanced timer component with circular progress and modern design
 * Provides better visual feedback for rest periods during workouts
 */
@Composable
fun EnhancedTimerBar(
    isRunning: Boolean,
    currentTime: Int,
    targetTime: Int = 120, // Default 2 minutes rest
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val progress = if (targetTime > 0) (currentTime.toFloat() / targetTime.toFloat()).coerceIn(0f, 1f) else 0f
    
    // Animated progress for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "timer_progress"
    )
    
    // Color scheme based on timer state
    val timerColor = when {
        currentTime >= targetTime -> MaterialTheme.colorScheme.primary
        currentTime >= targetTime * 0.8f -> MaterialTheme.colorScheme.tertiary
        currentTime >= targetTime * 0.5f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    
    AnimatedVisibility(
        visible = isRunning || currentTime > 0,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular timer display. Long-press to reset the countdown
                // back to the full target duration — the visible Reset
                // IconButton was dropped in slice 78 to de-clutter the bar.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .combinedClickable(
                            onClick = { /* tap is a no-op; primary actions live in the button row */ },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReset()
                            },
                            onLongClickLabel = "Reset Timer"
                        )
                ) {
                    // Background circle
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        strokeWidth = 6.dp,
                    )

                    // Progress circle
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = timerColor,
                        strokeWidth = 6.dp,
                    )

                    // Time text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatTime(currentTime),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (targetTime > 0) {
                            Text(
                                text = "/ ${formatTime(targetTime)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Control buttons: pause/resume · +15s · stop. Reset moved to
                // long-press-on-time-display above.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isRunning) onPause() else onResume()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause Timer" else "Resume Timer",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddTime()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "+15s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop Timer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


/**
 * Video selection component for recording form references
 */
@Composable
fun VideoReferenceSelector(
    currentVideoPath: String?,
    onVideoSelected: (String) -> Unit,
    onVideoRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            // Store the URI directly (no copying to local storage)
            try {
                // Grant persistent permission to access the URI
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                onVideoSelected(selectedUri.toString())
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (e: Exception) {
                // Handle error - URI might not support persistent permissions
                // Still store the URI as it might work for current session
                onVideoSelected(selectedUri.toString())
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Video status indicator
        if (currentVideoPath != null) {
            // Show video attached indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = "Video attached",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Form video referenced",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Remove video button
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVideoRemoved()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove video",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Show add video button
            OutlinedButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = "Add form video",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reference Form Video",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}