package com.senghol.knotx.calling.data.webrtc

import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

interface WebRtcEventListener {
    fun onIceCandidate(
        participantId: String,
        candidate: IceCandidate
    )

    fun onRemoteStream(
        participantId: String,
        stream: MediaStream
    )

    fun onConnectionStateChanged(
        participantId: String,
        state: PeerConnection.PeerConnectionState
    )

    fun onIceConnectionStateChanged(
        participantId: String,
        state: PeerConnection.IceConnectionState
    )
}