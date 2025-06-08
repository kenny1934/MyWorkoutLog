package com.example.myworkoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// This ViewModel holds the state for an active workout session.
class WorkoutLoggerViewModel(
    private val templateDao: WorkoutTemplateDao,
    private val loggedWorkoutDao: LoggedWorkoutDao
) : ViewModel() {

    // A private mutable state flow to hold the in-progress workout
    private val _activeWorkoutState = MutableStateFlow<LoggedWorkout?>(null)
    // A public, read-only state flow for the UI to observe
    val activeWorkoutState: StateFlow<LoggedWorkout?> = _activeWorkoutState.asStateFlow()

    // This function starts a new workout based on a template
    fun startWorkoutFromTemplate(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            templateDao.getTemplateById(templateId).collect { template ->
                if (template != null) {
                    val loggedExercises = template.templateExercises.map { templateExercise ->
                        LoggedExercise(
                            id = UUID.randomUUID().toString(),
                            exerciseId = templateExercise.exerciseId,
                            exerciseName = templateExercise.exerciseName,
                            sets = templateExercise.sets.map { templateSet ->
                                // Create empty LoggedSet objects, but pre-fill targets from template
                                LoggedSet(
                                    id = UUID.randomUUID().toString(),
                                    // Actual performance is null initially
                                    reps = null,
                                    weight = null,
                                    // We can store the target in notes or dedicated fields later
                                    notes = "Target: ${templateSet.targetReps}"
                                )
                            }
                        )
                    }

                    val newLoggedWorkout = LoggedWorkout(
                        id = UUID.randomUUID().toString(),
                        name = template.name,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        loggedExercises = loggedExercises,
                        workoutTemplateId = template.id
                    )
                    _activeWorkoutState.value = newLoggedWorkout
                }
            }
        }
    }

    // Called when the user enters their performance for a set
    fun updateSet(exerciseId: String, setId: String, reps: String, weight: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(
                                        reps = reps.toIntOrNull(),
                                        weight = weight.toDoubleOrNull()
                                    )
                                } else {
                                    set
                                }
                            }
                        )
                    } else {
                        exercise
                    }
                }
            )
        }
    }

    // Saves the completed workout to the database
    fun finishWorkout() {
        viewModelScope.launch(Dispatchers.IO) {
            activeWorkoutState.value?.let {
                loggedWorkoutDao.insert(it)
                // Clear the state after saving
                _activeWorkoutState.value = null
            }
        }
    }

    fun updateBodyweight(bodyweight: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(bodyweight = bodyweight.toDoubleOrNull())
        }
    }
}

// The factory for creating our new ViewModel
class WorkoutLoggerViewModelFactory(
    private val templateDao: WorkoutTemplateDao,
    private val loggedWorkoutDao: LoggedWorkoutDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutLoggerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutLoggerViewModel(templateDao, loggedWorkoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}