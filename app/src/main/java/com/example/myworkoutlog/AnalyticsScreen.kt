@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.myworkoutlog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val volumeData by viewModel.volumeProgressionData.collectAsStateWithLifecycle()
    val muscleGroupDistribution by viewModel.muscleGroupDistribution.collectAsStateWithLifecycle()
    val exercisePerformanceTrend by viewModel.exercisePerformanceTrend.collectAsStateWithLifecycle()
    val personalRecordProgress by viewModel.personalRecordProgress.collectAsStateWithLifecycle()
    val cycleComparison by viewModel.cycleComparison.collectAsStateWithLifecycle()
    val availableExercises by viewModel.availableExercises.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Volume", "Performance", "PRs", "Comparison")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Analytics Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.refreshAnalytics() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh Analytics",
                    tint = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Range Selector
        TimeRangeSelector(
            selectedTimeRange = selectedTimeRange,
            onTimeRangeSelected = viewModel::selectTimeRange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTab) {
            0 -> OverviewTab(
                volumeData = volumeData,
                muscleGroupDistribution = muscleGroupDistribution,
                cycleComparison = cycleComparison
            )
            1 -> VolumeTab(
                volumeData = volumeData,
                muscleGroupDistribution = muscleGroupDistribution
            )
            2 -> PerformanceTab(
                availableExercises = availableExercises,
                selectedExerciseId = selectedExerciseId,
                exercisePerformanceTrend = exercisePerformanceTrend,
                onExerciseSelected = viewModel::selectExercise
            )
            3 -> PersonalRecordsTab(
                availableExercises = availableExercises,
                selectedExerciseId = selectedExerciseId,
                personalRecordProgress = personalRecordProgress,
                onExerciseSelected = viewModel::selectExercise
            )
            4 -> ComparisonTab(cycleComparison = cycleComparison)
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimeRange.values()) { timeRange ->
            FilterChip(
                selected = timeRange == selectedTimeRange,
                onClick = { onTimeRangeSelected(timeRange) },
                label = { Text(timeRange.displayName) }
            )
        }
    }
}

@Composable
private fun OverviewTab(
    volumeData: List<VolumeDataPoint>,
    muscleGroupDistribution: List<MuscleGroupVolume>,
    cycleComparison: CycleComparison?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Key Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Total Volume",
                    value = "${volumeData.sumOf { it.totalVolume }.toInt()}",
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Workouts",
                    value = "${volumeData.size}",
                    unit = "sessions",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Volume",
                    value = if (volumeData.isNotEmpty()) "${(volumeData.sumOf { it.totalVolume } / volumeData.size).toInt()}" else "0",
                    unit = "kg/workout",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Volume Trend Chart
        item {
            VolumeProgressionChart(volumeData = volumeData)
        }

        // Top Muscle Groups
        item {
            TopMuscleGroupsCard(muscleGroupDistribution = muscleGroupDistribution.take(5))
        }

        // Cycle Progress (if available)
        cycleComparison?.let { comparison ->
            item {
                CycleProgressCard(cycleComparison = comparison)
            }
        }
    }
}

@Composable
private fun VolumeTab(
    volumeData: List<VolumeDataPoint>,
    muscleGroupDistribution: List<MuscleGroupVolume>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            VolumeProgressionChart(volumeData = volumeData)
        }
        
        item {
            MuscleGroupDistributionChart(muscleGroupDistribution = muscleGroupDistribution)
        }
        
        item {
            MuscleGroupDetailsList(muscleGroupDistribution = muscleGroupDistribution)
        }
    }
}

@Composable
private fun PerformanceTab(
    availableExercises: List<Exercise>,
    selectedExerciseId: String?,
    exercisePerformanceTrend: PerformanceTrend?,
    onExerciseSelected: (String?) -> Unit
) {
    Column {
        ExerciseSelector(
            exercises = availableExercises,
            selectedExerciseId = selectedExerciseId,
            onExerciseSelected = onExerciseSelected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        exercisePerformanceTrend?.let { trend ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PerformanceTrendCard(trend = trend)
                }
                
                if (trend.dataPoints.isNotEmpty()) {
                    item {
                        PerformanceChart(dataPoints = trend.dataPoints)
                    }
                }
                
                item {
                    RecommendationCard(recommendation = trend.recommendedAction ?: "No recommendations available")
                }
            }
        } ?: Text(
            "Select an exercise to view performance trends",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PersonalRecordsTab(
    availableExercises: List<Exercise>,
    selectedExerciseId: String?,
    personalRecordProgress: PersonalRecordProgress?,
    onExerciseSelected: (String?) -> Unit
) {
    Column {
        ExerciseSelector(
            exercises = availableExercises,
            selectedExerciseId = selectedExerciseId,
            onExerciseSelected = onExerciseSelected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        personalRecordProgress?.let { progress ->
            PersonalRecordCard(progress = progress)
        } ?: Text(
            "Select an exercise to view personal record progress",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ComparisonTab(
    cycleComparison: CycleComparison?
) {
    cycleComparison?.let { comparison ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CycleComparisonCard(comparison = comparison)
            }
            
            item {
                StrengthGainsCard(strengthGains = comparison.strengthGains)
            }
        }
    } ?: Text(
        "No active cycle to compare",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge
    )
}

// Individual UI Components

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VolumeProgressionChart(volumeData: List<VolumeDataPoint>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Volume Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (volumeData.isNotEmpty()) {
                val chartModelProducer = remember { ChartEntryModelProducer() }
                val chartScrollState = rememberChartScrollState()
                
                LaunchedEffect(volumeData) {
                    val entries = volumeData.mapIndexed { index, dataPoint ->
                        entryOf(index.toFloat(), dataPoint.totalVolume.toFloat())
                    }
                    chartModelProducer.setEntries(entries)
                }
                
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(title = "Volume (kg)"),
                    bottomAxis = rememberBottomAxis(title = "Workouts"),
                    modifier = Modifier.height(200.dp),
                    chartScrollState = chartScrollState
                )
            } else {
                Text(
                    "No volume data available",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TopMuscleGroupsCard(muscleGroupDistribution: List<MuscleGroupVolume>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Top Muscle Groups",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            muscleGroupDistribution.forEach { muscleGroup ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        muscleGroup.muscleGroup.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.titlecase() }
                    )
                    Text(
                        "${muscleGroup.percentage.toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleProgressCard(cycleComparison: CycleComparison) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Current Cycle Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Program: ${cycleComparison.programTemplateName}")
            Text("Completion: ${cycleComparison.completionRate.toInt()}%")
            cycleComparison.averageWorkoutDuration?.let { duration ->
                Text("Avg Workout: ${duration}min")
            }
            cycleComparison.totalVolumeChange?.let { change ->
                Text(
                    "Volume Change: ${if (change >= 0) "+" else ""}${change.toInt()}%",
                    color = if (change >= 0) Color.Green else Color.Red
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupDistributionChart(muscleGroupDistribution: List<MuscleGroupVolume>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Muscle Group Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (muscleGroupDistribution.isNotEmpty()) {
                val chartModelProducer = remember { ChartEntryModelProducer() }
                
                LaunchedEffect(muscleGroupDistribution) {
                    val entries = muscleGroupDistribution.mapIndexed { index, muscleGroup ->
                        entryOf(index.toFloat(), muscleGroup.totalVolume.toFloat())
                    }
                    chartModelProducer.setEntries(entries)
                }
                
                Chart(
                    chart = columnChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(title = "Volume (kg)"),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = AxisValueFormatter { value, _ ->
                            muscleGroupDistribution.getOrNull(value.toInt())?.muscleGroup?.name
                                ?.replace("_", " ")?.lowercase()
                                ?.replaceFirstChar { it.titlecase() } ?: ""
                        }
                    ),
                    modifier = Modifier.height(200.dp)
                )
            } else {
                Text(
                    "No muscle group data available",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupDetailsList(muscleGroupDistribution: List<MuscleGroupVolume>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Detailed Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            muscleGroupDistribution.forEach { muscleGroup ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            muscleGroup.muscleGroup.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.titlecase() },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${muscleGroup.exerciseCount} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${muscleGroup.totalVolume.toInt()}kg",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${muscleGroup.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (muscleGroup != muscleGroupDistribution.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ExerciseSelector(
    exercises: List<Exercise>,
    selectedExerciseId: String?,
    onExerciseSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedExercise = exercises.find { it.id == selectedExerciseId }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedExercise?.name ?: "Select an exercise",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        onExerciseSelected(exercise.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PerformanceTrendCard(trend: PerformanceTrend) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Performance Trend: ${trend.exerciseName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trend Direction")
                    Text(
                        trend.trendDirection.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.titlecase() },
                        fontWeight = FontWeight.Bold,
                        color = when (trend.trendDirection) {
                            TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING -> Color.Green
                            TrendDirection.STABLE -> MaterialTheme.colorScheme.primary
                            TrendDirection.SLIGHTLY_DECLINING, TrendDirection.STRONGLY_DECLINING -> Color.Red
                            TrendDirection.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Trend Strength")
                    Text(
                        "${(trend.trendStrength * 100).toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceChart(dataPoints: List<ExercisePerformancePoint>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Performance Over Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dataPoints.isNotEmpty()) {
                val chartModelProducer = remember { ChartEntryModelProducer() }
                
                LaunchedEffect(dataPoints) {
                    val entries = dataPoints.mapIndexed { index, point ->
                        entryOf(index.toFloat(), point.estimated1RM?.toFloat() ?: 0f)
                    }
                    chartModelProducer.setEntries(entries)
                }
                
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(title = "Estimated 1RM (kg)"),
                    bottomAxis = rememberBottomAxis(title = "Workouts"),
                    modifier = Modifier.height(200.dp)
                )
            } else {
                Text(
                    "No performance data available",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Recommendation",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                recommendation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PersonalRecordCard(progress: PersonalRecordProgress) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Personal Record: ${progress.exerciseName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Current PR: ${progress.currentPR.date}")
            when (progress.currentPR.type) {
                PRType.MAX_WEIGHT_FOR_REPS -> {
                    Text("${progress.currentPR.weight}kg × ${progress.currentPR.reps} reps")
                }
                PRType.MAX_REPS_AT_WEIGHT -> {
                    Text("${progress.currentPR.reps} reps @ ${progress.currentPR.weight}kg")
                }
                PRType.DURATION -> {
                    Text("${progress.currentPR.durationSecs}s")
                }
            }
            
            progress.improvement?.let { improvement ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Improvement: ${if (improvement >= 0) "+" else ""}${improvement}",
                    color = if (improvement >= 0) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CycleComparisonCard(comparison: CycleComparison) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cycle Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Program: ${comparison.programTemplateName}")
            Text("Completion Rate: ${comparison.completionRate.toInt()}%")
            
            comparison.totalVolumeChange?.let { change ->
                Text(
                    "Volume Change: ${if (change >= 0) "+" else ""}${change.toInt()}%",
                    color = if (change >= 0) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            
            comparison.averageWorkoutDuration?.let { duration ->
                Text("Average Workout Duration: ${duration}min")
            }
        }
    }
}

@Composable
private fun StrengthGainsCard(strengthGains: List<ExerciseStrengthGain>) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Strength Gains",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (strengthGains.isNotEmpty()) {
                strengthGains.forEach { gain ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            gain.exerciseName,
                            modifier = Modifier.weight(1f)
                        )
                        gain.strengthGainPercentage?.let { percentage ->
                            Text(
                                "${if (percentage >= 0) "+" else ""}${percentage.toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = if (percentage >= 0) Color.Green else Color.Red
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No strength gains data available",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}