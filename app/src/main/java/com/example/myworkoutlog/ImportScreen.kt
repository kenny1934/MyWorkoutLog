@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myworkoutlog

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            // Convert URI to file path (this is simplified - real implementation would need proper URI handling)
            val filePath = uri.path ?: uri.toString()
            viewModel.selectFile(filePath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // File Selection Card
            FileSelectionCard(
                selectedFile = uiState.selectedFile,
                fileInfo = viewModel.getFileInfo(),
                onSelectFile = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/csv", "text/plain"))
                    }
                    filePickerLauncher.launch(Intent.createChooser(intent, "Select Import File"))
                }
            )

            // Import Options Card
            if (uiState.selectedFile != null) {
                ImportOptionsCard(
                    importMode = uiState.importMode,
                    dataType = uiState.dataType,
                    skipDuplicates = uiState.skipDuplicates,
                    allowSchemaUpgrade = uiState.allowSchemaUpgrade,
                    onImportModeChanged = viewModel::setImportMode,
                    onDataTypeChanged = viewModel::setDataType,
                    onSkipDuplicatesChanged = viewModel::setSkipDuplicates,
                    onAllowSchemaUpgradeChanged = viewModel::setAllowSchemaUpgrade,
                    getDataTypeDescription = viewModel::getDataTypeDescription,
                    getImportModeDescription = viewModel::getImportModeDescription
                )
            }

            // Validation Report Card
            uiState.validationReport?.let { report ->
                ValidationReportCard(report = report)
            }

            // Import Progress Card
            uiState.progress?.let { progress ->
                ImportProgressCard(progress = progress)
            }

            // Import Actions
            if (uiState.selectedFile != null && uiState.validationReport != null) {
                ImportActionsCard(
                    canPerformImport = viewModel.canPerformImport(),
                    canValidateOnly = viewModel.canValidateOnly(),
                    isLoading = uiState.isLoading,
                    onPerformImport = viewModel::performImport,
                    onValidateOnly = viewModel::validateOnly
                )
            }

            // Error Display
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = viewModel::clearError
                )
            }

            // Import Result Display
            uiState.importResult?.let { result ->
                ImportResultCard(
                    result = result,
                    onDismiss = viewModel::clearImportResult
                )
            }
        }
    }
}

@Composable
private fun FileSelectionCard(
    selectedFile: String?,
    fileInfo: FileInfo?,
    onSelectFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Upload,
                    contentDescription = "File Selection",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Select Import File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedFile == null) {
                Button(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = "Select File"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose File")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Supported formats: JSON, CSV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Show file info
                fileInfo?.let { info ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.fileName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${info.recordCount} records • ${info.dataType.name.replace("_", " ").lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            IconButton(onClick = onSelectFile) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Change File"
                                )
                            }
                        }

                        if (!info.isCompatible) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "File may not be compatible",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
private fun ImportOptionsCard(
    importMode: ImportMode,
    dataType: ImportDataType,
    skipDuplicates: Boolean,
    allowSchemaUpgrade: Boolean,
    onImportModeChanged: (ImportMode) -> Unit,
    onDataTypeChanged: (ImportDataType) -> Unit,
    onSkipDuplicatesChanged: (Boolean) -> Unit,
    onAllowSchemaUpgradeChanged: (Boolean) -> Unit,
    getDataTypeDescription: () -> String,
    getImportModeDescription: () -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Import Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Import Mode Selection
            Text(
                "Import Mode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(modifier = Modifier.selectableGroup()) {
                ImportMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = importMode == mode,
                                onClick = { onImportModeChanged(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = importMode == mode,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Text(
                text = getImportModeDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Data Type Selection
            Text(
                "Data Type",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Simplified data type selection for common types
            val commonTypes = listOf(
                ImportDataType.AUTO_DETECT,
                ImportDataType.COMPLETE_BACKUP,
                ImportDataType.WORKOUTS,
                ImportDataType.EXERCISES
            )

            Column(modifier = Modifier.selectableGroup()) {
                commonTypes.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = dataType == type,
                                onClick = { onDataTypeChanged(type) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = dataType == type,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text(
                text = getDataTypeDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = skipDuplicates,
                    onCheckedChange = onSkipDuplicatesChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Skip duplicates",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Don't import records that already exist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = allowSchemaUpgrade,
                    onCheckedChange = onAllowSchemaUpgradeChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Allow schema upgrade",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Import data from older app versions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationReportCard(report: ValidationReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.isValid) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (report.isValid) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = if (report.isValid) "Valid" else "Invalid",
                    tint = if (report.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Text(
                    if (report.isValid) "File Validation Passed" else "File Validation Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (report.isValid) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }

            if (report.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                report.issues.take(3).forEach { issue ->
                    Text(
                        "• ${issue.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (report.isValid) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                if (report.issues.size > 3) {
                    Text(
                        "... and ${report.issues.size - 3} more issues",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (report.isValid) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportProgressCard(progress: ImportProgress) {
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
                if (progress.total > 0) {
                    Text(
                        "${progress.processed}/${progress.total}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (progress.total > 0) {
                LinearProgressIndicator(
                    progress = { progress.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ImportActionsCard(
    canPerformImport: Boolean,
    canValidateOnly: Boolean,
    isLoading: Boolean,
    onPerformImport: () -> Unit,
    onValidateOnly: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Import Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onValidateOnly,
                    enabled = canValidateOnly && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Validate",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Validate Only")
                }

                Button(
                    onClick = onPerformImport,
                    enabled = canPerformImport && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importing...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Upload,
                            contentDescription = "Import",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Data")
                    }
                }
            }
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
private fun ImportResultCard(
    result: ImportResult,
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
                        text = if (result.success) "Import Successful" else "Import Failed",
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