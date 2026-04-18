@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CloudBackupScreen(
    viewModel: CloudBackupViewModel,
    onNavigateUp: () -> Unit
) {
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()

    if (layoutInfo.useMasterDetail && authStatus.isSignedIn) {
        // Large screen: Master-detail layout for signed-in users
        CloudBackupMasterDetailView(
            viewModel = viewModel,
            layoutInfo = layoutInfo,
            uiState = uiState,
            authStatus = authStatus,
            backups = backups,
            storageInfo = storageInfo,
            onNavigateUp = onNavigateUp
        )
    } else {
        // Small screen or not authenticated: Original single-column layout
        CloudBackupSingleColumnView(
            viewModel = viewModel,
            uiState = uiState,
            authStatus = authStatus,
            backups = backups,
            storageInfo = storageInfo,
            onNavigateUp = onNavigateUp
        )
    }
}

@Composable
private fun CloudBackupSingleColumnView(
    viewModel: CloudBackupViewModel,
    uiState: CloudBackupUiState,
    authStatus: CloudAuthStatus,
    backups: List<CloudBackupItem>,
    storageInfo: CloudStorageInfo?,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Authentication Section
            if (!authStatus.isSignedIn) {
                AuthenticationCard(
                    onSignIn = { viewModel.authenticate() },
                    isLoading = uiState.isLoading
                )
            } else {
                // User Info Card
                UserInfoCard(
                    authStatus = authStatus,
                    storageInfo = storageInfo,
                    onSignOut = { viewModel.signOut() },
                    onRefresh = { 
                        viewModel.loadBackups()
                        viewModel.loadStorageInfo()
                    }
                )

                // Backup Actions Card
                BackupActionsCard(
                    onCreateBackup = { viewModel.createBackup() },
                    isBackupInProgress = uiState.isBackupInProgress,
                    backupProgress = uiState.backupProgress
                )

                // Backup List
                if (uiState.isLoadingBackups) {
                    LoadingCard("Loading backups...")
                } else if (backups.isNotEmpty()) {
                    BackupListCard(
                        backups = backups,
                        onRestoreBackup = { backupId -> 
                            viewModel.restoreBackup(backupId)
                        },
                        onDeleteBackup = { backupId -> 
                            viewModel.deleteBackup(backupId)
                        },
                        isRestoreInProgress = uiState.isRestoreInProgress,
                        restoreProgress = uiState.restoreProgress
                    )
                } else {
                    EmptyBackupsCard(
                        onCreateFirstBackup = { viewModel.createBackup() }
                    )
                }

                // Restore Progress Card
                uiState.restoreProgress?.let { progress ->
                    RestoreProgressCard(progress = progress)
                }

                // Restore Result Card
                uiState.restoreResult?.let { result ->
                    RestoreResultCard(
                        result = result,
                        onDismiss = { viewModel.clearRestoreResult() }
                    )
                }
            }

            // Error Display
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun AuthenticationCard(
    onSignIn: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = "Cloud Storage",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Sign in to Google Drive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Securely backup your workout data to Google Drive and access it from any device",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing in...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Sign In",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        }
    }
}

@Composable
private fun UserInfoCard(
    authStatus: CloudAuthStatus,
    storageInfo: CloudStorageInfo?,
    onSignOut: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            authStatus.userName ?: "Unknown User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            authStatus.userEmail ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            }
            
            // Storage Info
            storageInfo?.let { info ->
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Storage Usage",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${info.formattedUsedSpace} / ${info.formattedTotalSpace}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { (info.usagePercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    if (info.backupSpaceUsedBytes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Backup space: ${info.formattedBackupSpace}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupActionsCard(
    onCreateBackup: () -> Unit,
    isBackupInProgress: Boolean,
    backupProgress: CloudBackupProgress?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Backup Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isBackupInProgress && backupProgress != null) {
                // Show backup progress
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            backupProgress.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${(backupProgress.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { backupProgress.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Button(
                    onClick = onCreateBackup,
                    enabled = !isBackupInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Backup",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Backup Now")
                }
            }
        }
    }
}

@Composable
private fun BackupListCard(
    backups: List<CloudBackupItem>,
    onRestoreBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
    isRestoreInProgress: Boolean,
    restoreProgress: CloudBackupProgress?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Available Backups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(backups) { backup ->
                    BackupItemCard(
                        backup = backup,
                        onRestore = { onRestoreBackup(backup.id) },
                        onDelete = { onDeleteBackup(backup.id) },
                        isRestoreInProgress = isRestoreInProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupItemCard(
    backup: CloudBackupItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    isRestoreInProgress: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        backup.deviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        backup.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    backup.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Backup metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${backup.metadata.totalWorkouts} workouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${backup.metadata.totalExercises} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${backup.metadata.totalPersonalRecords} PRs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!backup.isCompatible || backup.needsUpgrade) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (backup.isCompatible) Icons.Filled.Warning else Icons.Filled.Error,
                        contentDescription = "Compatibility Warning",
                        tint = if (backup.isCompatible) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (backup.isCompatible) "May require data migration" else "Incompatible backup version",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (backup.isCompatible) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                
                Button(
                    onClick = { showRestoreDialog = true },
                    enabled = backup.isCompatible && !isRestoreInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Restore",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Backup") },
            text = { 
                Text("Are you sure you want to delete this backup? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Restore confirmation dialog
    if (showRestoreDialog) {
        RestoreConfirmationDialog(
            backup = backup,
            onDismiss = { showRestoreDialog = false },
            onConfirm = { mode, skipDuplicates ->
                onRestore()
                showRestoreDialog = false
            }
        )
    }
}

@Composable
private fun RestoreConfirmationDialog(
    backup: CloudBackupItem,
    onDismiss: () -> Unit,
    onConfirm: (ImportMode, Boolean) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ImportMode.MERGE) }
    var skipDuplicates by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Backup") },
        text = {
            Column {
                Text("Restore backup from ${backup.deviceName}?")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Import Mode:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column(modifier = Modifier.selectableGroup()) {
                    ImportMode.values().filter { it != ImportMode.VALIDATE_ONLY }.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ImportMode.MERGE -> "Merge (recommended)"
                                    ImportMode.REPLACE -> "Replace existing data"
                                    else -> mode.name
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = skipDuplicates,
                        onCheckedChange = { skipDuplicates = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Skip duplicates",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMode, skipDuplicates) }
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RestoreProgressCard(progress: CloudBackupProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    progress.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RestoreResultCard(
    result: CloudRestoreResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = if (result.success) "Success" else "Error",
                        tint = if (result.success) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.success) "Restore Successful" else "Restore Failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.success) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = if (result.success) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
            
            if (result.success) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Imported: ${result.importedRecords} records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (result.skippedRecords > 0) {
                    Text(
                        text = "Skipped: ${result.skippedRecords} duplicates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                result.errors.take(3).forEach { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBackupsCard(
    onCreateFirstBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = "No Backups",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "No backups found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Create your first backup to securely store your workout data in the cloud",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onCreateFirstBackup) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Create Backup",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Backup")
            }
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun CloudBackupMasterDetailView(
    viewModel: CloudBackupViewModel,
    layoutInfo: AdaptiveLayoutInfo,
    uiState: CloudBackupUiState,
    authStatus: CloudAuthStatus,
    backups: List<CloudBackupItem>,
    storageInfo: CloudStorageInfo?,
    onNavigateUp: () -> Unit
) {
    var selectedBackup by remember { mutableStateOf<CloudBackupItem?>(null) }
    
    // Auto-select first backup when data loads
    LaunchedEffect(backups) {
        if (selectedBackup == null && backups.isNotEmpty()) {
            selectedBackup = backups.first()
        } else if (selectedBackup != null && backups.none { it.id == selectedBackup?.id }) {
            // Selected backup was deleted, select first available or clear selection
            selectedBackup = backups.firstOrNull()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(layoutInfo.contentPadding)
        ) {
            // Master Panel (Left side - 40%)
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .heightIn(min = 400.dp), // Ensure consistent minimum height
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Cloud Backups",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    item {
                        // User info section
                        UserInfoCard(
                            authStatus = authStatus,
                            storageInfo = storageInfo,
                            onSignOut = { viewModel.signOut() },
                            onRefresh = {
                                viewModel.loadBackups()
                                viewModel.loadStorageInfo()
                            }
                        )
                    }
                    
                    item {
                        // Backup actions
                        BackupActionsCard(
                            onCreateBackup = { viewModel.createBackup() },
                            isBackupInProgress = uiState.isBackupInProgress,
                            backupProgress = uiState.backupProgress
                        )
                    }
                    
                    if (uiState.isLoadingBackups) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (backups.isNotEmpty()) {
                        items(backups) { backup ->
                            BackupListItem(
                                backup = backup,
                                isSelected = selectedBackup?.id == backup.id,
                                onBackupSelected = { selectedBackup = backup },
                                onRestoreBackup = { viewModel.restoreBackup(backup.id) },
                                onDeleteBackup = { viewModel.deleteBackup(backup.id) }
                            )
                        }
                    } else {
                        item {
                            EmptyBackupsCard(
                                onCreateFirstBackup = { viewModel.createBackup() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Detail Panel (Right side - 60%)
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .heightIn(min = 400.dp), // Ensure consistent minimum height
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                CloudBackupDetailPanel(
                    selectedBackup = selectedBackup,
                    storageInfo = storageInfo,
                    uiState = uiState,
                    onRestoreBackup = { backup -> viewModel.restoreBackup(backup.id) },
                    onDeleteBackup = { backup -> viewModel.deleteBackup(backup.id) },
                    onClearRestoreResult = { viewModel.clearRestoreResult() }
                )
            }
        }
    }
}

@Composable
private fun BackupListItem(
    backup: CloudBackupItem,
    isSelected: Boolean,
    onBackupSelected: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDeleteBackup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBackupSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = backup.deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = backup.formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloudBackupDetailPanel(
    selectedBackup: CloudBackupItem?,
    storageInfo: CloudStorageInfo?,
    uiState: CloudBackupUiState,
    onRestoreBackup: (CloudBackupItem) -> Unit,
    onDeleteBackup: (CloudBackupItem) -> Unit,
    onClearRestoreResult: () -> Unit
) {
    if (selectedBackup == null) {
        // No backup selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Select Backup",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a backup to view details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Backup selected - show detailed view
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with backup name and actions
                Text(
                    text = selectedBackup.deviceName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Created: ${selectedBackup.formattedDate}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Backup metadata
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Backup Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Size: ${selectedBackup.formattedSize}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Workouts: ${selectedBackup.metadata.totalWorkouts}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Exercises: ${selectedBackup.metadata.totalExercises}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Personal Records: ${selectedBackup.metadata.totalPersonalRecords}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        if (!selectedBackup.isCompatible) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Incompatible version",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onRestoreBackup(selectedBackup) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isRestoreInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Backup")
                    }
                    
                    OutlinedButton(
                        onClick = { onDeleteBackup(selectedBackup) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
            
            // Progress and result cards
            uiState.restoreProgress?.let { progress ->
                item {
                    RestoreProgressCard(progress = progress)
                }
            }
            
            uiState.restoreResult?.let { result ->
                item {
                    RestoreResultCard(
                        result = result,
                        onDismiss = onClearRestoreResult
                    )
                }
            }
        }
    }
}