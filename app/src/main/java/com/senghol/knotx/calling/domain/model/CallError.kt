package com.senghol.knotx.calling.domain.model

sealed interface CallError {
    data object PermissionDenied : CallError
    data object NetworkUnavailable : CallError
    data object SignalingDisconnected : CallError
    data object ConnectionFailed : CallError
    data object Timeout : CallError
    data object Busy : CallError
    data object CallAlreadyActive : CallError
    data object CameraUnavailable : CallError
    data object MicrophoneUnavailable : CallError
    data object IceConnectionFailed : CallError
    data object WebRtcInitializationFailed : CallError
    data object CallNotFound : CallError
    data object NotAuthorized : CallError
    data object Unknown : CallError
}