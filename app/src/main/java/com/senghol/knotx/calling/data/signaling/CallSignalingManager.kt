package com.senghol.knotx.calling.data.signaling

import android.util.Log
import com.senghol.knotx.calling.data.model.toDomain
import com.senghol.knotx.calling.data.model.toPayload
import com.senghol.knotx.calling.data.webrtc.WebRtcEventListener
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
import com.senghol.knotx.calling.domain.manager.CallAudioManager
import com.senghol.knotx.calling.domain.manager.CallCameraManager
import com.senghol.knotx.calling.domain.manager.CallManager
import com.senghol.knotx.calling.domain.manager.CallMediaManager
import com.senghol.knotx.calling.domain.manager.CallNetworkManager
import com.senghol.knotx.calling.domain.manager.CallSoundManager
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallId
import com.senghol.knotx.calling.domain.model.CallMediaType
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallSession
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.ui.CallNotificationManager

import com.senghol.knotx.connection.Knotx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

// Short blips (Wi-Fi roaming, lift, tunnel) often heal by themselves.
private const val DISCONNECT_GRACE_MS = 4_000L

// ICE-restart retry interval and total budget before the call is given up.
private const val RECONNECT_RETRY_MS = 8_000L
private const val RECONNECT_BUDGET_MS = 30_000L

// An invited participant who never answers is dropped after this long, WITHOUT
// ending the call for everyone else.
private const val PARTICIPANT_RING_TIMEOUT_MS = 45_000L

class CallSignalingManager(
    private val callManager: CallManager,
    private val webRtcManager: WebRtcManager,
    private val callMediaManager: CallMediaManager,
    private val callAudioManager: CallAudioManager,
    private val callCameraManager: CallCameraManager,
    private val callNetworkManager: CallNetworkManager,
    private val knotx: Knotx,
    private val callNotificationManager: CallNotificationManager,
    private val callSoundManager: CallSoundManager
)  : WebRtcEventListener {

    companion object {
        private const val MAX_INVITE_AGE_MS = 50_000L
        private const val ENDED_CALL_MEMORY_MS = 10 * 60_000L
        private const val MAX_ENDED_CALLS = 50
    }
    // callId -> "am I revealed to the other person"
    private val iAmRevealed = ConcurrentHashMap<String, Boolean>()
    private val signalingDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val signalingScope =
        CoroutineScope(SupervisorJob() + signalingDispatcher)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // All incoming signals are processed ONE AT A TIME, in arrival order.
    // Before: every message was a separate coroutine, and because handlers
    // suspend (setRemoteDescription, createAnswer) a later ICE candidate could
    // run in the middle of an earlier OFFER handler.
    private val incomingSignals = Channel<CallData>(Channel.UNLIMITED)

    // Group-call state. Declared before init{} on purpose.
    //  - participantRingJobs: per-invitee "no answer" timers
    //  - weOfferedTo: peers we sent the ORIGINAL offer to. In a mesh "caller"
    //    is per PAIR, so only the offering side restarts ICE for that pair.
    private val participantRingJobs = ConcurrentHashMap<String, Job>()
    private val weOfferedTo: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        signalingScope.launch {
            for (callData in incomingSignals) {
                try {
                    handleIncomingCallData(callData)
                } catch (e: Exception) {
                    Log.e("WebRTC", "Signal ${callData.action} failed", e)
                }
            }
        }

        knotx.setCallDataCallback { callData ->
            incomingSignals.trySend(callData)
        }

        webRtcManager.setEventListener(this)

        callManager.onCallTimeout = { call, finalState ->
            handleCallTimeout(call, finalState)
        }

        observeNetworkChanges()
        observeCallLifecycle()
        observeScreenSharing()
    }

    /**
     * Screen share started / stopped (also when the user stops it from the
     * system UI): record it on the call and tell the other side, so its UI can
     * switch between "camera" and "screen" rendering.
     */
    private fun observeScreenSharing() {
        scope.launch {
            // Also re-publish when the camera flag changes, e.g. the camera
            // coming back on after sharing stops.
            combine(
                webRtcManager.screenSharing,
                webRtcManager.localVideoEnabled
            ) { sharing, _ -> sharing }.collect { sharing ->
                if (callManager.activeCall.value == null) return@collect

                if (sharing) {
                    callManager.addMediaType(CallMediaType.SCREEN)
                } else {
                    callManager.removeMediaType(CallMediaType.SCREEN)
                }

                publishCurrentMediaState()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Call lifecycle (works with or without a ViewModel / UI)
    // -------------------------------------------------------------------------

    private val teardownMutex = Mutex()

    /**
     * Whenever a call leaves "active" for ANY reason (remote reject/cancel/busy,
     * timeout, failure, local end) we
     *  - dismiss its incoming-call notification, and
     *  - release mic / camera / audio mode / peer connections.
     * Before, only the ViewModel's own end/cancel/reject paths did this, so
     * every other way of ending left WebRTC and audio mode running.
     */
    private fun observeCallLifecycle() {
        scope.launch {
            var lastCallId: String? = null

            callManager.activeCall.collect { call ->
                val currentId = call?.callId?.value
                val previousId = lastCallId
                lastCallId = currentId

                if (previousId != null && previousId != currentId) {
                    callNotificationManager.cancelIncomingCall(previousId)
                }

                if (call == null && previousId != null) {
                    clearGroupCallState()
                    teardownLocalMediaIfIdle()
                }
            }
        }
    }

    private suspend fun teardownLocalMediaIfIdle() {
        teardownMutex.withLock {
            // A new call may already have started; never tear that one down.
            if (callManager.activeCall.value != null) return

            withContext(Dispatchers.Main) {
                callMediaManager.reset()
                callCameraManager.reset()

                // Let busy / failed / end tones finish on the call audio
                // route before the audio mode is released.
                callSoundManager.awaitEndTone()

                // A new call may have started while the tone was playing.
                if (callManager.activeCall.value != null) return@withContext

                callAudioManager.reset()
            }
        }
    }

    private fun handleCallTimeout(call: CallSession, finalState: CallState) {
        Log.w(
            "WebRTC",
            "Call timed out: id=${call.callId.value}, dir=${call.direction}, -> $finalState"
        )

        // Tell the other side so its phone stops ringing / cleans up too.
        call.participants.forEach { participant ->
            when (finalState) {
                CallState.CANCELLED ->
                    sendCallCancel(call.callId.value, participant.userNumber)

                CallState.FAILED ->
                    sendCallEnd(call.callId.value, participant.userNumber)

                else -> Unit // MISSED on the callee: the caller already gave up
            }
        }
        // Media teardown + notification dismissal happen in observeCallLifecycle().
    }
    private fun observeNetworkChanges() {
        scope.launch {
            callNetworkManager.networkState
                .map { it.isAvailable }
                .distinctUntilChanged()
                .collect { available ->
                    if (available) {
                        handleNetworkAvailable()
                    }
                }
        }
    }
    private fun handleNetworkAvailable() {
        val call = callManager.activeCall.value ?: return

        // Only for calls that were already established.
        if (call.answeredAt == null) return

        call.participants
            .filter {
                // Only peers we actually have a connection to; a participant
                // that is still ringing has nothing to reconnect.
                it.connectionState.isAlive &&
                        it.connectionState != ParticipantConnectionState.CONNECTED &&
                        webRtcManager.getPeerConnection(it.participantId) != null
            }
            .forEach { participant ->
                if (call.state == CallState.RECONNECTING) {
                    // Network is back: retry right now instead of waiting for
                    // the next tick.
                    startReconnect(participant.participantId, force = true)
                } else {
                    beginReconnect(participant.participantId)
                }
            }
    }

    // -------------------------------------------------------------------------
    // Incoming
    // -------------------------------------------------------------------------

    suspend fun handleIncomingCallData(data: CallData) {

        // First time we see this call: store the reveal flag the server sent.
        // putIfAbsent = later messages never overwrite it.
        data.revealReceiverNumber?.let { iAmRevealed.putIfAbsent(data.callId, it) }

        when (data.action) {
            CallSignalType.CALL_INVITE -> handleCallInvite(data)
            CallSignalType.CALL_RINGING -> handleCallRinging(data)
            CallSignalType.CALL_ACCEPT -> handleCallAccept(data)
            CallSignalType.CALL_REJECT -> handleCallReject(data)
            CallSignalType.CALL_CANCEL -> handleCallCancel(data)
            CallSignalType.CALL_BUSY -> handleCallBusy(data)
            CallSignalType.CALL_END -> handleCallEnd(data)

            CallSignalType.ADD_PARTICIPANT -> handleAddParticipant(data)
            CallSignalType.REMOVE_PARTICIPANT -> handleRemoveParticipant(data)
            CallSignalType.PARTICIPANT_JOINED -> handleParticipantJoined(data)
            CallSignalType.PARTICIPANT_LEFT -> handleParticipantLeft(data)

            CallSignalType.PARTICIPANT_MEDIA_CHANGED -> handleParticipantMediaChanged(data)
            CallSignalType.WEBRTC_OFFER -> handleWebRtcOffer(data)
            CallSignalType.WEBRTC_ANSWER -> handleWebRtcAnswer(data)
            CallSignalType.ICE_CANDIDATE -> handleIceCandidate(data)

            CallSignalType.CALL_FAILED -> handleCallFailed(data)
        }
    }

    private suspend fun handleParticipantMediaChanged(data: CallData) {
        val payload = data.payload ?: return
        val participantId = data.sender ?: return

        payload.isMicrophoneEnabled?.let { enabled ->
            callManager.setParticipantMicrophone(
                participantId = participantId,
                enabled = enabled
            )
        }

        payload.isCameraEnabled?.let { enabled ->
            callManager.setParticipantCamera(
                participantId = participantId,
                enabled = enabled
            )
        }

        payload.isScreenSharing?.let { enabled ->
            callManager.setParticipantScreenSharing(
                participantId = participantId,
                enabled = enabled
            )
        }
    }
    fun sendParticipantMediaChanged(
        callId: String,
        target: String? = null,
        isMicrophoneEnabled: Boolean,
        isCameraEnabled: Boolean,
        isScreenSharing: Boolean
    ) {
        emit(
            action = CallSignalType.PARTICIPANT_MEDIA_CHANGED,
            callId = callId,
            target = target,
            payload = CallPayload(
                isMicrophoneEnabled = isMicrophoneEnabled,
                isCameraEnabled = isCameraEnabled,
                isScreenSharing = isScreenSharing
            )
        )
    }
    private suspend fun handleCallInvite(data: CallData) {
        val payload = data.payload ?: return
        val sender = data.sender ?: return

        // Same invite delivered twice (retry / reconnect): ignore, don't BUSY it.
        if (callManager.activeCall.value?.callId?.value == data.callId) return

        val callType = payload.callType?.let {
            runCatching { CallType.valueOf(it) }.getOrNull()
        } ?: return

        val participantPayload = payload.participant

        val participant = CallParticipant(
            participantId = sender,
            userNumber = sender,
            userNameId = participantPayload?.userNameId,
            displayName = participantPayload?.displayName,
            profileUrl = participantPayload?.profileUrl,
            connectionState = ParticipantConnectionState.RINGING
        )

        // Group call: the inviter also sends everyone else already in the call,
        // so we can connect to all of them once we accept.
        val roster = payload.participants.orEmpty()
            .filter { !it.participantId.isNullOrBlank() && it.participantId != sender }
            .map { it.toDomain() }

        val call = CallSession(
            callId = CallId(data.callId),
            callType = callType,
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            participants = listOf(participant) + roster,
            activeMediaTypes = setOf(CallMediaType.AUDIO) +
                    if (callType == CallType.VIDEO) {
                        setOf(CallMediaType.VIDEO)
                    } else {
                        emptySet()
                    },
            createdAt = data.timeStamp
        )

        val ageMs = System.currentTimeMillis() - data.timeStamp

        if (ageMs > MAX_INVITE_AGE_MS) {
            Log.w("WebRTC", "Stale invite ignored: ${data.callId} age=${ageMs}ms")
            rememberEnded(data.callId)
            callManager.recordMissedCall(call)
            return
        }

        if (wasEnded(data.callId)) {
            Log.w("WebRTC", "Invite for an already ended call ignored: ${data.callId}")
            return
        }

        // They can only be in one call: if our old call with them is stuck
        // (reconnecting / half-dead), it is over. Don't answer BUSY for it.
        supersedeStuckCall(sender)

        if (!callManager.receiveIncomingCall(call)) {
            // Before: silently dropped, so the caller rang forever and got
            // stuck in RINGING too. Tell them we're busy.
            sendCallBusy(
                callId = data.callId,
                target = sender
            )
            return
        }

        sendCallRinging(
            callId = data.callId,
            target = sender
        )

        callNotificationManager.showIncomingCall(call)
    }
    private suspend fun handleCallRinging(data: CallData) {
        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        val sender = data.sender ?: return

        callManager.updateParticipantState(
            participantId = sender,
            state = ParticipantConnectionState.RINGING
        )

        val currentCall = callManager.activeCall.value ?: return
        if (currentCall.callId.value != data.callId) return

        if (currentCall.state == CallState.INITIATING) {
            callManager.setState(CallState.RINGING)
        }
    }

    private suspend fun handleCallAccept(data: CallData) {
        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        val sender = data.sender ?: return

        participantRingJobs.remove(sender)?.cancel()
        ensureParticipant(data.callId, sender)

        // Group call: another invitee accepted while WE are still ringing.
        // Don't touch our own call state. They offer to us / we offer to them
        // as soon as we accept ourselves (our CALL_ACCEPT goes to everyone).
        if (call.direction == CallDirection.INCOMING &&
            call.state == CallState.RINGING
        ) {
            return
        }

        callManager.updateParticipantState(
            participantId = sender,
            state = ParticipantConnectionState.CONNECTING
        )

        val currentCall = callManager.activeCall.value ?: return
        if (currentCall.callId.value != data.callId) return

        if (currentCall.state != CallState.CONNECTING &&
            currentCall.state != CallState.CONNECTED
        ) {
            if (!callManager.setState(CallState.CONNECTING)) {
                return
            }
        }

        createOfferForParticipant(sender)
    }

    private suspend fun handleCallReject(data: CallData) {
        cancelIncomingUnlessGroup(data.callId)

        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        val sender = data.sender ?: return

        markRemoteGone(sender, ParticipantConnectionState.DECLINED)

        // 1:1: unchanged behaviour.
        if (call.participants.size <= 1) {
            callManager.setState(
                CallState.REJECTED,
                CallEndReason.REJECTED
            )
            return
        }

        // Group: one decline only ends the call when nobody is left.
        endWhenNobodyLeft(data.callId)
    }

    private val endedCallIds = LinkedHashMap<String, Long>()

    private fun rememberEnded(callId: String) {
        val now = System.currentTimeMillis()

        synchronized(endedCallIds) {
            endedCallIds[callId] = now

            endedCallIds.entries.removeAll { now - it.value > ENDED_CALL_MEMORY_MS }

            while (endedCallIds.size > MAX_ENDED_CALLS) {
                endedCallIds.remove(endedCallIds.keys.first())
            }
        }
    }

    private fun wasEnded(callId: String): Boolean =
        synchronized(endedCallIds) { endedCallIds.containsKey(callId) }

    private suspend fun handleCallCancel(data: CallData) {
        rememberEnded(data.callId)
        callNotificationManager.cancelIncomingCall(data.callId)

        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        callManager.setState(
            CallState.CANCELLED,
            CallEndReason.CANCELLED
        )
    }

    private suspend fun handleCallBusy(data: CallData) {
        cancelIncomingUnlessGroup(data.callId)

        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        // Group / established call: someone being busy must not end it.
        if (call.participants.size > 1 || call.answeredAt != null) {
            val sender = data.sender ?: return

            markRemoteGone(sender, ParticipantConnectionState.DECLINED)
            // Busy goes only to the inviter, so tell the others it's off.
            broadcastParticipantLeft(data.callId, sender)
            endWhenNobodyLeft(data.callId)
            return
        }

        callManager.setState(
            CallState.BUSY,
            CallEndReason.BUSY
        )
    }
    private var onRemoteCallEnd: (suspend (String) -> Unit)? = null

    fun setOnRemoteCallEndListener(
        listener: (suspend (String) -> Unit)?
    ) {
        onRemoteCallEnd = listener
    }
    private suspend fun handleCallEnd(data: CallData) {
        // Group call: one person hanging up is "they left", not "call over".
        val groupCall = callManager.activeCall.value
        val endSender = data.sender

        if (groupCall != null &&
            groupCall.callId.value == data.callId &&
            endSender != null &&
            groupCall.participants.size > 1 &&
            groupCall.participants.any { it.participantId == endSender }
        ) {
            markRemoteGone(endSender, ParticipantConnectionState.LEFT)
            endWhenNobodyLeft(data.callId)
            return
        }

        rememberEnded(data.callId)

        callNotificationManager.cancelIncomingCall(data.callId)

        val call = callManager.activeCall.value

        if (call == null) {
            Log.e("CallEndDebug", "NO ACTIVE CALL")
            return
        }

        if (call.callId.value != data.callId) {
            Log.e(
                "CallEndDebug",
                "CALL ID MISMATCH: active=${call.callId.value}, received=${data.callId}"
            )
            return
        }


        val listener = onRemoteCallEnd

        if (listener != null) {
            listener.invoke(data.callId)
        } else {
            // No ViewModel is listening (app opened by a push, screen closed).
            // End the call here so it can't stay active forever.
            callManager.endCall()
        }

    }
    // -------------------------------------------------------------------------
    // Group participants
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Group-call helpers
    // -------------------------------------------------------------------------

    /**
     * Adds [participants] to the running call in ONE step:
     *  1. registers them locally (state INVITED),
     *  2. CALL_INVITE to each, carrying the roster (existing members AND the
     *     other newcomers, so newcomers can connect to each other too),
     *  3. ADD_PARTICIPANT to every existing member for every newcomer.
     *
     * Existing members each send their own offer when a newcomer's
     * CALL_ACCEPT arrives (handleCallAccept); a newcomer only answers.
     * Names / photos stay on this device: everyone resolves them from their
     * own contacts, so they are not put on the wire.
     */
    suspend fun inviteParticipants(participants: List<CallParticipant>): Boolean {
        val call = callManager.activeCall.value ?: return false
        val callId = call.callId.value

        val registered = mutableListOf<CallParticipant>()

        participants.distinctBy { it.participantId }.forEach { candidate ->
            val newcomer = candidate.copy(
                connectionState = ParticipantConnectionState.INVITED,
                joinedAt = null,
                leftAt = null
            )

            val known = call.participants.firstOrNull {
                it.participantId == newcomer.participantId
            }

            val ok = when {
                known == null -> callManager.addParticipant(callId, newcomer)
                // Someone who declined / left / timed out can be invited again.
                !known.connectionState.isAlive -> callManager.updateParticipant(callId, newcomer)
                else -> false
            }

            if (ok) registered += newcomer
        }

        if (registered.isEmpty()) return false

        val registeredIds = registered.map { it.participantId }.toSet()

        val members = call.participants.filter {
            it.participantId !in registeredIds && it.connectionState.isAlive
        }

        fun CallParticipant.forWire() = copy(displayName = null, profileUrl = null)

        registered.forEach { newcomer ->
            sendCallInvite(
                callId = callId,
                target = newcomer.userNumber,
                callType = call.callType,
                roster = members + registered.filter {
                    it.participantId != newcomer.participantId
                }
            )
        }

        members.forEach { member ->
            registered.forEach { newcomer ->
                sendAddParticipant(
                    callId = callId,
                    target = member.userNumber,
                    participant = newcomer.forWire()
                )
            }
        }

        return true
    }

    /**
     * [sender] is inviting us to a NEW call while our current call still lists
     * them as a live participant. A person can only be in one call, so they
     * have already dropped the old one (crash, network loss, the CALL_END
     * never reached us). Without this the old call kept a peer connection
     * alive, looked "busy" to CallManager, and the new call was refused.
     *
     * Only established / half-established calls are touched. If other people
     * are still in the old call we stay busy.
     */
    private suspend fun supersedeStuckCall(sender: String) {
        val current = callManager.activeCall.value ?: return

        val established = current.state == CallState.ACCEPTING ||
                current.state == CallState.CONNECTING ||
                current.state == CallState.CONNECTED ||
                current.state == CallState.RECONNECTING

        if (!established) return

        val stuck = current.participants.firstOrNull {
            (it.participantId == sender || it.userNumber == sender) &&
                    it.connectionState.isAlive
        } ?: return

        Log.w(
            "WebRTC",
            "New invite from $sender while call ${current.callId.value} " +
                    "(${current.state}) still lists them: dropping the old call"
        )

        markRemoteGone(stuck.participantId, ParticipantConnectionState.LEFT)

        if (callManager.hasLiveRemotes()) return

        callManager.failCall(CallEndReason.NETWORK_ERROR)
    }

    /** A signal from someone we don't know yet (late ADD_PARTICIPANT): add them. */
    private suspend fun ensureParticipant(callId: String, id: String) {
        val call = callManager.activeCall.value ?: return
        if (call.callId.value != callId) return

        if (call.participants.any { it.participantId == id || it.userNumber == id }) return

        callManager.addParticipant(
            callId = callId,
            participant = CallParticipant(
                participantId = id,
                userNumber = id,
                userNameId = null,
                displayName = null,
                profileUrl = null,
                connectionState = ParticipantConnectionState.CONNECTING
            )
        )
    }

    /**
     * A remote is gone for good (left / declined / failed / no answer):
     * stop every timer and connection for them. Never call this from inside
     * that participant's own reconnect job (it would cancel itself).
     */
    private suspend fun markRemoteGone(
        participantId: String,
        state: ParticipantConnectionState
    ) {
        participantRingJobs.remove(participantId)?.cancel()
        disconnectGraceJobs.remove(participantId)?.cancel()
        cancelReconnect(participantId)

        callManager.updateParticipantState(
            participantId = participantId,
            state = state
        )

        webRtcManager.removePeerConnection(participantId)
        clearOutgoingIceState(participantId)
        weOfferedTo.remove(participantId)
    }

    /**
     * Ends the call ONLY when no remote is left. Replaces the old
     * `participants.size <= 2` checks, which ended a 3-way call on the
     * first decline.
     */
    private suspend fun endWhenNobodyLeft(
        callId: String,
        failed: Boolean = false
    ) {
        val call = callManager.activeCall.value ?: return
        if (call.callId.value != callId || callManager.hasLiveRemotes()) return

        when {
            failed -> callManager.setState(CallState.FAILED)

            // Was a real call: same path as a remote hang-up.
            call.answeredAt != null -> {
                val listener = onRemoteCallEnd

                if (listener != null) {
                    listener.invoke(callId)
                } else {
                    callManager.endCall()
                }
            }

            call.direction == CallDirection.INCOMING -> {
                callNotificationManager.cancelIncomingCall(callId)
                callManager.setState(CallState.CANCELLED, CallEndReason.CANCELLED)
            }

            call.state == CallState.INITIATING ||
                    call.state == CallState.RINGING -> {
                if (call.participants.all {
                        it.connectionState == ParticipantConnectionState.DECLINED
                    }
                ) {
                    callManager.setState(CallState.REJECTED, CallEndReason.REJECTED)
                } else {
                    // Nobody picked up: shown as "No answer".
                    callManager.setState(CallState.CANCELLED, CallEndReason.MISSED)
                }
            }

            else -> callManager.failCall(CallEndReason.NETWORK_ERROR)
        }
    }

    private fun broadcastParticipantLeft(callId: String, participantId: String) {
        val call = callManager.activeCall.value ?: return

        call.participants
            .filter { it.participantId != participantId && it.connectionState.isAlive }
            .forEach { sendParticipantLeft(callId, it.userNumber, participantId) }
    }

    /**
     * Per-invitee "no answer" timer. The call-level watchdog stops once the
     * call is CONNECTED, so without this an added person would ring forever.
     */
    private fun armParticipantRingTimeout(callId: String, participantId: String) {
        participantRingJobs.remove(participantId)?.cancel()

        val job = scope.launch {
            delay(PARTICIPANT_RING_TIMEOUT_MS)

            val call = callManager.activeCall.value ?: return@launch
            if (call.callId.value != callId) return@launch

            val state = callManager.getParticipant(participantId)
                ?.connectionState ?: return@launch

            if (state != ParticipantConnectionState.INVITED &&
                state != ParticipantConnectionState.RINGING
            ) return@launch

            Log.w("WebRTC", "Participant $participantId did not answer in time")

            markRemoteGone(participantId, ParticipantConnectionState.NO_ANSWER)
            sendCallCancel(callId, participantId)
            broadcastParticipantLeft(callId, participantId)
            endWhenNobodyLeft(callId)
        }

        participantRingJobs[participantId] = job
        job.invokeOnCompletion { participantRingJobs.remove(participantId, job) }
    }

    /** Group calls keep notifications of OTHER pending invites alive. */
    private fun cancelIncomingUnlessGroup(callId: String) {
        val size = callManager.activeCall.value
            ?.takeIf { it.callId.value == callId }
            ?.participants?.size ?: 0

        if (size <= 1) {
            callNotificationManager.cancelIncomingCall(callId)
        }
    }

    private fun onPeerClosed(participantId: String) {
        scope.launch {
            val call = callManager.activeCall.value ?: return@launch

            if (call.participants.size > 1) {
                // Group: one closed connection is one person gone.
                markRemoteGone(participantId, ParticipantConnectionState.LEFT)
                endWhenNobodyLeft(call.callId.value)
            } else {
                callManager.updateParticipantState(
                    participantId = participantId,
                    state = ParticipantConnectionState.LEFT
                )
                callManager.endCall()
            }
        }
    }

    private fun clearGroupCallState() {
        participantRingJobs.values.forEach { it.cancel() }
        participantRingJobs.clear()
        weOfferedTo.clear()
    }

    private suspend fun handleAddParticipant(data: CallData) {
        val participant = data.payload?.participant ?: return

        callManager.addParticipant(
            callId = data.callId,
            participant = participant.toDomain()
        )
    }

    private suspend fun handleRemoveParticipant(data: CallData) {
        val participantId = data.payload?.participantId ?: return

        markRemoteGone(participantId, ParticipantConnectionState.LEFT)

        callManager.removeParticipant(
            callId = data.callId,
            participantId = participantId
        )
    }

    private suspend fun handleParticipantJoined(data: CallData) {
        val participant = data.payload?.participant ?: return

        callManager.addParticipant(
            callId = data.callId,
            participant = participant.toDomain(
                defaultState = ParticipantConnectionState.CONNECTING
            )
        )
    }

    private suspend fun handleParticipantLeft(data: CallData) {
        val participantId = data.payload?.participantId ?: return

        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        markRemoteGone(participantId, ParticipantConnectionState.LEFT)
        endWhenNobodyLeft(data.callId)
    }

    // -------------------------------------------------------------------------
    // WebRTC signaling
    // -------------------------------------------------------------------------

    private suspend fun handleWebRtcOffer(data: CallData) {
        val sender = data.sender ?: return
        val sdp = data.payload?.sdp ?: return


        // We answer this peer, so we are not the side that restarts ICE.
        weOfferedTo.remove(sender)
        ensureParticipant(data.callId, sender)

        val existingPc = webRtcManager.getPeerConnection(sender)


        resetOutgoingIceGate(sender)
        webRtcManager.createPeerConnection(sender)

        val pcAfterCreate = webRtcManager.getPeerConnection(sender)



        val applied = webRtcManager.setRemoteDescription(
            participantId = sender,
            description = SessionDescription(
                SessionDescription.Type.OFFER,
                sdp
            )
        )

        val pcAfterRemote = webRtcManager.getPeerConnection(sender)


        if (!applied) {
            Log.e(
                "WebRTC",
                "Failed to apply remote OFFER: callId=${data.callId}, sender=$sender"
            )
            return
        }

        if (callManager.activeCall.value?.state == CallState.ACCEPTING) {
            callManager.setState(CallState.CONNECTING)
        }


        val answer = webRtcManager.createAnswer(sender)


        if (answer == null) {
            Log.e(
                "WebRTC",
                "Failed to create ANSWER: callId=${data.callId}, sender=$sender"
            )
            return
        }


        sendLocalSdp(
            action = CallSignalType.WEBRTC_ANSWER,
            callId = data.callId,
            target = sender,
            sdp = answer.description
        )

        callManager.updateParticipantState(
            participantId = sender,
            state = ParticipantConnectionState.CONNECTING
        )

    }
    private suspend fun handleWebRtcAnswer(data: CallData) {
        val sender = data.sender ?: return
        val sdp = data.payload?.sdp ?: return

        val applied = webRtcManager.setRemoteDescription(
            participantId = sender,
            description = SessionDescription(
                SessionDescription.Type.ANSWER,
                sdp
            )
        )

        if (!applied) {
            Log.e("WebRTC", "Failed to apply remote ANSWER from $sender")
            return
        }

        callManager.updateParticipantState(
            participantId = sender,
            state = ParticipantConnectionState.CONNECTING
        )
    }

    private fun handleIceCandidate(data: CallData) {
        val sender = data.sender ?: return
        val payload = data.payload ?: return

        val candidate = payload.candidate ?: return
        val sdpMid = payload.sdpMid ?: return
        val sdpMLineIndex = payload.sdpMLineIndex ?: return

        // Do NOT create a PeerConnection here. A PC is created only by the
        // offer/accept flow, after local tracks exist. If the PC isn't there
        // yet the candidate is buffered by WebRtcManager and applied after
        // setRemoteDescription.
        webRtcManager.addIceCandidate(
            participantId = sender,
            candidate = IceCandidate(
                sdpMid,
                sdpMLineIndex,
                candidate
            )
        )
    }

    suspend fun createOfferForParticipant(participantId: String) {
        val call = callManager.activeCall.value ?: return

        weOfferedTo.add(participantId)
        resetOutgoingIceGate(participantId)
        webRtcManager.createPeerConnection(participantId)

        val offer = webRtcManager.createOffer(participantId) ?: return

        sendLocalSdp(
            action = CallSignalType.WEBRTC_OFFER,
            callId = call.callId.value,
            target = participantId,
            sdp = offer.description
        )
    }
    // -------------------------------------------------------------------------
    // Reconnect
    //
    // Flow for an ESTABLISHED call:
    //   DISCONNECTED -> wait DISCONNECT_GRACE_MS (WebRTC often self-heals)
    //                -> still not connected -> RECONNECTING
    //   FAILED       -> RECONNECTING immediately
    //   RECONNECTING -> the CALLER sends ICE-restart offers every
    //                   RECONNECT_RETRY_MS; the callee only answers (two sides
    //                   restarting at once would collide).
    //   CONNECTED    -> everything cancelled, call back to CONNECTED
    //   budget used up (RECONNECT_BUDGET_MS) -> call FAILED on both sides
    //
    // A call that never connected (answeredAt == null) still fails
    // immediately: retrying a broken first connection just wastes 30 s.
    // -------------------------------------------------------------------------

    private val reconnectJobs = ConcurrentHashMap<String, Job>()
    private val disconnectGraceJobs = ConcurrentHashMap<String, Job>()
    private val reconnectStartedAt = ConcurrentHashMap<String, Long>()
    private val lastRestartAt = ConcurrentHashMap<String, Long>()

    private fun onPeerDisconnected(participantId: String) {
        callManager.updateParticipantStateAsync(
            participantId = participantId,
            state = ParticipantConnectionState.DISCONNECTED
        )

        disconnectGraceJobs.remove(participantId)?.cancel()

        disconnectGraceJobs[participantId] = scope.launch {
            delay(DISCONNECT_GRACE_MS)

            val stillDown = callManager.getParticipant(participantId)
                ?.connectionState == ParticipantConnectionState.DISCONNECTED

            if (stillDown) beginReconnect(participantId)
        }
    }

    private fun onPeerRecovered(participantId: String) {
        disconnectGraceJobs.remove(participantId)?.cancel()
        cancelReconnect(participantId)
    }

    private fun beginReconnect(participantId: String) {
        scope.launch {
            val call = callManager.activeCall.value ?: return@launch

            if (call.answeredAt == null) {
                // Never connected: a real failure, not a "reconnect".
                callManager.failCall(CallEndReason.NETWORK_ERROR)
                return@launch
            }

            Log.w("WebRTC", "Reconnecting: participant=$participantId")

            // Group call: only flag the WHOLE call as reconnecting when no
            // other peer is still connected.
            val othersConnected = call.participants.any {
                it.participantId != participantId &&
                        it.connectionState == ParticipantConnectionState.CONNECTED
            }

            if (!othersConnected) {
                callManager.setState(CallState.RECONNECTING)
            }

            callManager.updateParticipantState(
                participantId = participantId,
                state = ParticipantConnectionState.RECONNECTING
            )

            startReconnect(participantId, force = false)
        }
    }

    private fun startReconnect(
        participantId: String,
        force: Boolean = false
    ) {
        if (!force && reconnectJobs[participantId]?.isActive == true) return

        reconnectJobs.remove(participantId)?.cancel()

        val startedAt = reconnectStartedAt.getOrPut(participantId) {
            System.currentTimeMillis()
        }

        reconnectJobs[participantId] = scope.launch {
            while (isActive) {
                val call = callManager.activeCall.value ?: break

                val participant = call.participants
                    .firstOrNull { it.participantId == participantId }

                if (participant == null ||
                    participant.connectionState == ParticipantConnectionState.LEFT ||
                    participant.connectionState == ParticipantConnectionState.CONNECTED
                ) {
                    break
                }

                if (System.currentTimeMillis() - startedAt > RECONNECT_BUDGET_MS) {
                    giveUpReconnect(call, participantId)
                    break
                }

                // Only the side that sent the original offer restarts ICE.
                // In a group call "caller" is per pair, not per call.
                if (participantId in weOfferedTo) {
                    restartIceForParticipant(participantId)
                }

                delay(RECONNECT_RETRY_MS)
            }

            reconnectJobs.remove(participantId)
        }
    }

    private suspend fun giveUpReconnect(call: CallSession, participantId: String) {
        Log.e("WebRTC", "Reconnect budget exhausted: participant=$participantId")

        // NOTE: this runs INSIDE the participant's own reconnect job, so do not
        // call cancelReconnect()/markRemoteGone() here (they'd cancel this job).
        reconnectStartedAt.remove(participantId)
        lastRestartAt.remove(participantId)

        callManager.updateParticipantState(
            participantId = participantId,
            state = ParticipantConnectionState.FAILED
        )

        webRtcManager.removePeerConnection(participantId)
        clearOutgoingIceState(participantId)
        weOfferedTo.remove(participantId)

        // Tell the other side so it doesn't sit in RECONNECTING for 30 s too.
        sendCallEnd(call.callId.value, participantId)

        if (!callManager.hasLiveRemotes()) {
            callManager.failCall(CallEndReason.NETWORK_ERROR)
        } else {
            // Others are still fine: don't leave the whole call flagged as
            // RECONNECTING because of one dead peer.
            val current = callManager.activeCall.value
            if (current?.state == CallState.RECONNECTING &&
                current.participants.any {
                    it.connectionState == ParticipantConnectionState.CONNECTED
                }
            ) {
                callManager.setState(CallState.CONNECTED)
            }
        }
    }

    private fun cancelReconnect(participantId: String) {
        reconnectJobs.remove(participantId)?.cancel()
        reconnectStartedAt.remove(participantId)
        lastRestartAt.remove(participantId)
    }

    private suspend fun restartIceForParticipant(participantId: String) {
        val call = callManager.activeCall.value ?: return
        val peerConnection = webRtcManager.getPeerConnection(participantId) ?: return

        // Don't stack a second offer on top of one still waiting for its
        // answer, unless that offer is clearly lost (older than one retry).
        val now = System.currentTimeMillis()
        val last = lastRestartAt[participantId] ?: 0L

        if (peerConnection.signalingState() != PeerConnection.SignalingState.STABLE &&
            now - last < RECONNECT_RETRY_MS
        ) {
            return
        }

        lastRestartAt[participantId] = now

        resetOutgoingIceGate(participantId)

        val offer = webRtcManager.restartIce(participantId) ?: return

        sendLocalSdp(
            action = CallSignalType.WEBRTC_OFFER,
            callId = call.callId.value,
            target = participantId,
            sdp = offer.description
        )
    }

    // -------------------------------------------------------------------------
    // WebRTC events
    // -------------------------------------------------------------------------

    override fun onIceCandidate(
        participantId: String,
        candidate: IceCandidate
    ) {
        val call = callManager.activeCall.value ?: return

        // Gathering starts inside setLocalDescription, i.e. BEFORE the
        // offer/answer has been handed to the network. Sending candidates
        // first makes the remote create a PC too early and/or drop them.
        // Hold them until our SDP has actually been sent.
        val canSendNow = synchronized(iceGateLock) {
            if (participantId in sdpSentTo) {
                true
            } else {
                heldLocalCandidates
                    .getOrPut(participantId) { mutableListOf() }
                    .add(candidate)
                false
            }
        }

        if (canSendNow) {
            emitIceCandidate(call.callId.value, participantId, candidate)
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing SDP / ICE ordering
    // -------------------------------------------------------------------------

    private val iceGateLock = Any()
    private val sdpSentTo = mutableSetOf<String>()
    private val heldLocalCandidates =
        mutableMapOf<String, MutableList<IceCandidate>>()

    private fun resetOutgoingIceGate(participantId: String) {
        synchronized(iceGateLock) {
            sdpSentTo.remove(participantId)
        }
    }

    private fun sendLocalSdp(
        action: CallSignalType,
        callId: String,
        target: String,
        sdp: String
    ) {
        emit(
            action = action,
            callId = callId,
            target = target,
            payload = CallPayload(sdp = sdp)
        )

        val held = synchronized(iceGateLock) {
            sdpSentTo.add(target)
            heldLocalCandidates.remove(target).orEmpty()
        }

        held.forEach { emitIceCandidate(callId, target, it) }
    }

    private fun emitIceCandidate(
        callId: String,
        target: String,
        candidate: IceCandidate
    ) {
        emit(
            action = CallSignalType.ICE_CANDIDATE,
            callId = callId,
            target = target,
            payload = CallPayload(
                candidate = candidate.sdp,
                sdpMid = candidate.sdpMid,
                sdpMLineIndex = candidate.sdpMLineIndex
            )
        )
    }

    private fun clearOutgoingIceState(participantId: String) {
        synchronized(iceGateLock) {
            sdpSentTo.remove(participantId)
            heldLocalCandidates.remove(participantId)
        }
    }

    override fun onRemoteStream(
        participantId: String,
        stream: MediaStream
    ) {
        // Remote stream is exposed by WebRtcManager.
        // UI observes WebRtcManager.remoteStreams.
    }

    override fun onConnectionStateChanged(
        participantId: String,
        state: PeerConnection.PeerConnectionState
    ) {

        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                scope.launch {
                    callManager.setState(
                        CallState.CONNECTED
                    )
                }

                publishCurrentMediaState()
                onPeerRecovered(participantId)

                callManager.updateParticipantStateAsync(
                    participantId = participantId,
                    state = ParticipantConnectionState.CONNECTED
                )
            }

            PeerConnection.PeerConnectionState.DISCONNECTED -> {

                // Grace period first; WebRTC may recover by itself.
                onPeerDisconnected(participantId)
            }

            PeerConnection.PeerConnectionState.FAILED -> {

                callManager.updateParticipantStateAsync(
                    participantId = participantId,
                    state = ParticipantConnectionState.DISCONNECTED
                )

                beginReconnect(participantId)
            }

            PeerConnection.PeerConnectionState.CLOSED -> {

                onPeerClosed(participantId)
            }

            PeerConnection.PeerConnectionState.NEW -> Unit

            PeerConnection.PeerConnectionState.CONNECTING -> Unit
        }
    }

    override fun onIceConnectionStateChanged(
        participantId: String,
        state: PeerConnection.IceConnectionState
    ) {

        when (state) {
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                onPeerRecovered(participantId)

                callManager.updateParticipantStateAsync(
                    participantId = participantId,
                    state = ParticipantConnectionState.CONNECTED
                )

                scope.launch {
                    callManager.setState(
                        CallState.CONNECTED
                    )
                }
            }

            PeerConnection.IceConnectionState.DISCONNECTED -> {
                onPeerDisconnected(participantId)
            }

            PeerConnection.IceConnectionState.FAILED -> {
                Log.e(
                    "WebRTC",
                    "ICE FAILED: participant=$participantId"
                )

                beginReconnect(participantId)
            }

            PeerConnection.IceConnectionState.CLOSED -> {
                onPeerClosed(participantId)
            }

            else -> Unit
        }
    }

    // Tells the remote side our real mic/camera state once media is flowing,
    // so its UI doesn't have to guess from defaults.
    private fun publishCurrentMediaState() {
        val call = callManager.activeCall.value ?: return

        call.participants.forEach { participant ->
            sendParticipantMediaChanged(
                callId = call.callId.value,
                target = participant.userNumber,
                isMicrophoneEnabled = callMediaManager.isMicrophoneEnabled.value,
                isCameraEnabled = callMediaManager.isSendingVideo,
                isScreenSharing = callMediaManager.isScreenSharing.value
            )
        }
    }

    private fun updateParticipantConnectionState(
        participantId: String,
        state: ParticipantConnectionState
    ) {
        callManager.updateParticipantStateAsync(
            participantId = participantId,
            state = state
        )
    }

    // -------------------------------------------------------------------------
    // Outgoing call signaling
    // -------------------------------------------------------------------------

    fun sendCallInvite(
        callId: String,
        target: String,
        callType: CallType,
        participant: CallParticipant? = null,
        roster: List<CallParticipant> = emptyList()
    ) {
        emit(
            action = CallSignalType.CALL_INVITE,
            callId = callId,
            target = target,
            payload = CallPayload(
                callType = callType.name,
                participant = participant?.toPayload(),
                participants = roster
                    .takeIf { it.isNotEmpty() }
                    // names / photos are resolved locally by each receiver
                    ?.map { it.toPayload().copy(displayName = null, profileUrl = null) }
            )
        )

        // Plain 1:1 ringing is covered by the call-level watchdog. Everything
        // else (group start, add to a running call) needs its own timer.
        val call = callManager.activeCall.value

        if (call != null &&
            call.callId.value == callId &&
            (call.participants.size > 1 || call.answeredAt != null)
        ) {
            val participantId = call.participants
                .firstOrNull { it.userNumber == target }
                ?.participantId ?: target

            armParticipantRingTimeout(callId, participantId)
        }
    }

    fun sendCallRinging(
        callId: String,
        target: String
    ) {
        emit(
            action = CallSignalType.CALL_RINGING,
            callId = callId,
            target = target
        )
    }

    fun sendCallAccept(
        callId: String,
        target: String
    ) {
        emit(
            action = CallSignalType.CALL_ACCEPT,
            callId = callId,
            target = target
        )
        callNotificationManager.cancelIncomingCall(callId)
    }

    fun sendCallReject(
        callId: String,
        target: String
    ) {
        emit(
            action = CallSignalType.CALL_REJECT,
            callId = callId,
            target = target
        )
        callNotificationManager.cancelIncomingCall(callId)
    }

    fun sendCallCancel(
        callId: String,
        target: String
    ) {
        emit(
            action = CallSignalType.CALL_CANCEL,
            callId = callId,
            target = target
        )
        callNotificationManager.cancelIncomingCall(callId)
    }

    fun sendCallBusy(
        callId: String,
        target: String
    ) {
        emit(
            action = CallSignalType.CALL_BUSY,
            callId = callId,
            target = target
        )

    }

    fun sendCallEnd(
        callId: String,
        target: String? = null
    ) {
        emit(
            action = CallSignalType.CALL_END,
            callId = callId,
            target = target
        )
        callNotificationManager.cancelIncomingCall(callId)
    }

    fun sendAddParticipant(
        callId: String,
        target: String,
        participant: CallParticipant
    ) {
        emit(
            action = CallSignalType.ADD_PARTICIPANT,
            callId = callId,
            target = target,
            payload = CallPayload(
                participant = participant.toPayload()
            )
        )
    }

    fun sendRemoveParticipant(
        callId: String,
        target: String,
        participantId: String
    ) {
        emit(
            action = CallSignalType.REMOVE_PARTICIPANT,
            callId = callId,
            target = target,
            payload = CallPayload(
                participantId = participantId
            )
        )
    }

    fun sendParticipantJoined(
        callId: String,
        target: String,
        participant: CallParticipant
    ) {
        emit(
            action = CallSignalType.PARTICIPANT_JOINED,
            callId = callId,
            target = target,
            payload = CallPayload(
                participant = participant.toPayload()
            )
        )
    }

    fun sendParticipantLeft(
        callId: String,
        target: String,
        participantId: String
    ) {
        emit(
            action = CallSignalType.PARTICIPANT_LEFT,
            callId = callId,
            target = target,
            payload = CallPayload(
                participantId = participantId
            )
        )
    }

    // -------------------------------------------------------------------------
    // Remote failure signaling
    // -------------------------------------------------------------------------

    private suspend fun handleCallFailed(data: CallData) {
        cancelIncomingUnlessGroup(data.callId)
        val sender = data.sender ?: return

        val call = callManager.activeCall.value ?: return
        if (call.callId.value != data.callId) return

        markRemoteGone(sender, ParticipantConnectionState.FAILED)

        // 1:1: unchanged behaviour.
        if (call.participants.size <= 1) {
            callManager.setState(CallState.FAILED)
            return
        }

        endWhenNobodyLeft(data.callId, failed = true)
    }

    // -------------------------------------------------------------------------
    // Wire helper
    // -------------------------------------------------------------------------

    private fun emit(
        action: CallSignalType,
        callId: String,
        target: String? = null,
        payload: CallPayload? = null
    ) {
        val callData = CallData(
            action = action,
            callId = callId,
            sender = null,
            target = target,
            timeStamp = System.currentTimeMillis(),
            payload = payload,
            revealSenderNumber = iAmRevealed[callId]
        )
        scope.launch {
            knotx.sendCallData(callData)
        }
    }

    fun shutdown() {
        reconnectJobs.values.forEach { it.cancel() }
        reconnectJobs.clear()
        disconnectGraceJobs.values.forEach { it.cancel() }
        disconnectGraceJobs.clear()
        reconnectStartedAt.clear()
        lastRestartAt.clear()
        clearGroupCallState()
        webRtcManager.setEventListener(null)
        incomingSignals.close()
        signalingScope.cancel()
        scope.cancel()
    }
}