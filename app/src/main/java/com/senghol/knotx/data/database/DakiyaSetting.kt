package com.senghol.knotx.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DakiyaSetting")
data class DakiyaSetting(
    @PrimaryKey
    val settingKey: String,  // Unique key for each setting

    @ColumnInfo(name = "setting_value")
    val settingValue: String  // JSON string to store the setting value (boolean, string, int, etc.)
)
