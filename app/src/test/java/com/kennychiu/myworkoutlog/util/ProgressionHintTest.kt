package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgressionScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionHintTest {

    @Test
    fun `null scheme returns null`() {
        assertNull(formatProgressionHint(scheme = null))
    }

    @Test
    fun `NONE scheme returns null`() {
        assertNull(formatProgressionHint(scheme = ProgressionScheme.NONE))
    }

    @Test
    fun `LINEAR with integer increment drops decimal`() {
        assertEquals(
            "Linear +5kg/wk",
            formatProgressionHint(scheme = ProgressionScheme.LINEAR, increment = 5.0),
        )
    }

    @Test
    fun `LINEAR with fractional increment keeps decimal`() {
        assertEquals(
            "Linear +2.5kg/wk",
            formatProgressionHint(scheme = ProgressionScheme.LINEAR, increment = 2.5),
        )
    }

    @Test
    fun `LINEAR with no increment falls back to bare label`() {
        assertEquals(
            "Linear",
            formatProgressionHint(scheme = ProgressionScheme.LINEAR, increment = null),
        )
    }

    @Test
    fun `LINEAR honors non-default weight unit`() {
        assertEquals(
            "Linear +5lb/wk",
            formatProgressionHint(scheme = ProgressionScheme.LINEAR, increment = 5.0, weightUnit = "lb"),
        )
    }

    @Test
    fun `LINEAR ignores zero or negative increment`() {
        assertEquals(
            "Linear",
            formatProgressionHint(scheme = ProgressionScheme.LINEAR, increment = 0.0),
        )
    }

    @Test
    fun `DOUBLE with both min and max renders range`() {
        assertEquals(
            "Double 8–12 reps",
            formatProgressionHint(scheme = ProgressionScheme.DOUBLE, minReps = 8, maxReps = 12),
        )
    }

    @Test
    fun `DOUBLE with only min renders floor`() {
        assertEquals(
            "Double ≥8 reps",
            formatProgressionHint(scheme = ProgressionScheme.DOUBLE, minReps = 8),
        )
    }

    @Test
    fun `DOUBLE with only max renders ceiling`() {
        assertEquals(
            "Double ≤12 reps",
            formatProgressionHint(scheme = ProgressionScheme.DOUBLE, maxReps = 12),
        )
    }

    @Test
    fun `DOUBLE with no reps falls back to bare label`() {
        assertEquals(
            "Double progression",
            formatProgressionHint(scheme = ProgressionScheme.DOUBLE),
        )
    }

    @Test
    fun `RPE with target renders number`() {
        assertEquals(
            "RPE 8",
            formatProgressionHint(scheme = ProgressionScheme.RPE, targetRpe = "8"),
        )
    }

    @Test
    fun `RPE with range renders range`() {
        assertEquals(
            "RPE 7-8",
            formatProgressionHint(scheme = ProgressionScheme.RPE, targetRpe = "7-8"),
        )
    }

    @Test
    fun `RPE with blank target renders bare label`() {
        assertEquals(
            "RPE",
            formatProgressionHint(scheme = ProgressionScheme.RPE, targetRpe = "   "),
        )
    }

    @Test
    fun `TOP_SET renders fixed label`() {
        assertEquals(
            "Top set + backoffs",
            formatProgressionHint(scheme = ProgressionScheme.TOP_SET),
        )
    }
}
