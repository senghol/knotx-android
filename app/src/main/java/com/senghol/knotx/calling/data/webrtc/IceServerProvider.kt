package com.senghol.knotx.calling.data.webrtc

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IceServerProvider @Inject constructor() {

    fun getIceServers(): List<IceServerConfig> {
        return listOf(
            IceServerConfig(
                urls = listOf(
                    "turn:coturn.api.knotx.com:3478?transport=udp",
                    "turn:coturn.api.knotx.com:3478?transport=tcp"
                ),
                username = "dakiya",
                credential = "dakiyapass"
            )
        )
    }

    fun getConfig(): WebRtcConfig {
        return WebRtcConfig(
            iceServers = getIceServers(),
            enableAudio = true,
            enableVideo = true,
            enableScreenShare = true
        )
    }
}