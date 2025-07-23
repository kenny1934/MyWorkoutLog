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
    private val activeCycleDao: ActiveCycleDao
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
        
        viewModelScope.launch(Dispatchers.IO) {
            // Get the existing workout from database
            loggedWorkoutDao.getLoggedWorkoutById(workoutId).collect { existingWorkout ->
                if (existingWorkout != null) {
                    // Set the workout start time to preserve timing context
                    workoutStartTimeMillis = existingWorkout.startTimestamp ?: System.currentTimeMillis()
                    
                    // Load the workout into active state for editing
                    _activeWorkoutState.value = existingWorkout
                    
                    // Initialize performance suggestions for editing context
                    initializePerformanceSuggestions()
                }
            }
        }
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
        
        viewModelScope.launch(Dispatchers.IO) {
            // PHASE 4: Check for existing in-progress workout first
            val inProgressWorkout = loggedWorkoutDao.getInProgressWorkoutForTemplate(templateId)
            if (inProgressWorkout != null) {
                // Found existing in-progress workout, load it instead of creating new one
                loadInProgressWorkout(inProgressWorkout)
                return@launch
            }
            
            // No existing workout found, create new one
            workoutStartTimeMillis = System.currentTimeMillis()
            
            // Get template snapshot for immediate access
            val template = templateDao.getTemplateByIdSnapshot(templateId)
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
                    // For edit mode, preserve original timestamps and ID
                    workoutToSave.copy(
                        id = originalWorkoutId!!, // Keep original ID
                        performedWeightUnit = currentUnit,
                        bodyweight = finalBodyweight,
                        userCycleName = activeCycle?.userCycleName,
                        isInProgress = false // Mark as completed
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
                _activeWorkoutState.value = null
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
    
    // Calculate and cache performance suggestions for all exercises in current workout
    private fun calculatePerformanceSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentWorkout = _activeWorkoutState.value ?: return@launch
            val suggestions = mutableMapOf<String, PerformanceSuggestion>()
            
            for (exercise in currentWorkout.loggedExercises) {
                val suggestion = calculateSuggestionForExercise(exercise.exerciseId)
                if (suggestion != null) {
                    suggestions[exercise.exerciseId] = suggestion
                }
            }
            
            _performanceSuggestions.value = suggestions
        }
    }
    
    // Calculate performance suggestion for a single exercise
    private suspend fun calculateSuggestionForExercise(exerciseId: String): PerformanceSuggestion? {
        val currentWorkout = _activeWorkoutState.value ?: return null
        val currentTemplateId = currentWorkout.workoutTemplateId ?: return null
        
        // First, try to find same exercise in same template (session-based matching)
        var recentWorkout = loggedWorkoutDao.getLatestWorkoutWithExerciseInTemplate(exerciseId, currentTemplateId)
        var isFromSameSession = true
        
        // Fallback to any recent workout with this exercise if no template match
        if (recentWorkout == null) {
            recentWorkout = loggedWorkoutDao.getLatestWorkoutWithExercise(exerciseId)
            isFromSameSession = false
        }
        
        if (recentWorkout == null) return null
        
        // Find the exercise in that workout
        val recentExercise = recentWorkout.loggedExercises.find { it.exerciseId == exerciseId }
            ?: return null
        
        // Get working sets (exclude obvious warm-up sets and failed sets)
        val workingSets = recentExercise.sets
            .filter { it.reps != null && it.reps > 0 } // Only completed sets
            .filter { set -> 
                // Exclude obvious warm-up sets (very light weight or very high reps)
                when {
                    set.weight != null && set.weight > 0 -> {
                        val maxWeight = recentExercise.sets.mapNotNull { it.weight }.maxOrNull() ?: 0.0
                        set.weight >= maxWeight * 0.8 // Only sets within 80% of max weight
                    }
                    else -> (set.reps ?: 0) <= 20 // For bodyweight, exclude very high rep sets
                }
            }
        
        if (workingSets.isEmpty()) return null
        
        // Use the most representative working set (highest weight OR most common weight)
        val representativeSet = workingSets
            .groupBy { it.weight }
            .maxByOrNull { (_, sets) -> sets.size }?.value?.first() // Most frequently used weight
            ?: workingSets.maxByOrNull { it.weight ?: 0.0 } // Fallback to heaviest
            ?: return null
        
        // Calculate days since last workout
        val daysAgo = calculateDaysSince(recentWorkout.date)
        
        // Get current cycle context for better RIR interpretation
        val currentCycleWeek = getCurrentCycleWeek()
        
        // Determine progression type based on session context and performance
        val progressionType = determineProgressionTypeImproved(
            daysAgo, representativeSet, isFromSameSession, currentCycleWeek
        )
        
        // Calculate suggested values with realistic increments
        val (suggestedWeight, suggestedReps, suggestedRir) = calculateProgressedValuesImproved(
            representativeSet, progressionType
        )
        
        // Calculate confidence based on session context
        val confidence = calculateConfidenceImproved(daysAgo, workingSets.size, isFromSameSession)
        
        return PerformanceSuggestion(
            suggestedWeight = suggestedWeight,
            suggestedReps = suggestedReps,
            suggestedRir = suggestedRir,
            confidence = confidence,
            basedonLastWorkout = true,
            daysAgo = daysAgo,
            progressionType = progressionType
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
    }
    
    // PHASE 4: Load an existing in-progress workout (must be called from IO context)
    private suspend fun loadInProgressWorkout(workout: LoggedWorkout) {
        // Set timing context
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