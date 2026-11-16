package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
@Dao
interface DakiyaSettingDao {

    // Insert a setting into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: DakiyaSetting)

    // Get a setting by its key
    @Query("SELECT * FROM DakiyaSetting WHERE settingKey = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): DakiyaSetting?

    // Get all settings
    @Query("SELECT * FROM DakiyaSetting")
    suspend fun getAllSettings(): List<DakiyaSetting>

    // Update a setting by its key
    @Update
    suspend fun updateSetting(setting: DakiyaSetting)

    // Delete a setting by its key
    @Query("DELETE FROM DakiyaSetting WHERE settingKey = :key")
    suspend fun deleteSettingByKey(key: String)

    // Delete all settings
    @Query("DELETE FROM DakiyaSetting")
    suspend fun deleteAllSettings()
}

