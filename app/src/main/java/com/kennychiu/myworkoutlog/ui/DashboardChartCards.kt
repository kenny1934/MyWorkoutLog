package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter

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
                text = "No performance data yet",
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
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
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
            Spacer(modifier = Modifier.height(Dimens.spacing12))
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
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable {
                navController.navigate(Screen.Analytics.createRouteWithTab("Performance", exercise.exerciseId))
            },
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
                    text = exercise.formatWeightProgression(),
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
                        exercise.improvementPercentage > 5 -> Icons.AutoMirrored.Filled.TrendingUp
                        exercise.improvementPercentage < -5 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
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
                text = "No volume data yet",
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
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
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
            Spacer(modifier = Modifier.height(Dimens.spacing12))
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
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No volume data yet",
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Volume Statistics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))

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
                Spacer(modifier = Modifier.height(Dimens.spacing12))
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
