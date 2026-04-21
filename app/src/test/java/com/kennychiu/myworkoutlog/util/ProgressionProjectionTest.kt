package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionProjectionTest {

    @Test
    fun `zero weeks returns empty list`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 0,
            baseline = ExerciseTopSet(100.0, 5),
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `LINEAR — week 1 is baseline, week N adds increment N-1 times`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 4,
            baseline = ExerciseTopSet(100.0, 5),
        )
        assertEquals(4, rows.size)
        assertEquals(100.0, rows[0].weight!!, 0.001)
        assertEquals(102.5, rows[1].weight!!, 0.001)
        assertEquals(105.0, rows[2].weight!!, 0.001)
        assertEquals(107.5, rows[3].weight!!, 0.001)
        rows.forEach { assertEquals(5, it.reps) }
        rows.forEach { assertFalse(it.isActual) }
    }

    @Test
    fun `LINEAR — missing increment holds baseline weight`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = null,
            cycleWeekCount = 3,
            baseline = ExerciseTopSet(100.0, 5),
        )
        rows.forEach { assertEquals(100.0, it.weight!!, 0.001) }
    }

    @Test
    fun `LINEAR — actual at week 2 re-baselines subsequent weeks`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 4,
            baseline = ExerciseTopSet(100.0, 5),
            actualsByWeek = mapOf(2 to ExerciseTopSet(100.0, 5)),
        )
        // Week 1 = baseline plan (100)
        // Week 2 = actual (100)
        // Week 3 = actual(w2) + 1*inc = 102.5
        // Week 4 = actual(w2) + 2*inc = 105
        assertEquals(100.0, rows[0].weight!!, 0.001)
        assertEquals(100.0, rows[1].weight!!, 0.001)
        assertTrue(rows[1].isActual)
        assertEquals(102.5, rows[2].weight!!, 0.001)
        assertFalse(rows[2].isActual)
        assertEquals(105.0, rows[3].weight!!, 0.001)
    }

    @Test
    fun `DOUBLE — climbs reps within range, then bumps weight and resets`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.DOUBLE,
            increment = 5.0,
            minReps = 8,
            maxReps = 12,
            cycleWeekCount = 6,
            baseline = ExerciseTopSet(100.0, 10),
        )
        // Anchor at baseline = (100, 10).
        // W1 = (100, 10)  -- offset 0
        // W2 = (100, 11)  -- climbing
        // W3 = (100, 12)  -- still climbing, reached max
        // W4 = (105, 8)   -- at max → bump weight, reset reps
        // W5 = (105, 9)
        // W6 = (105, 10)
        assertEquals(100.0 to 10, rows[0].weight to rows[0].reps)
        assertEquals(100.0 to 11, rows[1].weight to rows[1].reps)
        assertEquals(100.0 to 12, rows[2].weight to rows[2].reps)
        assertEquals(105.0 to 8, rows[3].weight to rows[3].reps)
        assertEquals(105.0 to 9, rows[4].weight to rows[4].reps)
        assertEquals(105.0 to 10, rows[5].weight to rows[5].reps)
    }

    @Test
    fun `DOUBLE — no maxReps keeps climbing reps indefinitely`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.DOUBLE,
            increment = 2.5,
            minReps = 5,
            maxReps = null,
            cycleWeekCount = 4,
            baseline = ExerciseTopSet(80.0, 5),
        )
        assertEquals(5, rows[0].reps)
        assertEquals(6, rows[1].reps)
        assertEquals(7, rows[2].reps)
        assertEquals(8, rows[3].reps)
        rows.forEach { assertEquals(80.0, it.weight!!, 0.001) }
    }

    @Test
    fun `TOP_SET — default small-plate bump when no increment set`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.TOP_SET,
            increment = null,
            cycleWeekCount = 3,
            baseline = ExerciseTopSet(80.0, 5),
        )
        assertEquals(80.0, rows[0].weight!!, 0.001)
        assertEquals(82.5, rows[1].weight!!, 0.001)
        assertEquals(85.0, rows[2].weight!!, 0.001)
    }

    @Test
    fun `RPE — target-only label, no weight or reps projected`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.RPE,
            targetRpe = "8",
            cycleWeekCount = 3,
            baseline = ExerciseTopSet(100.0, 5),
        )
        rows.forEach {
            assertEquals("Target RPE 8", it.label)
            assertNull(it.weight)
            assertNull(it.reps)
        }
    }

    @Test
    fun `RPE — no target shows bare label`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.RPE,
            targetRpe = null,
            cycleWeekCount = 2,
            baseline = ExerciseTopSet(100.0, 5),
        )
        rows.forEach { assertEquals("RPE", it.label) }
    }

    @Test
    fun `NONE — freeform label, no numbers`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.NONE,
            cycleWeekCount = 2,
            baseline = ExerciseTopSet(100.0, 5),
        )
        rows.forEach {
            assertEquals("Freeform", it.label)
            assertNull(it.weight)
            assertNull(it.reps)
        }
    }

    @Test
    fun `null scheme — freeform label`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = null,
            cycleWeekCount = 2,
        )
        rows.forEach { assertEquals("Freeform", it.label) }
    }

    @Test
    fun `no baseline + no actuals — em-dash label for numeric schemes`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 2,
            baseline = null,
        )
        rows.forEach { assertEquals("—", it.label) }
    }

    @Test
    fun `actual present at week 1 overrides baseline as anchor`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 3,
            baseline = ExerciseTopSet(100.0, 5),
            actualsByWeek = mapOf(1 to ExerciseTopSet(110.0, 5)),
        )
        assertTrue(rows[0].isActual)
        assertEquals(110.0, rows[0].weight!!, 0.001)
        assertEquals(112.5, rows[1].weight!!, 0.001)
        assertEquals(115.0, rows[2].weight!!, 0.001)
    }

    @Test
    fun `label formats integer weights without decimal`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 5.0,
            cycleWeekCount = 2,
            baseline = ExerciseTopSet(100.0, 8),
        )
        assertEquals("100kg × 8", rows[0].label)
        assertEquals("105kg × 8", rows[1].label)
    }

    @Test
    fun `label formats half-plate weights with decimal`() {
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 2,
            baseline = ExerciseTopSet(100.0, 5),
        )
        assertEquals("102.5kg × 5", rows[1].label)
    }

    @Test
    fun `null actual for a week does not anchor on it`() {
        // A week in actualsByWeek mapped to null should not count as an anchor.
        val rows = projectExerciseAcrossWeeks(
            scheme = ProgressionScheme.LINEAR,
            increment = 2.5,
            cycleWeekCount = 3,
            baseline = ExerciseTopSet(100.0, 5),
            actualsByWeek = mapOf(2 to null),
        )
        // Week 2 should NOT emit actual row; should still project from baseline.
        rows.forEach { assertFalse(it.isActual) }
        assertEquals(100.0, rows[0].weight!!, 0.001)
        assertEquals(102.5, rows[1].weight!!, 0.001)
        assertEquals(105.0, rows[2].weight!!, 0.001)
    }
}
