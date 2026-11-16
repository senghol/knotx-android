package com.senghol.knotx.calling.domain.model

data class CallParticipantUi(
    val participant: CallParticipant,
    val displayName: String,
    val profileUrl: String?
)