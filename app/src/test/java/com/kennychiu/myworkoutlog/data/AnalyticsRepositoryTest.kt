package com.kennychiu.myworkoutlog.data

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    // -- getWeeklyVolumeSummary --

    @Test
    fun `weekly summary endDate is six days after start and periodLabel names the week`() = runTest {
        val repo = repo(dateRangeWorkouts = emptyList())

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        assertEquals("2026-04-01", summary.startDate)
        assertEquals("2026-04-07", summary.endDate)
        assertEquals("Week of 2026-04-01", summary.periodLabel)
    }

    @Test
    fun `weekly summary empty workouts yield zero volume and zero average`() = runTest {
        val repo = repo(dateRangeWorkouts = emptyList())

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        assertEquals(0.0, summary.totalVolume, 0.0)
        assertEquals(0, summary.workoutCount)
        assertEquals(0.0, summary.averageVolumePerWorkout, 0.0)
        assertTrue(summary.exerciseBreakdown.isEmpty())
    }

    @Test
    fun `weekly summary totalVolume sums every set across all workouts`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(
                set(weight = 100.0, reps = 5),
                set(weight = 100.0, reps = 5),
            )))),
            workout(exercises = listOf(loggedExercise("bn", "Bench", listOf(
                set(weight = 60.0, reps = 8),
            )))),
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        assertEquals(1000.0 + 480.0, summary.totalVolume, 0.0)
        assertEquals(2, summary.workoutCount)
        assertEquals((1000.0 + 480.0) / 2, summary.averageVolumePerWorkout, 0.0)
    }

    @Test
    fun `weekly summary merges the same exercise across workouts into one breakdown entry`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 5))))),
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(
                set(weight = 110.0, reps = 5),
                set(weight = 110.0, reps = 5),
            )))),
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        val breakdown = summary.exerciseBreakdown.single()
        assertEquals("sq", breakdown.exerciseId)
        assertEquals(500.0 + 1100.0, breakdown.totalVolume, 0.0)
        assertEquals(3, breakdown.setCount)
    }

    @Test
    fun `weekly summary setCount includes sets with null weight or reps`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(
                set(weight = 100.0, reps = 5),
                set(weight = null, reps = null),
                set(weight = 100.0, reps = null),
            ))))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        val breakdown = summary.exerciseBreakdown.single()
        assertEquals(3, breakdown.setCount)
        assertEquals(500.0, breakdown.totalVolume, 0.0)
    }

    @Test
    fun `weekly summary averageWeight is the mean of non-null weights`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(loggedExercise("sq", "Squat", listOf(
                set(weight = 100.0, reps = 5),
                set(weight = 120.0, reps = 5),
                set(weight = null, reps = 5),     // excluded from averageWeight
            ))))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        assertEquals(110.0, summary.exerciseBreakdown.single().averageWeight!!, 0.0)
    }

    @Test
    fun `weekly summary averageWeight is null when no sets have weight`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(loggedExercise("plank", "Plank", listOf(set(secs = 60)))))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val summary = repo.getWeeklyVolumeSummary("2026-04-01").first()

        assertNull(summary.exerciseBreakdown.single().averageWeight)
    }

    // -- getMuscleGroupVolumeDistribution --

    @Test
    fun `muscle distribution for a single-muscle exercise gives 100 percent to that muscle`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(
                loggedExercise("bn", "Bench", listOf(set(weight = 100.0, reps = 5)), muscles = listOf(MuscleGroup.CHEST))
            ))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val dist = repo.getMuscleGroupVolumeDistribution("2026-04-01", "2026-04-07").first()

        val chest = dist.single()
        assertEquals(MuscleGroup.CHEST, chest.muscleGroup)
        assertEquals(500.0, chest.totalVolume, 0.0)
        assertEquals(100.0, chest.percentage, 0.0)
        assertEquals(1, chest.exerciseCount)
    }

    @Test
    fun `muscle distribution splits volume evenly across an exercise's target muscles`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(
                loggedExercise("bn", "Bench", listOf(set(weight = 100.0, reps = 5)),
                    muscles = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS))
            ))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val dist = repo.getMuscleGroupVolumeDistribution("2026-04-01", "2026-04-07").first()

        assertEquals(2, dist.size)
        assertEquals(250.0, dist.first { it.muscleGroup == MuscleGroup.CHEST }.totalVolume, 0.0)
        assertEquals(250.0, dist.first { it.muscleGroup == MuscleGroup.TRICEPS }.totalVolume, 0.0)
        assertEquals(50.0, dist.first { it.muscleGroup == MuscleGroup.CHEST }.percentage, 0.0)
        assertEquals(50.0, dist.first { it.muscleGroup == MuscleGroup.TRICEPS }.percentage, 0.0)
    }

    @Test
    fun `muscle distribution results are sorted by totalVolume descending`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(
                loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 5)),
                    muscles = listOf(MuscleGroup.QUADS)),                      // 500
                loggedExercise("bn", "Bench", listOf(set(weight = 100.0, reps = 3)),
                    muscles = listOf(MuscleGroup.CHEST)),                      // 300
            ))
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val dist = repo.getMuscleGroupVolumeDistribution("2026-04-01", "2026-04-07").first()

        assertEquals(MuscleGroup.QUADS, dist[0].muscleGroup)
        assertEquals(MuscleGroup.CHEST, dist[1].muscleGroup)
    }

    @Test
    fun `muscle distribution exerciseCount dedupes the same exercise across workouts`() = runTest {
        val workouts = listOf(
            workout(exercises = listOf(
                loggedExercise("sq", "Squat", listOf(set(weight = 100.0, reps = 5)),
                    muscles = listOf(MuscleGroup.QUADS))
            )),
            workout(exercises = listOf(
                loggedExercise("sq", "Squat", listOf(set(weight = 110.0, reps = 5)),
                    muscles = listOf(MuscleGroup.QUADS))
            )),
        )
        val repo = repo(dateRangeWorkouts = workouts)

        val dist = repo.getMuscleGroupVolumeDistribution("2026-04-01", "2026-04-07").first()

        assertEquals(1, dist.single().exerciseCount)
        assertEquals(500.0 + 550.0, dist.single().totalVolume, 0.0)
    }

    @Test
    fun `muscle distribution empty workouts yield an empty list`() = runTest {
        val repo = repo(dateRangeWorkouts = emptyList())

        val dist = repo.getMuscleGroupVolumeDistribution("2026-04-01", "2026-04-07").first()

        assertTrue(dist.isEmpty())
    }

    // -- getPersonalRecordProgress --

    @Test
    fun `PR progress returns null when exercise has no PRs`() = runTest {
        val repo = repo(prs = emptyList())

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertNull(progress)
    }

    @Test
    fun `PR progress with a single PR reports NEW_PR and no improvement value`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "p1", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-04-10", weight = 100.0, reps = 5)
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertNotNull(progress)
        assertEquals(PRImprovementType.NEW_PR, progress!!.improvementType)
        assertNull(progress.improvement)
        assertEquals("p1", progress.currentPR.id)
        assertNull(progress.previousPR)
    }

    @Test
    fun `PR progress picks the most recent PR as current`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "p1", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-03-01", weight = 95.0, reps = 5),
            pr(id = "p3", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-04-10", weight = 110.0, reps = 5),
            pr(id = "p2", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-03-20", weight = 100.0, reps = 5),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals("p3", progress!!.currentPR.id)
        assertEquals("p2", progress.previousPR!!.id)
    }

    @Test
    fun `PR progress MAX_WEIGHT_FOR_REPS with heavier current reports WEIGHT_INCREASE delta`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "prev", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-03-01", weight = 100.0, reps = 5),
            pr(id = "cur",  type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-04-01", weight = 115.0, reps = 5),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals(PRImprovementType.WEIGHT_INCREASE, progress!!.improvementType)
        assertEquals(15.0, progress.improvement!!, 0.0)
    }

    @Test
    fun `PR progress MAX_WEIGHT_FOR_REPS with lighter current reports NO_IMPROVEMENT`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "prev", type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-03-01", weight = 115.0, reps = 5),
            pr(id = "cur",  type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-04-01", weight = 100.0, reps = 5),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals(PRImprovementType.NO_IMPROVEMENT, progress!!.improvementType)
        assertEquals(0.0, progress.improvement!!, 0.0)
    }

    @Test
    fun `PR progress MAX_REPS_AT_WEIGHT reports REP_INCREASE as a Double delta`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "prev", type = PRType.MAX_REPS_AT_WEIGHT, date = "2026-03-01", weight = 80.0, reps = 5),
            pr(id = "cur",  type = PRType.MAX_REPS_AT_WEIGHT, date = "2026-04-01", weight = 80.0, reps = 8),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals(PRImprovementType.REP_INCREASE, progress!!.improvementType)
        assertEquals(3.0, progress.improvement!!, 0.0)
    }

    @Test
    fun `PR progress DURATION reports DURATION_INCREASE as a Double second delta`() = runTest {
        val repo = repo(prs = listOf(
            pr(id = "prev", type = PRType.DURATION, date = "2026-03-01", durationSecs = 60),
            pr(id = "cur",  type = PRType.DURATION, date = "2026-04-01", durationSecs = 90),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals(PRImprovementType.DURATION_INCREASE, progress!!.improvementType)
        assertEquals(30.0, progress.improvement!!, 0.0)
    }

    @Test
    fun `PR progress tolerates a PR row with an unparseable date`() = runTest {
        // Bogus-date PR is treated as LocalDate.MIN → never wins currentPR, never wins previousPR.
        // The real valid row is selected.
        val repo = repo(prs = listOf(
            pr(id = "bogus", type = PRType.MAX_WEIGHT_FOR_REPS, date = "not-a-date", weight = 200.0, reps = 5),
            pr(id = "cur",   type = PRType.MAX_WEIGHT_FOR_REPS, date = "2026-04-01", weight = 100.0, reps = 5),
        ))

        val progress = repo.getPersonalRecordProgress("sq").first()

        assertEquals("cur", progress!!.currentPR.id)
    }

    // -- getExercisePerformanceTrend --

    @Test
    fun `trend returns INSUFFICIENT_DATA and the unknown-exercise label when no workouts exist`() = runTest {
        val repo = repo(workoutsWithExercise = emptyList())

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.INSUFFICIENT_DATA, trend.trendDirection)
        assertEquals("Unknown Exercise", trend.exerciseName)
        assertTrue(trend.dataPoints.isEmpty())
        assertEquals("More data needed for trend analysis.", trend.recommendedAction)
    }

    @Test
    fun `trend returns INSUFFICIENT_DATA with fewer than three valid dataPoints`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-08", weight = 105.0, reps = 1),
        )
        val repo = repo(workoutsWithExercise = workouts)

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.INSUFFICIENT_DATA, trend.trendDirection)
        assertEquals(2, trend.dataPoints.size)
    }

    @Test
    fun `trend slope between 0_5 and 2_0 resolves to SLIGHTLY_IMPROVING`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-08", weight = 101.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-15", weight = 102.0, reps = 1),
        )
        val repo = repo(workoutsWithExercise = workouts)

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.SLIGHTLY_IMPROVING, trend.trendDirection)
        assertEquals("Steady improvement. Keep up the consistent training.", trend.recommendedAction)
    }

    @Test
    fun `trend slope above 2_0 resolves to STRONGLY_IMPROVING`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-08", weight = 103.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-15", weight = 106.0, reps = 1),
        )
        val repo = repo(workoutsWithExercise = workouts)

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.STRONGLY_IMPROVING, trend.trendDirection)
    }

    @Test
    fun `trend with flat 1RM values resolves to STABLE`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-08", weight = 100.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-15", weight = 100.0, reps = 1),
        )
        val repo = repo(workoutsWithExercise = workouts)

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.STABLE, trend.trendDirection)
    }

    @Test
    fun `trend with steep descending slope resolves to STRONGLY_DECLINING`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 106.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-08", weight = 103.0, reps = 1),
            workoutWithExercise(exId = "sq", date = "2026-04-15", weight = 100.0, reps = 1),
        )
        val repo = repo(workoutsWithExercise = workouts)

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(TrendDirection.STRONGLY_DECLINING, trend.trendDirection)
    }

    @Test
    fun `trend for a non-bodyweight exercise carries externalWeight as bestWeight and null bodyweight`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1, bodyweight = 80.0)
        )
        val repo = repo(workoutsWithExercise = workouts, exerciseInfo = masterExercise("sq", usesBodyweight = false))

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        val point = trend.dataPoints.single()
        assertEquals(100.0, point.bestWeight!!, 0.0)
        assertEquals(100.0, point.externalWeight!!, 0.0)
        assertFalse(point.usesBodyweight)
        assertNull(point.bodyweight)
        assertEquals(100.0, point.estimated1RM!!, 0.0)
    }

    @Test
    fun `trend for a bodyweight exercise adds user bodyweight to bestWeight and 1RM`() = runTest {
        val workouts = listOf(
            workoutWithExercise(exId = "pu", date = "2026-04-01", weight = 20.0, reps = 1, bodyweight = 80.0)
        )
        val repo = repo(workoutsWithExercise = workouts, exerciseInfo = masterExercise("pu", usesBodyweight = true))

        val trend = repo.getExercisePerformanceTrend("pu").first()!!

        val point = trend.dataPoints.single()
        assertEquals(100.0, point.bestWeight!!, 0.0)       // 20 external + 80 bodyweight
        assertEquals(20.0, point.externalWeight!!, 0.0)
        assertEquals(80.0, point.bodyweight!!, 0.0)
        assertTrue(point.usesBodyweight)
        assertEquals(100.0, point.estimated1RM!!, 0.0)
    }

    @Test
    fun `trend skips workouts that do not include the target exercise`() = runTest {
        val targeted = workoutWithExercise(exId = "sq", date = "2026-04-01", weight = 100.0, reps = 1)
        val untargeted = workoutWithExercise(exId = "bn", date = "2026-04-05", weight = 60.0, reps = 5)
        val repo = repo(workoutsWithExercise = listOf(targeted, untargeted))

        val trend = repo.getExercisePerformanceTrend("sq").first()!!

        assertEquals(1, trend.dataPoints.size)
        assertEquals("sq", trend.dataPoints.single().exerciseId)
    }

    // -- fixtures --

    private fun repo(
        currentWorkouts: List<LoggedWorkout> = emptyList(),
        previousWorkouts: List<LoggedWorkout> = emptyList(),
        activeCycle: ActiveProgramCycle? = null,
        dateRangeWorkouts: List<LoggedWorkout> = emptyList(),
        prs: List<PersonalRecord> = emptyList(),
        workoutsWithExercise: List<LoggedWorkout> = emptyList(),
        exerciseInfo: Exercise? = null,
    ): AnalyticsRepository {
        val loggedDao: LoggedWorkoutDao = mockk(relaxed = true) {
            every { getWorkoutsByCycle("cur") } returns flowOf(currentWorkouts)
            every { getWorkoutsByCycle("prev") } returns flowOf(previousWorkouts)
            every { getWorkoutsByDateRange(any(), any()) } returns flowOf(dateRangeWorkouts)
            every { getRecentWorkoutsForPRAnalysis(any()) } returns flowOf(emptyList())
            every { getAllWorkoutsWithExercise(any()) } returns flowOf(workoutsWithExercise)
        }
        val cycleDao: ActiveCycleDao = mockk(relaxed = true) {
            every { getActiveCycle() } returns flowOf(activeCycle)
        }
        val prDao: PersonalRecordDao = mockk(relaxed = true) {
            every { getPRsForExerciseFlow(any()) } returns flowOf(prs)
        }
        val exDao: ExerciseDao = mockk(relaxed = true) {
            every { getExerciseById(any()) } returns exerciseInfo
        }
        return AnalyticsRepository(loggedDao, cycleDao, prDao, exDao)
    }

    private fun masterExercise(id: String, usesBodyweight: Boolean) = Exercise(
        id = id,
        name = id,
        usesBodyweight = usesBodyweight,
        targetMuscleGroups = emptyList(),
        equipment = emptyList(),
    )

    private fun workoutWithExercise(
        exId: String,
        date: String,
        weight: Double,
        reps: Int,
        bodyweight: Double? = null,
    ) = workout(
        exercises = listOf(loggedExercise(exId, exId, listOf(set(weight = weight, reps = reps)))),
    ).let {
        // workout() hardcodes date = "2026-04-22"; override the date + bodyweight for trend tests.
        it.copy(date = date, bodyweight = bodyweight)
    }

    private fun pr(
        id: String,
        type: PRType,
        date: String,
        weight: Double? = null,
        reps: Int? = null,
        durationSecs: Int? = null,
    ) = PersonalRecord(
        id = id,
        exerciseId = "sq",
        exerciseName = "Squat",
        date = date,
        loggedWorkoutId = "w-$id",
        type = type,
        weightUnit = "kg",
        reps = reps,
        weight = weight,
        durationSecs = durationSecs,
    )

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
        muscles: List<MuscleGroup> = emptyList(),
    ) = LoggedExercise(
        id = "le-$exerciseId-${UUID.randomUUID()}",
        exerciseId = exerciseId,
        exerciseName = name,
        targetMuscleGroups = muscles,
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
