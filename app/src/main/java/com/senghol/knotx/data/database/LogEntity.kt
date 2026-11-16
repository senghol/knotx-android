package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debug_logs")
data class DebugLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long = System.currentTimeMillis(),
    val readableTime: String = getReadableTime(),

    val level: String,
    val tag: String,
    val message: String,

    val msgId: String? = null,
    val userId: String? = null,
    val sessionId: String? = null
)

fun getReadableTime(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}