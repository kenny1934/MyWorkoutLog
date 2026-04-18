package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ProgramSessionDefinition
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramEditorHelpersTest {

    private fun session(id: String, name: String = id, templateId: String = "tpl", order: Int = 1) =
        ProgramSessionDefinition(id = id, sessionName = name, workoutTemplateId = templateId, order = order)

    private fun week(
        id: String,
        label: String = id,
        order: Int = 1,
        sessions: List<ProgramSessionDefinition> = emptyList(),
        isDeload: Boolean = false,
        rir: String? = null
    ) = ProgramWeekDefinition(
        id = id,
        weekLabel = label,
        sessions = sessions,
        order = order,
        isDeloadWeek = isDeload,
        targetRir = rir
    )

    @Test
    fun `duplicate inserts a copy immediately after the source`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2), week("c", order = 3))
        val counter = IdCounter()
        val result = duplicateWeekInto(weeks, weeks[0], counter)

        assertEquals(4, result.size)
        assertEquals("a", result[0].id)
        assertEquals("id-1", result[1].id)
        assertEquals("b", result[2].id)
        assertEquals("c", result[3].id)
    }

    @Test
    fun `duplicate renumbers order to match list position`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2), week("c", order = 3))
        val result = duplicateWeekInto(weeks, weeks[1], IdCounter())

        assertEquals(listOf(1, 2, 3, 4), result.map { it.order })
    }

    @Test
    fun `duplicate copies sessions with fresh ids`() {
        val originalSessions = listOf(session("s1"), session("s2", order = 2))
        val weeks = listOf(week("a", sessions = originalSessions))
        val result = duplicateWeekInto(weeks, weeks[0], IdCounter())

        val copy = result[1]
        assertEquals(2, copy.sessions.size)
        val copyIds = copy.sessions.map { it.id }
        assertNotEquals("s1", copyIds[0])
        assertNotEquals("s2", copyIds[1])
        assertTrue(copyIds.all { it.startsWith("id-") })
        assertEquals(originalSessions.map { it.sessionName }, copy.sessions.map { it.sessionName })
        assertEquals(originalSessions.map { it.order }, copy.sessions.map { it.order })
    }

    @Test
    fun `duplicate preserves deload flag and targetRir on the copy`() {
        val src = week("a", isDeload = true, rir = "2-3")
        val result = duplicateWeekInto(listOf(src), src, IdCounter())

        val copy = result[1]
        assertTrue(copy.isDeloadWeek)
        assertEquals("2-3", copy.targetRir)
    }

    @Test
    fun `duplicate prefixes label with 'Copy of'`() {
        val src = week("a", label = "Week 1")
        val result = duplicateWeekInto(listOf(src), src, IdCounter())

        assertEquals("Copy of Week 1", result[1].weekLabel)
        assertEquals("Week 1", result[0].weekLabel)
    }

    @Test
    fun `duplicate of unknown source returns the list unchanged`() {
        val weeks = listOf(week("a"), week("b"))
        val orphan = week("zzz")
        val result = duplicateWeekInto(weeks, orphan, IdCounter())

        assertEquals(weeks, result)
    }

    @Test
    fun `moveWeek swaps a week upward`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2), week("c", order = 3))
        val result = moveWeek(weeks, fromIndex = 2, toIndex = 1)

        assertEquals(listOf("a", "c", "b"), result.map { it.id })
    }

    @Test
    fun `moveWeek swaps a week downward`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2), week("c", order = 3))
        val result = moveWeek(weeks, fromIndex = 0, toIndex = 1)

        assertEquals(listOf("b", "a", "c"), result.map { it.id })
    }

    @Test
    fun `moveWeek renumbers order to match list position`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2), week("c", order = 3))
        val result = moveWeek(weeks, fromIndex = 0, toIndex = 2)

        assertEquals(listOf("b", "c", "a"), result.map { it.id })
        assertEquals(listOf(1, 2, 3), result.map { it.order })
    }

    @Test
    fun `moveWeek is a no-op when indices are equal`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2))
        val result = moveWeek(weeks, fromIndex = 1, toIndex = 1)

        assertEquals(weeks, result)
    }

    @Test
    fun `moveWeek is a no-op when fromIndex is out of bounds`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2))
        assertEquals(weeks, moveWeek(weeks, fromIndex = -1, toIndex = 0))
        assertEquals(weeks, moveWeek(weeks, fromIndex = 5, toIndex = 0))
    }

    @Test
    fun `moveWeek is a no-op when toIndex is out of bounds`() {
        val weeks = listOf(week("a", order = 1), week("b", order = 2))
        assertEquals(weeks, moveWeek(weeks, fromIndex = 0, toIndex = -1))
        assertEquals(weeks, moveWeek(weeks, fromIndex = 0, toIndex = 5))
    }

    @Test
    fun `moveSessionToWeek appends session to target week with correct order`() {
        val weeks = listOf(
            week("w1", sessions = listOf(session("s1", order = 1), session("s2", order = 2))),
            week("w2", sessions = listOf(session("t1", order = 1)))
        )
        val result = moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "s1", toWeekId = "w2")

        val dest = result.first { it.id == "w2" }
        assertEquals(listOf("t1", "s1"), dest.sessions.map { it.id })
        assertEquals(listOf(1, 2), dest.sessions.map { it.order })
    }

    @Test
    fun `moveSessionToWeek renumbers source week sessions after removal`() {
        val weeks = listOf(
            week("w1", sessions = listOf(
                session("s1", order = 1),
                session("s2", order = 2),
                session("s3", order = 3)
            )),
            week("w2")
        )
        val result = moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "s2", toWeekId = "w2")

        val src = result.first { it.id == "w1" }
        assertEquals(listOf("s1", "s3"), src.sessions.map { it.id })
        assertEquals(listOf(1, 2), src.sessions.map { it.order })
    }

    @Test
    fun `moveSessionToWeek is a no-op when source and target are the same week`() {
        val weeks = listOf(week("w1", sessions = listOf(session("s1"))))
        val result = moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "s1", toWeekId = "w1")
        assertEquals(weeks, result)
    }

    @Test
    fun `moveSessionToWeek is a no-op when source week is missing`() {
        val weeks = listOf(week("w1", sessions = listOf(session("s1"))), week("w2"))
        assertEquals(weeks, moveSessionToWeek(weeks, fromWeekId = "missing", sessionId = "s1", toWeekId = "w2"))
    }

    @Test
    fun `moveSessionToWeek is a no-op when target week is missing`() {
        val weeks = listOf(week("w1", sessions = listOf(session("s1"))), week("w2"))
        assertEquals(weeks, moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "s1", toWeekId = "missing"))
    }

    @Test
    fun `moveSessionToWeek is a no-op when session is missing in source week`() {
        val weeks = listOf(week("w1", sessions = listOf(session("s1"))), week("w2"))
        assertEquals(weeks, moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "zzz", toWeekId = "w2"))
    }

    @Test
    fun `moveSessionToWeek preserves session id sessionName and workoutTemplateId`() {
        val original = ProgramSessionDefinition(id = "s1", sessionName = "Push A", workoutTemplateId = "tpl-42", order = 1)
        val weeks = listOf(
            week("w1", sessions = listOf(original)),
            week("w2")
        )
        val result = moveSessionToWeek(weeks, fromWeekId = "w1", sessionId = "s1", toWeekId = "w2")

        val moved = result.first { it.id == "w2" }.sessions.single()
        assertEquals("s1", moved.id)
        assertEquals("Push A", moved.sessionName)
        assertEquals("tpl-42", moved.workoutTemplateId)
    }

    @Test
    fun `moveWeek preserves sessions and ids of the moved week`() {
        val sessions = listOf(session("s1"), session("s2", order = 2))
        val weeks = listOf(
            week("a", order = 1),
            week("b", order = 2, sessions = sessions, isDeload = true, rir = "2-3"),
            week("c", order = 3)
        )
        val result = moveWeek(weeks, fromIndex = 1, toIndex = 0)

        val moved = result[0]
        assertEquals("b", moved.id)
        assertEquals(sessions, moved.sessions)
        assertTrue(moved.isDeloadWeek)
        assertEquals("2-3", moved.targetRir)
    }

    private class IdCounter : () -> String {
        private var n = 0
        override fun invoke(): String = "id-${++n}"
    }
}
