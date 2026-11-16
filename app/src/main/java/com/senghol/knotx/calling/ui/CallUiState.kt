package com.senghol.knotx.calling.ui

import com.senghol.knotx.calling.domain.manager.CallAudioRoute
import com.senghol.knotx.calling.domain.model.CallError
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import org.webrtc.MediaStream




import com.senghol.knotx.calling.ui.active.ActiveCallUiState
import com.senghol.knotx.calling.ui.ended.EndedCallUiState
import com.senghol.knotx.calling.ui.incoming.IncomingCallUiState


sealed interface CallUiState {
    data object Idle : CallUiState

    data class Incoming(
        val state: IncomingCallUiState
    ) : CallUiState

    data class Active(
        val state: ActiveCallUiState,
        val callType: CallType? = null
    ) : CallUiState

    data class Ended(
        val state: EndedCallUiState
    ) : CallUiState

    data object Finished : CallUiState

    data class Error(
        val message: String? = null
    ) : CallUiState
}