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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString

import coil.compose.rememberAsyncImagePainter
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.R
import com.senghol.knotx.ui.theme.DootawasColors

import com.senghol.knotx.utils.data.ParsedDocumentSandesh
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType


import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentBubble(
    sandeshId: String,
    isSelf: Boolean,
    sandeshType: SandeshType,
    timeLong: Long,
    formattedTime: String,
    formattedDate: String,
    savedFileUri: String?,
    duplicateUri: String?,
    mimeType: String,
    downloadPercent: Int,
    downloadSize: String,
    sandeshStithi: SandeshStithi,
    replyOriginalMessage: JsonObject?,
    isSelectionMode: Boolean = false,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onToggleSelection: (uri: String?, mimeType: String) -> Unit,
    onDownloadClick: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    isHighlighted: Boolean,
    onCancelDownload: () -> Unit,
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
    val timeTextColor = if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText
    val timeBackground = if (isSelf) dootawasColors.rightTimeBackGround else dootawasColors.leftTimeBackGround

    // ---------- State ----------
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val timeText = if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    val toggleTime = { if (!isSelectionMode) showRelativeTime = !showRelativeTime }

    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    val hasPdfPreview = mimeType == "application/pdf" &&
            !duplicateUri.isNullOrEmpty() && duplicateUri != "null"

    // PDF preview if we have one, otherwise the generic document icon
    @Composable
    fun DocumentVisual() {
        if (hasPdfPreview) {
            Image(
                painter = rememberAsyncImagePainter(duplicateUri),
                contentDescription = "Document Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(3.dp)
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Document icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }

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
            Box(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 270.dp)
                    .height(200.dp)
                    .shadow(1.dp, RoundedCornerShape(10.dp), clip = false)
                    .background(bubbleColor, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { onToggleSelection(savedFileUri, mimeType) },
                        onLongClick = onLongPress
                    )
            ) {

                when {
                    // Sent by me: preview + upload progress
                    isSelf -> {
                        DocumentVisual()

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

                    // Received and fully downloaded
                    downloadPercent == 100 -> DocumentVisual()

                    // Received, not downloaded yet: icon + download / progress controls
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Document icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            if (downloadPercent == 0) {
                                IconButton(onClick = onDownloadClick) {
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
                            } else if (downloadPercent in 1..99) {
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

                // ----- Time pill, bottom-right -----
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
                        color = timeTextColor,
                        style = DbNoFontPadding
                    )
                    statusColor?.let { DbStatusIcon(it) }
                }
            }
        }
    }
}

private val DbNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
private fun DbStatusIcon(tint: Color) {
    Spacer(Modifier.width(3.dp))
    Icon(
        painter = painterResource(id = R.drawable.icon_status),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(13.dp)
    )
}