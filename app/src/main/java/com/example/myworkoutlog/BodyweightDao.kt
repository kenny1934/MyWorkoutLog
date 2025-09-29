package com.example.myworkoutlog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyweightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBodyweightEntry(entry: BodyweightEntry)

    @Update
    fun updateBodyweightEntry(entry: BodyweightEntry)

    @Delete
    fun deleteBodyweightEntry(entry: BodyweightEntry)

    @Query("SELECT * FROM bodyweight_entry_table WHERE date = :date ORDER BY timestamp DESC LIMIT 1")
    fun getBodyweightForDate(date: String): BodyweightEntry?

    @Query("SELECT * FROM bodyweight_entry_table ORDER BY date DESC, timestamp DESC LIMIT 1")
    fun getLatestBodyweightEntry(): BodyweightEntry?

    @Query("SELECT * FROM bodyweight_entry_table ORDER BY date DESC, timestamp DESC")
    fun getAllBodyweightEntries(): Flow<List<BodyweightEntry>>

    @Query("SELECT * FROM bodyweight_entry_table WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, timestamp DESC")
    fun getBodyweightEntriesInRange(startDate: String, endDate: String): Flow<List<BodyweightEntry>>

    @Query("SELECT * FROM bodyweight_entry_table ORDER BY date DESC, timestamp DESC LIMIT :limit")
    fun getRecentBodyweightEntries(limit: Int): Flow<List<BodyweightEntry>>

    @Query("DELETE FROM bodyweight_entry_table WHERE id = :entryId")
    fun deleteBodyweightEntryById(entryId: String)

    // Get the most recent bodyweight before or on a specific date
    @Query("SELECT * FROM bodyweight_entry_table WHERE date <= :date ORDER BY date DESC, timestamp DESC LIMIT 1")
    fun getLatestBodyweightBeforeDate(date: String): BodyweightEntry?
}