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
                workouts.filter { it.activeProgramCycleId == cycle.cycleUuid }
                    .sortedByDescending { it.date }
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

    // Get all program templates for lookup
    val allPrograms: StateFlow<List<ProgramTemplate>> = programDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Computed property: Completed cycles with their workouts
    val completedCycles: StateFlow<List<CycleWithWorkouts>> = 
        combine(allCycleIds, allLoggedWorkouts, activeCycle, allPrograms) { cycleIds, workouts, currentCycle, programs ->
            val completedCycleIds = cycleIds.filter { cycleId ->
                currentCycle?.cycleUuid != cycleId
            }
            
            completedCycleIds.mapNotNull { cycleId ->
                val cycleWorkouts = workouts.filter { it.activeProgramCycleId == cycleId }
                val actualWorkouts = cycleWorkouts.size
                
                // Get the program template for accurate calculation
                val firstWorkout = cycleWorkouts.firstOrNull()
                val programTemplate = firstWorkout?.workoutTemplateId?.let { templateId ->
                    programs.find { program ->
                        program.weeks.any { week ->
                            week.sessions.any { session ->
                                session.workoutTemplateId == templateId
                            }
                        }
                    }
                }
                
                // Calculate total planned sessions from the program template
                val totalPlannedSessions = programTemplate?.let { program ->
                    program.weeks.sumOf { it.sessions.size }
                } ?: calculateEstimatedTotalSessions(cycleWorkouts)
                
                // Calculate accurate completion rate
                val completionRate = if (totalPlannedSessions > 0) {
                    (actualWorkouts.toDouble() / totalPlannedSessions.toDouble()).coerceAtMost(1.0)
                } else {
                    if (actualWorkouts > 0) 1.0 else 0.0
                }
                
                if (cycleWorkouts.isNotEmpty()) {
                    CycleWithWorkouts(
                        cycleId = cycleId,
                        cycle = null,
                        program = programTemplate,
                        workouts = cycleWorkouts.sortedByDescending { it.date },
                        completionRate = completionRate,
                        totalWorkouts = totalPlannedSessions,
                        startDate = cycleWorkouts.minByOrNull { it.date }?.date,
                        userCycleName = cycleWorkouts.firstOrNull()?.userCycleName ?: "Cycle $cycleId"
                    )
                } else null
            }.sortedByDescending { it.startDate }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Helper function to estimate total sessions for a cycle
    private fun calculateEstimatedTotalSessions(cycleWorkouts: List<LoggedWorkout>): Int {
        if (cycleWorkouts.isEmpty()) return 0
        
        // Group workouts by program template and week to estimate cycle structure
        val workoutsByTemplate = cycleWorkouts.groupBy { it.workoutTemplateId }
        val uniqueWeeks = cycleWorkouts.mapNotNull { it.programWeekDefinitionId }.distinct().size
        val uniqueSessions = cycleWorkouts.mapNotNull { it.programSessionDefinitionId }.distinct().size
        
        // If we have program structure data, use it
        if (uniqueWeeks > 0 && uniqueSessions > 0) {
            // Estimate based on program structure: weeks * sessions per week
            val estimatedSessionsPerWeek = (uniqueSessions.toDouble() / uniqueWeeks.toDouble()).toInt().coerceAtLeast(1)
            return uniqueWeeks * estimatedSessionsPerWeek
        }
        
        // Fallback: if no program structure, assume they completed what they logged
        // This is a conservative approach for legacy data
        return cycleWorkouts.size
    }

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