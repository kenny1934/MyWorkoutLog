package com.example.myworkoutlog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

// Base widget card component
@Composable
fun DashboardWidgetCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isExpanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .let { mod ->
                if (onExpandToggle != null) {
                    mod.clickable { onExpandToggle() }
                } else {
                    mod
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row {
                    actions()
                    
                    onExpandToggle?.let {
                        IconButton(
                            onClick = it,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            content()
        }
    }
}

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
            Icons.Default.TrendingUp to Color(0xFF4CAF50)
        TrendDirection.STRONGLY_DECLINING, TrendDirection.SLIGHTLY_DECLINING -> 
            Icons.Default.TrendingDown to Color(0xFFF44336)
        TrendDirection.STABLE -> Icons.Default.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
        TrendDirection.INSUFFICIENT_DATA -> Icons.Default.ShowChart to Color(0xFFFF9800)
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

// Simple bodyweight widget card
@Composable
fun SimpleBodyweightWidgetCard(
    currentWeight: Double?,
    lastRecordedDate: String?,
    unit: String,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(
        title = "Current Weight",
        icon = Icons.Default.MonitorWeight,
        modifier = modifier
    ) {
        if (currentWeight != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            }
        } else {
            Text(
                text = "No bodyweight data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
