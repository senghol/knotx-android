package com.senghol.knotx.ui.components.group

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.senghol.knotx.R
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.single.formatDuration
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.ui.theme.GroupDootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupAudioBubble(
    sandeshId: String,
    userNumber: String,
    isPlaying: Boolean,
    userColor: Int,
    userImageUri: Uri?,
    savedFileUri: String?,
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
    onReplySlide: () -> Unit,
    dootawasColors: GroupDootawasColors
) {
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
            .padding(horizontal = 2.dp)
            .padding(top = 10.dp, bottom = 5.dp),
//            .animateContentSize(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {


        if (replyOriginalMessage != null) {
            ReplyBubble(replyOriginalMessage, isSelf, onReplyClick, dootawasColors.replyBackground,dootawasColors.replyText)
        }
        if (!isSelf) {

            if (userNumber.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start, // or End if needed
                    modifier = Modifier
                        .fillMaxWidth()   // 👈 REQUIRED
                        .padding(start = 6.dp, end = 8.dp)

                ) {

                    Text(
                        text = userNumber.removePrefix("+"),
                        style = TextStyle(
                            color = Color(userColor),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        )
                    )
                    if (userImageUri != null && userImageUri.toString().isNotBlank()) {
                        AsyncImage(
                            model = userImageUri,
                            contentDescription = "User Image",
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                }
            }
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
