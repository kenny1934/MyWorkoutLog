package com.example.myworkoutlog

// --- IMPORTS ---
import java.util.UUID

// This object acts as a dedicated service for handling PR logic.
object PrService {

    // This function takes a completed workout and a list of existing PRs,
    // and returns a list of new or updated PRs.
    fun detectNewPRs(workout: LoggedWorkout, existingPRs: List<PersonalRecord>): List<PersonalRecord> {
        val potentialPRs = mutableListOf<PersonalRecord>()

        workout.loggedExercises.forEach { loggedEx ->
            loggedEx.sets.forEach { set ->
                // Check for Max Weight for Reps PR
                if (set.reps != null && set.weight != null && set.reps > 0 && set.weight > 0) {
                    potentialPRs.add(
                        PersonalRecord(
                            id = "max_weight_for_reps_${loggedEx.exerciseId}_${set.reps}",
                            exerciseId = loggedEx.exerciseId,
                            exerciseName = loggedEx.exerciseName,
                            date = workout.date,
                            type = PRType.MAX_WEIGHT_FOR_REPS,
                            reps = set.reps,
                            weight = set.weight,
                            durationSecs = null
                        )
                    )
                }

                // Check for Max Reps at Weight PR
                if (set.reps != null && set.weight != null && set.reps > 0 && set.weight > 0) {
                    potentialPRs.add(
                        PersonalRecord(
                            id = "max_reps_at_weight_${loggedEx.exerciseId}_${set.weight}",
                            exerciseId = loggedEx.exerciseId,
                            exerciseName = loggedEx.exerciseName,
                            date = workout.date,
                            type = PRType.MAX_REPS_AT_WEIGHT,
                            reps = set.reps,
                            weight = set.weight,
                            durationSecs = null
                        )
                    )
                }

                // Check for Duration PR
                if (set.secs != null && set.secs > 0) {
                    potentialPRs.add(
                        PersonalRecord(
                            id = "duration_${loggedEx.exerciseId}",
                            exerciseId = loggedEx.exerciseId,
                            exerciseName = loggedEx.exerciseName,
                            date = workout.date,
                            type = PRType.DURATION,
                            reps = null,
                            weight = null,
                            durationSecs = set.secs
                        )
                    )
                }
            }
        }

        val allPRs = existingPRs + potentialPRs
        val bestPRs = mutableMapOf<String, PersonalRecord>()

        allPRs.forEach { pr ->
            val existingBest = bestPRs[pr.id]
            if (existingBest == null) {
                bestPRs[pr.id] = pr
            } else {
                when (pr.type) {
                    PRType.MAX_WEIGHT_FOR_REPS -> {
                        if ((pr.weight ?: 0.0) > (existingBest.weight ?: 0.0)) {
                            bestPRs[pr.id] = pr
                        }
                    }
                    PRType.MAX_REPS_AT_WEIGHT -> {
                        if ((pr.reps ?: 0) > (existingBest.reps ?: 0)) {
                            bestPRs[pr.id] = pr
                        }
                    }
                    PRType.DURATION -> {
                        if ((pr.durationSecs ?: 0) > (existingBest.durationSecs ?: 0)) {
                            bestPRs[pr.id] = pr
                        }
                    }
                }
            }
        }

        // Return only the PRs that were newly set in this workout.
        return bestPRs.values.filter { it.date == workout.date }
    }
}