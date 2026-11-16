package com.senghol.knotx.calling.data.signaling
import com.google.gson.annotations.SerializedName

enum class CallSignalType {
    @SerializedName("CALL_INVITE")
    CALL_INVITE,

    @SerializedName("CALL_RINGING")
    CALL_RINGING,

    @SerializedName("CALL_ACCEPT")
    CALL_ACCEPT,

    @SerializedName("CALL_REJECT")
    CALL_REJECT,

    @SerializedName("CALL_CANCEL")
    CALL_CANCEL,

    @SerializedName("CALL_BUSY")
    CALL_BUSY,

    @SerializedName("CALL_END")
    CALL_END,

    @SerializedName("ADD_PARTICIPANT")
    ADD_PARTICIPANT,

    @SerializedName("REMOVE_PARTICIPANT")
    REMOVE_PARTICIPANT,

    @SerializedName("PARTICIPANT_JOINED")
    PARTICIPANT_JOINED,

    @SerializedName("PARTICIPANT_LEFT")
    PARTICIPANT_LEFT,

    @SerializedName("PARTICIPANT_MEDIA_CHANGED")
    PARTICIPANT_MEDIA_CHANGED,

    @SerializedName("WEBRTC_OFFER")
    WEBRTC_OFFER,

    @SerializedName("WEBRTC_ANSWER")
    WEBRTC_ANSWER,

    @SerializedName("ICE_CANDIDATE")
    ICE_CANDIDATE,


    @SerializedName("CALL_FAILED")
    CALL_FAILED
}