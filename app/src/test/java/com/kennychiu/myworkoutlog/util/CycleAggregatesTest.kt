package com.kennychiu.myworkoutlog.util

import com.kennychiu.myworkoutlog.data.ActiveProgramCycle
import com.kennychiu.myworkoutlog.data.Equipment
import com.kennychiu.myworkoutlog.data.LoggedExercise
import com.kennychiu.myworkoutlog.data.LoggedSet
import com.kennychiu.myworkoutlog.data.LoggedWorkout
import com.kennychiu.myworkoutlog.data.MuscleGroup
import com.kennychiu.myworkoutlog.data.PRType
import com.kennychiu.myworkoutlog.data.PersonalRecord
import com.kennychiu.myworkoutlog.data.ProgramSessionDefinition
import com.kennychiu.myworkoutlog.data.ProgramTemplate
import com.kennychiu.myworkoutlog.data.ProgramWeekDefinition
import com.kennychiu.myworkoutlog.data.TemplateExercise
import com.kennychiu.myworkoutlog.data.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleAggregatesTest {

    @Test
    fun `empty workouts — no per-week entries, no PRs`() {
        val agg = cycleAggregates(cycle(), workouts = emptyList())
        assertTrue(agg.perWeek.isEmpty())
        assertTrue(agg.prsHit.isEmpty())
        assertNull(agg.weightUnit)
    }

    @Test
    fun `workouts outside the cycle are filtered out`() {
        val c = cycle()
        val other = workout("w-other", cycleId = "OTHER", weekId = "w1", sets = 2)
        val mine = workout("w-mine", cycleId = c.cycleUuid, weekId = "w1", sets = 3)
        val agg = cycleAggregates(c, listOf(other, mine))
        assertEquals(1, agg.perWeek.size)
        assertEquals(3, agg.perWeek["w1"]!!.setCount)
        assertEquals(1, agg.perWeek["w1"]!!.workoutCount)
    }

    @Test
    fun `per-week totals sum sets, volume, and duration`() {
        val c = cycle()
        val w1a = workout(
            "a", c.cycleUuid, weekId = "w1",
            loggedExercises = listOf(
                exercise(sets = listOf(set(reps = 10, weight = 100.0), set(reps = 8, weight = 110.0)))
            ),
            startMs = 1_000L, endMs = 1_000L + 30 * 60_000L,
            unit = "kg",
        )
        val w1b = workout(
            "b", c.cycleUuid, weekId = "w1",
            loggedExercises = listOf(
                exercise(sets = listOf(set(reps = 5, weight = 80.0)))
            ),
            startMs = 2_000L, endMs = 2_000L + 20 * 60_000L,
            unit = "kg",
        )
        val agg = cycleAggregates(c, listOf(w1a, w1b))
        val w1 = agg.perWeek["w1"]!!
        assertEquals(2, w1.workoutCount)
        assertEquals(3, w1.setCount)
        // 10*100 + 8*110 + 5*80 = 1000 + 880 + 400 = 2280
        assertEquals(2280.0, w1.totalVolume, 0.0001)
        assertEquals(50L * 60_000L, w1.totalDurationMs)
        assertEquals("kg", agg.weightUnit)
    }

    @Test
    fun `workouts missing programWeekDefinitionId are dropped`() {
        val c = cycle()
        val noWeek = workout("x", c.cycleUuid, weekId = null, sets = 5)
        val agg = cycleAggregates(c, listOf(noWeek))
        assertTrue(agg.perWeek.isEmpty())
    }

    @Test
    fun `null or zero-width timestamps don't contribute to duration`() {
        val c = cycle()
        val a = workout("a", c.cycleUuid, weekId = "w1", sets = 1, startMs = null, endMs = 123L)
        val b = workout("b", c.cycleUuid, weekId = "w1", sets = 1, startMs = 500L, endMs = 500L)
        val agg = cycleAggregates(c, listOf(a, b))
        assertEquals(0L, agg.perWeek["w1"]!!.totalDurationMs)
    }

    @Test
    fun `PRs hit during this cycle are attached to their week`() {
        val c = cycle()
        val w1 = workout("w1id", c.cycleUuid, weekId = "w1", sets = 1)
        val w2 = workout("w2id", c.cycleUuid, weekId = "w2", sets = 1)
        val prInCycle = pr(id = "pr1", workoutId = "w1id", date = "2026-04-10")
        val prDifferentWorkout = pr(id = "pr2", workoutId = "other", date = "2026-04-11")
        val prInW2 = pr(id = "pr3", workoutId = "w2id", date = "2026-04-12")
        val agg = cycleAggregates(c, listOf(w1, w2), listOf(prInCycle, prDifferentWorkout, prInW2))
        assertEquals(2, agg.prsHit.size)
        // sorted by date desc
        assertEquals("pr3", agg.prsHit[0].pr.id)
        assertEquals("w2", agg.prsHit[0].weekId)
        assertEquals("pr1", agg.prsHit[1].pr.id)
        assertEquals("w1", agg.prsHit[1].weekId)
    }

    @Test
    fun `weightUnit picks the most common across cycle workouts`() {
        val c = cycle()
        val a = workout("a", c.cycleUuid, weekId = "w1", sets = 1, unit = "kg")
        val b = workout("b", c.cycleUuid, weekId = "w1", sets = 1, unit = "kg")
        val d = workout("d", c.cycleUuid, weekId = "w2", sets = 1, unit = "lbs")
        val agg = cycleAggregates(c, listOf(a, b, d))
        assertEquals("kg", agg.weightUnit)
    }

    @Test
    fun `weightUnit is null when no workout has one recorded`() {
        val c = cycle()
        val a = workout("a", c.cycleUuid, weekId = "w1", sets = 1, unit = null)
        val agg = cycleAggregates(c, listOf(a))
        assertNull(agg.weightUnit)
        assertNotNull(agg.perWeek["w1"])
    }

    // ── cycleBaselinesByExercise ──────────────────────────────────────────

    @Test
    fun `baselines — empty cycle yields empty map`() {
        val empty = cycle().copy(cycleProgram = cycle().cycleProgram.copy(weeks = emptyList()))
        val baselines = cycleBaselinesByExercise(empty, emptyMap()) { null }
        assertTrue(baselines.isEmpty())
    }

    @Test
    fun `baselines — missing templates are skipped silently`() {
        val c = cycle()
        val lookups = mutableListOf<String>()
        val baselines = cycleBaselinesByExercise(c, emptyMap()) { id ->
            lookups += id
            null
        }
        assertTrue(baselines.isEmpty())
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun `baselines — lookup returns null excludes the exercise`() {
        val c = cycle()
        val templates = mapOf("t" to template("t", listOf(templateExercise("exid"))))
        val baselines = cycleBaselinesByExercise(c, templates) { null }
        assertTrue(baselines.isEmpty())
    }

    @Test
    fun `baselines — picks heaviest weight-reps top set from pre-cycle workout`() {
        val c = cycle()
        val templates = mapOf("t" to template("t", listOf(templateExercise("exid"))))
        val preCycle = workout(
            id = "pre",
            cycleId = "OTHER",
            weekId = null,
            loggedExercises = listOf(
                exercise(sets = listOf(
                    set(reps = 10, weight = 80.0),
                    set(reps = 5, weight = 100.0),
                    set(reps = 8, weight = 90.0),
                )),
            ),
        )
        val baselines = cycleBaselinesByExercise(c, templates) { id ->
            if (id == "exid") preCycle else null
        }
        assertEquals(ExerciseTopSet(100.0, 5), baselines["exid"])
    }

    @Test
    fun `baselines — duration-only top set stores ExerciseTopSet with null weight and reps`() {
        val c = cycle()
        val templates = mapOf("t" to template("t", listOf(templateExercise("exid"))))
        val preCycle = workout(
            id = "pre",
            cycleId = "OTHER",
            weekId = null,
            loggedExercises = listOf(
                exercise(sets = listOf(durationSet(secs = 60), durationSet(secs = 45))),
            ),
        )
        val baselines = cycleBaselinesByExercise(c, templates) { id ->
            if (id == "exid") preCycle else null
        }
        assertEquals(ExerciseTopSet(null, null), baselines["exid"])
    }

    @Test
    fun `baselines — exerciseId referenced in multiple sessions dedupes to one lookup`() {
        val twoWeekCycle = cycle()
        val templates = mapOf("t" to template("t", listOf(templateExercise("exid"))))
        val preCycle = workout(
            id = "pre",
            cycleId = "OTHER",
            weekId = null,
            loggedExercises = listOf(
                exercise(sets = listOf(set(reps = 5, weight = 120.0))),
            ),
        )
        val lookups = mutableListOf<String>()
        val baselines = cycleBaselinesByExercise(twoWeekCycle, templates) { id ->
            lookups += id
            preCycle
        }
        assertEquals(1, lookups.size)
        assertEquals("exid", lookups.single())
        assertEquals(ExerciseTopSet(120.0, 5), baselines["exid"])
    }

    @Test
    fun `baselines — returned workout missing exerciseId is excluded`() {
        val c = cycle()
        val templates = mapOf("t" to template("t", listOf(templateExercise("exid"))))
        val preCycle = workout(
            id = "pre",
            cycleId = "OTHER",
            weekId = null,
            loggedExercises = listOf(
                LoggedExercise(
                    id = "ex",
                    exerciseId = "different-id",
                    exerciseName = "Something else",
                    targetMuscleGroups = emptyList(),
                    equipment = emptyList(),
                    sets = listOf(set(reps = 5, weight = 100.0)),
                ),
            ),
        )
        val baselines = cycleBaselinesByExercise(c, templates) { preCycle }
        assertTrue(baselines.isEmpty())
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun cycle(): ActiveProgramCycle {
        val week1 = ProgramWeekDefinition(
            id = "w1", weekLabel = "Week 1", order = 0,
            sessions = listOf(
                ProgramSessionDefinition(id = "w1s1", sessionName = "A", workoutTemplateId = "t", order = 0),
            ),
        )
        val week2 = ProgramWeekDefinition(
            id = "w2", weekLabel = "Week 2", order = 1,
            sessions = listOf(
                ProgramSessionDefinition(id = "w2s1", sessionName = "A", workoutTemplateId = "t", order = 0),
            ),
        )
        val program = ProgramTemplate(
            id = "p", name = "P", description = null,
            weeks = listOf(week1, week2),
        )
        return ActiveProgramCycle(
            cycleUuid = "CYCLE-UUID",
            programTemplateId = "p",
            programTemplateName = "P",
            userCycleName = "Test",
            startDate = "2026-04-01",
            completedSessions = emptyMap(),
            cycleProgram = program,
        )
    }

    private fun workout(
        id: String,
        cycleId: String,
        weekId: String?,
        sets: Int = 0,
        loggedExercises: List<LoggedExercise>? = null,
        startMs: Long? = null,
        endMs: Long? = null,
        unit: String? = "kg",
    ): LoggedWorkout {
        val exs = loggedExercises ?: listOf(
            exercise(sets = (1..sets).map { set(reps = 5, weight = 50.0) })
        )
        return LoggedWorkout(
            id = id,
            date = "2026-04-10",
            name = null,
            overallComments = null,
            startTimestamp = startMs,
            endTimestamp = endMs,
            bodyweight = null,
            performedWeightUnit = unit,
            activeProgramCycleId = cycleId,
            programWeekDefinitionId = weekId,
            programSessionDefinitionId = null,
            userCycleName = null,
            loggedExercises = exs,
            workoutTemplateId = null,
            isInProgress = false,
        )
    }

    private fun exercise(sets: List<LoggedSet>) = LoggedExercise(
        id = "ex",
        exerciseId = "exid",
        exerciseName = "Squat",
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
        sets = sets,
    )

    private fun set(reps: Int?, weight: Double?): LoggedSet =
        LoggedSet(id = java.util.UUID.randomUUID().toString(), reps = reps, weight = weight)

    private fun durationSet(secs: Int): LoggedSet =
        LoggedSet(id = java.util.UUID.randomUUID().toString(), secs = secs)

    private fun template(id: String, exercises: List<TemplateExercise>) = WorkoutTemplate(
        id = id,
        name = id,
        description = null,
        templateExercises = exercises,
    )

    private fun templateExercise(exerciseId: String) = TemplateExercise(
        id = "te-$exerciseId",
        exerciseId = exerciseId,
        exerciseName = exerciseId,
        targetMuscleGroups = emptyList<MuscleGroup>(),
        equipment = emptyList<Equipment>(),
        sets = emptyList(),
        order = 0,
    )

    private fun pr(id: String, workoutId: String, date: String) = PersonalRecord(
        id = id,
        exerciseId = "exid",
        exerciseName = "Squat",
        date = date,
        loggedWorkoutId = workoutId,
        type = PRType.MAX_WEIGHT_FOR_REPS,
        weightUnit = "kg",
        reps = 5,
        weight = 100.0,
        durationSecs = null,
    )
}
