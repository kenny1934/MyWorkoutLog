package com.example.myworkoutlog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    // We use "upsert" (update or insert) because when you set a new PR,
    // it should replace the old one of the same type for that exercise.
    @Upsert
    fun upsert(pr: PersonalRecord)

    @Query("SELECT * FROM personal_record_table WHERE exerciseId = :exerciseId")
    fun getPRsForExercise(exerciseId: String): List<PersonalRecord>

    @Query("SELECT * FROM personal_record_table ORDER BY date DESC")
    fun getAllPRs(): Flow<List<PersonalRecord>>
}