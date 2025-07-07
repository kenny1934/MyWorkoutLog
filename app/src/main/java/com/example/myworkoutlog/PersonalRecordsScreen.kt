package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun PersonalRecordsScreen(
    viewModel: PrViewModel,
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToExerciseAnalytics: (String) -> Unit = {}
) {
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val filteredPRs by viewModel.filteredPRs.collectAsStateWithLifecycle()
    val prsByExercise = filteredPRs.groupBy { it.exerciseName }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = viewModel::onSearchTextChanged,
            label = { Text("Search Exercise...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") }
        )

        if (prsByExercise.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No PRs found.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                prsByExercise.forEach { (exerciseName, prsForExercise) ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        exerciseName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // Analytics button for this exercise
                                    IconButton(
                                        onClick = { 
                                            // Use the exercise ID from the first PR for this exercise
                                            val exerciseId = prsForExercise.firstOrNull()?.exerciseId
                                            exerciseId?.let { onNavigateToExerciseAnalytics(it) }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Analytics,
                                            contentDescription = "View $exerciseName analytics",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Separate PRs by type for clarity
                                val weightPRs = prsForExercise.filter { it.type == PRType.MAX_WEIGHT_FOR_REPS }.sortedBy { it.reps }
                                val repsPRs = prsForExercise.filter { it.type == PRType.MAX_REPS_AT_WEIGHT }.sortedByDescending { it.weight }
                                val durationPRs = prsForExercise.filter { it.type == PRType.DURATION }

                                // Display each PR type in its own row with an icon
                                if (weightPRs.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    PRCategoryRow(icon = Icons.Filled.FitnessCenter, title = "Best Weight for Reps")
                                    weightPRs.forEach { pr ->
                                        PRDetailRow(pr = pr, onNavigateToWorkout = onNavigateToWorkout)
                                    }
                                }
                                if (repsPRs.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    PRCategoryRow(icon = Icons.Filled.Repeat, title = "Best Reps at Weight")
                                    repsPRs.forEach { pr ->
                                        PRDetailRow(pr = pr, onNavigateToWorkout = onNavigateToWorkout)
                                    }
                                }
                                if (durationPRs.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    PRCategoryRow(icon = Icons.Filled.Timer, title = "Best Duration")
                                    durationPRs.forEach { pr ->
                                        PRDetailRow(pr = pr, onNavigateToWorkout = onNavigateToWorkout)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PRCategoryRow(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

// Helper function to format weight display for bodyweight exercises
fun formatWeightDisplay(pr: PersonalRecord): String {
    val weightUnit = pr.weightUnit ?: "kg"
    
    return if (pr.usesBodyweight && pr.bodyweightUsed != null && pr.externalWeight != null) {
        // Bodyweight exercise: show breakdown with decimal precision
        if (pr.externalWeight > 0) {
            "BW(${formatWeight(pr.bodyweightUsed)}$weightUnit) + ${formatWeight(pr.externalWeight)}$weightUnit = ${formatWeight(pr.weight)}$weightUnit"
        } else {
            "BW(${formatWeight(pr.bodyweightUsed)}$weightUnit) = ${formatWeight(pr.weight)}$weightUnit"
        }
    } else {
        // Regular exercise: show total weight only
        "${formatWeight(pr.weight)}$weightUnit"
    }
}

// Helper function to format weight with appropriate decimal precision
private fun formatWeight(weight: Double?): String {
    return when {
        weight == null -> "0"
        weight % 1.0 == 0.0 -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

@Composable
fun PRDetailRow(pr: PersonalRecord, onNavigateToWorkout: (String) -> Unit) {
    val weightUnit = pr.weightUnit ?: "kg"
    val e1RM = if (pr.weight != null && pr.reps != null) {
        StrengthAnalytics.calculateEpley1RM(pr.weight, pr.reps).roundToInt()
    } else null

    val prText = when (pr.type) {
        PRType.MAX_WEIGHT_FOR_REPS -> "${pr.reps} reps"
        PRType.MAX_REPS_AT_WEIGHT -> {
            if (pr.usesBodyweight && pr.bodyweightUsed != null && pr.externalWeight != null) {
                // For bodyweight exercises, show the breakdown in the context
                "@ ${formatWeightDisplay(pr)}"
            } else {
                "@ ${pr.weight} $weightUnit"
            }
        }
        PRType.DURATION -> "Max Duration"
    }
    val prValue = when (pr.type) {
        PRType.MAX_WEIGHT_FOR_REPS -> formatWeightDisplay(pr)
        PRType.MAX_REPS_AT_WEIGHT -> "${pr.reps} reps"
        PRType.DURATION -> "${pr.durationSecs}s"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToWorkout(pr.loggedWorkoutId) }
            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp), // Indent PRs
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add bodyweight indicator icon
        if (pr.usesBodyweight) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Bodyweight Exercise",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(prText, style = MaterialTheme.typography.bodyLarge)
            Text(pr.date, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(prValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (e1RM != null && pr.type == PRType.MAX_WEIGHT_FOR_REPS) {
                Text("e1RM: $e1RM $weightUnit", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
        }
    }
}