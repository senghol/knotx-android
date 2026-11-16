package com.senghol.knotx.utils.data

import com.senghol.knotx.utils.enums.AudioOutputDevice
import com.senghol.knotx.utils.enums.CallState
import org.webrtc.VideoTrack

data class CallUiState(
    val callState: CallState = CallState.Idle,
    val isMicMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isSpeakerOn: Boolean = true,
    val isVideoCall: Boolean = true,
    val isFrontCamera: Boolean = true,
    val remoteUserName: String? = null,
    val remoteUserNumber: String? = null,
    val remoteUserProfileUrl: String? = null,
    val callDuration: Long = 0L,
    val localVideoTrack: VideoTrack? = null,
    val remoteVideoTrack: VideoTrack? = null,

    // 🔄 NEW/UPDATED
    val audioOutputDevice: AudioOutputDevice = AudioOutputDevice.UNKNOWN,
    val audioOutputDeviceId: Int = -1,
    val availableAudioDevices: List<AudioDeviceDakiya> = emptyList()
)
