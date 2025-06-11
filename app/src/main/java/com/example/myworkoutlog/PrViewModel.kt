// In PrViewModel.kt
package com.example.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class PrViewModel(personalRecordDao: PersonalRecordDao) : ViewModel() {

    // Holds the user's search text
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    // Holds the original, complete list of PRs from the database
    private val _allPRs = personalRecordDao.getAllPRs()

    // This new StateFlow combines the search text and the full PR list.
    // It produces a new, filtered list whenever either of them changes.
    val filteredPRs: StateFlow<List<PersonalRecord>> = _searchText
        .combine(_allPRs) { text, prs ->
            if (text.isBlank()) {
                prs // If search is empty, return the full list
            } else {
                // Otherwise, return PRs where the exercise name contains the search text
                prs.filter { it.exerciseName.contains(text, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Function for the UI to call when the user types in the search bar
    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }
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