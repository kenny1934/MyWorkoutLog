package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import com.kennychiu.myworkoutlog.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavHostController

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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised),
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised),
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
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
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
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised)
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

/**
 * Persistent top-of-dashboard CTA for the next unfinished session in the active cycle.
 * Renders nothing when no active cycle, no current week, or no next session — the cycle
 * is complete or unseeded. Mirrors the Deload / Target-RIR badges from
 * `SimpleCycleProgressWidgetCard` so both surfaces agree visually.
 */
@Composable
fun NextSessionCtaCard(
    cycle: ActiveProgramCycle,
    navController: NavHostController
) {
    val info = remember(cycle) { cycleProgress(cycle) }
    val nextWeek = info.currentWeek ?: return
    val nextSession = info.nextSession ?: return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val route = Screen.WorkoutLogger.createRoute(
                    templateId = nextSession.workoutTemplateId,
                    cycleId = cycle.cycleUuid,
                    weekId = nextWeek.id,
                    sessionId = nextSession.id
                )
                navController.navigate(route)
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start next session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nextSession.sessionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nextWeek.weekLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (nextWeek.isDeloadWeek) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Deload",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            val targetRir = nextWeek.targetRir?.takeIf { it.isNotBlank() }
            if (targetRir != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "RIR $targetRir",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
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
    val info = remember(widget.cycle) { cycleProgress(widget.cycle) }
    val cycleDateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.CycleDetail.route) },
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cycle Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (info.currentWeek?.isDeloadWeek == true) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Deload",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                val targetRir = info.currentWeek?.targetRir?.takeIf { it.isNotBlank() }
                if (targetRir != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "RIR $targetRir",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
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
                        progress = { widget.completionPercentage },
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
                    if (info.startDate != null && info.plannedEndDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Started ${info.startDate.format(cycleDateFormatter)} · " +
                                    "Planned end ${info.plannedEndDate.format(cycleDateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary action — the primary "Start next session" CTA lives
                    // at the top of the dashboard (NextSessionCtaCard, slice 36),
                    // so this widget only surfaces Analytics / Cycle-complete.
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
                            text = if (info.isComplete) "Cycle complete" else "Analytics",
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationCardRaised)
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
private fun PerformanceTrendMetaRow(timeframe: String, volumeTrend: ProgressTrend) {
    val (volumeIcon, volumeTint) = when (volumeTrend.direction) {
        TrendDirection.STRONGLY_IMPROVING,
        TrendDirection.SLIGHTLY_IMPROVING -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.primary
        TrendDirection.STRONGLY_DECLINING,
        TrendDirection.SLIGHTLY_DECLINING -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.error
        TrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
        TrendDirection.INSUFFICIENT_DATA -> Icons.Filled.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = timeframe,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = volumeIcon,
                contentDescription = null,
                tint = volumeTint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = volumeTrend.description,
                style = MaterialTheme.typography.labelSmall,
                color = volumeTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                // Header meta — timeframe + overall volume trend. Both fields were
                // computed in WidgetRepositorySimplified but never displayed
                // before slice 44; surfacing them gives the card context beyond
                // the single top-exercise strength summary below.
                PerformanceTrendMetaRow(
                    timeframe = widget.timeframe,
                    volumeTrend = widget.volumeTrend,
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                topExercise.improvementPercentage > 5 -> Icons.AutoMirrored.Filled.TrendingUp
                                topExercise.improvementPercentage < -5 -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.AutoMirrored.Filled.TrendingFlat
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
                                volumeChange > 5f -> Icons.AutoMirrored.Filled.TrendingUp
                                volumeChange < -5f -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.AutoMirrored.Filled.TrendingFlat
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
