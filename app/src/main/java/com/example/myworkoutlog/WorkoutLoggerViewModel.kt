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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// This ViewModel holds the state for an active workout session.
class WorkoutLoggerViewModel(
    private val templateDao: WorkoutTemplateDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModel() {

    // A private mutable state flow to hold the in-progress workout
    private val _activeWorkoutState = MutableStateFlow<LoggedWorkout?>(null)
    // A public, read-only state flow for the UI to observe
    val activeWorkoutState: StateFlow<LoggedWorkout?> = _activeWorkoutState.asStateFlow()

    // --- REFACTORED TIMER/STOPWATCH LOGIC ---
    private var workoutStartTimeMillis: Long = 0L

    // This flow now calculates the elapsed time based on the start time
    val sessionElapsedTime: StateFlow<Int> = flow {
        while (true) {
            if (workoutStartTimeMillis > 0) {
                val elapsed = (System.currentTimeMillis() - workoutStartTimeMillis) / 1000
                emit(elapsed.toInt())
            } else {
                emit(0)
            }
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- UPDATED: Rest Timer State ---
    private var restTimerJob: Job? = null
    private val _timerValueSeconds = MutableStateFlow(0)
    val timerValueSeconds: StateFlow<Int> = _timerValueSeconds.asStateFlow()
    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    // --- TIMER AND STOPWATCH CONTROLS ---

    fun startRestTimer(durationSeconds: Int = 90) {
        restTimerJob?.cancel()
        // If a duration is passed, start from that. Otherwise, continue from the current value.
        if (_timerValueSeconds.value == 0 || durationSeconds != _timerValueSeconds.value) {
            _timerValueSeconds.value = durationSeconds
        }
        _timerIsRunning.value = true

        restTimerJob = viewModelScope.launch {
            flow {
                for (i in _timerValueSeconds.value downTo 0) {
                    emit(i)
                    delay(1000)
                }
            }.collect {
                _timerValueSeconds.value = it
                if (it == 0) {
                    _timerIsRunning.value = false
                }
            }
        }
    }

    // NEW: Pause function
    fun pauseRestTimer() {
        restTimerJob?.cancel()
        _timerIsRunning.value = false
    }

    // NEW: Resume function
    fun resumeRestTimer() {
        // Simply call startRestTimer again, it will pick up from the current value
        if (!_timerIsRunning.value && _timerValueSeconds.value > 0) {
            startRestTimer(_timerValueSeconds.value)
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _timerIsRunning.value = false
        _timerValueSeconds.value = 0
    }

    fun resetRestTimer(durationSeconds: Int = 90) {
        stopRestTimer()
        startRestTimer(durationSeconds)
    }

    fun addTimeToRestTimer(secondsToAdd: Int) {
        val newDuration = _timerValueSeconds.value + secondsToAdd
        startRestTimer(newDuration)
    }

    // This function starts a new workout based on a template
    fun startWorkoutFromTemplate(
        templateId: String,
        cycleId: String?,
        weekId: String?,
        sessionId: String?
    ) {
        // When starting a new workout, ensure any old timer is stopped.
        stopRestTimer()
        workoutStartTimeMillis = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            templateDao.getTemplateById(templateId).collect { template ->
                if (template != null) {
                    val loggedExercises = template.templateExercises.map { templateExercise ->
                        LoggedExercise(
                            id = UUID.randomUUID().toString(),
                            exerciseId = templateExercise.exerciseId,
                            exerciseName = templateExercise.exerciseName,
                            targetMuscleGroups = templateExercise.targetMuscleGroups,
                            equipment = templateExercise.equipment,
                            sets = templateExercise.sets.map { templateSet ->
                                // Create empty LoggedSet objects, but pre-fill targets from template
                                LoggedSet(
                                    id = UUID.randomUUID().toString(),
                                    // Actual performance is null initially
                                    reps = null,
                                    weight = null,
                                    secs = null,
                                    targetReps = templateSet.targetReps,
                                    targetSecs = templateSet.targetSecs
                                )
                            },
                            isSubstitute = false
                        )
                    }

                    val newLoggedWorkout = LoggedWorkout(
                        id = UUID.randomUUID().toString(),
                        name = template.name,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        startTimestamp = workoutStartTimeMillis, // Save start time
                        endTimestamp = null, // End time is null for now
                        performedWeightUnit = null,
                        bodyweight = null,
                        activeProgramCycleId = cycleId, // Save the cycle context
                        programWeekDefinitionId = weekId, // Save the week context
                        programSessionDefinitionId = sessionId, // Save the session context
                        userCycleName = null, // Will be set when workout is finished
                        loggedExercises = loggedExercises,
                        workoutTemplateId = template.id
                    )
                    _activeWorkoutState.value = newLoggedWorkout
                }
            }
        }
    }

    // Called when the user enters their performance for a set
    fun updateSet(
        exerciseId: String, 
        setId: String, 
        reps: String, 
        weight: Double?, 
        secs: String, 
        rir: String? = null, 
        bands: String? = null, 
        notes: String? = null
    ) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(
                                        reps = reps.toIntOrNull(),
                                        weight = weight,
                                        secs = secs.toIntOrNull(),
                                        rir = rir?.toIntOrNull(),
                                        bands = bands?.takeIf { it.isNotBlank() },
                                        notes = notes?.takeIf { it.isNotBlank() }
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

    // Overloaded function to maintain backward compatibility
    fun updateSet(exerciseId: String, setId: String, reps: String, weight: Double?, secs: String) {
        updateSet(exerciseId, setId, reps, weight, secs, null, null, null)
    }

    // Saves the completed workout to the database
    fun finishWorkout(currentUnit: String, activeCycle: ActiveProgramCycle?) {
        stopRestTimer()
        val endTimeMillis = System.currentTimeMillis() // Record end time

        activeWorkoutState.value?.let { workoutToSave ->
            viewModelScope.launch(Dispatchers.IO) {
                var finalBodyweight = workoutToSave.bodyweight

                // If the current session's bodyweight is null or zero...
                if (finalBodyweight == null || finalBodyweight <= 0) {
                    // ...try to find the last workout that had a bodyweight.
                    val lastWorkoutWithBw = loggedWorkoutDao.getLatestLoggedWorkoutWithBodyweight()
                    finalBodyweight = lastWorkoutWithBw?.bodyweight
                }

                // Create the final workout object to be saved, using the session's bodyweight
                // or the fallback value we just found.
                val finalWorkout = workoutToSave.copy(
                    endTimestamp = endTimeMillis,
                    performedWeightUnit = currentUnit,
                    bodyweight = finalBodyweight,
                    userCycleName = activeCycle?.userCycleName
                )

                loggedWorkoutDao.insert(finalWorkout)

                //Update the Active Cycle if this workout was part of one
                if (activeCycle != null && finalWorkout.programWeekDefinitionId != null && finalWorkout.programSessionDefinitionId != null) {
                    val sessionKey = "${finalWorkout.programWeekDefinitionId}_${finalWorkout.programSessionDefinitionId}"
                    val updatedCompletedSessions = activeCycle.completedSessions.toMutableMap()
                    updatedCompletedSessions[sessionKey] = finalWorkout.id

                    val updatedCycle = activeCycle.copy(completedSessions = updatedCompletedSessions)
                    activeCycleDao.setActiveCycle(updatedCycle)
                }

                val exerciseIds = finalWorkout.loggedExercises.map { it.exerciseId }
                val existingPRs = exerciseIds.flatMap { personalRecordDao.getPRsForExercise(it) }
                val allExercises = exerciseDao.getAllExercisesSnapshot()
                // Pass the workout with the unit to the PR service
                val newPRs = PrService.detectNewPRs(finalWorkout, existingPRs, allExercises)
                newPRs.forEach { pr ->
                    personalRecordDao.upsert(pr)
                }

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
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutLoggerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutLoggerViewModel(templateDao, loggedWorkoutDao, personalRecordDao, exerciseDao, activeCycleDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}