package com.kennychiu.myworkoutlog.data

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsRepositoryTest {

    @Test
    fun `totalVolumeChange is positive when current exceeds previous`() = runTest {
        val current = listOf(workout(sets = listOf(set(weight = 100.0, reps = 5))))    // 500
        val previous = listOf(workout(sets = listOf(set(weight = 100.0, reps = 4))))   // 400
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        assertEquals(25.0, comp.totalVolumeChange!!, 0.001)
        assertEquals("cur", comp.currentCycleId)
        assertEquals("prev", comp.previousCycleId)
    }

    @Test
    fun `totalVolumeChange is null when previous volume is zero`() = runTest {
        val current = listOf(workout(sets = listOf(set(weight = 100.0, reps = 5))))
        val previous = listOf(workout(sets = listOf(set(weight = null, reps = null))))
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        assertNull(comp.totalVolumeChange)
    }

    @Test
    fun `totalVolumeChange is null and previous side is empty when previousCycleId is null`() = runTest {
        val current = listOf(workout(sets = listOf(set(weight = 100.0, reps = 5))))
        val repo = repo(currentWorkouts = current, previousWorkouts = emptyList())

        val comp = repo.compareCycles("cur", previousCycleId = null).first()

        assertNull(comp.totalVolumeChange)
        assertTrue(comp.strengthGains.isEmpty())
        assertNull(comp.previousCycleId)
    }

    @Test
    fun `strengthGains include exercises present in both cycles`() = runTest {
        val current = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 120.0, reps = 1)))))
        )
        val previous = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 1)))))
        )
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        val gain = comp.strengthGains.single()
        assertEquals("sq", gain.exerciseId)
        assertEquals(20.0, gain.strengthGainPercentage!!, 0.001)   // (120-100)/100 * 100
        assertEquals(20.0, gain.weightIncrease!!, 0.001)
        assertEquals(0, gain.repIncrease)
    }

    @Test
    fun `strengthGains pick the set with highest Epley 1RM per exercise`() = runTest {
        // Current: 100x1 (1RM 100) beats 80x5 (1RM 93.33); previous best is 90x1 (1RM 90).
        val current = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(
                set(weight = 80.0, reps = 5),
                set(weight = 100.0, reps = 1),
            ))))
        )
        val previous = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 90.0, reps = 1)))))
        )
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        val gain = comp.strengthGains.single()
        assertEquals(11.111, gain.strengthGainPercentage!!, 0.01)   // (100-90)/90 * 100
        assertEquals(10.0, gain.weightIncrease!!, 0.001)
        assertEquals(0, gain.repIncrease)
    }

    @Test
    fun `strengthGains exclude exercises only in current cycle`() = runTest {
        val current = listOf(
            workout(exercises = listOf(loggedExercise("new", "New lift", listOf(set(weight = 60.0, reps = 5)))))
        )
        val previous = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 1)))))
        )
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        assertTrue(comp.strengthGains.isEmpty())
    }

    @Test
    fun `strengthGains exclude exercises with no valid sets in current cycle`() = runTest {
        val current = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = null, reps = null)))))
        )
        val previous = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 1)))))
        )
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        assertTrue(comp.strengthGains.isEmpty())
    }

    @Test
    fun `strengthGains exclude exercises with no valid sets in previous cycle`() = runTest {
        val current = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 120.0, reps = 1)))))
        )
        val previous = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = null, reps = null)))))
        )
        val repo = repo(currentWorkouts = current, previousWorkouts = previous)

        val comp = repo.compareCycles("cur", "prev").first()

        assertTrue(comp.strengthGains.isEmpty())
    }

    @Test
    fun `averageWorkoutDuration is the mean of current workouts in minutes`() = runTest {
        val current = listOf(
            // 10 min = 600_000 ms
            workout(start = 0L, end = 600_000L),
            // 30 min
            workout(start = 0L, end = 1_800_000L),
        )
        val repo = repo(currentWorkouts = current)

        val comp = repo.compareCycles("cur", "prev").first()

        assertEquals(20L, comp.averageWorkoutDuration)
    }

    @Test
    fun `averageWorkoutDuration is null when no workouts have both timestamps`() = runTest {
        val current = listOf(
            workout(start = null, end = 600_000L),
            workout(start = 0L, end = null),
        )
        val repo = repo(currentWorkouts = current)

        val comp = repo.compareCycles("cur", "prev").first()

        assertNull(comp.averageWorkoutDuration)
    }

    @Test
    fun `averageWorkoutDuration skips workouts missing either timestamp`() = runTest {
        val current = listOf(
            workout(start = 0L, end = 600_000L),        // counted: 10 min
            workout(start = null, end = 1_800_000L),    // skipped
            workout(start = 0L, end = 1_200_000L),      // counted: 20 min
        )
        val repo = repo(currentWorkouts = current)

        val comp = repo.compareCycles("cur", "prev").first()

        assertEquals(15L, comp.averageWorkoutDuration)
    }

    @Test
    fun `programTemplateName is taken from the active cycle`() = runTest {
        val repo = repo(
            currentWorkouts = emptyList(),
            previousWorkouts = emptyList(),
            activeCycle = activeCycle(programName = "My Hypertrophy Block"),
        )

        val comp = repo.compareCycles("cur", "prev").first()

        assertEquals("My Hypertrophy Block", comp.programTemplateName)
    }

    @Test
    fun `programTemplateName falls back to Unknown Program when no active cycle`() = runTest {
        val repo = repo(currentWorkouts = emptyList(), previousWorkouts = emptyList(), activeCycle = null)

        val comp = repo.compareCycles("cur", "prev").first()

        assertEquals("Unknown Program", comp.programTemplateName)
    }

    @Test
    fun `empty current and previous cycles yield empty strengthGains and null volumeChange`() = runTest {
        val repo = repo(currentWorkouts = emptyList(), previousWorkouts = emptyList())

        val comp = repo.compareCycles("cur", "prev").first()

        assertNull(comp.totalVolumeChange)
        assertTrue(comp.strengthGains.isEmpty())
        assertNull(comp.averageWorkoutDuration)
    }

    // -- fixtures --

    private fun repo(
        currentWorkouts: List<LoggedWorkout> = emptyList(),
        previousWorkouts: List<LoggedWorkout> = emptyList(),
        activeCycle: ActiveProgramCycle? = null,
    ): AnalyticsRepository {
        val loggedDao: LoggedWorkoutDao = mockk(relaxed = true) {
            every { getWorkoutsByCycle("cur") } returns flowOf(currentWorkouts)
            every { getWorkoutsByCycle("prev") } returns flowOf(previousWorkouts)
        }
        val cycleDao: ActiveCycleDao = mockk(relaxed = true) {
            every { getActiveCycle() } returns flowOf(activeCycle)
        }
        val prDao: PersonalRecordDao = mockk(relaxed = true)
        return AnalyticsRepository(loggedDao, cycleDao, prDao)
    }

    private fun set(
        weight: Double? = null,
        reps: Int? = null,
        secs: Int? = null,
    ) = LoggedSet(
        id = UUID.randomUUID().toString(),
        weight = weight,
        reps = reps,
        secs = secs,
    )

    private fun loggedExercise(
        exerciseId: String,
        name: String,
        sets: List<LoggedSet>,
    ) = LoggedExercise(
        id = "le-$exerciseId-${UUID.randomUUID()}",
        exerciseId = exerciseId,
        exerciseName = name,
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
        sets = sets,
    )

    private fun workout(
        sets: List<LoggedSet>? = null,
        exercises: List<LoggedExercise>? = null,
        start: Long? = null,
        end: Long? = null,
    ): LoggedWorkout {
        val ex = exercises ?: listOf(loggedExercise("sq", "Squat", sets ?: emptyList()))
        return LoggedWorkout(
            id = UUID.randomUUID().toString(),
            date = "2026-04-22",
            name = null,
            overallComments = null,
            startTimestamp = start,
            endTimestamp = end,
            bodyweight = null,
            performedWeightUnit = "kg",
            activeProgramCycleId = null,
            programWeekDefinitionId = null,
            programSessionDefinitionId = null,
            userCycleName = null,
            loggedExercises = ex,
            workoutTemplateId = null,
            isInProgress = false,
        )
    }

    private fun activeCycle(programName: String = "Unused Program") = ActiveProgramCycle(
        id = 1,
        cycleUuid = UUID.randomUUID().toString(),
        programTemplateId = "pt",
        programTemplateName = programName,
        userCycleName = "my cycle",
        startDate = "2026-04-01",
        completedSessions = emptyMap(),
        cycleProgram = ProgramTemplate(id = "pt", name = programName, weeks = emptyList()),
    )
}
