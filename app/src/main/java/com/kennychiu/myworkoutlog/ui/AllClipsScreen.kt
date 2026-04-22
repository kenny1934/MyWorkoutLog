@file:OptIn(ExperimentalMaterial3Api::class)

package com.kennychiu.myworkoutlog.ui

import com.kennychiu.myworkoutlog.data.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Clips") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(visibleClips, key = { it.setId }) { clip ->
                        AllClipsCell(clip)
                    }
                }
            }
        }
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
            onAttach = null
        )
    }
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
