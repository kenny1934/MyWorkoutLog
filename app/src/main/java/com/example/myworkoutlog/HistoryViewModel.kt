package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// This ViewModel provides the list of all saved workouts.
class HistoryViewModel(private val loggedWorkoutDao: LoggedWorkoutDao) : ViewModel() {


    val allLoggedWorkouts: StateFlow<List<LoggedWorkout>> = loggedWorkoutDao.getAllLoggedWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getLoggedWorkoutById(id: String): Flow<LoggedWorkout?> {
        return loggedWorkoutDao.getLoggedWorkoutById(id)
    }
}

// The factory for creating our HistoryViewModel
class HistoryViewModelFactory(
    private val loggedWorkoutDao: LoggedWorkoutDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(loggedWorkoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}