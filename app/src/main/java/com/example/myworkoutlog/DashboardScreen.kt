package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.time.LocalDate

// Widget component definitions - must be defined before they're used

@Composable
fun SimpleWelcomeWidgetCard(widget: DashboardWidget.WelcomeWidget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = widget.greeting,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = widget.motivationalMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (widget.currentStreak > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${widget.currentStreak} day streak 🔥",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SimpleQuickStatsWidgetCard(widget: DashboardWidget.QuickStatsWidget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = widget.totalWorkouts.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Workouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${widget.currentStreak}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = widget.recentPRs.size.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Recent PRs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBodyweightWidgetCard(
    currentWeight: Double?,
    lastRecordedDate: String?,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Weight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (currentWeight != null) {
                Text(
                    text = "${currentWeight.toInt()} $unit",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                lastRecordedDate?.let { date ->
                    Text(
                        text = "Last recorded: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No weight recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable  
fun SimpleCycleProgressWidgetCard(widget: DashboardWidget.CycleProgressWidget, navController: NavHostController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cycle Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Indicator
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = widget.completionPercentage,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${(widget.completionPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Progress Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = widget.weekProgress,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = widget.sessionProgress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick action button
                    widget.nextSession?.let {
                        ElevatedButton(
                            onClick = { 
                                navController.navigate("workoutLogger/${widget.cycle.cycleUuid}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Next Session")
                        }
                    } ?: run {
                        OutlinedButton(
                            onClick = { 
                                navController.navigate("analytics")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Analytics")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleActivityHeatmapWidgetCard(widget: DashboardWidget.ActivityHeatmapWidget) {
    SimpleExpandableWidgetCard(
        title = "Activity Heatmap",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            // Collapsed: Show streak info summary
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${widget.streakInfo.currentStreak}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${widget.streakInfo.thisWeekCount}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This Week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${widget.streakInfo.longestStreak}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Best Streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Mini heatmap for collapsed state
                ActivityHeatmapGrid(
                    data = widget.workoutDays,
                    modifier = Modifier.fillMaxWidth(),
                    cellSize = 10.dp,
                    spacing = 1.dp,
                    weeksToShow = 12
                )
                
                if (widget.isExpandable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to see detailed activity pattern",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        expandedContent = {
            // Expanded: Show enhanced interactive heatmap
            Column {
                // Enhanced activity heatmap with yearly view option
                EnhancedActivityHeatmap(
                    workoutData = widget.workoutDays,
                    showYearlyView = false,
                    onDayClick = { date, intensity ->
                        // Could navigate to day details or show more info
                        // For now, just showing day details in the heatmap itself
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun SimpleBodyweightTrendWidgetCard(widget: DashboardWidget.BodyweightTrendWidget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bodyweight Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                TrendIndicator(
                    trend = widget.trend,
                    showPercentage = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (widget.bodyweightData.isNotEmpty()) {
                // Show trend summary
                val currentWeight = widget.bodyweightData.lastOrNull()?.weight
                val previousWeight = widget.bodyweightData.firstOrNull()?.weight
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentWeight?.toInt() ?: 0} kg",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (currentWeight != null && previousWeight != null) {
                        val change = currentWeight - previousWeight
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${if (change >= 0) "+" else ""}${String.format("%.1f", change)} kg",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    change > 0 -> Color(0xFF4CAF50)
                                    change < 0 -> Color(0xFFF44336)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${widget.bodyweightData.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Simple trend visualization
                BodyweightMiniChart(
                    data = widget.bodyweightData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
            } else {
                Text(
                    text = "No bodyweight data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SimplePerformanceTrendWidgetCard(
    widget: DashboardWidget.PerformanceTrendWidget,
    navController: NavHostController
) {
    SimpleExpandableWidgetCard(
        title = "Performance Trends",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            // Collapsed: Show overview with top exercise
            Column {
                if (widget.strengthGains.isNotEmpty()) {
                    val topExercise = widget.strengthGains.first()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topExercise.exerciseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${topExercise.currentMax.toInt()}kg (${if (topExercise.improvementPercentage >= 0) "+" else ""}${topExercise.improvementPercentage.toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (topExercise.improvementPercentage >= 0) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.error
                            )
                        }
                        // Mini trend indicator
                        Icon(
                            imageVector = when {
                                topExercise.improvementPercentage > 5 -> Icons.Default.TrendingUp
                                topExercise.improvementPercentage < -5 -> Icons.Default.TrendingDown
                                else -> Icons.Default.TrendingFlat
                            },
                            contentDescription = "Trend",
                            tint = when {
                                topExercise.improvementPercentage > 5 -> MaterialTheme.colorScheme.primary
                                topExercise.improvementPercentage < -5 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (widget.strengthGains.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show trend for the top exercise using previous->current progression
                        val trendData = listOf(topExercise.previousMax, topExercise.currentMax)
                        SparklineChart(
                            data = trendData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            color = if (topExercise.improvementPercentage >= 0) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error,
                            fillArea = true
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to see all ${widget.strengthGains.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "No performance data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        expandedContent = {
            // Expanded: Show enhanced interactive chart only (no duplicate cards)
            Column {
                Text(
                    text = "Performance Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Enhanced interactive performance chart with drill-down capability
                EnhancedInteractivePerformanceChart(
                    strengthGains = widget.strengthGains,
                    navController = navController,
                    modifier = Modifier.fillMaxWidth(),
                    showTooltips = true,
                    enableDrillDown = true
                )
            }
        }
    )
}

@Composable
fun SimpleNextSessionWidgetCard(widget: DashboardWidget.NextSessionWidget, navController: NavHostController) {
    SimpleExpandableWidgetCard(
        title = "Next Session",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            // Collapsed: Show session overview
            Column {
                Text(
                    text = widget.session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = widget.session.weekLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${widget.estimatedDuration} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DifficultyBadge(difficulty = widget.difficulty)
                }
            }
        },
        expandedContent = {
            // Expanded: Show detailed session preview
            Column {
                // Session header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = widget.session.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = widget.session.weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DifficultyBadge(difficulty = widget.difficulty)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Exercise previews
                Text(
                    text = "Exercises (${widget.exercises.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                widget.exercises.take(5).forEach { exercise ->
                    ExercisePreviewCard(exercise = exercise)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (widget.exercises.size > 5) {
                    Text(
                        text = "... and ${widget.exercises.size - 5} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Start session button
                Button(
                    onClick = { 
                        // Navigate to workout logger with proper session details
                        if (widget.cycleId != null && widget.weekId != null && 
                            widget.sessionId != null && widget.templateId != null) {
                            val route = Screen.WorkoutLogger.createRoute(
                                templateId = widget.templateId,
                                cycleId = widget.cycleId,
                                weekId = widget.weekId,
                                sessionId = widget.sessionId
                            )
                            navController.navigate(route)
                        } else {
                            // Fallback to library if navigation data is missing
                            navController.navigate("library")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Session")
                }
            }
        }
    )
}

@Composable
fun SimpleVolumeProgressWidgetCard(
    widget: DashboardWidget.VolumeProgressWidget,
    navController: NavHostController
) {
    SimpleExpandableWidgetCard(
        title = "Volume Progress",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            // Collapsed: Show volume summary
            Column {
                if (widget.weeklyVolume.isNotEmpty()) {
                    val latestVolume = widget.weeklyVolume.last()
                    val previousVolume = widget.weeklyVolume.getOrNull(widget.weeklyVolume.size - 2)
                    val volumeChange = if (previousVolume != null) {
                        ((latestVolume.totalVolume - previousVolume.totalVolume) / previousVolume.totalVolume * 100).toFloat()
                    } else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Latest Volume",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${latestVolume.totalVolume.toInt()}kg",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (previousVolume != null) {
                                Text(
                                    text = "${if (volumeChange >= 0) "+" else ""}${volumeChange.toInt()}% from last week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (volumeChange >= 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        // Mini volume trend indicator
                        Icon(
                            imageVector = when {
                                volumeChange > 5f -> Icons.Default.TrendingUp
                                volumeChange < -5f -> Icons.Default.TrendingDown
                                else -> Icons.Default.TrendingFlat
                            },
                            contentDescription = "Volume Trend",
                            tint = when {
                                volumeChange > 5f -> MaterialTheme.colorScheme.primary
                                volumeChange < -5f -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to see ${widget.weeklyVolume.size} data points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "No volume data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        expandedContent = {
            // Expanded: Show enhanced interactive volume chart and breakdown
            Column {
                Text(
                    text = "Volume Progression",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Enhanced interactive volume chart with drill-down and comparison
                EnhancedInteractiveVolumeChart(
                    volumeData = widget.weeklyVolume,
                    navController = navController,
                    modifier = Modifier.fillMaxWidth(),
                    showTooltips = true,
                    enableDrillDown = true,
                    showComparison = true
                )
            }
        }
    )
}

@Composable
fun SimpleAchievementWidgetCard(widget: DashboardWidget.AchievementWidget) {
    SimpleExpandableWidgetCard(
        title = "Achievements",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            if (widget.recentAchievements.isNotEmpty()) {
                // Show the most recent achievement
                val recentAchievement = widget.recentAchievements.first()
                AchievementCard(
                    achievement = recentAchievement,
                    isCompact = true
                )
            } else {
                Text(
                    text = "Keep training to unlock achievements!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        expandedContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recent achievements
                if (widget.recentAchievements.isNotEmpty()) {
                    Text(
                        text = "Recent Achievements",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    widget.recentAchievements.forEach { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            isCompact = false
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Next milestone
                widget.nextMilestone?.let { milestone ->
                    Text(
                        text = "Next Milestone",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    MilestoneCard(milestone = milestone)
                }
            }
        }
    )
}

// Interactive Chart Components - must be defined before they're used

@Composable
fun InteractivePerformanceChart(
    strengthGains: List<ExerciseProgress>,
    modifier: Modifier = Modifier
) {
    if (strengthGains.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No performance data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    var selectedExercise by remember { mutableStateOf<ExerciseProgress?>(null) }

    // Prepare chart data
    val chartEntries = strengthGains.mapIndexed { index, exercise ->
        entryOf(index.toFloat(), exercise.improvementPercentage)
    }

    LaunchedEffect(strengthGains) {
        chartEntryModelProducer.setEntries(chartEntries)
    }

    Column(modifier = modifier) {
        // Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(
                        valueFormatter = AxisValueFormatter { value, _ -> "${value.toInt()}%" }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter { value, _ ->
                            strengthGains.getOrNull(value.toInt())?.exerciseName?.take(8) ?: ""
                        }
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Chart interaction feedback
        selectedExercise?.let { exercise ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Improvement: ${if (exercise.improvementPercentage >= 0) "+" else ""}${exercise.improvementPercentage}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Current: ${exercise.currentMax}kg → Previous: ${exercise.previousMax}kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisePerformanceCard(
    exercise: ExerciseProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${exercise.previousMax.toInt()}kg → ${exercise.currentMax.toInt()}kg",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${if (exercise.improvementPercentage >= 0) "+" else ""}${exercise.improvementPercentage.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (exercise.improvementPercentage >= 0) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.error
                )
                
                Icon(
                    imageVector = when {
                        exercise.improvementPercentage > 5 -> Icons.Default.TrendingUp
                        exercise.improvementPercentage < -5 -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    },
                    contentDescription = "Trend",
                    tint = when {
                        exercise.improvementPercentage > 5 -> MaterialTheme.colorScheme.primary
                        exercise.improvementPercentage < -5 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun InteractiveVolumeChart(
    volumeData: List<VolumeDataPoint>,
    modifier: Modifier = Modifier
) {
    if (volumeData.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No volume data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    var selectedDataPoint by remember { mutableStateOf<VolumeDataPoint?>(null) }

    // Prepare chart data
    val chartEntries = volumeData.mapIndexed { index, dataPoint ->
        entryOf(index.toFloat(), dataPoint.totalVolume.toFloat())
    }

    LaunchedEffect(volumeData) {
        chartEntryModelProducer.setEntries(chartEntries)
    }

    Column(modifier = modifier) {
        // Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(
                        valueFormatter = AxisValueFormatter { value, _ -> "${(value / 1000).toInt()}k" }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter { value, _ ->
                            volumeData.getOrNull(value.toInt())?.date?.take(5) ?: ""
                        }
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Chart interaction feedback
        selectedDataPoint?.let { dataPoint ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = dataPoint.date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Volume: ${dataPoint.totalVolume.toInt()}kg",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VolumeStatisticsCard(
    volumeData: List<VolumeDataPoint>,
    targetVolume: Float?,
    modifier: Modifier = Modifier
) {
    if (volumeData.isEmpty()) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No volume data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val totalVolume = volumeData.sumOf { it.totalVolume }
    val averageVolume = totalVolume / volumeData.size
    val maxVolume = volumeData.maxOfOrNull { it.totalVolume } ?: 0.0
    val minVolume = volumeData.minOfOrNull { it.totalVolume } ?: 0.0

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Volume Statistics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VolumeStatItem(
                    label = "Average",
                    value = "${averageVolume.toInt()}kg",
                    modifier = Modifier.weight(1f)
                )
                VolumeStatItem(
                    label = "Peak",
                    value = "${maxVolume.toInt()}kg",
                    modifier = Modifier.weight(1f)
                )
                VolumeStatItem(
                    label = "Low",
                    value = "${minVolume.toInt()}kg",
                    modifier = Modifier.weight(1f)
                )
            }
            
            targetVolume?.let { target ->
                Spacer(modifier = Modifier.height(12.dp))
                val targetComparison = ((averageVolume / target.toDouble()) * 100).toInt()
                VolumeStatItem(
                    label = "vs Target",
                    value = "$targetComparison%",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VolumeStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Legacy dashboard components - must be defined before they're used

@Composable
fun NoActiveCycleDashboard(
    historyViewModel: HistoryViewModel,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Good day!", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ready for your next session?", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("Start a new workout:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onNavigate(Screen.Library.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose from Template")
            }
        }
    }
}

@Composable
fun ActiveCycleDashboard(
    activeCycle: ActiveProgramCycle,
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    navController: NavHostController
) {
    val program = activeCycle.cycleProgram
    var showEndCycleConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(activeCycle.userCycleName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(activeCycle.programTemplateName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showEndCycleConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End Current Cycle")
            }
        }

        program.weeks.sortedBy { it.order }.forEach { week ->
            item {
                Text(week.weekLabel, style = MaterialTheme.typography.titleLarge)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            items(week.sessions.sortedBy { it.order }) { session ->
                val sessionKey = "${week.id}_${session.id}"
                val isCompleted = activeCycle.completedSessions.containsKey(sessionKey)
                val workoutId = activeCycle.completedSessions[sessionKey]

                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(session.sessionName, modifier = Modifier.weight(1f))
                        if (isCompleted && workoutId != null) {
                            Button(
                                onClick = {
                                    navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) { Text("View") }
                        } else {
                            Button(onClick = {
                                val route = Screen.WorkoutLogger.createRoute(
                                    templateId = session.workoutTemplateId,
                                    cycleId = activeCycle.cycleUuid,
                                    weekId = week.id,
                                    sessionId = session.id
                                )
                                navController.navigate(route)
                            }) {
                                Text("Start")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showEndCycleConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndCycleConfirmation = false },
            title = { Text("End Cycle") },
            text = { Text("Are you sure you want to end the current cycle?") },
            confirmButton = {
                Button(
                    onClick = {
                        showEndCycleConfirmation = false
                        activeCycleViewModel.endCycle()
                    }
                ) {
                    Text("End Cycle")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndCycleConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LegacyDashboardScreen(
    historyViewModel: HistoryViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    programViewModel: ProgramViewModel,
    navController: NavHostController
) {
    val activeCycle by activeCycleViewModel.activeCycle.collectAsStateWithLifecycle()

    if (activeCycle == null) {
        NoActiveCycleDashboard(
            historyViewModel = historyViewModel,
            onNavigate = { route -> navController.navigate(route) }
        )
    } else {
        ActiveCycleDashboard(
            activeCycle = activeCycle!!,
            programViewModel = programViewModel,
            activeCycleViewModel = activeCycleViewModel,
            navController = navController
        )
    }
}

// Main dashboard components

@Composable
fun DashboardScreen(
    historyViewModel: HistoryViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    programViewModel: ProgramViewModel,
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel? = null
) {
    // Use enhanced dashboard if available, fallback to legacy
    if (dashboardViewModel != null) {
        EnhancedDashboardScreen(
            dashboardViewModel = dashboardViewModel,
            navController = navController
        )
    } else {
        // Legacy dashboard implementation
        LegacyDashboardScreen(
            historyViewModel = historyViewModel,
            activeCycleViewModel = activeCycleViewModel,
            programViewModel = programViewModel,
            navController = navController
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
    val isLoading by dashboardViewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by dashboardViewModel.error.collectAsStateWithLifecycle()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { dashboardViewModel.onPullToRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header
        item {
            Text(
                text = "Dashboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Error state
        error?.let { errorMessage ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { dashboardViewModel.refreshDashboard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
        
        // Loading state
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // High priority insights
            val urgentInsights = dashboardState.insights.filter { 
                it.priority == InsightPriority.URGENT || it.priority == InsightPriority.HIGH 
            }
            if (urgentInsights.isNotEmpty()) {
                items(urgentInsights) { insight ->
                    EnhancedInsightCard(
                        insight = insight,
                        onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                        onAction = { /* TODO: Handle insight actions */ }
                    )
                }
            }
            
            // Quick actions
            if (dashboardState.quickActions.isNotEmpty()) {
                item {
                    EnhancedDashboardWidgetCard(
                        title = "Quick Actions",
                        icon = Icons.Default.FlashOn
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dashboardState.quickActions) { action ->
                                EnhancedQuickActionButton(
                                    action = action,
                                    onClick = { selectedAction -> dashboardViewModel.executeQuickAction(selectedAction) { route -> navController.navigate(route) } }
                                )
                            }
                        }
                    }
                }
            }
            
            // Dashboard widgets (simplified for compilation)
            items(dashboardState.widgets) { widget ->
                when (widget) {
                    is DashboardWidget.WelcomeWidget -> SimpleWelcomeWidgetCard(widget)
                    is DashboardWidget.QuickStatsWidget -> SimpleQuickStatsWidgetCard(widget)
                    is DashboardWidget.BodyweightWidget -> SimpleBodyweightWidgetCard(
                        currentWeight = widget.currentWeight,
                        lastRecordedDate = widget.lastRecordedDate,
                        unit = widget.unit
                    )
                    is DashboardWidget.CycleProgressWidget -> SimpleCycleProgressWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.ActivityHeatmapWidget -> SimpleActivityHeatmapWidgetCard(widget)
                    is DashboardWidget.BodyweightTrendWidget -> SimpleBodyweightTrendWidgetCard(widget)
                    is DashboardWidget.PerformanceTrendWidget -> SimplePerformanceTrendWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.NextSessionWidget -> SimpleNextSessionWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.VolumeProgressWidget -> SimpleVolumeProgressWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.AchievementWidget -> SimpleAchievementWidgetCard(widget)
                    // All widget types are handled above
                }
            }
            
            // Low priority insights
            val lowPriorityInsights = dashboardState.insights.filter { 
                it.priority == InsightPriority.LOW || it.priority == InsightPriority.MEDIUM 
            }
            if (lowPriorityInsights.isNotEmpty()) {
                item {
                    EnhancedDashboardWidgetCard(
                        title = "Insights",
                        icon = Icons.Default.Lightbulb
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowPriorityInsights.forEach { insight ->
                                EnhancedInsightCard(
                                    insight = insight,
                                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
