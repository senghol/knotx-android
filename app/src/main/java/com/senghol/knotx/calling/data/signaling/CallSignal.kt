package com.senghol.knotx.calling.data.signaling

import com.senghol.knotx.calling.domain.model.CallId
import com.senghol.knotx.calling.domain.model.CallMediaType
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CallSignal {
    val type: CallSignalType
    val callId: String
    val sender: String
}


@Serializable
@SerialName("CALL_INVITE")
data class CallInviteSignal(
    override val callId: String,
    override val sender: String,
    val receiver: String,
    val callType: CallType,
    val mediaTypes: Set<CallMediaType> = setOf(CallMediaType.AUDIO),
    val participantIds: List<String> = emptyList()
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_INVITE
}

@Serializable
@SerialName("CALL_RINGING")
data class CallRingingSignal(
    override val callId: String,
    override val sender: String,
    val receiver: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_RINGING
}


@Serializable
@SerialName("CALL_ACCEPT")
data class CallAcceptSignal(
    override val callId: String,
    override val sender: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_ACCEPT
}

@Serializable
@SerialName("CALL_REJECT")
data class CallRejectSignal(
    override val callId: String,
    override val sender: String,
    val reason: String? = null
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_REJECT
}

@Serializable
@SerialName("CALL_CANCEL")
data class CallCancelSignal(
    override val callId: String,
    override val sender: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_CANCEL
}

@Serializable
@SerialName("CALL_BUSY")
data class CallBusySignal(
    override val callId: String,
    override val sender: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_BUSY
}
@Serializable
@SerialName("CALL_END")
data class CallEndSignal(
    override val callId: String,
    override val sender: String,
    val reason: String? = null
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_END
}

@Serializable
@SerialName("ADD_PARTICIPANT")
data class AddParticipantSignal(
    override val callId: String,
    override val sender: String,
    val participant: CallParticipant
) : CallSignal {
    override val type: CallSignalType = CallSignalType.ADD_PARTICIPANT
}


@Serializable
@SerialName("REMOVE_PARTICIPANT")
data class RemoveParticipantSignal(
    override val callId: String,
    override val sender: String,
    val participantId: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.REMOVE_PARTICIPANT
}

@Serializable
@SerialName("PARTICIPANT_JOINED")
data class ParticipantJoinedSignal(
    override val callId: String,
    override val sender: String,
    val participant: CallParticipant
) : CallSignal {
    override val type: CallSignalType = CallSignalType.PARTICIPANT_JOINED
}


@Serializable
@SerialName("PARTICIPANT_LEFT")
data class ParticipantLeftSignal(
    override val callId: String,
    override val sender: String,
    val participantId: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.PARTICIPANT_LEFT
}

@Serializable
@SerialName("WEBRTC_OFFER")
data class WebRtcOfferSignal(
    override val callId: String,
    override val sender: String,
    val target: String,
    val sdp: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.WEBRTC_OFFER
}


@Serializable
@SerialName("WEBRTC_ANSWER")
data class WebRtcAnswerSignal(
    override val callId: String,
    override val sender: String,
    val target: String,
    val sdp: String
) : CallSignal {
    override val type: CallSignalType = CallSignalType.WEBRTC_ANSWER
}

@Serializable
@SerialName("ICE_CANDIDATE")
data class IceCandidateSignal(
    override val callId: String,
    override val sender: String,
    val target: String,
    val candidate: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int
) : CallSignal {
    override val type: CallSignalType = CallSignalType.ICE_CANDIDATE
}

@Serializable
@SerialName("CALL_FAILED")
data class CallFailedSignal(
    override val callId: String,
    override val sender: String,
    val reason: String? = null
) : CallSignal {
    override val type: CallSignalType = CallSignalType.CALL_FAILED
}

@Serializable
data class CallSignalEnvelope(
    val type: CallSignalType,
    val callId: String,
    val sender: String,
    val receiver: String? = null,
    val payload: String? = null,
    val timestamp: Long
)