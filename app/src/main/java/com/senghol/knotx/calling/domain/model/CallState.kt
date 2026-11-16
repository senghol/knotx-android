package com.senghol.knotx.calling.domain.model

enum class CallState {
    IDLE,
    INITIATING,
    RINGING,
    ACCEPTING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ENDING,
    ENDED,
    REJECTED,
    CANCELLED,
    BUSY,
    MISSED,
    FAILED
}