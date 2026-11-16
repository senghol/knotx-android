package com.senghol.knotx.utils.converters

import androidx.room.TypeConverter
import com.senghol.knotx.utils.enums.SandeshStithi

class SandeshStithiConverter {
    @TypeConverter
    fun fromInt(value: Int): SandeshStithi {
        return SandeshStithi.fromInt(value)
    }

    @TypeConverter
    fun toInt(sandeshStithi: SandeshStithi): Int {
        return sandeshStithi.value
    }
}
