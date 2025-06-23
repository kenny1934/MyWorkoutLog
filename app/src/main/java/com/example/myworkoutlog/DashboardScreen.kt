package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
                    is DashboardWidget.CycleProgressWidget -> SimpleCycleProgressWidgetCard(widget)
                    is DashboardWidget.ActivityHeatmapWidget -> SimpleActivityHeatmapWidgetCard(widget)
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
fun SimpleCycleProgressWidgetCard(widget: DashboardWidget.CycleProgressWidget) {
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
            Spacer(modifier = Modifier.height(12.dp))
            
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

// End of simplified dashboard implementation
