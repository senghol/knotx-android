package com.senghol.knotx.ui.components.group

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.gson.JsonParser
import com.senghol.knotx.R

import com.senghol.knotx.utils.enums.SandeshStithi
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel

import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import com.senghol.knotx.ui.components.ImageBubbleContent
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import com.senghol.knotx.ui.theme.GroupDootawasColors
import kotlin.math.roundToInt
@Composable
fun SentImageLoader(
    imageUris: List<String>,
    blurUri: String?,
    downloadPercent: Int,
    downloadSize: String,
    onDownloadClickImage: () -> Unit,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    onCancelDownload: () -> Unit
) {

    var isError by remember { mutableStateOf(false) }

    val displayUri = imageUris.firstOrNull()?.toUri()
    val blurUriParsed = blurUri?.toUri()

    val uriToDisplay = when (downloadPercent) {
        100 -> displayUri
        in 0..99 -> blurUriParsed ?: displayUri
        else -> null
    }

    val painter = rememberAsyncImagePainter(
        model = uriToDisplay,
        imageLoader = imageLoader,
        onState = { state ->
            isError = state is AsyncImagePainter.State.Error
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {

        // 🖼️ PREVIEW IMAGE
        if (uriToDisplay != null) {

            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // ❌ ERROR
        if (isError) {

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )
        }

        // 🔥 MULTI IMAGE BADGE
        if (imageUris.size > 1) {

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {

                Text(
                    text = "+${imageUris.size - 1}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ⬇️ DOWNLOAD / PROGRESS
        if (downloadPercent in 0..99) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {

                if (downloadPercent == 0) {

                    IconButton(onClick = onDownloadClickImage) {

                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = downloadSize,
                        color = Color.White,
                        fontSize = 12.sp
                    )

                } else {

                    CircularProgressIndicator(
                        progress = { downloadPercent / 100f },
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = "$downloadPercent%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    TextButton(onClick = onCancelDownload) {

                        Text(
                            "Cancel",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupImageBubble(
    userNumber: String,
    userColor: Int,
    userImageUri: Uri?,
    isSelf: Boolean,
    imageUris: List<String>,
    blurFileUri: String?,
    downloadPercent: Int,
    downloadSize: String,
    timeLong: Long,
    formattedTime: String,
    replyOriginalMessage: JsonObject?,
    isSelected: Boolean = false,
    paath: String,
    onLongPress: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    isHighlighted: Boolean,
    sandeshStithi: SandeshStithi,
    imageLoader: ImageLoader,
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
    val captionColor = if (isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
    val captionTimeColor = if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText

    // ---------- State ----------
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val timeText = if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    val toggleTime = { showRelativeTime = !showRelativeTime }

    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    // Reserve room for the time at the end of the caption
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val timeSpaceWidth: TextUnit = remember(timeText, statusColor != null) {
        with(density) {
            val textWidth = textMeasurer
                .measure(timeText, TextStyle(fontSize = 11.sp))
                .size.width.toDp()
            val iconWidth = if (statusColor != null) 16.dp else 0.dp
            (textWidth + iconWidth + 8.dp).toSp()
        }
    }
    val timeSpaceContent = mapOf(
        GIB_TIME_SPACE_ID to InlineTextContent(
            Placeholder(timeSpaceWidth, 1.sp, PlaceholderVerticalAlign.TextBottom)
        ) { }
    )

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
                    onClick = toggleTime,
                    onLongClick = onLongPress
                )
                Spacer(Modifier.width(4.dp))
            }

            // ---------- Single bubble ----------
            Column(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 270.dp)
                    .shadow(1.dp, RoundedCornerShape(10.dp), clip = false)
                    .background(bubbleColor, RoundedCornerShape(10.dp))
                    .padding(2.dp)
            ) {

                // Username INSIDE the bubble, above the image
                if (!isSelf && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber.removePrefix("+"),
                        color = Color(userColor),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        style = GibNoFontPadding,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 3.dp)
                    )
                }

                // ----- Media (image grid / download loader) -----
                Box {
                    when {
                        // Sent by me: grid with upload progress
                        isSelf -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            ImageBubbleContent(
                                imageUris = imageUris,
                                imageLoader = imageLoader,
                                gridColor = bubbleColor,
                                onClick = { index -> onImageClick(imageUris, index) },
                                onLongClick = { onLongPress() }
                            )

                            if (downloadPercent in 0..99) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { downloadPercent / 100f },
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        trackColor = Color.Gray.copy(alpha = 0.5f),
                                        strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                    )
                                    Text(
                                        text = "$downloadPercent%",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // Received, not downloaded yet
                        downloadPercent < 100 -> SentImageLoader(
                            imageUris = imageUris,
                            blurUri = blurFileUri,
                            downloadPercent = downloadPercent,
                            downloadSize = downloadSize,
                            onDownloadClickImage = { onDownloadClick() },
                            imageLoader = imageLoader,
                            onCancelDownload = { onCancelClick() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )

                        // Received and downloaded
                        else -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            ImageBubbleContent(
                                imageUris = imageUris,
                                imageLoader = imageLoader,
                                gridColor = bubbleColor,
                                onClick = { index -> onImageClick(imageUris, index) },
                                onLongClick = { onLongPress() }
                            )
                        }
                    }

                    // No caption -> time sits on the image, bottom-right
                    if (paath.isBlank()) {
                        val timeBackground = if (isSelf) dootawasColors.rightTimeBackGround else dootawasColors.leftTimeBackGround
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(timeBackground, RoundedCornerShape(5.dp))
                                .clickable { toggleTime() }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeText,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                color = captionTimeColor,
                                style = GibNoFontPadding
                            )
                            statusColor?.let { GibStatusIcon(it) }
                        }
                    }
                }

                // ----- Caption with time in its corner -----
                if (paath.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = toggleTime, onLongClick = onLongPress)
                            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(paath)
                                append("\u00A0")
                                appendInlineContent(GIB_TIME_SPACE_ID)
                            },
                            inlineContent = timeSpaceContent,
                            color = captionColor,
                            fontSize = 16.sp,
                            lineHeight = 19.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = GibNoFontPadding
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeText,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                color = captionTimeColor,
                                style = GibNoFontPadding
                            )
                            statusColor?.let { GibStatusIcon(it) }
                        }
                    }
                }
            }
        }
    }
}

private val GibNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private const val GIB_TIME_SPACE_ID = "timeSpace"

@Composable
private fun GibStatusIcon(tint: Color) {
    Spacer(Modifier.width(3.dp))
    Icon(
        painter = painterResource(id = R.drawable.icon_status),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(13.dp)
    )
}

// ---------- Avatar (image, or first-letter fallback) ----------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GibAvatar(
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


