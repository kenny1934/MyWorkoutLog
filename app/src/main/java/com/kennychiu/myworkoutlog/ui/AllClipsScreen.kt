@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class AllClipEntry(
    val setId: String,
    val workoutId: String,
    val date: String,
    val startTimestamp: Long?,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val videoRef: String,
    val videoMarks: String?
)

private data class ExerciseFilterOption(
    val exerciseId: String,
    val exerciseName: String
)

@Composable
fun AllClipsScreen(
    viewModel: HistoryViewModel,
    onNavigateUp: () -> Unit
) {
    val workouts by viewModel.allLoggedWorkouts.collectAsStateWithLifecycle()
    val allClips = remember(workouts) { flattenClipsChronological(workouts) }
    val exerciseOptions = remember(allClips) {
        allClips
            .map { ExerciseFilterOption(it.exerciseId, it.exerciseName) }
            .distinctBy { it.exerciseId }
            .sortedBy { it.exerciseName.lowercase() }
    }
    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleClips = remember(allClips, selectedExerciseId) {
        val id = selectedExerciseId
        if (id == null) allClips else allClips.filter { it.exerciseId == id }
    }
    val layoutInfo = rememberAdaptiveLayoutInfo()
    val columnCount = if (layoutInfo.useMasterDetail) 4 else 2

    val groupedByDate = remember(visibleClips) { visibleClips.groupBy { it.date } }
    val dateToHeaderIndex = remember(groupedByDate) {
        buildMap<String, Int> {
            var cursor = 0
            groupedByDate.forEach { (date, clips) ->
                put(date, cursor)
                cursor += 1 + clips.size
            }
        }
    }
    val videoDateMillis = remember(visibleClips) {
        visibleClips.asSequence()
            .map { it.date }
            .distinct()
            .mapNotNull { parseIsoDateToUtcMidnightMillis(it) }
            .toSet()
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val currentStickyDate by remember(dateToHeaderIndex) {
        derivedStateOf {
            if (dateToHeaderIndex.isEmpty()) null
            else dateToHeaderIndex.entries
                .lastOrNull { it.value <= gridState.firstVisibleItemIndex }
                ?.key
        }
    }
    val showStickyHeader by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 ||
                gridState.firstVisibleItemScrollOffset > 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Clips") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (videoDateMillis.isNotEmpty()) {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Jump to date"
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (exerciseOptions.isNotEmpty()) {
                ExerciseFilterChipRow(
                    options = exerciseOptions,
                    selectedExerciseId = selectedExerciseId,
                    onSelectionChange = { selectedExerciseId = it }
                )
            }

            if (visibleClips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AllClipsEmptyState(
                        hasAnyClip = allClips.isNotEmpty(),
                        filterName = exerciseOptions.firstOrNull { it.exerciseId == selectedExerciseId }?.exerciseName,
                        onClearFilter = { selectedExerciseId = null }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedByDate.forEach { (date, clipsForDate) ->
                            item(
                                key = "hdr-$date",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                DateGroupHeader(isoDate = date, clipCount = clipsForDate.size)
                            }
                            items(clipsForDate, key = { it.setId }) { clip ->
                                AllClipsCell(clip)
                            }
                        }
                    }
                    StickyDateHeaderOverlay(
                        visible = showStickyHeader && currentStickyDate != null,
                        isoDate = currentStickyDate,
                        clipCount = currentStickyDate?.let { groupedByDate[it]?.size } ?: 0,
                        onClick = {
                            val date = currentStickyDate ?: return@StickyDateHeaderOverlay
                            val index = dateToHeaderIndex[date] ?: return@StickyDateHeaderOverlay
                            coroutineScope.launch { gridState.animateScrollToItem(index) }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        JumpToDateDialog(
            selectableMillis = videoDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                val iso = utcMillisToIsoDate(millis)
                val index = dateToHeaderIndex[iso]
                if (index != null) {
                    coroutineScope.launch { gridState.animateScrollToItem(index) }
                }
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun JumpToDateDialog(
    selectableMillis: Set<Long>,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val selectableYears = remember(selectableMillis) {
        selectableMillis.map { utcMillisToYear(it) }.toSet()
    }
    val selectableDates = remember(selectableMillis, selectableYears) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in selectableMillis
            override fun isSelectableYear(year: Int): Boolean =
                year in selectableYears
        }
    }
    val initialMillis = remember(selectableMillis) { selectableMillis.maxOrNull() }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = selectableDates
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let(onDateSelected) },
                enabled = pickerState.selectedDateMillis != null
            ) {
                Text("Jump")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(
            state = pickerState,
            showModeToggle = false
        )
    }
}

@Composable
private fun ExerciseFilterChipRow(
    options: List<ExerciseFilterOption>,
    selectedExerciseId: String?,
    onSelectionChange: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "__all__") {
            FilterChip(
                onClick = { onSelectionChange(null) },
                label = { Text("All") },
                selected = selectedExerciseId == null
            )
        }
        items(options, key = { it.exerciseId }) { option ->
            FilterChip(
                onClick = { onSelectionChange(option.exerciseId) },
                label = { Text(option.exerciseName) },
                selected = selectedExerciseId == option.exerciseId
            )
        }
    }
}

@Composable
private fun AllClipsEmptyState(
    hasAnyClip: Boolean,
    filterName: String?,
    onClearFilter: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when {
            !hasAnyClip -> {
                Text(
                    text = "No clips yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Attach a video to a set during a workout to see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            filterName != null -> {
                Text(
                    text = "No clips for $filterName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClearFilter) {
                    Text("Show all clips")
                }
            }
            else -> {
                Text(
                    text = "No clips match this filter",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllClipsCell(clip: AllClipEntry) {
    val thumbnail = rememberVideoThumbnail(clip.videoRef)
    var showSheet by rememberSaveable(clip.setId) { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Clip for set ${clip.setNumber} of ${clip.exerciseName}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = clip.exerciseName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "${clip.date} · Set ${clip.setNumber}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showSheet) {
        VideoPlayerSheet(
            setNumber = null,
            initialUri = clip.videoRef,
            initialMarks = clip.videoMarks,
            onDismiss = { showSheet = false },
            onAttach = null,
            exerciseId = clip.exerciseId,
            excludingWorkoutId = clip.workoutId
        )
    }
}

@Composable
private fun StickyDateHeaderOverlay(
    visible: Boolean,
    isoDate: String?,
    clipCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && isoDate != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (isoDate != null) {
            StickyDateHeaderBar(
                isoDate = isoDate,
                clipCount = clipCount,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun StickyDateHeaderBar(
    isoDate: String,
    clipCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatDateHeader(isoDate),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (clipCount == 1) "1 clip" else "$clipCount clips",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DateGroupHeader(isoDate: String, clipCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatDateHeader(isoDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (clipCount == 1) "1 clip" else "$clipCount clips",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val isoDateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dateHeaderFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
private val utcIsoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun formatDateHeader(isoDate: String): String {
    return try {
        val parsed = isoDateParser.parse(isoDate) ?: return isoDate
        dateHeaderFormatter.format(parsed)
    } catch (_: Exception) {
        isoDate
    }
}

private fun parseIsoDateToUtcMidnightMillis(isoDate: String): Long? {
    return try {
        utcIsoDateFormatter.parse(isoDate)?.time
    } catch (_: Exception) {
        null
    }
}

private fun utcMillisToIsoDate(utcMillis: Long): String =
    utcIsoDateFormatter.format(Date(utcMillis))

private fun utcMillisToYear(utcMillis: Long): Int {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = utcMillis
    return cal.get(Calendar.YEAR)
}

private fun flattenClipsChronological(workouts: List<LoggedWorkout>): List<AllClipEntry> =
    workouts.flatMap { workout ->
        workout.loggedExercises.flatMap { exercise ->
            exercise.sets.mapIndexedNotNull { index, set ->
                val ref = set.videoReference?.takeIf { it.isNotBlank() }
                ref?.let {
                    AllClipEntry(
                        setId = set.id,
                        workoutId = workout.id,
                        date = workout.date,
                        startTimestamp = workout.startTimestamp,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        setNumber = index + 1,
                        videoRef = it,
                        videoMarks = set.videoMarks
                    )
                }
            }
        }
    }.sortedWith(
        compareByDescending<AllClipEntry> { it.date }
            .thenByDescending { it.startTimestamp ?: 0L }
    )
