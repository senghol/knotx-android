package com.senghol.knotx.calling.ui.active

import com.senghol.knotx.calling.domain.manager.CallAudioRoute
import com.senghol.knotx.calling.domain.model.CallMediaType
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import org.webrtc.MediaStream

data class ActiveCallUiState(
    val callId: String? = null,
    val callState: CallState = CallState.IDLE,
    val participants: List<CallParticipant> = emptyList(),
    val remoteStreams: Map<String, MediaStream> = emptyMap(),
    val activeMediaTypes: Set<CallMediaType> = emptySet(),
    val isMicrophoneEnabled: Boolean = true,
    val isCameraEnabled: Boolean = false,
    val isSpeakerEnabled: Boolean = false,
    val audioRoute: CallAudioRoute = CallAudioRoute.EARPIECE,
    val isFrontCamera: Boolean = true,
    val callDurationSeconds: Long = 0L,
    val callType: CallType? = null,
)