package com.senghol.knotx.utils.enums;
public enum PurchaseOrderType {
    OTHER((byte) 0),
    AI((byte) 1);

    private final byte value;

    PurchaseOrderType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PurchaseOrderType fromValue(byte value) {
        for (PurchaseOrderType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown purchase order type: " + value);
    }
}