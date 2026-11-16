package com.senghol.knotx.ui.calling
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.senghol.knotx.R
import com.senghol.knotx.utils.data.CallEvent
import com.senghol.knotx.utils.data.CallUiState
import org.webrtc.SurfaceViewRenderer
// Utility to format duration
import java.util.Locale
import com.senghol.knotx.utils.enums.AudioOutputDevice
import com.senghol.knotx.utils.enums.CallResult

fun formatCallDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, secs)
    }
}

@Composable
fun CallDurationChip(
    duration: Long,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatCallDuration(duration),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun InCallView(
    uiState: CallUiState,
    isVideoCall: Boolean,
    isInPipMode: Boolean,
    onEvent: (CallEvent) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    // Use rememberSaveable to persist isSwapped across configuration changes
    var isSwapped by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    // Create and remember the renderers once per Composable lifecycle
    val remoteRenderer = remember { SurfaceViewRenderer(context).apply { init(EglUtils.eglBaseContext, null) } }
    val localRenderer = remember { SurfaceViewRenderer(context).apply { init(EglUtils.eglBaseContext, null) } }
    // Dispose renderers when Composable is destroyed
    DisposableEffect(Unit) {
        onDispose {
            remoteRenderer.release()
            localRenderer.release()
        }
    }
    // Update sinks only when isSwapped or video tracks change
    LaunchedEffect(isSwapped, uiState.remoteVideoTrack, uiState.localVideoTrack, uiState.isFrontCamera) {

        uiState.remoteVideoTrack?.removeSink(remoteRenderer)
        uiState.remoteVideoTrack?.removeSink(localRenderer)
        uiState.localVideoTrack?.removeSink(remoteRenderer)
        uiState.localVideoTrack?.removeSink(localRenderer)

        if (isSwapped) {
            // swapped

            uiState.remoteVideoTrack?.addSink(localRenderer)
            uiState.localVideoTrack?.addSink(remoteRenderer)

            // local video is on remoteRenderer now
            localRenderer.setMirror(false) // remote
            remoteRenderer.setMirror(uiState.isFrontCamera) // local (mirror only if front)

        } else {
            // normal

            uiState.remoteVideoTrack?.addSink(remoteRenderer)
            uiState.localVideoTrack?.addSink(localRenderer)

            // local video is on localRenderer
            localRenderer.setMirror(uiState.isFrontCamera) // mirror only if front
            remoteRenderer.setMirror(false) // remote
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)) {
            // Always show call duration in the main view, top center
            val formattedTime = formatCallDuration(uiState.callDuration)
            // ⏱ Time overlay (always on top)
            CallDurationChip(
                duration = uiState.callDuration,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(2f)
            )
        if (isVideoCall) {
                // Main video view (always visible)
                AndroidView(
                    factory = { if (isSwapped) localRenderer else remoteRenderer },
                    modifier = Modifier.fillMaxSize()
                )
            // Smaller PiP view - show only if NOT in PiP mode
            if (!isInPipMode) {
                AndroidView(
                    factory = { if (isSwapped) remoteRenderer else localRenderer },
                    modifier = Modifier
                        .size(width = 125.dp, height = 160.dp) // Portrait: 2:3 aspect ratio
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isSwapped = !isSwapped }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp), // ⬅ under time
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 🔹 Name (big + bold)
                uiState.remoteUserName?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                if (uiState.remoteUserName != uiState.remoteUserNumber){
                    // 🔹 Number (smaller)
                    uiState.remoteUserNumber?.let {
                        Text(
                            text = it,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

            }
        }


        // 👆 PiP button (top start)
        if (!isInPipMode) {
            IconButton(
                onClick = onEnterPip,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.bottom_right_click_24px),
//                    contentDescription = "Enter PiP",
//                    tint = Color.White
//                )
            }
        }

        // 🔼 Arrow button (bottom center)
        if (!controlsVisible) {
            IconButton(
                onClick = { controlsVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.arrow_up_24px),
//                    contentDescription = "Show Controls",
//                    tint = Color.White
//                )
            }
        }
        // Controls UI composable
        CallControls(
            uiState = uiState,
            isInPipMode = isInPipMode,
            isVideo = isVideoCall,
            onEvent = onEvent,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

class CallAudioManager(context: Context) {
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0  // using same sound for both buttons (or add separate IDs)

    init {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(4)   // small number is enough for UI clicks
                .setAudioAttributes(attrs)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(4, AudioManager.STREAM_MUSIC, 0)
        }

        // Load once (replace mute_beep with your actual raw file name without .mp3)
//        clickSoundId = soundPool!!.load(context, R.raw.mouse_click, 1)
    }

    fun playClick() {
        soundPool?.play(clickSoundId, 0.7f, 0.7f, 1, 0, 1f)  // volume 70% – feels natural for UI
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

@Composable
fun CallControls(
    uiState: CallUiState,
    isInPipMode: Boolean,
    isVideo: Boolean,
    onEvent: (CallEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var audioDevicesExpanded by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val selectedDevice = uiState.availableAudioDevices.find { it.id == uiState.audioOutputDeviceId }
    val context = LocalContext.current
    val audioManager = remember { CallAudioManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioManager.release()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 🔊 Device Selector Row (animated and appears above buttons)
            AnimatedVisibility(
                visible = audioDevicesExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.availableAudioDevices.forEach { device ->
                        val isSelected = device.id == uiState.audioOutputDeviceId
                        IconButton(
                            onClick = {
                                onEvent(CallEvent.SetAudioOutput(device.type))
                                audioDevicesExpanded = false
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isSelected) Color.White else Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        ) {
//                            Icon(
//                                painter = painterResource(id = getAudioIcon(device.type)),
//                                contentDescription = device.name,
//                                tint = if (isSelected) Color.Black else Color.White,
//                                modifier = Modifier.size(24.dp)
//                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🎛️ Main Control Row
            AnimatedVisibility(
                visible = controlsVisible && !isInPipMode,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @Composable
                    fun controlButton(@DrawableRes icon: Int, onClick: () -> Unit, tint: Color = Color.White) {
                        IconButton(
                            onClick = onClick,
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = icon),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

//                    controlButton(
//                        icon = if (uiState.isMicMuted) R.drawable.mic_off_24px else R.drawable.mic_24px,
//                        onClick = {
//                            onEvent(CallEvent.ToggleMic)
//                            audioManager.playClick()
//                        }
//                    )
//
//                    if (isVideo){
//                        controlButton(
//                            icon = R.drawable.icon_camera_switch_24px,
//                            onClick = {
//                                onEvent(CallEvent.SwitchCamera)
//                                audioManager.playClick()
//                            }
//                        )
//                    }
//
//
//                    controlButton(
//                        icon = R.drawable.call_end_24px,
//                        onClick = { onEvent(CallEvent.EndCall(CallResult.CANCELED)) },
//                        tint = Color.Red
//                    )

                    // 🔊 Audio toggle (speaker, BT, etc.)
                    IconButton(
                        onClick = {
                            audioDevicesExpanded = !audioDevicesExpanded
                            onEvent(CallEvent.RefreshDevices)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
//                            Icon(
//                                painter = painterResource(id = getAudioIcon(uiState.audioOutputDevice)),
//                                contentDescription = selectedDevice?.name ?: "Audio Output",
//                                tint = Color.White,
//                                modifier = Modifier.size(24.dp)
//                            )
//                            Icon(
//                                painter = painterResource(
//                                    id = if (audioDevicesExpanded)
//
//                                        R.drawable.arrow_drop_down_24px
//                                    else
//                                        R.drawable.arrow_drop_up_24px
//
//                                ),
//                                contentDescription = if (audioDevicesExpanded) "Collapse Devices" else "Expand Devices",
//                                tint = Color.White,
//                                modifier = Modifier.size(20.dp)
//                            )
                        }
                    }
                }
            }

            // 🔼 Show Button if controls hidden
            if (!controlsVisible && !isInPipMode) {
                IconButton(
                    onClick = { controlsVisible = true },
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.arrow_up_24px),
//                        contentDescription = "Show Controls",
//                        tint = Color.White
//                    )
                }
            }
        }
    }
}

//@DrawableRes
//fun getAudioIcon(type: AudioOutputDevice?): Int {
//    return when (type) {
//        AudioOutputDevice.SPEAKER -> R.drawable.volume_up_24px
//        AudioOutputDevice.BLUETOOTH -> R.drawable.bluetooth_connected_24px
//        AudioOutputDevice.EARPIECE -> R.drawable.hearing_aid_left_24px
//        AudioOutputDevice.WIRED_HEADSET -> R.drawable.headset_mic_24px
//        else -> R.drawable.volume_up_24px
//    }
//}
