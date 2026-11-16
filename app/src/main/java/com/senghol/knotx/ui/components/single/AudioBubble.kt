package com.senghol.knotx.ui.components.single
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.senghol.knotx.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.gson.JsonObject
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.enums.SandeshStithi

import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import kotlin.math.roundToInt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioBubble(
    sandeshId: String,
    savedFileUri: String?,
    isPlaying: Boolean,
    exoPlayer: ExoPlayer?,
    currentlyPlayingId: String?,
    onPlayClick: () -> Unit,
    downloadPercent: Int,
    downloadSize: String,
    isSelf: Boolean,
    sandeshStithi: SandeshStithi,
    timeLong: Long,
    formattedTime: String,
    replyOriginalMessage: JsonObject?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onToggleSelection: (String?) -> Unit,
    onDownloadClick: () -> Unit,
    onReplyClick: (String) -> Unit,
    isHighlighted: Boolean,
    duration: Long,
    dootawasColors: DootawasColors,
    onReplySlide: () -> Unit
) {
//    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var showRelativeTime by remember { mutableStateOf(false) }

    val timeTextColor = if (isSelf) Color.Black else Color.White
    val timeBackgroundColor = if (isSelf) Color(0xFFFFFFFF) else Color(0xFF000000)
    val borderColor = remember(isHighlighted, isSelected, isSelf) {
        when {
            isHighlighted -> Color(0xFFD50000)
            isSelected -> Color.Blue
            isSelf -> dootawasColors.rightMessageBackground
            else -> dootawasColors.leftMessageBackground
        }
    }
    val displayedTime = remember(showRelativeTime, timeLong, formattedTime) {
        if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    }


    val isCurrentAudio = sandeshId == currentlyPlayingId
    val showPlaying = isCurrentAudio && isPlaying
    var offsetX by remember { mutableFloatStateOf(0f) }

val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    LaunchedEffect(showPlaying) {

        if (!isCurrentAudio) return@LaunchedEffect

        val player = exoPlayer ?: return@LaunchedEffect

        while (player.isPlaying) {

            position = player.currentPosition
//            duration = player.duration

            delay(200)
        }
    }
    DisposableEffect(exoPlayer) {

        val listener = object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {

                if (state == Player.STATE_ENDED) {
                    exoPlayer?.seekTo(0)
                }
            }
        }

        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
//            .animateContentSize(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {

        if (replyOriginalMessage != null) {
            ReplyBubble(replyOriginalMessage, isSelf, onReplyClick, dootawasColors.replyBackground,dootawasColors.replyText)
        }


        ConstraintLayout {
            val (audioBox, timeText) = createRefs()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(maxSwipe, trigger) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            val touchSlop = viewConfiguration.touchSlop

                            var totalDragX = 0f
                            var totalDragY = 0f
                            var horizontalStarted = false

                            horizontalDrag(down.id) { change ->
                                val dx = change.positionChange().x
                                val dy = change.positionChange().y

                                totalDragX += dx
                                totalDragY += dy

                                if (!horizontalStarted) {
                                    if (kotlin.math.abs(totalDragY) > touchSlop) {
                                        return@horizontalDrag
                                    }

                                    if (totalDragX > touchSlop) {
                                        horizontalStarted = true
                                    } else {
                                        return@horizontalDrag
                                    }
                                }

                                if (dx > 0 || offsetX > 0f) {
                                    offsetX = (offsetX + dx).coerceIn(0f, maxSwipe)
                                }

                                change.consume()
                            }

                            if (horizontalStarted && offsetX >= trigger) {
                                onReplySlide()
                            }

                            offsetX = 0f
                        }
                    }
                    .constrainAs(audioBox) {
                        if (isSelf) end.linkTo(parent.end, margin = 1.dp)
                        else start.linkTo(parent.start, margin = 1.dp)
                        top.linkTo(parent.top)
                    }
                    .widthIn(min = 180.dp, max = 300.dp)
                    .background(
                        MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(
                            12.dp
                        )
                    )
                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {

                            onToggleSelection(savedFileUri)

                        },
                        onLongClick = onLongPress
                    )
                    .padding(6.dp)
            )
            {
                if (downloadPercent < 100 && !isSelf) {
                    // Show download controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (downloadPercent == 0) {
                            IconButton(onClick = onDownloadClick) {
                                Icon(Icons.Default.Add, contentDescription = "Download", tint = Color.Black)
                            }
                            Text(
                                text = downloadSize,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { downloadPercent / 100f },
                                modifier = Modifier.size(32.dp),
                                color = Color.Black
                            )
                            Text(
                                text = "$downloadPercent%",
                                color = Color.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    savedFileUri?.let {
                                        onPlayClick()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (showPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (showPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = formatDuration(position),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Slider(
                                value = position.toFloat(),
                                onValueChange = {
//                                    exoPlayer?.seekTo(it.toLong())
                                    position = it.toLong()
                                },
                                valueRange = 0f..(duration.takeIf { it > 0 } ?: 1).toFloat(),
                                modifier = Modifier.weight(1f),
                            )

                            Text(
                                text = formatDuration(duration),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
            }
            }

            // ⏰ Time
            Text(
                text = buildAnnotatedString {
                    append(displayedTime)
                    if (sandeshStithi != SandeshStithi.LIKHIT && isSelf) {
                        append(" ")
                        appendInlineContent("icon")
                    }
                },
                fontSize = 11.sp,
                color = timeTextColor,
                modifier = Modifier
                    .constrainAs(timeText) {
                        bottom.linkTo(audioBox.bottom, margin = (-11).dp)
                        if (isSelf) end.linkTo(audioBox.end, margin = 4.dp)
                        else start.linkTo(audioBox.start, margin = 4.dp)
                    }
                    .background(timeBackgroundColor, shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp)
                    .clickable { showRelativeTime = !showRelativeTime },
                inlineContent = if (isSelf) {
                    mapOf("icon" to InlineTextContent(
                        placeholder = Placeholder(
                            width = 14.sp,
                            height = 14.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        val iconRes = when (sandeshStithi) {
                            SandeshStithi.PREKSHIT, SandeshStithi.SAMARPIT, SandeshStithi.DHRIST -> R.drawable.icon_status
                            else -> null
                        }
                        val iconColor = when (sandeshStithi) {
                            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
                            SandeshStithi.PREKSHIT -> Color.Gray
                            SandeshStithi.DHRIST -> Color(0xFF00C853)
                            else -> Color.Unspecified
                        }
                        iconRes?.let {
                            Icon(
                                painter = painterResource(id = it),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = iconColor
                            )
                        }
                    })
                } else emptyMap()
            )
        }
    }
}


fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}

fun formatDurationCall(seconds: Long): String {

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 ->
            "${hours}h ${minutes}m ${secs}s"

        minutes > 0 ->
            "${minutes}m ${secs}s"

        else ->
            "${secs}s"
    }
}