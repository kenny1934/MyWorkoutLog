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

class VolumeViewModel(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val programTemplateDao: ProgramTemplateDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModel() {

    // --- State for the UI ---
    // Holds the currently selected week. Can be null if no cycle is active.
    private val _selectedWeek = MutableStateFlow<ProgramWeekDefinition?>(null)
    val selectedWeek: StateFlow<ProgramWeekDefinition?> = _selectedWeek

    // This flow will provide the list of all weeks in the current cycle to the UI dropdown
    val weeksInActiveCycle: StateFlow<List<ProgramWeekDefinition>> = combine(
        activeCycleDao.getActiveCycle(),
        programTemplateDao.getAllPrograms()
    ) { activeCycle, programs ->
        if (activeCycle != null) {
            val program = programs.find { it.id == activeCycle.programTemplateId }
            program?.weeks?.sortedBy { it.order } ?: emptyList()
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val volumeData: StateFlow<Map<MuscleGroup, Int>> = combine(
        loggedWorkoutDao.getAllLoggedWorkouts(),
        exerciseDao.getAllExercises(),
        activeCycleDao.getActiveCycle(),
        _selectedWeek
    ) { loggedWorkouts, exercises, activeCycle, week ->
        val volumeMap = mutableMapOf<MuscleGroup, Int>()
        if (activeCycle == null || week == null) return@combine emptyMap()

        // 1. Get the session definitions for the selected week.
        val sessionsInSelectedWeek = week.sessions

        // 2. From the active cycle, find which of these sessions have been completed.
        //    This gives us the IDs of the logged workouts for THIS week only.
        val completedWorkoutIdsForWeek = activeCycle.completedSessions
            .filterKeys { key ->
                // The key is "weekId_sessionId". We check if the weekId matches.
                key.startsWith(week.id)
            }
            .values

        // 3. Filter the main list of all logged workouts to get the specific workout objects for this week.
        val workoutsInWeek = loggedWorkouts.filter { it.id in completedWorkoutIdsForWeek }

        val exerciseMap = exercises.associateBy { it.id }

        // 4. Calculate volume only for the workouts in the selected week.
        workoutsInWeek.forEach { workout ->
            workout.loggedExercises.forEach { loggedExercise ->
                val muscleGroups = exerciseMap[loggedExercise.exerciseId]?.targetMuscleGroups ?: emptyList()
                val setCount = loggedExercise.sets.size
                muscleGroups.forEach { muscleGroup ->
                    volumeMap[muscleGroup] = (volumeMap[muscleGroup] ?: 0) + setCount
                }
            }
        }
        volumeMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    fun onWeekSelected(week: ProgramWeekDefinition?) {
        _selectedWeek.value = week
    }
}


class VolumeViewModelFactory(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val programTemplateDao: ProgramTemplateDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VolumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VolumeViewModel(loggedWorkoutDao, exerciseDao, programTemplateDao, activeCycleDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}