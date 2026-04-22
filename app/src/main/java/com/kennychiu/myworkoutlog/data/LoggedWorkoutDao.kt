package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedWorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loggedWorkout: LoggedWorkout)

    @androidx.room.Update
    fun updateLoggedWorkout(loggedWorkout: LoggedWorkout)
    
    @Delete
    fun deleteLoggedWorkout(loggedWorkout: LoggedWorkout)
    
    // Alternative delete method by ID for easier use
    @Query("DELETE FROM logged_workout_table WHERE id = :workoutId")
    fun deleteLoggedWorkoutById(workoutId: String)

    // We'll use this function later for the History screen
    @Query("SELECT * FROM logged_workout_table ORDER BY startTimestamp DESC, date DESC")
    fun getAllLoggedWorkouts(): Flow<List<LoggedWorkout>>

    // NEW function to get a single logged workout by its ID
    @Query("SELECT * FROM logged_workout_table WHERE id = :workoutId")
    fun getLoggedWorkoutById(workoutId: String): Flow<LoggedWorkout?>

    // NEW: This query finds the single most recent workout that has a bodyweight recorded.
    @Query("SELECT * FROM logged_workout_table WHERE bodyweight IS NOT NULL ORDER BY date DESC LIMIT 1")
    fun getLatestLoggedWorkoutWithBodyweight(): LoggedWorkout?

    // MESOCYCLE-SPECIFIC QUERIES
    
    // Get all workouts for a specific mesocycle
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId = :cycleId ORDER BY date DESC, startTimestamp DESC")
    fun getWorkoutsByCycle(cycleId: String): Flow<List<LoggedWorkout>>
    
    // Backfill userCycleName snapshot on every workout in a given cycle.
    // Used by the cycle-rename flow so history display picks up the new name.
    @Query("UPDATE logged_workout_table SET userCycleName = :newName WHERE activeProgramCycleId = :cycleId")
    fun renameLoggedWorkoutsByCycle(cycleId: String, newName: String): Int

    // Get all unique cycle IDs that have logged workouts
    @Query("SELECT DISTINCT activeProgramCycleId FROM logged_workout_table WHERE activeProgramCycleId IS NOT NULL ORDER BY activeProgramCycleId")
    fun getAllCycleIds(): Flow<List<String>>
    
    // Get workouts that don't belong to any cycle (orphaned workouts)
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId IS NULL ORDER BY date DESC, startTimestamp DESC")
    fun getOrphanedWorkouts(): Flow<List<LoggedWorkout>>
    
    // Get workouts for a specific program template across all cycles
    @Query("""
        SELECT lw.* FROM logged_workout_table lw 
        INNER JOIN active_program_cycle_table apc ON lw.activeProgramCycleId = apc.id 
        WHERE apc.programTemplateId = :programTemplateId 
        ORDER BY lw.date ASC
    """)
    fun getWorkoutsByProgramTemplate(programTemplateId: String): Flow<List<LoggedWorkout>>
    
    // Get cycle completion statistics
    @Query("""
        SELECT activeProgramCycleId, COUNT(*) as workoutCount 
        FROM logged_workout_table 
        WHERE activeProgramCycleId IS NOT NULL 
        GROUP BY activeProgramCycleId
    """)
    fun getCycleWorkoutCounts(): Flow<List<CycleWorkoutCount>>
    
    // EXERCISE-SPECIFIC QUERIES FOR SMART PRE-FILL
    
    // Get recent workouts containing a specific exercise (for pre-fill suggestions)
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%' 
        ORDER BY date DESC, startTimestamp DESC 
        LIMIT :limit
    """)
    fun getRecentWorkoutsWithExercise(exerciseId: String, limit: Int = 5): Flow<List<LoggedWorkout>>
    
    // Get the most recent workout containing a specific exercise
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%' 
        ORDER BY date DESC, startTimestamp DESC 
        LIMIT 1
    """)
    fun getLatestWorkoutWithExercise(exerciseId: String): LoggedWorkout?
    
    // Get the most recent workout with same template containing a specific exercise
    @Query("""
        SELECT * FROM logged_workout_table
        WHERE workoutTemplateId = :templateId
        AND loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%'
        ORDER BY date DESC, startTimestamp DESC
        LIMIT 1
    """)
    fun getLatestWorkoutWithExerciseInTemplate(exerciseId: String, templateId: String): LoggedWorkout?

    // Most recent workout with a specific exercise whose date is strictly before
    // :beforeDate — used to anchor pre-cycle baselines for forward projection.
    @Query("""
        SELECT * FROM logged_workout_table
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%'
        AND date < :beforeDate
        ORDER BY date DESC, startTimestamp DESC
        LIMIT 1
    """)
    fun getLatestWorkoutWithExerciseBefore(exerciseId: String, beforeDate: String): LoggedWorkout?

    // Candidate workouts for "most recent prior clip of this exercise" — the SQL
    // videoReference match is a substring check against the JSON blob, so a workout
    // that contains the target exercise AND a different exercise with a video will
    // also match. Kotlin-side filtering (findMostRecentSetWithVideo) narrows it down.
    @Query("""
        SELECT * FROM logged_workout_table
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%'
        AND loggedExercises LIKE '%"videoReference":"%'
        AND id != :excludingWorkoutId
        ORDER BY date DESC, startTimestamp DESC
        LIMIT :limit
    """)
    fun getWorkoutsWithExerciseAndVideo(
        exerciseId: String,
        excludingWorkoutId: String,
        limit: Int = 10
    ): Flow<List<LoggedWorkout>>
    
    // Get recent workouts with same template (for session-based context)
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE workoutTemplateId = :templateId 
        ORDER BY date DESC, startTimestamp DESC 
        LIMIT :limit
    """)
    fun getRecentWorkoutsByTemplate(templateId: String, limit: Int = 5): Flow<List<LoggedWorkout>>
    
    // ANALYTICS QUERIES FOR TIER 2 FEATURES
    
    // Get all workouts within a date range for volume analysis
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC, startTimestamp ASC
    """)
    fun getWorkoutsByDateRange(startDate: String, endDate: String): Flow<List<LoggedWorkout>>
    
    // Get workouts containing specific exercise within date range
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE date BETWEEN :startDate AND :endDate 
        AND loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%' 
        ORDER BY date ASC, startTimestamp ASC
    """)
    fun getWorkoutsWithExerciseInDateRange(exerciseId: String, startDate: String, endDate: String): Flow<List<LoggedWorkout>>
    
    // Get all workouts for performance trend analysis (no date limit)
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%' 
        ORDER BY date ASC, startTimestamp ASC
    """)
    fun getAllWorkoutsWithExercise(exerciseId: String): Flow<List<LoggedWorkout>>
    
    // Get workouts grouped by month for volume summaries
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE date LIKE :yearMonth || '%' 
        ORDER BY date ASC, startTimestamp ASC
    """)
    fun getWorkoutsByMonth(yearMonth: String): Flow<List<LoggedWorkout>> // yearMonth format: "2024-01"
    
    // Get workouts for cycle comparison (two specific cycles)
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE activeProgramCycleId IN (:cycleId1, :cycleId2) 
        ORDER BY activeProgramCycleId, date ASC
    """)
    fun getWorkoutsForCycleComparison(cycleId1: String, cycleId2: String): Flow<List<LoggedWorkout>>
    
    // Get all workouts with duration data for average workout time analysis
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE startTimestamp IS NOT NULL AND endTimestamp IS NOT NULL 
        AND activeProgramCycleId = :cycleId 
        ORDER BY date ASC
    """)
    fun getWorkoutsWithDurationByCycle(cycleId: String): Flow<List<LoggedWorkout>>
    
    // Get most recent workouts for each exercise (for PR tracking)
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE loggedExercises LIKE '%"exerciseId":"' || :exerciseId || '"%' 
        ORDER BY date DESC, startTimestamp DESC 
        LIMIT 10
    """)
    fun getRecentWorkoutsForPRAnalysis(exerciseId: String): Flow<List<LoggedWorkout>>
    
    // SIMPLE COUNT QUERIES (using distinct names to avoid conflicts)
    
    @Query("SELECT COUNT(*) FROM logged_workout_table")
    fun totalWorkoutCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM logged_workout_table 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    fun workoutCountBetweenDates(startDate: String, endDate: String): Int
    
    // IN-PROGRESS WORKOUT QUERIES FOR SESSION STATE PERSISTENCE
    
    // Get any in-progress workout for a specific template
    @Query("""
        SELECT * FROM logged_workout_table 
        WHERE workoutTemplateId = :templateId 
        AND isInProgress = 1 
        ORDER BY startTimestamp DESC 
        LIMIT 1
    """)
    fun getInProgressWorkoutForTemplate(templateId: String): LoggedWorkout?
    
    // Get all in-progress workouts (for cleanup/recovery)
    @Query("SELECT * FROM logged_workout_table WHERE isInProgress = 1 ORDER BY startTimestamp DESC")
    fun getAllInProgressWorkouts(): Flow<List<LoggedWorkout>>
    
    // Mark workout as completed (no longer in progress)
    @Query("UPDATE logged_workout_table SET isInProgress = 0 WHERE id = :workoutId")
    fun markWorkoutAsCompleted(workoutId: String): Int
    
    // Clean up old abandoned in-progress workouts (older than 24 hours)
    @Query("""
        UPDATE logged_workout_table 
        SET isInProgress = 0 
        WHERE isInProgress = 1 
        AND startTimestamp < :cutoffTimestamp
    """)
    fun cleanupAbandonedInProgressWorkouts(cutoffTimestamp: Long): Int
}