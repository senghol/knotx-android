package com.senghol.knotx.calling.ui.incoming

import com.senghol.knotx.calling.domain.model.CallParticipantUi
import com.senghol.knotx.calling.domain.model.CallType

data class IncomingCallUiState(
    val callId: String? = null,
    val participant: CallParticipantUi? = null,
    val callType: CallType? = null,
    val isAccepting: Boolean = false,
    val isRejecting: Boolean = false
)