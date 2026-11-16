package com.senghol.knotx.calling.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.calling.domain.repository.CallParticipantRepository
import com.senghol.knotx.calling.data.signaling.CallSignalingManager
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.manager.CallAudioDevice
import com.senghol.knotx.calling.domain.manager.CallAudioManager
import com.senghol.knotx.calling.domain.manager.CallAudioRoute
import com.senghol.knotx.calling.domain.manager.CallCameraManager
import com.senghol.knotx.calling.domain.manager.CallManager
import com.senghol.knotx.calling.domain.manager.CallMediaManager
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.permission.CallPermissionManager
import com.senghol.knotx.calling.ui.active.ActiveCallUiState
import com.senghol.knotx.calling.ui.ended.EndedCallUiState
import com.senghol.knotx.calling.ui.incoming.IncomingCallUiState

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import kotlin.time.Duration.Companion.milliseconds

// Mesh topology: every person uploads one stream per peer. 3 remotes = 4 people.
private const val MAX_REMOTE_PARTICIPANTS = 3

data class CallPermissionRequest(
    val participants: List<CallParticipant>,
    val callType: CallType,
    val permissions: List<String>,
    val isAcceptingCall: Boolean = false
)
class CallViewModel(
    private val callManager: CallManager,
    private val callMediaManager: CallMediaManager,
    private val callAudioManager: CallAudioManager,
    private val callCameraManager: CallCameraManager,
    private val callSignalingManager: CallSignalingManager,
    private val webRtcManager: WebRtcManager,
    private val callParticipantRepository: CallParticipantRepository,
    private val startCallService: () -> Unit,
    private val callPermissionManager: CallPermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    private var callTimerJob: Job? = null

    // Media teardown of the previous call. A new call (recall / new invite)
    // must wait for it, otherwise dispose() can destroy tracks the new call
    // has just created.
    private var teardownJob: Job? = null

    val availableAudioDevices: StateFlow<List<CallAudioDevice>> =
        callAudioManager.availableAudioDevices

    val currentAudioRoute: StateFlow<CallAudioRoute> =
        callAudioManager.audioRoute
    val localVideoTrack: VideoTrack?
        get() = webRtcManager.getLocalVideoTrack()

    val videoEglContext: EglBase.Context
        get() = webRtcManager.getEglBaseContext()

    private val _endedCall = MutableStateFlow<EndedCallUiState?>(null)

    // -------------------------------------------------------------------------
    // Participant details (name / photo)
    //
    // CallSession only carries the raw participant (mostly just the number).
    // The incoming screen got name + photo because it called
    // resolveParticipant(); the active and ended screens never did. We resolve
    // once per call, cache it, and merge it into the participants we hand to
    // the UI.
    // -------------------------------------------------------------------------

    private data class ResolvedParticipantInfo(
        val displayName: String?,
        val profileUrl: String?,
        val userNameId: String?
    )

    private val participantInfoCache =
        ConcurrentHashMap<String, ResolvedParticipantInfo>()

    private var participantCacheCallId: String? = null

    private suspend fun resolveInfo(
        participant: CallParticipant
    ): ResolvedParticipantInfo? {
        participantInfoCache[participant.participantId]?.let { return it }

        val ui = runCatching {
            callParticipantRepository.resolveParticipant(participant)
        }.getOrNull()


        if (ui == null) return null

        return ResolvedParticipantInfo(
            displayName = ui.displayName,
            profileUrl = ui.profileUrl,
            userNameId = ui.participant?.userNameId
        ).also { participantInfoCache[participant.participantId] = it }
    }

    private fun CallParticipant.withResolved(): CallParticipant {
        val info = participantInfoCache[participantId] ?: return this

        return copy(
            displayName = info.displayName ?: displayName,
            profileUrl = info.profileUrl ?: profileUrl,
            userNameId = info.userNameId ?: userNameId
        )
    }

    private suspend fun resolvedParticipant(
        participant: CallParticipant
    ): CallParticipant {
        resolveInfo(participant)
        return participant.withResolved()
    }
    private val _openCallScreen = MutableSharedFlow<Unit>()
    val openCallScreen = _openCallScreen.asSharedFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> =
        _callDurationSeconds.asStateFlow()

    private val _callPermissionRequest = MutableStateFlow<CallPermissionRequest?>(null)
    val callPermissionRequest: StateFlow<CallPermissionRequest?> =
        _callPermissionRequest.asStateFlow()
    init {
        callSignalingManager.setOnRemoteCallEndListener {
            handleRemoteCallEnd()
        }
        observeCall()
        observeMedia()
        observeAudio()
        observeRemoteStreams()
        observeCallTimer()
        observeCallEnded()
    }

    /**
     * Calls that end WITHOUT the user pressing end (busy, declined, no answer,
     * connection failed) used to drop to a blank Idle screen. Publish an
     * EndedCallUiState for them so the user sees why.
     * Local end / remote end paths already publish their own, so skip those.
     */
    private fun observeCallEnded() {
        viewModelScope.launch {
            callManager.callEnded.collect { call ->
                if (_endedCall.value?.callId == call.callId.value) return@collect

                val reason = when {
                    call.state == CallState.BUSY ->
                        CallEndReason.BUSY

                    call.state == CallState.REJECTED &&
                            call.direction == CallDirection.OUTGOING ->
                        CallEndReason.REJECTED

                    call.state == CallState.CANCELLED &&
                            call.direction == CallDirection.OUTGOING &&
                            call.endReason == CallEndReason.MISSED ->
                        CallEndReason.MISSED

                    call.state == CallState.FAILED ->
                        CallEndReason.NETWORK_ERROR

                    // ended from the notification / status-bar chip
                    call.state == CallState.ENDED && call.answeredAt != null ->
                        CallEndReason.LOCAL_ENDED

                    // user cancelled, incoming missed/cancelled, normal end:
                    // nothing extra to show
                    else -> return@collect
                }

                val participant = call.participants.firstOrNull() ?: return@collect

                val durationSeconds = call.answeredAt?.let {
                    ((System.currentTimeMillis() - it) / 1000L).coerceAtLeast(0L)
                } ?: 0L

                val ended = EndedCallUiState(
                    callId = call.callId.value,
                    callType = call.callType,
                    participant = resolvedParticipant(participant),
                    durationSeconds = durationSeconds,
                    endReason = reason
                )

                _endedCall.value = ended
                _uiState.value = CallUiState.Ended(ended)
            }
        }
    }

    private fun startCallTimer(startedAt: Long) {
        if (callTimerJob?.isActive == true) return

        callTimerJob = viewModelScope.launch {
            while (true) {
                _callDurationSeconds.value =
                    ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)

                delay(1000L.milliseconds)
            }
        }
    }
    fun requestAcceptCall() {
        val call = callManager.activeCall.value ?: return

        val missingPermissions =
            callPermissionManager.getMissingPermissions(call.callType)

        if (missingPermissions.isEmpty()) {
            acceptCall()
            return
        }

        _callPermissionRequest.value = CallPermissionRequest(
            participants = call.participants,
            callType = call.callType,
            permissions = missingPermissions,
            isAcceptingCall = true
        )
    }
    fun startCallWithPermission(
        participants: List<CallParticipant>,
        callType: CallType
    ) {

        val missingPermissions =
            callPermissionManager.getMissingPermissions(callType)


        if (missingPermissions.isEmpty()) {
            startCall(participants, callType)
            return
        }


        _callPermissionRequest.value = CallPermissionRequest(
            participants = participants,
            callType = callType,
            permissions = missingPermissions
        )
    }


    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
        _callDurationSeconds.value = 0L
    }

    fun onCallPermissionResult(granted: Boolean) {
        val request = _callPermissionRequest.value ?: return
        _callPermissionRequest.value = null

        if (!granted) return

        if (request.isAcceptingCall) {
            acceptCall()
        } else {
            startCall(
                participants = request.participants,
                callType = request.callType
            )
        }
    }
    private fun observeCall() {
        viewModelScope.launch {
            callManager.activeCall.collect { call ->


                if (call != null) {
                    if (call.callId.value != participantCacheCallId) {
                        participantInfoCache.clear()
                        participantCacheCallId = call.callId.value
                    }

                    call.participants.forEach { resolveInfo(it) }
                }

                _uiState.value = when {
                    call == null -> {
                        _endedCall.value?.let {
                            CallUiState.Ended(it)
                        } ?: CallUiState.Idle
                    }

                    call.direction == CallDirection.INCOMING &&
                            call.state == CallState.RINGING -> {
                        _endedCall.value = null

                        val participant = call.participants.firstOrNull()
                        val resolvedParticipant = participant?.let {
                            callParticipantRepository.resolveParticipant(it)
                        }

                        CallUiState.Incoming(
                            IncomingCallUiState(
                                callId = call.callId.value,
                                participant = resolvedParticipant,
                                callType = call.callType
                            )
                        )
                    }

                    call.state in setOf(
                        CallState.ENDED,
                        CallState.REJECTED,
                        CallState.CANCELLED,
                        CallState.BUSY,
                        CallState.MISSED,
                        CallState.FAILED
                    ) -> {
                        _endedCall.value?.let {
                            CallUiState.Ended(it)
                        } ?: CallUiState.Idle
                    }

                    else -> {

                        CallUiState.Active(
                            buildActiveCallUiState(call)
                        )
                    }
                }

            }
        }
    }
    private fun buildActiveCallUiState(
        call: CallSession
    ): ActiveCallUiState {
        return ActiveCallUiState(
            callId = call.callId.value,
            callState = call.state,
            callType = call.callType,
            participants = call.participants.map { it.withResolved() },
            remoteStreams = webRtcManager.remoteStreams.value,
            activeMediaTypes = call.activeMediaTypes,
            isMicrophoneEnabled = callMediaManager.isMicrophoneEnabled.value,
            isCameraEnabled = callMediaManager.isCameraEnabled.value,
            isSpeakerEnabled = callAudioManager.isSpeakerEnabled.value,
            audioRoute = callAudioManager.audioRoute.value,
            isFrontCamera = callCameraManager.isFrontCamera.value,
            callDurationSeconds = callDurationSeconds.value
        )
    }

    private fun observeCallTimer() {
        viewModelScope.launch {
            callManager.activeCall.collect { call ->
                if (call?.state == CallState.CONNECTED ||
                    call?.state == CallState.RECONNECTING
                ) {
                    startCallTimer(
                        call.answeredAt ?: System.currentTimeMillis()
                    )
                } else {
                    stopCallTimer()
                }
            }
        }

        viewModelScope.launch {
            callDurationSeconds.collect {
                refreshActiveState()
            }
        }
    }

    private fun observeMedia() {
        viewModelScope.launch {
            callMediaManager.isMicrophoneEnabled.collect {
                refreshActiveState()
            }
        }

        viewModelScope.launch {
            callMediaManager.isCameraEnabled.collect {
                refreshActiveState()
            }
        }
    }

    private fun observeAudio() {
        viewModelScope.launch {
            callAudioManager.audioRoute.collect {
                refreshActiveState()
            }
        }

        viewModelScope.launch {
            callAudioManager.isSpeakerEnabled.collect {
                refreshActiveState()
            }
        }
    }

    private fun observeRemoteStreams() {
        viewModelScope.launch {
            webRtcManager.remoteStreams.collect {
                refreshActiveState()
            }
        }
    }

    private fun refreshActiveState() {
        val call = callManager.activeCall.value ?: return

        if (_uiState.value is CallUiState.Active) {
            _uiState.value = CallUiState.Active(
                buildActiveCallUiState(call)
            )
        }
    }

    fun startCall(
        participants: List<CallParticipant>,
        callType: CallType
    ) {

        // Mesh topology: no duplicates, and never more than the supported size
        // (the UI already warns; this is the safety net).
        val invitees = participants
            .distinctBy { it.participantId }
            .take(MAX_REMOTE_PARTICIPANTS)

        if (invitees.isEmpty()) return

        viewModelScope.launch {
            teardownJob?.join()


            val call = callManager.startCall(
                participants = invitees,
                callType = callType
            ) ?: run {
                Log.e("CallViewModel", "callManager.startCall() returned null - call already active")
                _uiState.value = CallUiState.Error(
                    message = "A call is already active."
                )
                return@launch
            }

            _openCallScreen.emit(Unit)

            startCallService()

            callAudioManager.initialize(callType)

            // Mic + (idle) video track must exist BEFORE the invite goes out.
            callMediaManager.prepareLocalMedia()

            if (callType == CallType.VIDEO) {
                val initialized = callCameraManager.initializeCamera()

                if (initialized) {
                    callCameraManager.setCameraEnabled(true)
                }
            }

            call.participants.forEach { participant ->

                callSignalingManager.sendCallInvite(
                    callId = call.callId.value,
                    target = participant.userNumber,
                    callType = call.callType,
                    // Group start: each invitee also learns who else is invited.
                    roster = call.participants.filter {
                        it.participantId != participant.participantId
                    }
                )

            }

        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            teardownJob?.join()

            val call = callManager.activeCall.value ?: return@launch

            if (!callManager.acceptCall()) {
                return@launch
            }

            startCallService()

            callAudioManager.initialize(call.callType)

            // Everything local must be ready BEFORE CALL_ACCEPT is sent: the
            // caller's offer arrives right after it.
            callMediaManager.prepareLocalMedia()

            if (call.callType == CallType.VIDEO) {
                callCameraManager.initializeCamera()
                callCameraManager.setCameraEnabled(true)
            }

            if (call.participants.isEmpty()) return@launch

            // Group call: tell EVERYONE we are in, so each of them offers to us.
            call.participants.forEach { participant ->
                callSignalingManager.sendCallAccept(
                    callId = call.callId.value,
                    target = participant.userNumber
                )
            }
        }
    }

    fun rejectCall() {
        teardownJob = viewModelScope.launch {
            val call = callManager.activeCall.value ?: return@launch

            if (!callManager.rejectCall()) {
                return@launch
            }

            call.participants.forEach { participant ->
                callSignalingManager.sendCallReject(
                    callId = call.callId.value,
                    target = participant.userNumber
                )
            }

            callAudioManager.reset()
            callCameraManager.reset()
        }
    }
    fun cancelCall() {
        teardownJob = viewModelScope.launch {
            val call = callManager.activeCall.value ?: return@launch

            if (!callManager.cancelCall()) {
                return@launch
            }

            call.participants.forEach { participant ->
                callSignalingManager.sendCallCancel(
                    callId = call.callId.value,
                    target = participant.userNumber
                )
            }

            callAudioManager.reset()
            callCameraManager.reset()
        }
    }

    fun endCall() {
        teardownJob = viewModelScope.launch {
            val call = callManager.activeCall.value ?: return@launch
            val participant = call.participants.firstOrNull() ?: return@launch

            val durationSeconds = if (
                call.answeredAt != null
            ) {
                ((System.currentTimeMillis() - call.answeredAt) / 1000L)
                    .coerceAtLeast(0L)
            } else {
                0L
            }

            val endedCall = EndedCallUiState(
                callId = call.callId.value,
                callType = call.callType,
                participant = resolvedParticipant(participant),
                durationSeconds = durationSeconds,
                endReason = CallEndReason.LOCAL_ENDED
            )

            _endedCall.value = endedCall

            if (!callManager.endCall()) {
                _endedCall.value = null
                return@launch
            }

            call.participants.forEach { participant ->
                callSignalingManager.sendCallEnd(
                    callId = call.callId.value,
                    target = participant.userNumber
                )
            }

            callMediaManager.reset()
            callCameraManager.reset()
            callAudioManager.reset()
        }
    }

    fun handleRemoteCallEnd() {
        teardownJob = viewModelScope.launch {
            val call = callManager.activeCall.value ?: return@launch
            val participant = call.participants.firstOrNull() ?: return@launch

            val durationSeconds = if (call.answeredAt != null) {
                ((System.currentTimeMillis() - call.answeredAt) / 1000L)
                    .coerceAtLeast(0L)
            } else {
                0L
            }

            _endedCall.value = EndedCallUiState(
                callId = call.callId.value,
                callType = call.callType,
                participant = resolvedParticipant(participant),
                durationSeconds = durationSeconds,
                endReason = CallEndReason.REMOTE_ENDED
            )

            if (!callManager.endCall()) {
                _endedCall.value = null
                return@launch
            }

            callMediaManager.reset()
            callCameraManager.reset()
            callAudioManager.reset()
        }
    }
    fun recallAudio() {
        val endedCall = _endedCall.value ?: return

        _endedCall.value = null

        startCall(
            participants = listOf(endedCall.participant),
            callType = CallType.VOICE
        )
    }
    fun recallVideo() {
        val endedCall = _endedCall.value ?: return

        _endedCall.value = null

        startCall(
            participants = listOf(endedCall.participant),
            callType = CallType.VIDEO
        )
    }
    fun dismissEndedCall() {
        _endedCall.value = null

        if (callManager.activeCall.value == null) {
            _uiState.value = CallUiState.Finished
        }
    }
    fun toggleMicrophone() {
        callMediaManager.toggleMicrophone()

        val call = callManager.activeCall.value ?: return

        call.participants.forEach { participant ->
            callSignalingManager.sendParticipantMediaChanged(
                callId = call.callId.value,
                target = participant.userNumber,
                isMicrophoneEnabled = callMediaManager.isMicrophoneEnabled.value,
                isCameraEnabled = callMediaManager.isSendingVideo,
                isScreenSharing = callMediaManager.isScreenSharing.value
            )
        }
    }

    fun toggleCamera() {
        callMediaManager.toggleCamera()

        val call = callManager.activeCall.value ?: return

        call.participants.forEach { participant ->
            callSignalingManager.sendParticipantMediaChanged(
                callId = call.callId.value,
                target = participant.userNumber,
                isMicrophoneEnabled = callMediaManager.isMicrophoneEnabled.value,
                isCameraEnabled = callMediaManager.isSendingVideo,
                isScreenSharing = callMediaManager.isScreenSharing.value
            )
        }
    }


    /**
     * Add-participant is only offered on an established call, below the size
     * cap, and with no other invite still pending (two simultaneous adds
     * would leave the two newcomers unaware of each other).
     */
    fun canAddParticipant(): Boolean {
        val call = callManager.activeCall.value ?: return false

        if (call.answeredAt == null) return false
        if (call.state != CallState.CONNECTED &&
            call.state != CallState.RECONNECTING
        ) return false

        val pending = call.participants.any {
            it.connectionState == ParticipantConnectionState.INVITED ||
                    it.connectionState == ParticipantConnectionState.RINGING
        }

        val alive = call.participants.count { it.connectionState.isAlive }

        return !pending && alive < MAX_REMOTE_PARTICIPANTS
    }

    fun addParticipant(participant: CallParticipant) =
        addParticipants(listOf(participant))

    /**
     * Invites several people in one go (they all learn about each other).
     * Anyone already in the call is skipped and the list is trimmed to the
     * free slots.
     */
    fun addParticipants(participants: List<CallParticipant>) {
        viewModelScope.launch {
            if (!canAddParticipant()) {
                Log.w("CallViewModel", "addParticipants ignored: not allowed right now")
                return@launch
            }

            val call = callManager.activeCall.value ?: return@launch

            val inCall = call.participants
                .filter { it.connectionState.isAlive }
                .map { it.participantId }
                .toSet()

            val freeSlots = MAX_REMOTE_PARTICIPANTS - inCall.size

            val fresh = participants
                .distinctBy { it.participantId }
                .filter { it.participantId !in inCall }
                .take(freeSlots)

            if (fresh.isEmpty()) return@launch

            val invited = callSignalingManager.inviteParticipants(fresh)
        }
    }

    fun switchCamera() {
        callCameraManager.switchCamera()
    }

    fun setAudioRoute(route: CallAudioRoute) {
        callAudioManager.setAudioRoute(route)
    }

    fun toggleSpeaker() {
        callAudioManager.toggleSpeaker()
    }

    override fun onCleared() {
        // The signaling manager outlives this ViewModel. If we leave the
        // listener registered, a remote CALL_END calls into a dead
        // viewModelScope and nothing happens.
        callSignalingManager.setOnRemoteCallEndListener(null)
    }

    fun clearError() {
        _uiState.value = CallUiState.Idle
    }
}