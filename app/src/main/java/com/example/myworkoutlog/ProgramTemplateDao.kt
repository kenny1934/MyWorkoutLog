package com.example.myworkoutlog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(program: ProgramTemplate)

    @Update
    fun update(program: ProgramTemplate)

    @Query("SELECT * FROM program_template_table ORDER BY name ASC")
    fun getAllPrograms(): Flow<List<ProgramTemplate>>

    @Query("DELETE FROM program_template_table WHERE id = :programId")
    fun deleteById(programId: String)

    @Query("SELECT * FROM program_template_table WHERE id = :programId")
    fun getProgramById(programId: String): Flow<ProgramTemplate?>
}