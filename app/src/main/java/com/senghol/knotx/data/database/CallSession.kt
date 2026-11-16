package com.senghol.knotx.data.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.senghol.knotx.utils.CallDataTypeConverter
import com.senghol.knotx.utils.CallOutcomeConverter
import com.senghol.knotx.utils.enums.CallDataType
import com.senghol.knotx.utils.enums.CallResult

@Entity(
    tableName = "call_sessions",
    indices = [
        Index(value = ["room_id"], unique = true),
        Index(value = ["receiver"]),
        Index(value = ["start_time"])
    ]
)


@TypeConverters(CallOutcomeConverter::class, CallDataTypeConverter::class)
data class CallSession(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val sender: String,

    val receiver: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "call_data_type")
    val callDataType: CallDataType,

    val outcome: CallResult,

    @ColumnInfo(name = "room_id")
    val roomId: String,

    val duration: Long? = null
)