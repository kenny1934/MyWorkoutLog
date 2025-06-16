package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    // Get the details of the program blueprint for our active cycle
    val program by programViewModel.getProgramById(activeCycle.programTemplateId).collectAsState(initial = null)

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
            Button(onClick = { activeCycleViewModel.endCycle() }, modifier = Modifier.fillMaxWidth()) {
                Text("End Current Cycle")
            }
        }

        program?.weeks?.sortedBy { it.order }?.forEach { week ->
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
                                    cycleId = activeCycle.id.toString(),
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