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
    @Query("SELECT * FROM logged_workout_table ORDER BY date DESC")
    fun getAllLoggedWorkouts(): Flow<List<LoggedWorkout>>

    // NEW function to get a single logged workout by its ID
    @Query("SELECT * FROM logged_workout_table WHERE id = :workoutId")
    fun getLoggedWorkoutById(workoutId: String): Flow<LoggedWorkout?>

    // NEW: This query finds the single most recent workout that has a bodyweight recorded.
    @Query("SELECT * FROM logged_workout_table WHERE bodyweight IS NOT NULL ORDER BY date DESC LIMIT 1")
    fun getLatestLoggedWorkoutWithBodyweight(): LoggedWorkout?
}