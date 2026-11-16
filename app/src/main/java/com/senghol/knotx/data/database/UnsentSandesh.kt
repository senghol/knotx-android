package com.senghol.knotx.data.database

import androidx.room.*
import com.senghol.knotx.utils.enums.DataType
import java.time.Instant

@Entity(
    tableName = "unsentSandesh",
    indices = [Index(value = ["messId", "receiver", "dataType"], unique = true)]
)

data class UnsentSandesh(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val messId: String,
    val receiver: String,
    val message: String? = null,
    val dataType: DataType,
    val time: String = Instant.now().toEpochMilli().toString(),
    val dataSent: Boolean = false ,
    val isGroup: Boolean = false
)
