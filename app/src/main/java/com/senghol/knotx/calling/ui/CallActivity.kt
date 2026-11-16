package com.senghol.knotx.calling.ui

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.senghol.knotx.KnotxApp

import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.ui.active.ActiveVideoCallScreen
import com.senghol.knotx.calling.ui.active.ActiveVoiceCallScreen
import com.senghol.knotx.calling.ui.incoming.IncomingCallScreen

import android.content.res.Configuration
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.util.Rational
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.senghol.knotx.calling.domain.model.CallMediaType
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.service.CallService
import com.senghol.knotx.calling.ui.ended.EndedCallScreen
import com.senghol.knotx.ui.contactPicker.ContactPickerScreen
import com.senghol.knotx.ui.contactPicker.PickerSources
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


// COPY-PASTE REPLACEMENT: keep your existing `package` line and imports, and ADD:
//
//   import android.app.PictureInPictureParams
//   import android.content.pm.PackageManager
//   import android.media.AudioManager
//   import android.os.Build
//   import android.util.Rational
//   import androidx.compose.runtime.getValue
//   import androidx.compose.runtime.mutableStateOf
//   import androidx.compose.runtime.setValue
//
// MANIFEST (required, otherwise PiP crashes or the activity restarts on resize):
//
//   <activity
//       android:name=".calling.ui.CallActivity"   <- your real name
//       android:supportsPictureInPicture="true"
//       android:resizeableActivity="true"
//       android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />
//
// What changed:
//  - isInPip is a Compose-observable flag passed to ActiveVideoCallScreen,
//    which then shows ONLY the remote video (no controls / header / self-view)
//  - PiP is only enabled during an ACTIVE VIDEO call (auto-enter on Android 12+,
//    onUserLeaveHint below that), and switched off otherwise so leaving the app
//    during an incoming call doesn't shrink it into a tiny window
//  - PiP params are built in one place and wrapped in try/catch + feature check
//  - if the call ends while in PiP the window just closes
//  - finish() calls moved out of composition into LaunchedEffect
//  - call volume buttons control call volume, not media volume

// COPY-PASTE REPLACEMENT: keep your existing `package` line and imports, and ADD:
//
//   import android.app.PictureInPictureParams
//   import android.content.pm.PackageManager
//   import android.media.AudioManager
//   import android.os.Build
//   import android.util.Rational
//   import android.view.KeyEvent
//   import androidx.compose.runtime.getValue
//   import androidx.compose.runtime.mutableStateOf
//   import androidx.compose.runtime.setValue
//
// MANIFEST (required, otherwise PiP crashes or the activity restarts on resize):
//
//   <activity
//       android:name=".calling.ui.CallActivity"   <- your real name
//       android:supportsPictureInPicture="true"
//       android:resizeableActivity="true"
//       android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />
//
// What changed:
//  - isInPip is a Compose-observable flag passed to ActiveVideoCallScreen,
//    which then shows ONLY the remote video (no controls / header / self-view)
//  - PiP is only enabled during an ACTIVE VIDEO call (auto-enter on Android 12+,
//    onUserLeaveHint below that), and switched off otherwise so leaving the app
//    during an incoming call doesn't shrink it into a tiny window
//  - PiP params are built in one place and wrapped in try/catch + feature check
//  - if the call ends while in PiP the window just closes
//  - finish() calls moved out of composition into LaunchedEffect
//  - call volume buttons control call volume, not media volume
// COPY-PASTE REPLACEMENT: keep your existing `package` line and imports, and ADD:
//
//   import android.app.PictureInPictureParams
//   import android.content.pm.PackageManager
//   import android.media.AudioManager
//   import android.os.Build
//   import android.util.Rational
//   import android.view.KeyEvent
//   import android.app.Activity
//   import android.media.projection.MediaProjectionManager
//   import androidx.activity.result.contract.ActivityResultContracts
//   import com.senghol.knotx.calling.domain.model.CallMediaType
//   import androidx.compose.runtime.getValue
//   import androidx.compose.runtime.mutableStateOf
//   import androidx.compose.runtime.setValue
//
// MANIFEST (required, otherwise PiP crashes or the activity restarts on resize):
//
//   <activity
//       android:name=".calling.ui.CallActivity"   <- your real name
//       android:supportsPictureInPicture="true"
//       android:resizeableActivity="true"
//       android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation" />
//
// What changed:
//  - isInPip is a Compose-observable flag passed to ActiveVideoCallScreen,
//    which then shows ONLY the remote video (no controls / header / self-view)
//  - PiP is only enabled during an ACTIVE VIDEO call (auto-enter on Android 12+,
//    onUserLeaveHint below that), and switched off otherwise so leaving the app
//    during an incoming call doesn't shrink it into a tiny window
//  - PiP params are built in one place and wrapped in try/catch + feature check
//  - if the call ends while in PiP the window just closes
//  - finish() calls moved out of composition into LaunchedEffect
//  - call volume buttons control call volume, not media volume

class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels {
        CallViewModelFactory(
            callContainer = (application as KnotxApp).callContainer
        )
    }

    private val callSoundManager by lazy {
        (application as KnotxApp).callContainer.callSoundManager
    }

    // System "start recording / casting?" dialog for screen sharing.
    private val screenShareLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                // CallService becomes a mediaProjection foreground service,
                // THEN starts the capture.
                CallService.startScreenShare(this, data)
            } else {
            }
        }

    private var skipPipOnce = false
    private fun toggleScreenShare(isSharing: Boolean) {
        if (isSharing) {
            CallService.stopScreenShare(this)
        } else {
            val manager = getSystemService(MediaProjectionManager::class.java)
            screenShareLauncher.launch(manager.createScreenCaptureIntent())
        }
    }

    // Observed by Compose: true while the PiP window is showing.
    private var isInPip by mutableStateOf(false)

    companion object {
        const val EXTRA_AUTO_ACCEPT = "auto_accept"
    }

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val granted = results.isNotEmpty() && results.values.all { it }
            viewModel.onCallPermissionResult(granted)
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Volume keys should control the CALL volume (also ringback / busy tones).
        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        // Correct after a recreation while already in PiP.
        isInPip = isInPictureInPictureMode

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPipSupported() && isVideoCallActive()) {
                    enterPip()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContent {
            val permissionRequest by viewModel.callPermissionRequest.collectAsState()

            LaunchedEffect(permissionRequest) {
                permissionRequest?.let { request ->
                    callPermissionLauncher.launch(request.permissions.toTypedArray())
                }
            }
            var showAddPicker by rememberSaveable { mutableStateOf(false) }

            val uiState by viewModel.uiState.collectAsState()

            val audioRoute by viewModel.currentAudioRoute.collectAsState()
            val availableAudioDevices by viewModel.availableAudioDevices.collectAsState()

            LaunchedEffect(uiState) { if (uiState !is CallUiState.Active) showAddPicker = false }
            val isVideoActive =
                (uiState as? CallUiState.Active)?.state?.callType == CallType.VIDEO

            // Keep "enter PiP automatically on Home" in sync with the call:
            // ON only while an active video call is showing.
            LaunchedEffect(isVideoActive) {
                updatePipParams(autoEnter = isVideoActive)
            }

            LaunchedEffect(Unit) { consumeAutoAccept() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        // PiP has no system bars; padding would leave a gap.
                        if (isInPip) Modifier else Modifier.statusBarsPadding()
                    )
            ) {
                when (val state = uiState) {
                    CallUiState.Finished -> {
                        LaunchedEffect(Unit) { finish() }
                    }

                    CallUiState.Idle -> {
                        // Nothing to show. If it STAYS idle (call rejected or
                        // cancelled elsewhere, e.g. from the notification), close.
                        // The effect is cancelled automatically if a call appears.
                        LaunchedEffect(Unit) {
                            delay(4_000.milliseconds)
                            finish()
                        }
                    }
                    is CallUiState.Ended -> {
                        if (isInPip) {
                            // The PiP window is too small for the ended screen:
                            // just close it.
                            LaunchedEffect(Unit) { finish() }
                        } else {
                            EndedCallScreen(
                                state = state.state,
                                onAudioCall = viewModel::recallAudio,
                                onVideoCall = viewModel::recallVideo,
                                onDismiss = viewModel::dismissEndedCall
                            )
                        }
                    }

                    is CallUiState.Incoming -> {
                        IncomingCallScreen(
                            state = state.state,
                            onAccept = viewModel::requestAcceptCall,
                            onReject = viewModel::rejectCall
                        )
                    }

                    is CallUiState.Active -> {
                        when (state.state.callType) {

                            CallType.VOICE -> {
                                ActiveVoiceCallScreen(
                                    state = state.state,
                                    onEnd = viewModel::endCall,
                                    onToggleMicrophone = viewModel::toggleMicrophone,
                                    onToggleSpeaker = viewModel::toggleSpeaker,
                                    onAddParticipant = if (viewModel.canAddParticipant()) ({ showAddPicker = true }) else null
                                )
                            }

                            CallType.VIDEO -> {
                                val isSharing =
                                    CallMediaType.SCREEN in state.state.activeMediaTypes

                                ActiveVideoCallScreen(
                                    state = state.state,
                                    localVideoTrack = viewModel.localVideoTrack,
                                    videoEglContext = viewModel.videoEglContext,
                                    audioRoute = audioRoute,
                                    availableAudioDevices = availableAudioDevices,
                                    onEnd = viewModel::endCall,
                                    onToggleMicrophone = viewModel::toggleMicrophone,
                                    onToggleCamera = viewModel::toggleCamera,
                                    onSwitchCamera = viewModel::switchCamera,
                                    onAudioRouteSelected = viewModel::setAudioRoute,
                                    isScreenSharing = isSharing,
                                    onToggleScreenShare = { toggleScreenShare(isSharing) },
                                    isInPip = isInPip,
                                    onAddParticipant = if (viewModel.canAddParticipant()) ({ showAddPicker = true }) else null,
                                )
                            }

                            null -> {
                                LaunchedEffect(Unit) { finish() }
                            }
                        }
                    }

                    is CallUiState.Error -> {
                        LaunchedEffect(Unit) { finish() }
                    }
                }
                if (showAddPicker && !isInPip && uiState is CallUiState.Active) {
                    ContactPickerScreen(
                        sources = PickerSources(
                            numberContacts = true,
                            usernameContacts = true,
                            groups = false,
                            deviceContacts = true
                        ),
                        title = "Add people",
                        confirmLabel = "Add",
                        onBack = { showAddPicker = false },
                        onConfirm = { picked ->
                            showAddPicker = false
                            viewModel.addParticipants(
                                picked.filter { !it.isGroup }.map {
                                    CallParticipant(
                                        participantId = it.userNumber,
                                        userNumber = it.userNumber,
                                        userNameId = null,
                                        displayName = it.userName.takeIf { n -> n.isNotBlank() },
                                        profileUrl = it.profileUri
                                    )
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // Volume button while an incoming call is ringing: silence the ring and
    // vibration (like a normal phone call). The call screen stays up.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            viewModel.uiState.value is CallUiState.Incoming
        ) {
            callSoundManager.silenceRinging()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Screen already open and the user pressed Accept on the notification:
        // this is the only place that sees it.
        consumeAutoAccept()
    }

    private fun consumeAutoAccept() {
        if (!intent.getBooleanExtra(EXTRA_AUTO_ACCEPT, false)) return

        // consume it so a rotation / recreation doesn't accept again
        intent.removeExtra(EXTRA_AUTO_ACCEPT)

        viewModel.requestAcceptCall()
    }



    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        isInPip = isInPictureInPictureMode

    }

    // -------------------------------------------------------------------------
    // Picture-in-Picture helpers
    // -------------------------------------------------------------------------

    private fun isVideoCallActive(): Boolean {
        val ui = viewModel.uiState.value
        return ui is CallUiState.Active && ui.state.callType == CallType.VIDEO
    }

    private fun isPipSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder
                .setAutoEnterEnabled(autoEnter)
                .setSeamlessResizeEnabled(false) // video, not a static layout
        }

        return builder.build()
    }

    private fun updatePipParams(autoEnter: Boolean) {
        if (!isPipSupported()) return

        try {
            setPictureInPictureParams(buildPipParams(autoEnter))
        } catch (e: IllegalStateException) {
            Log.e("CallPiP", "setPictureInPictureParams failed", e)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (skipPipOnce) return
        if (!isPipSupported() || !isVideoCallActive()) return

        // Swap to the PiP layout BEFORE the window shrinks (no "full screen
        // squeezed into a tiny window" flash).
        isInPip = true

        // Android 12+ enters PiP by itself (auto-enter in the params).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPip()
        }
    }

    override fun onResume() {
        super.onResume()
        skipPipOnce = false
        // Undo the early flag if PiP never actually started.
        isInPip = isInPictureInPictureMode
    }

    private fun enterPip() {
        if (!isPipSupported() || !isVideoCallActive()) return

        isInPip = true
        try {
            // returns false (without throwing) when the system refuses
            val entered = enterPictureInPictureMode(buildPipParams(autoEnter = true))
            if (!entered) isInPip = false
        } catch (e: IllegalStateException) {
            isInPip = false
            Log.e("CallPiP", "enterPictureInPictureMode failed", e)
        }
    }

//    override fun onStop() {
//        super.onStop()
//        // The PiP window was dismissed: the activity stops while still "in PiP".
//        if (isInPip && lifecycle.currentState == Lifecycle.State.CREATED) {
//            viewModel.endCall()
//        }
//    }
}