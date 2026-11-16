package com.senghol.knotx.ui.components.group

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.R
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.GroupParsedLocationSandesh
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.senghol.knotx.ui.theme.GroupDootawasColors
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupLocationBubble(
    sandeshId: String,
    userNumber: String,
    userColor: Int,
    userImageUri: Uri?,
    isSelf: Boolean,
    sandeshType: SandeshType,
    timeLong: Long,
    formattedTime: String,
    formattedDate: String,
    savedFileUri: String?,
    duplicateUri: String?,
    latitude: Double,
    longitude: Double,
    sandeshStithi: SandeshStithi,
    replyOriginalMessage: JsonObject?,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,      // NEW (optional): selected highlight, like the other bubbles
    isHighlighted: Boolean,
    onLongPress: () -> Unit,
    onToggleSelection: (Uri?) -> Unit,
    onDownloadClick: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    isLive: Boolean,
    liveUntil: Long?,
    isStopped: Boolean,
    currentTime: Long,
    onStopLiveLocation: (() -> Unit)? = null,
    onReplySlide: () -> Unit,
    dootawasColors: GroupDootawasColors
) {
    // ---------- Colors ----------
    val bubbleColor = when {
        isHighlighted -> Color(0xFFD50000)
        isSelected -> Color(0xFFB3E5FC)
        isSelf -> dootawasColors.rightMessageBackground
        else -> dootawasColors.leftMessageBackground
    }
    val timeBackground = if (isSelf) dootawasColors.rightTimeBackGround else dootawasColors.leftTimeBackGround

    val captionTimeColor = if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText
    // ---------- State ----------
    var isImageLoaded by remember { mutableStateOf(false) }
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val timeText = if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime

    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    // ---------- Live location state ----------
    val remainingMinutes =
        if (liveUntil != null) ((liveUntil - currentTime) / (60 * 1000)).coerceAtLeast(0) else 0
    val isExpired = isLive && remainingMinutes <= 0
    val liveText = when {
        isStopped -> "Stopped"
        remainingMinutes <= 0 -> "Expired"
        remainingMinutes >= 60 -> "Live ${remainingMinutes / 60}h"
        else -> "Live ${remainingMinutes}m"
    }
    val liveColor = when {
        isStopped -> Color.Gray.copy(alpha = 0.85f)
        isExpired -> Color.Red.copy(alpha = 0.85f)
        else -> Color(0xCC00C853)
    }
    val pinTint = when {
        !isLive -> Color.White
        isStopped -> Color.LightGray
        isExpired -> Color(0xFFFF8A80)
        else -> Color(0xFF69F0AE)
    }
    val showStop = isLive && isSelf && !isStopped && (liveUntil == null || liveUntil > currentTime)

    // The map image: saved file first, otherwise the duplicate (and ask for a download)
    val mapUri: String? = savedFileUri ?: duplicateUri
    if (savedFileUri == null) {
        LaunchedEffect(Unit) { onDownloadClick() }
    }

    val onBubbleClick = {
        val geoUri = "geo:$latitude,$longitude?q=$latitude,$longitude".toUri()
        onToggleSelection(geoUri)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {

        // Reply preview ABOVE the bubble (indented past the avatar for others' messages)
        if (replyOriginalMessage != null) {
            Box(modifier = Modifier.padding(start = if (!isSelf) 30.dp else 0.dp)) {
                ReplyBubble(
                    replyOriginalMessage,
                    isSelf,
                    onReplyClick,
                    dootawasColors.replyBackground,
                    dootawasColors.replyText
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        // Only the avatar + bubble move on swipe
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                },
            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top      // avatar sits at the TOP-left of the bubble
        ) {

            // Profile picture OUTSIDE the bubble, top-left (non-self only)
            if (!isSelf) {
                GroupAvatar(
                    imageUri = userImageUri,
                    number = userNumber,
                    color = Color(userColor),
                    onClick = onBubbleClick,
                    onLongClick = onLongPress
                )
                Spacer(Modifier.width(4.dp))
            }

            // ---------- Single bubble ----------
            Column(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 270.dp)
                    .background(bubbleColor, RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onBubbleClick, onLongClick = onLongPress)
                    .padding(3.dp)
            ) {

                // Username INSIDE the bubble, above the map
                if (!isSelf && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber.removePrefix("+"),
                        color = Color(userColor),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        style = GlbNoFontPadding,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {

                    // ----- Map image -----
                    mapUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                uri,
                                onState = {
                                    if (it is AsyncImagePainter.State.Success) isImageLoaded = true
                                }
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isImageLoaded) 1f else 0f)
                        )
                    }

                    // ----- Top-left: pin icon (+ live status) -----
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = if (isLive) "Live location" else "Current location",
                                tint = pinTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isLive) {
                            Text(
                                text = liveText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(liveColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // ----- Top-right: stop live sharing -----
                    if (showStop) {
                        Text(
                            text = "Stop",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color(0xCCD32F2F), RoundedCornerShape(10.dp))
                                .clickable { onStopLiveLocation?.invoke() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // ----- Time pill, bottom-right -----
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(timeBackground, RoundedCornerShape(5.dp))
                            .clickable { showRelativeTime = !showRelativeTime }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            color = captionTimeColor,
                            style = GlbNoFontPadding
                        )
                        statusColor?.let {
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.icon_status),
                                contentDescription = null,
                                tint = it,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val GlbNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

// ---------- Avatar (image, or first-letter fallback) ----------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlbAvatar(
    imageUri: Uri?,
    number: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val size = 26.dp
    val clickModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)

    if (imageUri != null && imageUri.toString().isNotBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = "User Image",
            contentScale = ContentScale.Crop,
            modifier = clickModifier.background(Color.White)
        )
    } else {
        Box(
            modifier = clickModifier.background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.removePrefix("+").firstOrNull()?.toString() ?: "?",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}