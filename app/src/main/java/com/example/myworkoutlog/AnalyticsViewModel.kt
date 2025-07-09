package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModel() {

    // --- UI State ---
    private val _selectedTimeRange = MutableStateFlow(TimeRange.LAST_30_DAYS)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    val selectedExerciseId: StateFlow<String?> = _selectedExerciseId

    private val _selectedCycleId = MutableStateFlow<String?>(null)
    val selectedCycleId: StateFlow<String?> = _selectedCycleId

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- Analytics Data ---
    val volumeProgressionData: StateFlow<List<VolumeDataPoint>> = combine(
        _selectedTimeRange,
        _selectedExerciseId
    ) { timeRange, exerciseId ->
        val (startDate, endDate) = getDateRangeForTimeRange(timeRange)
        android.util.Log.d("AnalyticsViewModel", "=== VOLUME DATA DEBUG ===")
        android.util.Log.d("AnalyticsViewModel", "TimeRange: $timeRange")
        android.util.Log.d("AnalyticsViewModel", "ExerciseId: $exerciseId")
        android.util.Log.d("AnalyticsViewModel", "Date range: $startDate to $endDate")
        Pair(startDate to endDate, exerciseId)
    }.flatMapLatest { (dateRange, exerciseId) ->
        android.util.Log.d("AnalyticsViewModel", "Calling getVolumeProgressionData with: ${dateRange.first} to ${dateRange.second}, exerciseId: $exerciseId")
        analyticsRepository.getVolumeProgressionData(dateRange.first, dateRange.second, exerciseId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyVolumeSummary: StateFlow<VolumeSummary?> = _selectedTimeRange.flatMapLatest { timeRange ->
        if (timeRange == TimeRange.THIS_WEEK) {
            val startDate = getStartOfCurrentWeek()
            analyticsRepository.getWeeklyVolumeSummary(startDate)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val muscleGroupDistribution: StateFlow<List<MuscleGroupVolume>> = _selectedTimeRange.flatMapLatest { timeRange ->
        val (startDate, endDate) = getDateRangeForTimeRange(timeRange)
        analyticsRepository.getMuscleGroupVolumeDistribution(startDate, endDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisePerformanceTrend: StateFlow<PerformanceTrend?> = _selectedExerciseId.flatMapLatest { exerciseId ->
        if (exerciseId != null) {
            analyticsRepository.getExercisePerformanceTrend(exerciseId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val personalRecordProgress: StateFlow<PersonalRecordProgress?> = _selectedExerciseId.flatMapLatest { exerciseId ->
        if (exerciseId != null) {
            analyticsRepository.getPersonalRecordProgress(exerciseId)
                .catch { emit(null) } // Handle any errors gracefully
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cycleComparison: StateFlow<CycleComparison?> = activeCycleDao.getActiveCycle().flatMapLatest { activeCycle ->
        if (activeCycle != null) {
            analyticsRepository.compareCycles(activeCycle.cycleUuid)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Available exercises for selection
    val availableExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User Actions ---
    fun selectTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
    }

    fun selectExercise(exerciseId: String?) {
        _selectedExerciseId.value = exerciseId
    }
    
    fun selectCycle(cycleId: String?) {
        _selectedCycleId.value = cycleId
    }
    
    fun selectMuscleGroup(muscleGroup: String?) {
        _selectedMuscleGroup.value = muscleGroup
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Force refresh by emitting current values
                _selectedTimeRange.value = _selectedTimeRange.value
                _selectedExerciseId.value = _selectedExerciseId.value
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Helper Functions ---
    private fun getDateRangeForTimeRange(timeRange: TimeRange): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        return when (timeRange) {
            TimeRange.THIS_WEEK -> {
                val startOfWeek = getStartOfCurrentWeek()
                startOfWeek to today.format(formatter)
            }
            TimeRange.LAST_30_DAYS -> {
                val startDate = today.minusDays(30)
                startDate.format(formatter) to today.format(formatter)
            }
            TimeRange.LAST_3_MONTHS -> {
                val startDate = today.minusMonths(3)
                startDate.format(formatter) to today.format(formatter)
            }
            TimeRange.LAST_6_MONTHS -> {
                val startDate = today.minusMonths(6)
                startDate.format(formatter) to today.format(formatter)
            }
            TimeRange.THIS_YEAR -> {
                val startOfYear = LocalDate.of(today.year, 1, 1)
                startOfYear.format(formatter) to today.format(formatter)
            }
            TimeRange.ALL_TIME -> {
                // Use a very early date for all time
                "2020-01-01" to today.format(formatter)
            }
        }
    }

    private fun getStartOfCurrentWeek(): String {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
        return startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

enum class TimeRange(val displayName: String) {
    THIS_WEEK("This Week"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

class AnalyticsViewModelFactory(
    private val analyticsRepository: AnalyticsRepository,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(analyticsRepository, exerciseDao, activeCycleDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}