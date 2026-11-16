package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.senghol.knotx.utils.enums.SandeshStithi

@Entity(tableName = "group_sandesh")
data class GroupSandesh(
        @PrimaryKey val sandeshId: String,
        val userNumber: String,
        val groupId: String,
        val samagri: String,
        val isSelf: Boolean ,
        val time: Long,
        val sandeshType: Int,
        val sandeshStithi: SandeshStithi,
)
