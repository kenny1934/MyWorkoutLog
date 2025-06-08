package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProgramViewModel(
    private val programDao: ProgramTemplateDao,
    private val templateDao: WorkoutTemplateDao
) : ViewModel() {

    val allPrograms: StateFlow<List<ProgramTemplate>> = programDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // NEW: Expose all workout templates so the UI can access them
    val allWorkoutTemplates: StateFlow<List<WorkoutTemplate>> = templateDao.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // NEW: Get a single program
    fun getProgramById(id: String): Flow<ProgramTemplate?> {
        return programDao.getProgramById(id)
    }

    fun insert(programName: String, description: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val newProgram = ProgramTemplate(
                id = UUID.randomUUID().toString(),
                name = programName,
                description = description,
                weeks = emptyList() // A new program starts with no weeks
            )
            programDao.insert(newProgram)
        }
    }

    // NEW: Update an existing program
    fun update(program: ProgramTemplate) {
        viewModelScope.launch(Dispatchers.IO) {
            programDao.update(program)
        }
    }
}

class ProgramViewModelFactory(
    private val programDao: ProgramTemplateDao,
    private val templateDao: WorkoutTemplateDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgramViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgramViewModel(programDao, templateDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}