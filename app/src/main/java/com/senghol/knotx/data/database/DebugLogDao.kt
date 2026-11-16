package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DebugLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DebugLogEntity)

    @Query("SELECT * FROM debug_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<DebugLogEntity>

    @Query("DELETE FROM debug_logs WHERE timestamp < :cutoff")
    suspend fun deleteOldLogs(cutoff: Long)

    @Query("DELETE FROM debug_logs")
    suspend fun clearAll()
}