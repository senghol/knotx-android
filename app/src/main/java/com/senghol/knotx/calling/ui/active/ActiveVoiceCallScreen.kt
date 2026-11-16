package com.senghol.knotx.calling.ui.active

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text

import androidx.compose.ui.unit.dp
import com.senghol.knotx.calling.domain.model.CallState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallStatus
import com.senghol.knotx.calling.domain.model.CallStatusBanner
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.domain.model.pillText
import com.senghol.knotx.calling.domain.model.toCallStatus

// COPY-PASTE REPLACEMENT: replace your ActiveVoiceCallScreen function with this
// one and keep your existing imports. CallAvatar, toCallStatus, pillText and
// CallStatusBanner are in package ui.active (import them if your screen is
// in a different package).
//
// Changes vs your version:
//  1. real contact photo via CallAvatar (letter fallback, never a blank circle)
//  2. blank displayName falls through to username / number
//  3. status text from the shared CallStatus: Calling / Ringing / Connecting /
//     timer / Reconnecting (INITIATING and RINGING used to both say "Calling...")
//  4. amber colours + "Poor connection..." banner when the link is unstable

@Composable
fun ActiveVoiceCallScreen(
    state: ActiveCallUiState,
    onEnd: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onAddParticipant: (() -> Unit)? = null
) {
    val liveRemotes = state.participants.filter { it.connectionState.isAlive }
    val participant = liveRemotes.ifEmpty { state.participants }.firstOrNull()
    val displayName = when {
        liveRemotes.size <= 1 -> participant?.let {
            it.displayName?.takeIf { n -> n.isNotBlank() }
                ?: it.userNameId?.takeIf { n -> n.isNotBlank() }
                ?: it.userNumber
        } ?: "Unknown Caller"
        else -> liveRemotes.take(2).joinToString(", ") {
            it.displayName?.takeIf { n -> n.isNotBlank() } ?: it.userNumber
        } + if (liveRemotes.size > 2) " +${liveRemotes.size - 2}" else ""
    }

    val status = state.toCallStatus()
    val isConnected = status == CallStatus.CONNECTED
    val isDegraded = status == CallStatus.UNSTABLE || status == CallStatus.RECONNECTING

    val statusText = status.pillText(formatCallDuration(state.callDurationSeconds))

    // Badge colours: green = fine, amber = link problem, white = not connected yet
    val badgeAccent = when {
        isConnected -> Color(0xFF4CAF50)
        isDegraded -> Color(0xFFFFA000)
        else -> Color.White
    }
    val badgeText = when {
        isConnected -> Color(0xFF81C784)
        isDegraded -> Color(0xFFFFCC80)
        else -> Color.White.copy(alpha = 0.8f)
    }


    // --- Subtle Audio Pulse Animation ---
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.22f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141923),
                        Color(0xFF090A0F)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Poor connection banner (only while still connected but unstable) ---
        if (status == CallStatus.UNSTABLE) {
            CallStatusBanner(
                status = status,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        if (onAddParticipant != null) {
            IconButton(
                onClick = onAddParticipant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, "Add people", tint = Color.White)
            }
        }

        // --- Center Section: Avatar & Status Info ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulse Ring Container
            if (liveRemotes.size > 1) {
                Box(
                    modifier = Modifier.height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GroupAvatars(liveRemotes)
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Outer Pulse Layer
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .background(
                                color = if (isConnected) Color(0xFF2196F3).copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                    )

                    // Main Avatar: contact photo, or the initial if there is none
                    CallAvatar(
                        name = displayName,
                        profileUrl = participant?.profileUrl,
                        size = 130.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Participant Name
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status / Duration Badge
            Surface(
                shape = CircleShape,
                color = badgeAccent.copy(alpha = if (badgeAccent == Color.White) 0.1f else 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = badgeAccent.copy(alpha = if (badgeAccent == Color.White) 0.15f else 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    if (isConnected || isDegraded) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(badgeAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeText
                    )
                }
            }
        }

        // --- Bottom Floating Control Bar ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            VoiceCallControls(
                isMicrophoneEnabled = state.isMicrophoneEnabled,
                isSpeakerEnabled = state.isSpeakerEnabled,
                onToggleMicrophone = onToggleMicrophone,
                onToggleSpeaker = onToggleSpeaker,
                onEndCall = onEnd
            )
        }
    }
}
@Composable
private fun VoiceCallControls(
    isMicrophoneEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    onToggleMicrophone: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFF1E2029).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoiceCallControlButton(
                icon = if (isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                isActive = isMicrophoneEnabled,
                onClick = onToggleMicrophone
            )

            VoiceCallControlButton(
                icon = Icons.Default.VolumeUp,
                isActive = isSpeakerEnabled,
                onClick = onToggleSpeaker
            )

            // End Call Button
            FilledIconButton(
                onClick = onEndCall,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceCallControlButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isActive) Color.White.copy(alpha = 0.15f) else Color.White,
            contentColor = if (isActive) Color.White else Color.Black
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun GroupAvatars(participants: List<CallParticipant>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        participants.forEach { p ->
            val name = p.displayName?.takeIf { it.isNotBlank() }
                ?: p.userNameId?.takeIf { it.isNotBlank() }
                ?: p.userNumber

            val sub = when (p.connectionState) {
                ParticipantConnectionState.INVITED,
                ParticipantConnectionState.RINGING -> "Ringing…"
                ParticipantConnectionState.CONNECTING -> "Connecting…"
                ParticipantConnectionState.RECONNECTING -> "Reconnecting…"
                ParticipantConnectionState.DISCONNECTED -> "Disconnected"
                else -> null
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(84.dp)
            ) {
                CallAvatar(name = name, profileUrl = p.profileUrl, size = 72.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (sub != null) {
                    Text(text = sub, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}