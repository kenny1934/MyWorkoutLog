package com.kennychiu.myworkoutlog.viewmodel

import app.cash.turbine.test
import com.kennychiu.myworkoutlog.MainDispatcherRule
import com.kennychiu.myworkoutlog.data.ActiveCycleDao
import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveCycleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activeCycleFlow = MutableStateFlow<ActiveProgramCycle?>(null)
    private val dao: ActiveCycleDao = mockk(relaxed = true) {
        every { getActiveCycle() } returns activeCycleFlow
    }

    private fun program(id: String = "prog-1") = ProgramTemplate(
        id = id,
        name = "Test Program",
        description = null,
        weeks = emptyList()
    )

    @Test
    fun `activeCycle reflects DAO flow`() = runTest {
        val vm = ActiveCycleViewModel(dao)
        vm.activeCycle.test {
            assertNull(awaitItem())
            val cycle = sampleCycle("abc")
            activeCycleFlow.value = cycle
            assertEquals(cycle, awaitItem())
        }
    }

    @Test
    fun `startCycle generates a unique cycleUuid and persists via DAO`() = runTest {
        val captured = mutableListOf<ActiveProgramCycle>()
        every { dao.setActiveCycle(any()) } answers { captured += firstArg<ActiveProgramCycle>() }
        val vm = ActiveCycleViewModel(dao)
        val prog = program()

        vm.startCycle(prog, "Hypertrophy Block 1")

        verify(timeout = 2_000) { dao.setActiveCycle(any()) }
        val saved = captured.single()
        assertEquals(prog.id, saved.programTemplateId)
        assertEquals("Hypertrophy Block 1", saved.userCycleName)
        assertEquals(emptyMap<String, String>(), saved.completedSessions)
        assertEquals(prog, saved.cycleProgram)
        // cycleUuid must be a valid UUID (don't just rely on non-empty)
        UUID.fromString(saved.cycleUuid)
    }

    @Test
    fun `two consecutive startCycle calls produce distinct cycleUuids`() = runTest {
        val captured = mutableListOf<ActiveProgramCycle>()
        every { dao.setActiveCycle(any()) } answers { captured += firstArg<ActiveProgramCycle>() }
        val vm = ActiveCycleViewModel(dao)

        vm.startCycle(program(), "Cycle A")
        vm.startCycle(program(), "Cycle B")

        verify(timeout = 2_000, exactly = 2) { dao.setActiveCycle(any()) }
        assertEquals(2, captured.size)
        assertNotEquals(captured[0].cycleUuid, captured[1].cycleUuid)
    }

    @Test
    fun `endCycle clears DAO state`() = runTest {
        val vm = ActiveCycleViewModel(dao)
        vm.endCycle()
        verify(timeout = 2_000) { dao.clear() }
    }

    private fun sampleCycle(uuid: String) = ActiveProgramCycle(
        cycleUuid = uuid,
        programTemplateId = "prog-1",
        programTemplateName = "Test Program",
        userCycleName = "My Cycle",
        startDate = "2026-04-18",
        completedSessions = emptyMap(),
        cycleProgram = program()
    )
}
