package com.example.myworkoutlog

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveCycleDao {
    // Since there's only ever one active cycle, we replace it when starting a new one.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setActiveCycle(cycle: ActiveProgramCycle)

    @Query("SELECT * FROM active_program_cycle_table WHERE id = 1")
    fun getActiveCycle(): Flow<ActiveProgramCycle?>

    @Query("DELETE FROM active_program_cycle_table")
    fun clear() // To end a cycle
}