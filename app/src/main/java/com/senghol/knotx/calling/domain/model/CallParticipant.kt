package com.senghol.knotx.calling.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CallParticipant(
    val participantId: String,
    val userNumber: String,
    val userNameId: String?,
    val displayName: String?,
    val profileUrl: String?,
    val connectionState: ParticipantConnectionState = ParticipantConnectionState.INVITED,
    val isMicrophoneEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isScreenSharing: Boolean = false,
    val joinedAt: Long? = null,
    val leftAt: Long? = null
)