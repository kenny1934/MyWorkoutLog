package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CycleDetailUiState(
    val cycle: ActiveProgramCycle?,
    val aggregates: CycleAggregates,
    val templates: Map<String, WorkoutTemplate> = emptyMap(),
    // Per-exercise top set logged at each 1-based cycle week. Feeds the adaptive
    // projection surfaced on each week card — a missed or under-planned week
    // anchors downstream projections off what actually happened.
    val actualsByExerciseWeek: Map<String, Map<Int, ExerciseTopSet>> = emptyMap(),
    // Pre-cycle top set per exercise — anchors the forward projection on week 1
    // so weeks before the first intra-cycle log read real numbers instead of "—".
    val baselinesByExercise: Map<String, ExerciseTopSet> = emptyMap(),
) {
    companion object {
        val EMPTY = CycleDetailUiState(null, CycleAggregates.EMPTY)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CycleDetailViewModel(
    private val activeCycleDao: ActiveCycleDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    personalRecordDao: PersonalRecordDao,
    workoutTemplateDao: WorkoutTemplateDao,
) : ViewModel() {

    val state: StateFlow<CycleDetailUiState> = activeCycleDao.getActiveCycle()
        .flatMapLatest { cycle ->
            if (cycle == null) {
                flowOf(CycleDetailUiState.EMPTY)
            } else {
                combine(
                    loggedWorkoutDao.getWorkoutsByCycle(cycle.cycleUuid),
                    personalRecordDao.getAllPRs(),
                    workoutTemplateDao.getAllTemplates(),
                ) { workouts, prs, templates ->
                    val templatesById = templates.associateBy { it.id }
                    val baselines = withContext(Dispatchers.IO) {
                        cycleBaselinesByExercise(cycle, templatesById) { exerciseId ->
                            loggedWorkoutDao.getLatestWorkoutWithExerciseBefore(exerciseId, cycle.startDate)
                        }
                    }
                    CycleDetailUiState(
                        cycle = cycle,
                        aggregates = cycleAggregates(cycle, workouts, prs),
                        templates = templatesById,
                        actualsByExerciseWeek = cycleActualsByExerciseAndWeek(cycle, workouts),
                        baselinesByExercise = baselines,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CycleDetailUiState.EMPTY,
        )

    fun renameActiveCycle(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val cycleUuid = state.value.cycle?.cycleUuid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            activeCycleDao.renameActiveCycle(trimmed)
            loggedWorkoutDao.renameLoggedWorkoutsByCycle(cycleUuid, trimmed)
        }
    }
}

class CycleDetailViewModelFactory(
    private val activeCycleDao: ActiveCycleDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleDetailViewModel(
                activeCycleDao,
                loggedWorkoutDao,
                personalRecordDao,
                workoutTemplateDao,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
