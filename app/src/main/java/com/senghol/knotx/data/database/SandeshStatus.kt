package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sandeshStatus")
data class SandeshStatus(
    @PrimaryKey val sandeshId: String,  // ✅ Unique for each message
    val userNumber: String,
    val statusJson: String  // ✅ Stores JSON as String
)
