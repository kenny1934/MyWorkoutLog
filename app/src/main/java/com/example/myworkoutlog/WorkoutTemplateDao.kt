package com.example.myworkoutlog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(template: WorkoutTemplate)

    @Update
    fun update(template: WorkoutTemplate)

    @Query("SELECT * FROM workout_template_table ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>

    @Query("DELETE FROM workout_template_table WHERE id = :templateId")
    fun deleteById(templateId: String)
}