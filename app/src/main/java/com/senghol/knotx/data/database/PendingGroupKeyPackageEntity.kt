package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_group_key_packages",
    primaryKeys = ["groupId", "userNumber"]
)
data class PendingGroupKeyPackageEntity(
    val groupId: String,
    val userNumber: String,
    val messageJson: String,
    val createdAt: Long
)