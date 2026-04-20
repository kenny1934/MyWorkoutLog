package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import android.content.Intent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced input field specifically designed for workout data entry
 * Replaces the ugly OutlinedTextField with a beautiful, fitness-focused component
 */
@Composable
fun EnhancedWorkoutInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    unit: String? = null,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    isError: Boolean = false,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    placeholder: String? = null,
    maxLength: Int = 10,
    supportingText: String? = null,
    colors: WorkoutInputColors = WorkoutInputDefaults.colors()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val haptics = LocalHapticFeedback.current
    
    // Animated properties for focus states
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.errorBorder
            isFocused -> colors.focusedBorder
            else -> colors.unfocusedBorder
        },
        animationSpec = tween(200),
        label = "border_color"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledBackground
            isFocused -> colors.focusedBackground
            else -> colors.unfocusedBackground
        },
        animationSpec = tween(200),
        label = "background_color"
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )
    
    Column(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(animatedScale),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isFocused) 8.dp else 4.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedBackgroundColor)
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = animatedBorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optional icon
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (isFocused) colors.focusedIcon else colors.unfocusedIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Input field column
                    Column(modifier = Modifier.weight(1f)) {
                        // Label
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFocused) colors.focusedLabel else colors.unfocusedLabel,
                            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Input field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = { newValue ->
                                    if (newValue.length <= maxLength) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onValueChange(newValue)
                                    }
                                },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inputText,
                                    textAlign = TextAlign.Start
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                                interactionSource = interactionSource,
                                enabled = enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { focusState ->
                                        onFocusChanged?.invoke(focusState.isFocused)
                                    },
                                decorationBox = { innerTextField ->
                                    if (value.isEmpty() && placeholder != null) {
                                        Text(
                                            text = placeholder,
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = colors.placeholder,
                                                textAlign = TextAlign.Start
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            
                            // Unit display
                            unit?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.unitText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Supporting text
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.errorText else colors.supportingText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Enhanced input field with increment/decrement buttons for quick adjustments
 * Perfect for weight and rep inputs
 */
@Composable
fun EnhancedStepperInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
    step: Double = 1.0,
    minValue: Double = 0.0,
    maxValue: Double = 999.0,
    decimalPlaces: Int = 1,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    colors: WorkoutInputColors = WorkoutInputDefaults.colors()
) {
    val haptics = LocalHapticFeedback.current
    val currentValue = value.toDoubleOrNull() ?: 0.0
    
    fun incrementValue() {
        val newValue = (currentValue + step).coerceIn(minValue, maxValue)
        val formattedValue = if (decimalPlaces == 0) {
            newValue.toInt().toString()
        } else {
            "%.${decimalPlaces}f".format(newValue).trimEnd('0').trimEnd('.')
        }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onValueChange(formattedValue)
    }
    
    fun decrementValue() {
        val newValue = (currentValue - step).coerceIn(minValue, maxValue)
        val formattedValue = if (decimalPlaces == 0) {
            newValue.toInt().toString()
        } else {
            "%.${decimalPlaces}f".format(newValue).trimEnd('0').trimEnd('.')
        }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onValueChange(formattedValue)
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement button
        FilledTonalIconButton(
            onClick = { decrementValue() },
            enabled = currentValue > minValue,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Input field
        EnhancedWorkoutInputField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            unit = unit,
            onFocusChanged = onFocusChanged,
            placeholder = "0",
            modifier = Modifier.weight(1f),
            colors = colors
        )
        
        // Increment button
        FilledTonalIconButton(
            onClick = { incrementValue() },
            enabled = currentValue < maxValue,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Compact input field for RIR (Reps in Reserve) with visual scale
 */
@Composable
fun RirInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val currentRir = value.toIntOrNull() ?: 0
    
    Column(modifier = modifier) {
        Text(
            text = "RIR",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // RIR scale buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(11) { rir ->
                val isSelected = currentRir == rir
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onValueChange(rir.toString())
                    },
                    label = {
                        Text(
                            text = rir.toString(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.size(width = 48.dp, height = 44.dp),
                    leadingIcon = null
                )
            }
        }
        
        // RIR description
        Text(
            text = when (currentRir) {
                0 -> "Failure"
                1, 2 -> "Very Hard"
                3, 4 -> "Hard"
                5, 6 -> "Moderate"
                7, 8 -> "Easy"
                else -> "Very Easy"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Color scheme for workout input fields
 */
data class WorkoutInputColors(
    val focusedBackground: Color,
    val unfocusedBackground: Color,
    val disabledBackground: Color,
    val focusedBorder: Color,
    val unfocusedBorder: Color,
    val errorBorder: Color,
    val focusedLabel: Color,
    val unfocusedLabel: Color,
    val focusedIcon: Color,
    val unfocusedIcon: Color,
    val inputText: Color,
    val placeholder: Color,
    val unitText: Color,
    val supportingText: Color,
    val errorText: Color
)

object WorkoutInputDefaults {
    @Composable
    fun colors(): WorkoutInputColors {
        return WorkoutInputColors(
            focusedBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            unfocusedBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedBorder = MaterialTheme.colorScheme.primary,
            unfocusedBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            errorBorder = MaterialTheme.colorScheme.error,
            focusedLabel = MaterialTheme.colorScheme.primary,
            unfocusedLabel = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIcon = MaterialTheme.colorScheme.primary,
            unfocusedIcon = MaterialTheme.colorScheme.onSurfaceVariant,
            inputText = MaterialTheme.colorScheme.onSurface,
            placeholder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            unitText = MaterialTheme.colorScheme.onSurfaceVariant,
            supportingText = MaterialTheme.colorScheme.onSurfaceVariant,
            errorText = MaterialTheme.colorScheme.error
        )
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
                                        text = "Substituted",
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
 * Enhanced set row with modern design and better visual grouping
 * Provides cleaner layout and better user experience
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
    modifier: Modifier = Modifier,
    colors: WorkoutInputColors = WorkoutInputDefaults.colors()
) {
    val haptics = LocalHapticFeedback.current
    var showExpandedOptions by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Set header with number and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Set number and rest time display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set number badge
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
                    
                    // Rest time badge (if recorded)
                    restTimeSeconds?.let { restTime ->
                        Surface(
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
                                    text = "${restTime / 60}:${String.format("%02d", restTime % 60)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
                
                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button (only show if more than 1 set)
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
                    
                    // Timer button
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
            
            // Performance suggestion chip (only show for empty sets)
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
            
            Spacer(modifier = Modifier.height(if (isLargeScreen) 16.dp else 12.dp))
            
            // Input fields grid - adaptive layout for large screens
            if (showWeightReps) {
                if (isLargeScreen) {
                    // Large screen: Separate rows for better fit
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weight and Reps row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Weight input
                            EnhancedWorkoutInputField(
                                value = weightValue,
                                onValueChange = onWeightChange,
                                label = "Weight",
                                unit = weightUnit,
                                placeholder = "0",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(workoutInputHeight()),
                                colors = colors
                            )
                            
                            // Reps input
                            EnhancedWorkoutInputField(
                                value = repsValue,
                                onValueChange = onRepsChange,
                                label = "Reps",
                                placeholder = "0",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(workoutInputHeight()),
                                colors = colors
                            )
                        }
                        
                        // RIR input on separate row for large screens
                        RirInputField(
                            value = rirValue,
                            onValueChange = onRirChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Compact screen: Standard layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weight input
                        EnhancedWorkoutInputField(
                            value = weightValue,
                            onValueChange = onWeightChange,
                            label = "Weight",
                            unit = weightUnit,
                            placeholder = "0",
                            modifier = Modifier.weight(1f),
                            colors = colors
                        )
                        
                        // Reps input
                        EnhancedWorkoutInputField(
                            value = repsValue,
                            onValueChange = onRepsChange,
                            label = "Reps",
                            placeholder = "0",
                            modifier = Modifier.weight(1f),
                            colors = colors
                        )
                    }
                }
            }
            
            // Seconds input for time-based exercises
            if (showSecs) {
                Spacer(modifier = Modifier.height(if (isLargeScreen) 12.dp else 8.dp))
                EnhancedWorkoutInputField(
                    value = secsValue,
                    onValueChange = onSecsChange,
                    label = "Duration",
                    unit = "sec",
                    placeholder = "0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLargeScreen) Modifier.height(workoutInputHeight()) else Modifier),
                    colors = colors
                )
            }
            
            // RIR input (compact version - only for compact screens when weight/reps are shown)
            if (!isLargeScreen && showWeightReps && (rirValue.isNotEmpty() || true)) {
                Spacer(modifier = Modifier.height(12.dp))
                RirInputField(
                    value = rirValue,
                    onValueChange = onRirChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Expandable options toggle
            Spacer(modifier = Modifier.height(12.dp))
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
            
            // Expandable bands and notes section
            AnimatedVisibility(visible = showExpandedOptions) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bands input
                    EnhancedWorkoutInputField(
                        value = bandsValue,
                        onValueChange = onBandsChange,
                        label = "Resistance Bands",
                        placeholder = "e.g., Red, Blue",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth(),
                        colors = colors
                    )
                    
                    // Notes input
                    EnhancedWorkoutInputField(
                        value = notesValue,
                        onValueChange = onNotesChange,
                        label = "Notes",
                        placeholder = "Personal notes...",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth(),
                        colors = colors
                    )
                    
                    // Video reference selector
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
                // Circular timer display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
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
                            text = "${currentTime / 60}:${String.format("%02d", currentTime % 60)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (targetTime > 0) {
                            Text(
                                text = "/ ${targetTime / 60}:${String.format("%02d", targetTime % 60)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button (larger)
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
                    
                    // Quick time additions
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
                    
                    // Reset button
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "Reset Timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Stop button
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