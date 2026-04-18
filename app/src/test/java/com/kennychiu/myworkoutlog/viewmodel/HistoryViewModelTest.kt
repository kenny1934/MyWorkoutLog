package com.kennychiu.myworkoutlog.viewmodel

import app.cash.turbine.test
import com.kennychiu.myworkoutlog.MainDispatcherRule
import com.kennychiu.myworkoutlog.data.ActiveCycleDao
import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import com.kennychiu.myworkoutlog.data.LoggedWorkoutDao
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import com.kennychiu.myworkoutlog.data.ProgramTemplateDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val allWorkouts = MutableStateFlow<List<LoggedWorkout>>(emptyList())
    private val activeCycle = MutableStateFlow<ActiveProgramCycle?>(null)
    private val orphanedWorkouts = MutableStateFlow<List<LoggedWorkout>>(emptyList())
    private val allCycleIds = MutableStateFlow<List<String>>(emptyList())
    private val allPrograms = MutableStateFlow<List<ProgramTemplate>>(emptyList())

    private val loggedDao: LoggedWorkoutDao = mockk(relaxed = true) {
        every { getAllLoggedWorkouts() } returns allWorkouts
        every { getOrphanedWorkouts() } returns orphanedWorkouts
        every { getAllCycleIds() } returns allCycleIds
    }
    private val cycleDao: ActiveCycleDao = mockk(relaxed = true) {
        every { getActiveCycle() } returns activeCycle
    }
    private val programDao: ProgramTemplateDao = mockk(relaxed = true) {
        every { getAllPrograms() } returns allPrograms
    }

    private fun vm() = HistoryViewModel(loggedDao, cycleDao, programDao)

    private fun workout(
        id: String,
        cycleId: String? = null,
        date: String = "2026-04-18",
        templateId: String? = null,
        userCycleName: String? = null,
    ) = LoggedWorkout(
        id = id,
        date = date,
        name = "w-$id",
        overallComments = null,
        startTimestamp = null,
        endTimestamp = null,
        bodyweight = null,
        performedWeightUnit = null,
        activeProgramCycleId = cycleId,
        programWeekDefinitionId = null,
        programSessionDefinitionId = null,
        userCycleName = userCycleName,
        loggedExercises = emptyList(),
        workoutTemplateId = templateId,
        isInProgress = false
    )

    private fun cycle(uuid: String) = ActiveProgramCycle(
        cycleUuid = uuid,
        programTemplateId = "prog",
        programTemplateName = "Prog",
        userCycleName = "Named cycle",
        startDate = "2026-04-10",
        completedSessions = emptyMap(),
        cycleProgram = ProgramTemplate(id = "prog", name = "Prog", description = null, weeks = emptyList())
    )

    @Test
    fun `activeCycleWorkouts only emits workouts whose activeProgramCycleId matches the current cycle`() = runTest {
        val vm = vm()
        activeCycle.value = cycle("cycle-A")
        allWorkouts.value = listOf(
            workout("w1", cycleId = "cycle-A"),
            workout("w2", cycleId = "cycle-B"),   // different cycle
            workout("w3", cycleId = null),         // orphan
            workout("w4", cycleId = "cycle-A"),
        )

        vm.activeCycleWorkouts.test {
            val filtered = awaitItem().ifEmpty { awaitItem() }
            assertEquals(setOf("w1", "w4"), filtered.map { it.id }.toSet())
        }
    }

    @Test
    fun `activeCycleWorkouts is empty when there is no active cycle`() = runTest {
        val vm = vm()
        activeCycle.value = null
        allWorkouts.value = listOf(workout("w1", cycleId = "cycle-A"))

        vm.activeCycleWorkouts.test {
            assertEquals(emptyList<LoggedWorkout>(), awaitItem())
        }
    }

    @Test
    fun `completedCycles groups workouts by cycleId and excludes the currently active cycle`() = runTest {
        val vm = vm()
        activeCycle.value = cycle("cycle-current")
        allCycleIds.value = listOf("cycle-current", "cycle-old-1", "cycle-old-2")
        allWorkouts.value = listOf(
            workout("a", cycleId = "cycle-current", date = "2026-04-18"),
            workout("b", cycleId = "cycle-old-1", date = "2026-03-01", userCycleName = "Block 1"),
            workout("c", cycleId = "cycle-old-1", date = "2026-03-05", userCycleName = "Block 1"),
            workout("d", cycleId = "cycle-old-2", date = "2026-02-01", userCycleName = "Block 0"),
        )

        vm.completedCycles.test {
            var cycles = awaitItem()
            // StateFlow may emit initial empty, then the computed value
            while (cycles.isEmpty()) cycles = awaitItem()
            val byId = cycles.associateBy { it.cycleId }
            assertEquals(setOf("cycle-old-1", "cycle-old-2"), byId.keys)
            assertEquals(2, byId["cycle-old-1"]!!.workouts.size)
            assertEquals("Block 1", byId["cycle-old-1"]!!.userCycleName)
            assertEquals(1, byId["cycle-old-2"]!!.workouts.size)
            // Sorted by startDate descending
            assertEquals("cycle-old-1", cycles[0].cycleId)
            assertEquals("cycle-old-2", cycles[1].cycleId)
        }
    }

    @Test
    fun `orphanedWorkouts passes through DAO flow - represents workouts with no cycle id, NOT workouts from ended cycles`() = runTest {
        val vm = vm()
        val orphans = listOf(workout("solo", cycleId = null))
        orphanedWorkouts.value = orphans

        vm.orphanedWorkouts.test {
            val initial = awaitItem()
            val emitted = if (initial.isEmpty()) awaitItem() else initial
            assertEquals(listOf("solo"), emitted.map { it.id })
        }

        // Sanity check: workouts from completed (no-longer-active) cycles are NOT
        // orphans — they still carry their cycleId and should be in completedCycles.
        // This is enforced by the DAO query behind getOrphanedWorkouts() which filters
        // on `activeProgramCycleId IS NULL`, so this test pins the semantic that
        // HistoryViewModel doesn't redefine "orphan" to mean "from ended cycle".
        activeCycle.value = null
        allCycleIds.value = listOf("ended-cycle")
        allWorkouts.value = listOf(workout("inEnded", cycleId = "ended-cycle"))
        vm.completedCycles.test {
            var cycles = awaitItem()
            while (cycles.isEmpty()) cycles = awaitItem()
            assertTrue("workout from ended cycle belongs to completedCycles",
                cycles.any { it.workouts.any { w -> w.id == "inEnded" } })
        }
    }

    @Test
    fun `deleteWorkout delegates to DAO`() = runTest {
        val vm = vm()
        vm.deleteWorkout("target-id")
        verify(timeout = 2_000) { loggedDao.deleteLoggedWorkoutById("target-id") }
    }
}
