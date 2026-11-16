package com.senghol.knotx.calling.domain.manager

import android.util.Log
import com.senghol.knotx.calling.data.webrtc.WebRtcManager
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
import com.senghol.knotx.calling.domain.repository.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// How long a call may sit in INITIATING/RINGING before we give up.
private const val RING_TIMEOUT_MS = 45_000L

// How long ACCEPTING/CONNECTING may last before the call is declared failed.
private const val CONNECT_TIMEOUT_MS = 30_000L

// Used by the "is this call still alive?" check (slightly above RING_TIMEOUT_MS).
private const val STALE_RINGING_MS = 60_000L

class CallManager(
    private val repository: CallRepository,
    private val webRtcManager: WebRtcManager
){

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val stateMutex = Mutex()

    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    /**
     * Invoked AFTER a watchdog moved the call to a final state
     * (MISSED / CANCELLED / FAILED). CallSignalingManager uses it to notify
     * the remote side. Media teardown is handled by observing activeCall.
     */
    var onCallTimeout: (suspend (CallSession, CallState) -> Unit)? = null

    /**
     * Called synchronously when a call reaches a terminal state, just before
     * activeCall is cleared. CallSoundManager plays busy/failed/end tones here.
     */
    var onCallEnded: ((CallSession) -> Unit)? = null

    /**
     * Every call that reaches a terminal state is published here (final state
     * and endReason filled in). The ViewModel uses it to show WHY a call ended
     * (busy / declined / no answer / failed) instead of a blank screen.
     */
    private val _callEnded = MutableSharedFlow<CallSession>(extraBufferCapacity = 8)
    val callEnded: SharedFlow<CallSession> = _callEnded.asSharedFlow()

    private var watchdogJob: Job? = null

    // -------------------------------------------------------------------------
    // Watchdog
    // -------------------------------------------------------------------------

    /**
     * Must be called with stateMutex held (or from a context where no other
     * state change can run). Re-arms the single timer for the call's CURRENT
     * state, so no call can stay in RINGING / ACCEPTING / CONNECTING forever.
     */
    private fun armWatchdog(call: CallSession) {
        watchdogJob?.cancel()
        watchdogJob = null

        val timeoutMs: Long
        val targetState: CallState
        val reason: CallEndReason

        when (call.state) {
            CallState.INITIATING,
            CallState.RINGING -> {
                timeoutMs = RING_TIMEOUT_MS

                if (call.direction == CallDirection.INCOMING) {
                    targetState = CallState.MISSED
                    reason = CallEndReason.MISSED
                } else {
                    targetState = CallState.CANCELLED
                    reason = CallEndReason.MISSED // outgoing + MISSED == "No answer"
                }
            }

            CallState.ACCEPTING,
            CallState.CONNECTING -> {
                timeoutMs = CONNECT_TIMEOUT_MS
                targetState = CallState.FAILED
                reason = CallEndReason.NETWORK_ERROR
            }

            else -> return
        }

        val callId = call.callId
        val expectedState = call.state

        watchdogJob = scope.launch {
            delay(timeoutMs)

            val current = _activeCall.value ?: return@launch

            if (current.callId != callId || current.state != expectedState) {
                return@launch
            }

            Log.w(
                "CallManager",
                "Watchdog: call ${callId.value} stuck in $expectedState " +
                        "for ${timeoutMs}ms -> $targetState"
            )

            if (setState(targetState, reason)) {
                onCallTimeout?.invoke(current, targetState)
            }
        }
    }

    private fun cancelWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    // -------------------------------------------------------------------------
    // Call creation
    // -------------------------------------------------------------------------

    suspend fun startCall(
        participants: List<CallParticipant>,
        callType: CallType
    ): CallSession? = stateMutex.withLock {

        val existingCall = _activeCall.value

        if (existingCall != null) {
            val age = System.currentTimeMillis() - existingCall.createdAt

            val isStaleRinging =
                existingCall.state == CallState.RINGING &&
                        age >= STALE_RINGING_MS

            if (isStaleRinging) {
                Log.w(
                    "CallManager",
                    "Cleaning stale RINGING call before starting new call: " +
                            "id=${existingCall.callId.value}, age=${age}ms"
                )

                repository.saveCall(
                    existingCall.copy(
                        state = CallState.MISSED,
                        endedAt = System.currentTimeMillis()
                    )
                )

                cancelWatchdog()
                _activeCall.value = null
                _isCallActive.value = false
            } else {
                Log.e(
                    "CallManager",
                    "Cannot start call - active call already exists: " +
                            "${existingCall.callId.value}, state=${existingCall.state}"
                )
                return@withLock null
            }
        }

        if (_isCallActive.value) {
            Log.e(
                "CallManager",
                "Cannot start call - call runtime is still active"
            )
            return@withLock null
        }

        val uniqueParticipants = participants
            .distinctBy { it.participantId }


        if (uniqueParticipants.isEmpty()) {
            Log.e("CallManager", "Cannot start call - no participants")
            return@withLock null
        }

        val callId = CallId(UUID.randomUUID().toString())
        val now = System.currentTimeMillis()

        val mediaTypes = buildSet {
            add(CallMediaType.AUDIO)

            if (callType == CallType.VIDEO) {
                add(CallMediaType.VIDEO)
            }
        }

        val call = CallSession(
            callId = callId,
            callType = callType,
            direction = CallDirection.OUTGOING,
            state = CallState.INITIATING,
            participants = uniqueParticipants,
            activeMediaTypes = mediaTypes,
            createdAt = now
        )

        repository.saveCall(call)

        _activeCall.value = call
        _isCallActive.value = true
        armWatchdog(call)


        call
    }

    fun start() {
        scope.launch {
            restoreActiveCall()
        }
    }

    private fun isCallRuntimeAlive(call: CallSession): Boolean {
        return when (call.state) {
            CallState.RINGING -> {
                System.currentTimeMillis() - call.createdAt <= STALE_RINGING_MS
            }

            CallState.INITIATING,
            CallState.ACCEPTING,
            CallState.CONNECTING,
            CallState.RECONNECTING,
            CallState.CONNECTED -> {
                webRtcManager.getPeerConnectionCount() > 0
            }

            else -> false
        }
    }

    suspend fun receiveIncomingCall(call: CallSession): Boolean = stateMutex.withLock {
        val current = _activeCall.value


        if (current == null) {
            repository.saveCall(call)
            _activeCall.value = call
            _isCallActive.value = true
            armWatchdog(call)


            return@withLock true
        }

        // The same person calling again while their previous invite is still
        // ringing here means the previous call is dead on their side (they
        // hung up / crashed and the CANCEL never reached us). One person can't
        // have two calls with us, so the new invite replaces the old one.
        val isSameCallerRetrying =
            current.direction == CallDirection.INCOMING &&
                    current.state == CallState.RINGING &&
                    current.participants.firstOrNull()?.participantId != null &&
                    current.participants.firstOrNull()?.participantId ==
                    call.participants.firstOrNull()?.participantId

        if (!isSameCallerRetrying && isCallRuntimeAlive(current)) {
            Log.w(
                "CallManager",
                "Incoming call rejected as BUSY: " +
                        "activeCall=${current.callId}, state=${current.state}"
            )

            return@withLock false
        }

        Log.w(
            "CallManager",
            if (isSameCallerRetrying) {
                "Previous invite from the same caller superseded: ${current.callId}"
            } else {
                "Existing call is stale. Cleaning it before accepting incoming call: " +
                        "${current.callId}"
            }
        )

        val now = System.currentTimeMillis()

        repository.updateCall(
            current.copy(
                state = if (isSameCallerRetrying) CallState.MISSED else CallState.FAILED,
                endedAt = now,
                endReason = if (isSameCallerRetrying) {
                    CallEndReason.MISSED
                } else {
                    CallEndReason.UNKNOWN
                }
            )
        )

        _activeCall.value = call
        _isCallActive.value = true
        armWatchdog(call)

        repository.saveCall(call)


        true
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    fun updateParticipantStateAsync(
        participantId: String,
        state: ParticipantConnectionState
    ) {
        scope.launch {
            updateParticipantState(
                participantId = participantId,
                state = state
            )
        }
    }

    fun setStateAsync(
        state: CallState,
        endReason: CallEndReason? = null
    ) {
        scope.launch {
            setState(
                state = state,
                endReason = endReason
            )
        }
    }

    suspend fun setState(
        state: CallState,
        endReason: CallEndReason? = null
    ): Boolean = stateMutex.withLock {
        val current = _activeCall.value ?: return@withLock false

        if (!isValidStateTransition(current.state, state)) {
            Log.w(
                "CallManager",
                "Invalid state transition: ${current.state} -> $state"
            )
            return@withLock false
        }

        val now = System.currentTimeMillis()

        val updated = current.copy(
            state = state,
            startedAt = if (
                state == CallState.CONNECTING &&
                current.startedAt == null
            ) {
                now
            } else {
                current.startedAt
            },
            answeredAt = if (
                state == CallState.CONNECTED &&
                current.answeredAt == null
            ) {
                now
            } else {
                current.answeredAt
            },
            endedAt = if (isTerminalState(state)) {
                now
            } else {
                current.endedAt
            },
            endReason = if (isTerminalState(state)) {
                endReason ?: current.endReason ?: state.toEndReason()
            } else {
                current.endReason
            }
        )

        _activeCall.value = updated
        repository.updateCall(updated)

        if (isTerminalState(state)) {
            cancelWatchdog()
            onCallEnded?.invoke(updated)
            _callEnded.tryEmit(updated)
            _isCallActive.value = false
            _activeCall.value = null
        } else if (current.state != state) {
            // New phase -> fresh timer (or none for CONNECTED etc.)
            armWatchdog(updated)
        }

        true
    }

    private fun isValidStateTransition(
        current: CallState,
        next: CallState
    ): Boolean {
        if (current == next) return true
        if (isTerminalState(current)) return false

        return when (current) {
            CallState.IDLE -> next in setOf(
                CallState.INITIATING,
                CallState.RINGING
            )

            CallState.INITIATING -> next in setOf(
                CallState.RINGING,
                CallState.ACCEPTING,
                CallState.CONNECTING,
                CallState.ENDING,
                CallState.CANCELLED,
                CallState.REJECTED,
                CallState.BUSY,
                CallState.FAILED
            )

            CallState.RINGING -> next in setOf(
                CallState.ACCEPTING,
                CallState.CONNECTING,
                CallState.ENDING,
                CallState.REJECTED,
                CallState.CANCELLED,
                CallState.BUSY,
                CallState.MISSED,
                CallState.FAILED
            )

            CallState.ACCEPTING -> next in setOf(
                CallState.CONNECTING,
                CallState.ENDING,
                CallState.CANCELLED,
                CallState.FAILED
            )

            CallState.CONNECTING -> next in setOf(
                CallState.CONNECTED,
                CallState.RECONNECTING,
                CallState.ENDING,
                CallState.CANCELLED,
                CallState.FAILED
            )

            CallState.CONNECTED -> next in setOf(
                CallState.RECONNECTING,
                CallState.ENDING,
                CallState.FAILED
            )

            CallState.RECONNECTING -> next in setOf(
                CallState.CONNECTING,
                CallState.CONNECTED,
                CallState.ENDING,
                CallState.FAILED
            )

            CallState.ENDING -> next in setOf(
                CallState.ENDED,
                CallState.FAILED
            )

            CallState.ENDED,
            CallState.REJECTED,
            CallState.CANCELLED,
            CallState.BUSY,
            CallState.MISSED,
            CallState.FAILED -> false
        }
    }

    // -------------------------------------------------------------------------
    // Accept / reject / cancel / end
    // -------------------------------------------------------------------------

    suspend fun acceptCall(): Boolean {
        val call = _activeCall.value ?: return false

        if (call.state != CallState.RINGING) {
            return false
        }

        return setState(CallState.ACCEPTING)
    }

    suspend fun rejectCall(
        reason: CallEndReason = CallEndReason.REJECTED
    ): Boolean {
        val call = _activeCall.value ?: return false

        if (call.state != CallState.RINGING &&
            call.state != CallState.ACCEPTING
        ) {
            return false
        }

        return setState(
            state = CallState.REJECTED,
            endReason = reason
        )
    }

    suspend fun cancelCall(): Boolean {
        val call = _activeCall.value ?: return false

        if (isTerminalState(call.state)) {
            return false
        }

        return setState(
            state = CallState.CANCELLED,
            endReason = CallEndReason.CANCELLED
        )
    }

    suspend fun endCall(): Boolean {
        val call = _activeCall.value ?: return false

        if (isTerminalState(call.state)) {
            clearActiveCall()
            return false
        }

        if (call.state != CallState.ENDING) {
            if (!setState(CallState.ENDING)) {
                return false
            }
        }

        return setState(
            state = CallState.ENDED,
            endReason = CallEndReason.LOCAL_ENDED
        )
    }

    suspend fun failCall(
        reason: CallEndReason = CallEndReason.UNKNOWN
    ) {
        if (_activeCall.value == null) return

        setState(
            state = CallState.FAILED,
            endReason = reason
        )
    }
    suspend fun recordMissedCall(call: CallSession) {
        repository.saveCall(
            call.copy(
                state = CallState.MISSED,
                endedAt = System.currentTimeMillis(),
                endReason = CallEndReason.MISSED
            )
        )
    }
    suspend fun markMissedCall() {
        if (_activeCall.value == null) return

        setState(
            state = CallState.MISSED,
            endReason = CallEndReason.MISSED
        )
    }

    // -------------------------------------------------------------------------
    // Participants
    // -------------------------------------------------------------------------

    suspend fun addParticipant(
        callId: String,
        participant: CallParticipant
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        if (call.callId.value != callId) {
            return@withLock false
        }

        if (call.participants.any {
                it.participantId == participant.participantId
            }
        ) {
            return@withLock false
        }

        val updated = call.copy(
            participants = call.participants + participant
        )

        updateActiveCallLocked(updated)

        true
    }

    suspend fun updateParticipant(
        callId: String,
        participant: CallParticipant
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        if (call.callId.value != callId) {
            return@withLock false
        }

        if (call.participants.none {
                it.participantId == participant.participantId
            }
        ) {
            return@withLock false
        }

        val updated = call.copy(
            participants = call.participants.map {
                if (it.participantId == participant.participantId) {
                    participant
                } else {
                    it
                }
            }
        )

        updateActiveCallLocked(updated)

        true
    }

    suspend fun removeParticipant(
        callId: String,
        participantId: String
    ): Boolean {
        val shouldEnd = stateMutex.withLock {
            val call = _activeCall.value ?: return@withLock null

            if (call.callId.value != callId) {
                return@withLock null
            }

            if (call.participants.none {
                    it.participantId == participantId
                }
            ) {
                return@withLock null
            }

            val updated = call.copy(
                participants = call.participants.filterNot {
                    it.participantId == participantId
                }
            )

            updateActiveCallLocked(updated)

            updated.participants.isEmpty()
        } ?: return false

        if (shouldEnd) {
            endCall()
        }

        return true
    }

    suspend fun updateParticipantState(
        participantId: String,
        state: ParticipantConnectionState
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        val index = call.participants.indexOfFirst {
            it.participantId == participantId
        }

        if (index == -1) {
            return@withLock false
        }

        val participant = call.participants[index]

        val updatedParticipant = participant.copy(
            connectionState = state,
            joinedAt = if (
                state == ParticipantConnectionState.CONNECTED &&
                participant.joinedAt == null
            ) {
                System.currentTimeMillis()
            } else {
                participant.joinedAt
            },
            leftAt = if (
                state == ParticipantConnectionState.LEFT ||
                state == ParticipantConnectionState.DISCONNECTED
            ) {
                System.currentTimeMillis()
            } else {
                participant.leftAt
            }
        )

        val updated = call.copy(
            participants = call.participants.map {
                if (it.participantId == participantId) {
                    updatedParticipant
                } else {
                    it
                }
            }
        )

        updateActiveCallLocked(updated)

        true
    }

    suspend fun setParticipantMicrophone(
        participantId: String,
        enabled: Boolean
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        val participant = call.participants.firstOrNull {
            it.participantId == participantId
        } ?: return@withLock false

        val updated = call.copy(
            participants = call.participants.map {
                if (it.participantId == participantId) {
                    participant.copy(
                        isMicrophoneEnabled = enabled
                    )
                } else {
                    it
                }
            }
        )

        updateActiveCallLocked(updated)

        true
    }

    suspend fun setParticipantCamera(
        participantId: String,
        enabled: Boolean
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        val participant = call.participants.firstOrNull {
            it.participantId == participantId
        } ?: return@withLock false

        val updated = call.copy(
            participants = call.participants.map {
                if (it.participantId == participantId) {
                    participant.copy(
                        isCameraEnabled = enabled
                    )
                } else {
                    it
                }
            }
        )

        updateActiveCallLocked(updated)

        true
    }

    suspend fun setParticipantScreenSharing(
        participantId: String,
        enabled: Boolean
    ): Boolean = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock false

        val participant = call.participants.firstOrNull {
            it.participantId == participantId
        } ?: return@withLock false

        val updated = call.copy(
            participants = call.participants.map {
                if (it.participantId == participantId) {
                    participant.copy(
                        isScreenSharing = enabled
                    )
                } else {
                    it
                }
            }
        )

        updateActiveCallLocked(updated)

        true
    }

    // -------------------------------------------------------------------------
    // Call media
    // -------------------------------------------------------------------------

    suspend fun addMediaType(
        mediaType: CallMediaType
    ) = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock

        if (mediaType in call.activeMediaTypes) {
            return@withLock
        }

        updateActiveCallLocked(
            call.copy(
                activeMediaTypes = call.activeMediaTypes + mediaType
            )
        )
    }

    suspend fun removeMediaType(
        mediaType: CallMediaType
    ) = stateMutex.withLock {
        val call = _activeCall.value ?: return@withLock

        if (mediaType == CallMediaType.AUDIO) {
            return@withLock
        }

        updateActiveCallLocked(
            call.copy(
                activeMediaTypes = call.activeMediaTypes - mediaType
            )
        )
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    fun getParticipant(
        participantId: String
    ): CallParticipant? {
        return _activeCall.value
            ?.participants
            ?.firstOrNull {
                it.participantId == participantId
            }
    }

    fun getParticipantCount(): Int {
        return _activeCall.value?.participants?.size ?: 0
    }

    fun isParticipantConnected(
        participantId: String
    ): Boolean {
        return getParticipant(participantId)
            ?.connectionState == ParticipantConnectionState.CONNECTED
    }

    // `participants` does NOT include the local user, so 2+ remotes = 3+ people.
    fun isGroupCall(): Boolean {
        return getParticipantCount() > 1
    }

    /** True while at least one remote is connected, connecting or still expected. */
    fun hasLiveRemotes(): Boolean {
        return _activeCall.value
            ?.participants
            ?.any { it.connectionState.isAlive }
            ?: false
    }

    // -------------------------------------------------------------------------
    // Active call
    // -------------------------------------------------------------------------

    private suspend fun updateActiveCallLocked(
        call: CallSession
    ) {
        val current = _activeCall.value ?: return

        if (current.callId != call.callId) {
            return
        }

        _activeCall.value = call
        repository.updateCall(call)
    }

    suspend fun clearActiveCall() {
        stateMutex.withLock {
            cancelWatchdog()
            _activeCall.value = null
            _isCallActive.value = false
        }
    }

    suspend fun shutdown() {
        clearActiveCall()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private suspend fun restoreActiveCall() {
        stateMutex.withLock {
            if (_isCallActive.value) return@withLock


            val call = repository.getActiveCall() ?: return@withLock
            val now = System.currentTimeMillis()

            when (call.state) {
                CallState.RINGING -> {
                    if (now - call.createdAt > STALE_RINGING_MS) {
                        repository.updateCall(
                            call.copy(
                                state = CallState.MISSED,
                                endedAt = now,
                                endReason = CallEndReason.MISSED
                            )
                        )
                        return@withLock
                    }
                }

                CallState.INITIATING,
                CallState.ACCEPTING,
                CallState.CONNECTING,
                CallState.RECONNECTING,
                CallState.CONNECTED -> {
                    // Do not restore these from DB alone.
                    // WebRTC runtime is the authority for an established/runtime call.

                    repository.updateCall(
                        call.copy(
                            state = CallState.FAILED,
                            endedAt = now,
                            endReason = CallEndReason.UNKNOWN
                        )
                    )
                    return@withLock
                }

                else -> return@withLock
            }

            _activeCall.value = call
            _isCallActive.value = true
            armWatchdog(call)

        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun isTerminalState(
        state: CallState
    ): Boolean {
        return state == CallState.ENDED ||
                state == CallState.REJECTED ||
                state == CallState.CANCELLED ||
                state == CallState.BUSY ||
                state == CallState.MISSED ||
                state == CallState.FAILED
    }

    private fun CallState.toEndReason(): CallEndReason {
        return when (this) {
            CallState.REJECTED -> CallEndReason.REJECTED
            CallState.CANCELLED -> CallEndReason.CANCELLED
            CallState.BUSY -> CallEndReason.BUSY
            CallState.MISSED -> CallEndReason.MISSED
            CallState.FAILED -> CallEndReason.UNKNOWN
            CallState.ENDED -> CallEndReason.REMOTE_ENDED
            else -> CallEndReason.UNKNOWN
        }
    }
}