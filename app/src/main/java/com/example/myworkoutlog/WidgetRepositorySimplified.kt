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
    private val programTemplateDao: ProgramTemplateDao
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
        
        // Activity heatmap (simplified)
        widgets.add(DashboardWidget.ActivityHeatmapWidget(
            workoutDays = emptyMap(),
            streakInfo = StreakInfo(
                currentStreak = streak,
                longestStreak = streak,
                weeklyTarget = 4,
                thisWeekCount = 2
            )
        ))
        
        emit(DashboardState(
            mode = DashboardMode.NoActiveCycle,
            widgets = widgets,
            quickActions = getBasicQuickActions(null),
            insights = emptyList()
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
            insights = emptyList()
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
        val totalSessions = activeCycle.cycleProgram.weeks.sumOf { it.sessions.size }
        val completedSessions = activeCycle.completedSessions.size
        
        return if (totalSessions > 0) {
            (completedSessions.toFloat() / totalSessions.toFloat()) * 100f
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
}

data class BodyweightInfo(
    val weight: Double,
    val date: String,
    val unit: String
)