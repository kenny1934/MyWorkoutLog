package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.ProgramSessionDefinition
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class CyclePhaseTest {

    @Test
    fun `deload week classifies as Deload even when positional fallback would say Peak`() {
        val weeks = listOf(
            week("w1", 0, targetRir = null),
            week("w2", 1, targetRir = null, isDeload = true) // last index — positional = Peak
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Deload, classifyCycleWeek(weeks[1], cycle))
    }

    @Test
    fun `monotonic decreasing RIR — minimum week is Peak`() {
        val weeks = listOf(
            week("w1", 0, targetRir = "3"),
            week("w2", 1, targetRir = "2"),
            week("w3", 2, targetRir = "1")
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(weeks[2], cycle))
    }

    @Test
    fun `monotonic decreasing RIR — maximum week is Accumulation`() {
        val weeks = listOf(
            week("w1", 0, targetRir = "4"),
            week("w2", 1, targetRir = "3"),
            week("w3", 2, targetRir = "2"),
            week("w4", 3, targetRir = "1")
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Accumulation, classifyCycleWeek(weeks[0], cycle))
    }

    @Test
    fun `monotonic decreasing RIR — below midpoint but above min is Intensity`() {
        // range 4..1, midpoint 2.5; week value 2 → below midpoint, not min → Intensity
        val weeks = listOf(
            week("w1", 0, targetRir = "4"),
            week("w2", 1, targetRir = "3"),
            week("w3", 2, targetRir = "2"),
            week("w4", 3, targetRir = "1")
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Intensity, classifyCycleWeek(weeks[2], cycle))
    }

    @Test
    fun `targetRir range string parses using first integer`() {
        // "2-3" parses to 2; cycle 3,2,2-3? let's keep it simple — verify "2-3" → 2
        val weeks = listOf(
            week("w1", 0, targetRir = "3-4"),
            week("w2", 1, targetRir = "2-3"),
            week("w3", 2, targetRir = "1-2")
        )
        val cycle = cycleOf(weeks)
        // values parse to 3, 2, 1 — monotonic decreasing; last week is Peak
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(weeks[2], cycle))
        assertEquals(CycleWeekPhase.Accumulation, classifyCycleWeek(weeks[0], cycle))
    }

    @Test
    fun `flat RIR across weeks falls back to positional`() {
        val weeks = listOf(
            week("w1", 0, targetRir = "2"),
            week("w2", 1, targetRir = "2"),
            week("w3", 2, targetRir = "2")
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Baseline, classifyCycleWeek(weeks[0], cycle))
        assertEquals(CycleWeekPhase.MidBlock, classifyCycleWeek(weeks[1], cycle))
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(weeks[2], cycle))
    }

    @Test
    fun `no targetRir — positional — first week is Baseline`() {
        val weeks = listOf(
            week("w1", 0, targetRir = null),
            week("w2", 1, targetRir = null),
            week("w3", 2, targetRir = null)
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Baseline, classifyCycleWeek(weeks[0], cycle))
    }

    @Test
    fun `no targetRir — positional — last week is Peak`() {
        val weeks = listOf(
            week("w1", 0, targetRir = null),
            week("w2", 1, targetRir = null),
            week("w3", 2, targetRir = null),
            week("w4", 3, targetRir = null)
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(weeks[3], cycle))
    }

    @Test
    fun `no targetRir — positional — middle weeks are MidBlock`() {
        val weeks = listOf(
            week("w1", 0, targetRir = null),
            week("w2", 1, targetRir = null),
            week("w3", 2, targetRir = null),
            week("w4", 3, targetRir = null)
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.MidBlock, classifyCycleWeek(weeks[1], cycle))
        assertEquals(CycleWeekPhase.MidBlock, classifyCycleWeek(weeks[2], cycle))
    }

    @Test
    fun `single-week cycle is Baseline`() {
        val weeks = listOf(week("w1", 0, targetRir = null))
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Baseline, classifyCycleWeek(weeks[0], cycle))
    }

    @Test
    fun `partial targetRir coverage falls back to positional`() {
        // Only some weeks have targetRir — gradient detection requires all
        val weeks = listOf(
            week("w1", 0, targetRir = "3"),
            week("w2", 1, targetRir = null),
            week("w3", 2, targetRir = "1")
        )
        val cycle = cycleOf(weeks)
        assertEquals(CycleWeekPhase.Baseline, classifyCycleWeek(weeks[0], cycle))
        assertEquals(CycleWeekPhase.MidBlock, classifyCycleWeek(weeks[1], cycle))
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(weeks[2], cycle))
    }

    @Test
    fun `out-of-order weeks are sorted before gradient check`() {
        val w1 = week("w1", 0, targetRir = "3")
        val w2 = week("w2", 1, targetRir = "2")
        val w3 = week("w3", 2, targetRir = "1")
        // Build the program with the weeks in reversed order
        val program = ProgramTemplate(
            id = "p", name = "Test", description = null,
            weeks = listOf(w3, w2, w1)
        )
        val cycle = ActiveProgramCycle(
            cycleUuid = "c", programTemplateId = "p", programTemplateName = "Test",
            userCycleName = "Test", startDate = "2026-04-22",
            completedSessions = emptyMap(),
            cycleProgram = program
        )
        // After sorting by order, w3 is still the min value — should be Peak
        assertEquals(CycleWeekPhase.Peak, classifyCycleWeek(w3, cycle))
        assertEquals(CycleWeekPhase.Accumulation, classifyCycleWeek(w1, cycle))
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun week(
        id: String,
        order: Int,
        targetRir: String?,
        isDeload: Boolean = false
    ) = ProgramWeekDefinition(
        id = id,
        weekLabel = "Week ${order + 1}",
        order = order,
        sessions = listOf(
            ProgramSessionDefinition(id = "${id}s1", sessionName = "Day 1", workoutTemplateId = "t", order = 0)
        ),
        isDeloadWeek = isDeload,
        targetRir = targetRir
    )

    private fun cycleOf(weeks: List<ProgramWeekDefinition>): ActiveProgramCycle {
        val program = ProgramTemplate(
            id = "p", name = "Test", description = null,
            weeks = weeks
        )
        return ActiveProgramCycle(
            cycleUuid = "c", programTemplateId = "p", programTemplateName = "Test",
            userCycleName = "Test", startDate = "2026-04-22",
            completedSessions = emptyMap(),
            cycleProgram = program
        )
    }
}
