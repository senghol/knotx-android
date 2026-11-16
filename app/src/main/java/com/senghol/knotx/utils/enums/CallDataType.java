package com.senghol.knotx.utils.enums;

public enum CallDataType {
    OFFER(0),
    ANSWER(1),
    BUSY_ON_ANOTHER_CALL(2),
    VIDEO_INITIATING(3),
    AUDIO_INITIATING(4),
    VIDEO_RECEIVED(5),
    AUDIO_RECEIVED(6),
    CALL_RESPONSE(7),
    END_CALL(8),
    ICE(9),
    START_VIDEO(10),
    START_AUDIO(11),
    ACK(12),
    REJECTED(13),
    CALL_REACHED_SUCCESSFULLY(14);
    private final int value;
    CallDataType(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    // Static method to get enum by int value
    public static CallDataType fromInt(int i) {
        for (CallDataType type : CallDataType.values()) {
            if (type.getValue() == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unexpected value: " + i);
    }
}
