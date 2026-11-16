package com.senghol.knotx.ui.viewmodel

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.Connection
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.senghol.knotx.utils.data.CallEvent
import com.senghol.knotx.utils.data.CallUiState
import com.senghol.knotx.utils.enums.CallState
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.R
import com.senghol.knotx.calling.domain.model.CallStatus
import com.senghol.knotx.connection.Knotx

import com.senghol.knotx.ui.calling.AudioRouter
import com.senghol.knotx.ui.calling.CallActivity2
//import com.senghol.knotx.ui.calling.AudioRouteManager
import com.senghol.knotx.ui.calling.CallSignalBus

import com.senghol.knotx.ui.calling.CallWebRTC
import com.senghol.knotx.ui.calling.ConnectionDootaws
import com.senghol.knotx.ui.calling.EglUtils.eglBase
import com.senghol.knotx.utils.data.AudioDeviceDakiya
import com.senghol.knotx.utils.data.CallUpbhogta
import com.senghol.knotx.utils.enums.AudioOutputDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

import com.senghol.knotx.utils.enums.CallDataType
import com.senghol.knotx.utils.enums.CallResult
import com.senghol.knotx.utils.enums.DataType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.objects.OutgoingTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import java.util.UUID
import java.util.concurrent.Executors

private const val MAX_CONCURRENT_SENDS = 5 // Max number of messages waiting for ACK at any time
private const val RESEND_TIMEOUT_MS = 700L // Timeout for waiting for an ACK before resending
private const val SEND_LOOP_POLL_DELAY_MS = 50L // How often the sending loop checks for work


class CallViewModel(
    application: Application,  // pass Application instead of Context
    private val knotx: Knotx,
    val roomId: String
) : AndroidViewModel(application) {

    private val _callStatus = MutableStateFlow(CallStatus.CONNECTING)
    val callStatus = _callStatus.asStateFlow()

    fun setRinging() {
        _callStatus.value = CallStatus.RINGING
        val context = getApplication<Application>().applicationContext
        OutgoingTone.switchToRinging(context)
    }
    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState


    private val _callUpbhogta = MutableStateFlow<CallUpbhogta?>(null)
    val callUpbhogta = _callUpbhogta.asStateFlow()

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private lateinit var callWebRTC: CallWebRTC
    private var signalingJob: Job? = null
    private var callTimerJob: Job? = null
    private var callResult: CallResult = CallResult.NO_ANSWER



    init {
        
        CallSignalBus.startObserving(viewModelScope) { json ->
            handleIncomingSignal(JsonParser.parseString(json).asJsonObject)
        }
    }


    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    data class SignalMessage(
        val id: String,
        val type: CallDataType,
        val payload: Any
    )

    private fun sendAck(id: String) {
        val ackJson = JsonObject().apply {
            addProperty(JsonKeys.DATA_TYPE, DataType.CALL_DATA.value)
            addProperty(JsonKeys.CALL_DATA_TYPE, CallDataType.ACK.value)
            addProperty(JsonKeys.RECEIVER, _uiState.value.remoteUserNumber)
            addProperty(JsonKeys.MESSAGE_ID, id)
            addProperty(JsonKeys.PROFILE_ID, roomId)
            addProperty(JsonKeys.SANDESH, "ACK DATA")
        }
        
        CoroutineScope(Dispatchers.IO).launch {
//            knotx.writeJsonToSessionCall(ackJson)
        }
    }

    private val processedSignalIds = mutableSetOf<String>()

    private fun handleIncomingSignal(json: JsonObject) {
        

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val callType = json[JsonKeys.CALL_DATA_TYPE]?.asInt
                val id = json[JsonKeys.MESSAGE_ID]?.asString

                if (id == null || callType == null) {
                    
                    return@launch
                }

                // 📨 Send ACK for every valid, new message (except ACK itself)
                if (callType != CallDataType.ACK.value) {
                    sendAck(id)
                }

                // 🛑 Skip already processed signals early
                if (callType != CallDataType.ACK.value && !processedSignalIds.add(id)) {
                    
                    return@launch
                }

                when (callType) {
                    CallDataType.OFFER.value -> {
                        
                        initCallWebRTC()

                        callResult = CallResult.PICKED
                        val sdp = json[JsonKeys.SANDESH]?.asString
                        sdp?.let {
                            callWebRTC.setRemoteDescription(
                                SessionDescription(SessionDescription.Type.OFFER, it)
                            ) {
                                
                                for (ice in pendingIceCandidates) {
                                    
                                    callWebRTC.addIceCandidate(ice)
                                }
                                pendingIceCandidates.clear()
                                callWebRTC.createAnswer()
                            }
                        }
                    }

                    CallDataType.ANSWER.value -> {
                        callResult = CallResult.PICKED
                        val sdp = json[JsonKeys.SANDESH]?.asString
                        
                        sdp?.let {
                            callWebRTC.setRemoteDescription(
                                SessionDescription(SessionDescription.Type.ANSWER, it)
                            )
                        }
                    }

                    CallDataType.ICE.value -> {
                        val iceJson = json[JsonKeys.SANDESH]?.asJsonObject
                        

                        val sdpMid = iceJson?.get("sdpMid")?.asString
                        val sdpMLineIndex = iceJson?.get("sdpMLineIndex")?.asInt
                        val candidate = iceJson?.get("candidate")?.asString

                        
                        
                        
                        

                        if (sdpMid != null && sdpMLineIndex != null && candidate != null) {
                            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                            if (::callWebRTC.isInitialized && callWebRTC.isPeerReady) {
                                
                                callWebRTC.addIceCandidate(iceCandidate)
                            } else {
                                
                                pendingIceCandidates.add(iceCandidate)
                            }
                        } else {
                            
                        }
                    }

                    CallDataType.ACK.value -> {
                        // ✅ Remove from in-flight messages safely
                        val ackId = json[JsonKeys.MESSAGE_ID]?.asString
                        if (ackId != null) {
                            withContext(Dispatchers.IO) {
                                sendMutex.withLock {
                                    if (inFlightMessages.remove(ackId) != null) {
                                        
                                    }
                                }
                            }
                        }
                    }

                    CallDataType.END_CALL.value -> {
                            endCall()
                    }
                    CallDataType.REJECTED.value -> {
                            endCall()
                    }

                    CallDataType.BUSY_ON_ANOTHER_CALL.value -> {
                        _uiState.update { current ->
                            current.copy(callState = CallState.BusyOnAnotherCall)
                        }
                    }

                    CallDataType.CALL_REACHED_SUCCESSFULLY.value -> {
                        setRinging()

                    }
                    else -> {
                        
                    }
                }

            } catch (e: Exception) {
                
            }
        }
    }


    fun onEvent(event: CallEvent) {
        when (event) {
            is CallEvent.ToggleMic -> toggleMic()
            is CallEvent.ToggleCamera -> toggleCamera()
            is CallEvent.SetAudioOutput -> setAudioOutputDevice(event.deviceId) // 🔄 NEW
            is CallEvent.SwitchCamera -> switchCamera()
            is CallEvent.EndCall -> {
                endCall()
                sendSignal(CallDataType.END_CALL, "CALL ENDED BY USER")
            }
            is CallEvent.AcceptCall -> acceptCall()
            is CallEvent.RejectCall -> rejectCall()
            CallEvent.RefreshDevices -> refreshAudioDevicesUI()
        }
    }


    fun initCallUiState(
        remoteNumber: String,
        isVideo: Boolean,
        isOutgoing: Boolean
    ) {
        viewModelScope.launch {
            val userName = knotx.getUserNameFromAll(remoteNumber)
            val profileUrl = knotx.getUserProfileImageUrlFromAll(remoteNumber)

            _uiState.update { current ->
                current.copy(
                    callState = if (isOutgoing) CallState.Calling else CallState.Ringing,
                    isVideoCall = isVideo,
                    remoteUserName = userName,
                    remoteUserNumber = remoteNumber,
                    remoteUserProfileUrl = profileUrl
                )
            }
        }

    }

    fun startOutgoingTelecom(
        calleeName: String?,
        isVideoCall: Boolean?,
        userId: String?,
        context: Context
    ) {
        val telecomManager =
            context.getSystemService(TELECOM_SERVICE) as? TelecomManager
                ?: return

        // 🔐 Permission check (MANDATORY)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("VOIP", "CALL_PHONE permission not granted — cannot place call")
            // Optionally trigger permission request here
            return
        }

        val phoneAccountHandle = PhoneAccountHandle(
            ComponentName(context, ConnectionDootaws::class.java),
            "DootawasVoIPAccount"
        )

        val uri = Uri.fromParts("tel", userId ?: "", null)

        val callExtras = Bundle().apply {
            putString("CALLER_NAME", calleeName ?: "Unknown")
            putString("ROOM_ID", roomId)
            putBoolean("IS_VIDEO_CALL", isVideoCall == true)
            putString("USER_ID", userId ?: "")
        }

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, callExtras) // 🔥 IMPORTANT
        }

        try {
            telecomManager.placeCall(uri, extras)
        } catch (se: SecurityException) {
            // 👈 Explicit handling (Lint is happy now)
            Log.e("VOIP", "SecurityException: CALL_PHONE missing", se)
        } catch (e: Exception) {
            Log.e("VOIP", "❌ Telecom placeCall failed", e)
        }

    }
    private var isOutgoingCall = false

    fun startOutgoingCall(isVideo: Boolean, remoteUser: String, remoteNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isOutgoingCall = true
            val context = getApplication<Application>().applicationContext

            val myNumber = knotx.userNumberKhud.firstOrNull()
            if (myNumber != null && myNumber == remoteNumber) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "You can't call yourself 😅", Toast.LENGTH_SHORT).show()
                }
                endCall()
                return@launch
            }

            if (Build.VERSION.SDK_INT >= 34) {
                registerEndpointCallback()   // ✅ register BEFORE placeCall so first callback is caught
                startOutgoingTelecom(remoteUser, isVideo, remoteNumber, context)
            } else {
                withContext(Dispatchers.Main) {
                    audioRouter = AudioRouter(context).apply {
                        onDeviceListChanged = { refreshAudioDevicesUI() }
                        init(isVideoCall = _uiState.value.isVideoCall)
                    }
                }
                refreshAudioDevicesUI()
            }

            knotx.waitUntilParmaanTrueOrEndCall()
            if (!knotx.isParmaan) {
                withContext(Dispatchers.Main) { endCall() }
                return@launch
            }

            val callType = if (isVideo) CallDataType.VIDEO_INITIATING else CallDataType.AUDIO_INITIATING
            knotx.createCallSession(
                receiver = remoteNumber,
                roomId = roomId,
                callDataType = callType
            )
        }
    }

    fun receiveIncomingCall(remoteUser: String, isVideo: Boolean, remoteNumber: String) {
        callResult = CallResult.MISSED

        _uiState.update {
            it.copy(
                callState = CallState.Ringing,
                isVideoCall = isVideo,
                remoteUserName = remoteUser,
                remoteUserNumber = remoteNumber

            )
        }

        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            if (Build.VERSION.SDK_INT >= 34) {
                registerEndpointCallback()   // ✅ register early for incoming too
            } else {
                withContext(Dispatchers.Main) {
                    audioRouter = AudioRouter(context).apply {
                        onDeviceListChanged = { refreshAudioDevicesUI() }
                        init(isVideoCall = isVideo)
                    }
                }
                refreshAudioDevicesUI()
            }

            withContext(Dispatchers.IO) {
                val callType = if (isVideo) CallDataType.VIDEO_RECEIVED else CallDataType.AUDIO_RECEIVED
                knotx.createCallSession(
                    receiver = remoteNumber,
                    roomId = roomId,
                    callDataType = callType,
                    outcome = callResult
                )
            }
        }
    }
    @RequiresApi(34)
    private fun CallEndpoint.toAudioDevice(): AudioDeviceDakiya {
        return AudioDeviceDakiya(
            id = identifier.hashCode(), // or keep String separately if you want
            type = when (endpointType) {
                CallEndpoint.TYPE_SPEAKER -> AudioOutputDevice.SPEAKER
                CallEndpoint.TYPE_EARPIECE -> AudioOutputDevice.EARPIECE
                CallEndpoint.TYPE_BLUETOOTH -> AudioOutputDevice.BLUETOOTH
                CallEndpoint.TYPE_WIRED_HEADSET -> AudioOutputDevice.WIRED_HEADSET
                else -> AudioOutputDevice.UNKNOWN
            },
            name = endpointName.toString()
        )
    }

    private fun refreshAudioDevicesUI() {
        if (Build.VERSION.SDK_INT >= 34) return  // handled by callback now
        val router = audioRouter ?: return
        val devices = router.getAvailableDevices()
        val current = router.getCurrentDevice()
        _uiState.update {
            it.copy(availableAudioDevices = devices, audioOutputDevice = current)
        }
    }


    var isOfferer = false
    private fun acceptCall() {
        viewModelScope.launch {


            callResult = CallResult.PICKED
            val conn = ConnectionDootaws.currentConnection
            if (conn == null) {
                Log.w("VOIP", "No active connection to answer")
                return@launch
            }
            conn.onSilence()




            _uiState.update { it.copy(callState = CallState.Connecting) }

            if (Build.VERSION.SDK_INT <= 34) {
                prepareLegacyAudio(
                    getApplication(),
                    _uiState.value.isVideoCall
                )
            }
            initCallWebRTC()
            knotx.waitUntilParmaanTrueOrEndCall()


            if (!knotx.isParmaan) {
                    endCall()
                return@launch // Stop if still not true
            }


            callWebRTC.createOffer()
            isOfferer = true
        }
    }


    private var isOnHold = false
        private set // only we can change it

    private fun toggleHold() {
        val conn = ConnectionDootaws.currentConnection ?: return

        if (isOnHold) {
            conn.onUnhold()
        } else {
            conn.onHold()
        }
        // No need to manually flip isOnHold — do it in the state listener instead
    }

    // Call this from UI (reject button)
    fun rejectCall() {
        callResult = CallResult.DECLINED
        ConnectionDootaws.currentConnection?.onReject()
        endCall()
        sendSignal(CallDataType.END_CALL, "CALL ENDED BY USER")
        knotx.rejectCall(_uiState.value.remoteUserNumber?:"")
    }


    // Call this when user presses End Call, or remote ends call
    fun endCall() {
        viewModelScope.launch(Dispatchers.IO) {


            cancelCallNotification()
            delay(500)
            finalizeCallDuration()
            roomId.let { roomId ->
                knotx.updateCallOutcome(roomId, callResult,_uiState.value.remoteUserNumber,_uiState.value.isVideoCall,isOutgoingCall)
            }

            // 2. UI state
            _uiState.update { it.copy(callState = CallState.Ended) }

            // 3. Timer
            // 3. Timer
            callTimerJob?.cancel()
            callTimerJob = null

            // 4. WebRTC tracks & ICE
            clearTracks()
            pendingIceCandidates.clear()

            // 5. Signaling cleanup
            pendingSignals.clear()
            stopSendingLoop()
            audioRouter?.release()
            audioRouter = null
            // 6. Release WebRTC client safely
            if (::callWebRTC.isInitialized) {
                callWebRTC.releaseSafe()
            }

            // 7. Telecom / Manual Connection — THE SAFE WAY
            ConnectionDootaws.currentConnection?.let { conn ->
                if (conn.state == Connection.STATE_RINGING) {
                    conn.onReject()
                } else {
                    conn.onDisconnect()
                }
            }
            ConnectionDootaws.clearEndpointChangeCallback()
            // 8. Reset companion object — critical!
            ConnectionDootaws.clearAll()

        }
    }

    fun reconnectCall() {
        

        stopSendingLoop()
        callTimerJob?.cancel()

        pendingIceCandidates.clear()
        pendingSignals.clear()
        if (::callWebRTC.isInitialized) {
            callWebRTC.releaseSafe()
        }
        initCallWebRTC() // Recreate peer connection
        if (isOfferer) {
            callWebRTC.createOffer()
        }
    }


    private fun toggleMic() {
        val newMuted = !_uiState.value.isMicMuted

        _uiState.update { it.copy(isMicMuted = newMuted) }

        audioRouter?.muteMic(newMuted)

        if (::callWebRTC.isInitialized) {
            callWebRTC.toggleMic(enabled = !newMuted)
        } else {
            Log.w("CallVM", "toggleMic ignored: WebRTC not initialized")
        }
    }
    private fun toggleCamera() {
        val newEnabled = !_uiState.value.isCameraOn

        _uiState.update { it.copy(isCameraOn = newEnabled) }

        if (::callWebRTC.isInitialized) {
            callWebRTC.toggleCamera(newEnabled)
        } else {
            Log.w("CallVM", "toggleCamera ignored: WebRTC not initialized")
        }
    }

    private fun toggleSpeaker() {
        _uiState.update { it.copy(isSpeakerOn = !_uiState.value.isSpeakerOn) }
        // Optional: Update audio route here
    }

    private fun switchCamera() {
        if (::callWebRTC.isInitialized) {
            callWebRTC.switchCamera { isFront ->
                _uiState.update { it.copy(isFrontCamera = isFront) }
            }
        } else {
            Log.w("CallVM", "switchCamera ignored: WebRTC not initialized")
        }
    }

    private fun clearTracks() {
        localVideoTrack?.dispose()
        remoteVideoTrack?.dispose()
        localVideoTrack = null
        remoteVideoTrack = null
        _uiState.update {
            it.copy(localVideoTrack = null, remoteVideoTrack = null)
        }
    }
    private var audioRouter: AudioRouter? = null
    fun setAudioOutputDevice(type: AudioOutputDevice) {
        if (Build.VERSION.SDK_INT >= 34) {
            val endpointType = when (type) {
                AudioOutputDevice.SPEAKER      -> CallEndpoint.TYPE_SPEAKER
                AudioOutputDevice.EARPIECE     -> CallEndpoint.TYPE_EARPIECE
                AudioOutputDevice.BLUETOOTH    -> CallEndpoint.TYPE_BLUETOOTH
                AudioOutputDevice.WIRED_HEADSET -> CallEndpoint.TYPE_WIRED_HEADSET
                else -> return
            }
            val conn = ConnectionDootaws.currentConnection ?: return
            val endpoint = ConnectionDootaws.getAvailableEndpoints()
                .firstOrNull { it.endpointType == endpointType }
                ?: run {
                    Log.w("VOIP", "Requested endpoint $type not available")
                    return
                }

            conn.requestCallEndpointChange(
                endpoint,
                Executors.newSingleThreadExecutor(),
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) {
                        // ✅ Only update UI after confirmed success
                        _uiState.update { it.copy(audioOutputDevice = type) }
                    }
                    override fun onError(error: CallEndpointException) {
                        Log.e("VOIP", "Failed to route audio to $type", error)
                        // Optionally re-sync UI with actual current state
                        refreshAudioDevicesUI()
                    }
                }
            )
        } else {
            // Legacy: route through AudioRouter, not conn.setAudioRoute
            val router = audioRouter ?: run {
                Log.w("VOIP", "AudioRouter not initialized")
                return
            }
            router.setOutputDevice(type)  // your AudioRouter should wrap AudioManager calls
            // Synchronous — safe to update UI immediately
            _uiState.update { it.copy(audioOutputDevice = type) }
        }
    }

    // Utility to map Android type to enum
    private fun mapAudioDeviceType(type: Int): AudioOutputDevice {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputDevice.BLUETOOTH

            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioOutputDevice.WIRED_HEADSET

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputDevice.SPEAKER

            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> AudioOutputDevice.EARPIECE

            else -> AudioOutputDevice.UNKNOWN
        }
    }
    private fun initCallWebRTC() {
        val context = getApplication<Application>().applicationContext
        callWebRTC = CallWebRTC(
            context = context,
            isVideoCall = _uiState.value.isVideoCall,
            eglBaseContext = eglBase.eglBaseContext,
            signalingListener = object : CallWebRTC.SignalingListener {
                override fun onOfferCreated(sdp: SessionDescription) {
                    
                    callWebRTC.setLocalDescription(sdp) // ✅ Correct use
                    sendSignal(CallDataType.OFFER, sdp.description)
                }

                override fun onAnswerCreated(sdp: SessionDescription) {
                    
                    callWebRTC.setLocalDescription(sdp) // ✅ Correct use
                    sendSignal(CallDataType.ANSWER, sdp.description)
                }


                override fun onIceCandidateGenerated(candidate: IceCandidate) {
                    val remoteNumber = _uiState.value.remoteUserNumber
                    if (remoteNumber.isNullOrBlank()) {
                        
                        return
                    }
                    
                    val iceJson = JsonObject().apply {
                        addProperty("sdpMid", candidate.sdpMid)
                        addProperty("sdpMLineIndex", candidate.sdpMLineIndex)
                        addProperty("candidate", candidate.sdp)
                    }
                    sendSignal(CallDataType.ICE, iceJson)
                }

                override fun onRemoteStreamAvailable() {}
                override fun onRemoteTrackReceived(track: VideoTrack) {
                    remoteVideoTrack = track
                    _uiState.update { it.copy(remoteVideoTrack = track) }
                }

                override fun onLocalTrackCreated(track: VideoTrack) {
                    localVideoTrack = track
                    _uiState.update { it.copy(localVideoTrack = track) }
                }

                override fun onCallConnected() {
                    _uiState.update { it.copy(callState = CallState.InCall) }


                        OutgoingTone.stop()
                        val conn = ConnectionDootaws.currentConnection
                        if (Build.VERSION.SDK_INT >= 34) {
                            if (conn != null && conn.state != Connection.STATE_ACTIVE) {
//                                conn.setActive()   // 🔥 THIS FIXES ANDROID 11 / 16 AUDIO
                            }
                            if (_uiState.value.isVideoCall) {
                                conn?.onAnswer(VideoProfile.STATE_BIDIRECTIONAL)
                            } else {
                                conn?.onAnswer()
                            }
                        }
//                    applyInitialAudioRoute(_uiState.value.isVideoCall)
                    startCallTimer()
                }
                override fun onCallEnded() {
                    endCall()
                }
                override fun onCallDisconnectedTemporarily() {
                    reconnectCall()
                }
            }

        )
        pendingSignals.clear()
        processedSignalIds.clear()
        // 🔥 Add these calls to fully initialize WebRTC:
        callWebRTC.initPeerConnectionFactory()
        callWebRTC.createPeerConnection()

        callWebRTC.startLocalMedia(
            isMicEnabled = !_uiState.value.isMicMuted
        )
    }

    // Call this after the connection is created (e.g. end of initCallWebRTC or in startOutgoingCall)
    private fun registerEndpointCallback() {
        if (Build.VERSION.SDK_INT < 34) return

        ConnectionDootaws.setEndpointChangeCallback { endpoints, current ->
            val devices = endpoints.map { it.toAudioDevice() }
            val currentType = current?.toAudioDevice()?.type ?: AudioOutputDevice.UNKNOWN
            _uiState.update {
                it.copy(
                    availableAudioDevices = devices,
                    audioOutputDevice = currentType
                )
            }
        }

        // ✅ Seed UI immediately with whatever is already available
        // (covers the race where endpoints arrived before callback was registered)
        val existing = ConnectionDootaws.getAvailableEndpoints()
        if (existing.isNotEmpty()) {
            val devices = existing.map { it.toAudioDevice() }
            val currentType = ConnectionDootaws.getCurrentEndpoint()?.toAudioDevice()?.type
                ?: AudioOutputDevice.UNKNOWN
            _uiState.update {
                it.copy(
                    availableAudioDevices = devices,
                    audioOutputDevice = currentType
                )
            }
        }
    }
    private fun prepareLegacyAudio(context: Context, isVideo: Boolean) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        am.isMicrophoneMute = false
        @Suppress("DEPRECATION")
        am.isSpeakerphoneOn = isVideo
        am.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
        )
    }

    private var sendingJob: Job? = null
    private val sendMutex = Mutex()
    private val pendingSignals = ArrayDeque<SignalMessage>() // Queue of signals to be sent

    // Stores messages that have been sent and are awaiting ACK. Key: messageId
    private val inFlightMessages = mutableMapOf<String, SignalMessage>()
    private var isSendingActive = false

    // Public function to send a signal
    fun sendSignal(type: CallDataType, data: Any) { // Made public for example usage
        queueSignal(type, data)
        // Ensure the sending loop is started if it's not already
        if (sendingJob?.isActive != true) {
            isSendingActive = true
            startSendingLoop()
        }
    }

    private fun queueSignal(type: CallDataType, payload: Any) {
        val id = UUID.randomUUID().toString()
        val message = SignalMessage(id, type, payload)
        CoroutineScope(Dispatchers.IO).launch { // Use a common scope or inject one
            sendMutex.withLock {
                pendingSignals.addLast(message)
                
            }
            // If the loop is active, it will pick it up.
            // Optionally, could notify or "tickle" the sending loop here if it might be waiting on an empty queue.
        }
    }



    private fun stopSendingLoop() {
        isSendingActive = false // Signal loops to stop
        sendingJob?.cancel()    // Cancel the main sending loop
        sendingJob = null
        CoroutineScope(Dispatchers.IO).launch { // Use a common scope or inject one
            sendMutex.withLock {
                
                inFlightMessages.clear()
                // pendingSignals.clear() // Optional: clear signals that were never even sent
            }
        }
    }

    // Coroutine to process the sending queue
    private fun startSendingLoop() {

        if (sendingJob?.isActive == true) return // Already running

        sendingJob = CoroutineScope(Dispatchers.IO).launch { // Use a common scope or inject one
            
            while (isSendingActive) {
                var sentThisCycle = false
                sendMutex.withLock {
                    // Check if we can send more signals (not exceeding concurrency limit and queue is not empty)
                    if (inFlightMessages.size < MAX_CONCURRENT_SENDS && pendingSignals.isNotEmpty()) {
                        val msg = pendingSignals.removeFirst()
                        inFlightMessages[msg.id] = msg // Add to in-flight messages
                        
                        actuallySendSignal(msg)
                        sentThisCycle = true
                    }
                }
                // Delay to prevent tight loop, potentially longer if nothing was sent
                delay(if (sentThisCycle) SEND_LOOP_POLL_DELAY_MS / 2 else SEND_LOOP_POLL_DELAY_MS)
            }
            
        }
    }
    private var postEndSendCount = 0

    private fun isCallAlive(): Boolean {
        return _uiState.value.callState != CallState.Ended
    }

    private fun cleanupSignaling(reason: String) {
        

        isSendingActive = false

        CoroutineScope(Dispatchers.IO).launch {
            sendMutex.withLock {
                inFlightMessages.clear()
                pendingSignals.clear()
            }
        }
    }

    private val resendCounts = mutableMapOf<String, Int>()

    // Actual send function that writes to the network
    private fun actuallySendSignal(msg: SignalMessage) {
        // Construct the JSON payload
        val json = JsonObject().apply {
            addProperty(JsonKeys.DATA_TYPE, DataType.CALL_DATA.value)
            addProperty(JsonKeys.CALL_DATA_TYPE, msg.type.value)
            addProperty(JsonKeys.RECEIVER, _uiState.value.remoteUserNumber)
            addProperty(JsonKeys.MESSAGE_ID, msg.id)
            addProperty(JsonKeys.PROFILE_ID,roomId)
            when (val data = msg.payload) {
                is String -> addProperty(JsonKeys.SANDESH, data)
                is JsonObject -> add(JsonKeys.SANDESH, data)
                else -> {
                    
                    // If payload is unsupported, we should remove it from inFlightMessages
                    // to prevent an infinite resend loop.
                    CoroutineScope(Dispatchers.IO).launch {
                        sendMutex.withLock {
                            inFlightMessages.remove(msg.id)
                        }
                    }
                    // Potentially notify about the failure
                    return // Do not attempt to send or set up resend timer
                }
            }
        }

        
        // Start a resend timer for this specific message
        CoroutineScope(Dispatchers.IO).launch {

            sendMutex.withLock {
                val count = (resendCounts[msg.id] ?: 0) + 1
                resendCounts[msg.id] = count

                if (count > 10) {
                    
                    cleanupSignaling("resend limit exceeded for ${msg.id}")
                    return@launch
                }

                
            }

//            knotx.writeJsonToSessionCall(json)

            delay(RESEND_TIMEOUT_MS)

            if (!isSendingActive || !isCallAlive()) return@launch

            sendMutex.withLock {
                if (inFlightMessages.containsKey(msg.id)) {
                    
                    actuallySendSignal(msg)
                }
            }
        }
    }

    private var isDurationFinalized = false // 👈 Prevents duplicate writes

    fun finalizeCallDuration() {
        if (isDurationFinalized) return
        isDurationFinalized = true
        val finalDuration = _uiState.value.callDuration

        viewModelScope.launch {
            knotx.updateCallDuration(roomId, finalDuration)
        }
    }


    private fun startCallTimer() {
        callTimerJob = viewModelScope.launch {
            var localDuration = _uiState.value.callDuration

            updateCallNotification(localDuration) // 🔥 call once here

            while (true) {
                delay(1000)
                localDuration += 1
                _uiState.update { it.copy(callDuration = localDuration) }

                if (localDuration % 5 == 0L) {
                    launch(Dispatchers.IO) {
                        try {
                            knotx.updateCallDuration(roomId, localDuration)
                        } catch (e: Exception) {}
                    }
                }
            }
        }
    }

    private var callStartTimeMs: Long = 0L

    private fun updateCallNotification(seconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val app = getApplication<Application>()

        // 🔥 Only set start time once
        if (callStartTimeMs == 0L) {
            callStartTimeMs = System.currentTimeMillis() - (seconds * 1000)
        }

        val openIntent = PendingIntent.getActivity(
            app, 0,
            Intent(app, CallActivity2::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(app, CALL_TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_call)
            .setContentTitle(_uiState.value.remoteUserName)
            .setContentText("Knotx Call")
            .setWhen(callStartTimeMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .build()

        NotificationManagerCompat.from(app)
            .notify(CALL_TIMER_NOTIFICATION_ID, notification)
    }

    fun cancelCallNotification() {
        NotificationManagerCompat.from(getApplication())
            .cancel(CALL_TIMER_NOTIFICATION_ID)
    }

    companion object {
        const val CALL_TIMER_NOTIFICATION_ID = 9001
        const val CALL_TIMER_CHANNEL_ID = "call_timer_channel"
    }

    override fun onCleared() {
        signalingJob?.cancel()
        callTimerJob?.cancel()
    }
}
