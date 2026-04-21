package com.kennychiu.myworkoutlog.data

import com.kennychiu.myworkoutlog.ui.*
import com.kennychiu.myworkoutlog.viewmodel.*
import com.kennychiu.myworkoutlog.util.*
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveCycleDao {
    // Since there's only ever one active cycle, we replace it when starting a new one.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setActiveCycle(cycle: ActiveProgramCycle)

    @Query("SELECT * FROM active_program_cycle_table WHERE id = 1")
    fun getActiveCycle(): Flow<ActiveProgramCycle?>

    // Snapshot read from IO thread — used where a Flow subscription would be overkill
    // (progression hint refresh on workout load).
    @Query("SELECT * FROM active_program_cycle_table WHERE id = 1")
    fun getActiveCycleSnapshot(): ActiveProgramCycle?

    @Query("DELETE FROM active_program_cycle_table")
    fun clear() // To end a cycle

    @Query("UPDATE active_program_cycle_table SET userCycleName = :newName WHERE id = 1")
    fun renameActiveCycle(newName: String): Int
}