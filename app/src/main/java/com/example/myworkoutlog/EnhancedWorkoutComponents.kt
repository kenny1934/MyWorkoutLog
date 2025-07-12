package com.example.myworkoutlog

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.TextStyle
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
                    .padding(16.dp)
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
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
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
                                    fontSize = 18.sp,
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
                                                fontSize = 18.sp,
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
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
                    modifier = Modifier.size(32.dp),
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