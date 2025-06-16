package com.example.myworkoutlog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedWorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(loggedWorkout: LoggedWorkout)

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
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId = :cycleId ORDER BY date ASC")
    fun getWorkoutsByCycle(cycleId: String): Flow<List<LoggedWorkout>>
    
    // Get all unique cycle IDs that have logged workouts
    @Query("SELECT DISTINCT activeProgramCycleId FROM logged_workout_table WHERE activeProgramCycleId IS NOT NULL ORDER BY activeProgramCycleId")
    fun getAllCycleIds(): Flow<List<String>>
    
    // Get workouts that don't belong to any cycle (orphaned workouts)
    @Query("SELECT * FROM logged_workout_table WHERE activeProgramCycleId IS NULL ORDER BY date DESC")
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
}