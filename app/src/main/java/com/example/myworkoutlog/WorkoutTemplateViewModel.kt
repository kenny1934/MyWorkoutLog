package com.example.myworkoutlog

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// The ViewModel for managing WorkoutTemplates.
// It now takes BOTH DAOs because it needs to access templates and exercises.
class WorkoutTemplateViewModel(
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao
) : ViewModel() {

    val allTemplates: StateFlow<List<WorkoutTemplate>> = templateDao.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // We also expose the full list of master exercises to the UI
    val allMasterExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // NEW: A function to get a single template by its ID.
    // This will be used by our detail screen.
    fun getTemplateById(id: String): Flow<WorkoutTemplate?> {
        return templateDao.getTemplateById(id)
    }


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

    fun update(template: WorkoutTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.update(template)
        }
    }

    fun deleteById(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.deleteById(templateId)
        }
    }
}


// The Factory now needs to accept BOTH DAOs.
class WorkoutTemplateViewModelFactory(
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutTemplateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutTemplateViewModel(templateDao, exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}