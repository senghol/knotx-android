package com.senghol.knotx.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.senghol.knotx.utils.enums.FileDataType

@Entity(tableName = "unsentFiles")
data class UnsentFiles(
    @PrimaryKey val messageId: String,  // Using messageId as the primary key

    @ColumnInfo(name = "receiver")
    val receiver: String,  // Also NOT NULL
    val data: String,
    val dataType: FileDataType, // Enum for different data types
    val time: Long = System.currentTimeMillis(),
    val isGroup: Boolean = false
)
