package com.senghol.knotx.calling.domain.manager

import android.util.Log
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class CallCameraManager @Inject constructor(
    private val webRtcManager: WebRtcManager
) {
    val isCameraEnabled: StateFlow<Boolean> =
        webRtcManager.localVideoEnabled

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> =
        _isFrontCamera.asStateFlow()

    fun initializeCamera(): Boolean {
        return webRtcManager.createLocalVideoTrack() != null
    }

    fun setCameraEnabled(enabled: Boolean): Boolean {
        val caller = Throwable().stackTrace
            .firstOrNull {
                !it.className.contains("CallCameraManager") &&
                        !it.className.contains("Thread")
            }


        val current = webRtcManager.localVideoEnabled.value
        val trackExists = webRtcManager.getLocalVideoTrack() != null


        if (enabled && !trackExists) {

            if (!initializeCamera()) {
                Log.e(
                    "CameraToggle",
                    "setCameraEnabled: initializeCamera() FAILED"
                )
                return false
            }

        }


        webRtcManager.setVideoEnabled(enabled)


        return true
    }


    fun switchCamera() {
        webRtcManager.switchCamera { isFrontCamera ->
            _isFrontCamera.value = isFrontCamera
        }
    }

    fun getLocalVideoTrack() =
        webRtcManager.getLocalVideoTrack()

    fun reset() {
        webRtcManager.setVideoEnabled(false)
        _isFrontCamera.value = true
    }
}