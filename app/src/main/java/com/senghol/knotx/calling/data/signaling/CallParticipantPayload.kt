package com.senghol.knotx.calling.data.signaling

import com.google.gson.annotations.SerializedName

data class CallParticipantPayload(
    @SerializedName("participantId") val participantId: String? = null,
    @SerializedName("userNumber") val userNumber: String? = null,
    @SerializedName("userNameId") val userNameId: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("profileUrl") val profileUrl: String? = null
)