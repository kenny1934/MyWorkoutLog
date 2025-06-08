package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ActiveCycleViewModel(private val activeCycleDao: ActiveCycleDao) : ViewModel() {

    val activeCycle: StateFlow<ActiveProgramCycle?> = activeCycleDao.getActiveCycle()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun startCycle(program: ProgramTemplate, cycleName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCycle = ActiveProgramCycle(
                programTemplateId = program.id,
                programTemplateName = program.name,
                userCycleName = cycleName,
                startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                completedSessions = emptyMap()
            )
            activeCycleDao.setActiveCycle(newCycle)
        }
    }

    fun endCycle() {
        viewModelScope.launch(Dispatchers.IO) {
            activeCycleDao.clear()
        }
    }
}

class ActiveCycleViewModelFactory(
    private val activeCycleDao: ActiveCycleDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveCycleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveCycleViewModel(activeCycleDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}