// In VolumeViewModel.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// Enum to represent the different timeframes the user can select
enum class VolumeTimeframe(val displayName: String) {
    THIS_WEEK("This Week"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days")
}


class VolumeViewModel(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    // Get all logged workouts and all master exercises
    private val _allLoggedWorkouts = loggedWorkoutDao.getAllLoggedWorkouts()
    private val _allExercises = exerciseDao.getAllExercises()

    // NEW: A state to hold the user's selected timeframe. Defaults to This Week.
    private val _selectedTimeframe = MutableStateFlow(VolumeTimeframe.THIS_WEEK)
    val selectedTimeframe: StateFlow<VolumeTimeframe> = _selectedTimeframe

    // This powerful flow combines three sources. It will automatically re-run its calculation
    // whenever the workouts, exercises, or the selected timeframe changes.
    val volumeData: StateFlow<Map<MuscleGroup, Int>> = combine(
        _allLoggedWorkouts,
        _allExercises,
        _selectedTimeframe
    ) { loggedWorkouts, exercises, timeframe ->
        val volumeMap = mutableMapOf<MuscleGroup, Int>()
        val now = LocalDate.now()

        // Determine the date range based on the selected timeframe
        val startDate = when (timeframe) {
            VolumeTimeframe.THIS_WEEK -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            VolumeTimeframe.LAST_7_DAYS -> now.minusDays(6)
            VolumeTimeframe.THIS_MONTH -> now.withDayOfMonth(1)
            VolumeTimeframe.LAST_30_DAYS -> now.minusDays(29)
        }

        // Filter for workouts within the calculated date range
        val recentWorkouts = loggedWorkouts.filter {
            !LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(startDate)
        }

        val exerciseMap = exercises.associateBy { it.id }

        // Calculate volume
        recentWorkouts.forEach { workout ->
            workout.loggedExercises.forEach { loggedExercise ->
                val muscleGroups = exerciseMap[loggedExercise.exerciseId]?.targetMuscleGroups ?: emptyList()
                val setCount = loggedExercise.sets.size
                muscleGroups.forEach { muscleGroup ->
                    volumeMap[muscleGroup] = (volumeMap[muscleGroup] ?: 0) + setCount
                }
            }
        }
        volumeMap
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // NEW: Function for the UI to call when a new timeframe is selected
    fun onTimeframeSelected(timeframe: VolumeTimeframe) {
        _selectedTimeframe.value = timeframe
    }
}

class VolumeViewModelFactory(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VolumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VolumeViewModel(loggedWorkoutDao, exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}