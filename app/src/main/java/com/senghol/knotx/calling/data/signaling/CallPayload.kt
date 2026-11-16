package com.senghol.knotx.calling.data.signaling

import com.google.gson.annotations.SerializedName

data class CallPayload(
    @SerializedName("callType")
    val callType: String? = null,
    @SerializedName("participantId")
    val participantId: String? = null,
    @SerializedName("participant")
    val participant: CallParticipantPayload? = null,
    // Group calls: everyone else already in the call, sent with CALL_INVITE
    // so the invitee knows who to connect to once it accepts.
    @SerializedName("participants")
    val participants: List<CallParticipantPayload>? = null,
    @SerializedName("sdp")
    val sdp: String? = null,
    @SerializedName("candidate")
    val candidate: String? = null,
    @SerializedName("sdpMid")
    val sdpMid: String? = null,
    @SerializedName("sdpMLineIndex")
    val sdpMLineIndex: Int? = null,
    @SerializedName("isMicrophoneEnabled")
    val isMicrophoneEnabled: Boolean? = null,
    @SerializedName("isCameraEnabled")
    val isCameraEnabled: Boolean? = null,
    @SerializedName("isScreenSharing")
    val isScreenSharing: Boolean? = null
)