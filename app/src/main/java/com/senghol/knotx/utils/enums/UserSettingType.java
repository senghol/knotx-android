package com.senghol.knotx.utils.enums;

public enum UserSettingType {
    BALANCE(0),
    THEME(1),
    WALLPAPER(2),
    DISAPPEARING_DURATION(3);

    private final int value;

    UserSettingType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UserSettingType fromInt(int value) {
        for (UserSettingType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return DISAPPEARING_DURATION;
    }
}