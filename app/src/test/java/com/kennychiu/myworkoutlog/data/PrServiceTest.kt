package com.kennychiu.myworkoutlog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PrServiceTest {

    @Test
    fun `emits MAX_WEIGHT_FOR_REPS and MAX_REPS_AT_WEIGHT for a weighted set`() {
        val w = workout(sets = listOf(set(reps = 5, weight = 100.0)))

        val new = PrService.detectNewPRs(w, existingPRs = emptyList(), allMasterExercises = listOf(exercise()))

        assertEquals(2, new.size)
        val maxWeight = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        val maxReps = new.single { it.type == PRType.MAX_REPS_AT_WEIGHT }
        assertEquals(5, maxWeight.reps)
        assertEquals(100.0, maxWeight.weight!!, 0.0)
        assertEquals(5, maxReps.reps)
        assertEquals(100.0, maxReps.weight!!, 0.0)
    }

    @Test
    fun `emits DURATION PR for a timed set`() {
        val w = workout(sets = listOf(set(secs = 45)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        assertEquals(1, new.size)
        assertEquals(PRType.DURATION, new.single().type)
        assertEquals(45, new.single().durationSecs)
        assertNull(new.single().weight)
    }

    @Test
    fun `ignores sets with zero reps`() {
        val w = workout(sets = listOf(set(reps = 0, weight = 80.0)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        assertTrue(new.isEmpty())
    }

    @Test
    fun `ignores sets with null reps even when weight is set`() {
        val w = workout(sets = listOf(set(reps = null, weight = 80.0)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        assertTrue(new.isEmpty())
    }

    @Test
    fun `ignores sets with null weight when reps are set`() {
        val w = workout(sets = listOf(set(reps = 5, weight = null)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        assertTrue(new.isEmpty())
    }

    @Test
    fun `skips DURATION when secs is zero or null`() {
        val w = workout(
            sets = listOf(
                set(secs = 0),
                set(secs = null),
            )
        )

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        assertTrue(new.isEmpty())
    }

    @Test
    fun `keeps heaviest weight when multiple sets share the same rep count`() {
        val w = workout(
            sets = listOf(
                set(reps = 5, weight = 80.0),
                set(reps = 5, weight = 90.0),
                set(reps = 5, weight = 70.0),
            )
        )

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        val maxWeight = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        assertEquals(90.0, maxWeight.weight!!, 0.0)
    }

    @Test
    fun `keeps most reps at the same effective weight`() {
        val w = workout(
            sets = listOf(
                set(reps = 5, weight = 80.0),
                set(reps = 8, weight = 80.0),
                set(reps = 6, weight = 80.0),
            )
        )

        val new = PrService.detectNewPRs(w, emptyList(), listOf(exercise()))

        val maxReps = new.single { it.type == PRType.MAX_REPS_AT_WEIGHT }
        assertEquals(8, maxReps.reps)
    }

    @Test
    fun `returns no MAX_WEIGHT_FOR_REPS when existing record already beats the new set`() {
        val existing = listOf(
            pr(
                id = "max_weight_for_reps_ex-1_5",
                type = PRType.MAX_WEIGHT_FOR_REPS,
                reps = 5,
                weight = 100.0,
                weightUnit = "kg",
                loggedWorkoutId = "old-workout",
            )
        )
        val w = workout(sets = listOf(set(reps = 5, weight = 80.0)))

        val new = PrService.detectNewPRs(w, existing, listOf(exercise()))

        assertFalse(new.any { it.type == PRType.MAX_WEIGHT_FOR_REPS })
    }

    @Test
    fun `compares weights across units by converting both to kilograms`() {
        // Existing 200 lb (~90.72 kg) beats new 90 kg at reps=5.
        val existing = listOf(
            pr(
                id = "max_weight_for_reps_ex-1_5",
                type = PRType.MAX_WEIGHT_FOR_REPS,
                reps = 5,
                weight = 200.0,
                weightUnit = "lb",
                loggedWorkoutId = "old-workout",
            )
        )
        val w = workout(unit = "kg", sets = listOf(set(reps = 5, weight = 90.0)))

        val new = PrService.detectNewPRs(w, existing, listOf(exercise()))

        assertFalse(new.any { it.type == PRType.MAX_WEIGHT_FOR_REPS })
    }

    @Test
    fun `ties on MAX_WEIGHT_FOR_REPS promote the current workout to the record`() {
        val existing = listOf(
            pr(
                id = "max_weight_for_reps_ex-1_5",
                type = PRType.MAX_WEIGHT_FOR_REPS,
                reps = 5,
                weight = 100.0,
                weightUnit = "kg",
                loggedWorkoutId = "old-workout",
            )
        )
        val w = workout(id = "new-workout", unit = "kg", sets = listOf(set(reps = 5, weight = 100.0)))

        val new = PrService.detectNewPRs(w, existing, listOf(exercise()))

        val match = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        assertEquals("new-workout", match.loggedWorkoutId)
        assertEquals(100.0, match.weight!!, 0.0)
    }

    @Test
    fun `bodyweight exercise adds user bodyweight to effective weight`() {
        val ex = exercise(usesBodyweight = true)
        val w = workout(bodyweight = 80.0, sets = listOf(set(reps = 5, weight = 20.0)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(ex))

        val maxWeight = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        assertEquals(100.0, maxWeight.weight!!, 0.0)
        assertEquals(80.0, maxWeight.bodyweightUsed!!, 0.0)
        assertEquals(20.0, maxWeight.externalWeight!!, 0.0)
        assertTrue(maxWeight.usesBodyweight)
    }

    @Test
    fun `bodyweight exercise with null workout bodyweight treats it as zero`() {
        val ex = exercise(usesBodyweight = true)
        val w = workout(bodyweight = null, sets = listOf(set(reps = 5, weight = 20.0)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(ex))

        val maxWeight = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        assertEquals(20.0, maxWeight.weight!!, 0.0)
        assertEquals(0.0, maxWeight.bodyweightUsed!!, 0.0)
        assertEquals(20.0, maxWeight.externalWeight!!, 0.0)
    }

    @Test
    fun `exercise missing from master list is treated as non-bodyweight`() {
        val w = workout(bodyweight = 80.0, sets = listOf(set(reps = 5, weight = 100.0)))

        val new = PrService.detectNewPRs(w, emptyList(), allMasterExercises = emptyList())

        val maxWeight = new.single { it.type == PRType.MAX_WEIGHT_FOR_REPS }
        assertEquals(100.0, maxWeight.weight!!, 0.0)
        assertFalse(maxWeight.usesBodyweight)
        assertNull(maxWeight.bodyweightUsed)
    }

    @Test
    fun `duration PR captures externalWeight and bodyweight when timed set has weight`() {
        val ex = exercise(usesBodyweight = true)
        val w = workout(bodyweight = 80.0, sets = listOf(set(secs = 60, weight = 20.0)))

        val new = PrService.detectNewPRs(w, emptyList(), listOf(ex))

        val duration = new.single { it.type == PRType.DURATION }
        assertEquals(60, duration.durationSecs)
        assertEquals(20.0, duration.externalWeight!!, 0.0)
        assertEquals(80.0, duration.bodyweightUsed!!, 0.0)
        assertTrue(duration.usesBodyweight)
    }

    @Test
    fun `returns PRs for each exercise independently`() {
        val squat = exercise(id = "sq-1", name = "Squat")
        val bench = exercise(id = "bn-1", name = "Bench")
        val w = workout(
            loggedExercises = listOf(
                loggedExercise(exerciseId = "sq-1", name = "Squat", sets = listOf(set(reps = 5, weight = 120.0))),
                loggedExercise(exerciseId = "bn-1", name = "Bench", sets = listOf(set(reps = 8, weight = 60.0))),
            )
        )

        val new = PrService.detectNewPRs(w, emptyList(), listOf(squat, bench))

        assertEquals(4, new.size)
        assertTrue(new.any { it.exerciseId == "sq-1" && it.type == PRType.MAX_WEIGHT_FOR_REPS })
        assertTrue(new.any { it.exerciseId == "sq-1" && it.type == PRType.MAX_REPS_AT_WEIGHT })
        assertTrue(new.any { it.exerciseId == "bn-1" && it.type == PRType.MAX_WEIGHT_FOR_REPS })
        assertTrue(new.any { it.exerciseId == "bn-1" && it.type == PRType.MAX_REPS_AT_WEIGHT })
    }

    // -- fixtures --

    private fun exercise(
        id: String = "ex-1",
        name: String = "Squat",
        usesBodyweight: Boolean = false,
    ) = Exercise(
        id = id,
        name = name,
        usesBodyweight = usesBodyweight,
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
    )

    private fun loggedExercise(
        exerciseId: String = "ex-1",
        name: String = "Squat",
        sets: List<LoggedSet>,
    ) = LoggedExercise(
        id = "le-$exerciseId",
        exerciseId = exerciseId,
        exerciseName = name,
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
        sets = sets,
    )

    private fun set(
        reps: Int? = null,
        weight: Double? = null,
        secs: Int? = null,
    ) = LoggedSet(
        id = UUID.randomUUID().toString(),
        reps = reps,
        weight = weight,
        secs = secs,
    )

    private fun workout(
        id: String = "new-workout",
        unit: String? = "kg",
        bodyweight: Double? = null,
        sets: List<LoggedSet> = emptyList(),
        loggedExercises: List<LoggedExercise>? = null,
    ) = LoggedWorkout(
        id = id,
        date = "2026-04-22",
        name = null,
        overallComments = null,
        startTimestamp = null,
        endTimestamp = null,
        bodyweight = bodyweight,
        performedWeightUnit = unit,
        activeProgramCycleId = null,
        programWeekDefinitionId = null,
        programSessionDefinitionId = null,
        userCycleName = null,
        loggedExercises = loggedExercises ?: listOf(loggedExercise(sets = sets)),
        workoutTemplateId = null,
        isInProgress = false,
    )

    private fun pr(
        id: String,
        type: PRType,
        reps: Int? = null,
        weight: Double? = null,
        durationSecs: Int? = null,
        weightUnit: String? = "kg",
        loggedWorkoutId: String = "old-workout",
    ) = PersonalRecord(
        id = id,
        exerciseId = "ex-1",
        exerciseName = "Squat",
        date = "2026-04-01",
        loggedWorkoutId = loggedWorkoutId,
        type = type,
        weightUnit = weightUnit,
        reps = reps,
        weight = weight,
        durationSecs = durationSecs,
    )
}
