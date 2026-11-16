package com.senghol.knotx.utils.data

import com.senghol.knotx.utils.enums.AudioOutputDevice
import com.senghol.knotx.utils.enums.CallResult

sealed class CallEvent {
    object ToggleMic : CallEvent()
    object ToggleCamera : CallEvent()
    data class SetAudioOutput(val deviceId: AudioOutputDevice) : CallEvent()
    object SwitchCamera : CallEvent()
    object AcceptCall : CallEvent()
    object RefreshDevices : CallEvent()

    // Call flow ends with an outcome
    data class EndCall(val outcome: CallResult) : CallEvent()
    data class RejectCall(val outcome: CallResult) : CallEvent()
}

