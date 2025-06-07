package com.example.myworkoutlog // Your package name

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// @Dao marks this as a Data Access Object
@Dao
interface ExerciseDao {

    // @Insert tells Room this function inserts data.
    // OnConflictStrategy.IGNORE means if we try to insert an exercise
    // with an ID that already exists, just ignore it.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(exercise: Exercise): Long

    // @Query lets us write our own database commands.
    // This query gets all exercises from the table, ordered by name.
    // 'Flow' is a special type that automatically emits new data when the
    // database changes. Our UI will observe this Flow to stay up-to-date.
    @Query("SELECT * FROM exercise_table ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>
}