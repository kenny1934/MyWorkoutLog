package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.ProgramSessionDefinition
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleProgressTest {

    @Test
    fun `fresh cycle — current week is week 0, next session is session 0`() {
        val cycle = cycle(completed = emptyMap())
        val p = cycleProgress(cycle)

        assertEquals(0, p.currentWeekIndex)
        assertEquals("w1", p.currentWeek?.id)
        assertEquals("w1s1", p.nextSession?.id)
        assertEquals(0, p.completedSessionCount)
        assertEquals(4, p.totalSessionCount)
        assertFalse(p.isComplete)
    }

    @Test
    fun `one session done — next session skips over it`() {
        val cycle = cycle(completed = mapOf("w1_w1s1" to "workout-a"))
        val p = cycleProgress(cycle)

        assertEquals(0, p.currentWeekIndex)
        assertEquals("w1s2", p.nextSession?.id)
        assertEquals(1, p.completedSessionCount)
    }

    @Test
    fun `first week fully done — advances to week 1`() {
        val cycle = cycle(completed = mapOf(
            "w1_w1s1" to "a",
            "w1_w1s2" to "b"
        ))
        val p = cycleProgress(cycle)

        assertEquals(1, p.currentWeekIndex)
        assertEquals("w2", p.currentWeek?.id)
        assertEquals("w2s1", p.nextSession?.id)
        assertEquals(2, p.completedSessionCount)
    }

    @Test
    fun `all sessions done — isComplete true, no current week`() {
        val cycle = cycle(completed = mapOf(
            "w1_w1s1" to "a", "w1_w1s2" to "b",
            "w2_w2s1" to "c", "w2_w2s2" to "d"
        ))
        val p = cycleProgress(cycle)

        assertNull(p.currentWeek)
        assertNull(p.currentWeekIndex)
        assertNull(p.nextSession)
        assertEquals(4, p.completedSessionCount)
        assertTrue(p.isComplete)
    }

    @Test
    fun `out-of-order weeks and sessions are sorted before scanning`() {
        val week1 = ProgramWeekDefinition(
            id = "w1", weekLabel = "Week 1", order = 0,
            sessions = listOf(
                ProgramSessionDefinition(id = "w1s2", sessionName = "B", workoutTemplateId = "t", order = 1),
                ProgramSessionDefinition(id = "w1s1", sessionName = "A", workoutTemplateId = "t", order = 0)
            )
        )
        val week2 = ProgramWeekDefinition(
            id = "w2", weekLabel = "Week 2", order = 1,
            sessions = listOf(
                ProgramSessionDefinition(id = "w2s1", sessionName = "A", workoutTemplateId = "t", order = 0)
            )
        )
        val program = ProgramTemplate(
            id = "p", name = "Prog", description = null,
            weeks = listOf(week2, week1) // reversed on purpose
        )
        val cycle = ActiveProgramCycle(
            cycleUuid = "c", programTemplateId = "p", programTemplateName = "Prog",
            userCycleName = "Test", startDate = "2026-04-18",
            completedSessions = mapOf("w1_w1s1" to "a"),
            cycleProgram = program
        )

        val p = cycleProgress(cycle)

        assertEquals(0, p.currentWeekIndex)
        assertEquals("w1", p.currentWeek?.id)
        assertEquals("w1s2", p.nextSession?.id) // session with order=1, since s1 is done
    }

    @Test
    fun `planned end date is start plus weeks times 7`() {
        val cycle = cycle(completed = emptyMap(), startDate = "2026-04-01")
        val p = cycleProgress(cycle)

        assertEquals(LocalDate.of(2026, 4, 1), p.startDate)
        // 2 weeks → end is start + 14 days
        assertEquals(LocalDate.of(2026, 4, 15), p.plannedEndDate)
    }

    @Test
    fun `malformed start date yields null dates but rest of progress is valid`() {
        val cycle = cycle(completed = emptyMap(), startDate = "not-a-date")
        val p = cycleProgress(cycle)

        assertNull(p.startDate)
        assertNull(p.plannedEndDate)
        assertEquals(4, p.totalSessionCount)
        assertEquals(0, p.completedSessionCount)
    }

    @Test
    fun `empty program — totalSessionCount is 0 and isComplete is false`() {
        val program = ProgramTemplate(id = "p", name = "Empty", description = null, weeks = emptyList())
        val cycle = ActiveProgramCycle(
            cycleUuid = "c", programTemplateId = "p", programTemplateName = "Empty",
            userCycleName = "Test", startDate = "2026-04-18",
            completedSessions = emptyMap(),
            cycleProgram = program
        )

        val p = cycleProgress(cycle)

        assertNull(p.currentWeek)
        assertEquals(0, p.totalSessionCount)
        assertFalse(p.isComplete) // no sessions → "done" is meaningless
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun cycle(
        completed: Map<String, String>,
        startDate: String = "2026-04-18"
    ): ActiveProgramCycle {
        val week1 = ProgramWeekDefinition(
            id = "w1", weekLabel = "Week 1", order = 0,
            sessions = listOf(
                ProgramSessionDefinition(id = "w1s1", sessionName = "Push", workoutTemplateId = "t", order = 0),
                ProgramSessionDefinition(id = "w1s2", sessionName = "Pull", workoutTemplateId = "t", order = 1)
            )
        )
        val week2 = ProgramWeekDefinition(
            id = "w2", weekLabel = "Week 2", order = 1,
            sessions = listOf(
                ProgramSessionDefinition(id = "w2s1", sessionName = "Push", workoutTemplateId = "t", order = 0),
                ProgramSessionDefinition(id = "w2s2", sessionName = "Pull", workoutTemplateId = "t", order = 1)
            )
        )
        val program = ProgramTemplate(
            id = "p", name = "Two-week test", description = null,
            weeks = listOf(week1, week2)
        )
        return ActiveProgramCycle(
            cycleUuid = "c", programTemplateId = "p", programTemplateName = "Two-week test",
            userCycleName = "Test Cycle", startDate = startDate,
            completedSessions = completed,
            cycleProgram = program
        )
    }
}
