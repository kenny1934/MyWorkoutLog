// In PrViewModel.kt
package com.kennychiu.myworkoutlog

// --- IMPORTS ---
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Data class for exercise groupings in master panel
data class ExerciseGroup(
    val exerciseId: String,
    val exerciseName: String,
    val prCount: Int,
    val latestDate: String?,
    val prs: List<PersonalRecord>
)

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

    // Selection state for master-detail layout
    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    val selectedExerciseId: StateFlow<String?> = _selectedExerciseId

    // Get PRs for the selected exercise
    val selectedExercisePRs: StateFlow<List<PersonalRecord>> = combine(
        selectedExerciseId,
        filteredPRs
    ) { exerciseId, allPRs ->
        if (exerciseId != null) {
            allPRs.filter { it.exerciseId == exerciseId }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Get exercise names grouped by exercise for master panel
    val exerciseGroups: StateFlow<List<ExerciseGroup>> = filteredPRs.map { prs ->
        val grouped = prs.groupBy { it.exerciseId to it.exerciseName }
        grouped.map { (exerciseIdName, prsForExercise) ->
            val (exerciseId, exerciseName) = exerciseIdName
            val latestPR = prsForExercise.maxByOrNull { it.date }
            ExerciseGroup(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                prCount = prsForExercise.size,
                latestDate = latestPR?.date,
                prs = prsForExercise
            )
        }.sortedBy { it.exerciseName }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Function for the UI to call when the user types in the search bar
    fun onSearchTextChanged(text: String) {
        _searchText.value = text
        // Clear selection when search changes to avoid showing irrelevant data
        if (text.isNotBlank()) {
            _selectedExerciseId.value = null
        }
    }

    // Function to select an exercise in master-detail layout
    fun selectExercise(exerciseId: String) {
        _selectedExerciseId.value = exerciseId
    }

    // Function to clear exercise selection
    fun clearSelection() {
        _selectedExerciseId.value = null
    }

    // Auto-select first exercise for large screens
    fun autoSelectFirstExercise() {
        val currentGroups = exerciseGroups.value
        if (currentGroups.isNotEmpty() && _selectedExerciseId.value == null) {
            _selectedExerciseId.value = currentGroups.first().exerciseId
        }
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