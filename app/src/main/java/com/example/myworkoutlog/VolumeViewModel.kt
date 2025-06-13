// In VolumeViewModel.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VolumeViewModel(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    // Get all logged workouts and all master exercises
    private val _allLoggedWorkouts = loggedWorkoutDao.getAllLoggedWorkouts()
    private val _allExercises = exerciseDao.getAllExercises()

    // Combine the two flows to calculate volume
    val weeklyVolume: StateFlow<Map<MuscleGroup, Int>> = combine(
        _allLoggedWorkouts,
        _allExercises
    ) { loggedWorkouts, exercises ->
        val volumeMap = mutableMapOf<MuscleGroup, Int>()
        val oneWeekAgo = LocalDate.now().minusWeeks(1)

        // Filter for workouts in the last week
        val recentWorkouts = loggedWorkouts.filter {
            LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE).isAfter(oneWeekAgo)
        }

        // Create a map for quick exercise lookups
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