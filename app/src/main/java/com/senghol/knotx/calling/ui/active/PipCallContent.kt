package com.senghol.knotx.calling.ui.active

import com.senghol.knotx.calling.domain.model.CallStatus
import com.senghol.knotx.calling.domain.model.isEstablished
import com.senghol.knotx.calling.domain.model.toCallStatus
import com.senghol.knotx.calling.ui.components.ParticipantGrid


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senghol.knotx.calling.domain.model.isAlive
import com.senghol.knotx.calling.domain.model.pillText
import com.senghol.knotx.calling.ui.components.PipVideoGrid
import org.webrtc.EglBase

/**
 * What the tiny Picture-in-Picture window shows: ONLY the other person's
 * video. No header, no controls, no banner, no self-view.
 *
 * If there is no remote video yet (ringing / connecting / reconnecting) it
 * shows a small avatar and one short status line instead of a black box.
 */
@Composable
fun PipCallContent(
    state: ActiveCallUiState,
    videoEglContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    val status = state.toCallStatus()

    val liveRemotes = state.participants.filter { it.connectionState.isAlive }
    val participant = liveRemotes.ifEmpty { state.participants }.firstOrNull()

    val name = participant?.displayName?.takeIf { it.isNotBlank() }
        ?: participant?.userNameId?.takeIf { it.isNotBlank() }
        ?: participant?.userNumber
        ?: ""

    // Only people who are in the call AND actually sending video.
    val videoTiles = liveRemotes
        .filter { it.isCameraEnabled && state.remoteStreams[it.participantId] != null }
        .take(4)

    val hasRemoteVideo = status.isEstablished && videoTiles.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasRemoteVideo) {
            PipVideoGrid(
                tiles = videoTiles,
                remoteStreams = state.remoteStreams,
                videoEglContext = videoEglContext
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallAvatar(
                    name = name,
                    profileUrl = participant?.profileUrl,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = status.pillText(formatCallDuration(state.callDurationSeconds)),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Tiny spinner in the corner when the link is struggling
        if (status == CallStatus.RECONNECTING || status == CallStatus.UNSTABLE) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        }
    }
}
