@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgramDetailViewer(
    program: ProgramTemplate,
    allTemplates: List<WorkoutTemplate>,
    activeCycleViewModel: ActiveCycleViewModel,
    onStartEdit: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onDeleteProgram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartCycleDialog by remember { mutableStateOf(false) }
    var cycleName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Title row with compact actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Icon-only Delete button
                IconButton(
                    onClick = { onDeleteProgram(program.id) }
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete Program",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Icon-only Edit button for maximum space efficiency
                IconButton(onClick = onStartEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit Program",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Compact Start button with shorter text
                FilledTonalButton(
                    onClick = { showStartCycleDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", maxLines = 1)
                }
            }
        }

        // Description now spans full width below title row
        if (program.description?.isNotEmpty() == true) {
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Program Overview Stats - horizontal distribution
        ProgramOverviewStats(program = program)

        Spacer(modifier = Modifier.height(12.dp))

        // Weeks Overview
        if (program.weeks.isNotEmpty()) {
            Text(
                text = "Program Structure",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(program.weeks) { week ->
                    WeekOverviewCard(
                        week = week,
                        allTemplates = allTemplates
                    )
                }
            }
        } else {
            EmptyProgramStructure(onStartEdit = onStartEdit)
        }
    }

    if (showStartCycleDialog) {
        StartCycleSheet(
            program = program,
            cycleName = cycleName,
            onCycleNameChange = { cycleName = it },
            onConfirm = {
                val finalCycleName = cycleName.ifBlank { program.name }
                activeCycleViewModel.startCycle(program, finalCycleName)
                showStartCycleDialog = false
                cycleName = ""
                onNavigateToDashboard()
            },
            onDismiss = {
                showStartCycleDialog = false
                cycleName = ""
            }
        )
    }
}

@Composable
private fun ProgramOverviewStats(program: ProgramTemplate) {
    val totalSessions = program.weeks.sumOf { it.sessions.size }
    val uniqueTemplates = program.weeks.flatMap { it.sessions.map { session -> session.workoutTemplateId } }.distinct().size

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OverviewStatCard(
            label = "Weeks",
            value = program.weeks.size.toString(),
            icon = Icons.Outlined.CalendarToday
        )

        OverviewStatCard(
            label = "Sessions",
            value = totalSessions.toString(),
            icon = Icons.Outlined.FitnessCenter
        )

        OverviewStatCard(
            label = "Templates",
            value = uniqueTemplates.toString(),
            icon = Icons.AutoMirrored.Outlined.Assignment
        )
    }
}

@Composable
private fun OverviewStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WeekOverviewCard(
    week: ProgramWeekDefinition,
    allTemplates: List<WorkoutTemplate>
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = week.weekLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            week.sessions.sortedBy { it.order }.forEach { session ->
                val template = allTemplates.find { it.id == session.workoutTemplateId }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${session.order}. ${session.sessionName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = template?.name ?: "Unknown Template",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (template != null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    if (template != null) {
                        Text(
                            text = "${template.templateExercises.size} exercises",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (session != week.sessions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramStructure(onStartEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.EditCalendar,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No program structure yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Start editing to add weeks and sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onStartEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Editing")
        }
    }
}
