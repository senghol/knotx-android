package com.senghol.knotx.utils;

import androidx.room.TypeConverter;

import com.senghol.knotx.utils.enums.CallDataType;

public class CallDataTypeConverter {

    @TypeConverter
    public static CallDataType toCallDataType(int value) {
        return CallDataType.fromInt(value);
    }

    @TypeConverter
    public static int fromCallDataType(CallDataType type) {
        return type.getValue();
    }
}
