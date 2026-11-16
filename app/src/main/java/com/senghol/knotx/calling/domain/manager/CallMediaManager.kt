package com.senghol.knotx.calling.domain.manager

import android.content.Intent
import android.util.Log
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.model.CallMediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallMediaManager @Inject constructor(
    private val webRtcManager: WebRtcManager
) {
    private val _activeMediaTypes = MutableStateFlow<Set<CallMediaType>>(emptySet())
    val activeMediaTypes: StateFlow<Set<CallMediaType>> =
        _activeMediaTypes.asStateFlow()

    val isMicrophoneEnabled: StateFlow<Boolean> =
        webRtcManager.localAudioEnabled

    val isCameraEnabled: StateFlow<Boolean> =
        webRtcManager.localVideoEnabled

    val isScreenSharing: StateFlow<Boolean> =
        webRtcManager.screenSharing

    /** True when ANY video is being sent: camera OR screen. */
    val isSendingVideo: Boolean
        get() = isCameraEnabled.value || isScreenSharing.value

    /**
     * Call ONCE per call, before sending CALL_INVITE (caller) or CALL_ACCEPT
     * (callee). After this returns, the mic track and the (idle) video track
     * exist, so any PeerConnection created later always has both attached.
     * The camera is NOT opened here; CallCameraManager / enableCamera() does that.
     */
    fun prepareLocalMedia() {
        webRtcManager.createLocalAudioTrack()
        webRtcManager.setAudioEnabled(true)
        webRtcManager.ensureLocalVideoTrack()
        _activeMediaTypes.value += CallMediaType.AUDIO
    }

    fun initializeAudio() {
        webRtcManager.createLocalAudioTrack()
        _activeMediaTypes.value += CallMediaType.AUDIO
    }

    fun enableMicrophone() {
        webRtcManager.createLocalAudioTrack()
        webRtcManager.setAudioEnabled(true)
        _activeMediaTypes.value += CallMediaType.AUDIO
    }

    fun disableMicrophone() {
        webRtcManager.setAudioEnabled(false)
        _activeMediaTypes.value -= CallMediaType.AUDIO
    }

    fun toggleMicrophone() {
        if (isMicrophoneEnabled.value) {
            disableMicrophone()
        } else {
            enableMicrophone()
        }
    }

    fun enableCamera() {
        webRtcManager.createLocalVideoTrack()
        webRtcManager.setVideoEnabled(true)
        _activeMediaTypes.value += CallMediaType.VIDEO
    }

    fun disableCamera() {
        webRtcManager.setVideoEnabled(false)
        _activeMediaTypes.value -= CallMediaType.VIDEO
    }

    fun toggleCamera() {
        if (isCameraEnabled.value) {
            disableCamera()
        } else {
            enableCamera()
        }
    }

    /**
     * [permissionData] comes from the system screen-capture dialog. Call this
     * only AFTER CallService is a foreground service of type mediaProjection.
     */
    fun startScreenShare(permissionData: Intent): Boolean {
        val started = webRtcManager.startScreenCapture(permissionData)

        if (started) {
            _activeMediaTypes.value += CallMediaType.SCREEN
        }

        return started
    }

    fun stopScreenShare() {
        webRtcManager.stopScreenCapture()
        _activeMediaTypes.value -= CallMediaType.SCREEN
    }

    suspend fun reset() {
        webRtcManager.dispose()
        _activeMediaTypes.value = emptySet()
    }
}