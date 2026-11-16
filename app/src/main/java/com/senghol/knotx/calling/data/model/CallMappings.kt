package com.senghol.knotx.calling.data.model


import com.senghol.knotx.calling.domain.model.CallId
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState

import com.senghol.knotx.calling.data.signaling.CallParticipantPayload
import com.senghol.knotx.data.sqldelight.Call_participant
import com.senghol.knotx.data.sqldelight.Call_session
//import comsengholkaleshdatasqldelight.Call_participant
//import comsengholkaleshdatasqldelight.Call_session

// ============================================================
// SQLDELIGHT <-> DOMAIN
// ============================================================

fun Call_session.toDomain(
    participants: List<Call_participant>
): CallSession {
    return CallSession(
        callId = CallId(callId),
        callType = CallType.valueOf(callType),
        direction = CallDirection.valueOf(direction),
        state = CallState.valueOf(state),
        participants = participants.map { it.toDomain() },
        createdAt = createdAt,
        startedAt = startedAt,
        answeredAt = answeredAt,
        endedAt = endedAt,
        endReason = endReason?.let { CallEndReason.valueOf(it) }
    )
}

fun Call_participant.toDomain(): CallParticipant {
    return CallParticipant(
        participantId = participantId,
        userNumber = userNumber,
        userNameId = userNameId,

        // Profile information is resolved separately from Contact/Upbhogta.
        displayName = null,
        profileUrl = null,

        connectionState = ParticipantConnectionState.valueOf(connectionState),
        isMicrophoneEnabled = isMicrophoneEnabled != 0L,
        isCameraEnabled = isCameraEnabled != 0L,
        isScreenSharing = isScreenSharing != 0L,
        joinedAt = joinedAt,
        leftAt = leftAt
    )
}

// ============================================================
// CALL PARTICIPANT <-> PAYLOAD
// ============================================================

fun CallParticipant.toPayload() = CallParticipantPayload(
    participantId = participantId,
    userNumber = userNumber,
    userNameId = userNameId,
    displayName = displayName,
    profileUrl = profileUrl
)

fun CallParticipantPayload.toDomain(
    defaultState: ParticipantConnectionState = ParticipantConnectionState.INVITED
) = CallParticipant(
    participantId = participantId ?: "",
    userNumber = userNumber ?: "",
    userNameId = userNameId,
    displayName = displayName,
    profileUrl = profileUrl,
    connectionState = defaultState
)