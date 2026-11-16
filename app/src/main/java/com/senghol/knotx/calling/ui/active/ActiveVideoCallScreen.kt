package com.senghol.knotx.calling.ui.active

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text

import androidx.compose.ui.unit.dp
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.ui.components.LocalVideoPreview
import com.senghol.knotx.calling.ui.components.LocalVideoView
import com.senghol.knotx.calling.ui.components.ParticipantGrid
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.senghol.knotx.calling.domain.manager.CallAudioDevice
import com.senghol.knotx.calling.domain.manager.CallAudioRoute
import com.senghol.knotx.calling.domain.model.CallStatus
import com.senghol.knotx.calling.domain.model.CallStatusBanner
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.domain.model.isEstablished
import com.senghol.knotx.calling.domain.model.pillText
import com.senghol.knotx.calling.domain.model.toCallStatus
import kotlin.math.roundToInt

fun formatCallDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// COPY-PASTE REPLACEMENT: replace your ActiveVideoCallScreen function with this
// one and keep your existing imports. CallAvatar, toCallStatus, isEstablished,
// pillText and CallStatusBanner live in the same package (ui.active); if your
// screen is in a different package, import them.
//
// Changes vs your version:
//  1. connecting overlay uses CallAvatar (photo + name fallback)
//  2. status text: Calling / Ringing / Connecting / duration / Reconnecting
//  3. remote video stays visible while RECONNECTING, with a banner on top
//  4. green dot in the header turns amber when the link is unstable

// COPY-PASTE REPLACEMENT: replace your ActiveVideoCallScreen function with this
// one and keep your existing imports. CallAvatar, toCallStatus, isEstablished,
// pillText and CallStatusBanner live in the same package (ui.active); if your
// screen is in a different package, import them.
//
// Changes vs your version:
//  1. connecting overlay uses CallAvatar (photo + name fallback)
//  2. status text: Calling / Ringing / Connecting / duration / Reconnecting
//  3. remote video stays visible while RECONNECTING, with a banner on top
//  4. green dot in the header turns amber when the link is unstable
//  5. new isInPip parameter: in Picture-in-Picture only the remote video is shown

// COPY-PASTE REPLACEMENT: replace your ActiveVideoCallScreen function with this
// one and keep your existing imports. CallAvatar, toCallStatus, isEstablished,
// pillText and CallStatusBanner live in the same package (ui.active); if your
// screen is in a different package, import them.
//
// Changes vs your version:
//  1. connecting overlay uses CallAvatar (photo + name fallback)
//  2. status text: Calling / Ringing / Connecting / duration / Reconnecting
//  3. remote video stays visible while RECONNECTING, with a banner on top
//  4. green dot in the header turns amber when the link is unstable
//  5. new isInPip parameter: in Picture-in-Picture only the remote video is shown
//  6. screen sharing: share / stop button in the header, a "You're sharing your
//     screen" banner with Stop, and your own tile is hidden while sharing
//     (otherwise you would see yourself recursively).
//     Extra imports: androidx.compose.material.icons.filled.ScreenShare,
//     androidx.compose.material.icons.filled.StopScreenShare,
//     androidx.compose.material3.IconButton, androidx.compose.material3.Icon,
//     androidx.compose.material3.TextButton

// Height the bottom control bar takes. The name label inside the video tile
// lifts itself by this much, so it always sits just above the controls and
// drops to the bottom when they are hidden.
val LocalCallControlsInset = compositionLocalOf { 0.dp }
@Composable
fun ActiveVideoCallScreen(
    state: ActiveCallUiState,
    localVideoTrack: VideoTrack?,
    videoEglContext: EglBase.Context,
    audioRoute: CallAudioRoute,
    availableAudioDevices: List<CallAudioDevice>,
    onEnd: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAudioRouteSelected: (CallAudioRoute) -> Unit,
    isScreenSharing: Boolean = false,
    onToggleScreenShare: () -> Unit = {},
    isInPip: Boolean = false,
    onAddParticipant: (() -> Unit)?= null
) {
    val liveRemotes = state.participants.filter { it.connectionState.isAlive }
    val participant = liveRemotes.ifEmpty { state.participants }.firstOrNull()
    val displayName = when {
        liveRemotes.size <= 1 -> participant?.let {
            it.displayName?.takeIf { n -> n.isNotBlank() }
                ?: it.userNameId?.takeIf { n -> n.isNotBlank() }
                ?: it.userNumber
        } ?: "Connecting..."
        else -> liveRemotes.take(2).joinToString(", ") {
            it.displayName?.takeIf { n -> n.isNotBlank() } ?: it.userNumber
        } + if (liveRemotes.size > 2) " +${liveRemotes.size - 2}" else ""
    }

    val status = state.toCallStatus()
    val isEstablished = status.isEstablished
    val showingRemote = isEstablished && state.remoteStreams.isNotEmpty()

    // Tap the video to hide / show the controls
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    val showControls = controlsVisible || !showingRemote

    // roughly the height of the control bar; 0 when it is hidden
    val controlsInset by animateDpAsState(
        targetValue = if (showControls) 88.dp else 0.dp,
        label = "controlsInset"
    )

    // banners sit below the header, or near the top when it is hidden
    val bannerTop = if (isEstablished && showControls) 124.dp else 16.dp

    // Picture-in-Picture: the window is tiny, so show ONLY the remote video.
    if (isInPip) {
        PipCallContent(
            state = state,
            videoEglContext = videoEglContext
        )
        return
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // --- 1. Background / Main Stream ---
        if (showingRemote) {
            CompositionLocalProvider(LocalCallControlsInset provides controlsInset) {
                ParticipantGrid(
                    participants = state.participants,
                    remoteStreams = state.remoteStreams,
                    // While sharing, the local track carries the screen: don't show it
                    localVideoTrack = if (isScreenSharing) null else localVideoTrack,
                    isFrontCamera = state.isFrontCamera,
                    videoEglContext = videoEglContext,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Pre-call background preview or blurred camera feed
            if (localVideoTrack != null && state.isCameraEnabled) {
                LocalVideoView(
                    videoTrack = localVideoTrack,
                    eglBaseContext = videoEglContext,
                    mirror = state.isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Connecting/Calling Center Avatar Info Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.85f)),
                            radius = 1200f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CallAvatar(
                        name = displayName,
                        profileUrl = participant?.profileUrl,
                        size = 96.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = status.pillText(
                                formatCallDuration(state.callDurationSeconds)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Top Header Bar (while the call is established) ---
        AnimatedVisibility(
            visible = isEstablished && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (status == CallStatus.CONNECTED) {
                                        Color(0xFF4CAF50)
                                    } else {
                                        Color(0xFFFFA000)
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCallDuration(state.callDurationSeconds),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Small share / stop-sharing button under the timer
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = if (isScreenSharing) {
                            Color(0xFFE53935)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        }
                    ) {
                        IconButton(
                            onClick = onToggleScreenShare,
                            enabled = isScreenSharing || status == CallStatus.CONNECTED,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isScreenSharing) {
                                    Icons.AutoMirrored.Filled.StopScreenShare
                                } else {
                                    Icons.AutoMirrored.Filled.ScreenShare
                                },
                                contentDescription = if (isScreenSharing) {
                                    "Stop sharing"
                                } else {
                                    "Share screen"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (onAddParticipant != null) {
                    IconButton(
                        onClick = onAddParticipant,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.PersonAdd, "Add people", tint = Color.White)
                    }
                }


            }
        }

        // --- "You're sharing your screen" banner ---
        if (isScreenSharing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = bannerTop),
                shape = CircleShape,
                color = Color(0xFFE53935).copy(alpha = 0.9f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                ) {
                    Text(
                        text = "You're sharing your screen",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )

                    TextButton(onClick = onToggleScreenShare) {
                        Text(
                            text = "Stop",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 3. Poor connection / Reconnecting banner over live video ---
        if (showingRemote) {
            CallStatusBanner(
                status = status,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (isScreenSharing) bannerTop + 44.dp else bannerTop)
            )
        }

        // --- 4. Floating Modern Bottom Control Bar (tap the video to hide / show) ---
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                // keep the buttons clear of the gesture bar / 3-button navigation
                .navigationBarsPadding()
                    .padding(bottom = 4.dp, top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            VideoCallControls(
                isMicrophoneEnabled = state.isMicrophoneEnabled,
                isCameraEnabled = state.isCameraEnabled,
                audioRoute = audioRoute,
                availableAudioDevices = availableAudioDevices,
                onToggleMicrophone = onToggleMicrophone,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = onSwitchCamera,
                onAudioRouteSelected = onAudioRouteSelected,
                onEndCall = onEnd
            )
            }
        }
    }
}

@Composable
private fun VideoCallControls(
    isMicrophoneEnabled: Boolean,
    isCameraEnabled: Boolean,
    audioRoute: CallAudioRoute,
    availableAudioDevices: List<CallAudioDevice>,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onAudioRouteSelected: (CallAudioRoute) -> Unit,
    onEndCall: () -> Unit
) {
    var showAudioOutputMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoCallControlButton(
                icon = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                isActive = isMicrophoneEnabled,
                onClick = onToggleMicrophone
            )

            VideoCallControlButton(
                icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                isActive = isCameraEnabled,
                onClick = onToggleCamera
            )

            VideoCallControlButton(
                icon = Icons.Default.Cameraswitch,
                isActive = true,
                onClick = onSwitchCamera
            )

            Box {
                VideoCallControlButton(
                    icon = when (audioRoute) {
                        CallAudioRoute.SPEAKER -> Icons.Default.VolumeUp
                        CallAudioRoute.EARPIECE -> Icons.Default.PhoneInTalk
                        CallAudioRoute.BLUETOOTH -> Icons.Default.Bluetooth
                        CallAudioRoute.WIRED_HEADSET -> Icons.Default.Headset
                    },
                    isActive = true,
                    onClick = {
                        showAudioOutputMenu = true
                    }
                )

                DropdownMenu(
                    expanded = showAudioOutputMenu,
                    onDismissRequest = {
                        showAudioOutputMenu = false
                    }
                ) {
                    availableAudioDevices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(device.name)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (device.route) {
                                        CallAudioRoute.SPEAKER ->
                                            Icons.Default.VolumeUp

                                        CallAudioRoute.EARPIECE ->
                                            Icons.Default.PhoneInTalk

                                        CallAudioRoute.BLUETOOTH ->
                                            Icons.Default.Bluetooth

                                        CallAudioRoute.WIRED_HEADSET ->
                                            Icons.Default.Headset
                                    },
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (device.route == audioRoute) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected"
                                    )
                                }
                            },
                            onClick = {
                                onAudioRouteSelected(device.route)
                                showAudioOutputMenu = false
                            }
                        )
                    }
                }
            }

            FilledIconButton(
                onClick = onEndCall,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoCallControlButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isActive) Color.White.copy(alpha = 0.18f) else Color.White,
            contentColor = if (isActive) Color.White else Color.Black
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
    }
}