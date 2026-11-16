package com.senghol.knotx.calling.domain.model

data class CallSession(
    val callId: CallId,
    val callType: CallType,
    val direction: CallDirection,
    val state: CallState,
    val participants: List<CallParticipant>,
    val activeMediaTypes: Set<CallMediaType> = setOf(CallMediaType.AUDIO),
    val createdAt: Long,
    val startedAt: Long? = null,
    val answeredAt: Long? = null,
    val endedAt: Long? = null,
    val endReason: CallEndReason? = null
)