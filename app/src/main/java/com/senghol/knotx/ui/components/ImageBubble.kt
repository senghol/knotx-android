package com.senghol.knotx.ui.components

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

import androidx.constraintlayout.compose.ConstraintLayout


import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.senghol.knotx.R

import com.senghol.knotx.utils.enums.SandeshStithi
import androidx.core.net.toUri
import coil.ImageLoader
import com.google.gson.JsonObject
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme

import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
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

        // 🖼️ Preview (single image)
        if (uriToDisplay != null) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        if (isError) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = Color(0xFF867F7F),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
            )
        }
        // 🔥 MULTI IMAGE BADGE (better than text)
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

        // ⬇️ DOWNLOAD / PROGRESS OVERLAY
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
                            contentDescription = null,
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
                        Text("Cancel", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBubble(
    isSelf: Boolean,
    imageUris: List<String>,
    blurFileUri: String?,
    downloadPercent: Int,
    downloadSize: String,
    timeLong: Long,
    formattedTime: String,
    replyOriginalMessage: JsonObject?,
    isSelected: Boolean = false,
    onLongPress: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    isHighlighted: Boolean,
    paath: String,
    sandeshStithi: SandeshStithi,
    imageLoader: ImageLoader,
    dootawasColors: DootawasColors,
    onReplySlide: () -> Unit
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

    // Reserve room for the time at the end of the caption (same trick as text bubbles)
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
        IB_TIME_SPACE_ID to InlineTextContent(
            Placeholder(timeSpaceWidth, 1.sp, PlaceholderVerticalAlign.TextBottom)
        ) { }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {

        // Reply preview ABOVE the bubble (separate from it)
        if (replyOriginalMessage != null) {
            ReplyBubble(
                replyOriginalMessage,
                isSelf,
                onReplyClick,
                dootawasColors.replyBackground,
                dootawasColors.replyText
            )
            Spacer(Modifier.height(2.dp))
        }

        // Only the bubble moves on swipe
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
            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
        ) {

            // ---------- Single bubble ----------
            Column(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 270.dp)
                    .shadow(1.dp, RoundedCornerShape(10.dp), clip = false)
                    .background(bubbleColor, RoundedCornerShape(10.dp))
                    .padding(2.dp)
            ) {

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
                                style = IbNoFontPadding
                            )
                            statusColor?.let { IbStatusIcon(it) }
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
                                appendInlineContent(IB_TIME_SPACE_ID)
                            },
                            inlineContent = timeSpaceContent,
                            color = captionColor,
                            fontSize = 16.sp,
                            lineHeight = 19.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = IbNoFontPadding
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
                                style = IbNoFontPadding
                            )
                            statusColor?.let { IbStatusIcon(it) }
                        }
                    }
                }
            }
        }
    }
}

private val IbNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private const val IB_TIME_SPACE_ID = "timeSpace"

@Composable
private fun IbStatusIcon(tint: Color) {
    Spacer(Modifier.width(3.dp))
    Icon(
        painter = painterResource(id = R.drawable.icon_status),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(13.dp)
    )
}

@Composable
fun ImageBubbleContent(
    imageUris: List<String>,
    imageLoader: ImageLoader,
    gridColor: Color,
    onClick: (Int) -> Unit,
    onLongClick: () -> Unit // ✅ NEW
) {

    // 🚨 FIX: handle empty list
    if (imageUris.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(gridColor),
            contentAlignment = Alignment.Center
        ) {
            Text("No Image", color = Color.DarkGray)
        }
        return
    }
    val visibleImages = imageUris.take(4)
    val totalCount = imageUris.size

    when (visibleImages.size) {

        // ✅ 1 IMAGE
        1 -> {
            SingleImage(
                uri = visibleImages[0],
                imageLoader = imageLoader,
                onClick = { onClick(0) },
                onLongClick = onLongClick // ✅
            )
        }

        // ✅ 2 IMAGES
        2 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(gridColor)
            ) {
                visibleImages.forEachIndexed { index, uri ->
                    GridImage(
                        uri = uri,
                        imageLoader = imageLoader,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),   // makes each image fill the full 200dp height
                        index = index,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            }
        }

        3 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(gridColor)
            ) {

                // 🔹 LEFT BIG IMAGE
                GridImage(
                    uri = visibleImages[0],
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    index = 0,
                    onClick = onClick,
                    onLongClick = onLongClick // ✅
                )

                // 🔹 RIGHT STACK (2 images)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    visibleImages.drop(1).forEachIndexed { index, uri ->
                        GridImage(
                            uri = uri,
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            index = index + 1,
                            onClick = onClick,
                            onLongClick = onLongClick // ✅
                        )
                    }
                }
            }
        }
        // ✅ 4+ IMAGES (2x2)
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(gridColor)
            ) {
                for (row in 0 until 2) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val uri = visibleImages[index]

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {

                                GridImage(
                                    uri = uri,
                                    imageLoader = imageLoader,
                                    modifier = Modifier.fillMaxSize(),
                                    index = index,
                                    onClick = onClick,
                                    onLongClick = onLongClick // ✅
                                )

                                // 🔥 "+N" overlay
                                if (index == 3 && totalCount > 4) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${totalCount - 4}",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleImage(
    uri: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val painter = rememberAsyncImagePainter(
        model = uri,
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    )
}

@Composable
fun GridImage(
    uri: String,
    imageLoader: ImageLoader,
    modifier: Modifier,
    index: Int,
    onClick: (Int) -> Unit,
    onLongClick: () -> Unit
) {
    val painter = rememberAsyncImagePainter(
        model = uri,
        imageLoader = imageLoader
    )

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onClick(index) },
                onLongClick = onLongClick
            )
    )
}



