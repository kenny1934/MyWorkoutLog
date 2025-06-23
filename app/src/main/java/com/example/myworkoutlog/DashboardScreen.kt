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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.*

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

@Composable
fun EnhancedDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
    val isLoading by dashboardViewModel.isLoading.collectAsStateWithLifecycle()
    val error by dashboardViewModel.error.collectAsStateWithLifecycle()
    
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
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
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
                    InsightCard(
                        insight = insight,
                        onDismiss = { dashboardViewModel.dismissInsight(it) },
                        onAction = { /* TODO: Handle insight actions */ }
                    )
                }
            }
            
            // Quick actions
            if (dashboardState.quickActions.isNotEmpty()) {
                item {
                    DashboardWidgetCard(
                        title = "Quick Actions",
                        icon = Icons.Default.FlashOn
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dashboardState.quickActions) { action ->
                                QuickActionButton(
                                    action = action,
                                    onClick = { dashboardViewModel.executeQuickAction(it) { route -> navController.navigate(route) } }
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
                    is DashboardWidget.PerformanceTrendWidget -> SimplePerformanceTrendWidgetCard(widget)
                    is DashboardWidget.NextSessionWidget -> SimpleNextSessionWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.VolumeProgressWidget -> SimpleVolumeProgressWidgetCard(widget)
                    is DashboardWidget.AchievementWidget -> SimpleAchievementWidgetCard(widget)
                    else -> {
                        // Fallback for other widget types
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = widget.title,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
            
            // Low priority insights
            val lowPriorityInsights = dashboardState.insights.filter { 
                it.priority == InsightPriority.LOW || it.priority == InsightPriority.MEDIUM 
            }
            if (lowPriorityInsights.isNotEmpty()) {
                item {
                    DashboardWidgetCard(
                        title = "Insights",
                        icon = Icons.Default.Lightbulb
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowPriorityInsights.forEach { insight ->
                                InsightCard(
                                    insight = insight,
                                    onDismiss = { dashboardViewModel.dismissInsight(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
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

    // Conditionally show the correct dashboard
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

@Composable
fun ActiveCycleDashboard(
    activeCycle: ActiveProgramCycle,
    programViewModel: ProgramViewModel,
    activeCycleViewModel: ActiveCycleViewModel,
    navController: NavHostController
) {
    // Use the embedded program data (snapshot) instead of dynamic lookup
    val program = activeCycle.cycleProgram
    
    // State for end cycle confirmation dialog
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
    
    // End cycle confirmation dialog
    if (showEndCycleConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndCycleConfirmation = false },
            title = { Text("End Cycle") },
            text = { Text("Are you sure you want to end the current cycle '${activeCycle.userCycleName}'? This action cannot be undone.") },
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
fun NoActiveCycleDashboard(
    historyViewModel: HistoryViewModel,
    onNavigate: (String) -> Unit
) {
    val loggedWorkouts by historyViewModel.allLoggedWorkouts.collectAsStateWithLifecycle()
    val latestWorkout = loggedWorkouts.firstOrNull()

    // Prepare data and labels for the chart
    val chartData = loggedWorkouts
        .filter { it.bodyweight != null && it.bodyweight > 0 }
        .sortedBy { it.date }

    val bodyweightEntries = chartData.mapIndexed { index, workout ->
        entryOf(index.toFloat(), workout.bodyweight!!.toFloat())
    }

    val chartModelProducer = ChartEntryModelProducer(bodyweightEntries)

    // Create a custom formatter for the bottom axis
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        // We use the integer part of the value as an index to get the date
        val index = value.toInt()
        if (index in chartData.indices) {
            // Format the date string like "06/08" from "2025-06-08"
            val date = chartData[index].date
            date.substring(5).replace('-', '/')
        } else {
            ""
        }
    }

    // Dynamic Greeting Logic
    val calendar = Calendar.getInstance()
    val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning!"
        in 12..16 -> "Good afternoon!"
        else -> "Good evening!"
    }

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
                    // Use the new dynamic greeting
                    Text(greeting, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ready for your next session?", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // The chart now has its axes configured with the new formatter
        if (bodyweightEntries.isNotEmpty()) {
            item {
                Text("Bodyweight Trend", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = chartModelProducer,
                        startAxis = rememberStartAxis(
                            title = "Bodyweight" // Add a title to the Y-axis
                        ),
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = bottomAxisValueFormatter, // Use our custom date formatter
                            guideline = null // Hide vertical guidelines for a cleaner look
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
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

        if (latestWorkout != null) {
            item {
                Text("Last Workout", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Navigate to the detail screen for this specific workout
                            onNavigate(Screen.HistoryDetail.createRoute(latestWorkout.id))
                        },
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(latestWorkout.name ?: "Workout", fontWeight = FontWeight.Bold)
                        Text(latestWorkout.date, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${latestWorkout.loggedExercises.size} exercises performed.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// Simplified Dashboard Widget Cards

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
fun SimpleCycleProgressWidgetCard(
    widget: DashboardWidget.CycleProgressWidget,
    navController: NavHostController
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    DashboardWidgetCard(
        title = "Cycle Progress",
        icon = Icons.Default.FitnessCenter,
        isExpanded = isExpanded,
        onExpandToggle = { isExpanded = !isExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        // Basic cycle info (always visible)
        Text(
            text = widget.cycle.userCycleName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = widget.weekProgress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = widget.sessionProgress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = widget.completionPercentage / 100f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${widget.completionPercentage.toInt()}% Complete",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Expanded content with session management
        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Session list
            val completedSessionIds = widget.cycle.completedSessions.keys.toSet()
            widget.cycle.cycleProgram.weeks.forEachIndexed { weekIndex, week ->
                Text(
                    text = "Week ${weekIndex + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                week.sessions.forEach { session ->
                    val sessionKey = "${week.id}_${session.id}"
                    val isCompleted = sessionKey in completedSessionIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isCompleted) {
                                    // Navigate to the logged workout history screen
                                    val loggedWorkoutId = widget.cycle.completedSessions[sessionKey]
                                    loggedWorkoutId?.let { workoutId ->
                                        navController.navigate(Screen.HistoryDetail.createRoute(workoutId))
                                    }
                                } else {
                                    // Start the session
                                    val route = Screen.WorkoutLogger.createRoute(
                                        templateId = session.workoutTemplateId,
                                        cycleId = widget.cycle.cycleUuid,
                                        weekId = week.id,
                                        sessionId = session.id
                                    )
                                    navController.navigate(route)
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isCompleted) "Completed" else "Not completed",
                                tint = if (isCompleted) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = session.sessionName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (!isCompleted) {
                            IconButton(
                                onClick = {
                                    val route = Screen.WorkoutLogger.createRoute(
                                        templateId = session.workoutTemplateId,
                                        cycleId = widget.cycle.cycleUuid,
                                        weekId = week.id,
                                        sessionId = session.id
                                    )
                                    navController.navigate(route)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start session",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SimpleActivityHeatmapWidgetCard(widget: DashboardWidget.ActivityHeatmapWidget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${widget.streakInfo.thisWeekCount}/${widget.streakInfo.weeklyTarget}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Best Streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBodyweightTrendWidgetCard(widget: DashboardWidget.BodyweightTrendWidget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bodyweight Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (widget.bodyweightData.isNotEmpty()) {
                // Prepare data for chart
                val chartData = widget.bodyweightData.mapIndexed { index, point ->
                    entryOf(index.toFloat(), point.weight)
                }
                
                val chartModelProducer = ChartEntryModelProducer(chartData)
                
                // Custom formatter for dates
                val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    val index = value.toInt()
                    if (index in widget.bodyweightData.indices) {
                        val date = widget.bodyweightData[index].date
                        "${date.monthValue}/${date.dayOfMonth}"
                    } else {
                        ""
                    }
                }
                
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(
                        title = "Weight (kg)"
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = bottomAxisValueFormatter,
                        guideline = null
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Trend indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val trendColor = when (widget.trend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Color(0xFF4CAF50)
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Color(0xFFF44336)
                        TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color(0xFFFF9800)
                    }
                    
                    val trendIcon = when (widget.trend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                        TrendDirection.STABLE -> Icons.Default.TrendingFlat
                        else -> Icons.Default.ShowChart
                    }
                    
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = "Trend",
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = widget.trend.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = trendColor
                    )
                    
                    if (widget.trend.percentage > 0) {
                        Text(
                            text = "${widget.trend.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = trendColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "No bodyweight data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SimplePerformanceTrendWidgetCard(widget: DashboardWidget.PerformanceTrendWidget) {
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
                    text = "Performance Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = widget.timeframe,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Overall volume trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Volume",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val trendColor = when (widget.volumeTrend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Color(0xFF4CAF50)
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Color(0xFFF44336)
                        TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color(0xFFFF9800)
                    }
                    
                    val trendIcon = when (widget.volumeTrend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                        TrendDirection.STABLE -> Icons.Default.TrendingFlat
                        else -> Icons.Default.ShowChart
                    }
                    
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = "Volume Trend",
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    
                    if (widget.volumeTrend.percentage > 0) {
                        Text(
                            text = "${widget.volumeTrend.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = trendColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            if (widget.strengthGains.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Top Exercise Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Exercise progress list
                widget.strengthGains.forEach { exerciseProgress ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exerciseProgress.exerciseName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${exerciseProgress.currentMax.toInt()}kg (was ${exerciseProgress.previousMax.toInt()}kg)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val progressColor = when (exerciseProgress.trend.direction) {
                                TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Color(0xFF4CAF50)
                                TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Color(0xFFF44336)
                                TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color(0xFFFF9800)
                            }
                            
                            val progressIcon = when (exerciseProgress.trend.direction) {
                                TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                                TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                                TrendDirection.STABLE -> Icons.Default.TrendingFlat
                                else -> Icons.Default.ShowChart
                            }
                            
                            Icon(
                                imageVector = progressIcon,
                                contentDescription = "Progress Trend",
                                tint = progressColor,
                                modifier = Modifier.size(12.dp)
                            )
                            
                            Text(
                                text = if (exerciseProgress.improvementPercentage > 0) "+${exerciseProgress.improvementPercentage.toInt()}%" else "${exerciseProgress.improvementPercentage.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = progressColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start working out to see performance trends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SimpleNextSessionWidgetCard(
    widget: DashboardWidget.NextSessionWidget, 
    navController: NavHostController
) {
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
                    text = "Next Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Difficulty indicator
                val difficultyColor = when (widget.difficulty) {
                    SessionDifficulty.LIGHT -> Color(0xFF4CAF50)
                    SessionDifficulty.MODERATE -> Color(0xFFFF9800)
                    SessionDifficulty.HARD -> Color(0xFFFF5722)
                    SessionDifficulty.VERY_HARD -> Color(0xFFF44336)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Difficulty",
                        tint = difficultyColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = widget.difficulty.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = difficultyColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Session info
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = widget.session.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Week",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = widget.session.weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Duration",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${widget.estimatedDuration} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (widget.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Exercise Preview (${widget.exercises.size} exercises)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show first 3 exercises
                widget.exercises.take(3).forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${exercise.sets} sets × ${exercise.reps} reps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Muscle group indicator
                        Text(
                            text = exercise.muscleGroup.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (widget.exercises.size > 3) {
                    Text(
                        text = "... and ${widget.exercises.size - 3} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Start session button
            Button(
                onClick = {
                    // Navigate to workout logger - need to extract the necessary parameters
                    // This would need access to the active cycle to get the proper route
                    // For now, we'll navigate to the session directly if possible
                    navController.navigate("workout_logger/${widget.session.id}")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start Session",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Session")
            }
        }
    }
}

@Composable
fun SimpleVolumeProgressWidgetCard(widget: DashboardWidget.VolumeProgressWidget) {
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
                    text = "Volume Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Trend indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val trendColor = when (widget.trend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Color(0xFF4CAF50)
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Color(0xFFF44336)
                        TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color(0xFFFF9800)
                    }
                    
                    val trendIcon = when (widget.trend.direction) {
                        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Icons.Default.TrendingUp
                        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> Icons.Default.TrendingDown
                        TrendDirection.STABLE -> Icons.Default.TrendingFlat
                        else -> Icons.Default.ShowChart
                    }
                    
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = "Volume Trend",
                        tint = trendColor,
                        modifier = Modifier.size(14.dp)
                    )
                    
                    if (widget.trend.percentage > 0) {
                        Text(
                            text = "${widget.trend.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = trendColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = widget.trend.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (widget.weeklyVolume.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current week stats
                val currentWeek = widget.weeklyVolume.lastOrNull()
                currentWeek?.let { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "This Week",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(week.totalVolume / 1000).toInt()}k kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        widget.targetVolume?.let { target ->
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Target",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(target / 1000).toInt()}k kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Weekly progression mini chart
                Text(
                    text = "Weekly Progression (${widget.weeklyVolume.size} weeks)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple bar chart representation
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(widget.weeklyVolume) { volumePoint ->
                        val maxVolume = widget.weeklyVolume.maxOfOrNull { it.totalVolume } ?: 1.0
                        val heightRatio = (volumePoint.totalVolume / maxVolume).toFloat()
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            // Bar
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height((heightRatio * 40).dp.coerceAtLeast(4.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Week label (simplified)
                            Text(
                                text = try {
                                    val date = java.time.LocalDate.parse(volumePoint.date)
                                    "W${date.dayOfYear / 7 + 1}"
                                } catch (e: Exception) {
                                    "W"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Volume summary
                val totalVolume = widget.weeklyVolume.sumOf { it.totalVolume }
                val averageVolume = if (widget.weeklyVolume.isNotEmpty()) {
                    totalVolume / widget.weeklyVolume.size
                } else 0.0
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Volume",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(totalVolume / 1000).toInt()}k kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Weekly Average",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(averageVolume / 1000).toInt()}k kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start working out to track volume progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SimpleAchievementWidgetCard(widget: DashboardWidget.AchievementWidget) {
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
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (widget.recentAchievements.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = Color(0xFFFFD700), // Gold color
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (widget.recentAchievements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Recent Achievements",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recent achievements list
                widget.recentAchievements.forEach { achievement ->
                    val categoryColor = when (achievement.category) {
                        AchievementCategory.STRENGTH -> Color(0xFFE91E63) // Pink for strength
                        AchievementCategory.VOLUME -> Color(0xFF2196F3) // Blue for volume
                        AchievementCategory.CONSISTENCY -> Color(0xFF4CAF50) // Green for consistency
                        AchievementCategory.MILESTONE -> Color(0xFFFF9800) // Orange for milestones
                        AchievementCategory.SPECIAL -> Color(0xFF9C27B0) // Purple for special
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Achievement icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        categoryColor.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = achievement.icon,
                                    fontSize = 18.sp
                                )
                            }
                            
                            // Achievement details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = achievement.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = categoryColor
                                )
                                Text(
                                    text = achievement.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Achievement date
                            Text(
                                text = try {
                                    val today = java.time.LocalDate.now()
                                    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(achievement.unlockedDate, today)
                                    when {
                                        daysAgo == 0L -> "Today"
                                        daysAgo == 1L -> "Yesterday"
                                        daysAgo <= 7L -> "${daysAgo}d ago"
                                        else -> "${achievement.unlockedDate.monthValue}/${achievement.unlockedDate.dayOfMonth}"
                                    }
                                } catch (e: Exception) {
                                    "Recent"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Next milestone section
            widget.nextMilestone?.let { milestone ->
                if (widget.recentAchievements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = milestone.progress.coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${(milestone.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Target: ${milestone.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Empty state
            if (widget.recentAchievements.isEmpty() && widget.nextMilestone == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "No achievements yet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start working out to unlock achievements!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// End of simplified dashboard implementation
