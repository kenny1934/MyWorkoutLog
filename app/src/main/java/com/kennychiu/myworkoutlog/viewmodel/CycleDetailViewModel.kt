package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class CycleDetailUiState(
    val cycle: ActiveProgramCycle?,
    val aggregates: CycleAggregates,
) {
    companion object {
        val EMPTY = CycleDetailUiState(null, CycleAggregates.EMPTY)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CycleDetailViewModel(
    activeCycleDao: ActiveCycleDao,
    loggedWorkoutDao: LoggedWorkoutDao,
    personalRecordDao: PersonalRecordDao,
) : ViewModel() {

    val state: StateFlow<CycleDetailUiState> = activeCycleDao.getActiveCycle()
        .flatMapLatest { cycle ->
            if (cycle == null) {
                flowOf(CycleDetailUiState.EMPTY)
            } else {
                combine(
                    loggedWorkoutDao.getWorkoutsByCycle(cycle.cycleUuid),
                    personalRecordDao.getAllPRs(),
                ) { workouts, prs ->
                    CycleDetailUiState(cycle, cycleAggregates(cycle, workouts, prs))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CycleDetailUiState.EMPTY,
        )
}

class CycleDetailViewModelFactory(
    private val activeCycleDao: ActiveCycleDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleDetailViewModel(activeCycleDao, loggedWorkoutDao, personalRecordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
