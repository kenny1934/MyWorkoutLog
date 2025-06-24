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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cycle implementation placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Activity tracking placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Trend analysis placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SimplePerformanceTrendWidgetCard(widget: DashboardWidget.PerformanceTrendWidget) {
    SimpleExpandableWidgetCard(
        title = "Performance Trends",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            Text(
                text = "Performance overview placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        expandedContent = {
            Text(
                text = "Detailed performance analysis placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun SimpleNextSessionWidgetCard(widget: DashboardWidget.NextSessionWidget, navController: NavHostController) {
    SimpleExpandableWidgetCard(
        title = "Next Session",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            Text(
                text = "Next session overview placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        expandedContent = {
            Text(
                text = "Session details placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun SimpleVolumeProgressWidgetCard(widget: DashboardWidget.VolumeProgressWidget) {
    SimpleExpandableWidgetCard(
        title = "Volume Progress",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            Text(
                text = "Volume overview placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        expandedContent = {
            Text(
                text = "Volume analysis placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun SimpleAchievementWidgetCard(widget: DashboardWidget.AchievementWidget) {
    SimpleExpandableWidgetCard(
        title = "Achievements",
        isExpandable = widget.isExpandable,
        collapsedContent = {
            Text(
                text = "Achievements overview placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        expandedContent = {
            Text(
                text = "Achievement details placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
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
                    is DashboardWidget.PerformanceTrendWidget -> SimplePerformanceTrendWidgetCard(widget)
                    is DashboardWidget.NextSessionWidget -> SimpleNextSessionWidgetCard(
                        widget = widget,
                        navController = navController
                    )
                    is DashboardWidget.VolumeProgressWidget -> SimpleVolumeProgressWidgetCard(widget)
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
