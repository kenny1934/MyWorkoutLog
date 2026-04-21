package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailScreen(
    viewModel: CycleDetailViewModel,
    navController: NavHostController,
    onNavigateUp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        // The outer MainActivity Scaffold already consumes the bottom system-nav-bar
        // inset via its bottomBar. Without zeroing insets here, this nested Scaffold
        // reserves that space again and leaves a blank strip above the nav bar.
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        val cycle = state.cycle
        if (cycle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No active cycle",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val info = remember(cycle) { cycleProgress(cycle) }
        val completed = cycle.completedSessions
        val aggregates = state.aggregates
        var showRenameDialog by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // Week cards follow the header + storyboard (both always present) and
        // two conditional cards. Index of orderedWeeks[i] = weekStartIndex + i.
        val weekStartIndex = 2 +
            (if (info.isComplete) 1 else 0) +
            (if (aggregates.prsHit.isNotEmpty()) 1 else 0)

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CycleHeaderCard(
                    cycle = cycle,
                    info = info,
                    onRenameClick = { showRenameDialog = true },
                )
            }
            item {
                CycleStoryboardStrip(
                    cycle = cycle,
                    info = info,
                    onWeekClick = { weekIndex ->
                        coroutineScope.launch {
                            listState.animateScrollToItem(weekStartIndex + weekIndex)
                        }
                    },
                )
            }
            if (info.isComplete) {
                item {
                    CycleSummaryCard(
                        info = info,
                        aggregates = aggregates,
                    )
                }
            }
            if (aggregates.prsHit.isNotEmpty()) {
                item {
                    CyclePrsCard(
                        prs = aggregates.prsHit,
                        onPrClick = { pr ->
                            navController.navigate(Screen.HistoryDetail.createRoute(pr.loggedWorkoutId))
                        },
                    )
                }
            }
            items(info.orderedWeeks) { week ->
                CycleWeekCard(
                    week = week,
                    completedSessions = completed,
                    isCurrentWeek = week.id == info.currentWeek?.id,
                    aggregate = aggregates.perWeek[week.id],
                    weightUnit = aggregates.weightUnit,
                    templates = state.templates,
                    onSessionClick = { session ->
                        val key = "${week.id}_${session.id}"
                        val completedWorkoutId = completed[key]
                        if (completedWorkoutId != null) {
                            navController.navigate(Screen.HistoryDetail.createRoute(completedWorkoutId))
                        } else {
                            navController.navigate(Screen.TemplateDetail.createRoute(session.workoutTemplateId))
                        }
                    },
                )
            }
        }

        if (showRenameDialog) {
            RenameCycleDialog(
                currentName = cycle.userCycleName,
                onConfirm = { newName ->
                    viewModel.renameActiveCycle(newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false },
            )
        }
    }
}

@Composable
private fun CycleHeaderCard(
    cycle: ActiveProgramCycle,
    info: CycleProgressInfo,
    onRenameClick: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cycle.userCycleName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename cycle",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = cycle.programTemplateName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (info.startDate != null && info.plannedEndDate != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Started ${info.startDate.format(dateFormatter)} · " +
                        "Planned end ${info.plannedEndDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            val total = info.totalSessionCount
            val done = info.completedSessionCount
            val pct = if (total > 0) done.toFloat() / total else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$done / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (info.isComplete) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Cycle complete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CycleStoryboardStrip(
    cycle: ActiveProgramCycle,
    info: CycleProgressInfo,
    onWeekClick: (Int) -> Unit,
) {
    if (info.orderedWeeks.isEmpty()) return
    val currentWeekId = info.currentWeek?.id
    val completed = cycle.completedSessions

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = "Cycle plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(info.orderedWeeks) { index, week ->
                    val phase = remember(week, cycle) { classifyCycleWeek(week, cycle) }
                    val isCurrent = week.id == currentWeekId
                    val sessionsSorted = remember(week) { week.sessions.sortedBy { it.order } }
                    val completedFlags = sessionsSorted.map { session ->
                        completed.containsKey("${week.id}_${session.id}")
                    }
                    WeekPill(
                        weekNumber = index + 1,
                        phaseLabel = phase.label,
                        completedFlags = completedFlags,
                        isCurrent = isCurrent,
                        onClick = { onWeekClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekPill(
    weekNumber: Int,
    phaseLabel: String,
    completedFlags: List<Boolean>,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 84.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "W$weekNumber",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (completedFlags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    completedFlags.forEach { done ->
                        Icon(
                            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleSummaryCard(
    info: CycleProgressInfo,
    aggregates: CycleAggregates,
) {
    val unit = aggregates.weightUnit ?: "kg"
    val totalSets = aggregates.perWeek.values.sumOf { it.setCount }
    val totalVolume = aggregates.perWeek.values.sumOf { it.totalVolume }
    val totalDurationMs = aggregates.perWeek.values.sumOf { it.totalDurationMs }
    val topPr = remember(aggregates.prsHit) {
        aggregates.prsHit
            .map { it.pr }
            .filter { it.type == PRType.MAX_WEIGHT_FOR_REPS }
            .maxByOrNull { it.weight ?: 0.0 }
            ?: aggregates.prsHit.firstOrNull()?.pr
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cycle summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AggregateChip(
                    label = "Workouts",
                    value = "${info.completedSessionCount}",
                    modifier = Modifier.weight(1f),
                )
                AggregateChip(
                    label = "Sets",
                    value = "$totalSets",
                    modifier = Modifier.weight(1f),
                )
                AggregateChip(
                    label = "PRs",
                    value = "${aggregates.prsHit.size}",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AggregateChip(
                    label = "Volume",
                    value = "${trim(totalVolume)} $unit",
                    modifier = Modifier.weight(1f),
                )
                AggregateChip(
                    label = "Time",
                    value = formatDurationShort(totalDurationMs),
                    modifier = Modifier.weight(1f),
                )
            }
            if (topPr != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Top PR · ${topPr.exerciseName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatPrSummary(topPr),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CyclePrsCard(
    prs: List<CyclePrHit>,
    onPrClick: (PersonalRecord) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "PRs this cycle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${prs.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            prs.forEach { hit ->
                PrRow(hit = hit, onClick = { onPrClick(hit.pr) })
            }
        }
    }
}

@Composable
private fun PrRow(hit: CyclePrHit, onClick: () -> Unit) {
    val pr = hit.pr
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pr.exerciseName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatPrSummary(pr),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = pr.date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPrSummary(pr: PersonalRecord): String {
    val unit = pr.weightUnit ?: "kg"
    return when (pr.type) {
        PRType.MAX_WEIGHT_FOR_REPS -> {
            val w = pr.weight?.let { trim(it) } ?: "-"
            val r = pr.reps?.toString() ?: "-"
            "Max weight · $w $unit × $r"
        }
        PRType.MAX_REPS_AT_WEIGHT -> {
            val w = pr.weight?.let { trim(it) } ?: "-"
            val r = pr.reps?.toString() ?: "-"
            "Max reps · $r @ $w $unit"
        }
        PRType.DURATION -> {
            val s = pr.durationSecs ?: 0
            "Duration · ${formatSecondsShort(s)}"
        }
    }
}

private fun trim(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)

private fun formatSecondsShort(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

@Composable
private fun CycleWeekCard(
    week: ProgramWeekDefinition,
    completedSessions: Map<String, String>,
    isCurrentWeek: Boolean,
    aggregate: CycleWeekAggregate?,
    weightUnit: String?,
    templates: Map<String, WorkoutTemplate>,
    onSessionClick: (ProgramSessionDefinition) -> Unit,
) {
    val rir = week.targetRir?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isCurrentWeek) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        } else {
            CardDefaults.cardColors()
        },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = week.weekLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrentWeek) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                if (week.isDeloadWeek) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "Deload",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                if (rir != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = "RIR $rir",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            if (aggregate != null && aggregate.workoutCount > 0) {
                Spacer(Modifier.height(8.dp))
                WeekAggregatesRow(aggregate = aggregate, weightUnit = weightUnit)
            }

            Spacer(Modifier.height(12.dp))

            val sortedSessions = remember(week) { week.sessions.sortedBy { it.order } }
            sortedSessions.forEach { session ->
                val done = completedSessions.containsKey("${week.id}_${session.id}")
                SessionRow(
                    sessionName = session.sessionName,
                    done = done,
                    template = templates[session.workoutTemplateId],
                    weightUnit = weightUnit ?: "kg",
                    onClick = { onSessionClick(session) },
                )
            }
        }
    }
}

@Composable
private fun WeekAggregatesRow(aggregate: CycleWeekAggregate, weightUnit: String?) {
    val unit = weightUnit ?: "kg"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AggregateChip(
            label = "Sets",
            value = aggregate.setCount.toString(),
            modifier = Modifier.weight(1f),
        )
        AggregateChip(
            label = "Volume",
            value = "${trim(aggregate.totalVolume)} $unit",
            modifier = Modifier.weight(1f),
        )
        AggregateChip(
            label = "Time",
            value = formatDurationShort(aggregate.totalDurationMs),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AggregateChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatDurationShort(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun SessionRow(
    sessionName: String,
    done: Boolean,
    template: WorkoutTemplate?,
    weightUnit: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (done) "Completed" else "Not completed",
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sessionName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
            )
            if (template != null && template.templateExercises.isNotEmpty()) {
                SessionExercisePreview(template = template, weightUnit = weightUnit, done = done)
            }
        }
    }
}

@Composable
private fun SessionExercisePreview(
    template: WorkoutTemplate,
    weightUnit: String,
    done: Boolean,
) {
    val sortedExercises = remember(template) { template.templateExercises.sortedBy { it.order } }
    val preview = sortedExercises.take(3)
    val overflow = sortedExercises.size - preview.size
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (done) 0.55f else 0.85f,
    )
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (done) 0.4f else 0.7f,
    )
    Spacer(Modifier.height(2.dp))
    preview.forEach { ex ->
        val hint = formatProgressionHint(ex, weightUnit = weightUnit)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = ex.exerciseName,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (hint != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "· $hint",
                    style = MaterialTheme.typography.bodySmall,
                    color = hintColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (overflow > 0) {
        Text(
            text = "+$overflow more",
            style = MaterialTheme.typography.bodySmall,
            color = hintColor,
        )
    }
}
