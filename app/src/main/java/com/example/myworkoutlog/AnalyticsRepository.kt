package com.example.myworkoutlog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class AnalyticsRepository(
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val activeCycleDao: ActiveCycleDao,
    private val personalRecordDao: PersonalRecordDao,
    private val exerciseDao: ExerciseDao? = null
) {
    
    // VOLUME PROGRESSION ANALYSIS
    
    fun getVolumeProgressionData(
        startDate: String,
        endDate: String,
        exerciseId: String? = null
    ): Flow<List<VolumeDataPoint>> {
        android.util.Log.d("AnalyticsRepository", "=== VOLUME PROGRESSION DATA DEBUG ===")
        android.util.Log.d("AnalyticsRepository", "StartDate: $startDate, EndDate: $endDate, ExerciseId: $exerciseId")
        
        return if (exerciseId != null) {
            loggedWorkoutDao.getWorkoutsWithExerciseInDateRange(exerciseId, startDate, endDate)
        } else {
            loggedWorkoutDao.getWorkoutsByDateRange(startDate, endDate)
        }.map { workouts ->
            android.util.Log.d("AnalyticsRepository", "Found ${workouts.size} workouts in date range")
            workouts.forEach { workout ->
                android.util.Log.d("AnalyticsRepository", "Workout: ${workout.name} on ${workout.date}")
            }
            
            workouts.mapNotNull { workout ->
                try {
                    val totalVolume = if (exerciseId != null) {
                        calculateExerciseVolumeInWorkout(workout, exerciseId)
                    } else {
                        calculateTotalWorkoutVolume(workout)
                    }
                    
                    android.util.Log.d("AnalyticsRepository", "Workout ${workout.name}: totalVolume = $totalVolume")
                    
                    VolumeDataPoint(
                        date = workout.date,
                        totalVolume = totalVolume,
                        workoutName = workout.name,
                        cycleId = workout.activeProgramCycleId
                    )
                } catch (e: Exception) {
                    // Skip workouts that cause errors
                    android.util.Log.e("AnalyticsRepository", "Error calculating volume for workout ${workout.name}: ${e.message}")
                    null
                }
            }.also { volumeDataPoints ->
                android.util.Log.d("AnalyticsRepository", "Final volume data points: ${volumeDataPoints.size}")
                volumeDataPoints.forEach { point ->
                    android.util.Log.d("AnalyticsRepository", "VolumeDataPoint: ${point.date} - ${point.totalVolume}kg")
                }
                android.util.Log.d("AnalyticsRepository", "=== END VOLUME PROGRESSION DATA DEBUG ===")
            }
        }
    }
    
    fun getWeeklyVolumeSummary(weekStartDate: String): Flow<VolumeSummary> {
        val weekEndDate = LocalDate.parse(weekStartDate).plusDays(6).toString()
        
        return loggedWorkoutDao.getWorkoutsByDateRange(weekStartDate, weekEndDate)
            .map { workouts ->
                val exerciseVolumeMap = mutableMapOf<String, MutableList<Double>>()
                val exerciseSetCountMap = mutableMapOf<String, Int>()
                val exerciseWeightMap = mutableMapOf<String, MutableList<Double>>()
                var totalVolume = 0.0
                
                workouts.forEach { workout ->
                    workout.loggedExercises.forEach { exercise ->
                        val exerciseVolume = exercise.sets.sumOf { set ->
                            (set.weight ?: 0.0) * (set.reps ?: 0)
                        }
                        
                        exerciseVolumeMap.getOrPut(exercise.exerciseId) { mutableListOf() }
                            .add(exerciseVolume)
                        
                        exerciseSetCountMap[exercise.exerciseId] = 
                            (exerciseSetCountMap[exercise.exerciseId] ?: 0) + exercise.sets.size
                        
                        exercise.sets.forEach { set ->
                            set.weight?.let { weight ->
                                exerciseWeightMap.getOrPut(exercise.exerciseId) { mutableListOf() }
                                    .add(weight)
                            }
                        }
                        
                        totalVolume += exerciseVolume
                    }
                }
                
                val exerciseBreakdown = exerciseVolumeMap.map { (exerciseId, volumes) ->
                    val exercise = workouts.flatMap { it.loggedExercises }
                        .first { it.exerciseId == exerciseId }
                    
                    ExerciseVolumeBreakdown(
                        exerciseId = exerciseId,
                        exerciseName = exercise.exerciseName,
                        muscleGroups = exercise.targetMuscleGroups,
                        totalVolume = volumes.sum(),
                        setCount = exerciseSetCountMap[exerciseId] ?: 0,
                        averageWeight = exerciseWeightMap[exerciseId]?.average()
                    )
                }
                
                VolumeSummary(
                    periodLabel = "Week of $weekStartDate",
                    startDate = weekStartDate,
                    endDate = weekEndDate,
                    totalVolume = totalVolume,
                    workoutCount = workouts.size,
                    averageVolumePerWorkout = if (workouts.isNotEmpty()) totalVolume / workouts.size else 0.0,
                    exerciseBreakdown = exerciseBreakdown
                )
            }
    }
    
    fun getMuscleGroupVolumeDistribution(
        startDate: String,
        endDate: String
    ): Flow<List<MuscleGroupVolume>> {
        return loggedWorkoutDao.getWorkoutsByDateRange(startDate, endDate)
            .map { workouts ->
                val muscleGroupVolumeMap = mutableMapOf<MuscleGroup, Double>()
                val muscleGroupExerciseCount = mutableMapOf<MuscleGroup, MutableSet<String>>()
                var totalVolume = 0.0
                
                workouts.forEach { workout ->
                    workout.loggedExercises.forEach { exercise ->
                        val exerciseVolume = exercise.sets.sumOf { set ->
                            (set.weight ?: 0.0) * (set.reps ?: 0)
                        }
                        
                        exercise.targetMuscleGroups.forEach { muscleGroup ->
                            val volumePerMuscle = exerciseVolume / exercise.targetMuscleGroups.size
                            muscleGroupVolumeMap[muscleGroup] = 
                                (muscleGroupVolumeMap[muscleGroup] ?: 0.0) + volumePerMuscle
                            
                            muscleGroupExerciseCount.getOrPut(muscleGroup) { mutableSetOf() }
                                .add(exercise.exerciseId)
                        }
                        
                        totalVolume += exerciseVolume
                    }
                }
                
                muscleGroupVolumeMap.map { (muscleGroup, volume) ->
                    MuscleGroupVolume(
                        muscleGroup = muscleGroup,
                        totalVolume = volume,
                        percentage = if (totalVolume > 0) (volume / totalVolume) * 100 else 0.0,
                        exerciseCount = muscleGroupExerciseCount[muscleGroup]?.size ?: 0
                    )
                }.sortedByDescending { it.totalVolume }
            }
    }
    
    // EXERCISE-SPECIFIC PERFORMANCE TRENDS
    
    fun getExercisePerformanceTrend(exerciseId: String): Flow<PerformanceTrend?> {
        return loggedWorkoutDao.getAllWorkoutsWithExercise(exerciseId)
            .map { workouts ->
                try {
                    // Get exercise info once for all workouts (must run on IO thread)
                    val exerciseInfo = withContext(Dispatchers.IO) {
                        exerciseDao?.getExerciseById(exerciseId)
                    }
                    val usesBodyweight = exerciseInfo?.usesBodyweight ?: false
                    
                    val dataPoints = workouts.mapNotNull { workout ->
                        try {
                            val exercise = workout.loggedExercises.find { it.exerciseId == exerciseId }
                            exercise?.let {
                                val bestSet = findBestSetInExercise(it, workout, usesBodyweight)
                                val totalVolume = calculateExerciseVolumeInWorkout(workout, exerciseId)
                                val userBodyweight = workout.bodyweight ?: 0.0
                                
                                val totalEffectiveWeight = bestSet?.weight?.let { weight ->
                                    calculateTotalEffectiveWeight(weight, usesBodyweight, userBodyweight)
                                }
                                
                                val estimated1RM = bestSet?.let { set ->
                                    if (set.weight != null && set.reps != null && set.reps > 0) {
                                        val effectiveWeight = calculateTotalEffectiveWeight(set.weight, usesBodyweight, userBodyweight)
                                        StrengthAnalytics.calculateEpley1RM(effectiveWeight, set.reps)
                                    } else null
                                }
                                
                                ExercisePerformancePoint(
                                    date = workout.date,
                                    exerciseId = exerciseId,
                                    exerciseName = it.exerciseName,
                                    bestWeight = totalEffectiveWeight, // Use total effective weight instead of just external weight
                                    bestReps = bestSet?.reps,
                                    totalVolume = totalVolume,
                                    estimated1RM = estimated1RM,
                                    workoutId = workout.id,
                                    cycleId = workout.activeProgramCycleId,
                                    // Bodyweight breakdown for clearer display
                                    usesBodyweight = usesBodyweight,
                                    bodyweight = if (usesBodyweight) userBodyweight else null,
                                    externalWeight = bestSet?.weight
                                )
                            }
                        } catch (e: Exception) {
                            // Log error and skip this workout
                            null
                        }
                    }
                    
                    val trend = analyzeTrend(dataPoints)
                    val exerciseName = dataPoints.firstOrNull()?.exerciseName ?: "Unknown Exercise"
                    
                    val result = PerformanceTrend(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        trendDirection = trend.direction,
                        trendStrength = trend.strength,
                        dataPoints = dataPoints,
                        recommendedAction = generateRecommendation(trend)
                    )
                    result
                } catch (e: Exception) {
                    // Return null if processing fails completely
                    null
                }
            }
    }
    
    fun getPersonalRecordProgress(exerciseId: String): Flow<PersonalRecordProgress?> {
        return combine(
            personalRecordDao.getPRsForExerciseFlow(exerciseId),
            loggedWorkoutDao.getRecentWorkoutsForPRAnalysis(exerciseId)
        ) { prs: List<PersonalRecord>, workouts: List<LoggedWorkout> ->
            try {
                if (prs.isEmpty()) return@combine null
                
                val currentPR = prs.maxByOrNull { 
                    try {
                        LocalDate.parse(it.date)
                    } catch (e: Exception) {
                        LocalDate.MIN // fallback for invalid dates
                    }
                }
                val previousPRs = prs.filter { it.id != currentPR?.id }
                val previousPR = previousPRs.maxByOrNull { 
                    try {
                        LocalDate.parse(it.date)
                    } catch (e: Exception) {
                        LocalDate.MIN
                    }
                }
                
                currentPR?.let { current: PersonalRecord ->
                    val improvement = calculatePRImprovement(current, previousPR)
                    
                    PersonalRecordProgress(
                        exerciseId = exerciseId,
                        exerciseName = current.exerciseName,
                        currentPR = current,
                        previousPR = previousPR,
                        improvement = improvement.value,
                        improvementType = improvement.type
                    )
                }
            } catch (e: Exception) {
                // Return null if processing fails
                null
            }
        }
    }
    
    // CYCLE COMPARISON
    
    fun compareCycles(currentCycleId: String, previousCycleId: String? = null): Flow<CycleComparison> {
        return if (previousCycleId != null) {
            combine(
                loggedWorkoutDao.getWorkoutsByCycle(currentCycleId),
                loggedWorkoutDao.getWorkoutsByCycle(previousCycleId),
                activeCycleDao.getActiveCycle()
            ) { currentWorkouts, previousWorkouts, activeCycle ->
                createCycleComparison(currentCycleId, previousCycleId, currentWorkouts, previousWorkouts, activeCycle?.cycleProgram?.name ?: "Unknown Program")
            }
        } else {
            // Compare with most recent previous cycle
            combine(
                loggedWorkoutDao.getWorkoutsByCycle(currentCycleId),
                activeCycleDao.getActiveCycle()
            ) { currentWorkouts, activeCycle ->
                // Find previous cycle automatically
                // This is a simplified version - in a real implementation, you'd query for the most recent previous cycle
                createCycleComparison(currentCycleId, null, currentWorkouts, emptyList(), activeCycle?.cycleProgram?.name ?: "Unknown Program")
            }
        }
    }
    
    // HELPER FUNCTIONS
    
    private fun calculateTotalWorkoutVolume(workout: LoggedWorkout): Double {
        return workout.loggedExercises.sumOf { exercise ->
            exercise.sets.sumOf { set ->
                (set.weight ?: 0.0) * (set.reps ?: 0)
            }
        }
    }
    
    private fun calculateExerciseVolumeInWorkout(workout: LoggedWorkout, exerciseId: String): Double {
        return workout.loggedExercises
            .filter { it.exerciseId == exerciseId }
            .sumOf { exercise ->
                exercise.sets.sumOf { set ->
                    (set.weight ?: 0.0) * (set.reps ?: 0)
                }
            }
    }
    
    private fun findBestSetInExercise(exercise: LoggedExercise, workout: LoggedWorkout, usesBodyweight: Boolean): LoggedSet? {
        val userBodyweight = workout.bodyweight ?: 0.0
        
        val validSets = exercise.sets.filter { it.weight != null && it.reps != null && it.reps > 0 }
        
        val bestSet = validSets.maxByOrNull { set ->
            val totalWeight = calculateTotalEffectiveWeight(set.weight!!, usesBodyweight, userBodyweight)
            StrengthAnalytics.calculateEpley1RM(totalWeight, set.reps!!)
        }
        
        return bestSet
    }
    
    private fun calculateTotalEffectiveWeight(externalWeight: Double, usesBodyweight: Boolean, bodyweight: Double): Double {
        return if (usesBodyweight) {
            externalWeight + bodyweight
        } else {
            externalWeight
        }
    }
    
    private data class TrendAnalysis(val direction: TrendDirection, val strength: Double)
    
    private fun analyzeTrend(dataPoints: List<ExercisePerformancePoint>): TrendAnalysis {
        if (dataPoints.size < 3) {
            return TrendAnalysis(TrendDirection.INSUFFICIENT_DATA, 0.0)
        }
        
        // Simple linear regression on estimated 1RM values
        val validPoints = dataPoints
            .filter { it.estimated1RM != null }
            .sortedBy { it.date }
        
        if (validPoints.size < 3) {
            return TrendAnalysis(TrendDirection.INSUFFICIENT_DATA, 0.0)
        }
        
        val n = validPoints.size
        val xValues = (0 until n).map { it.toDouble() }
        val yValues = validPoints.map { it.estimated1RM!! }
        
        // Calculate slope (trend direction)
        val xMean = xValues.average()
        val yMean = yValues.average()
        
        val numerator = xValues.zip(yValues) { x, y -> (x - xMean) * (y - yMean) }.sum()
        val denominator = xValues.sumOf { (it - xMean) * (it - xMean) }
        
        val slope = if (denominator != 0.0) numerator / denominator else 0.0
        val strength = minOf(abs(slope) / 5.0, 1.0) // Normalize strength to 0-1
        
        val direction = when {
            slope > 2.0 -> TrendDirection.STRONGLY_IMPROVING
            slope > 0.5 -> TrendDirection.SLIGHTLY_IMPROVING
            slope > -0.5 -> TrendDirection.STABLE
            slope > -2.0 -> TrendDirection.SLIGHTLY_DECLINING
            else -> TrendDirection.STRONGLY_DECLINING
        }
        
        return TrendAnalysis(direction, strength)
    }
    
    private fun generateRecommendation(trend: TrendAnalysis): String {
        return when (trend.direction) {
            TrendDirection.STRONGLY_IMPROVING -> "Great progress! Consider gradually increasing intensity."
            TrendDirection.SLIGHTLY_IMPROVING -> "Steady improvement. Keep up the consistent training."
            TrendDirection.STABLE -> "Performance is stable. Consider varying rep ranges or adding volume."
            TrendDirection.SLIGHTLY_DECLINING -> "Minor decline detected. Check recovery and consider deload."
            TrendDirection.STRONGLY_DECLINING -> "Significant decline. Consider form check, recovery, or deload week."
            TrendDirection.INSUFFICIENT_DATA -> "More data needed for trend analysis."
        }
    }
    
    private data class PRImprovementData(val value: Double?, val type: PRImprovementType)
    
    private fun calculatePRImprovement(current: PersonalRecord, previous: PersonalRecord?): PRImprovementData {
        if (previous == null) {
            return PRImprovementData(null, PRImprovementType.NEW_PR)
        }
        
        return when (current.type) {
            PRType.MAX_WEIGHT_FOR_REPS -> {
                val weightImprovement = ((current.weight ?: 0.0) - (previous.weight ?: 0.0))
                if (weightImprovement > 0) {
                    PRImprovementData(weightImprovement, PRImprovementType.WEIGHT_INCREASE)
                } else {
                    PRImprovementData(0.0, PRImprovementType.NO_IMPROVEMENT)
                }
            }
            PRType.MAX_REPS_AT_WEIGHT -> {
                val repImprovement = (current.reps ?: 0) - (previous.reps ?: 0)
                if (repImprovement > 0) {
                    PRImprovementData(repImprovement.toDouble(), PRImprovementType.REP_INCREASE)
                } else {
                    PRImprovementData(0.0, PRImprovementType.NO_IMPROVEMENT)
                }
            }
            PRType.DURATION -> {
                val durationImprovement = (current.durationSecs ?: 0) - (previous.durationSecs ?: 0)
                if (durationImprovement > 0) {
                    PRImprovementData(durationImprovement.toDouble(), PRImprovementType.DURATION_INCREASE)
                } else {
                    PRImprovementData(0.0, PRImprovementType.NO_IMPROVEMENT)
                }
            }
        }
    }
    
    private fun createCycleComparison(
        currentCycleId: String,
        previousCycleId: String?,
        currentWorkouts: List<LoggedWorkout>,
        previousWorkouts: List<LoggedWorkout>,
        programName: String
    ): CycleComparison {
        val currentVolume = currentWorkouts.sumOf { calculateTotalWorkoutVolume(it) }
        val previousVolume = previousWorkouts.sumOf { calculateTotalWorkoutVolume(it) }
        
        val volumeChange = if (previousVolume > 0) {
            ((currentVolume - previousVolume) / previousVolume) * 100
        } else null
        
        val strengthGains = calculateStrengthGains(currentWorkouts, previousWorkouts)
        
        val currentDurations = currentWorkouts.mapNotNull { workout ->
            if (workout.startTimestamp != null && workout.endTimestamp != null) {
                (workout.endTimestamp - workout.startTimestamp) / 60000 // Convert to minutes
            } else null
        }
        
        val avgDuration = if (currentDurations.isNotEmpty()) {
            currentDurations.average().toLong()
        } else null
        
        return CycleComparison(
            currentCycleId = currentCycleId,
            previousCycleId = previousCycleId,
            programTemplateName = programName,
            totalVolumeChange = volumeChange,
            strengthGains = strengthGains,
            completionRate = 100.0, // This would need actual program template data for accurate calculation
            averageWorkoutDuration = avgDuration
        )
    }
    
    private fun calculateStrengthGains(
        currentWorkouts: List<LoggedWorkout>,
        previousWorkouts: List<LoggedWorkout>
    ): List<ExerciseStrengthGain> {
        val currentExerciseData = mutableMapOf<String, MutableList<LoggedSet>>()
        val previousExerciseData = mutableMapOf<String, MutableList<LoggedSet>>()
        
        currentWorkouts.forEach { workout ->
            workout.loggedExercises.forEach { exercise ->
                currentExerciseData.getOrPut(exercise.exerciseId) { mutableListOf() }
                    .addAll(exercise.sets)
            }
        }
        
        previousWorkouts.forEach { workout ->
            workout.loggedExercises.forEach { exercise ->
                previousExerciseData.getOrPut(exercise.exerciseId) { mutableListOf() }
                    .addAll(exercise.sets)
            }
        }
        
        return currentExerciseData.mapNotNull { (exerciseId, currentSets) ->
            val previousSets = previousExerciseData[exerciseId]
            if (previousSets != null) {
                val exerciseName = currentWorkouts.flatMap { it.loggedExercises }
                    .find { it.exerciseId == exerciseId }?.exerciseName ?: "Unknown"
                
                val currentBest = currentSets.filter { it.weight != null && it.reps != null }
                    .maxByOrNull { StrengthAnalytics.calculateEpley1RM(it.weight!!, it.reps!!) }
                
                val previousBest = previousSets.filter { it.weight != null && it.reps != null }
                    .maxByOrNull { StrengthAnalytics.calculateEpley1RM(it.weight!!, it.reps!!) }
                
                if (currentBest != null && previousBest != null) {
                    val current1RM = StrengthAnalytics.calculateEpley1RM(currentBest.weight!!, currentBest.reps!!)
                    val previous1RM = StrengthAnalytics.calculateEpley1RM(previousBest.weight!!, previousBest.reps!!)
                    
                    val strengthGain = ((current1RM - previous1RM) / previous1RM) * 100
                    val weightIncrease = (currentBest.weight ?: 0.0) - (previousBest.weight ?: 0.0)
                    val repIncrease = (currentBest.reps ?: 0) - (previousBest.reps ?: 0)
                    
                    ExerciseStrengthGain(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        strengthGainPercentage = strengthGain,
                        weightIncrease = weightIncrease,
                        repIncrease = repIncrease
                    )
                } else null
            } else null
        }
    }
    
    // DASHBOARD-SPECIFIC METHODS
    
    suspend fun getTotalWorkoutCount(): Int {
        return loggedWorkoutDao.totalWorkoutCount()
    }
    
    suspend fun getThisWeekWorkoutCount(): Int {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
        val endOfWeek = startOfWeek.plusDays(6)
        
        val startDate = startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = endOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        return loggedWorkoutDao.workoutCountBetweenDates(startDate, endDate)
    }
    
    suspend fun getAverageWeeklyVolume(): Float {
        val endDate = LocalDate.now()
        val startDate = endDate.minusWeeks(12) // Last 12 weeks
        
        val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val workouts = loggedWorkoutDao.getWorkoutsByDateRange(startDateStr, endDateStr).first()
        
        if (workouts.isEmpty()) return 0f
        
        val weeklyVolumes = mutableMapOf<Int, Float>()
        
        workouts.forEach { workout ->
            val workoutDate = LocalDate.parse(workout.date)
            val weekOfYear = workoutDate.dayOfYear / 7 // Simplified week calculation
            val volume = calculateTotalWorkoutVolume(workout)
            
            weeklyVolumes[weekOfYear] = (weeklyVolumes[weekOfYear] ?: 0f) + volume.toFloat()
        }
        
        return if (weeklyVolumes.isNotEmpty()) {
            weeklyVolumes.values.average().toFloat()
        } else 0f
    }

    suspend fun getCurrentStreak(): Int {
        val startDate = LocalDate.now().minusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val workouts = loggedWorkoutDao.getWorkoutsByDateRange(startDate, endDate).first()
        if (workouts.isEmpty()) return 0
        
        // Sort workouts by date (most recent first)
        val sortedWorkouts = workouts.sortedByDescending { it.date }
        
        var streak = 0
        var currentDate = LocalDate.now()
        
        for (workout in sortedWorkouts) {
            val workoutDate = LocalDate.parse(workout.date)
            
            // Check if workout is on current date or previous consecutive days
            if (workoutDate == currentDate || workoutDate == currentDate.minusDays(1)) {
                streak++
                currentDate = workoutDate.minusDays(1) // Move to previous day to check
            } else {
                break // Break streak if there's a gap
            }
        }
        
        return streak
    }
    
    suspend fun getVolumeData(startDate: LocalDate, endDate: LocalDate): List<VolumeDataPoint> {
        val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Group workouts by week and calculate weekly volume
        val workouts = loggedWorkoutDao.getWorkoutsByDateRange(startDateStr, endDateStr).first()
        val weeklyData = mutableMapOf<LocalDate, Double>()
        
        workouts.forEach { workout ->
            val workoutDate = LocalDate.parse(workout.date)
            val weekStart = workoutDate.minusDays(workoutDate.dayOfWeek.value - 1L)
            val volume = calculateTotalWorkoutVolume(workout)
            
            weeklyData[weekStart] = (weeklyData[weekStart] ?: 0.0) + volume
        }
        
        return weeklyData.map { (date, volume) ->
            VolumeDataPoint(
                date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                totalVolume = volume,
                workoutName = "Weekly Total",
                cycleId = null
            )
        }.sortedBy { it.date }
    }
}