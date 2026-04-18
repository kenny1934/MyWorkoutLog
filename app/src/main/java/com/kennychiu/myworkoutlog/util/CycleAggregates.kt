package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import com.kennychiu.myworkoutlog.data.PersonalRecord

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
