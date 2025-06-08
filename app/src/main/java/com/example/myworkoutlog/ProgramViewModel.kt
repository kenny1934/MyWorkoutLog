package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProgramViewModel(private val programDao: ProgramTemplateDao) : ViewModel() {

    val allPrograms: StateFlow<List<ProgramTemplate>> = programDao.getAllPrograms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
}

class ProgramViewModelFactory(
    private val programDao: ProgramTemplateDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgramViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgramViewModel(programDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}