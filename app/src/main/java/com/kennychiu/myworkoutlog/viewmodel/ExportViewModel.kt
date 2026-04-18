package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// UI State for Export Screen
data class ExportUiState(
    val isLoading: Boolean = false,
    val selectedFormat: ExportFormat = ExportFormat.CSV,
    val selectedDataType: ExportDataType = ExportDataType.WORKOUTS,
    val selectedDateRange: ExportDateRange = ExportDateRange.ALL_TIME,
    val customStartDate: String? = null,
    val customEndDate: String? = null,
    val includeMetadata: Boolean = true,
    val exportSummary: ExportSummary? = null,
    val lastExportResult: ExportResult? = null,
    val error: String? = null
)

// Date range options for export
enum class ExportDateRange {
    ALL_TIME,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    THIS_YEAR,
    CUSTOM_RANGE
}

class ExportViewModel(
    private val exportRepository: ExportRepository
) : ViewModel() {

    // --- UI State ---
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState

    // Export summary data
    val exportSummary: StateFlow<ExportSummary?> = exportRepository.getExportSummaryFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        loadExportSummary()
    }

    // --- UI Actions ---

    fun selectFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun selectDataType(dataType: ExportDataType) {
        _uiState.value = _uiState.value.copy(selectedDataType = dataType)
    }

    fun selectDateRange(dateRange: ExportDateRange) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = dateRange,
            customStartDate = if (dateRange != ExportDateRange.CUSTOM_RANGE) null else _uiState.value.customStartDate,
            customEndDate = if (dateRange != ExportDateRange.CUSTOM_RANGE) null else _uiState.value.customEndDate
        )
    }

    fun setCustomDateRange(startDate: String, endDate: String) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = ExportDateRange.CUSTOM_RANGE,
            customStartDate = startDate,
            customEndDate = endDate
        )
    }

    fun toggleIncludeMetadata() {
        _uiState.value = _uiState.value.copy(
            includeMetadata = !_uiState.value.includeMetadata
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearLastExportResult() {
        _uiState.value = _uiState.value.copy(lastExportResult = null)
    }

    // --- Export Operations ---

    fun exportData() {
        val currentState = _uiState.value
        
        // Validate custom date range if selected
        if (currentState.selectedDateRange == ExportDateRange.CUSTOM_RANGE) {
            if (currentState.customStartDate.isNullOrBlank() || currentState.customEndDate.isNullOrBlank()) {
                _uiState.value = currentState.copy(error = "Please select both start and end dates for custom range")
                return
            }
        }

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val dateRange = getDateRangeForExport(currentState.selectedDateRange, currentState.customStartDate, currentState.customEndDate)
                
                val exportOptions = ExportOptions(
                    format = currentState.selectedFormat,
                    dataType = currentState.selectedDataType,
                    dateRange = dateRange,
                    includeMetadata = currentState.includeMetadata
                )

                val result = withContext(Dispatchers.IO) {
                    exportRepository.exportData(exportOptions)
                }

                _uiState.value = currentState.copy(
                    isLoading = false,
                    lastExportResult = result,
                    error = if (!result.success) result.error else null
                )

            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    // --- Private Helper Functions ---

    private fun loadExportSummary() {
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    exportRepository.getExportSummary()
                }
                _uiState.value = _uiState.value.copy(exportSummary = summary)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load export summary: ${e.message}"
                )
            }
        }
    }

    private fun getDateRangeForExport(
        exportDateRange: ExportDateRange,
        customStartDate: String?,
        customEndDate: String?
    ): DateRange? {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return when (exportDateRange) {
            ExportDateRange.ALL_TIME -> null // No date filter
            ExportDateRange.LAST_30_DAYS -> {
                val startDate = today.minusDays(30).format(formatter)
                val endDate = today.format(formatter)
                DateRange(startDate, endDate)
            }
            ExportDateRange.LAST_3_MONTHS -> {
                val startDate = today.minusMonths(3).format(formatter)
                val endDate = today.format(formatter)
                DateRange(startDate, endDate)
            }
            ExportDateRange.LAST_6_MONTHS -> {
                val startDate = today.minusMonths(6).format(formatter)
                val endDate = today.format(formatter)
                DateRange(startDate, endDate)
            }
            ExportDateRange.THIS_YEAR -> {
                val startDate = today.withDayOfYear(1).format(formatter)
                val endDate = today.format(formatter)
                DateRange(startDate, endDate)
            }
            ExportDateRange.CUSTOM_RANGE -> {
                if (!customStartDate.isNullOrBlank() && !customEndDate.isNullOrBlank()) {
                    DateRange(customStartDate, customEndDate)
                } else null
            }
        }
    }

    // --- Data Summary Helpers ---

    fun getEstimatedRecordCount(): Int {
        val summary = _uiState.value.exportSummary ?: return 0
        
        return when (_uiState.value.selectedDataType) {
            ExportDataType.WORKOUTS -> summary.totalWorkouts
            ExportDataType.EXERCISES -> summary.totalExercises
            ExportDataType.PERSONAL_RECORDS -> summary.totalPersonalRecords
            ExportDataType.PROGRAM_TEMPLATES -> summary.totalPrograms
            ExportDataType.COMPLETE_BACKUP -> {
                summary.totalWorkouts + summary.totalExercises + 
                summary.totalPersonalRecords + summary.totalPrograms
            }
        }
    }

    fun getDataTypeDescription(): String {
        return when (_uiState.value.selectedDataType) {
            ExportDataType.WORKOUTS -> "All logged workouts with exercise details, sets, reps, and weights"
            ExportDataType.EXERCISES -> "Exercise library with muscle groups and equipment information"
            ExportDataType.PERSONAL_RECORDS -> "Personal record achievements across all exercises"
            ExportDataType.PROGRAM_TEMPLATES -> "Workout program templates and training plans"
            ExportDataType.COMPLETE_BACKUP -> "Complete database backup including all data types"
        }
    }
}

// Extension function to create Flow for export summary
private fun ExportRepository.getExportSummaryFlow() = kotlinx.coroutines.flow.flow {
    emit(getExportSummary())
}

// Factory for creating ExportViewModel
class ExportViewModelFactory(
    private val exportRepository: ExportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExportViewModel(exportRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}