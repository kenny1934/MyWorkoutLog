package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.util.*
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// This ViewModel holds the state for an active workout session.
class WorkoutLoggerViewModel(
    private val templateDao: WorkoutTemplateDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao,
    private val bodyweightDao: BodyweightDao
) : ViewModel() {

    // A private mutable state flow to hold the in-progress workout
    private val _activeWorkoutState = MutableStateFlow<LoggedWorkout?>(null)
    // A public, read-only state flow for the UI to observe
    val activeWorkoutState: StateFlow<LoggedWorkout?> = _activeWorkoutState.asStateFlow()
    
    // StateFlow for all exercises (for exercise selection dialog)
    val allExercises: StateFlow<List<Exercise>> = exerciseDao.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Cache for performance suggestions to avoid repeated calculations
    private val _performanceSuggestions = MutableStateFlow<Map<String, PerformanceSuggestion>>(emptyMap())

    // Cache for last-performance summaries keyed by exerciseId (surfaced on EnhancedExerciseCard).
    private val _lastPerformanceSummaries = MutableStateFlow<Map<String, String>>(emptyMap())

    // Cache for progression hints keyed by exerciseId (surfaced under the exercise name
    // in both compact and master-detail logger layouts). Populated from the workout's
    // template whenever the workout is loaded or resumed; stays empty for ad-hoc workouts
    // without a template or when no exercise in the template has a scheme configured.
    private val _progressionHints = MutableStateFlow<Map<String, String>>(emptyMap())

    // Cache of the TemplateExercise entries keyed by the underlying exerciseId. Mirrors
    // _progressionHints (same template lookup, same refresh points) but keeps the raw
    // params around so the scheme-aware chip can re-run suggestForScheme per set number
    // without re-parsing the JSON blob.
    private val _templateExercisesByExerciseId = MutableStateFlow<Map<String, TemplateExercise>>(emptyMap())

    // Representative last-session set per exerciseId plus the daysAgo tag. Populated in
    // the same loop as _performanceSuggestions; reads feed the scheme-aware chip so it
    // can build a fresh suggestion per set number without redoing the DB lookup.
    private val _recentRepresentatives = MutableStateFlow<Map<String, RecentRepresentative>>(emptyMap())

    // Track if we're in edit mode for an existing workout
    private var isEditMode = false
    private var originalWorkoutId: String? = null
    
    // Public getter for edit mode state
    fun isInEditMode(): Boolean = isEditMode
    
    // PHASE 2: Session cleanup functionality
    init {
        // Clean up any abandoned in-progress workouts from previous sessions
        viewModelScope.launch(Dispatchers.IO) {
            cleanupAbandonedSessions()
        }
    }
    
    private fun cleanupAbandonedSessions() {
        // Clean up sessions older than 24 hours
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
        loggedWorkoutDao.cleanupAbandonedInProgressWorkouts(cutoffTime)
    }
    
    // PHASE 3: Auto-save current workout state to database
    private fun autoSaveWorkout() {
        activeWorkoutState.value?.let { workout ->
            if (workout.isInProgress && !isEditMode) { // Only auto-save new in-progress workouts
                viewModelScope.launch(Dispatchers.IO) {
                    loggedWorkoutDao.updateLoggedWorkout(workout)
                }
            }
        }
    }

    // --- REFACTORED TIMER/STOPWATCH LOGIC ---
    private var workoutStartTimeMillis: Long = 0L
    
    // Store original workout duration for edit mode
    private val _originalWorkoutDurationSeconds = MutableStateFlow<Int?>(null)

    // Stopwatch in normal mode; frozen original duration in edit mode.
    val sessionElapsedTime: StateFlow<Int> = flow {
        while (true) {
            if (isEditMode) {
                emit(_originalWorkoutDurationSeconds.value ?: 0)
            } else if (workoutStartTimeMillis > 0) {
                val elapsed = (System.currentTimeMillis() - workoutStartTimeMillis) / 1000
                emit(maxOf(0, elapsed.toInt()))
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
    
    // Rest time tracking for specific sets
    private var currentRestTimerSet: Pair<String, String>? = null // (exerciseId, setId)
    private var restStartTime: Long = 0L

    // --- TIMER AND STOPWATCH CONTROLS ---

    fun startRestTimer(durationSeconds: Int = 120) {
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
        // Record rest time before stopping
        recordCurrentRestTime()
        
        restTimerJob?.cancel()
        _timerIsRunning.value = false
        _timerValueSeconds.value = 0
        
        // Clear current set tracking
        currentRestTimerSet = null
        restStartTime = 0L
    }

    fun resetRestTimer(durationSeconds: Int = 120) {
        stopRestTimer()
        startRestTimer(durationSeconds)
    }

    fun addTimeToRestTimer(secondsToAdd: Int) {
        val newDuration = _timerValueSeconds.value + secondsToAdd
        startRestTimer(newDuration)
    }
    
    // Start rest timer for a specific set to track rest time
    fun startRestTimerForSet(exerciseId: String, setId: String, durationSeconds: Int = 120) {
        // Record rest time for previous set if timer was running
        recordCurrentRestTime()
        
        // Set up for new set
        currentRestTimerSet = Pair(exerciseId, setId)
        restStartTime = System.currentTimeMillis()
        
        // Start the timer
        startRestTimer(durationSeconds)
    }
    
    // Record the actual rest time for the current set
    private fun recordCurrentRestTime() {
        currentRestTimerSet?.let { (exerciseId, setId) ->
            if (restStartTime > 0) {
                val actualRestSeconds = ((System.currentTimeMillis() - restStartTime) / 1000).toInt()
                updateSetRestTime(exerciseId, setId, actualRestSeconds)
            }
        }
    }
    
    // Update a specific set with recorded rest time
    private fun updateSetRestTime(exerciseId: String, setId: String, restSeconds: Int) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.map { set ->
                                if (set.id == setId) {
                                    set.copy(restTimeSeconds = restSeconds)
                                } else set
                            }
                        )
                    } else exercise
                }
            )
        }
    }

    // Load an existing workout for editing
    fun loadWorkoutForEdit(workoutId: String) {
        isEditMode = true
        originalWorkoutId = workoutId

        // One-shot load. Using .collect on this hot Room Flow would re-fire on every
        // subsequent DB update of the same row and clobber in-memory edits (including
        // the re-emission that finishWorkout itself triggers after an update).
        viewModelScope.launch(Dispatchers.IO) {
            loggedWorkoutDao.getLoggedWorkoutById(workoutId).first()?.let { existingWorkout ->
                // Don't set workoutStartTimeMillis in edit mode to prevent timer from running
                val originalDurationSeconds = if (existingWorkout.startTimestamp != null) {
                    if (existingWorkout.endTimestamp != null) {
                        ((existingWorkout.endTimestamp - existingWorkout.startTimestamp) / 1000).toInt()
                    } else {
                        ((System.currentTimeMillis() - existingWorkout.startTimestamp) / 1000).toInt()
                    }
                } else null
                _originalWorkoutDurationSeconds.value = originalDurationSeconds
                _activeWorkoutState.value = existingWorkout
                initializePerformanceSuggestions()
            }
        }
    }
    
    // Update workout duration manually in edit mode
    fun updateWorkoutDuration(durationSeconds: Int) {
        if (!isEditMode) return

        _originalWorkoutDurationSeconds.value = durationSeconds

        // Setting a concrete duration is a finalizing action — flip isInProgress=false
        // so an edited in-progress row doesn't end up with endTimestamp set while the
        // in-progress flag still says it's live (history reads endTimestamp, resume
        // lookups read isInProgress; a half-and-half row breaks both).
        _activeWorkoutState.value?.let { workout ->
            if (workout.startTimestamp != null) {
                val newEndTimestamp = workout.startTimestamp + (durationSeconds * 1000L)
                _activeWorkoutState.value = workout.copy(
                    endTimestamp = newEndTimestamp,
                    isInProgress = false,
                )
            }
        }
    }

    // This function starts a new workout based on a template
    fun startWorkoutFromTemplate(
        templateId: String,
        cycleId: String?,
        weekId: String?,
        sessionId: String?,
        forceNew: Boolean = false
    ) {
        // When starting a new workout, ensure any old timer is stopped.
        stopRestTimer()
        
        viewModelScope.launch(Dispatchers.IO) {
            // Check for existing in-progress workout
            val inProgressWorkout = loggedWorkoutDao.getInProgressWorkoutForTemplate(templateId)
            
            if (inProgressWorkout != null && !forceNew) {
                // Found existing in-progress workout and not forcing new - load it
                loadInProgressWorkout(inProgressWorkout)
                return@launch
            } else if (inProgressWorkout != null && forceNew) {
                // Found existing workout but user wants fresh start - clean it up
                loggedWorkoutDao.markWorkoutAsCompleted(inProgressWorkout.id)
            }
            
            // No existing workout found, create new one
            workoutStartTimeMillis = System.currentTimeMillis()

            // Clear any stale timer state from previous sessions
            _originalWorkoutDurationSeconds.value = null
            
            // Get template snapshot for immediate access
            val template = try {
                templateDao.getTemplateByIdSnapshot(templateId)
            } catch (e: Exception) {
                null
            }
            
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
                                targetSecs = templateSet.targetSecs,
                                targetWeight = templateSet.targetWeight,
                            )
                        },
                        isSubstitute = false
                    )
                }

                // Get today's bodyweight entry if available
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayBodyweight = try {
                    bodyweightDao.getBodyweightForDate(todayDate)?.weight
                } catch (e: Exception) {
                    null
                }

                val newLoggedWorkout = LoggedWorkout(
                    id = UUID.randomUUID().toString(),
                    name = template.name,
                    date = todayDate,
                    startTimestamp = workoutStartTimeMillis, // Save start time
                    endTimestamp = null, // End time is null for now
                    performedWeightUnit = null,
                    bodyweight = todayBodyweight, // Auto-populate from bodyweight entry
                    activeProgramCycleId = cycleId, // Save the cycle context
                    programWeekDefinitionId = weekId, // Save the week context
                    programSessionDefinitionId = sessionId, // Save the session context
                    userCycleName = null, // Will be set when workout is finished
                    loggedExercises = loggedExercises,
                    workoutTemplateId = template.id,
                    isInProgress = true // Mark as in-progress for session persistence
                )
                
                // PHASE 2: Immediate Persistence - Save as in-progress workout immediately
                loggedWorkoutDao.insert(newLoggedWorkout)
                
                // Update state on Main thread
                withContext(Dispatchers.Main) {
                    _activeWorkoutState.value = newLoggedWorkout
                }
                
                // Initialize performance suggestions after workout is loaded
                initializePerformanceSuggestions()
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
        notes: String? = null,
        videoReference: String? = null
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
                                        notes = notes?.takeIf { it.isNotBlank() },
                                        videoReference = videoReference?.takeIf { it.isNotBlank() }
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
        
        // PHASE 3: Auto-save after set update
        autoSaveWorkout()
    }

    // Overloaded function to maintain backward compatibility
    fun updateSet(exerciseId: String, setId: String, reps: String, weight: Double?, secs: String) {
        updateSet(exerciseId, setId, reps, weight, secs, null, null, null, null)
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
                val finalWorkout = if (isEditMode && originalWorkoutId != null) {
                    // For edit mode, preserve original timestamps, ID, and in-progress state
                    val originalWorkout = _activeWorkoutState.value
                    workoutToSave.copy(
                        id = originalWorkoutId!!, // Keep original ID
                        performedWeightUnit = currentUnit,
                        bodyweight = finalBodyweight,
                        userCycleName = activeCycle?.userCycleName,
                        isInProgress = originalWorkout?.isInProgress ?: false // Preserve original in-progress state
                        // Note: We don't update endTimestamp in edit mode to preserve original workout timing
                    )
                } else {
                    // For new workouts, mark as completed and set end time
                    workoutToSave.copy(
                        endTimestamp = endTimeMillis,
                        performedWeightUnit = currentUnit,
                        bodyweight = finalBodyweight,
                        userCycleName = activeCycle?.userCycleName,
                        isInProgress = false // Mark as completed
                    )
                }

                if (isEditMode) {
                    // Update existing workout
                    loggedWorkoutDao.updateLoggedWorkout(finalWorkout)
                } else {
                    // Insert new workout
                    loggedWorkoutDao.insert(finalWorkout)
                }

                //Update the Active Cycle if this workout was part of one (only for new workouts)
                if (!isEditMode && activeCycle != null && finalWorkout.programWeekDefinitionId != null && finalWorkout.programSessionDefinitionId != null) {
                    val sessionKey = "${finalWorkout.programWeekDefinitionId}_${finalWorkout.programSessionDefinitionId}"
                    val updatedCompletedSessions = activeCycle.completedSessions.toMutableMap()
                    updatedCompletedSessions[sessionKey] = finalWorkout.id

                    val updatedCycle = activeCycle.copy(completedSessions = updatedCompletedSessions)
                    activeCycleDao.setActiveCycle(updatedCycle)
                }

                // Process PRs (for both new and edited workouts)
                val exerciseIds = finalWorkout.loggedExercises.map { it.exerciseId }
                val existingPRs = exerciseIds.flatMap { personalRecordDao.getPRsForExercise(it) }
                val allExercises = exerciseDao.getAllExercisesSnapshot()
                // Pass the workout with the unit to the PR service
                val newPRs = PrService.detectNewPRs(finalWorkout, existingPRs, allExercises)
                newPRs.forEach { pr ->
                    personalRecordDao.upsert(pr)
                }

                // Reset edit mode state
                isEditMode = false
                originalWorkoutId = null

                // Handle state based on workout completion status
                if (finalWorkout.isInProgress == false) {
                    // Workout is completed - clear all state
                    _activeWorkoutState.value = null
                    _originalWorkoutDurationSeconds.value = null
                    workoutStartTimeMillis = 0L
                } else {
                    // Workout is still in-progress - restore timer state for continuation
                    workoutStartTimeMillis = finalWorkout.startTimestamp ?: System.currentTimeMillis()
                    _originalWorkoutDurationSeconds.value = null // Clear edit mode duration
                }
            }
        }
    }

    fun updateBodyweight(bodyweight: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(bodyweight = bodyweight.toDoubleOrNull())
        }
        
        // PHASE 3: Auto-save after bodyweight update
        autoSaveWorkout()
    }
    
    fun updateOverallComments(comments: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(overallComments = comments.takeIf { it.isNotBlank() })
        }
        
        // PHASE 3: Auto-save after comments update
        autoSaveWorkout()
    }
    
    // Add exercise to the current workout
    fun addExerciseToWorkout(exerciseId: String, numberOfSets: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the exercise details from the database
            val exercise = exerciseDao.getExerciseById(exerciseId)
            if (exercise != null) {
                // Create the LoggedExercise with empty sets
                val loggedSets = (1..numberOfSets).map {
                    LoggedSet(
                        id = UUID.randomUUID().toString(),
                        reps = null,
                        secs = null,
                        weight = null,
                        rir = null,
                        bands = null,
                        notes = null,
                        restTimeSeconds = null,
                        videoReference = null,
                        targetReps = "8-12", // Default target reps for ad-hoc exercises
                        targetSecs = null
                    )
                }
                
                val loggedExercise = LoggedExercise(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    targetMuscleGroups = exercise.targetMuscleGroups,
                    equipment = exercise.equipment,
                    sets = loggedSets,
                    isSubstitute = false, // This is an addition, not a substitution
                    notes = null
                )
                
                // Add the exercise to the current workout
                _activeWorkoutState.update { currentWorkout ->
                    currentWorkout?.copy(
                        loggedExercises = currentWorkout.loggedExercises + loggedExercise
                    )
                }
                
                // Recalculate suggestions for the new exercise
                calculatePerformanceSuggestions()
                
                // PHASE 3: Auto-save after adding exercise
                autoSaveWorkout()
            }
        }
    }
    
    // Substitute an exercise in the current workout
    fun substituteExercise(currentExerciseId: String, newExerciseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Get the new exercise details from the database
            val newExercise = exerciseDao.getExerciseById(newExerciseId)
            if (newExercise != null) {
                _activeWorkoutState.update { currentWorkout ->
                    currentWorkout?.copy(
                        loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                            if (exercise.id == currentExerciseId) {
                                // Replace the exercise but keep all the sets data
                                exercise.copy(
                                    exerciseId = newExercise.id,
                                    exerciseName = newExercise.name,
                                    targetMuscleGroups = newExercise.targetMuscleGroups,
                                    equipment = newExercise.equipment,
                                    isSubstitute = true // Mark as substituted
                                )
                            } else {
                                exercise
                            }
                        }
                    )
                }
                
                // PHASE 3: Auto-save after exercise substitution
                autoSaveWorkout()
            }
        }
    }
    
    // Remove an exercise from the current workout
    fun removeExerciseFromWorkout(exerciseId: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.filter { exercise ->
                    exercise.id != exerciseId
                }
            )
        }
        
        // PHASE 3: Auto-save after removing exercise
        autoSaveWorkout()
    }
    
    // Add a set to an existing exercise
    fun addSetToExercise(exerciseId: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        // Create a new empty set
                        val newSet = LoggedSet(
                            id = UUID.randomUUID().toString(),
                            reps = null,
                            secs = null,
                            weight = null,
                            rir = null,
                            bands = null,
                            notes = null,
                            restTimeSeconds = null,
                            videoReference = null,
                            targetReps = "8-12", // Default target for added sets
                            targetSecs = null
                        )
                        exercise.copy(sets = exercise.sets + newSet)
                    } else {
                        exercise
                    }
                }
            )
        }
        
        // PHASE 3: Auto-save after adding set
        autoSaveWorkout()
    }
    
    // Remove a set from an exercise
    fun removeSetFromExercise(exerciseId: String, setId: String) {
        _activeWorkoutState.update { currentWorkout ->
            currentWorkout?.copy(
                loggedExercises = currentWorkout.loggedExercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(
                            sets = exercise.sets.filter { set -> set.id != setId }
                        )
                    } else {
                        exercise
                    }
                }
            )
        }
        
        // PHASE 3: Auto-save after removing set
        autoSaveWorkout()
    }
    
    // SMART PRE-FILL FUNCTIONALITY
    
    // Get performance suggestion for a specific exercise
    fun getPerformanceSuggestion(exerciseId: String): PerformanceSuggestion? {
        return _performanceSuggestions.value[exerciseId]
    }

    // Get last-performance summary for a specific exercise (rendered under the exercise name).
    fun getLastPerformance(exerciseId: String): String? {
        return _lastPerformanceSummaries.value[exerciseId]
    }

    // Calculate and cache performance suggestions for all exercises in current workout
    private fun calculatePerformanceSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentWorkout = _activeWorkoutState.value ?: return@launch
            val suggestions = mutableMapOf<String, PerformanceSuggestion>()
            val summaries = mutableMapOf<String, String>()
            val representatives = mutableMapOf<String, RecentRepresentative>()

            for (exercise in currentWorkout.loggedExercises) {
                val recent = findRecentWorkoutForExercise(exercise.exerciseId, currentWorkout.workoutTemplateId)
                if (recent != null) {
                    val representative = pickRecentRepresentative(recent.workout, recent.exercise)
                    if (representative != null) {
                        representatives[exercise.exerciseId] = representative
                        val suggestion = buildSuggestionFromRepresentative(representative, recent.isFromSameSession)
                        suggestions[exercise.exerciseId] = suggestion
                    }

                    val summary = summarizeLastPerformance(recent.workout, recent.exercise)
                    if (summary != null) summaries[exercise.exerciseId] = summary
                }
            }

            _performanceSuggestions.value = suggestions
            _lastPerformanceSummaries.value = summaries
            _recentRepresentatives.value = representatives
        }
    }

    private data class RecentExerciseLookup(
        val workout: LoggedWorkout,
        val exercise: LoggedExercise,
        val isFromSameSession: Boolean,
    )

    // The representative set from a recent session, plus the working-set count and
    // calendar-days gap. Feeds both the legacy progression suggestion and the scheme-
    // aware chip — caching it avoids redoing the picker at render time.
    private data class RecentRepresentative(
        val set: LoggedSet,
        val workingSetCount: Int,
        val daysAgo: Int,
    )

    private fun findRecentWorkoutForExercise(exerciseId: String, currentTemplateId: String?): RecentExerciseLookup? {
        var workout: LoggedWorkout? = null
        var fromSameSession = true
        if (currentTemplateId != null) {
            workout = loggedWorkoutDao.getLatestWorkoutWithExerciseInTemplate(exerciseId, currentTemplateId)
        }
        if (workout == null) {
            workout = loggedWorkoutDao.getLatestWorkoutWithExercise(exerciseId)
            fromSameSession = false
        }
        val w = workout ?: return null
        val ex = w.loggedExercises.find { it.exerciseId == exerciseId } ?: return null
        return RecentExerciseLookup(w, ex, fromSameSession)
    }
    
    // Pick the representative working set from a recent workout/exercise pair. Filters
    // obvious warm-ups (weight < 80% of max) and very-high-rep bodyweight sets, then
    // picks the most-frequently-used weight (or heaviest as a fallback). Shared by the
    // legacy PerformanceSuggestion path and the scheme-aware chip.
    private fun pickRecentRepresentative(
        recentWorkout: LoggedWorkout,
        recentExercise: LoggedExercise,
    ): RecentRepresentative? {
        val workingSets = recentExercise.sets
            .filter { it.reps != null && it.reps > 0 }
            .filter { set ->
                when {
                    set.weight != null && set.weight > 0 -> {
                        val maxWeight = recentExercise.sets.mapNotNull { it.weight }.maxOrNull() ?: 0.0
                        set.weight >= maxWeight * 0.8
                    }
                    else -> (set.reps ?: 0) <= 20
                }
            }
        if (workingSets.isEmpty()) return null

        val representativeSet = workingSets
            .groupBy { it.weight }
            .maxByOrNull { (_, sets) -> sets.size }?.value?.first()
            ?: workingSets.maxByOrNull { it.weight ?: 0.0 }
            ?: return null

        return RecentRepresentative(
            set = representativeSet,
            workingSetCount = workingSets.size,
            daysAgo = calculateDaysSince(recentWorkout.date),
        )
    }

    // Wrap a representative set in the legacy "maintain / increase / decrease" suggestion.
    private fun buildSuggestionFromRepresentative(
        rep: RecentRepresentative,
        isFromSameSession: Boolean,
    ): PerformanceSuggestion {
        val currentCycleWeek = getCurrentCycleWeek()
        val progressionType = determineProgressionTypeImproved(
            rep.daysAgo, rep.set, isFromSameSession, currentCycleWeek
        )
        val (suggestedWeight, suggestedReps, suggestedRir) = calculateProgressedValuesImproved(
            rep.set, progressionType
        )
        val confidence = calculateConfidenceImproved(rep.daysAgo, rep.workingSetCount, isFromSameSession)

        return PerformanceSuggestion(
            suggestedWeight = suggestedWeight,
            suggestedReps = suggestedReps,
            suggestedRir = suggestedRir,
            confidence = confidence,
            basedonLastWorkout = true,
            daysAgo = rep.daysAgo,
            progressionType = progressionType,
        )
    }
    
    private fun calculateDaysSince(dateString: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDate = format.parse(dateString)
            val currentDate = Date()
            val diffInMillis = currentDate.time - (lastDate?.time ?: 0)
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Int.MAX_VALUE // Very old if we can't parse
        }
    }
    
    // Get current cycle week for context-aware RIR interpretation
    private fun getCurrentCycleWeek(): Int? {
        val currentWorkout = _activeWorkoutState.value ?: return null
        return currentWorkout.programWeekDefinitionId?.let { weekId ->
            // Extract week number from week definition ID if possible
            // This is a simplified approach - might need adjustment based on your ID format
            weekId.takeLast(1).toIntOrNull()
        }
    }
    
    // Improved progression logic based on session context and cycle position
    private fun determineProgressionTypeImproved(
        daysAgo: Int, 
        representativeSet: LoggedSet, 
        isFromSameSession: Boolean,
        currentCycleWeek: Int?
    ): ProgressionType {
        return when {
            // Same session type from last week (ideal scenario)
            isFromSameSession && daysAgo in 5..10 -> {
                // Use cycle-aware RIR interpretation
                val rirThreshold = when (currentCycleWeek) {
                    1, 2 -> 3    // Early cycle: conservative, need RIR 3+ to progress
                    3, 4 -> 2    // Mid cycle: moderate, need RIR 2+ to progress  
                    5, 6 -> 1    // Late cycle: aggressive, need RIR 1+ to progress
                    else -> 2    // Unknown cycle position: moderate approach
                }
                
                when {
                    (representativeSet.rir ?: 3) >= rirThreshold -> ProgressionType.INCREASE
                    else -> ProgressionType.MAINTAIN
                }
            }
            
            // Same session but from 2+ weeks ago
            isFromSameSession && daysAgo > 14 -> ProgressionType.DECREASE
            
            // Different session type but recent
            !isFromSameSession && daysAgo <= 7 -> ProgressionType.MAINTAIN
            
            // Old data from different session
            !isFromSameSession && daysAgo > 14 -> ProgressionType.DECREASE
            
            // Default case
            else -> ProgressionType.MAINTAIN
        }
    }
    
    // Keep original function for compatibility
    private fun determineProgressionType(daysAgo: Int, bestSet: LoggedSet): ProgressionType {
        return when {
            daysAgo <= 3 -> {
                // Recent workout - check RIR for progression potential
                when (bestSet.rir) {
                    in 0..2 -> ProgressionType.MAINTAIN // Low RIR - maintain weight
                    in 3..4 -> ProgressionType.INCREASE // Moderate RIR - small increase
                    else -> ProgressionType.INCREASE // High/unknown RIR - progress
                }
            }
            daysAgo <= 7 -> ProgressionType.MAINTAIN // Week old - maintain
            daysAgo <= 14 -> ProgressionType.DECREASE // 2+ weeks - slight deload
            else -> ProgressionType.DECREASE // Very old - significant deload
        }
    }
    
    // Improved weight calculation with realistic 1.25kg increments
    private fun calculateProgressedValuesImproved(
        representativeSet: LoggedSet, 
        progressionType: ProgressionType
    ): Triple<Double?, Int?, Int?> {
        val baseWeight = representativeSet.weight
        val baseReps = representativeSet.reps
        val baseRir = representativeSet.rir
        
        when (progressionType) {
            ProgressionType.INCREASE -> {
                val newWeight = baseWeight?.let { weight ->
                    // Standard 1.25kg increment (realistic small plates)
                    weight + 1.25
                }
                val newReps = baseReps // Keep same reps when increasing weight
                val newRir = (baseRir ?: 2) + 1 // Slightly higher RIR target for progression
                
                return Triple(newWeight, newReps, newRir)
            }
            
            ProgressionType.MAINTAIN -> {
                return Triple(baseWeight, baseReps, baseRir)
            }
            
            ProgressionType.DECREASE -> {
                val newWeight = baseWeight?.let { weight ->
                    // Conservative deload: -1.25kg or 5% reduction, whichever is smaller
                    val smallDecrease = weight - 1.25
                    val percentageDecrease = weight * 0.95
                    minOf(smallDecrease, percentageDecrease)
                }
                val newReps = baseReps // Keep same reps
                val newRir = (baseRir ?: 2) + 1 // Higher RIR for recovery
                
                return Triple(newWeight, newReps, newRir)
            }
        }
    }
    
    // Keep original function for compatibility
    private fun calculateProgressedValues(
        bestSet: LoggedSet, 
        progressionType: ProgressionType
    ): Triple<Double?, Int?, Int?> {
        val baseWeight = bestSet.weight
        val baseReps = bestSet.reps
        val baseRir = bestSet.rir
        
        when (progressionType) {
            ProgressionType.INCREASE -> {
                val newWeight = baseWeight?.let { 
                    // Increase by 2.5-5% for weights
                    val increment = when {
                        it < 50 -> 2.5 // Small weights: 2.5kg increment
                        it < 100 -> 5.0 // Medium weights: 5kg increment  
                        else -> (it * 0.025).let { inc -> // Large weights: 2.5% increment
                            (inc / 2.5).roundToInt() * 2.5 // Round to nearest 2.5kg
                        }
                    }
                    it + increment
                }
                val newReps = baseReps // Keep same reps when increasing weight
                val newRir = (baseRir ?: 2) + 1 // Slightly higher RIR target
                
                return Triple(newWeight, newReps, newRir)
            }
            
            ProgressionType.MAINTAIN -> {
                return Triple(baseWeight, baseReps, baseRir)
            }
            
            ProgressionType.DECREASE -> {
                val newWeight = baseWeight?.let { 
                    // Decrease by 5-10% for deload
                    it * 0.9
                }
                val newReps = baseReps // Keep same reps
                val newRir = (baseRir ?: 2) + 1 // Higher RIR for recovery
                
                return Triple(newWeight, newReps, newRir)
            }
        }
    }
    
    // Improved confidence calculation considering session context
    private fun calculateConfidenceImproved(
        daysAgo: Int, 
        setCount: Int, 
        isFromSameSession: Boolean
    ): Float {
        val recencyFactor = when {
            isFromSameSession && daysAgo in 5..10 -> 1.0f // Perfect: same session last week
            isFromSameSession && daysAgo <= 14 -> 0.9f    // Good: same session within 2 weeks
            !isFromSameSession && daysAgo <= 7 -> 0.7f    // OK: different session but recent
            daysAgo <= 14 -> 0.5f                          // Moderate: somewhat old
            daysAgo <= 30 -> 0.3f                          // Low: getting old
            else -> 0.1f                                   // Very low: very old
        }
        
        val setCountFactor = when {
            setCount >= 3 -> 1.0f     // Multiple working sets = reliable
            setCount == 2 -> 0.9f     // Two sets = good sample
            setCount == 1 -> 0.7f     // Single set = less reliable
            else -> 0.3f              // Very limited data
        }
        
        val sessionBonus = if (isFromSameSession) 0.1f else 0f // Bonus for same session type
        
        return ((recencyFactor * setCountFactor) + sessionBonus).coerceIn(0f, 1f)
    }
    
    // Keep original function for compatibility
    private fun calculateConfidence(daysAgo: Int, setCount: Int): Float {
        val recencyFactor = when {
            daysAgo <= 3 -> 1.0f
            daysAgo <= 7 -> 0.8f
            daysAgo <= 14 -> 0.6f
            daysAgo <= 30 -> 0.4f
            else -> 0.2f
        }
        
        val setCountFactor = when {
            setCount >= 3 -> 1.0f
            setCount == 2 -> 0.8f
            setCount == 1 -> 0.6f
            else -> 0.3f
        }
        
        return (recencyFactor * setCountFactor).coerceIn(0f, 1f)
    }
    
    // Call this when starting a workout to pre-calculate suggestions
    private fun initializePerformanceSuggestions() {
        calculatePerformanceSuggestions()
        refreshProgressionHints()
    }

    // Public getter mirrors getPerformanceSuggestion / getLastPerformance. Returns null
    // when no scheme is configured for this exercise in the active workout's template.
    fun getProgressionHint(exerciseId: String): String? = _progressionHints.value[exerciseId]

    // Scheme-aware chip suggestion. When the exercise's template entry has a progression
    // scheme, run the pure helper over the cached representative set and return the
    // scheme-derived PerformanceSuggestion (with suggestionLabel set so the chip renders
    // the scheme-specific suffix instead of "Xd ago"). When no scheme is configured, or
    // the helper has nothing to offer, fall through to the legacy cached suggestion.
    fun getChipSuggestion(exerciseId: String, setNumber: Int): PerformanceSuggestion? {
        val templateExercise = _templateExercisesByExerciseId.value[exerciseId]
        val representative = _recentRepresentatives.value[exerciseId]
        val scheme = templateExercise?.progressionScheme
        if (scheme != null && scheme != ProgressionScheme.NONE && representative != null) {
            val chip = suggestForScheme(
                scheme = scheme,
                setNumber = setNumber,
                lastWeight = representative.set.weight,
                lastReps = representative.set.reps,
                lastRir = representative.set.rir,
                increment = templateExercise.progressionIncrement,
                minReps = templateExercise.progressionMinReps,
                maxReps = templateExercise.progressionMaxReps,
                targetRpe = templateExercise.progressionTargetRpe,
            )
            if (chip != null) {
                return PerformanceSuggestion(
                    suggestedWeight = chip.weight,
                    suggestedReps = chip.reps,
                    suggestedRir = chip.rir,
                    confidence = 1f,
                    basedonLastWorkout = true,
                    daysAgo = representative.daysAgo,
                    suggestionLabel = chip.label,
                )
            }
        }
        return _performanceSuggestions.value[exerciseId]
    }

    // Fetches the active workout's template and recomputes the exerciseId → hint map.
    // Called from every workout-load entry point via initializePerformanceSuggestions.
    private fun refreshProgressionHints() {
        val workout = _activeWorkoutState.value
        val templateId = workout?.workoutTemplateId
        if (templateId == null) {
            _progressionHints.value = emptyMap()
            _templateExercisesByExerciseId.value = emptyMap()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val template = try {
                templateDao.getTemplateByIdSnapshot(templateId)
            } catch (e: Exception) {
                null
            }
            if (template == null) {
                _progressionHints.value = emptyMap()
                _templateExercisesByExerciseId.value = emptyMap()
                return@launch
            }
            val hints = mutableMapOf<String, String>()
            val byId = mutableMapOf<String, TemplateExercise>()
            template.templateExercises.forEach { ex ->
                byId[ex.exerciseId] = ex
                val hint = formatProgressionHint(ex)
                if (hint != null) hints[ex.exerciseId] = hint
            }
            _progressionHints.value = hints
            _templateExercisesByExerciseId.value = byId
        }
    }
    
    // PHASE 4: Load an existing in-progress workout (must be called from IO context)
    private suspend fun loadInProgressWorkout(workout: LoggedWorkout) {
        // Clear any stale edit mode state
        isEditMode = false
        originalWorkoutId = null
        _originalWorkoutDurationSeconds.value = null

        // Set timing context for live timer
        workoutStartTimeMillis = workout.startTimestamp ?: System.currentTimeMillis()

        // Load the workout into active state (switch to Main thread for UI update)
        withContext(Dispatchers.Main) {
            _activeWorkoutState.value = workout
        }
        
        // Initialize performance suggestions (this handles its own threading)
        initializePerformanceSuggestions()
    }
    
    // PHASE 4: Get list of all in-progress workouts (for recovery UI)
    fun getAllInProgressWorkouts(): Flow<List<LoggedWorkout>> {
        return loggedWorkoutDao.getAllInProgressWorkouts()
    }
    
    // NEW: Check for existing in-progress workout without auto-loading
    suspend fun checkForInProgressWorkout(templateId: String): LoggedWorkout? {
        return try {
            withContext(Dispatchers.IO) {
                loggedWorkoutDao.getInProgressWorkoutForTemplate(templateId)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // NEW: Force start a fresh workout, clearing any existing in-progress session
    fun startFreshWorkout(
        templateId: String,
        cycleId: String?,
        weekId: String?,
        sessionId: String?
    ) {
        startWorkoutFromTemplate(templateId, cycleId, weekId, sessionId, forceNew = true)
    }
    
    // NEW: Resume existing in-progress workout
    fun resumeInProgressWorkout(templateId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val inProgressWorkout = loggedWorkoutDao.getInProgressWorkoutForTemplate(templateId)
            if (inProgressWorkout != null) {
                loadInProgressWorkout(inProgressWorkout)
            }
        }
    }
    
    // NEW: Get session status for UI decision making
    suspend fun getSessionStatus(templateId: String): WorkoutSessionStatus {
        val inProgressWorkout = checkForInProgressWorkout(templateId)
        return if (inProgressWorkout != null) {
            val hoursAgo = (System.currentTimeMillis() - (inProgressWorkout.startTimestamp ?: 0)) / (1000 * 60 * 60)
            WorkoutSessionStatus.InProgress(inProgressWorkout, hoursAgo.toInt())
        } else {
            WorkoutSessionStatus.None
        }
    }
    
    // PHASE 2: Cancel workout - clean up in-progress state without saving as completed workout
    fun cancelWorkout() {
        activeWorkoutState.value?.let { workout ->
            if (!isEditMode) { // Only delete if it's a new workout, not an edit
                viewModelScope.launch(Dispatchers.IO) {
                    // Mark as completed (which effectively removes it from in-progress)
                    loggedWorkoutDao.markWorkoutAsCompleted(workout.id)
                }
            }
        }
        
        // Reset state
        stopRestTimer()
        _activeWorkoutState.value = null
        isEditMode = false
        originalWorkoutId = null
        workoutStartTimeMillis = 0L
        _originalWorkoutDurationSeconds.value = null
    }
    
    // PHASE 5: Lifecycle Management - Clean up when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        
        // Auto-save any pending changes before ViewModel is destroyed
        activeWorkoutState.value?.let { workout ->
            if (workout.isInProgress && !isEditMode) {
                // Use try-catch since we're in cleanup context
                try {
                    loggedWorkoutDao.updateLoggedWorkout(workout)
                } catch (e: Exception) {
                    // Log error but don't crash during cleanup
                }
            }
        }
        
        // Stop any running timers
        stopRestTimer()
    }
}

// The factory for creating our new ViewModel
class WorkoutLoggerViewModelFactory(
    private val templateDao: WorkoutTemplateDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao,
    private val activeCycleDao: ActiveCycleDao,
    private val bodyweightDao: BodyweightDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutLoggerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutLoggerViewModel(templateDao, loggedWorkoutDao, personalRecordDao, exerciseDao, activeCycleDao, bodyweightDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}