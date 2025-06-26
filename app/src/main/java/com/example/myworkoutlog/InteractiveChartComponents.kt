package com.example.myworkoutlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt

// Enhanced interactive chart with tap-to-drill-down functionality
@Composable
fun EnhancedInteractivePerformanceChart(
    strengthGains: List<ExerciseProgress>,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showTooltips: Boolean = true,
    enableDrillDown: Boolean = true
) {
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    val chartScrollState = rememberChartScrollState()
    
    // Convert real exercise data to chart entries - using improvement percentage for trend direction
    val chartEntries = if (strengthGains.isNotEmpty()) {
        strengthGains.mapIndexed { index, gain ->
            entryOf(index.toFloat(), gain.improvementPercentage)
        }
    } else {
        // Fallback sample data if no real data - improvement percentages
        listOf(
            entryOf(0f, 5f),
            entryOf(1f, 12f), 
            entryOf(2f, -3f),
            entryOf(3f, 18f),
            entryOf(4f, 8f)
        )
    }
    
    LaunchedEffect(strengthGains) {
        chartEntryModelProducer.setEntries(chartEntries)
    }

    Column(modifier = modifier) {
        if (strengthGains.isNotEmpty()) {
            Text(
                text = "Top Exercise Progress",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Show exercise names
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(strengthGains.take(3)) { gain: ExerciseProgress ->
                    FilterChip(
                        onClick = { 
                            if (enableDrillDown) {
                                navController.navigate("analytics")
                            }
                        },
                        label = { 
                            Text(
                                text = "${gain.exerciseName}: ${if (gain.improvementPercentage >= 0) "+" else ""}${gain.improvementPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        selected = false
                    )
                }
            }
        } else {
            Text(
                text = "Performance Trend (Sample Data)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Enhanced performance chart - using working Analytics pattern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { 
                    if (enableDrillDown) {
                        navController.navigate("analytics")
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(title = "Improvement (%)"),
                    bottomAxis = rememberBottomAxis(
                        title = "Exercises",
                        valueFormatter = { value, _ ->
                            val index = value.toInt()
                            if (index < strengthGains.size) {
                                strengthGains[index].exerciseName.take(8) // Truncate long names
                            } else {
                                "Exercise ${index + 1}"
                            }
                        }
                    ),
                    modifier = Modifier.height(200.dp),
                    chartScrollState = chartScrollState
                )
            }
        }
        
        if (strengthGains.isNotEmpty()) {
            Text(
                text = "Tap chart to view detailed analytics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedInteractiveVolumeChart(
    volumeData: List<VolumeDataPoint>,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showTooltips: Boolean = true,
    enableDrillDown: Boolean = true,
    showComparison: Boolean = false
) {
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    val chartScrollState = rememberChartScrollState()
    
    // Convert volume data to chart entries
    val chartEntries = if (volumeData.isNotEmpty()) {
        volumeData.mapIndexed { index, dataPoint ->
            entryOf(index.toFloat(), dataPoint.totalVolume.toFloat())
        }
    } else {
        // Fallback sample data
        listOf(
            entryOf(0f, 12500f),
            entryOf(1f, 13200f),
            entryOf(2f, 11800f),
            entryOf(3f, 14100f),
            entryOf(4f, 13600f)
        )
    }

    LaunchedEffect(volumeData) {
        chartEntryModelProducer.setEntries(chartEntries)
    }

    Column(modifier = modifier) {
        if (volumeData.isNotEmpty()) {
            Text(
                text = "Weekly Volume Progress",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Volume statistics
            val totalVolume = volumeData.sumOf { it.totalVolume }
            val avgVolume = if (volumeData.isNotEmpty()) totalVolume / volumeData.size else 0.0
            
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "${totalVolume.toInt()}kg",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Volume",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "${avgVolume.toInt()}kg",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly Avg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "Volume Trend (Sample Data)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Enhanced volume chart - using working Analytics pattern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { 
                    if (enableDrillDown) {
                        navController.navigate("analytics")
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(title = "Volume (kg)"),
                    bottomAxis = rememberBottomAxis(title = "Weeks"),
                    modifier = Modifier.height(200.dp),
                    chartScrollState = chartScrollState
                )
            }
        }
        
        if (volumeData.isNotEmpty()) {
            Text(
                text = "Tap chart to view detailed analytics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartTooltip(
    exercise: ExerciseProgress,
    position: Offset,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    
    Popup(
        offset = with(density) {
            androidx.compose.ui.unit.IntOffset(
                x = position.x.toDp().value.toInt(),
                y = (position.y - 100).toDp().value.toInt()
            )
        },
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clickable { onDismiss() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${if (exercise.improvementPercentage >= 0) "+" else ""}${exercise.improvementPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exercise.improvementPercentage >= 0) Color.Green else Color.Red,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${exercise.currentMax.toInt()}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun VolumeTooltip(
    dataPoint: VolumeDataPoint,
    position: Offset,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    
    Popup(
        offset = with(density) {
            androidx.compose.ui.unit.IntOffset(
                x = position.x.toDp().value.toInt(),
                y = (position.y - 100).toDp().value.toInt()
            )
        },
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clickable { onDismiss() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dataPoint.date,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${dataPoint.totalVolume.toInt()}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EnhancedExerciseDetailCard(
    exercise: ExerciseProgress,
    onDrillDown: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.let { modifier ->
            if (onDrillDown != null) {
                modifier.clickable { onDrillDown() }
            } else {
                modifier
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Improvement: ${if (exercise.improvementPercentage >= 0) "+" else ""}${exercise.improvementPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (exercise.improvementPercentage >= 0) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "${exercise.previousMax.toInt()}kg → ${exercise.currentMax.toInt()}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            if (onDrillDown != null) {
                IconButton(onClick = onDrillDown) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "View detailed analytics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedVolumeDetailCard(
    dataPoint: VolumeDataPoint,
    volumeData: List<VolumeDataPoint>,
    onDrillDown: (() -> Unit)? = null
) {
    val averageVolume = volumeData.map { it.totalVolume }.average()
    val percentageOfAverage = ((dataPoint.totalVolume / averageVolume) * 100).toInt()
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.let { modifier ->
            if (onDrillDown != null) {
                modifier.clickable { onDrillDown() }
            } else {
                modifier
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dataPoint.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Volume: ${dataPoint.totalVolume.toInt()}kg",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$percentageOfAverage% of average",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            if (onDrillDown != null) {
                IconButton(onClick = onDrillDown) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "View volume analysis",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeComparisonCard(
    currentData: List<VolumeDataPoint>,
    onDrillDown: (() -> Unit)? = null
) {
    if (currentData.size < 2) return
    
    val currentWeekVolume = currentData.takeLast(7).sumOf { it.totalVolume }
    val previousWeekVolume = if (currentData.size >= 14) {
        currentData.drop(currentData.size - 14).take(7).sumOf { it.totalVolume }
    } else {
        currentData.take(currentData.size / 2).sumOf { it.totalVolume }
    }
    
    val weeklyChange = if (previousWeekVolume > 0) {
        ((currentWeekVolume - previousWeekVolume) / previousWeekVolume * 100).toInt()
    } else {
        0
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.let { modifier ->
            if (onDrillDown != null) {
                modifier.clickable { onDrillDown() }
            } else {
                modifier
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Weekly Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This week: ${currentWeekVolume.toInt()}kg",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${if (weeklyChange >= 0) "+" else ""}$weeklyChange% vs last week",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (weeklyChange >= 0) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.error
                )
            }
            
            Icon(
                imageVector = when {
                    weeklyChange > 5 -> Icons.Default.TrendingUp
                    weeklyChange < -5 -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                },
                contentDescription = "Weekly trend",
                tint = when {
                    weeklyChange > 5 -> MaterialTheme.colorScheme.primary
                    weeklyChange < -5 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Enhanced Activity Heatmap Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedActivityHeatmap(
    workoutData: Map<LocalDate, WorkoutIntensity>,
    modifier: Modifier = Modifier,
    showYearlyView: Boolean = false,
    onDayClick: ((LocalDate, WorkoutIntensity?) -> Unit)? = null
) {
    var selectedPeriod by remember { mutableStateOf(if (showYearlyView) "Yearly" else "3 Months") }
    var selectedDate by remember { mutableStateOf(null as LocalDate?) }

    Column(modifier = modifier) {
        // Period selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Activity Pattern",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("3 Months", "6 Months", "Yearly").forEach { period ->
                    FilterChip(
                        onClick = { selectedPeriod = period },
                        label = { 
                            Text(
                                text = period,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedPeriod == period,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Enhanced heatmap grid
        when (selectedPeriod) {
            "3 Months" -> ActivityHeatmapCalendar(
                data = workoutData,
                weeksToShow = 12,
                onDayClick = { date, intensity ->
                    selectedDate = date
                    onDayClick?.invoke(date, intensity)
                },
                modifier = Modifier.fillMaxWidth()
            )
            "6 Months" -> ActivityHeatmapCalendar(
                data = workoutData,
                weeksToShow = 24,
                onDayClick = { date, intensity ->
                    selectedDate = date
                    onDayClick?.invoke(date, intensity)
                },
                modifier = Modifier.fillMaxWidth()
            )
            "Yearly" -> YearlyActivityHeatmap(
                data = workoutData,
                onDayClick = { date, intensity ->
                    selectedDate = date
                    onDayClick?.invoke(date, intensity)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Intensity legend
        Spacer(modifier = Modifier.height(12.dp))
        ActivityHeatmapLegend()
        
        // Selected day details
        selectedDate?.let { date ->
            val dayData = workoutData[date]
            if (dayData != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedDayDetails(
                    date = date,
                    intensity = dayData,
                    onDismiss = { selectedDate = null }
                )
            }
        }
    }
}

@Composable
private fun ActivityHeatmapCalendar(
    data: Map<LocalDate, WorkoutIntensity>,
    weeksToShow: Int,
    onDayClick: ((LocalDate, WorkoutIntensity?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startDate = today.minusWeeks(weeksToShow.toLong())
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Heatmap grid
        repeat(7) { dayOfWeek ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(weeksToShow) { week ->
                    val date = startDate.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                    val intensity = data[date]
                    
                    HeatmapCell(
                        date = date,
                        intensity = intensity,
                        onClick = onDayClick,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun YearlyActivityHeatmap(
    data: Map<LocalDate, WorkoutIntensity>,
    onDayClick: ((LocalDate, WorkoutIntensity?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startOfYear = today.withDayOfYear(1)
    val weeksInYear = 52
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").forEach { month ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Yearly heatmap (more compact)
        repeat(7) { dayOfWeek ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(weeksInYear) { week ->
                    val date = startOfYear.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                    val intensity = data[date]
                    
                    HeatmapCell(
                        date = date,
                        intensity = intensity,
                        onClick = onDayClick,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    date: LocalDate,
    intensity: WorkoutIntensity?,
    onClick: ((LocalDate, WorkoutIntensity?) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val intensityValue = intensity?.intensity ?: 0f
    val cellColor = getIntensityColor(intensityValue)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(cellColor)
            .let { mod ->
                if (onClick != null) {
                    mod.clickable { onClick(date, intensity) }
                } else {
                    mod
                }
            }
    )
}

private fun getIntensityColor(intensity: Float): Color {
    return when {
        intensity <= 0f -> Color(0xFFE0E0E0) // Light gray for no activity
        intensity <= 0.25f -> Color(0xFF81C784) // Light green
        intensity <= 0.5f -> Color(0xFF66BB6A) // Medium green
        intensity <= 0.75f -> Color(0xFF4CAF50) // Dark green
        else -> Color(0xFF2E7D32) // Very dark green
    }
}

@Composable
private fun ActivityHeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { intensity ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(getIntensityColor(intensity))
                )
            }
        }
        
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectedDayDetails(
    date: LocalDate,
    intensity: WorkoutIntensity,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Intensity: ${(intensity.intensity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Volume: ${intensity.volume.toInt()}kg",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Text(
                    text = "${intensity.duration}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Micro-Charts Components
@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    showDots: Boolean = false,
    fillArea: Boolean = false
) {
    if (data.isEmpty()) return
    
    val animatedData = data.map { value ->
        animateFloatAsState(
            targetValue = value,
            animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
            label = "sparkline_data"
        ).value
    }
    
    Canvas(modifier = modifier) {
        if (animatedData.size < 2) return@Canvas
        
        val maxValue = animatedData.maxOrNull() ?: 1f
        val minValue = animatedData.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        if (range == 0f) return@Canvas
        
        val stepX = size.width / (animatedData.size - 1)
        
        val points = animatedData.mapIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * size.height
            Offset(x, y)
        }
        
        // Fill area under line if requested
        if (fillArea) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, size.height)
                close()
            }
            
            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f)
                    )
                )
            )
        }
        
        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Draw dots if requested
        if (showDots) {
            points.forEach { point ->
                drawCircle(
                    color = color,
                    radius = strokeWidth.toPx() * 1.5f,
                    center = point
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    animationDurationMs: Int = 1000,
    showProgressText: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = animationDurationMs, easing = EaseOutCubic),
        label = "progress_ring"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
        }
        
        if (showProgressText) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TrendIndicator(
    value: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    size: Dp = 16.dp
) {
    val (icon, color) = when {
        value > 5f -> Icons.Default.TrendingUp to MaterialTheme.colorScheme.primary
        value < -5f -> Icons.Default.TrendingDown to MaterialTheme.colorScheme.error
        else -> Icons.Default.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Trend",
            tint = color,
            modifier = Modifier.size(size)
        )
        
        if (showPercentage) {
            Text(
                text = "${if (value >= 0) "+" else ""}${value.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MiniBarChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maxBars: Int = 7,
    showValues: Boolean = false
) {
    if (data.isEmpty()) return
    
    val displayData = if (data.size > maxBars) {
        data.takeLast(maxBars)
    } else {
        data
    }
    
    val maxValue = displayData.maxOrNull() ?: 1f
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        displayData.forEachIndexed { index, value ->
            val animatedHeight by animateFloatAsState(
                targetValue = if (maxValue > 0) value / maxValue else 0f,
                animationSpec = tween(
                    durationMillis = 500 + (index * 100),
                    easing = EaseOutCubic
                ),
                label = "bar_height_$index"
            )
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showValues) {
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedHeight)
                            .background(
                                color = color.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun MuscleGroupDistributionChart(
    muscleGroupData: Map<String, Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800),
        Color(0xFF9C27B0), Color(0xFFF44336), Color(0xFF607D8B),
        Color(0xFF795548), Color(0xFF3F51B5)
    )
) {
    if (muscleGroupData.isEmpty()) return
    
    val total = muscleGroupData.values.sum()
    if (total <= 0f) return
    
    val sortedData = muscleGroupData.toList().sortedByDescending { it.second }
    
    Column(modifier = modifier) {
        // Donut chart representation using progress bars
        sortedData.take(5).forEachIndexed { index, (muscleGroup, volume) ->
            val percentage = (volume / total * 100).toInt()
            val color = colors.getOrNull(index) ?: MaterialTheme.colorScheme.primary
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = muscleGroup,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PerformanceGauge(
    current: Float,
    target: Float,
    label: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    color: Color = MaterialTheme.colorScheme.primary
) {
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(
                progress = progress,
                strokeWidth = 6.dp,
                color = color,
                showProgressText = false,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = current.toInt().toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (target > 0f) {
            Text(
                text = "of ${target.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 8.sp
            )
        }
    }
}