package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import java.time.LocalDate

// Stat card component for displaying key metrics
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trend: ProgressTrend? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .let { if (onClick != null) it.clickable { onClick() } else it },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            trend?.let {
                Spacer(modifier = Modifier.height(4.dp))
                TrendIndicator(trend = it)
            }
        }
    }
}

// Progress ring component
@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    showPercentage: Boolean = true,
    animationDuration: Int = 1000
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing),
        label = "progress"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.toPx() - strokeWidthPx) / 2
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            
            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
            
            // Progress arc
            if (animatedProgress > 0) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                    size = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
        
        if (showPercentage) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Trend indicator component
@Composable
fun TrendIndicator(
    trend: ProgressTrend,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    val (icon, color) = when (trend.direction) {
        TrendDirection.STRONGLY_IMPROVING, TrendDirection.SLIGHTLY_IMPROVING ->
            Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF4CAF50)
        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING ->
            Icons.AutoMirrored.Filled.TrendingDown to Color(0xFFF44336)
        TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
        TrendDirection.INSUFFICIENT_DATA -> Icons.AutoMirrored.Filled.ShowChart to Color(0xFFFF9800)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = trend.description,
            modifier = Modifier.size(12.dp),
            tint = color
        )
        
        if (showPercentage && trend.percentage > 0) {
            Text(
                text = "${trend.percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Quick action button
@Composable
fun QuickActionButton(
    action: QuickAction,
    modifier: Modifier = Modifier,
    onClick: (QuickAction) -> Unit
) {
    Button(
        onClick = { onClick(action) },
        modifier = modifier,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.description,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// Insight card component
@Composable
fun InsightCard(
    insight: SmartInsight,
    modifier: Modifier = Modifier,
    onDismiss: ((String) -> Unit)? = null,
    onAction: ((SmartInsight) -> Unit)? = null
) {
    val backgroundColor = when (insight.type) {
        InsightType.WARNING -> MaterialTheme.colorScheme.errorContainer
        InsightType.CELEBRATION -> Color(0xFFE8F5E8)
        InsightType.RECOMMENDATION -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (insight.type) {
        InsightType.WARNING -> MaterialTheme.colorScheme.onErrorContainer
        InsightType.CELEBRATION -> Color(0xFF2E7D32)
        InsightType.RECOMMENDATION -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
                
                onDismiss?.let {
                    IconButton(
                        onClick = { it(insight.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(14.dp),
                            tint = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            if (insight.actionable && insight.actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onAction(insight) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = insight.actionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// Mini chart component for embedding in widgets
@Composable
fun MiniLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = maxValue - minValue
        
        if (range == 0f) return@Canvas
        
        val stepX = size.width / (data.size - 1)
        val stepY = size.height
        
        val points = data.mapIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * stepY
            Offset(x, y)
        }
        
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// Session difficulty indicator
@Composable
fun DifficultyIndicator(
    difficulty: SessionDifficulty,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (color, label, dots) = when (difficulty) {
        SessionDifficulty.LIGHT -> Triple(Color(0xFF4CAF50), "Light", 1)
        SessionDifficulty.MODERATE -> Triple(Color(0xFFFF9800), "Moderate", 2)
        SessionDifficulty.HARD -> Triple(Color(0xFFFF5722), "Hard", 3)
        SessionDifficulty.VERY_HARD -> Triple(Color(0xFFF44336), "Very Hard", 4)
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < dots) color else color.copy(alpha = 0.3f)
                    )
            )
        }
        
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Animated counter component
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    duration: Int = 1000
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = duration, easing = LinearEasing),
        label = "counter"
    )
    
    Text(
        text = animatedValue.toInt().toString(),
        style = textStyle,
        modifier = modifier
    )
}

// Heatmap grid for activity visualization
@Composable
fun ActivityHeatmapGrid(
    data: Map<java.time.LocalDate, WorkoutIntensity>,
    modifier: Modifier = Modifier,
    cellSize: Dp = 12.dp,
    spacing: Dp = 2.dp,
    weeksToShow: Int = 12
) {
    val today = java.time.LocalDate.now()
    val startDate = today.minusWeeks(weeksToShow.toLong())
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(7) { dayOfWeek ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(weeksToShow) { week ->
                    val date = startDate.plusWeeks(week.toLong()).plusDays(dayOfWeek.toLong())
                    val intensity = data[date]?.intensity ?: 0f
                    
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = if (intensity > 0) 0.2f + (intensity * 0.8f) else 0.1f
                                )
                            )
                    )
                }
            }
        }
    }
}

// Workout Heatmap Grid Component
@Composable
fun WorkoutHeatmapGrid(
    workoutDays: Map<LocalDate, WorkoutIntensity>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val startDate = today.minusDays(90) // Show last 90 days for simplicity
    
    Column(modifier = modifier) {
        // Header showing current month
        Text(
            text = "Last 90 Days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Create a grid showing the last 90 days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7), // 7 days per week
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(90) { dayOffset ->
                val date = startDate.plusDays(dayOffset.toLong())
                val intensity = workoutDays[date]?.intensity ?: 0f
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = getIntensityColor(intensity),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend
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
                for (i in 0..4) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = getIntensityColor(i / 4f),
                                shape = RoundedCornerShape(1.dp)
                            )
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
}

@Composable
private fun getIntensityColor(intensity: Float): Color {
    val primaryColor = MaterialTheme.colorScheme.primary
    return when {
        intensity <= 0f -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) // More visible empty state
        intensity <= 0.2f -> primaryColor.copy(alpha = 0.3f)
        intensity <= 0.4f -> primaryColor.copy(alpha = 0.5f)
        intensity <= 0.6f -> primaryColor.copy(alpha = 0.7f)
        intensity <= 0.8f -> primaryColor.copy(alpha = 0.9f)
        else -> primaryColor
    }
}

// Bodyweight Mini Chart Component
@Composable
fun BodyweightMiniChart(
    data: List<BodyweightPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 8.dp.toPx()
        
        val plotWidth = canvasWidth - (padding * 2)
        val plotHeight = canvasHeight - (padding * 2)
        
        // Find min/max weights for scaling
        val minWeight = data.minOfOrNull { it.weight } ?: 0f
        val maxWeight = data.maxOfOrNull { it.weight } ?: 100f
        val weightRange = maxWeight - minWeight
        
        if (weightRange == 0f) return@Canvas
        
        // Create path for line chart
        val path = androidx.compose.ui.graphics.Path()
        
        data.forEachIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1).toFloat()) * plotWidth
            val y = padding + plotHeight - ((point.weight - minWeight) / weightRange) * plotHeight
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        // Draw the line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw points
        data.forEachIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1).toFloat()) * plotWidth
            val y = padding + plotHeight - ((point.weight - minWeight) / weightRange) * plotHeight
            
            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

// Achievement Card Component
@Composable
fun AchievementCard(
    achievement: Achievement,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Achievement icon
            Text(
                text = achievement.icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Achievement details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (!isCompact) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Category badge
            Surface(
                color = getCategoryColor(achievement.category),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = achievement.category.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// Milestone Card Component
@Composable
fun MilestoneCard(
    milestone: Milestone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "${(milestone.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { milestone.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = milestone.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getCategoryColor(category: AchievementCategory): Color {
    return when (category) {
        AchievementCategory.STRENGTH -> Color(0xFFE53E3E)
        AchievementCategory.VOLUME -> Color(0xFF3182CE)
        AchievementCategory.CONSISTENCY -> Color(0xFF38A169)
        AchievementCategory.MILESTONE -> Color(0xFFD69E2E)
        AchievementCategory.SPECIAL -> Color(0xFF805AD5)
    }
}

// Difficulty Badge Component
@Composable
fun DifficultyBadge(
    difficulty: SessionDifficulty,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Calculate compact mode manually for consistency
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val columnCount = smartColumnCount()
    val availableWidthPerWidget = (layoutInfo.screenWidth - (layoutInfo.contentPadding * 2)) / columnCount
    val compactFromMode = availableWidthPerWidget < 180.dp
    val shouldUseCompactBadge = isCompact || compactFromMode
    
    val (color, label, dots) = when (difficulty) {
        SessionDifficulty.LIGHT -> Triple(Color(0xFF4CAF50), if (shouldUseCompactBadge) "L" else "Light", 1)
        SessionDifficulty.MODERATE -> Triple(Color(0xFFFF9800), if (shouldUseCompactBadge) "M" else "Moderate", 2)
        SessionDifficulty.HARD -> Triple(Color(0xFFFF5722), if (shouldUseCompactBadge) "H" else "Hard", 3)
        SessionDifficulty.VERY_HARD -> Triple(Color(0xFFF44336), if (shouldUseCompactBadge) "VH" else "Very Hard", 4)
    }
    
    // Force consistent label for testing
    val finalLabel = if (shouldUseCompactBadge) {
        when (difficulty) {
            SessionDifficulty.LIGHT -> "L"
            SessionDifficulty.MODERATE -> "M" 
            SessionDifficulty.HARD -> "H"
            SessionDifficulty.VERY_HARD -> "VH"
        }
    } else {
        when (difficulty) {
            SessionDifficulty.LIGHT -> "Light"
            SessionDifficulty.MODERATE -> "Moderate"
            SessionDifficulty.HARD -> "Hard"
            SessionDifficulty.VERY_HARD -> "Very Hard"
        }
    }
    
    // More detailed debug info
    val compactFromParam = isCompact
    val finalCompact = shouldUseCompactBadge
    
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(if (shouldUseCompactBadge) 8.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (shouldUseCompactBadge) 6.dp else 8.dp, 
                vertical = if (shouldUseCompactBadge) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (shouldUseCompactBadge) 2.dp else 4.dp)
        ) {
            // Difficulty dots (smaller in compact mode)
            if (!shouldUseCompactBadge) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < dots) color else color.copy(alpha = 0.3f)
                            )
                    )
                }
                
                Spacer(modifier = Modifier.width(2.dp))
            }
            
            Text(
                text = finalLabel,
                fontSize = if (shouldUseCompactBadge) 10.sp else MaterialTheme.typography.labelSmall.fontSize,
                color = color,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

// Exercise Preview Card Component
@Composable
fun ExercisePreviewCard(
    exercise: ExercisePreview,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exercise icon based on muscle group
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = getMuscleGroupColor(exercise.muscleGroup).copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getMuscleGroupIcon(exercise.muscleGroup),
                        contentDescription = exercise.muscleGroup.name,
                        modifier = Modifier.size(16.dp),
                        tint = getMuscleGroupColor(exercise.muscleGroup)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Exercise details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${exercise.sets} sets × ${exercise.reps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Weight info if available
            exercise.weight?.let { weight ->
                Text(
                    text = "${weight.toInt()}kg",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun getMuscleGroupColor(muscleGroup: MuscleGroup): Color {
    return when (muscleGroup) {
        MuscleGroup.CHEST -> Color(0xFFE53E3E)
        MuscleGroup.BACK -> Color(0xFF3182CE)
        MuscleGroup.SHOULDERS -> Color(0xFFED8936)
        MuscleGroup.BICEPS -> Color(0xFF805AD5)
        MuscleGroup.TRICEPS -> Color(0xFF805AD5)
        MuscleGroup.QUADS -> Color(0xFF38A169)
        MuscleGroup.HAMSTRINGS -> Color(0xFF38A169)
        MuscleGroup.GLUTES -> Color(0xFF38A169)
        MuscleGroup.CALVES -> Color(0xFF38A169)
        MuscleGroup.ABS -> Color(0xFFD69E2E)
        MuscleGroup.FOREARMS -> Color(0xFF805AD5)
        MuscleGroup.TRAPS -> Color(0xFFED8936)
        MuscleGroup.LATS -> Color(0xFF3182CE)
        MuscleGroup.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun getMuscleGroupIcon(muscleGroup: MuscleGroup): ImageVector {
    return when (muscleGroup) {
        MuscleGroup.CHEST -> Icons.Default.FitnessCenter
        MuscleGroup.BACK -> Icons.Default.FitnessCenter
        MuscleGroup.SHOULDERS -> Icons.Default.FitnessCenter
        MuscleGroup.BICEPS -> Icons.Default.FitnessCenter
        MuscleGroup.TRICEPS -> Icons.Default.FitnessCenter
        MuscleGroup.QUADS -> Icons.AutoMirrored.Filled.DirectionsRun
        MuscleGroup.HAMSTRINGS -> Icons.AutoMirrored.Filled.DirectionsRun
        MuscleGroup.GLUTES -> Icons.AutoMirrored.Filled.DirectionsRun
        MuscleGroup.CALVES -> Icons.AutoMirrored.Filled.DirectionsRun
        MuscleGroup.ABS -> Icons.Default.FitnessCenter
        MuscleGroup.FOREARMS -> Icons.Default.FitnessCenter
        MuscleGroup.TRAPS -> Icons.Default.FitnessCenter
        MuscleGroup.LATS -> Icons.Default.FitnessCenter
        MuscleGroup.OTHER -> Icons.Default.FitnessCenter
    }
}

