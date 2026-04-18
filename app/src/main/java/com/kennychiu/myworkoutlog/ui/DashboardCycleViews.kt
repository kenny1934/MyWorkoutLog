package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.time.format.DateTimeFormatter

private val DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy")

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
    val progress = remember(activeCycle) { cycleProgress(activeCycle) }
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
            CycleProgressSummary(progress)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showEndCycleConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End Current Cycle")
            }
        }

        progress.orderedWeeks.forEachIndexed { weekIndex, week ->
            val isCurrentWeek = weekIndex == progress.currentWeekIndex
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        week.weekLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isCurrentWeek) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCurrentWeek) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isCurrentWeek) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Current",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            items(week.sessions.sortedBy { it.order }) { session ->
                val sessionKey = "${week.id}_${session.id}"
                val isCompleted = activeCycle.completedSessions.containsKey(sessionKey)
                val workoutId = activeCycle.completedSessions[sessionKey]
                val isNextUp = !isCompleted && session.id == progress.nextSession?.id &&
                        week.id == progress.currentWeek?.id

                val cardColors = if (isNextUp) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    CardDefaults.cardColors()
                }

                Card(elevation = CardDefaults.cardElevation(2.dp), colors = cardColors) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.sessionName)
                            if (isNextUp) {
                                Text(
                                    "Up next",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
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
private fun CycleProgressSummary(progress: CycleProgressInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val totalWeeks = progress.orderedWeeks.size
            val headline = when {
                progress.isComplete -> "Cycle complete"
                progress.currentWeekIndex != null && totalWeeks > 0 ->
                    "Week ${progress.currentWeekIndex + 1} of $totalWeeks"
                else -> "No sessions scheduled"
            }
            Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (progress.totalSessionCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${progress.completedSessionCount} of ${progress.totalSessionCount} sessions done",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (progress.startDate != null && progress.plannedEndDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Started ${progress.startDate.format(DISPLAY_DATE)} · " +
                            "Planned end ${progress.plannedEndDate.format(DISPLAY_DATE)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
