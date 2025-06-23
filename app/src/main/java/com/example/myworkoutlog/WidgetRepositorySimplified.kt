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
        widgets.add(DashboardWidget.CycleProgressWidget(
            cycle = activeCycle,
            completionPercentage = progress,
            weekProgress = "Week 1 of 4", // Simplified
            sessionProgress = "Session 1 of 3", // Simplified
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
        
        emit(DashboardState(
            mode = DashboardMode.ActiveCycle(activeCycle, CycleProgress(
                completionPercentage = progress,
                weekProgress = "Week 1 of 4",
                sessionProgress = "Session 1 of 3",
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
        // Simplified progress calculation
        return if (activeCycle.completedSessions.isNotEmpty()) {
            50f // Placeholder
        } else {
            0f
        }
    }
    
    private fun getBasicQuickActions(activeCycle: ActiveProgramCycle?): List<QuickAction> {
        val actions = mutableListOf<QuickAction>()
        
        if (activeCycle != null) {
            actions.add(QuickAction(
                id = "start_next",
                title = "Start Next Session",
                description = "Continue your active cycle",
                icon = Icons.Default.Refresh,
                action = QuickActionType.START_NEXT_SESSION
            ))
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
}

data class BodyweightInfo(
    val weight: Double,
    val date: String,
    val unit: String
)