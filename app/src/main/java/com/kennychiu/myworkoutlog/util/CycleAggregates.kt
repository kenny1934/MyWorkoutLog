package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.LoggedSet
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import com.kennychiu.myworkoutlog.data.PersonalRecord
import com.kennychiu.myworkoutlog.data.WorkoutTemplate

data class CycleWeekAggregate(
    val weekId: String,
    val workoutCount: Int,
    val setCount: Int,
    val totalVolume: Double,
    val totalDurationMs: Long,
)

data class CyclePrHit(
    val pr: PersonalRecord,
    val weekId: String?,
)

data class CycleAggregates(
    val perWeek: Map<String, CycleWeekAggregate>,
    val prsHit: List<CyclePrHit>,
    val weightUnit: String?,
) {
    companion object {
        val EMPTY = CycleAggregates(emptyMap(), emptyList(), null)
    }
}

fun cycleAggregates(
    cycle: ActiveProgramCycle,
    workouts: List<LoggedWorkout>,
    prs: List<PersonalRecord> = emptyList(),
): CycleAggregates {
    val workoutsInCycle = workouts.filter { it.activeProgramCycleId == cycle.cycleUuid }

    val perWeek = workoutsInCycle
        .groupBy { it.programWeekDefinitionId }
        .mapNotNull { (weekId, ws) -> weekId?.let { id -> id to weekAggregate(id, ws) } }
        .toMap()

    val workoutIds = workoutsInCycle.mapTo(HashSet()) { it.id }
    val weekIdByWorkoutId = workoutsInCycle.associate { it.id to it.programWeekDefinitionId }
    val prsHit = prs
        .filter { it.loggedWorkoutId in workoutIds }
        .sortedByDescending { it.date }
        .map { CyclePrHit(it, weekIdByWorkoutId[it.loggedWorkoutId]) }

    val weightUnit = workoutsInCycle
        .mapNotNull { it.performedWeightUnit }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    return CycleAggregates(perWeek, prsHit, weightUnit)
}

// Per-exercise top set logged within each cycle week, keyed by exerciseId → 1-based
// week number. The "top set" is the heaviest-weight set in the session; ties break on
// highest reps. Sessions that only log duration-based exercises still emit a top set
// (weight = null, reps = null) so the projection helper can mark the week as logged
// even when there's no weight number to project from.
fun cycleActualsByExerciseAndWeek(
    cycle: ActiveProgramCycle,
    workouts: List<LoggedWorkout>,
): Map<String, Map<Int, ExerciseTopSet>> {
    val orderedWeeks = cycle.cycleProgram.weeks.sortedBy { it.order }
    val weekNumberByWeekId = orderedWeeks
        .withIndex()
        .associate { (index, week) -> week.id to index + 1 }
    val workoutsInCycle = workouts.filter { it.activeProgramCycleId == cycle.cycleUuid }

    val result = mutableMapOf<String, MutableMap<Int, ExerciseTopSet>>()
    for (workout in workoutsInCycle) {
        val weekNumber = workout.programWeekDefinitionId?.let(weekNumberByWeekId::get) ?: continue
        for (exercise in workout.loggedExercises) {
            val candidate = pickTopSet(exercise.sets) ?: continue
            val bucket = result.getOrPut(exercise.exerciseId) { mutableMapOf() }
            val prior = bucket[weekNumber]
            if (prior == null || topSetRank(candidate) > topSetRank(prior)) {
                bucket[weekNumber] = candidate
            }
        }
    }
    return result
}

// Pre-cycle baseline top set per exercise referenced by the cycle's session
// templates. `lookup` returns the most recent workout containing exerciseId
// before the cycle started (typically `LoggedWorkoutDao.getLatestWorkoutWithExerciseBefore`
// against `cycle.startDate`), and the helper extracts that workout's top set.
// Exercises with no pre-cycle history are omitted from the map.
fun cycleBaselinesByExercise(
    cycle: ActiveProgramCycle,
    templatesById: Map<String, WorkoutTemplate>,
    lookup: (exerciseId: String) -> LoggedWorkout?,
): Map<String, ExerciseTopSet> {
    val exerciseIds = mutableSetOf<String>()
    for (week in cycle.cycleProgram.weeks) {
        for (session in week.sessions) {
            val template = templatesById[session.workoutTemplateId] ?: continue
            for (ex in template.templateExercises) exerciseIds += ex.exerciseId
        }
    }

    val result = mutableMapOf<String, ExerciseTopSet>()
    for (exerciseId in exerciseIds) {
        val workout = lookup(exerciseId) ?: continue
        val loggedExercise = workout.loggedExercises.firstOrNull { it.exerciseId == exerciseId } ?: continue
        val top = pickTopSet(loggedExercise.sets) ?: continue
        result[exerciseId] = top
    }
    return result
}

private fun pickTopSet(sets: List<LoggedSet>): ExerciseTopSet? {
    val top = sets
        .filter { (it.weight != null && it.reps != null) || it.secs != null }
        .maxWithOrNull(
            compareBy({ it.weight ?: 0.0 }, { it.reps ?: 0 }, { it.secs ?: 0 })
        ) ?: return null
    return ExerciseTopSet(top.weight, top.reps)
}

private fun topSetRank(set: ExerciseTopSet): Double {
    // Scores weight then reps into one comparable — matches the maxWithOrNull rule
    // above so we pick the heaviest / highest-rep instance when a week has multiple
    // sessions of the same exercise.
    val w = set.weight ?: 0.0
    val r = (set.reps ?: 0).toDouble() / 1000.0
    return w + r
}

private fun weekAggregate(weekId: String, workouts: List<LoggedWorkout>): CycleWeekAggregate {
    var setCount = 0
    var volume = 0.0
    var durationMs = 0L
    for (w in workouts) {
        for (ex in w.loggedExercises) {
            for (s in ex.sets) {
                setCount += 1
                volume += (s.weight ?: 0.0) * (s.reps ?: 0)
            }
        }
        val start = w.startTimestamp
        val end = w.endTimestamp
        if (start != null && end != null && end > start) durationMs += end - start
    }
    return CycleWeekAggregate(
        weekId = weekId,
        workoutCount = workouts.size,
        setCount = setCount,
        totalVolume = volume,
        totalDurationMs = durationMs,
    )
}
