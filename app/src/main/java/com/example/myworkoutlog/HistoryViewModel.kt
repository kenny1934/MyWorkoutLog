package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Data structures for mesocycle-aware history
data class CycleWithWorkouts(
    val cycleId: String,
    val cycle: ActiveProgramCycle?,
    val program: ProgramTemplate?,
    val workouts: List<LoggedWorkout>,
    val completionRate: Double,
    val totalWorkouts: Int,
    val startDate: String?,
    val userCycleName: String?
)

data class HistoryFilter(
    val mesocycleFilter: MesocycleFilter = MesocycleFilter.ALL,
    val programTemplateIds: List<String> = emptyList(),
    val showPRWorkoutsOnly: Boolean = false
)

enum class MesocycleFilter {
    ALL, CURRENT_CYCLE, COMPLETED_CYCLES, NO_CYCLE_CONTEXT
}

enum class HistoryViewMode {
    MESOCYCLES, CHRONOLOGICAL, EXERCISE_FOCUSED
}

// Enhanced ViewModel with mesocycle support
class HistoryViewModel(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val activeCycleDao: ActiveCycleDao,
    private val programDao: ProgramTemplateDao
) : ViewModel() {

    // Existing functionality
    val allLoggedWorkouts: StateFlow<List<LoggedWorkout>> = loggedWorkoutDao.getAllLoggedWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // New mesocycle-aware properties
    val activeCycle: StateFlow<ActiveProgramCycle?> = activeCycleDao.getActiveCycle()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeCycleWorkouts: StateFlow<List<LoggedWorkout>> = 
        combine(activeCycle, allLoggedWorkouts) { cycle, workouts ->
            if (cycle != null) {
                workouts.filter { it.activeProgramCycleId == cycle.id.toString() }
            } else {
                emptyList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val orphanedWorkouts: StateFlow<List<LoggedWorkout>> = loggedWorkoutDao.getOrphanedWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCycleIds: StateFlow<List<String>> = loggedWorkoutDao.getAllCycleIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Computed property: Completed cycles with their workouts
    val completedCycles: StateFlow<List<CycleWithWorkouts>> = 
        combine(allCycleIds, allLoggedWorkouts, activeCycle) { cycleIds, workouts, currentCycle ->
            val completedCycleIds = cycleIds.filter { cycleId ->
                currentCycle?.id?.toString() != cycleId
            }
            
            completedCycleIds.map { cycleId ->
                val cycleWorkouts = workouts.filter { it.activeProgramCycleId == cycleId }
                val totalWorkouts = cycleWorkouts.size
                val completionRate = if (totalWorkouts > 0) 1.0 else 0.0 // Simplified for now
                
                CycleWithWorkouts(
                    cycleId = cycleId,
                    cycle = null, // Will be populated when we have access to cycle history
                    program = null, // Will be populated when we have access to program data
                    workouts = cycleWorkouts,
                    completionRate = completionRate,
                    totalWorkouts = totalWorkouts,
                    startDate = cycleWorkouts.minByOrNull { it.date }?.date,
                    userCycleName = "Cycle $cycleId" // Simplified for now
                )
            }.sortedByDescending { it.startDate }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Existing functions
    fun getLoggedWorkoutById(id: String): Flow<LoggedWorkout?> {
        return loggedWorkoutDao.getLoggedWorkoutById(id)
    }

    // New mesocycle-specific functions
    fun getWorkoutsByCycle(cycleId: String): Flow<List<LoggedWorkout>> {
        return loggedWorkoutDao.getWorkoutsByCycle(cycleId)
    }

    fun getWorkoutsByProgramTemplate(programTemplateId: String): Flow<List<LoggedWorkout>> {
        return loggedWorkoutDao.getWorkoutsByProgramTemplate(programTemplateId)
    }
}

// The factory for creating our HistoryViewModel
class HistoryViewModelFactory(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val activeCycleDao: ActiveCycleDao,
    private val programDao: ProgramTemplateDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(loggedWorkoutDao, activeCycleDao, programDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}