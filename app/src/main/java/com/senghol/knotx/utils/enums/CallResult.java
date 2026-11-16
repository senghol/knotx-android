package com.senghol.knotx.utils.enums;

public enum CallResult {
    PICKED(0),        // Call was answered and completed
    MISSED(1),        // Call rang, but was never answered
    DECLINED(2),      // Call was actively rejected
    CANCELED(3),      // Caller canceled before pickup
    FAILED(4),        // Call failed due to network issues
    BUSY(5),          // Receiver was on another call
    NO_ANSWER(6),     // Call rang but was ignored (not actively declined)
    DISCONNECTED(7),  // Call was dropped due to connection issues
    ENDED(8);
    private final int value;

    CallResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CallResult fromValue(int value) {
        for (CallResult outcome : values()) {
            if (outcome.value == value) {
                return outcome;
            }
        }
        throw new IllegalArgumentException("Unknown CallOutcome value: " + value);
    }
}

