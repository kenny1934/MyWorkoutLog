package com.example.myworkoutlog // Your package name

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// This is the ViewModel. It's the bridge between the UI and the data layer (database).
class ExerciseViewModel(private val exerciseDao: ExerciseDao) : ViewModel() {

    // Get all exercises from the DAO and expose them as a StateFlow.
    // The UI will collect this flow and automatically update when the data changes.
    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // This function will be called from our UI to add a new exercise.
    fun insert(exerciseName: String, equipmentString: String, usesBodyweight: Boolean) {
        // viewModelScope.launch runs this code in a background coroutine
        // so we don't block the UI thread with database operations.
        viewModelScope.launch(Dispatchers.IO) {
            val newExercise = Exercise(
                id = UUID.randomUUID().toString(),
                name = exerciseName,
                usesBodyweight = usesBodyweight,
                // A simple, but not very robust, way to handle equipment for now.
                // We'll improve this later.
                equipment = try {
                    listOf(Equipment.valueOf(equipmentString.uppercase().trim()))
                } catch (e: IllegalArgumentException) {
                    listOf(Equipment.OTHER)
                },
                targetMuscleGroups = listOf(MuscleGroup.OTHER)
            )
            exerciseDao.insert(newExercise)
        }
    }

    // NEW function for updating
    fun update(exercise: Exercise) {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseDao.update(exercise)
        }
    }

    // NEW function for deleting
    fun delete(exercise: Exercise) {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseDao.delete(exercise)
        }
    }
}


// This is the ViewModelFactory. Its job is to create an instance of our ExerciseViewModel.
class ExerciseViewModelFactory(private val exerciseDao: ExerciseDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is our ExerciseViewModel
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExerciseViewModel(exerciseDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}