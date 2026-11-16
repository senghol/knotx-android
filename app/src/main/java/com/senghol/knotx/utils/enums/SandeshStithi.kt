package com.senghol.knotx.utils.enums

enum class SandeshStithi(val value: Int) {
    LIKHIT(0),
    PREKSHIT(1),
    SAMARPIT(2),
    DHRIST(3);

    companion object {
        private val map = entries.associateBy { it.value }

        fun fromInt(value: Int): SandeshStithi = map[value] ?: LIKHIT
    }
}
