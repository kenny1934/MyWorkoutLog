@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ManageProgramsScreen(
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    onNavigateToProgram: (String) -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateUp: (() -> Unit)? = null
) {
    val programs by programViewModel.allPrograms.collectAsStateWithLifecycle()
    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var showStartCycleDialog by remember { mutableStateOf<ProgramTemplate?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var programToDelete by remember { mutableStateOf<ProgramTemplate?>(null) }
    var newName by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Program Blueprints",
        onNavigateUp = onNavigateUp,
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateProgramDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create new program")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(Dimens.screenPadding)) {
            if (programs.isEmpty()) {
                Text(
                    "No programs yet. Click the '+' button to create one.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(programs, key = { it.id }) { program ->
                        var showOverflowMenu by remember { mutableStateOf(false) }

                        Card(elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigateToProgram(program.id) }
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text(
                                        text = program.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // OVERFLOW MENU
                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Program") },
                                            onClick = {
                                                showOverflowMenu = false
                                                onNavigateToProgram(program.id)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate") },
                                            onClick = {
                                                showOverflowMenu = false
                                                programViewModel.duplicate(program)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ContentCopy,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Delete Program",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showOverflowMenu = false
                                                programToDelete = program
                                                showDeleteConfirmation = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        )
                                    }
                                }
                                // "Start Cycle" button
                                IconButton(
                                    onClick = { showStartCycleDialog = program },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Start Cycle"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom sheet for creating a new program
            if (showCreateProgramDialog) {
                CreateProgramSheet(
                    newName = newName,
                    onNameChange = { newName = it },
                    onConfirm = {
                        if (newName.isNotBlank()) {
                            programViewModel.insert(newName, null)
                            newName = ""
                            showCreateProgramDialog = false
                        }
                    },
                    onDismiss = {
                        showCreateProgramDialog = false
                        newName = ""
                    }
                )
            }

            // Bottom sheet for starting a new cycle
            if (showStartCycleDialog != null) {
                val programToStart = showStartCycleDialog!!
                StartCycleSheet(
                    program = programToStart,
                    cycleName = newName,
                    onCycleNameChange = { newName = it },
                    onConfirm = {
                        val cycleName = newName.ifBlank { programToStart.name }
                        activeCycleViewModel.startCycle(programToStart, cycleName)
                        showStartCycleDialog = null
                        newName = ""
                        onNavigateToDashboard()
                    },
                    onDismiss = {
                        showStartCycleDialog = null
                        newName = ""
                    }
                )
            }

            // Delete confirmation dialog
            if (showDeleteConfirmation && programToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmation = false
                        programToDelete = null
                    },
                    title = {
                        Text(
                            text = "Delete Program",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Are you sure you want to delete \"${programToDelete?.name}\"?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This will permanently delete the program blueprint and cannot be undone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                programToDelete?.let { program ->
                                    programViewModel.deleteById(program.id)
                                }
                                showDeleteConfirmation = false
                                programToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                programToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
