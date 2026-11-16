package com.senghol.knotx.calling.domain.model

enum class ParticipantConnectionState {
    INVITED,
    RINGING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    DECLINED,

    // Invited into a running call but never answered (per-participant
    // ring timeout). Stored as text in SQLDelight, so no migration needed.
    NO_ANSWER,
    LEFT,
    FAILED
}

/**
 * "Still part of the call, or still expected to be": everything except the
 * states that mean this person is gone for good. Used to decide when the
 * LAST remote has left so the call itself must end.
 */
val ParticipantConnectionState.isAlive: Boolean
    get() = this != ParticipantConnectionState.LEFT &&
            this != ParticipantConnectionState.DECLINED &&
            this != ParticipantConnectionState.NO_ANSWER &&
            this != ParticipantConnectionState.FAILED