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
    private val activeCycleDao: ActiveCycleDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CycleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CycleDetailViewModel(activeCycleDao, loggedWorkoutDao, personalRecordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
