package com.senghol.knotx.calling.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.webrtc.EglBase
import org.webrtc.MediaStream
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.ui.active.LocalCallControlsInset
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

/**
 * 1:1 call          -> big remote video + small tappable local tile.
 * 2-3 remotes (3-4 people) -> equal tiles in a 2-column grid.
 */
@Composable
fun ParticipantGrid(
    participants: List<CallParticipant>,
    remoteStreams: Map<String, MediaStream>,
    localVideoTrack: VideoTrack?,
    isFrontCamera: Boolean,
    videoEglContext: EglBase.Context,
    modifier: Modifier = Modifier,
    isCameraEnabled: Boolean = true
) {
    if (participants.isEmpty()) return

    // People who left / declined / timed out no longer get a tile.
    val visible = participants
        .filter { it.connectionState.isAlive }
        .ifEmpty { participants }

    // A disabled camera is a black frame, show "Camera off" instead.
    val localTrack = if (isCameraEnabled) localVideoTrack else null

    if (visible.size <= 1) {
        FocusedLayout(
            participants = visible,
            remoteStreams = remoteStreams,
            localVideoTrack = localTrack,
            isFrontCamera = isFrontCamera,
            videoEglContext = videoEglContext,
            modifier = modifier
        )
    } else {
        GroupGridLayout(
            participants = visible,
            remoteStreams = remoteStreams,
            localVideoTrack = localTrack,
            isFrontCamera = isFrontCamera,
            videoEglContext = videoEglContext,
            modifier = modifier
        )
    }
}

@Composable
private fun GroupGridLayout(
    participants: List<CallParticipant>,
    remoteStreams: Map<String, MediaStream>,
    localVideoTrack: VideoTrack?,
    isFrontCamera: Boolean,
    videoEglContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    // remotes first, "you" last; 2 per row (3 people: 2 + 1, 4 people: 2 + 2)
    val tileIds = participants.map { it.participantId } + "local"
    val rows = tileIds.chunked(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rows.forEachIndexed { rowIndex, rowIds ->
            // Only the bottom row sits above the call controls.
            val reserveControls = rowIndex == rows.lastIndex

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rowIds.forEach { id ->
                    val tileModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()

                    if (id == "local") {
                        LocalTile(
                            localVideoTrack = localVideoTrack,
                            isFrontCamera = isFrontCamera,
                            videoEglContext = videoEglContext,
                            reserveControls = reserveControls,
                            modifier = tileModifier
                        )
                    } else {
                        participants
                            .firstOrNull { it.participantId == id }
                            ?.let { participant ->
                                ParticipantTile(
                                    participant = participant,
                                    stream = remoteStreams[id],
                                    videoEglContext = videoEglContext,
                                    reserveControls = reserveControls,
                                    modifier = tileModifier
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalTile(
    localVideoTrack: VideoTrack?,
    isFrontCamera: Boolean,
    videoEglContext: EglBase.Context,
    reserveControls: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.DarkGray)) {
        if (localVideoTrack != null) {
            LocalVideoView(
                videoTrack = localVideoTrack,
                eglBaseContext = videoEglContext,
                modifier = Modifier.fillMaxSize(),
                mirror = isFrontCamera
            )
        } else {
            Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = "Camera off",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }

        NameBadge(
            text = "You",
            reserveControls = reserveControls,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun FocusedLayout(
    participants: List<CallParticipant>,
    remoteStreams: Map<String, MediaStream>,
    localVideoTrack: VideoTrack?,
    isFrontCamera: Boolean,
    videoEglContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    if (participants.isEmpty()) return

    var mainParticipantId by rememberSaveable { mutableStateOf<String?>(null) }

    val allParticipantIds = buildList {
        add("local")
        addAll(participants.map { it.participantId })
    }

    LaunchedEffect(allParticipantIds) {
        if (mainParticipantId == null || mainParticipantId !in allParticipantIds) {
            mainParticipantId = participants.firstOrNull()?.participantId
        }
    }

    val smallParticipantIds = allParticipantIds.filter {
        it != mainParticipantId
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // BIG VIDEO
        if (mainParticipantId == "local") {
            if (localVideoTrack != null) {
                LocalVideoView(
                    videoTrack = localVideoTrack,
                    eglBaseContext = videoEglContext,
                    modifier = Modifier.fillMaxSize(),
                    mirror = isFrontCamera
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera off",
                        color = Color.White
                    )
                }
            }
        } else {
            participants
                .firstOrNull { it.participantId == mainParticipantId }
                ?.let { participant ->
                    ParticipantTile(
                        participant = participant,
                        stream = remoteStreams[participant.participantId],
                        videoEglContext = videoEglContext,
                        modifier = Modifier.fillMaxSize()
                    )
                }
        }

        // SMALL VIDEOS
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            smallParticipantIds.forEach { participantId ->

                Box(
                    modifier = Modifier
                        .size(110.dp, 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            mainParticipantId = participantId
                        }
                ) {
                    if (participantId == "local") {
                        if (localVideoTrack != null) {
                            LocalVideoView(
                                videoTrack = localVideoTrack,
                                eglBaseContext = videoEglContext,
                                modifier = Modifier.fillMaxSize(),
                                mirror = isFrontCamera
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Camera off",
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        participants
                            .firstOrNull { it.participantId == participantId }
                            ?.let { participant ->
                                ParticipantTile(
                                    participant = participant,
                                    stream = remoteStreams[participantId],
                                    videoEglContext = videoEglContext,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantTile(
    participant: CallParticipant,
    stream: MediaStream?,
    videoEglContext: EglBase.Context,
    modifier: Modifier = Modifier,
    reserveControls: Boolean = true
) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    ) {
        if (stream != null && participant.isCameraEnabled) {
            RemoteVideoView(
                stream = stream,
                eglBaseContext = videoEglContext,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ParticipantPlaceholder(
                participant = participant,
                modifier = Modifier.fillMaxSize()
            )

            if (!participant.isCameraEnabled) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Camera off",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }
        }

        ParticipantOverlay(
            participant = participant,
            reserveControls = reserveControls,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun ParticipantPlaceholder(
    participant: CallParticipant,
    modifier: Modifier = Modifier
) {
    val name = participant.displayName
        ?: participant.userNameId
        ?: participant.userNumber

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = Color.White
        )

        val status = when (participant.connectionState) {
            ParticipantConnectionState.CONNECTING -> "Connecting..."
            ParticipantConnectionState.RECONNECTING -> "Reconnecting..."
            ParticipantConnectionState.RINGING -> "Ringing..."
            ParticipantConnectionState.INVITED -> "Invited"
            ParticipantConnectionState.DISCONNECTED -> "Disconnected"
            ParticipantConnectionState.FAILED -> "Connection failed"
            ParticipantConnectionState.DECLINED -> "Declined"
            ParticipantConnectionState.NO_ANSWER -> "No answer"
            ParticipantConnectionState.LEFT -> "Left"
            ParticipantConnectionState.CONNECTED -> null
        }

        if (status != null) {
            Text(
                text = status,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
            )
        }
    }
}

@Composable
private fun ParticipantOverlay(
    participant: CallParticipant,
    modifier: Modifier = Modifier,
    reserveControls: Boolean = true
) {
    val name = participant.displayName?.takeUnless { it.isBlank() }
        ?: participant.userNameId?.takeUnless { it.isBlank() }
        ?: participant.userNumber

    NameBadge(
        text = name,
        reserveControls = reserveControls,
        modifier = modifier
    )
}

/**
 * Name pill. [reserveControls] keeps it clear of the navigation bar and the
 * call-control bar; tiles in the upper rows of the grid don't need that.
 */
@Composable
private fun NameBadge(
    text: String,
    reserveControls: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (reserveControls) Modifier.navigationBarsPadding() else Modifier)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp + if (reserveControls) LocalCallControlsInset.current else 0.dp
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Raw video only: no self-view, no names, no icons, no status text. */
@Composable
fun PipVideoGrid(
    tiles: List<CallParticipant>,
    remoteStreams: Map<String, MediaStream>,
    videoEglContext: EglBase.Context
) {
    if (tiles.size == 1) {
        val id = tiles.first().participantId
        RemoteVideoView(
            stream = remoteStreams.getValue(id),
            eglBaseContext = videoEglContext,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                row.forEach { p ->
                    key(p.participantId) {
                        RemoteVideoView(
                            stream = remoteStreams.getValue(p.participantId),
                            eglBaseContext = videoEglContext,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}