package com.senghol.knotx.utils;

import androidx.room.TypeConverter;

import com.senghol.knotx.utils.enums.CallResult;

public class CallOutcomeConverter {

    @TypeConverter
    public static int fromCallOutcome(CallResult outcome) {
        return outcome != null ? outcome.getValue() : -1; // Default to -1 for null or invalid outcomes
    }

    @TypeConverter
    public static CallResult toCallOutcome(int value) {
        return CallResult.fromValue(value); // Convert back to enum
    }
}
