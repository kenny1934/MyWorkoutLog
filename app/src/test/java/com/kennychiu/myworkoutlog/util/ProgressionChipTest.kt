package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionChipTest {

    @Test
    fun `null scheme returns null`() {
        assertNull(
            suggestForScheme(
                scheme = null,
                lastWeight = 60.0,
                lastReps = 5,
            )
        )
    }

    @Test
    fun `NONE scheme returns null`() {
        assertNull(
            suggestForScheme(
                scheme = ProgressionScheme.NONE,
                lastWeight = 60.0,
                lastReps = 5,
            )
        )
    }

    @Test
    fun `missing last data returns null`() {
        assertNull(
            suggestForScheme(
                scheme = ProgressionScheme.LINEAR,
                lastWeight = null,
                lastReps = null,
                increment = 2.5,
            )
        )
    }

    @Test
    fun `LINEAR adds increment to last weight`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 60.0,
            lastReps = 5,
            increment = 2.5,
        )
        assertNotNull(chip)
        assertEquals(62.5, chip!!.weight!!, 0.0001)
        assertEquals(5, chip.reps)
        assertEquals("62.5kg 5r (next)", chip.label)
    }

    @Test
    fun `LINEAR integer increment drops decimal in label`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 60.0,
            lastReps = 5,
            increment = 5.0,
        )
        assertEquals("65kg 5r (next)", chip?.label)
    }

    @Test
    fun `LINEAR with null increment copies last weight`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 60.0,
            lastReps = 5,
            increment = null,
        )
        assertEquals(60.0, chip?.weight!!, 0.0001)
        assertEquals(5, chip.reps)
    }

    @Test
    fun `LINEAR respects weight unit in label`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 135.0,
            lastReps = 5,
            increment = 5.0,
            weightUnit = "lb",
        )
        assertEquals("140lb 5r (next)", chip?.label)
    }

    @Test
    fun `DOUBLE under max reps bumps reps only`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 60.0,
            lastReps = 9,
            minReps = 8,
            maxReps = 12,
        )
        assertEquals(60.0, chip?.weight!!, 0.0001)
        assertEquals(10, chip.reps)
        assertEquals("60kg 10r (next)", chip.label)
    }

    @Test
    fun `DOUBLE at max reps bumps weight and resets to min`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 60.0,
            lastReps = 12,
            minReps = 8,
            maxReps = 12,
            increment = 2.5,
        )
        assertEquals(62.5, chip?.weight!!, 0.0001)
        assertEquals(8, chip.reps)
        assertEquals("62.5kg 8r (next)", chip.label)
    }

    @Test
    fun `DOUBLE at max without increment defaults to 2_5`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 80.0,
            lastReps = 10,
            minReps = 6,
            maxReps = 10,
            increment = null,
        )
        assertEquals(82.5, chip?.weight!!, 0.0001)
        assertEquals(6, chip.reps)
    }

    @Test
    fun `DOUBLE without maxReps climbs reps indefinitely`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 60.0,
            lastReps = 20,
            minReps = null,
            maxReps = null,
        )
        assertEquals(21, chip?.reps)
    }

    @Test
    fun `DOUBLE without lastReps returns null`() {
        assertNull(
            suggestForScheme(
                scheme = ProgressionScheme.DOUBLE,
                lastWeight = 60.0,
                lastReps = null,
                minReps = 8,
                maxReps = 12,
            )
        )
    }

    @Test
    fun `RPE with numeric target derives RIR and tags label`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.RPE,
            lastWeight = 70.0,
            lastReps = 6,
            targetRpe = "8",
        )
        assertEquals(70.0, chip?.weight!!, 0.0001)
        assertEquals(6, chip.reps)
        assertEquals(2, chip.rir)
        assertEquals("70kg 6r @ RPE 8", chip.label)
    }

    @Test
    fun `RPE range uses lower bound for RIR`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.RPE,
            lastWeight = 70.0,
            lastReps = 6,
            targetRpe = "7-8",
        )
        assertEquals(3, chip?.rir)
        assertEquals("70kg 6r @ RPE 7-8", chip?.label)
    }

    @Test
    fun `RPE with blank target falls back to maintain label`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.RPE,
            lastWeight = 70.0,
            lastReps = 6,
            lastRir = 2,
            targetRpe = "",
        )
        assertEquals(70.0, chip?.weight!!, 0.0001)
        assertEquals(6, chip.reps)
        assertEquals(2, chip.rir)
        assertEquals("70kg 6r", chip.label)
    }

    @Test
    fun `TOP_SET first set bumps weight`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.TOP_SET,
            setNumber = 1,
            lastWeight = 100.0,
            lastReps = 5,
            increment = 2.5,
        )
        assertEquals(102.5, chip?.weight!!, 0.0001)
        assertEquals(5, chip.reps)
        assertEquals("102.5kg 5r (top)", chip.label)
    }

    @Test
    fun `TOP_SET backoff sets copy last`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.TOP_SET,
            setNumber = 3,
            lastWeight = 100.0,
            lastReps = 5,
            increment = 2.5,
        )
        assertEquals(100.0, chip?.weight!!, 0.0001)
        assertEquals(5, chip.reps)
        assertEquals("100kg 5r (backoff)", chip.label)
    }

    @Test
    fun `TOP_SET first set without increment defaults to 2_5`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.TOP_SET,
            setNumber = 1,
            lastWeight = 100.0,
            lastReps = 5,
            increment = null,
        )
        assertEquals(102.5, chip?.weight!!, 0.0001)
    }

    @Test
    fun `LINEAR baseline week holds last weight`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 60.0,
            lastReps = 5,
            increment = 2.5,
            cycleWeekNumber = 1,
        )
        assertEquals(60.0, chip?.weight!!, 0.0001)
        assertEquals(5, chip.reps)
        assertEquals("60kg 5r (baseline)", chip.label)
    }

    @Test
    fun `LINEAR week 2 resumes full increment`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.LINEAR,
            lastWeight = 60.0,
            lastReps = 5,
            increment = 2.5,
            cycleWeekNumber = 2,
        )
        assertEquals(62.5, chip?.weight!!, 0.0001)
        assertEquals("62.5kg 5r (next)", chip.label)
    }

    @Test
    fun `DOUBLE at max reps holds weight on baseline week`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 60.0,
            lastReps = 12,
            minReps = 8,
            maxReps = 12,
            increment = 2.5,
            cycleWeekNumber = 1,
        )
        assertEquals(60.0, chip?.weight!!, 0.0001)
        assertEquals(12, chip.reps)
        assertEquals("60kg 12r (baseline)", chip.label)
    }

    @Test
    fun `DOUBLE under max reps climbs even on baseline week`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.DOUBLE,
            lastWeight = 60.0,
            lastReps = 9,
            minReps = 8,
            maxReps = 12,
            cycleWeekNumber = 1,
        )
        assertEquals(60.0, chip?.weight!!, 0.0001)
        assertEquals(10, chip.reps)
    }

    @Test
    fun `TOP_SET baseline week holds top-set weight`() {
        val chip = suggestForScheme(
            scheme = ProgressionScheme.TOP_SET,
            setNumber = 1,
            lastWeight = 100.0,
            lastReps = 5,
            increment = 2.5,
            cycleWeekNumber = 1,
        )
        assertEquals(100.0, chip?.weight!!, 0.0001)
        assertEquals("100kg 5r (top · baseline)", chip.label)
    }
}
