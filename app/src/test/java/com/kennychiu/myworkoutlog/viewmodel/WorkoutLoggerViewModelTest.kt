package com.kennychiu.myworkoutlog.viewmodel

import com.kennychiu.myworkoutlog.MainDispatcherRule
import com.kennychiu.myworkoutlog.data.ActiveCycleDao
import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.BodyweightDao
import com.kennychiu.myworkoutlog.data.Equipment
import com.kennychiu.myworkoutlog.data.Exercise
import com.kennychiu.myworkoutlog.data.ExerciseDao
import com.kennychiu.myworkoutlog.data.LoggedExercise
import com.kennychiu.myworkoutlog.data.LoggedSet
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import com.kennychiu.myworkoutlog.data.LoggedWorkoutDao
import com.kennychiu.myworkoutlog.data.MuscleGroup
import com.kennychiu.myworkoutlog.data.PersonalRecordDao
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import com.kennychiu.myworkoutlog.data.TemplateExercise
import com.kennychiu.myworkoutlog.data.TemplateExerciseSet
import com.kennychiu.myworkoutlog.data.WorkoutTemplate
import com.kennychiu.myworkoutlog.data.WorkoutTemplateDao
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutLoggerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val templateDao: WorkoutTemplateDao = mockk(relaxed = true)
    private val loggedDao: LoggedWorkoutDao = mockk(relaxed = true)
    private val prDao: PersonalRecordDao = mockk(relaxed = true)
    private val exerciseDao: ExerciseDao = mockk(relaxed = true) {
        every { getAllExercises() } returns emptyFlow()
        every { getAllExercisesSnapshot() } returns emptyList()
    }
    private val activeCycleDao: ActiveCycleDao = mockk(relaxed = true)
    private val bodyweightDao: BodyweightDao = mockk(relaxed = true) {
        every { getBodyweightForDate(any()) } returns null
    }

    private fun newVm() = WorkoutLoggerViewModel(
        templateDao, loggedDao, prDao, exerciseDao, activeCycleDao, bodyweightDao
    )

    private fun sampleTemplate(id: String = "tpl-1") = WorkoutTemplate(
        id = id,
        name = "Push Day",
        description = null,
        templateExercises = listOf(
            TemplateExercise(
                id = "te-1",
                exerciseId = "ex-1",
                exerciseName = "Bench Press",
                targetMuscleGroups = listOf(MuscleGroup.CHEST),
                equipment = listOf(Equipment.BARBELL),
                sets = listOf(
                    TemplateExerciseSet(id = "s-1", targetReps = "8-12"),
                    TemplateExerciseSet(id = "s-2", targetReps = "8-12"),
                ),
                order = 0,
            )
        )
    )

    private fun completedWorkout(id: String, startMs: Long, endMs: Long) = LoggedWorkout(
        id = id,
        date = "2026-04-18",
        name = "Past workout",
        overallComments = null,
        startTimestamp = startMs,
        endTimestamp = endMs,
        bodyweight = null,
        performedWeightUnit = "kg",
        loggedExercises = listOf(
            LoggedExercise(
                id = "le-1",
                exerciseId = "ex-1",
                exerciseName = "Bench Press",
                targetMuscleGroups = listOf(MuscleGroup.CHEST),
                equipment = listOf(Equipment.BARBELL),
                sets = listOf(
                    LoggedSet(id = "ls-1", reps = 10, weight = 80.0)
                ),
            )
        ),
        workoutTemplateId = "tpl-1",
        isInProgress = false,
    )

    @Test
    fun `init triggers cleanup of abandoned in-progress workouts`() = runTest {
        newVm()
        verify(timeout = 2_000) { loggedDao.cleanupAbandonedInProgressWorkouts(any()) }
    }

    @Test
    fun `startWorkoutFromTemplate with no existing in-progress workout inserts a fresh LoggedWorkout`() = runTest {
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns null
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()

        val inserted = slot<LoggedWorkout>()
        every { loggedDao.insert(capture(inserted)) } answers { }

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", cycleId = "cycle-xyz", weekId = "w1", sessionId = "s1")

        verify(timeout = 2_000) { loggedDao.insert(any()) }
        val saved = inserted.captured
        assertTrue("new workout must be marked in-progress", saved.isInProgress)
        assertEquals("cycle-xyz", saved.activeProgramCycleId)
        assertEquals("w1", saved.programWeekDefinitionId)
        assertEquals("s1", saved.programSessionDefinitionId)
        assertEquals("tpl-1", saved.workoutTemplateId)
        assertEquals(2, saved.loggedExercises.first().sets.size)
        assertNull("end timestamp must be null for a new workout", saved.endTimestamp)
        assertNotNull("start timestamp must be set", saved.startTimestamp)
        // activeWorkoutState must now reflect the new workout
        assertEquals(saved.id, vm.activeWorkoutState.value?.id)
        assertFalse("fresh start is not edit mode", vm.isInEditMode())
    }

    @Test
    fun `startWorkoutFromTemplate with existing in-progress workout loads it instead of inserting`() = runTest {
        val resumed = completedWorkout("resumed-id", startMs = 1_000_000L, endMs = 0L)
            .copy(isInProgress = true, endTimestamp = null)
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns resumed
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", cycleId = null, weekId = null, sessionId = null)

        // We should NOT have inserted a new workout
        verify(timeout = 2_000, exactly = 0) { loggedDao.insert(any()) }
        // Wait until state reflects resumed workout
        waitUntil { vm.activeWorkoutState.value?.id == "resumed-id" }
        assertEquals("resumed-id", vm.activeWorkoutState.value?.id)
        assertFalse(vm.isInEditMode())
    }

    @Test
    fun `startFreshWorkout with existing in-progress workout marks the old one completed and inserts a new one`() = runTest {
        val old = completedWorkout("old-id", startMs = 1_000_000L, endMs = 0L)
            .copy(isInProgress = true, endTimestamp = null)
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns old
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()

        val inserted = slot<LoggedWorkout>()
        every { loggedDao.insert(capture(inserted)) } answers { }

        val vm = newVm()
        vm.startFreshWorkout("tpl-1", null, null, null)

        verify(timeout = 2_000) { loggedDao.markWorkoutAsCompleted("old-id") }
        verify(timeout = 2_000) { loggedDao.insert(any()) }
        assertNotEquals("fresh workout must have a new id", "old-id", inserted.captured.id)
        assertTrue(inserted.captured.isInProgress)
    }

    @Test
    fun `loadWorkoutForEdit sets edit mode and does not arm the live timer`() = runTest {
        val startMs = 1_000_000L
        val endMs = startMs + 600_000L // 10 minutes in ms
        val completed = completedWorkout("done-id", startMs = startMs, endMs = endMs)
        every { loggedDao.getLoggedWorkoutById("done-id") } returns flowOf(completed)

        val vm = newVm()
        vm.loadWorkoutForEdit("done-id")

        waitUntil { vm.activeWorkoutState.value?.id == "done-id" }
        assertTrue("VM must be in edit mode after loadWorkoutForEdit", vm.isInEditMode())
        assertEquals("done-id", vm.activeWorkoutState.value?.id)
    }

    @Test
    fun `finishWorkout in edit mode updates existing workout preserving id and startTimestamp, and resets edit flags`() = runTest {
        val startMs = 1_000_000L
        val endMs = startMs + 600_000L
        val completed = completedWorkout("done-id", startMs = startMs, endMs = endMs)
        every { loggedDao.getLoggedWorkoutById("done-id") } returns flowOf(completed)
        every { prDao.getPRsForExercise(any()) } returns emptyList()

        val updated = slot<LoggedWorkout>()
        every { loggedDao.updateLoggedWorkout(capture(updated)) } answers { }

        val vm = newVm()
        vm.loadWorkoutForEdit("done-id")
        waitUntil { vm.activeWorkoutState.value?.id == "done-id" }

        vm.finishWorkout(currentUnit = "kg", activeCycle = null)

        verify(timeout = 2_000) { loggedDao.updateLoggedWorkout(any()) }
        verify(timeout = 2_000, exactly = 0) { loggedDao.insert(any()) }
        val saved = updated.captured
        assertEquals("edit must preserve original id", "done-id", saved.id)
        assertEquals("edit must preserve startTimestamp", startMs, saved.startTimestamp)

        // After finishWorkout, edit mode must be cleared so a subsequent "resume" doesn't
        // re-enter edit mode (previous bug: editing then finishing left the VM in edit mode,
        // and resuming produced a duplicate workout).
        waitUntil { !vm.isInEditMode() }
        assertFalse("finishWorkout must reset edit mode", vm.isInEditMode())
    }

    @Test
    fun `finishWorkout for a new workout inserts with isInProgress=false and clears state`() = runTest {
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns null
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()
        every { prDao.getPRsForExercise(any()) } returns emptyList()

        val inserted = mutableListOf<LoggedWorkout>()
        every { loggedDao.insert(any()) } answers { inserted += firstArg<LoggedWorkout>() }

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", cycleId = null, weekId = null, sessionId = null)
        waitUntil { vm.activeWorkoutState.value != null }

        vm.finishWorkout(currentUnit = "kg", activeCycle = null)

        verify(timeout = 2_000, exactly = 2) { loggedDao.insert(any()) }
        // First insert: initial in-progress create. Second insert: finalized with endTimestamp.
        assertTrue(inserted.first().isInProgress)
        assertFalse("finished workout must flip isInProgress=false", inserted.last().isInProgress)
        assertNotNull("finished workout must have endTimestamp", inserted.last().endTimestamp)
        waitUntil { vm.activeWorkoutState.value == null }
        assertNull(vm.activeWorkoutState.value)
    }

    @Test
    fun `finishWorkout writes the completed workout id into activeCycle completedSessions under weekId_sessionId key`() = runTest {
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns null
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()
        every { prDao.getPRsForExercise(any()) } returns emptyList()

        val inserted = mutableListOf<LoggedWorkout>()
        every { loggedDao.insert(any()) } answers { inserted += firstArg<LoggedWorkout>() }

        val updatedCycle = slot<ActiveProgramCycle>()
        every { activeCycleDao.setActiveCycle(capture(updatedCycle)) } answers { }

        val activeCycle = ActiveProgramCycle(
            cycleUuid = "cycle-xyz",
            programTemplateId = "prog",
            programTemplateName = "Prog",
            userCycleName = "Block",
            startDate = "2026-04-18",
            completedSessions = emptyMap(),
            cycleProgram = ProgramTemplate(id = "prog", name = "Prog", description = null, weeks = emptyList())
        )

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", cycleId = "cycle-xyz", weekId = "week-1", sessionId = "session-3")
        waitUntil { vm.activeWorkoutState.value != null }

        vm.finishWorkout(currentUnit = "kg", activeCycle = activeCycle)

        verify(timeout = 2_000) { activeCycleDao.setActiveCycle(any()) }
        val saved = updatedCycle.captured
        val workoutId = inserted.last().id
        assertEquals(mapOf("week-1_session-3" to workoutId), saved.completedSessions)
    }

    @Test
    fun `cancelWorkout on a new workout marks it completed in the DB and clears VM state`() = runTest {
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns null
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", null, null, null)
        waitUntil { vm.activeWorkoutState.value != null }
        val id = vm.activeWorkoutState.value!!.id

        vm.cancelWorkout()

        verify(timeout = 2_000) { loggedDao.markWorkoutAsCompleted(id) }
        assertNull(vm.activeWorkoutState.value)
        assertFalse(vm.isInEditMode())
    }

    @Test
    fun `cancelWorkout while in edit mode does NOT mark the original workout completed`() = runTest {
        val completed = completedWorkout("done-id", startMs = 1_000_000L, endMs = 1_600_000L)
        every { loggedDao.getLoggedWorkoutById("done-id") } returns flowOf(completed)

        val vm = newVm()
        vm.loadWorkoutForEdit("done-id")
        waitUntil { vm.activeWorkoutState.value?.id == "done-id" }

        vm.cancelWorkout()

        // Crucial: editing a finished workout and then cancelling must not flip it to "completed"
        // again via markWorkoutAsCompleted — it was already not in-progress.
        verify(exactly = 0) { loggedDao.markWorkoutAsCompleted(any()) }
        assertNull(vm.activeWorkoutState.value)
        assertFalse(vm.isInEditMode())
    }

    @Test
    fun `updateSet mutates the correct set without affecting sibling sets`() = runTest {
        every { loggedDao.getInProgressWorkoutForTemplate("tpl-1") } returns null
        every { templateDao.getTemplateByIdSnapshot("tpl-1") } returns sampleTemplate()

        val vm = newVm()
        vm.startWorkoutFromTemplate("tpl-1", null, null, null)
        waitUntil { vm.activeWorkoutState.value != null }

        val workout = vm.activeWorkoutState.value!!
        val exerciseId = workout.loggedExercises.first().id
        val firstSetId = workout.loggedExercises.first().sets[0].id
        val secondSetId = workout.loggedExercises.first().sets[1].id

        vm.updateSet(exerciseId, firstSetId, reps = "10", weight = 80.0, secs = "")

        val updated = vm.activeWorkoutState.value!!.loggedExercises.first()
        val first = updated.sets.first { it.id == firstSetId }
        val second = updated.sets.first { it.id == secondSetId }
        assertEquals(10, first.reps)
        assertEquals(80.0, first.weight!!, 0.0)
        assertNull("second set must not be mutated", second.reps)
        assertNull(second.weight)
    }

    /** Wait up to [timeoutMs] for [condition] to be true. Keeps the test off the real clock. */
    private suspend fun waitUntil(timeoutMs: Long = 2_000, intervalMs: Long = 10, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(intervalMs)
        }
        if (!condition()) throw AssertionError("condition not met within ${timeoutMs}ms")
    }
}
