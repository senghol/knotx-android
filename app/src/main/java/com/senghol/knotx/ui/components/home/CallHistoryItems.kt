package com.senghol.knotx.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.senghol.knotx.R
import com.senghol.knotx.calling.domain.model.CallDirection
import com.senghol.knotx.calling.domain.model.CallState
import com.senghol.knotx.ui.components.single.formatDurationCall
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.viewmodel.CallHistoryItem
import com.senghol.knotx.utils.data.CallSessionDisplay
import com.senghol.knotx.utils.enums.CallDataType
import com.senghol.knotx.utils.enums.CallResult
import java.text.DateFormat
import java.util.Date

@Composable
fun CallHistoryItem(session: CallSessionDisplay) {
    val call = session.callSession
    val outcome = call.outcome

    val formattedTime = DateFormat.getDateTimeInstance().format(Date(call.startTime))
    val hasDuration = (call.duration ?: 0) > 0
    val durationText = call.duration?.let { formatDurationCall(it) }

    val isIncoming = when (call.callDataType) {
        CallDataType.VIDEO_RECEIVED, CallDataType.AUDIO_RECEIVED -> true
        else -> false
    }

    val isVideoCall = when (call.callDataType) {
        CallDataType.VIDEO_INITIATING, CallDataType.VIDEO_RECEIVED -> true
        else -> false
    }

    val outcomeLabel = when (outcome) {
        CallResult.PICKED       -> "Answered"
        CallResult.MISSED       -> "Missed"
        CallResult.DECLINED     -> "Declined"
        CallResult.CANCELED     -> "Canceled"
        CallResult.FAILED       -> "Failed"
        CallResult.BUSY         -> "Busy"
        CallResult.NO_ANSWER    -> "No Answer"
        CallResult.DISCONNECTED -> "Disconnected"
        CallResult.ENDED -> "End"
    }

    val numberColor = MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    val photoUrl = session.photoUrl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 👤 Profile Picture
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Profile",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .padding(8.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        val displayName = session.userName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: call.receiver

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = numberColor
            )

            Text(text = outcomeLabel, style = MaterialTheme.typography.bodySmall, color = secondaryColor)

            if (hasDuration) {
                Text(text = durationText!!, style = MaterialTheme.typography.bodySmall, color = secondaryColor)
            }

            Text(text = formattedTime, style = MaterialTheme.typography.bodySmall, color = secondaryColor)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val arrowIcon = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived
            else Icons.AutoMirrored.Filled.CallMade
            val arrowTint = if (isIncoming) Color(0xFF094D0B) else Color(0xFFF44336)
            val typeIcon = if (isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call

            Icon(
                imageVector = arrowIcon,
                contentDescription = if (isIncoming) "Incoming" else "Outgoing",
                tint = arrowTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = typeIcon,
                contentDescription = if (isVideoCall) "Video call" else "Audio call",
                tint = secondaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun CallHistoryRow(
    item: CallHistoryItem,
    modifier: Modifier = Modifier,
    dootawasColors: DootawasColors,
    onClick: () -> Unit = {},
    onCallClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val participant = item.participants.firstOrNull()
    val contactName = participant?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: participant?.userNumber
        ?: "Unknown contact"

    val isVideo = item.callType.name.contains("VIDEO", ignoreCase = true)
    val isIncoming = item.direction == CallDirection.INCOMING
    val isMissed = item.state in setOf(
        CallState.MISSED,
        CallState.REJECTED,
        CallState.BUSY,
        CallState.FAILED
    )

    // Dynamic color coding for missed calls vs. regular calls
    val directionColor = if (isMissed) colors.error else colors.primary
    val directionIcon = when {
        isMissed -> Icons.Default.CallMissed
        isIncoming -> Icons.AutoMirrored.Filled.CallReceived
        else -> Icons.AutoMirrored.Filled.CallMade
    }

    val callLabel = when {
        isMissed -> "Missed"
        isIncoming -> "Incoming"
        else -> "Outgoing"
    }

    val timeText = remember(item.createdAt) {
        java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.SHORT,
            java.text.DateFormat.SHORT
        ).format(java.util.Date(item.createdAt))
    }

    val durationText = remember(item.durationSeconds) {
        val duration = item.durationSeconds
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = dootawasColors.homeChatBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Fallback Initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!participant?.profileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = participant?.profileUrl,
                        contentDescription = contactName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = contactName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Main Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contactName,
                    color = if (isMissed) colors.error else dootawasColors.homeText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isMissed) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = callLabel,
                        tint = directionColor,
                        modifier = Modifier.size(16.dp)
                    )

                    if (isVideo) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video call",
                            tint = colors.outline,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = buildString {
                            append(callLabel)
                            if (item.wasAnswered && durationText.isNotBlank()) {
                                append(" • ")
                                append(durationText)
                            }
                        },
                        color = dootawasColors.lastMessageColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right Actions & Date Column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeText,
                    color = colors.outline,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Call $contactName",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}