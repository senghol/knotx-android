package com.senghol.knotx.calling.ui.ended

import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallType

data class EndedCallUiState(
    val callId: String,
    val callType: CallType,
    val participant: CallParticipant,
    val durationSeconds: Long = 0L,
    val endReason: CallEndReason? = null
)