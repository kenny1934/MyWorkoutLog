package com.example.myworkoutlog

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// The ViewModel for managing WorkoutTemplates.
class WorkoutTemplateViewModel(private val templateDao: WorkoutTemplateDao) : ViewModel() {

    // Expose a stream of all templates from the database.
    // The UI will observe this to stay up-to-date.
    val allTemplates: StateFlow<List<WorkoutTemplate>> = templateDao.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Function to add a new template to the database.
    fun insert(templateName: String, description: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTemplate = WorkoutTemplate(
                id = UUID.randomUUID().toString(),
                name = templateName,
                description = description,
                templateExercises = emptyList() // Starts with no exercises
            )
            templateDao.insert(newTemplate)
        }
    }

    // Function to update an existing template (e.g., after adding exercises to it).
    fun update(template: WorkoutTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.update(template)
        }
    }

    // Function to delete a template.
    fun deleteById(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.deleteById(templateId)
        }
    }
}


// The Factory to create our WorkoutTemplateViewModel.
class WorkoutTemplateViewModelFactory(private val templateDao: WorkoutTemplateDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutTemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutTemplateViewModel(templateDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}