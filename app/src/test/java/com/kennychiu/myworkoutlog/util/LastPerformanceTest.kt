package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.LoggedExercise
import com.kennychiu.myworkoutlog.data.LoggedSet
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class LastPerformanceTest {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Test
    fun `weight and reps summary picks heaviest set, shows set count and unit`() {
        val ex = exercise(
            listOf(set(reps = 8, weight = 60.0), set(reps = 6, weight = 70.0), set(reps = 5, weight = 65.0))
        )
        val w = workout(date = "2026-04-14", unit = "kg", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("3 × 6 @ 70kg (5d ago)", s)
    }

    @Test
    fun `integer weights render without decimal`() {
        val ex = exercise(listOf(set(reps = 5, weight = 100.0)))
        val w = workout(date = "2026-04-19", unit = "kg", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("1 × 5 @ 100kg (today)", s)
    }

    @Test
    fun `fractional weights render as decimal`() {
        val ex = exercise(listOf(set(reps = 5, weight = 22.5)))
        val w = workout(date = "2026-04-18", unit = "lb", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("1 × 5 @ 22.5lb (yesterday)", s)
    }

    @Test
    fun `time based summary when no weight sets`() {
        val ex = exercise(listOf(set(secs = 30), set(secs = 45), set(secs = 40)))
        val w = workout(date = "2026-04-16", unit = null, ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("3 × 45s (3d ago)", s)
    }

    @Test
    fun `null unit falls back to kg`() {
        val ex = exercise(listOf(set(reps = 10, weight = 40.0)))
        val w = workout(date = "2026-04-19", unit = null, ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("1 × 10 @ 40kg (today)", s)
    }

    @Test
    fun `returns null when no completed sets`() {
        val ex = exercise(listOf(set(reps = null, weight = null), set(reps = 0, weight = 60.0)))
        val w = workout(date = "2026-04-19", unit = "kg", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertNull(s)
    }

    @Test
    fun `ignores sets with weight but zero reps`() {
        val ex = exercise(
            listOf(set(reps = 0, weight = 80.0), set(reps = 5, weight = 60.0))
        )
        val w = workout(date = "2026-04-19", unit = "kg", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("1 × 5 @ 60kg (today)", s)
    }

    @Test
    fun `unparseable date omits days-ago suffix`() {
        val ex = exercise(listOf(set(reps = 5, weight = 60.0)))
        val w = workout(date = "bogus", unit = "kg", ex = ex)

        val s = summarizeLastPerformance(w, ex, today = dateFmt.parse("2026-04-19")!!)

        assertEquals("1 × 5 @ 60kg", s)
    }

    private fun exercise(sets: List<LoggedSet>) = LoggedExercise(
        id = "ex-id",
        exerciseId = "ex-1",
        exerciseName = "Squat",
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
        sets = sets,
    )

    private fun set(reps: Int? = null, weight: Double? = null, secs: Int? = null) = LoggedSet(
        id = java.util.UUID.randomUUID().toString(),
        reps = reps,
        weight = weight,
        secs = secs,
    )

    private fun workout(date: String, unit: String?, ex: LoggedExercise) = LoggedWorkout(
        id = "w",
        date = date,
        name = null,
        overallComments = null,
        startTimestamp = null,
        endTimestamp = null,
        bodyweight = null,
        performedWeightUnit = unit,
        activeProgramCycleId = null,
        programWeekDefinitionId = null,
        programSessionDefinitionId = null,
        userCycleName = null,
        loggedExercises = listOf(ex),
        workoutTemplateId = null,
        isInProgress = false,
    )
}
