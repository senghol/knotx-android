package com.senghol.knotx.ui.calling
import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.senghol.knotx.R

import com.senghol.knotx.utils.data.CallEvent
import com.senghol.knotx.utils.data.CallUiState
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.senghol.knotx.utils.enums.CallResult

@Composable
private fun PulsingRing(
    color: Color,
    size: Dp,
    delayMillis: Int
) {
    val transition = rememberInfiniteTransition(label = "pulse")

    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = delayMillis, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = delayMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}
@Composable
fun CallingPulseAvatar(
    imageUrl: String?,
    ringColor: Color = Color(0xFF4CAF50), // or Color(0xFFA3ECA7) for your light green vibe
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Three elegant pulsing rings
        repeat(3) { index ->
            PulsingRing(
                color = ringColor,
                delayMs = index * 400,
                maxScale = 1.28f + index * 0.14f,
                durationMs = 2200
            )
        }

        // Center avatar with subtle glow & border
        Box(
            modifier = Modifier
                .size(104.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = ringColor.copy(alpha = 0.4f)
                )
        ) {
//            AsyncImage(
//                model = imageUrl,
//                contentDescription = "Caller",
//                modifier = Modifier
//                    .matchParentSize()
//                    .clip(CircleShape)
//                    .background(Color(0xFF1A1A1A)),
//                contentScale = ContentScale.Crop,
//                placeholder = painterResource(R.drawable.person_24px),
//                error = painterResource(R.drawable.person_24px)
//            )

            // Subtle white rim
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun PulsingRing(
    color: Color,
    delayMs: Int,
    maxScale: Float = 1.45f,
    durationMs: Int = 2200
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$delayMs")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMs,
                easing = FastOutSlowInEasing,
                delayMillis = delayMs
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMs,
                easing = LinearEasing,
                delayMillis = delayMs
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
            .background(color.copy(alpha = alpha), CircleShape)
            .then(
                if (alpha > 0.08f) {
                    Modifier.border(
                        width = 1.2.dp,
                        color = color.copy(alpha = alpha * 0.6f),
                        shape = CircleShape
                    )
                } else Modifier
            )
    )
}

fun startIncomingCallVibration(context: Context) {
    val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

    val pattern = longArrayOf(0, 1000, 1000)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.vibrate(
            VibrationEffect.createWaveform(pattern, 0)
        )
    } else {
        @Suppress("DEPRECATION")
        val vibrator =
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, 0)
    }
}

fun stopIncomingCallVibration(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator.cancel()
    } else {
        @Suppress("DEPRECATION")
        val vibrator =
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }
}

//@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IncomingCallView(
    uiState: CallUiState,
    isVideoCall: Boolean,
    onEvent: (CallEvent) -> Unit,
) {
    val context = LocalContext.current
    val timeoutMillis = 37_000L

//    val callPermissions = rememberMultiplePermissionsState(
//        permissions = buildList {
//            add(Manifest.permission.CAMERA)
//            add(Manifest.permission.RECORD_AUDIO)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                add(Manifest.permission.BLUETOOTH_CONNECT)
//            }
//        }
//    )

//    // ⏱️ Ask permissions once
//    LaunchedEffect(Unit) {
//        callPermissions.launchMultiplePermissionRequest()
//    }
//
//    val hasCallPermission by remember {
//        derivedStateOf { callPermissions.allPermissionsGranted }
//    }



    // ⏳ Auto reject
    LaunchedEffect(Unit) {
        delay(timeoutMillis)
        onEvent(CallEvent.RejectCall(CallResult.MISSED))
    }


    // 🖼️ UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .union(WindowInsets(bottom = 12.dp))        // minimum bottom space
            )
    ) {

        // 🔔 TOP TEXT
        Text(
            text = "Incoming ${if (isVideoCall) "Video" else "Audio"} Call",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE91E63)
        )

        // 👤 CENTER CONTENT
        Column(
            modifier = Modifier.align(Alignment.Center)
            .offset(y = (-64).dp), // 🔼 move up
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CallingPulseAvatar(
                imageUrl = uiState.remoteUserProfileUrl,
                ringColor = Color(0xFFA3ECA7),
                modifier = Modifier
            )

//            Box(contentAlignment = Alignment.Center) {
//                PulsingRing(Color(0xFFA3ECA7), 160.dp, 0)
//                PulsingRing(Color(0xFF055626), 160.dp, 800)
//
//                // 👤 Caller Image
//                AsyncImage(
//                    model = uiState.remoteUserProfileUrl,
//                    contentDescription = "Caller Image",
//                    modifier = Modifier
//                        .size(96.dp)
//                        .clip(CircleShape)
//                        .background(Color.DarkGray),
//                    contentScale = ContentScale.Crop,
//                    error = painterResource(R.drawable.person_24px),
//                    placeholder = painterResource(R.drawable.person_24px)
//                )
//            }

            Spacer(Modifier.height(24.dp))

            val name = uiState.remoteUserName?.trim()
            val number = uiState.remoteUserNumber?.trim()

            Text(
                text = name ?: number ?: "Unknown",
                style = MaterialTheme.typography.headlineMedium, // ⬆ bigger
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (!number.isNullOrEmpty() && number != name) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // ☎️ BOTTOM ACTIONS
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            // ❌ Reject
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = { onEvent(CallEvent.RejectCall(CallResult.DECLINED)) },
                    containerColor = Color.Red,
                    modifier = Modifier.size(64.dp)
                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.call_end_24px),
//                        contentDescription = "Reject",
//                        tint = Color.White,
//                        modifier = Modifier.size(32.dp)
//                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Decline", color = Color.White)
            }

            // ✅ Accept
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = {
//                        if (hasCallPermission) {
//                            onEvent(CallEvent.AcceptCall)
//                        } else {
//                            Toast.makeText(
//                                context,
//                                "Permission required",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
                    },
                    containerColor = Color(0xFF2ECC71),
                    modifier = Modifier.size(64.dp)
                ) {
//                    Icon(
//                        painter = painterResource(
//                            if (isVideoCall)
//                                R.drawable.videocam_24px
//                            else
//                                R.drawable.call_24px
//                        ),
//                        contentDescription = "Accept",
//                        tint = Color.White,
//                        modifier = Modifier.size(32.dp)
//                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Accept", color = Color.White)
            }
        }
    }
}

