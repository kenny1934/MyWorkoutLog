@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.UUID

// Enhanced Program Card with large screen optimizations
@Composable
fun EnhancedProgramCard(
    program: ProgramTemplate,
    isSelected: Boolean,
    onProgramClick: () -> Unit,
    onStartCycle: () -> Unit,
    onDeleteProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    val touchTargetSize = workoutTouchTargetSize()
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Card(
        modifier = modifier
            .clickable { onProgramClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = program.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (program.description?.isNotEmpty() == true) {
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onDeleteProgram,
                        modifier = Modifier.size(touchTargetSize)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Program",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(
                        onClick = onStartCycle,
                        modifier = Modifier.size(touchTargetSize)
                    ) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            contentDescription = "Start Cycle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Program Statistics
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgramStatChip(
                    icon = Icons.Outlined.CalendarToday,
                    label = "${program.weeks.size} weeks"
                )

                val totalSessions = program.weeks.sumOf { it.sessions.size }
                ProgramStatChip(
                    icon = Icons.Outlined.FitnessCenter,
                    label = "$totalSessions sessions"
                )
            }
        }
    }
}

@Composable
private fun ProgramStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun EmptyProgramsState(
    isSearching: Boolean,
    onCreateProgram: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Outlined.SearchOff else Icons.Outlined.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearching) "No programs found" else "No programs yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isSearching) {
                "Try adjusting your search terms"
            } else {
                "Create your first program to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (!isSearching) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateProgram) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Program")
            }
        }
    }
}

@Composable
fun ProgramDetailEmptyState(
    onCreateProgram: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select a program",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Choose a program from the list to view or edit its details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CreateProgramSheet(
    newName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing24, vertical = Dimens.spacing8)
                .padding(bottom = Dimens.spacing16)
        ) {
            Text(
                "New Program Blueprint",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Text(
                "Create a new workout program with custom weeks and sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            OutlinedTextField(
                value = newName,
                onValueChange = onNameChange,
                label = { Text("Program Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimens.spacing24))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Button(
                    onClick = onConfirm,
                    enabled = newName.isNotBlank()
                ) { Text("Create") }
            }
        }
    }
}

@Composable
fun StartCycleSheet(
    program: ProgramTemplate,
    cycleName: String,
    onCycleNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing24, vertical = Dimens.spacing8)
                .padding(bottom = Dimens.spacing16)
        ) {
            Text(
                "Start New Cycle",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                "You are about to start the program: ${program.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (program.weeks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.spacing4))
                Text(
                    "This program contains ${program.weeks.size} weeks with ${program.weeks.sumOf { it.sessions.size }} total sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            OutlinedTextField(
                value = cycleName,
                onValueChange = onCycleNameChange,
                label = { Text("Give this cycle a name") },
                placeholder = {
                    Text(
                        text = program.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimens.spacing24))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Button(onClick = onConfirm) { Text("Start") }
            }
        }
    }
}

/**
 * Shared "Add Session to Week" sheet — pick a template, give the session a name, tap to add.
 * Previously duplicated as an AlertDialog in both program editors; lifted here so both editors
 * (compact + master-detail) share the same interaction.
 */
@Composable
fun AddSessionSheet(
    allTemplates: List<WorkoutTemplate>,
    currentSessionCount: Int,
    onAdd: (ProgramSessionDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    var sessionName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacing24, vertical = Dimens.spacing8)
                .padding(bottom = Dimens.spacing16)
        ) {
            Text(
                "Add Session",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name (e.g., Day 1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            Text(
                "Choose a template",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            if (allTemplates.isEmpty()) {
                Text(
                    "No templates yet. Create one under Library → Manage Templates first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.spacing8)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacing4)
                ) {
                    items(allTemplates, key = { it.id }) { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSession = ProgramSessionDefinition(
                                        id = UUID.randomUUID().toString(),
                                        sessionName = sessionName.ifBlank { "Session ${currentSessionCount + 1}" },
                                        workoutTemplateId = template.id,
                                        order = currentSessionCount + 1
                                    )
                                    onAdd(newSession)
                                }
                                .padding(vertical = Dimens.spacing12),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${template.templateExercises.size} exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}
