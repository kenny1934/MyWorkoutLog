package com.kennychiu.myworkoutlog.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kennychiu.myworkoutlog.WorkoutApplication
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Resolved pointer to a prior set's video clip, produced by
 * [findMostRecentSetWithVideo]. Carries enough context for a comparison-mode
 * caption ("Mar 15, 2026 · Set 3 · Cycle 3") and to key the ExoPlayer.
 */
data class PreviousSetReference(
    val workoutId: String,
    val workoutDate: String,
    val startTimestamp: Long?,
    val userCycleName: String?,
    val exerciseId: String,
    val setId: String,
    val setNumber: Int,
    val videoRef: String,
    val videoMarks: String?
)

/**
 * Walks a date-desc list of candidate workouts (from
 * `LoggedWorkoutDao.getWorkoutsWithExerciseAndVideo`) and returns the most recent
 * prior set of [exerciseId] that has a non-blank `videoReference`, skipping the
 * workout the caller is currently viewing.
 *
 * Within the chosen workout we pick the *last* matching set (`.lastOrNull()` rather
 * than `.firstOrNull()`) — users usually film the set that mattered (top set / PR
 * attempt), which tends to land later in the session.
 */
fun findMostRecentSetWithVideo(
    workouts: List<LoggedWorkout>,
    exerciseId: String,
    excludingWorkoutId: String
): PreviousSetReference? {
    for (workout in workouts) {
        if (workout.id == excludingWorkoutId) continue
        val exercise = workout.loggedExercises.firstOrNull { it.exerciseId == exerciseId }
            ?: continue
        val match = exercise.sets.asSequence()
            .mapIndexedNotNull { idx, set ->
                val ref = set.videoReference?.takeIf { it.isNotBlank() }
                    ?: return@mapIndexedNotNull null
                Triple(set, idx + 1, ref)
            }
            .lastOrNull() ?: continue
        val (set, setNumber, ref) = match
        return PreviousSetReference(
            workoutId = workout.id,
            workoutDate = workout.date,
            startTimestamp = workout.startTimestamp,
            userCycleName = workout.userCycleName,
            exerciseId = exerciseId,
            setId = set.id,
            setNumber = setNumber,
            videoRef = ref,
            videoMarks = set.videoMarks
        )
    }
    return null
}

/**
 * Resolves the "most recent prior set with video" for an exercise, for use inside
 * `VideoPlayerSheet`'s comparison mode. Returns null until the DAO emits, and stays
 * null whenever [exerciseId] or [excludingWorkoutId] is null (e.g. attach mode).
 *
 * The flow collects with lifecycle awareness so the sheet doesn't keep running the
 * query after the caller leaves the screen.
 */
@Composable
fun rememberPreviousSetWithVideo(
    exerciseId: String?,
    excludingWorkoutId: String?
): PreviousSetReference? {
    val context = LocalContext.current
    val flow = remember(context, exerciseId, excludingWorkoutId) {
        if (exerciseId.isNullOrBlank() || excludingWorkoutId.isNullOrBlank()) {
            flowOf<PreviousSetReference?>(null)
        } else {
            val dao = (context.applicationContext as WorkoutApplication).container.loggedWorkoutDao
            dao.getWorkoutsWithExerciseAndVideo(exerciseId, excludingWorkoutId)
                .map { findMostRecentSetWithVideo(it, exerciseId, excludingWorkoutId) }
        }
    }
    val state by flow.collectAsStateWithLifecycle(initialValue = null)
    return state
}
