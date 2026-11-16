package com.senghol.knotx.ui.components.group

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.R
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.ui.components.messageBorder
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.MSG_LINK
import com.senghol.knotx.utils.data.MSG_SINGLE_EMOJI
import com.senghol.knotx.utils.data.MSG_TEXT
import com.senghol.knotx.utils.enums.SandeshStithi
import java.util.regex.Pattern
import kotlin.system.measureNanoTime
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import com.senghol.knotx.ui.theme.GroupDootawasColors
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessageBubble(
    userNumber: String,
    userColor: Int,
    userImageUri: Uri?,
    isSelf: Boolean,
    timeLong: Long,
    data: String,
    formattedTime: String,
    msgType: Int,
    linkUri: String? = null,
    linkTitle: String? = null,
    replyOriginalMessage: JsonObject? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isFirstInGroup: Boolean = true,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onReplyClick: (String) -> Unit,
    isHighlighted: Boolean,
    sandeshStithi: SandeshStithi,
    fontSize: Int,
    borderMode: BorderMode,
    onReplySlide: () -> Unit,
    dootawasColors: GroupDootawasColors,
    maxWidthFraction: Float = 0.72f,
) {
    val isEmoji = msgType == MSG_SINGLE_EMOJI

    // ---------- Colors ----------
    val bubbleColor = when {
        isHighlighted -> Color(0xFFD50000)
        isSelected -> Color(0xFFB3E5FC)
        isSelf -> dootawasColors.rightMessageBackground
        else -> dootawasColors.leftMessageBackground
    }
    val borderColor = when {
        isSelected -> Color.Blue
        isSelf ->  dootawasColors.rightMessageBorderColor
        else -> dootawasColors.leftMessageBorderColor
    }
    val textColor = if (isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
    // Muted time, like WhatsApp
    val timeColor = (if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText).copy(alpha = 0.65f)


    // ---------- State ----------
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val onBubbleClick = {
        if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
    }

    // ---------- Time + status ----------
    val timeText = if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    // Measure the time label so text leaves exactly that much room at the end (+ a wider gap)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val maxBubbleWidth: Dp = remember(windowSize, maxWidthFraction, density) {
        if (windowSize.width <= 0) {
            270.dp // fallback for previews / first frame
        } else {
            val isLandscape = windowSize.width > windowSize.height
            val fraction = if (isLandscape) maxWidthFraction * 0.8f else maxWidthFraction
            with(density) { (windowSize.width * fraction).toDp() }
        }
    }
    val timeSpaceWidth: TextUnit = remember(timeText, statusColor != null, density) {
        with(density) {
            val textWidth = textMeasurer
                .measure(timeText, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal))
                .size.width.toDp()
            val iconWidth = if (statusColor != null) 16.dp else 0.dp
            (textWidth + iconWidth + 12.dp).toSp()
        }
    }
    val timeSpaceContent = mapOf(
        TIME_SPACE_ID to InlineTextContent(
            Placeholder(timeSpaceWidth, 1.sp, PlaceholderVerticalAlign.TextBottom)
        ) { }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .padding(top = if (isFirstInGroup) 5.dp else 1.dp, bottom = 1.dp),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {

        // Reply preview ABOVE the message (separate from the bubble)
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
            verticalAlignment = Alignment.Top          // avatar level with the top of the bubble
        ) {

            // Avatar OUTSIDE the bubble, top-left. Only on the first message of a run;
            // later messages get an empty slot so all bubbles stay aligned.
            if (!isSelf) {
                if (isFirstInGroup) {
                    GroupAvatar(
                        imageUri = userImageUri,
                        number = userNumber,
                        color = Color(userColor),
                        onClick = onBubbleClick,
                        onLongClick = onLongPress
                    )
                } else {
                    Spacer(Modifier.width(26.dp))
                }
                Spacer(Modifier.width(4.dp))
            }

            // ---------- Single bubble ----------
            Column(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = maxBubbleWidth)
                    .then(
                        if (isEmoji) Modifier
                        else Modifier
                            .shadow(1.dp, RoundedCornerShape(8.dp), clip = false)   // add this
                            .background(bubbleColor, RoundedCornerShape(8.dp))
                            .messageBorder(borderMode, borderColor)
                    )
                    .combinedClickable(onClick = onBubbleClick, onLongClick = onLongPress)
                    .padding(start = 9.dp, end = 9.dp, top = 5.dp, bottom = 6.dp)
            ) {

                // Username INSIDE the bubble (first message of a run only)
                if (!isSelf && isFirstInGroup && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber.removePrefix("+"),
                        color = Color(userColor),
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = NoFontPadding,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )
                }

                // Content + time (time sits next to the last line, or drops below if full)
                Box {
                    when (msgType) {
                        MSG_SINGLE_EMOJI -> Text(
                            text = buildAnnotatedString {
                                append(data)
                                appendTimeSpace()
                            },
                            inlineContent = timeSpaceContent,
                            fontSize = (fontSize * 2.5).sp,
                            lineHeight = (fontSize * 3).sp,
                            style = NoFontPadding
                        )

                        MSG_LINK -> LinkContent(
                            data = data,
                            imagePath = linkUri,
                            title = linkTitle,
                            fontSize = fontSize,
                            textColor = textColor,
                            inlineContent = timeSpaceContent,
                            onClick = onBubbleClick,
                            onLongPress = onLongPress
                        )

                        else -> Text(
                            text = buildAnnotatedString {
                                append(data)
                                appendTimeSpace()
                            },
                            inlineContent = timeSpaceContent,
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight(450),
                            color = textColor,
                            lineHeight = fontSize.sp * 1.2f,
                            style = NoFontPadding
                        )
                    }

                    // Time label: bottom-right, nudged slightly below the text baseline
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 12.sp,
                            color = timeColor,
                            style = NoFontPadding
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
private val NoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private const val TIME_SPACE_ID = "timeSpace"

// Invisible placeholder at the end of the text that reserves room for the time
private fun AnnotatedString.Builder.appendTimeSpace() {
    append("\u00A0")
    appendInlineContent(TIME_SPACE_ID)
}

// ---------- Avatar (image, or first-letter fallback) ----------
@Composable
private fun UserAvatar(
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

// ---------- Link message: preview card + clickable text ----------
@Composable
private fun LinkContent(
    data: String,
    imagePath: String?,
    title: String?,
    fontSize: Int,
    textColor: Color,
    inlineContent: Map<String, InlineTextContent>,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedText = remember(data) {
        buildAnnotatedString {
            val pattern = Pattern.compile(
                "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)|" +
                        "(www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)|" +
                        "([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})|" +
                        "(\\+?\\d{10,13})"
            )
            val matcher = pattern.matcher(data)
            var last = 0
            while (matcher.find()) {
                append(data.substring(last, matcher.start()))
                val match = matcher.group()
                pushStringAnnotation("LINK", match)
                withStyle(SpanStyle(color = Color(0xFF1E88E5), textDecoration = TextDecoration.Underline)) {
                    append(match)
                }
                pop()
                last = matcher.end()
            }
            if (last < data.length) append(data.substring(last))
            appendTimeSpace()
        }
    }

    Column {
        imagePath?.let { path ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    title?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = annotatedText,
            inlineContent = inlineContent,
            fontSize = fontSize.sp,
            color = textColor,
            style = NoFontPadding,
            onTextLayout = { textLayout = it },
            modifier = Modifier.pointerInput(annotatedText) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { pos ->
                        val offset = textLayout?.getOffsetForPosition(pos)
                        val link = offset?.let {
                            annotatedText.getStringAnnotations("LINK", it, it).firstOrNull()
                        }
                        if (link != null) {
                            val item = link.item
                            val uri = when {
                                item.startsWith("http") -> item
                                item.startsWith("www") -> "http://$item"
                                else -> "tel:$item"
                            }
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
                        } else {
                            onClick()
                        }
                    }
                )
            }
        )
    }
}
