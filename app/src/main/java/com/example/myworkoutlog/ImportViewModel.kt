package com.example.myworkoutlog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI State for Import Screen
data class ImportUiState(
    val isLoading: Boolean = false,
    val selectedFile: String? = null,
    val selectedUri: Uri? = null,
    val importMode: ImportMode = ImportMode.MERGE,
    val dataType: ImportDataType = ImportDataType.AUTO_DETECT,
    val skipDuplicates: Boolean = true,
    val allowSchemaUpgrade: Boolean = true,
    val importSummary: ImportSummary? = null,
    val validationReport: ValidationReport? = null,
    val importResult: ImportResult? = null,
    val error: String? = null,
    val progress: ImportProgress? = null
)

// Import progress tracking
data class ImportProgress(
    val stage: ImportStage,
    val message: String,
    val processed: Int = 0,
    val total: Int = 0
) {
    val progressPercentage: Int
        get() = if (total > 0) ((processed.toFloat() / total) * 100).toInt() else 0
}

enum class ImportStage {
    VALIDATING,
    PREPARING,
    IMPORTING,
    FINALIZING,
    COMPLETED,
    ERROR
}

class ImportViewModel(
    private val importRepository: ImportRepository,
    private val context: Context
) : ViewModel() {

    // --- UI State ---
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    // --- UI Actions ---

    fun selectFile(uri: Uri) {
        // Get display name for UI
        val displayName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    uri.toString()
                }
            } ?: uri.toString()
        } catch (e: Exception) {
            uri.toString()
        }
        
        _uiState.value = _uiState.value.copy(
            selectedFile = displayName,
            selectedUri = uri,
            importSummary = null,
            validationReport = null,
            importResult = null,
            error = null
        )
        
        // Automatically validate the selected file
        validateFile()
    }

    fun setImportMode(mode: ImportMode) {
        _uiState.value = _uiState.value.copy(importMode = mode)
    }

    fun setDataType(dataType: ImportDataType) {
        _uiState.value = _uiState.value.copy(dataType = dataType)
        
        // Re-validate if file is selected
        if (_uiState.value.selectedFile != null) {
            validateFile()
        }
    }

    fun setSkipDuplicates(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipDuplicates = skip)
    }

    fun setAllowSchemaUpgrade(allow: Boolean) {
        _uiState.value = _uiState.value.copy(allowSchemaUpgrade = allow)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearImportResult() {
        _uiState.value = _uiState.value.copy(
            importResult = null,
            progress = null
        )
    }

    // --- File Operations ---

    private fun validateFile() {
        val uri = _uiState.value.selectedUri ?: return
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            progress = ImportProgress(ImportStage.VALIDATING, "Validating file...")
        )

        viewModelScope.launch {
            try {
                val validationReport = withContext(Dispatchers.IO) {
                    importRepository.validateImportFile(context, uri, _uiState.value.dataType)
                }

                val importSummary = if (validationReport.isValid) {
                    withContext(Dispatchers.IO) {
                        importRepository.getImportSummary(context, uri)
                    }
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationReport = validationReport,
                    importSummary = importSummary,
                    error = if (!validationReport.isValid) {
                        validationReport.issues.firstOrNull { it.type == IssueType.ERROR }?.message
                    } else null,
                    progress = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Validation failed: ${e.message}",
                    progress = null
                )
            }
        }
    }

    fun performImport() {
        val currentState = _uiState.value
        val uri = currentState.selectedUri
        
        if (uri == null) {
            _uiState.value = currentState.copy(error = "No file selected")
            return
        }

        if (currentState.validationReport?.isValid != true) {
            _uiState.value = currentState.copy(error = "File validation failed")
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            error = null,
            importResult = null,
            progress = ImportProgress(ImportStage.PREPARING, "Preparing import...")
        )

        viewModelScope.launch {
            try {
                // Update progress
                _uiState.value = _uiState.value.copy(
                    progress = ImportProgress(ImportStage.IMPORTING, "Importing data...")
                )

                val importOptions = ImportOptions(
                    mode = currentState.importMode,
                    dataType = currentState.dataType,
                    uri = uri,
                    allowSchemaUpgrade = currentState.allowSchemaUpgrade,
                    skipDuplicates = currentState.skipDuplicates,
                    validateBeforeImport = true
                )

                val result = withContext(Dispatchers.IO) {
                    importRepository.importData(context, importOptions)
                }

                // Update progress to completed
                val finalProgress = if (result.success) {
                    ImportProgress(
                        ImportStage.COMPLETED, 
                        "Import completed successfully",
                        result.importedRecords,
                        result.importedRecords
                    )
                } else {
                    ImportProgress(
                        ImportStage.ERROR,
                        "Import failed"
                    )
                }

                _uiState.value = currentState.copy(
                    isLoading = false,
                    importResult = result,
                    error = if (!result.success) result.errors.firstOrNull() else null,
                    progress = finalProgress
                )

            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Import failed: ${e.message}",
                    progress = ImportProgress(ImportStage.ERROR, "Import failed")
                )
            }
        }
    }

    // --- Validation Only ---

    fun validateOnly() {
        val currentState = _uiState.value
        val uri = currentState.selectedUri
        
        if (uri == null) {
            _uiState.value = currentState.copy(error = "No file selected")
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            error = null,
            progress = ImportProgress(ImportStage.VALIDATING, "Validating import data...")
        )

        viewModelScope.launch {
            try {
                val importOptions = ImportOptions(
                    mode = ImportMode.VALIDATE_ONLY,
                    dataType = currentState.dataType,
                    uri = uri,
                    allowSchemaUpgrade = currentState.allowSchemaUpgrade,
                    skipDuplicates = currentState.skipDuplicates,
                    validateBeforeImport = true
                )

                val result = withContext(Dispatchers.IO) {
                    importRepository.importData(context, importOptions)
                }

                _uiState.value = currentState.copy(
                    isLoading = false,
                    importResult = result,
                    error = if (!result.success) result.errors.firstOrNull() else null,
                    progress = if (result.success) {
                        ImportProgress(ImportStage.COMPLETED, "Validation completed successfully")
                    } else {
                        ImportProgress(ImportStage.ERROR, "Validation failed")
                    }
                )

            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Validation failed: ${e.message}",
                    progress = ImportProgress(ImportStage.ERROR, "Validation failed")
                )
            }
        }
    }

    // --- Helper Functions ---

    fun getDataTypeDescription(): String {
        return when (_uiState.value.dataType) {
            ImportDataType.WORKOUTS -> "Import logged workouts with exercise details and performance data"
            ImportDataType.EXERCISES -> "Import exercise library with muscle groups and equipment information"
            ImportDataType.PERSONAL_RECORDS -> "Import personal record achievements and progress milestones"
            ImportDataType.PROGRAM_TEMPLATES -> "Import workout program templates and training plans"
            ImportDataType.WORKOUT_TEMPLATES -> "Import individual workout templates and exercise configurations"
            ImportDataType.COMPLETE_BACKUP -> "Import complete database backup with all data types"
            ImportDataType.AUTO_DETECT -> "Automatically detect data type from file content"
        }
    }

    fun getImportModeDescription(): String {
        return when (_uiState.value.importMode) {
            ImportMode.MERGE -> "Add new data while keeping existing records (recommended)"
            ImportMode.REPLACE -> "Replace existing data with imported data (destructive)"
            ImportMode.VALIDATE_ONLY -> "Only validate the import file without making changes"
        }
    }

    fun canPerformImport(): Boolean {
        val state = _uiState.value
        return !state.isLoading && 
               state.selectedFile != null && 
               state.validationReport?.isValid == true &&
               state.importMode != ImportMode.VALIDATE_ONLY
    }

    fun canValidateOnly(): Boolean {
        val state = _uiState.value
        return !state.isLoading && state.selectedFile != null
    }

    // --- File Info ---

    fun getFileInfo(): FileInfo? {
        val filePath = _uiState.value.selectedFile ?: return null
        val summary = _uiState.value.importSummary ?: return null
        
        return FileInfo(
            fileName = filePath.substringAfterLast('/'),
            filePath = filePath,
            dataType = summary.dataType,
            recordCount = summary.totalRecords,
            schemaVersion = summary.schemaVersion,
            appVersion = summary.appVersion,
            exportDate = summary.exportDate,
            isCompatible = summary.isCompatible
        )
    }
}

// File info data class
data class FileInfo(
    val fileName: String,
    val filePath: String,
    val dataType: ImportDataType,
    val recordCount: Int,
    val schemaVersion: String?,
    val appVersion: String?,
    val exportDate: String?,
    val isCompatible: Boolean
)

// Factory for creating ImportViewModel
class ImportViewModelFactory(
    private val importRepository: ImportRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImportViewModel(importRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}