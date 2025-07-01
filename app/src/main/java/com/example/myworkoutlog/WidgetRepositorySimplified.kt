package com.example.myworkoutlog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

class WidgetRepositorySimplified(
    private val analyticsRepository: AnalyticsRepository,
    private val personalRecordDao: PersonalRecordDao,
    private val loggedWorkoutDao: LoggedWorkoutDao,
    private val activeCycleDao: ActiveCycleDao,
    private val programTemplateDao: ProgramTemplateDao,
    private val workoutTemplateDao: WorkoutTemplateDao
) {
    
    suspend fun getDashboardState(activeCycle: ActiveProgramCycle?): Flow<DashboardState> {
        return if (activeCycle == null) {
            getNoActiveCycleDashboardState()
        } else {
            getActiveCycleDashboardState(activeCycle)
        }
    }
    
    private suspend fun getNoActiveCycleDashboardState(): Flow<DashboardState> = flow {
        val widgets = mutableListOf<DashboardWidget>()
        
        // Get bodyweight info
        val bodyweightInfo = getLatestBodyweightInfo()
        
        // Welcome widget
        val greeting = getTimeBasedGreeting()
        val streak = calculateBasicStreak()
        widgets.add(DashboardWidget.WelcomeWidget(
            greeting = greeting,
            userName = null,
            currentStreak = streak,
            motivationalMessage = getMotivationalMessage(streak)
        ))
        
        // Quick stats
        val totalWorkouts = try {
            analyticsRepository.getTotalWorkoutCount()
        } catch (e: Exception) { 0 }
        
        val recentPRs = try {
            personalRecordDao.recentPersonalRecords(5)
        } catch (e: Exception) { emptyList() }
        
        widgets.add(DashboardWidget.QuickStatsWidget(
            totalWorkouts = totalWorkouts,
            currentStreak = streak,
            bestWeek = "N/A",
            recentPRs = recentPRs
        ))
        
        // Bodyweight widget
        bodyweightInfo?.let { info ->
            widgets.add(DashboardWidget.BodyweightWidget(
                currentWeight = info.weight,
                lastRecordedDate = info.date,
                unit = info.unit
            ))
        }
        
        // Bodyweight trend chart
        val bodyweightData = getBodyweightTrendData()
        if (bodyweightData.isNotEmpty()) {
            widgets.add(DashboardWidget.BodyweightTrendWidget(
                bodyweightData = bodyweightData,
                trend = calculateBodyweightTrend(bodyweightData)
            ))
        }
        
        // Performance trends widget - always show with sample data if no real data
        val performanceTrends = getTopPerformanceTrends()
        val finalPerformanceTrends = if (performanceTrends.isNotEmpty()) {
            performanceTrends
        } else {
            // Create sample data for demonstration
            listOf(
                ExerciseProgress(
                    exerciseName = "Bench Press",
                    currentMax = 100f,
                    previousMax = 95f,
                    improvementPercentage = 5.3f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 5.3f,
                        description = "Improving"
                    )
                ),
                ExerciseProgress(
                    exerciseName = "Squat",
                    currentMax = 120f,
                    previousMax = 115f,
                    improvementPercentage = 4.3f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 4.3f,
                        description = "Improving"
                    )
                ),
                ExerciseProgress(
                    exerciseName = "Deadlift",
                    currentMax = 140f,
                    previousMax = 135f,
                    improvementPercentage = 3.7f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 3.7f,
                        description = "Improving"
                    )
                )
            )
        }
        
        widgets.add(DashboardWidget.PerformanceTrendWidget(
            strengthGains = finalPerformanceTrends,
            volumeTrend = calculateOverallVolumeTrend(),
            timeframe = if (performanceTrends.isNotEmpty()) "Last 30 Days" else "Sample Data"
        ))
        
        // Volume progress widget
        val volumeData = getWeeklyVolumeData()
        if (volumeData.isNotEmpty()) {
            widgets.add(DashboardWidget.VolumeProgressWidget(
                weeklyVolume = volumeData,
                trend = calculateVolumeProgressTrend(volumeData),
                targetVolume = null // Could be user-set goal
            ))
        }
        
        // Achievement widget
        val recentAchievements = getRecentAchievements()
        val nextMilestone = getNextMilestone()
        if (recentAchievements.isNotEmpty() || nextMilestone != null) {
            widgets.add(DashboardWidget.AchievementWidget(
                recentAchievements = recentAchievements,
                nextMilestone = nextMilestone
            ))
        }
        
        // Activity heatmap
        val activityData = getActivityHeatmapData()
        widgets.add(DashboardWidget.ActivityHeatmapWidget(
            workoutDays = activityData,
            streakInfo = getStreakInfo()
        ))
        
        emit(DashboardState(
            mode = DashboardMode.NoActiveCycle,
            widgets = widgets,
            quickActions = getBasicQuickActions(null),
            insights = generateBasicInsights(activeCycle = null)
        ))
    }
    
    private suspend fun getActiveCycleDashboardState(activeCycle: ActiveProgramCycle): Flow<DashboardState> = flow {
        val widgets = mutableListOf<DashboardWidget>()
        
        // Get bodyweight info
        val bodyweightInfo = getLatestBodyweightInfo()
        
        // Cycle progress
        val progress = calculateBasicCycleProgress(activeCycle)
        val (weekProgress, sessionProgress) = calculateCycleProgressText(activeCycle)
        widgets.add(DashboardWidget.CycleProgressWidget(
            cycle = activeCycle,
            completionPercentage = progress,
            weekProgress = weekProgress,
            sessionProgress = sessionProgress,
            nextSession = null,
            daysUntilNext = null
        ))
        
        // Bodyweight widget
        bodyweightInfo?.let { info ->
            widgets.add(DashboardWidget.BodyweightWidget(
                currentWeight = info.weight,
                lastRecordedDate = info.date,
                unit = info.unit
            ))
        }
        
        // Bodyweight trend chart
        val bodyweightData = getBodyweightTrendData()
        if (bodyweightData.isNotEmpty()) {
            widgets.add(DashboardWidget.BodyweightTrendWidget(
                bodyweightData = bodyweightData,
                trend = calculateBodyweightTrend(bodyweightData)
            ))
        }
        
        // Performance trends widget for active cycle - always show with sample data if no real data
        val performanceTrends = getTopPerformanceTrends()
        val finalPerformanceTrends = if (performanceTrends.isNotEmpty()) {
            performanceTrends
        } else {
            // Create sample data for demonstration
            listOf(
                ExerciseProgress(
                    exerciseName = "Bench Press",
                    currentMax = 100f,
                    previousMax = 95f,
                    improvementPercentage = 5.3f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 5.3f,
                        description = "Improving"
                    )
                ),
                ExerciseProgress(
                    exerciseName = "Squat",
                    currentMax = 120f,
                    previousMax = 115f,
                    improvementPercentage = 4.3f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 4.3f,
                        description = "Improving"
                    )
                ),
                ExerciseProgress(
                    exerciseName = "Deadlift",
                    currentMax = 140f,
                    previousMax = 135f,
                    improvementPercentage = 3.7f,
                    trend = ProgressTrend(
                        direction = TrendDirection.SLIGHTLY_IMPROVING,
                        percentage = 3.7f,
                        description = "Improving"
                    )
                )
            )
        }
        
        widgets.add(DashboardWidget.PerformanceTrendWidget(
            strengthGains = finalPerformanceTrends,
            volumeTrend = calculateOverallVolumeTrend(),
            timeframe = if (performanceTrends.isNotEmpty()) "This Cycle" else "Sample Data"
        ))
        
        // Volume progress widget for active cycle
        val volumeData = getWeeklyVolumeData()
        if (volumeData.isNotEmpty()) {
            widgets.add(DashboardWidget.VolumeProgressWidget(
                weeklyVolume = volumeData,
                trend = calculateVolumeProgressTrend(volumeData),
                targetVolume = null // Could be cycle-specific target
            ))
        }
        
        // Achievement widget for active cycle
        val recentAchievements = getRecentAchievements()
        val nextMilestone = getNextMilestone()
        if (recentAchievements.isNotEmpty() || nextMilestone != null) {
            widgets.add(DashboardWidget.AchievementWidget(
                recentAchievements = recentAchievements,
                nextMilestone = nextMilestone
            ))
        }
        
        // Next session widget for active cycle
        val nextSessionData = getNextSessionWithNavigation(activeCycle)
        nextSessionData?.let { (sessionInfo, weekId, sessionId) ->
            widgets.add(DashboardWidget.NextSessionWidget(
                session = sessionInfo,
                estimatedDuration = sessionInfo.estimatedDuration,
                exercises = sessionInfo.exercises,
                difficulty = sessionInfo.difficulty,
                cycleId = activeCycle.cycleUuid,
                weekId = weekId,
                sessionId = sessionId,
                templateId = sessionInfo.templateId
            ))
        }
        
        // Activity heatmap for active cycle
        val activityData = getActivityHeatmapData()
        widgets.add(DashboardWidget.ActivityHeatmapWidget(
            workoutDays = activityData,
            streakInfo = getStreakInfo()
        ))
        
        emit(DashboardState(
            mode = DashboardMode.ActiveCycle(activeCycle, CycleProgress(
                completionPercentage = progress,
                weekProgress = weekProgress,
                sessionProgress = sessionProgress,
                strengthGains = emptyList(),
                volumeTrend = ProgressTrend(TrendDirection.STABLE, 0f, "No data"),
                consistency = 0.8f,
                estimatedCompletion = null
            )),
            widgets = widgets,
            quickActions = getBasicQuickActions(activeCycle),
            insights = generateBasicInsights(activeCycle = activeCycle)
        ))
    }
    
    private fun getTimeBasedGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning!"
            in 12..16 -> "Good afternoon!"
            else -> "Good evening!"
        }
    }
    
    private suspend fun calculateBasicStreak(): Int {
        return try {
            // Simplified streak calculation
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            if (workouts.isNotEmpty()) 1 else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getMotivationalMessage(streak: Int): String {
        return when {
            streak == 0 -> "Ready to start your fitness journey?"
            streak < 7 -> "Great start! Keep building that momentum."
            else -> "Incredible consistency! Keep it up!"
        }
    }
    
    private fun calculateBasicCycleProgress(activeCycle: ActiveProgramCycle): Float {
        // Calculate actual progress based on completed sessions vs total sessions
        // Return value between 0.0 and 1.0 for CircularProgressIndicator
        val totalSessions = activeCycle.cycleProgram.weeks.sumOf { it.sessions.size }
        val completedSessions = activeCycle.completedSessions.size
        
        return if (totalSessions > 0) {
            completedSessions.toFloat() / totalSessions.toFloat()
        } else {
            0f
        }
    }
    
    private fun getBasicQuickActions(activeCycle: ActiveProgramCycle?): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        
        if (activeCycle != null) {
            val isCycleCompleted = isCycleCompleted(activeCycle)
            
            if (isCycleCompleted) {
                // Cycle is completed - show completion actions
                actions.add(QuickAction(
                    id = "complete_cycle",
                    title = "Complete Cycle",
                    description = "Finish current cycle and view results",
                    icon = Icons.Default.CheckCircle,
                    action = QuickActionType.COMPLETE_CYCLE
                ))
                actions.add(QuickAction(
                    id = "cycle_analytics",
                    title = "View Cycle Analytics",
                    description = "See your progress for this cycle",
                    icon = Icons.Default.Analytics,
                    action = QuickActionType.VIEW_CYCLE_ANALYTICS
                ))
            } else {
                // Cycle is ongoing - show next session action
                actions.add(QuickAction(
                    id = "start_next",
                    title = "Start Next Session",
                    description = "Continue your active cycle",
                    icon = Icons.Default.Refresh,
                    action = QuickActionType.START_NEXT_SESSION
                ))
            }
        } else {
            actions.add(QuickAction(
                id = "start_cycle",
                title = "Start New Cycle",
                description = "Begin a new program",
                icon = Icons.Default.Person,
                action = QuickActionType.START_NEW_CYCLE
            ))
        }
        
        actions.add(QuickAction(
            id = "analytics",
            title = "View Analytics",
            description = "Check your progress",
            icon = Icons.Default.Info,
            action = QuickActionType.VIEW_ANALYTICS
        ))
        
        return actions
    }
    
    private suspend fun getLatestBodyweightInfo(): BodyweightInfo? {
        return try {
            val latestWorkout = loggedWorkoutDao.getLatestLoggedWorkoutWithBodyweight()
            latestWorkout?.let { workout ->
                BodyweightInfo(
                    weight = workout.bodyweight ?: 0.0,
                    date = workout.date,
                    unit = "kg" // Default unit, could be made configurable
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateCycleProgressText(activeCycle: ActiveProgramCycle): Pair<String, String> {
        val totalWeeks = activeCycle.cycleProgram.weeks.size
        val totalSessions = activeCycle.cycleProgram.weeks.sumOf { it.sessions.size }
        val completedSessionsCount = activeCycle.completedSessions.size
        
        // Calculate current week based on completed sessions
        var currentWeek = 1
        var sessionsInCurrentWeek = 0
        var totalSessionsProcessed = 0
        
        activeCycle.cycleProgram.weeks.sortedBy { it.order }.forEachIndexed { weekIndex, week ->
            val weekSessionCount = week.sessions.size
            if (totalSessionsProcessed + weekSessionCount <= completedSessionsCount) {
                // This week is fully completed
                totalSessionsProcessed += weekSessionCount
                currentWeek = (weekIndex + 2).coerceAtMost(totalWeeks) // Next week or stay at last week
            } else if (totalSessionsProcessed < completedSessionsCount) {
                // This week is partially completed
                currentWeek = weekIndex + 1
                sessionsInCurrentWeek = completedSessionsCount - totalSessionsProcessed
                return@forEachIndexed
            }
        }
        
        // If all sessions are completed, stay at the last week
        if (completedSessionsCount >= totalSessions) {
            currentWeek = totalWeeks
        }
        
        val weekProgress = "Week $currentWeek of $totalWeeks"
        val sessionProgress = "$completedSessionsCount of $totalSessions sessions completed"
        
        return Pair(weekProgress, sessionProgress)
    }
    
    private fun isCycleCompleted(activeCycle: ActiveProgramCycle): Boolean {
        val totalSessions = activeCycle.cycleProgram.weeks.sumOf { it.sessions.size }
        val completedSessions = activeCycle.completedSessions.size
        return completedSessions >= totalSessions
    }
    
    private suspend fun getBodyweightTrendData(): List<BodyweightPoint> {
        return try {
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            workouts
                .filter { it.bodyweight != null && it.bodyweight!! > 0 }
                .sortedBy { it.date }
                .takeLast(30) // Last 30 data points
                .map { workout ->
                    BodyweightPoint(
                        date = java.time.LocalDate.parse(workout.date),
                        weight = workout.bodyweight!!.toFloat()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun calculateBodyweightTrend(data: List<BodyweightPoint>): ProgressTrend {
        if (data.size < 2) {
            return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Not enough data")
        }
        
        // Simple approach: compare most recent to oldest for small datasets
        if (data.size < 14) {
            val oldest = data.first().weight
            val newest = data.last().weight
            val changePercentage = ((newest - oldest) / oldest) * 100
            
            val direction = when {
                changePercentage > 2f -> TrendDirection.STRONGLY_IMPROVING
                changePercentage > 0.5f -> TrendDirection.SLIGHTLY_IMPROVING
                changePercentage < -2f -> TrendDirection.STRONGLY_DECLINING
                changePercentage < -0.5f -> TrendDirection.SLIGHTLY_DECLINING
                else -> TrendDirection.STABLE
            }
            
            return ProgressTrend(
                direction = direction,
                percentage = kotlin.math.abs(changePercentage),
                description = when (direction) {
                    TrendDirection.STRONGLY_IMPROVING -> "Significant increase"
                    TrendDirection.SLIGHTLY_IMPROVING -> "Slight increase"
                    TrendDirection.STRONGLY_DECLINING -> "Significant decrease"
                    TrendDirection.SLIGHTLY_DECLINING -> "Slight decrease"
                    TrendDirection.STABLE -> "Stable trend"
                    else -> "No clear trend"
                }
            )
        }
        
        // For larger datasets, use weekly comparison
        val recent = data.takeLast(7) // Last week
        val previous = data.dropLast(7).takeLast(7) // Previous week
        
        if (recent.isEmpty() || previous.isEmpty()) {
            return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Not enough data")
        }
        
        val recentAvg = recent.map { it.weight }.average().toFloat()
        val previousAvg = previous.map { it.weight }.average().toFloat()
        
        val changePercentage = ((recentAvg - previousAvg) / previousAvg) * 100
        
        val direction = when {
            changePercentage > 2f -> TrendDirection.STRONGLY_IMPROVING
            changePercentage > 0.5f -> TrendDirection.SLIGHTLY_IMPROVING
            changePercentage < -2f -> TrendDirection.STRONGLY_DECLINING
            changePercentage < -0.5f -> TrendDirection.SLIGHTLY_DECLINING
            else -> TrendDirection.STABLE
        }
        
        return ProgressTrend(
            direction = direction,
            percentage = kotlin.math.abs(changePercentage),
            description = when (direction) {
                TrendDirection.STRONGLY_IMPROVING -> "Significant increase"
                TrendDirection.SLIGHTLY_IMPROVING -> "Slight increase"
                TrendDirection.STRONGLY_DECLINING -> "Significant decrease"
                TrendDirection.SLIGHTLY_DECLINING -> "Slight decrease"
                TrendDirection.STABLE -> "Stable trend"
                else -> "No clear trend"
            }
        )
    }
    
    private suspend fun getTopPerformanceTrends(): List<ExerciseProgress> {
        return try {
            // Get the most frequently performed exercises
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            val exerciseFrequency = mutableMapOf<String, Pair<String, Int>>() // exerciseId to (name, count)
            
            workouts.forEach { workout ->
                workout.loggedExercises.forEach { exercise ->
                    val current = exerciseFrequency[exercise.exerciseId]
                    exerciseFrequency[exercise.exerciseId] = Pair(
                        exercise.exerciseName,
                        (current?.second ?: 0) + 1
                    )
                }
            }
            
            // Get top 3 most frequent exercises
            val topExercises = exerciseFrequency.entries
                .sortedByDescending { it.value.second }
                .take(3)
                .map { it.key to it.value.first }
            
            // Use Analytics methods for consistent trend calculation
            topExercises.mapNotNull { (exerciseId, exerciseName) ->
                convertAnalyticsTrendToExerciseProgress(exerciseId, exerciseName)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun convertAnalyticsTrendToExerciseProgress(
        exerciseId: String,
        exerciseName: String
    ): ExerciseProgress? {
        return try {
            // Use Analytics repository for consistent trend calculation
            val performanceTrend = analyticsRepository.getExercisePerformanceTrend(exerciseId).first()
            
            performanceTrend?.let { trend ->
                // Extract current and previous performance from Analytics data
                val dataPoints = trend.dataPoints.sortedBy { it.date }
                if (dataPoints.size < 2) return null
                
                val currentPoint = dataPoints.last()
                val previousPoint = dataPoints[dataPoints.size - 2]
                
                val currentMax = currentPoint.estimated1RM?.toFloat() ?: currentPoint.bestWeight?.toFloat() ?: 0f
                val previousMax = previousPoint.estimated1RM?.toFloat() ?: previousPoint.bestWeight?.toFloat() ?: 0f
                
                if (currentMax == 0f || previousMax == 0f) return null
                
                val improvementPercentage = ((currentMax - previousMax) / previousMax) * 100
                
                ExerciseProgress(
                    exerciseName = exerciseName,
                    currentMax = currentMax,
                    previousMax = previousMax,
                    improvementPercentage = improvementPercentage,
                    trend = ProgressTrend(
                        direction = trend.trendDirection,
                        percentage = kotlin.math.abs(improvementPercentage),
                        description = when (trend.trendDirection) {
                            TrendDirection.STRONGLY_IMPROVING -> "Great progress!"
                            TrendDirection.SLIGHTLY_IMPROVING -> "Steady gains"
                            TrendDirection.STRONGLY_DECLINING -> "Needs attention"
                            TrendDirection.SLIGHTLY_DECLINING -> "Slight decline"
                            TrendDirection.STABLE -> "Maintaining"
                            else -> "Insufficient data"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    
    private suspend fun calculateOverallVolumeTrend(): ProgressTrend {
        return try {
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            if (workouts.size < 4) {
                return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Need more data")
            }
            
            val recentVolume = workouts.take(7).sumOf { workout ->
                workout.loggedExercises.sumOf { exercise ->
                    exercise.sets.sumOf { set ->
                        (set.weight ?: 0.0) * (set.reps ?: 0)
                    }
                }
            }
            
            val previousVolume = workouts.drop(7).take(7).sumOf { workout ->
                workout.loggedExercises.sumOf { exercise ->
                    exercise.sets.sumOf { set ->
                        (set.weight ?: 0.0) * (set.reps ?: 0)
                    }
                }
            }
            
            if (previousVolume == 0.0) {
                return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Need more data")
            }
            
            val changePercentage = ((recentVolume - previousVolume) / previousVolume * 100).toFloat()
            
            val direction = when {
                changePercentage > 10f -> TrendDirection.STRONGLY_IMPROVING
                changePercentage > 3f -> TrendDirection.SLIGHTLY_IMPROVING
                changePercentage < -10f -> TrendDirection.STRONGLY_DECLINING
                changePercentage < -3f -> TrendDirection.SLIGHTLY_DECLINING
                else -> TrendDirection.STABLE
            }
            
            ProgressTrend(
                direction = direction,
                percentage = kotlin.math.abs(changePercentage),
                description = when (direction) {
                    TrendDirection.STRONGLY_IMPROVING -> "Volume increasing rapidly"
                    TrendDirection.SLIGHTLY_IMPROVING -> "Volume trending up"
                    TrendDirection.STRONGLY_DECLINING -> "Volume decreasing significantly"
                    TrendDirection.SLIGHTLY_DECLINING -> "Volume declining slightly"
                    TrendDirection.STABLE -> "Volume stable"
                    else -> "Insufficient data"
                }
            )
        } catch (e: Exception) {
            ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Error calculating trend")
        }
    }
    
    private suspend fun getNextSessionWithNavigation(activeCycle: ActiveProgramCycle): Triple<SessionInfo, String, String>? {
        return try {
            val completedSessionIds = activeCycle.completedSessions.keys.toSet()
            
            // Find the next incomplete session
            for (week in activeCycle.cycleProgram.weeks.sortedBy { it.order }) {
                for (session in week.sessions.sortedBy { it.order }) {
                    val sessionKey = "${week.id}_${session.id}"
                    if (sessionKey !in completedSessionIds) {
                        // Found the next session - get its details
                        val sessionInfo = createSessionInfo(session, week, activeCycle)
                        return sessionInfo?.let { Triple(it, week.id, session.id) }
                    }
                }
            }
            null // All sessions completed
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getNextSessionInfo(activeCycle: ActiveProgramCycle): SessionInfo? {
        return try {
            val completedSessionIds = activeCycle.completedSessions.keys.toSet()
            
            // Find the next incomplete session
            for (week in activeCycle.cycleProgram.weeks.sortedBy { it.order }) {
                for (session in week.sessions.sortedBy { it.order }) {
                    val sessionKey = "${week.id}_${session.id}"
                    if (sessionKey !in completedSessionIds) {
                        // Found the next session - get its details
                        return createSessionInfo(session, week, activeCycle)
                    }
                }
            }
            null // All sessions completed
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createSessionInfo(
        session: ProgramSessionDefinition,
        week: ProgramWeekDefinition,
        activeCycle: ActiveProgramCycle
    ): SessionInfo? {
        return try {
            // Get workout template to extract exercise details
            val workoutTemplate = workoutTemplateDao.getTemplateById(session.workoutTemplateId).first()
                ?: return null
            
            // Create exercise previews
            val exercisePreviews = workoutTemplate.templateExercises.take(3).map { templateExercise ->
                val sets = templateExercise.sets.size
                val repsRange = templateExercise.sets.firstOrNull()?.targetReps ?: "?"
                val primaryMuscleGroup = templateExercise.targetMuscleGroups.firstOrNull() ?: MuscleGroup.OTHER
                
                ExercisePreview(
                    name = templateExercise.exerciseName,
                    sets = sets,
                    reps = repsRange,
                    weight = null, // Will be determined during workout
                    muscleGroup = primaryMuscleGroup
                )
            }
            
            // Estimate duration based on exercises and sets
            val totalSets = workoutTemplate.templateExercises.sumOf { it.sets.size }
            val estimatedDuration = (totalSets * 3) + 10 // 3 min per set + 10 min warmup
            
            // Calculate difficulty based on volume and exercise count
            val exerciseCount = workoutTemplate.templateExercises.size
            val difficulty = when {
                exerciseCount > 8 && totalSets > 25 -> SessionDifficulty.VERY_HARD
                exerciseCount > 6 && totalSets > 20 -> SessionDifficulty.HARD
                exerciseCount > 4 && totalSets > 15 -> SessionDifficulty.MODERATE
                else -> SessionDifficulty.LIGHT
            }
            
            SessionInfo(
                id = session.id,
                name = session.sessionName,
                weekLabel = week.weekLabel,
                exercises = exercisePreviews,
                estimatedDuration = estimatedDuration,
                difficulty = difficulty,
                targetVolume = null, // Could be calculated if needed
                templateId = session.workoutTemplateId
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getRecentAchievements(): List<Achievement> {
        val achievements = mutableListOf<Achievement>()
        
        try {
            // Get recent personal records (last 30 days)
            val recentPRs = personalRecordDao.getAllPRs().first()
                .filter { pr ->
                    try {
                        val prDate = java.time.LocalDate.parse(pr.date)
                        val thirtyDaysAgo = java.time.LocalDate.now().minusDays(30)
                        prDate.isAfter(thirtyDaysAgo)
                    } catch (e: Exception) {
                        false
                    }
                }
                .take(3) // Most recent 3 PRs
            
            recentPRs.forEach { pr ->
                achievements.add(Achievement(
                    id = "pr_${pr.id}",
                    title = "Personal Record!",
                    description = "New ${pr.type.name.replace("_", " ").lowercase()} PR in ${pr.exerciseName}",
                    icon = "🏆",
                    unlockedDate = try { java.time.LocalDate.parse(pr.date) } catch (e: Exception) { java.time.LocalDate.now() },
                    category = AchievementCategory.STRENGTH
                ))
            }
            
            // Check for recent workout milestones
            val totalWorkouts = analyticsRepository.getTotalWorkoutCount()
            val milestoneTargets = listOf(10, 25, 50, 100, 200, 300, 500, 1000)
            val recentMilestone = milestoneTargets.firstOrNull { it == totalWorkouts }
            
            recentMilestone?.let { milestone ->
                achievements.add(Achievement(
                    id = "milestone_$milestone",
                    title = "Workout Milestone!",
                    description = "Completed $milestone workouts! Amazing dedication!",
                    icon = "🎯",
                    unlockedDate = java.time.LocalDate.now(),
                    category = AchievementCategory.MILESTONE
                ))
            }
            
            // Check for volume achievements (high volume week)
            val thisWeekVolume = getWeeklyVolumeData().lastOrNull()?.totalVolume ?: 0.0
            if (thisWeekVolume > 50000) { // 50k+ kg volume in a week
                achievements.add(Achievement(
                    id = "volume_high_week",
                    title = "Volume Beast!",
                    description = "Crushed ${(thisWeekVolume / 1000).toInt()}k kg this week!",
                    icon = "💪",
                    unlockedDate = java.time.LocalDate.now(),
                    category = AchievementCategory.VOLUME
                ))
            }
            
            // Check for consistency achievements
            val streak = calculateBasicStreak()
            if (streak >= 7) {
                achievements.add(Achievement(
                    id = "consistency_week",
                    title = "Consistency Champion!",
                    description = "Maintained workout streak for $streak days!",
                    icon = "🔥",
                    unlockedDate = java.time.LocalDate.now(),
                    category = AchievementCategory.CONSISTENCY
                ))
            }
            
        } catch (e: Exception) {
            // Handle errors gracefully
        }
        
        return achievements.take(5) // Max 5 recent achievements
    }
    
    private suspend fun getNextMilestone(): Milestone? {
        return try {
            val totalWorkouts = analyticsRepository.getTotalWorkoutCount()
            val milestoneTargets = listOf(10, 25, 50, 100, 200, 300, 500, 1000)
            
            val nextTarget = milestoneTargets.firstOrNull { it > totalWorkouts }
            
            nextTarget?.let { target ->
                val progress = totalWorkouts.toFloat() / target.toFloat()
                val remaining = target - totalWorkouts
                
                Milestone(
                    title = "Next Workout Milestone",
                    description = "Just $remaining workouts away from your $target workout milestone!",
                    progress = progress,
                    target = "$target workouts"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getWeeklyVolumeData(): List<VolumeDataPoint> {
        return try {
            // Use Analytics repository for consistent volume calculation
            val endDate = LocalDate.now()
            val startDate = endDate.minusWeeks(8) // Last 8 weeks
            analyticsRepository.getVolumeData(startDate, endDate)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun calculateVolumeProgressTrend(volumeData: List<VolumeDataPoint>): ProgressTrend {
        if (volumeData.size < 2) {
            return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Need more data")
        }
        
        // Compare recent weeks to previous weeks
        val recentWeeks = volumeData.takeLast(3) // Last 3 weeks
        val previousWeeks = volumeData.dropLast(3).takeLast(3) // Previous 3 weeks
        
        if (recentWeeks.isEmpty() || previousWeeks.isEmpty()) {
            return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Need more data")
        }
        
        val recentAverage = recentWeeks.map { it.totalVolume }.average()
        val previousAverage = previousWeeks.map { it.totalVolume }.average()
        
        if (previousAverage == 0.0) {
            return ProgressTrend(TrendDirection.INSUFFICIENT_DATA, 0f, "Need more data")
        }
        
        val changePercentage = ((recentAverage - previousAverage) / previousAverage * 100).toFloat()
        
        val direction = when {
            changePercentage > 15f -> TrendDirection.STRONGLY_IMPROVING
            changePercentage > 5f -> TrendDirection.SLIGHTLY_IMPROVING
            changePercentage < -15f -> TrendDirection.STRONGLY_DECLINING
            changePercentage < -5f -> TrendDirection.SLIGHTLY_DECLINING
            else -> TrendDirection.STABLE
        }
        
        return ProgressTrend(
            direction = direction,
            percentage = kotlin.math.abs(changePercentage),
            description = when (direction) {
                TrendDirection.STRONGLY_IMPROVING -> "Volume increasing significantly"
                TrendDirection.SLIGHTLY_IMPROVING -> "Volume trending upward"
                TrendDirection.STRONGLY_DECLINING -> "Volume declining significantly"
                TrendDirection.SLIGHTLY_DECLINING -> "Volume trending downward"
                TrendDirection.STABLE -> "Volume remaining stable"
                else -> "Insufficient data for trend"
            }
        )
    }
    
    // Activity Heatmap Data Generation
    private suspend fun getActivityHeatmapData(): Map<LocalDate, WorkoutIntensity> {
        return try {
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            val heatmapData = mutableMapOf<LocalDate, WorkoutIntensity>()
            
            // Get workout data for the last 365 days (one year)
            val oneYearAgo = LocalDate.now().minusDays(365)
            
            workouts.forEach { workout ->
                try {
                    val workoutDate = LocalDate.parse(workout.date)
                    if (workoutDate.isAfter(oneYearAgo)) {
                        // Calculate workout intensity
                        val volume = workout.loggedExercises.sumOf { exercise ->
                            exercise.sets.sumOf { set ->
                                (set.weight ?: 0.0) * (set.reps ?: 0)
                            }
                        }.toFloat()
                        
                        val exerciseCount = workout.loggedExercises.size
                        val setCount = workout.loggedExercises.sumOf { it.sets.size }
                        
                        // Calculate intensity level (0.0 to 1.0)
                        val baseIntensity = when {
                            exerciseCount >= 8 && setCount >= 25 -> 1.0f
                            exerciseCount >= 6 && setCount >= 20 -> 0.8f
                            exerciseCount >= 4 && setCount >= 15 -> 0.6f
                            exerciseCount >= 2 && setCount >= 10 -> 0.4f
                            else -> 0.2f
                        }
                        
                        // Adjust based on volume
                        val volumeMultiplier = when {
                            volume > 15000 -> 1.0f
                            volume > 10000 -> 0.9f
                            volume > 5000 -> 0.8f
                            volume > 2000 -> 0.7f
                            else -> 0.6f
                        }
                        
                        val finalIntensity = (baseIntensity * volumeMultiplier).coerceIn(0.1f, 1.0f)
                        
                        heatmapData[workoutDate] = WorkoutIntensity(
                            date = workoutDate,
                            intensity = finalIntensity,
                            volume = volume,
                            duration = 60 // Default duration, could be calculated if tracked
                        )
                    }
                } catch (e: Exception) {
                    // Skip invalid dates
                }
            }
            
            heatmapData
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // Streak Information Calculation
    private suspend fun getStreakInfo(): StreakInfo {
        return try {
            val workouts = loggedWorkoutDao.getAllLoggedWorkouts().first()
            val workoutDates = workouts.mapNotNull { workout ->
                try {
                    LocalDate.parse(workout.date)
                } catch (e: Exception) {
                    null
                }
            }.sorted()
            
            // Calculate current streak
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0
            var lastDate: LocalDate? = null
            
            val today = LocalDate.now()
            val reversedDates = workoutDates.reversed()
            
            // Check if we worked out yesterday or today for current streak
            val mostRecentDate = reversedDates.firstOrNull()
            if (mostRecentDate != null) {
                val daysSinceLastWorkout = java.time.temporal.ChronoUnit.DAYS.between(mostRecentDate, today)
                if (daysSinceLastWorkout <= 1) {
                    // Start counting current streak
                    currentStreak = 1
                    lastDate = mostRecentDate
                    
                    // Count consecutive days backwards
                    for (i in 1 until reversedDates.size) {
                        val currentDate = reversedDates[i]
                        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(currentDate, lastDate!!)
                        if (daysDiff == 1L) {
                            currentStreak++
                            lastDate = currentDate
                        } else {
                            break
                        }
                    }
                }
            }
            
            // Calculate longest streak
            tempStreak = 1
            lastDate = null
            
            for (date in workoutDates) {
                if (lastDate == null) {
                    lastDate = date
                } else {
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastDate, date)
                    if (daysDiff == 1L) {
                        tempStreak++
                    } else {
                        longestStreak = maxOf(longestStreak, tempStreak)
                        tempStreak = 1
                    }
                    lastDate = date
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak)
            
            // Calculate this week's workout count
            val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
            val thisWeekCount = workoutDates.count { date ->
                !date.isBefore(startOfWeek) && !date.isAfter(today)
            }
            
            StreakInfo(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                weeklyTarget = 4, // Default target, could be user-configurable
                thisWeekCount = thisWeekCount
            )
        } catch (e: Exception) {
            StreakInfo(
                currentStreak = 0,
                longestStreak = 0,
                weeklyTarget = 4,
                thisWeekCount = 0
            )
        }
    }
    
    // Smart Insights Generation
    private suspend fun generateBasicInsights(activeCycle: ActiveProgramCycle?): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()
        
        try {
            // Get basic analytics
            val streak = calculateBasicStreak()
            val totalWorkouts = analyticsRepository.getTotalWorkoutCount()
            val thisWeekWorkouts = analyticsRepository.getThisWeekWorkoutCount()
            
            // Welcome back insight for returning users
            if (totalWorkouts > 0 && thisWeekWorkouts == 0) {
                insights.add(SmartInsight(
                    id = "welcome_back",
                    title = "Welcome Back!",
                    message = "You haven't worked out this week yet. Ready to get back into it?",
                    type = InsightType.MOTIVATION,
                    priority = InsightPriority.MEDIUM,
                    actionable = true,
                    actionText = "Start Workout"
                ))
            }
            
            // Streak celebration
            if (streak >= 7) {
                insights.add(SmartInsight(
                    id = "streak_celebration",
                    title = "🔥 Amazing Streak!",
                    message = "You're on a ${streak}-day workout streak! Keep up the fantastic consistency.",
                    type = InsightType.CELEBRATION,
                    priority = InsightPriority.HIGH,
                    actionable = false
                ))
            } else if (streak >= 3) {
                insights.add(SmartInsight(
                    id = "streak_building",
                    title = "Building Momentum",
                    message = "${streak} days in a row! You're building great habits.",
                    type = InsightType.MOTIVATION,
                    priority = InsightPriority.MEDIUM,
                    actionable = false
                ))
            }
            
            // Weekly progress insight
            if (thisWeekWorkouts >= 4) {
                insights.add(SmartInsight(
                    id = "weekly_goal_met",
                    title = "✅ Weekly Goal Achieved",
                    message = "You've completed ${thisWeekWorkouts} workouts this week. Excellent consistency!",
                    type = InsightType.CELEBRATION,
                    priority = InsightPriority.MEDIUM,
                    actionable = false
                ))
            } else if (thisWeekWorkouts >= 1) {
                val remaining = 4 - thisWeekWorkouts
                insights.add(SmartInsight(
                    id = "weekly_progress",
                    title = "Weekly Progress",
                    message = "${thisWeekWorkouts} workouts completed this week. ${remaining} more to reach your goal!",
                    type = InsightType.PERFORMANCE,
                    priority = InsightPriority.LOW,
                    actionable = true,
                    actionText = "View Schedule"
                ))
            }
            
            // No active cycle recommendation
            if (activeCycle == null && totalWorkouts > 0) {
                insights.add(SmartInsight(
                    id = "start_program",
                    title = "Ready for Structure?",
                    message = "Consider starting a structured program to maximize your progress and stay motivated.",
                    type = InsightType.RECOMMENDATION,
                    priority = InsightPriority.MEDIUM,
                    actionable = true,
                    actionText = "Browse Programs"
                ))
            }
            
            // First workout encouragement
            if (totalWorkouts == 0) {
                insights.add(SmartInsight(
                    id = "first_workout",
                    title = "🚀 Ready to Start?",
                    message = "Welcome to MyWorkoutLog! Let's begin your fitness journey with your first workout.",
                    type = InsightType.MOTIVATION,
                    priority = InsightPriority.HIGH,
                    actionable = true,
                    actionText = "Start First Workout"
                ))
            }
            
            // Random motivational insights
            if (insights.size < 2) {
                val motivationalInsights = listOf(
                    SmartInsight(
                        id = "consistency_tip",
                        title = "💡 Pro Tip",
                        message = "Consistency beats perfection. Even a 15-minute workout is better than none!",
                        type = InsightType.MOTIVATION,
                        priority = InsightPriority.LOW,
                        actionable = false
                    ),
                    SmartInsight(
                        id = "progress_tracking",
                        title = "📊 Track Your Progress",
                        message = "Check your analytics to see how far you've come and plan your next goals.",
                        type = InsightType.RECOMMENDATION,
                        priority = InsightPriority.LOW,
                        actionable = true,
                        actionText = "View Analytics"
                    )
                )
                insights.add(motivationalInsights.random())
            }
            
        } catch (e: Exception) {
            // Fallback insight if analytics fail
            insights.add(SmartInsight(
                id = "system_ready",
                title = "System Ready",
                message = "Your workout tracker is ready to help you achieve your fitness goals!",
                type = InsightType.MOTIVATION,
                priority = InsightPriority.LOW,
                actionable = false
            ))
        }
        
        return insights.take(3) // Limit to 3 insights max
    }
}

data class BodyweightInfo(
    val weight: Double,
    val date: String,
    val unit: String
)