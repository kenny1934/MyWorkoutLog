package com.example.myworkoutlog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.LocalDateTime

// Dashboard mode definitions
sealed class DashboardMode {
    object NoActiveCycle : DashboardMode()
    data class ActiveCycle(
        val cycle: ActiveProgramCycle,
        val progress: CycleProgress
    ) : DashboardMode()
    object RestDay : DashboardMode()
    object CycleComplete : DashboardMode()
}

// Main dashboard state
@Stable
data class DashboardState(
    val mode: DashboardMode,
    val widgets: List<DashboardWidget>,
    val quickActions: List<QuickAction>,
    val insights: List<SmartInsight>,
    val isLoading: Boolean = false,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

// Widget system
sealed class DashboardWidget(
    val id: String,
    val title: String,
    val priority: Int,
    val isVisible: Boolean = true,
    val isExpandable: Boolean = false
) {
    data class WelcomeWidget(
        val greeting: String,
        val userName: String?,
        val currentStreak: Int,
        val motivationalMessage: String
    ) : DashboardWidget("welcome", "Welcome", 1)
    
    data class QuickStatsWidget(
        val totalWorkouts: Int,
        val currentStreak: Int,
        val bestWeek: String,
        val recentPRs: List<PersonalRecord>
    ) : DashboardWidget("quick_stats", "Quick Stats", 2)
    
    data class BodyweightWidget(
        val currentWeight: Double?,
        val lastRecordedDate: String?,
        val unit: String = "kg",
        val trend: String? = null
    ) : DashboardWidget("bodyweight", "Current Weight", 2)
    
    data class CycleProgressWidget(
        val cycle: ActiveProgramCycle,
        val completionPercentage: Float,
        val weekProgress: String,
        val sessionProgress: String,
        val nextSession: SessionInfo?,
        val daysUntilNext: Int?
    ) : DashboardWidget("cycle_progress", "Cycle Progress", 1)
    
    data class PerformanceTrendWidget(
        val strengthGains: List<ExerciseProgress>,
        val volumeTrend: ProgressTrend,
        val timeframe: String = "This Cycle"
    ) : DashboardWidget("performance_trend", "Performance Trend", 3, isExpandable = true)
    
    data class ActivityHeatmapWidget(
        val workoutDays: Map<LocalDate, WorkoutIntensity>,
        val streakInfo: StreakInfo
    ) : DashboardWidget("activity_heatmap", "Activity", 4, isExpandable = true)
    
    data class NextSessionWidget(
        val session: SessionInfo,
        val estimatedDuration: Int,
        val exercises: List<ExercisePreview>,
        val difficulty: SessionDifficulty,
        val cycleId: String? = null,
        val weekId: String? = null,
        val sessionId: String? = null,
        val templateId: String? = null
    ) : DashboardWidget("next_session", "Next Session", 2, isExpandable = true)
    
    data class AchievementWidget(
        val recentAchievements: List<Achievement>,
        val nextMilestone: Milestone?
    ) : DashboardWidget("achievements", "Achievements", 5, isExpandable = true)
    
    data class VolumeProgressWidget(
        val weeklyVolume: List<com.example.myworkoutlog.VolumeDataPoint>, // Use existing VolumeDataPoint
        val trend: ProgressTrend,
        val targetVolume: Float?
    ) : DashboardWidget("volume_progress", "Volume Progress", 4, isExpandable = true)
    
    data class BodyweightTrendWidget(
        val bodyweightData: List<BodyweightPoint>,
        val trend: ProgressTrend
    ) : DashboardWidget("bodyweight_trend", "Bodyweight Trend", 6)
}

// Supporting data models
data class CycleProgress(
    val completionPercentage: Float,
    val weekProgress: String, // "Week 2 of 4"
    val sessionProgress: String, // "Session 2 of 3 this week"
    val strengthGains: List<ExerciseProgress>,
    val volumeTrend: ProgressTrend,
    val consistency: Float, // 0-1 representing adherence
    val estimatedCompletion: LocalDate?
)

data class SessionInfo(
    val id: String,
    val name: String,
    val weekLabel: String,
    val exercises: List<ExercisePreview>,
    val estimatedDuration: Int, // minutes
    val difficulty: SessionDifficulty,
    val targetVolume: Float?,
    val templateId: String? = null
)

data class ExercisePreview(
    val name: String,
    val sets: Int,
    val reps: String, // Can be range like "8-12"
    val weight: Float?,
    val muscleGroup: MuscleGroup
)

data class ExerciseProgress(
    val exerciseId: String,
    val exerciseName: String,
    val currentMax: Float,
    val previousMax: Float,
    val improvementPercentage: Float,
    val trend: ProgressTrend,
    // Bodyweight breakdown for clearer display
    val usesBodyweight: Boolean = false,
    val currentBodyweight: Float? = null,
    val currentExternalWeight: Float? = null,
    val previousBodyweight: Float? = null,
    val previousExternalWeight: Float? = null
)

data class ProgressTrend(
    val direction: TrendDirection,
    val percentage: Float,
    val description: String
)

// Utility extension for formatting weight display with bodyweight breakdown
fun ExerciseProgress.formatCurrentWeight(): String {
    return if (usesBodyweight && currentBodyweight != null && currentExternalWeight != null) {
        "${formatDashboardWeight(currentMax)}kg (${formatDashboardWeight(currentBodyweight)} + ${formatDashboardWeight(currentExternalWeight)})"
    } else {
        "${formatDashboardWeight(currentMax)}kg"
    }
}

fun ExerciseProgress.formatPreviousWeight(): String {
    return if (usesBodyweight && previousBodyweight != null && previousExternalWeight != null) {
        "${formatDashboardWeight(previousMax)}kg (${formatDashboardWeight(previousBodyweight)} + ${formatDashboardWeight(previousExternalWeight)})"
    } else {
        "${formatDashboardWeight(previousMax)}kg"
    }
}

// Helper function to format weight with appropriate decimal precision for dashboard
private fun formatDashboardWeight(weight: Float): String {
    return when {
        weight % 1.0f == 0.0f -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

fun ExerciseProgress.formatWeightProgression(): String {
    return "${formatPreviousWeight()} → ${formatCurrentWeight()}"
}

// Using TrendDirection from DataModels.kt - removed duplicate

enum class SessionDifficulty {
    LIGHT, MODERATE, HARD, VERY_HARD
}

data class WorkoutIntensity(
    val date: LocalDate,
    val intensity: Float, // 0-1
    val volume: Float,
    val duration: Int
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val weeklyTarget: Int,
    val thisWeekCount: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedDate: LocalDate,
    val category: AchievementCategory
)

enum class AchievementCategory {
    STRENGTH, VOLUME, CONSISTENCY, MILESTONE, SPECIAL
}

data class Milestone(
    val title: String,
    val description: String,
    val progress: Float, // 0-1
    val target: String
)

data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: QuickActionType
)

enum class QuickActionType {
    START_NEXT_SESSION,
    LOG_QUICK_WORKOUT,
    VIEW_ANALYTICS,
    ADD_BODYWEIGHT,
    START_NEW_CYCLE,
    VIEW_HISTORY,
    COMPLETE_CYCLE,
    VIEW_CYCLE_ANALYTICS
}

data class SmartInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: InsightType,
    val priority: InsightPriority,
    val actionable: Boolean = false,
    val actionText: String? = null
)

enum class InsightType {
    PERFORMANCE, RECOVERY, MOTIVATION, WARNING, CELEBRATION, RECOMMENDATION
}

enum class InsightPriority {
    LOW, MEDIUM, HIGH, URGENT
}

// Using VolumeDataPoint from DataModels.kt - removed duplicate

data class BodyweightPoint(
    val date: LocalDate,
    val weight: Float
)

// Widget configuration
data class WidgetConfig(
    val widgetType: String,
    val isEnabled: Boolean,
    val position: Int,
    val customSettings: Map<String, Any> = emptyMap()
)

// Dashboard preferences
data class DashboardPreferences(
    val widgetConfigs: List<WidgetConfig>,
    val showMotivationalMessages: Boolean = true,
    val showAchievements: Boolean = true,
    val showInsights: Boolean = true,
    val autoRefresh: Boolean = true,
    val defaultTimeframe: String = "30days",
    val dismissedInsights: Set<String> = emptySet()
)