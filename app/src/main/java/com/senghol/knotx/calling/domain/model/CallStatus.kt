package com.senghol.knotx.calling.domain.model


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.senghol.knotx.calling.domain.model.CallEndReason
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.ui.active.ActiveCallUiState

/**
 * What the user should see about the call, derived in ONE place so the voice
 * and video screens can never disagree.
 */
enum class CallStatus {
    CALLING,       // invite sent, other phone hasn't answered the signal yet
    RINGING,       // other phone is ringing
    CONNECTING,    // accepted, media is being set up
    CONNECTED,
    UNSTABLE,      // connected, but the link just dropped (during the 4 s grace)
    RECONNECTING   // link lost, restarting ICE
}

fun ActiveCallUiState.toCallStatus(): CallStatus = when (callState) {
    CallState.INITIATING -> CallStatus.CALLING
    CallState.RINGING -> CallStatus.RINGING
    CallState.ACCEPTING,
    CallState.CONNECTING -> CallStatus.CONNECTING

    CallState.RECONNECTING -> CallStatus.RECONNECTING

    CallState.CONNECTED ->
        if (participants.any {
                it.connectionState == ParticipantConnectionState.DISCONNECTED
            }
        ) {
            CallStatus.UNSTABLE
        } else {
            CallStatus.CONNECTED
        }

    else -> CallStatus.CONNECTING
}

/** True once media has flowed at least once (so remote video may be shown). */
val CallStatus.isEstablished: Boolean
    get() = this == CallStatus.CONNECTED ||
            this == CallStatus.UNSTABLE ||
            this == CallStatus.RECONNECTING

/** Text for the pill under the contact name. [duration] is e.g. "02:31". */
fun CallStatus.pillText(duration: String): String = when (this) {
    CallStatus.CALLING -> "Calling…"
    CallStatus.RINGING -> "Ringing…"
    CallStatus.CONNECTING -> "Connecting…"
    CallStatus.CONNECTED,
    CallStatus.UNSTABLE -> duration

    CallStatus.RECONNECTING -> "Reconnecting…"
}

/** Text for the banner shown on top of live video; null = no banner. */
val CallStatus.bannerText: String?
    get() = when (this) {
        CallStatus.UNSTABLE -> "Poor connection…"
        CallStatus.RECONNECTING -> "Reconnecting…"
        else -> null
    }

/** Why a call ended, for the ended-call screen. */
fun CallEndReason.displayText(): String = when (this) {
    CallEndReason.BUSY -> "User is busy"
    CallEndReason.REJECTED -> "Call declined"
    CallEndReason.MISSED -> "No answer"
    CallEndReason.NETWORK_ERROR -> "Connection lost"
    else -> "Call ended"
}

@Composable
fun CallStatusBanner(
    status: CallStatus,
    modifier: Modifier = Modifier
) {
    val text = status.bannerText ?: return

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.65f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}