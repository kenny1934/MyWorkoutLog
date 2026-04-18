@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import android.content.Intent
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
fun ExportScreen(
    viewModel: ExportViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportSummary by viewModel.exportSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Sharing helper
    val sharingHelper = remember { ExportSharingHelper(context) }

    // Handle export result and show sharing options
    LaunchedEffect(uiState.lastExportResult) {
        uiState.lastExportResult?.let { result ->
            if (result.success) {
                // Show sharing dialog or automatically trigger sharing
                // This will be handled by the ExportResultCard now
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
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
            // Export Summary Card
            ExportSummaryCard(exportSummary = exportSummary)

            // Data Type Selection
            DataTypeSelectionCard(
                selectedDataType = uiState.selectedDataType,
                onDataTypeSelected = viewModel::selectDataType,
                getDataTypeDescription = viewModel::getDataTypeDescription,
                getEstimatedRecordCount = viewModel::getEstimatedRecordCount
            )

            // Format Selection
            FormatSelectionCard(
                selectedFormat = uiState.selectedFormat,
                onFormatSelected = viewModel::selectFormat
            )

            // Date Range Selection
            DateRangeSelectionCard(
                selectedDateRange = uiState.selectedDateRange,
                customStartDate = uiState.customStartDate,
                customEndDate = uiState.customEndDate,
                onDateRangeSelected = viewModel::selectDateRange,
                onCustomDateRangeSet = viewModel::setCustomDateRange
            )

            // Export Options
            ExportOptionsCard(
                includeMetadata = uiState.includeMetadata,
                onToggleIncludeMetadata = viewModel::toggleIncludeMetadata
            )

            // Export Button
            ExportButton(
                isLoading = uiState.isLoading,
                onExport = viewModel::exportData,
                estimatedRecords = viewModel.getEstimatedRecordCount()
            )

            // Error Display
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = viewModel::clearError
                )
            }

            // Export Result Display
            uiState.lastExportResult?.let { result ->
                ExportResultCard(
                    result = result,
                    onDismiss = viewModel::clearLastExportResult,
                    onShare = { 
                        val intent = sharingHelper.createSharingIntent(result)
                        intent?.let { context.startActivity(Intent.createChooser(it, "Share Export")) }
                    },
                    onSaveToDownloads = {
                        sharingHelper.saveToDownloads(result)
                    },
                    onEmailShare = {
                        val intent = sharingHelper.createEmailIntent(result)
                        intent?.let { context.startActivity(Intent.createChooser(it, "Email Export")) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExportSummaryCard(exportSummary: ExportSummary?) {
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
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = "Export Summary",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Data Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exportSummary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SummaryItem(
                        icon = Icons.Filled.FitnessCenter,
                        label = "Workouts",
                        count = exportSummary.totalWorkouts
                    )
                    SummaryItem(
                        icon = Icons.Filled.List,
                        label = "Exercises",
                        count = exportSummary.totalExercises
                    )
                    SummaryItem(
                        icon = Icons.Filled.EmojiEvents,
                        label = "PRs",
                        count = exportSummary.totalPersonalRecords
                    )
                    SummaryItem(
                        icon = Icons.Filled.Timeline,
                        label = "Programs",
                        count = exportSummary.totalPrograms
                    )
                }

                exportSummary.dateRangeStart?.let { start ->
                    exportSummary.dateRangeEnd?.let { end ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Data Range: $start to $end",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: ImageVector,
    label: String,
    count: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DataTypeSelectionCard(
    selectedDataType: ExportDataType,
    onDataTypeSelected: (ExportDataType) -> Unit,
    getDataTypeDescription: () -> String,
    getEstimatedRecordCount: () -> Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Select Data to Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.selectableGroup()) {
                ExportDataType.values().forEach { dataType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedDataType == dataType,
                                onClick = { onDataTypeSelected(dataType) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDataType == dataType,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dataType.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedDataType == dataType) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getDataTypeDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Estimated records: ${getEstimatedRecordCount()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FormatSelectionCard(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selectedFormat == format,
                                onClick = { onFormatSelected(format) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = format.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedFormat == format) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (selectedFormat) {
                    ExportFormat.CSV -> "Comma-separated values - compatible with Excel and other spreadsheet applications"
                    ExportFormat.JSON -> "JavaScript Object Notation - structured data format for technical users"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DateRangeSelectionCard(
    selectedDateRange: ExportDateRange,
    customStartDate: String?,
    customEndDate: String?,
    onDateRangeSelected: (ExportDateRange) -> Unit,
    onCustomDateRangeSet: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.selectableGroup()) {
                ExportDateRange.values().forEach { dateRange ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedDateRange == dateRange,
                                onClick = { onDateRangeSelected(dateRange) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDateRange == dateRange,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateRange.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedDateRange == dateRange) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            // Custom date range inputs (placeholder for now)
            if (selectedDateRange == ExportDateRange.CUSTOM_RANGE) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Custom date range selection will be implemented with date pickers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExportOptionsCard(
    includeMetadata: Boolean,
    onToggleIncludeMetadata: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Export Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeMetadata,
                    onCheckedChange = { onToggleIncludeMetadata() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Include metadata",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Export date, app version, and data source information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportButton(
    isLoading: Boolean,
    onExport: () -> Unit,
    estimatedRecords: Int
) {
    Button(
        onClick = onExport,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exporting...")
        } else {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Export"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export $estimatedRecords Records")
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
private fun ExportResultCard(
    result: ExportResult,
    onDismiss: () -> Unit,
    onShare: () -> Unit = {},
    onSaveToDownloads: () -> Unit = {},
    onEmailShare: () -> Unit = {}
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
                        text = if (result.success) "Export Successful" else "Export Failed",
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
                result.fileName?.let { fileName ->
                    Text(
                        text = "File: $fileName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "${result.recordCount} records exported",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // Sharing action buttons for successful exports
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    
                    OutlinedButton(
                        onClick = onEmailShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Email",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email")
                    }
                    
                    OutlinedButton(
                        onClick = onSaveToDownloads,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            } else {
                result.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}