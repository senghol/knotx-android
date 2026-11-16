package com.senghol.knotx.calling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senghol.knotx.calling.CallContainer

class CallViewModelFactory(
    private val callContainer: CallContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            return CallViewModel(
                callManager = callContainer.callManager,
                callSignalingManager = callContainer.callSignalingManager,
                callAudioManager = callContainer.callAudioManager,
                callCameraManager = callContainer.callCameraManager,
                webRtcManager = callContainer.webRtcManager,
                callMediaManager = callContainer.callMediaManager,
                callParticipantRepository = callContainer.callParticipantRepository,
                callPermissionManager = callContainer.callPermissionManager,
                startCallService = callContainer::startCallService
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}