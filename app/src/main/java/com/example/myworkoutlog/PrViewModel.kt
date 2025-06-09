// In PrViewModel.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PrViewModel(personalRecordDao: PersonalRecordDao) : ViewModel() {

    // Expose a stream of all PRs from the database, ordered by date
    val allPRs: StateFlow<List<PersonalRecord>> = personalRecordDao.getAllPRs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}

class PrViewModelFactory(
    private val personalRecordDao: PersonalRecordDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrViewModel(personalRecordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}