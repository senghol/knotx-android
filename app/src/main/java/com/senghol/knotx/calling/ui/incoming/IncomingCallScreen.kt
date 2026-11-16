package com.senghol.knotx.calling.ui.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import androidx.compose.runtime.getValue

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.senghol.knotx.calling.domain.model.CallType
import kotlinx.coroutines.launch

@Composable
fun IncomingCallScreen(
    state: IncomingCallUiState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val participantUi = state.participant
    val displayName = participantUi?.displayName ?: "Incoming Call"
    val profileUrl = participantUi?.profileUrl
    val subDetail = participantUi?.participant?.userNameId
        ?: participantUi?.participant?.userNumber

    // --- Dynamic Ambient Pulsing Animations for Buttons ---
    val infiniteTransition = rememberInfiniteTransition(label = "ButtonPulseTransition")

    // Scale pulse for Accept button
    val acceptPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AcceptPulseScale"
    )

    // Secondary pulse delay simulation for Reject button
    val rejectPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.22f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RejectPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141A29),
                        Color(0xFF080B11)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- Center Section: Avatar & Call Info ---
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(130.dp),
                shape = CircleShape,
                color = Color(0xFF232836),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.2f)
                ),
                shadowElevation = 16.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!profileUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profileUrl,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.4.sp
                )
            )

            if (!subDetail.isNullOrBlank() && subDetail != displayName) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subDetail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = when (state.callType) {
                            CallType.VOICE -> Icons.Default.Call
                            CallType.VIDEO -> Icons.Default.Videocam
                            null -> Icons.Default.Call
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when (state.callType) {
                            CallType.VOICE -> "Incoming Voice Call"
                            CallType.VIDEO -> "Incoming Video Call"
                            null -> "Incoming Call"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // --- Bottom Section: Animated Call Action Buttons ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Reject Button
                AnimatedActionButton(
                    backgroundColor = Color(0xFFE53935),
                    pulseScale = rejectPulseScale,
                    isLoading = state.isRejecting,
                    isEnabled = !state.isAccepting && !state.isRejecting,
                    onClick = onReject
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Reject Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Animated Accept Button
                AnimatedActionButton(
                    backgroundColor = Color(0xFF4CAF50),
                    pulseScale = acceptPulseScale,
                    isLoading = state.isAccepting,
                    isEnabled = !state.isAccepting && !state.isRejecting,
                    onClick = onAccept
                ) {
                    Icon(
                        imageVector = when (state.callType) {
                            CallType.VIDEO -> Icons.Default.Videocam
                            else -> Icons.Default.Call
                        },
                        contentDescription = "Accept Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedActionButton(
    backgroundColor: Color,
    pulseScale: Float,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp)
    ) {
        // Ambient Glowing Pulse Ring
        if (isEnabled && !isLoading) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulseScale)
                    .background(
                        color = backgroundColor.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
        }

        // Interactive Button Shell
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .scale(pressScale.value)
                .clip(CircleShape)
                .background(
                    if (isEnabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
                )
                .pointerInput(isEnabled, isLoading) {
                    if (!isEnabled || isLoading) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            coroutineScope.launch {
                                pressScale.animateTo(0.88f, spring())
                            }
                            tryAwaitRelease()
                            coroutineScope.launch {
                                pressScale.animateTo(1f, spring())
                            }
                        },
                        onTap = { onClick() }
                    )
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                content()
            }
        }
    }
}