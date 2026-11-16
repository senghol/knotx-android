package com.senghol.knotx.calling.domain.model
enum class CallEndReason {
    COMPLETED,
    REJECTED,
    CANCELLED,
    MISSED,
    BUSY,
    TIMEOUT,
    NETWORK_ERROR,
    WEBRTC_ERROR,
    PERMISSION_DENIED,
    REMOTE_ENDED,
    LOCAL_ENDED,
    UNKNOWN
}