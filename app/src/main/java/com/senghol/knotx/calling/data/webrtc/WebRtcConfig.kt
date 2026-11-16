package com.senghol.knotx.calling.data.webrtc

data class WebRtcConfig(
    val iceServers: List<IceServerConfig>,
    val enableAudio: Boolean = true,
    val enableVideo: Boolean = true,
    val enableScreenShare: Boolean = true
)

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)