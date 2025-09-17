package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

// Helper function to format weight with appropriate decimal precision
private fun formatWeightValue(weight: Double): String {
    return when {
        weight % 1.0 == 0.0 -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

// Overloaded function for Float values
private fun formatWeightValue(weight: Float): String {
    return when {
        weight % 1.0f == 0.0f -> weight.toInt().toString() // Show as integer if no decimal part
        else -> String.format("%.1f", weight) // Show one decimal place
    }
}

// Widget component definitions - must be defined before they're used

@Composable
fun SimpleWelcomeWidgetCard(widget: DashboardWidget.WelcomeWidget) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f),
        label = "welcome_scale"
    )
    
    val isCompactMode = isCompactWidgetMode()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = animatedScale,
                scaleY = animatedScale
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(adaptiveContentPadding())) {
                Text(
                    text = widget.greeting,
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.headlineMedium.fontSize,
                        compactMultiplier = 0.75f,
                        mediumMultiplier = 0.85f,
                        expandedMultiplier = 1f
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = if (isCompactMode) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(if (isCompactMode) 8.dp else 12.dp))
                Text(
                    text = widget.motivationalMessage,
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.bodyLarge.fontSize,
                        compactMultiplier = 0.8f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = if (isCompactMode) 18.sp else 22.sp,
                    maxLines = if (isCompactMode) 3 else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (widget.currentStreak > 0) {
                    Spacer(modifier = Modifier.height(if (isCompactMode) 12.dp else 16.dp))
                    Surface(
                        shape = RoundedCornerShape(if (isCompactMode) 16.dp else 20.dp),
                        color = Color(0xFFFF6B35).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isCompactMode) "🔥 ${widget.currentStreak} days" else "🔥 ${widget.currentStreak} day streak - You're on fire!",
                            fontSize = adaptiveTextSize(
                                baseSize = MaterialTheme.typography.labelLarge.fontSize,
                                compactMultiplier = 0.85f,
                                mediumMultiplier = 0.9f,
                                expandedMultiplier = 1f
                            ),
                            color = Color(0xFFFF6B35),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = if (isCompactMode) 12.dp else 16.dp,
                                vertical = if (isCompactMode) 6.dp else 8.dp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = if (isCompactMode) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleQuickStatsWidgetCard(widget: DashboardWidget.QuickStatsWidget) {
    val isCompactMode = isCompactWidgetMode()
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val columnCount = smartColumnCount()
    val availableWidthPerWidget = (layoutInfo.screenWidth - (layoutInfo.contentPadding * 2)) / columnCount
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(adaptiveContentPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isCompactMode) 16.dp else 20.dp)
                )
                Spacer(modifier = Modifier.width(if (isCompactMode) 6.dp else 8.dp))
                Text(
                    text = "Quick Stats",
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.titleLarge.fontSize,
                        compactMultiplier = 0.8f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Stats content - clean layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                
                if (isCompactMode) {
                    // Compact: Top stat + bottom row
                    StatCard(
                        value = widget.totalWorkouts.toString(),
                        label = "Workouts",
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.FitnessCenter
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            value = widget.currentStreak.toString(),
                            label = "Streak",
                            color = Color(0xFFFF6B35),
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            value = widget.recentPRs.size.toString(),
                            label = "PRs",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Normal: Horizontal layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCard(
                            value = widget.totalWorkouts.toString(),
                            label = "Workouts",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.FitnessCenter,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(
                            value = widget.currentStreak.toString(),
                            label = "Streak",
                            color = Color(0xFFFF6B35),
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(
                            value = widget.recentPRs.size.toString(),
                            label = "PRs",
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val isCompactMode = isCompactWidgetMode()
    
    Surface(
        shape = RoundedCornerShape(if (isCompactMode) 8.dp else 12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompactMode) 60.dp else 80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(
                horizontal = if (isCompactMode) 8.dp else 12.dp,
                vertical = if (isCompactMode) 8.dp else 12.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(if (isCompactMode) 18.dp else 22.dp)
            )
            Spacer(modifier = Modifier.height(if (isCompactMode) 2.dp else 4.dp))
            Text(
                text = value,
                fontSize = adaptiveTextSize(
                    baseSize = MaterialTheme.typography.titleLarge.fontSize,
                    compactMultiplier = 0.85f,
                    mediumMultiplier = 0.9f,
                    expandedMultiplier = 1f
                ),
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(if (isCompactMode) 1.dp else 2.dp))
            Text(
                text = label,
                fontSize = adaptiveTextSize(
                    baseSize = MaterialTheme.typography.bodySmall.fontSize,
                    compactMultiplier = 0.9f,
                    mediumMultiplier = 0.95f,
                    expandedMultiplier = 1f
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SimpleBodyweightWidgetCard(
    currentWeight: Double?,
    lastRecordedDate: String?,
    unit: String
) {
    val isCompactMode = isCompactWidgetMode()
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(adaptiveContentPadding())) {
            Text(
                text = "Current Weight",
                fontSize = adaptiveTextSize(
                    baseSize = MaterialTheme.typography.titleMedium.fontSize,
                    compactMultiplier = 0.8f,
                    mediumMultiplier = 0.9f,
                    expandedMultiplier = 1f
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(if (isCompactMode) 6.dp else 8.dp))
            
            if (currentWeight != null) {
                Text(
                    text = "${formatWeightValue(currentWeight)} $unit",
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.headlineMedium.fontSize,
                        compactMultiplier = 0.8f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                lastRecordedDate?.let { date ->
                    Text(
                        text = if (isCompactMode) date else "Last recorded: $date",
                        fontSize = adaptiveTextSize(
                            baseSize = MaterialTheme.typography.bodySmall.fontSize,
                            compactMultiplier = 0.85f,
                            mediumMultiplier = 0.9f,
                            expandedMultiplier = 1f
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isCompactMode) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = if (isCompactMode) "No weight" else "No weight recorded",
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.bodyMedium.fontSize,
                        compactMultiplier = 0.85f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable  
fun SimpleCycleProgressWidgetCard(
    widget: DashboardWidget.CycleProgressWidget, 
    navController: NavHostController,
    onEndCycle: (() -> Unit)? = null
) {
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
                            Text(
                                text = "Analytics",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = adaptiveTextSize(
                                    baseSize = MaterialTheme.typography.labelMedium.fontSize,
                                    compactMultiplier = 0.8f,
                                    mediumMultiplier = 0.9f,
                                    expandedMultiplier = 1f
                                )
                            )
                        }
                    }
                    
                    // Add End Cycle button if callback provided
                    if (onEndCycle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onEndCycle,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Cycle")
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
                            text = "${if (currentWeight != null) formatWeightValue(currentWeight) else "0"} kg",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(Screen.Analytics.createRouteWithTab("Performance", topExercise.exerciseId))
                            },
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
                                text = "${topExercise.formatCurrentWeight()} (${if (topExercise.improvementPercentage >= 0) "+" else ""}${topExercise.improvementPercentage.toInt()}%)",
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
                            text = "Tap ${widget.strengthGains.first().exerciseName} for details",
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
            val isCompactMode = isCompactWidgetMode()
            
            Column {
                Text(
                    text = widget.session.name,
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.titleMedium.fontSize,
                        compactMultiplier = 0.8f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isCompactMode) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(if (isCompactMode) 2.dp else 4.dp))
                Text(
                    text = widget.session.weekLabel,
                    fontSize = adaptiveTextSize(
                        baseSize = MaterialTheme.typography.bodySmall.fontSize,
                        compactMultiplier = 0.85f,
                        mediumMultiplier = 0.9f,
                        expandedMultiplier = 1f
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(if (isCompactMode) 6.dp else 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompactMode) 14.dp else 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(if (isCompactMode) 2.dp else 4.dp))
                        Text(
                            text = "${widget.estimatedDuration} min",
                            fontSize = adaptiveTextSize(
                                baseSize = MaterialTheme.typography.bodySmall.fontSize,
                                compactMultiplier = 0.8f,
                                mediumMultiplier = 0.9f,
                                expandedMultiplier = 1f
                            ),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = widget.session.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = widget.session.weekLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Give badge sufficient space with minimum width
                    Box(
                        modifier = Modifier.widthIn(min = 60.dp)
                    ) {
                        DifficultyBadge(difficulty = widget.difficulty)
                    }
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


@Composable
fun ArrowReorderWidgetCard(
    widget: DashboardWidget,
    navController: NavHostController,
    isCustomizationMode: Boolean,
    isWidgetVisible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleVisibility: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEndCycle: (() -> Unit)? = null
) {
    // Use transparent Card to preserve enhanced styling while maintaining structure  
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Render the actual widget content
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
                navController = navController,
                onEndCycle = onEndCycle
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
            }
            
            // Customization overlay (only show in customization mode)
            if (isCustomizationMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reorder arrow buttons
                    Column {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move up",
                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move down",
                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = widget.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                    
                    // Visibility toggle
                    IconButton(
                        onClick = { onToggleVisibility(widget.id) }
                    ) {
                        Icon(
                            imageVector = if (isWidgetVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isWidgetVisible) "Hide widget" else "Show widget",
                            tint = if (isWidgetVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveWidgetGrid(
    widgets: List<DashboardWidget>,
    quickActions: List<QuickAction>,
    insights: List<SmartInsight>,
    isCustomizationMode: Boolean,
    dashboardPreferences: DashboardPreferences,
    hiddenWidgets: List<DashboardWidget>,
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController,
    layoutInfo: AdaptiveLayoutInfo,
    onShowCompleteCycleConfirmation: (Boolean) -> Unit,
    onPendingCompleteCycleAction: (QuickAction?) -> Unit
) {
    // Calculate layout values before LazyColumn
    val columnCount = if (layoutInfo.useTwoColumns) {
        smartColumnCount(minWidgetWidth = 280.dp)
    } else {
        1
    }
    val spacing = adaptiveSpacing()
    val widgetHeight = adaptiveWidgetHeight()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // High priority insights (same as compact mode)
        val urgentInsights = insights.filter { 
            it.priority == InsightPriority.URGENT || it.priority == InsightPriority.HIGH 
        }
        items(urgentInsights) { insight ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedInsightCard(
                    insight = insight,
                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                    onAction = { insight -> 
                        dashboardViewModel.executeInsightAction(insight) { route -> 
                            navController.navigate(route) 
                        }
                    }
                )
            }
        }
        
        // Low priority insights (same as compact mode)
        val lowPriorityInsights = insights.filter { 
            it.priority == InsightPriority.LOW || it.priority == InsightPriority.MEDIUM 
        }
        items(lowPriorityInsights) { insight ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                EnhancedInsightCard(
                    insight = insight,
                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                    onAction = { insight -> 
                        dashboardViewModel.executeInsightAction(insight) { route -> 
                            navController.navigate(route) 
                        }
                    }
                )
            }
        }
        
        // Quick actions (if needed for large screens)
        if (quickActions.isNotEmpty()) {
            item {
                EnhancedDashboardWidgetCard(
                    title = "Quick Actions",
                    icon = Icons.Default.FlashOn
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(quickActions) { action ->
                            QuickActionButton(
                                action = action,
                                onClick = { clickedAction ->
                                    when (clickedAction.action) {
                                        QuickActionType.COMPLETE_CYCLE -> {
                                            onPendingCompleteCycleAction(clickedAction)
                                            onShowCompleteCycleConfirmation(true)
                                        }
                                        else -> {
                                            dashboardViewModel.executeQuickAction(clickedAction) { route -> 
                                                navController.navigate(route) 
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Widgets Section with Adaptive Grid
        if (layoutInfo.useTwoColumns) {
            // Smart grid layout that prevents widget squishing
            val chunkedWidgets = widgets.chunked(columnCount)
            
            items(chunkedWidgets) { widgetRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    widgetRow.forEach { widget ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = widgetHeight)
                        ) {
                            val index = widgets.indexOf(widget)
                            ArrowReorderWidgetCard(
                                widget = widget,
                                navController = navController,
                                isCustomizationMode = isCustomizationMode,
                                isWidgetVisible = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }?.isEnabled != false,
                                canMoveUp = index > 0,
                                canMoveDown = index < widgets.size - 1,
                                onToggleVisibility = { widgetId -> dashboardViewModel.toggleWidgetVisibility(widgetId) },
                                onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                                onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                                onEndCycle = null
                            )
                        }
                    }
                    // Fill remaining space if row is not complete
                    repeat(columnCount - widgetRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Single column layout for compact screens
            itemsIndexed(widgets) { index, widget ->
                ArrowReorderWidgetCard(
                    widget = widget,
                    navController = navController,
                    isCustomizationMode = isCustomizationMode,
                    isWidgetVisible = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }?.isEnabled != false,
                    canMoveUp = index > 0,
                    canMoveDown = index < widgets.size - 1,
                    onToggleVisibility = { widgetId -> dashboardViewModel.toggleWidgetVisibility(widgetId) },
                    onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                    onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                    onEndCycle = null
                )
            }
        }
        
        // Hidden widgets section (only show in customization mode)
        if (isCustomizationMode && hiddenWidgets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hidden widgets",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hidden Widgets",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        hiddenWidgets.forEach { widget ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = widget.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            dashboardViewModel.toggleWidgetVisibility(widget.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Show widget",
                                            tint = MaterialTheme.colorScheme.primary
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
}

@Composable
fun AdaptiveDashboardContent(
    dashboardState: DashboardState,
    isLoading: Boolean,
    error: String?,
    isCustomizationMode: Boolean,
    dashboardPreferences: DashboardPreferences,
    hiddenWidgets: List<DashboardWidget>,
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController,
    layoutInfo: AdaptiveLayoutInfo,
    showCompleteCycleConfirmation: Boolean,
    pendingCompleteCycleAction: QuickAction?,
    onShowCompleteCycleConfirmation: (Boolean) -> Unit,
    onPendingCompleteCycleAction: (QuickAction?) -> Unit
) {
    val spacing = adaptiveSpacing()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(layoutInfo.contentPadding)
    ) {
        // Header with customization toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showDebugDialog by remember { mutableStateOf(false) }
            
            Text(
                text = "Dashboard",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.combinedClickable(
                    onClick = { /* Normal click does nothing */ },
                    onLongClick = { showDebugDialog = true }
                )
            )
            
            OutlinedButton(
                onClick = { dashboardViewModel.toggleCustomizationMode() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isCustomizationMode) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = if (isCustomizationMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isCustomizationMode) "Done" else "Customize",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCustomizationMode) "Done" else "Edit")
            }
        }
        
        Spacer(modifier = Modifier.height(spacing))
        
        when {
            isLoading && dashboardState.widgets.isEmpty() -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                // Error state  
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { dashboardViewModel.onPullToRefresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                // Content with adaptive layout
                AdaptiveWidgetGrid(
                    widgets = dashboardState.widgets,
                    quickActions = dashboardState.quickActions,
                    insights = dashboardState.insights,
                    isCustomizationMode = isCustomizationMode,
                    dashboardPreferences = dashboardPreferences,
                    hiddenWidgets = hiddenWidgets,
                    dashboardViewModel = dashboardViewModel,
                    navController = navController,
                    layoutInfo = layoutInfo,
                    onShowCompleteCycleConfirmation = onShowCompleteCycleConfirmation,
                    onPendingCompleteCycleAction = onPendingCompleteCycleAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    navController: NavHostController
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
    val isLoading by dashboardViewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by dashboardViewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by dashboardViewModel.error.collectAsStateWithLifecycle()
    val isCustomizationMode by dashboardViewModel.isCustomizationMode.collectAsStateWithLifecycle()
    val dashboardPreferences by dashboardViewModel.dashboardPreferences.collectAsStateWithLifecycle()
    val hiddenWidgets by dashboardViewModel.hiddenWidgets.collectAsStateWithLifecycle()
    val showBodyweightDialog by dashboardViewModel.showBodyweightDialog.collectAsStateWithLifecycle()
    
    // Adaptive layout information
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val spacing = adaptiveSpacing()
    
    // Setup simple LazyColumn state (non-widget items)
    val lazyListState = rememberLazyListState()
    
    // Confirmation dialog state
    var showCompleteCycleConfirmation by remember { mutableStateOf(false) }
    var pendingCompleteCycleAction by remember { mutableStateOf<QuickAction?>(null) }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { dashboardViewModel.onPullToRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (layoutInfo.useTwoColumns) {
            // Use adaptive grid layout for large screens
            AdaptiveDashboardContent(
                dashboardState = dashboardState,
                isLoading = isLoading,
                error = error,
                isCustomizationMode = isCustomizationMode,
                dashboardPreferences = dashboardPreferences,
                hiddenWidgets = hiddenWidgets,
                dashboardViewModel = dashboardViewModel,
                navController = navController,
                layoutInfo = layoutInfo,
                showCompleteCycleConfirmation = showCompleteCycleConfirmation,
                pendingCompleteCycleAction = pendingCompleteCycleAction,
                onShowCompleteCycleConfirmation = { showCompleteCycleConfirmation = it },
                onPendingCompleteCycleAction = { pendingCompleteCycleAction = it }
            )
        } else {
            // Use single column layout for compact screens
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(layoutInfo.contentPadding),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
            // Header with customization toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showDebugDialog by remember { mutableStateOf(false) }
                    
                    Text(
                        text = "Dashboard",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.combinedClickable(
                            onClick = { /* Normal click does nothing */ },
                            onLongClick = { showDebugDialog = true }
                        )
                    )
                    
                    // Debug Dialog
                    if (showDebugDialog) {
                        AlertDialog(
                            onDismissRequest = { showDebugDialog = false },
                            title = { Text("Debug Options") },
                            text = { Text("Reset dismissed insights to show them again for testing?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dashboardViewModel.resetDismissedInsights()
                                        showDebugDialog = false
                                    }
                                ) {
                                    Text("Reset Insights")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDebugDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    IconButton(
                        onClick = { dashboardViewModel.toggleCustomizationMode() }
                    ) {
                        Icon(
                            imageVector = if (isCustomizationMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = if (isCustomizationMode) "Exit customization" else "Customize dashboard",
                            tint = if (isCustomizationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            }
            
            // Content (when not loading)
            if (!isLoading) {
                // High priority insights
                val urgentInsights = dashboardState.insights.filter { 
                    it.priority == InsightPriority.URGENT || it.priority == InsightPriority.HIGH 
                }
                items(urgentInsights) { insight ->
                    EnhancedInsightCard(
                        insight = insight,
                        onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                        onAction = { insight -> 
                            dashboardViewModel.executeInsightAction(insight) { route -> 
                                navController.navigate(route) 
                            }
                        }
                    )
                }
                
                // Quick actions
                if (dashboardState.quickActions.isNotEmpty()) {
                    item {
                        EnhancedDashboardWidgetCard(
                            title = "Quick Actions",
                            icon = Icons.Default.FlashOn
                        ) {
                            Column {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    items(dashboardState.quickActions) { action ->
                                        EnhancedQuickActionButton(
                                            action = action,
                                            onClick = { selectedAction -> 
                                                if (selectedAction.action == QuickActionType.COMPLETE_CYCLE) {
                                                    // Show confirmation dialog for cycle completion
                                                    pendingCompleteCycleAction = selectedAction
                                                    showCompleteCycleConfirmation = true
                                                } else {
                                                    // Execute other actions directly
                                                    dashboardViewModel.executeQuickAction(selectedAction) { route -> 
                                                        navController.navigate(route) 
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                
                                // Scroll indicator when there are more than 3 actions
                                if (dashboardState.quickActions.size > 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(3) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        CircleShape
                                                    )
                                            )
                                            if (index < 2) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Swipe for more actions",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Dashboard widgets with arrow button reordering
                itemsIndexed(
                    items = dashboardState.widgets,
                    key = { _, widget -> "widget_${widget.id}" }
                ) { index, widget ->
                    // Get current visibility state from preferences
                    val widgetConfig = dashboardPreferences.widgetConfigs.find { it.widgetType == widget.id }
                    val isWidgetVisible = widgetConfig?.isEnabled ?: widget.isVisible
                    
                    ArrowReorderWidgetCard(
                        widget = widget,
                        navController = navController,
                        isCustomizationMode = isCustomizationMode,
                        isWidgetVisible = isWidgetVisible,
                        canMoveUp = index > 0,
                        canMoveDown = index < dashboardState.widgets.size - 1,
                        onToggleVisibility = { widgetId: String ->
                            dashboardViewModel.toggleWidgetVisibility(widgetId)
                        },
                        onMoveUp = { dashboardViewModel.moveWidgetUp(index) },
                        onMoveDown = { dashboardViewModel.moveWidgetDown(index) },
                        onEndCycle = {
                            dashboardViewModel.executeQuickAction(
                                QuickAction(
                                    id = "end_cycle",
                                    title = "End Cycle",
                                    description = "End current cycle",
                                    icon = Icons.Default.Close,
                                    action = QuickActionType.COMPLETE_CYCLE
                                )
                            ) { route -> navController.navigate(route) }
                        }
                    )
                }
            
                // Hidden widgets section (only show in customization mode)
                if (isCustomizationMode && hiddenWidgets.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden widgets",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Hidden Widgets",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            hiddenWidgets.forEach { widget ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = widget.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        IconButton(
                                            onClick = { 
                                                dashboardViewModel.toggleWidgetVisibility(widget.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Show widget",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
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
                        EnhancedDashboardWidgetCard(
                        title = "Insights",
                        icon = Icons.Default.Lightbulb
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            lowPriorityInsights.forEach { insight ->
                                EnhancedInsightCard(
                                    insight = insight,
                                    onDismiss = { insightId -> dashboardViewModel.dismissInsight(insightId) },
                                    onAction = { insight -> 
                                        dashboardViewModel.executeInsightAction(insight) { route -> 
                                            navController.navigate(route) 
                                        }
                                    }
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
    
    // Complete Cycle Confirmation Dialog
    if (showCompleteCycleConfirmation && pendingCompleteCycleAction != null) {
        AlertDialog(
            onDismissRequest = { 
                showCompleteCycleConfirmation = false
                pendingCompleteCycleAction = null
            },
            title = { 
                Text(
                    text = "Complete Cycle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Text(
                    text = "Are you sure you want to complete the current cycle? This will end your current program and you can start a new one.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteCycleConfirmation = false
                        pendingCompleteCycleAction?.let { action ->
                            dashboardViewModel.executeQuickAction(action) { route -> 
                                navController.navigate(route) 
                            }
                        }
                        pendingCompleteCycleAction = null
                    }
                ) {
                    Text("Complete Cycle")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCompleteCycleConfirmation = false
                        pendingCompleteCycleAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bodyweight Entry Dialog
    if (showBodyweightDialog) {
        BodyweightEntryDialog(
            onDismiss = { dashboardViewModel.hideBodyweightDialog() },
            onSave = { weight, date, notes ->
                dashboardViewModel.saveBodyweightEntry(weight, date, notes)
            },
            weightUnit = "kg" // TODO: Get from user preferences
        )
    }
}

