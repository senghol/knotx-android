package com.senghol.knotx.calling.data.signaling

import com.google.gson.annotations.SerializedName
import com.senghol.knotx.utils.enums.JsonKeys

data class CallData(
    @SerializedName("action")
    val action: CallSignalType,
    @SerializedName("callId")
    val callId: String,
    @SerializedName("sender")
    val sender: String? = null,
    @SerializedName("target")
    val target: String? = null,
    @SerializedName("timeStamp")
    val timeStamp: Long = System.currentTimeMillis(),
    @SerializedName("payload")
    val payload: CallPayload? = null,
    @SerializedName(JsonKeys.REVEAL_SENDER_NUMBER)
    val revealSenderNumber: Boolean? = null,
    @SerializedName(JsonKeys.REVEAL_RECEIVER_NUMBER)
    val revealReceiverNumber: Boolean? = null
)