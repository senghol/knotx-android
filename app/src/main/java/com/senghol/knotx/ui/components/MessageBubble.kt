package com.senghol.knotx.ui.components


import android.R.attr.x
import android.content.Intent

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.R
import java.util.regex.Pattern
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.data.MSG_LINK
import com.senghol.knotx.utils.data.MSG_SINGLE_EMOJI
import com.senghol.knotx.utils.data.MSG_TEXT
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    isSelf: Boolean,
    data: String,
    formattedTime: String,
    formattedDate: String,
    msgType: Int,
    linkUri: String? = null,
    linkTitle: String? = null,
    replyOriginalMessage: JsonObject? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    onReplySlide: () -> Unit,
    isHighlighted: Boolean,
    sandeshStithi: SandeshStithi,
    fontSize: Int,
    borderMode: BorderMode,
    dootawasColors: DootawasColors,
    maxWidthFraction: Float = 0.75f
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
        isSelf -> dootawasColors.rightMessageBorderColor
        else -> dootawasColors.leftMessageBorderColor
    }
    val textColor = if (isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
    val timeColor = if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText

    // ---------- State ----------
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val onBubbleClick = {
        if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
    }

    // ---------- Time + status ----------
    val timeText = if (showRelativeTime) formattedDate else formattedTime
    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    // Measure the time label so the text leaves exactly that much room at its end
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val maxBubbleWidth: Dp = remember(windowSize, maxWidthFraction, density) {
        if (windowSize.width <= 0) {
            280.dp // fallback for previews / first frame
        } else {
            val isLandscape = windowSize.width > windowSize.height
            val fraction = if (isLandscape) maxWidthFraction * 0.8f else maxWidthFraction
            with(density) { (windowSize.width * fraction).toDp() }
        }
    }
    val timeSpaceWidth: TextUnit = remember(timeText, statusColor != null, density) {
        with(density) {
            val textWidth = textMeasurer
                .measure(timeText, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Light))
                .size.width.toDp()
            val iconWidth = if (statusColor != null) 16.dp else 0.dp
            (textWidth + iconWidth + 8.dp).toSp()
        }
    }
    val timeSpaceContent = mapOf(
        MB_TIME_SPACE_ID to InlineTextContent(
            Placeholder(timeSpaceWidth, 1.sp, PlaceholderVerticalAlign.TextBottom)
        ) { }
    )

    // ---------- Row with swipe-to-reply ----------
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {

        // Reply preview ABOVE the message (separate from the bubble)
        if (replyOriginalMessage != null) {
//            Box(modifier = Modifier.padding(start = 0.dp)) {
                ReplyBubble(
                    replyOriginalMessage,
                    isSelf,
                    onReplyClick,
                    dootawasColors.replyBackground,
                    dootawasColors.replyText
                )
//            }
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
            verticalAlignment = Alignment.Bottom
        ) {

            // ---------- Single bubble ----------
            Column(
                modifier = Modifier
                    .widthIn(
                        min = 90.dp,
                        max = maxBubbleWidth
                    )
                    .then(
                        if (isEmoji) Modifier
                        else Modifier

                            .shadow(1.dp, RoundedCornerShape(8.dp), clip = false)   // add this
                            .background(bubbleColor, RoundedCornerShape(8.dp))
                            .messageBorder(borderMode, borderColor)
                    )
                    .combinedClickable(onClick = onBubbleClick, onLongClick = onLongPress)
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 5.dp)
            ) {


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
                            style = MbNoFontPadding
                        )

                        MSG_LINK -> MessageLinkContent(
                            data = data,
                            imagePath = linkUri,
                            title = linkTitle,
                            fontSize = fontSize,
                            textColor = textColor,
                            inlineContent = timeSpaceContent,
                            isSelectionMode = isSelectionMode,
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
                            lineHeight = fontSize.sp * 1.15f,
                            style = MbNoFontPadding
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
                            style = MbNoFontPadding
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

private val MbNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

private const val MB_TIME_SPACE_ID = "timeSpace"

// Invisible placeholder at the end of the text that reserves room for the time
private fun AnnotatedString.Builder.appendTimeSpace() {
    append("\u00A0")
    appendInlineContent(MB_TIME_SPACE_ID)
}

// ---------- Link message: preview card + clickable text ----------
@Composable
private fun MessageLinkContent(
    data: String,
    imagePath: String?,
    title: String?,
    fontSize: Int,
    textColor: Color,
    inlineContent: Map<String, InlineTextContent>,
    isSelectionMode: Boolean,
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
            style = MbNoFontPadding,
            onTextLayout = { textLayout = it },
            modifier = Modifier.pointerInput(annotatedText, isSelectionMode) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { pos ->
                        // In selection mode every tap just toggles selection
                        if (isSelectionMode) {
                            onClick()
                            return@detectTapGestures
                        }
                        val offset = textLayout?.getOffsetForPosition(pos)
                        val link = offset?.let {
                            annotatedText.getStringAnnotations("LINK", it, it).firstOrNull()
                        }
                        if (link != null) {
                            val item = link.item.trim()
                            val uri = when {
                                item.matches(Regex("\\+?\\d{10,13}")) -> "tel:$item"
                                item.startsWith("http", true) -> item
                                else -> "http://$item"
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

enum class BubbleType {
    SINGLE,
    START,
    MID,
    END
}

// HOISTED REGEX PATTERN (Compiled once globally, completely skipping UI loop re-allocations)
private val CHAT_URL_PATTERN = java.util.regex.Pattern.compile(
    "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)|" +
            "(www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)|" +
            "([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})|" +
            "(\\+?\\d{10,13})"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernMessageBubble(
    isSelf: Boolean,
    data: String,
    formattedTime: String,
    formattedDate: String,
    timeLong: Long,
    msgType: Int,
    bubbleType: BubbleType = BubbleType.SINGLE,
    linkUri: String? = null,
    linkTitle: String? = null,
    replyOriginalMessage: JsonObject? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    onReplySlide: () -> Unit,
    isHighlighted: Boolean,
    sandeshStithi: SandeshStithi,
    fontSize: Int,
    borderMode: BorderMode,
    dootawasColors: DootawasColors,
    maxWidthFraction: Float = 0.75f,
    scaleMultiplier: Float = 2.5f,
    outerEdgeOffset: Dp = 3.dp,
    interFontFamily: FontFamily = FontFamily.Default
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    var showRelativeTime by remember { mutableStateOf(false) }

    var linkImageLoaded by remember(linkUri) { mutableStateOf(false) }
    // 1. PRODUCTION PALETTE SYSTEM (Cached with Theme Update Support)
    val messageBackgroundColor = remember(isHighlighted, isSelected, isSelf, dootawasColors) {
        when {
            isHighlighted -> Color(0xFFD50000)
            isSelected -> Color(0xFFB3E5FC)
            isSelf -> dootawasColors.rightMessageBackground
            else -> dootawasColors.leftMessageBackground
        }
    }

    val borderColor = remember(isSelected, isSelf, dootawasColors) {
        when {
            isSelected -> Color.Blue
            isSelf -> dootawasColors.rightMessageBorderColor
            else -> dootawasColors.leftMessageBorderColor
        }
    }

    val textColor = remember(isSelf, dootawasColors) {
        if (isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
    }
    val timeTextColor = remember(isSelf, dootawasColors) {
        if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText
    }

    val textStyle = remember(interFontFamily, fontSize, textColor) {
        TextStyle(
            fontFamily = interFontFamily,
            fontSize = fontSize.sp,
            fontWeight = FontWeight(450),
            color = textColor,
            lineHeight = fontSize.sp * 1.1f,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    }

    val timeStyle = remember(interFontFamily, timeTextColor) {
        TextStyle(
            fontFamily = interFontFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = timeTextColor,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    }

    // 2. TIMESTAMPS & RICH INLINE RES SYSTEM
    val displayedTimeString = remember(showRelativeTime, timeLong, formattedTime) {
        if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    }

    val timestampText = remember(showRelativeTime, displayedTimeString, sandeshStithi, isSelf) {
        buildAnnotatedString {
            append(displayedTimeString)
            if (sandeshStithi != SandeshStithi.LIKHIT && isSelf) {
                append(" ")
                appendInlineContent("icon")
            }
        }
    }

    val iconRes = remember(sandeshStithi) {
        when (sandeshStithi) {
            SandeshStithi.PREKSHIT -> R.drawable.prekshit // Single tick / sent
            SandeshStithi.SAMARPIT -> R.drawable.samarpit // Double tick / delivered
            SandeshStithi.DHRIST   -> R.drawable.dhrist   // Blue tick / read
            else -> null
        }
    }

    val iconColor = remember(sandeshStithi) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00EF63)
            else -> Color.Unspecified
        }
    }

    val inlineContent = remember(iconRes, iconColor, isSelf) {
        if (!isSelf || iconRes == null) emptyMap()
        else mapOf(
            "icon" to InlineTextContent(
                placeholder = Placeholder(
                    width = 12.sp,
                    height = 12.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = iconColor
                )
            }
        )
    }

    // 3. MEASUREMENT METRICS
    val bubblePadV = 4.dp // ONE value: bubble top edge -> text, and text/time -> bubble bottom edge
    val paddingStartDp = (5f * scaleMultiplier).dp
    val paddingEndDp = (8f * scaleMultiplier).dp

    val actualPaddingStart = if (isSelf) paddingStartDp else paddingEndDp
    val actualPaddingEnd = if (isSelf) paddingEndDp else paddingStartDp

    val timePaddingEnd = if (isSelf) {
        if (bubbleType == BubbleType.START || bubbleType == BubbleType.MID) {
            (4.5f * scaleMultiplier).dp
        } else {
            (7.5f * scaleMultiplier).dp
        }
    } else {
        (5.5f * scaleMultiplier).dp
    }
    val windowSize = LocalWindowInfo.current.containerSize
    val isLandscape = windowSize.width > windowSize.height
    val maxBubbleWidthPx = remember(windowSize, maxWidthFraction, isLandscape, density) {
        if (windowSize.width <= 0) {
            with(density) { 280.dp.toPx() } // fallback for previews / first frame
        } else {
            val fraction = if (isLandscape) maxWidthFraction * 0.8f else maxWidthFraction
            windowSize.width * fraction
        }
    }
    // 4. SMART MEASUREMENT ENGINE
    val (lineLengthWidth, lineLengthHeight) = remember(data, timestampText, msgType, maxBubbleWidthPx, density, scaleMultiplier, textStyle, timeStyle, linkImageLoaded, bubbleType, fontSize, isSelf, sandeshStithi, bubblePadV) {
        val horizontalPaddingPx = with(density) { (paddingStartDp + paddingEndDp).toPx() }
        val maxTextWidthPx = maxBubbleWidthPx - horizontalPaddingPx

        val measuredStyle = if (msgType == MSG_SINGLE_EMOJI) textStyle.copy(fontSize = (fontSize * 2.5).sp, lineHeight = (fontSize * 3).sp) else textStyle

        val textLayoutResult = textMeasurer.measure(
            text = data,
            style = measuredStyle,
            constraints = Constraints(maxWidth = maxTextWidthPx.coerceAtLeast(1f).toInt())
        )

        val textWidthDp = with(density) { textLayoutResult.size.width.toDp().value }
        val textHeightDp = with(density) { textLayoutResult.size.height.toDp().value }

        val timeLayoutResult = textMeasurer.measure(text = timestampText, style = timeStyle)
        val timeWidthDp = with(density) { timeLayoutResult.size.width.toDp().value }
        val timeHeightDp = with(density) { timeLayoutResult.size.height.toDp().value }

        val gapDp = 12f
        val timeGapDp = 2f // space between the text and a wrapped time row
        val lineCount = textLayoutResult.lineCount

        val lastLineIndex = (lineCount - 1).coerceAtLeast(0)
        val lastLineWidthPx = textLayoutResult.getLineRight(lastLineIndex) - textLayoutResult.getLineLeft(lastLineIndex)
        val lastLineWidthDp = with(density) { lastLineWidthPx.toDp().value }

        val finalWidthDp: Float
        val finalHeightDp: Float
        val maxAllowedTextWidthDp = with(density) { maxTextWidthPx.toDp().value }

        when (msgType) {
            MSG_SINGLE_EMOJI -> {
                finalWidthDp = maxOf(textWidthDp, timeWidthDp + (if (isSelf && sandeshStithi != SandeshStithi.LIKHIT) 16f else 0f))
                finalHeightDp = textHeightDp + timeHeightDp + 2f
            }
            MSG_LINK -> {
                finalWidthDp = maxOf(textWidthDp, timeWidthDp + gapDp)
                finalHeightDp = textHeightDp + timeHeightDp + if (linkImageLoaded) (98f * scaleMultiplier) else 0f
            }
            else -> {
                if (lineCount == 1) {
                    if (textWidthDp + gapDp + timeWidthDp <= maxAllowedTextWidthDp) {
                        finalWidthDp = textWidthDp + gapDp + timeWidthDp
                        finalHeightDp = maxOf(textHeightDp, timeHeightDp)
                    } else {
                        finalWidthDp = maxOf(textWidthDp, timeWidthDp + gapDp)
                        finalHeightDp = textHeightDp + timeHeightDp + timeGapDp
                    }
                } else {
                    val remainingSpaceOnLastLine = maxAllowedTextWidthDp - lastLineWidthDp
                    if (remainingSpaceOnLastLine >= (timeWidthDp + gapDp)) {
                        // FIX HERE: If expanding the time forces the total width wider than the text, handle it safely
                        finalWidthDp = maxOf(textWidthDp, lastLineWidthDp + gapDp + timeWidthDp)
                        finalHeightDp = textHeightDp
                    } else {
                        finalWidthDp = maxOf(textWidthDp, timeWidthDp + gapDp)
                        finalHeightDp = textHeightDp + timeHeightDp + timeGapDp
                    }
                }
            }
        }

        // Fixed part of each path shape (from the path strings): bounds = variable part + this
        val pathExtraWidth = when (bubbleType) {
            BubbleType.START -> 10f
            BubbleType.MID -> 13f
            else -> 12.3f // SINGLE, END
        }
        val pathExtraHeight = 12f

        // +1dp safety so the text never wraps because of rounding
        val boxWidthDp = finalWidthDp + paddingStartDp.value + paddingEndDp.value + 1f
        val boxHeightDp = finalHeightDp + 2 * bubblePadV.value

        val w = (boxWidthDp / scaleMultiplier - pathExtraWidth).coerceAtLeast(10f)
        val h = (boxHeightDp / scaleMultiplier - pathExtraHeight).coerceAtLeast(0f)

        // 2 decimals so the path string never gets scientific notation like 1.0E-4
        ((w * 100).roundToInt() / 100f) to ((h * 100).roundToInt() / 100f)
    }

    val dynamicPathString = remember(lineLengthWidth, lineLengthHeight, bubbleType) {
        when (bubbleType) {
            BubbleType.START -> "m48 7c2 0 2 0 2-2v-5-$lineLengthHeight 0c0-3-2-5-5-5h-$lineLengthWidth c-3 0-5 2-5 5v$lineLengthHeight 2c0 3 2 5 5 5h$lineLengthWidth\u006c3 0"
            BubbleType.MID -> "m48 7c2 0 2 0 2-2v-5-$lineLengthHeight-3c0-2 0-2-2-2l-6 0h-$lineLengthWidth c-3 0-5 2-5 5v$lineLengthHeight 2c0 3 2 5 5 5h$lineLengthWidth\u006c6 0"
            BubbleType.END -> "m46 7h-$lineLengthWidth c-3 0-5-2-5-5v-2-$lineLengthHeight c0-3 2-5 5-5h$lineLengthWidth\u006c3 0c2 0 2 0 2 2l0 3v$lineLengthHeight c0 3 1 5 2.3 6-.3 0-3.3 0-4.3-2 0 2-1 3-3 3"

            // FIX: Added space between $lineLengthWidth and c3
            BubbleType.SINGLE -> "m46 7h-$lineLengthWidth c-3 0-5-2-5-5v-2-$lineLengthHeight c0-3 2-5 5-5h$lineLengthWidth c3 0 5 2 5 5v$lineLengthHeight c0 3 1 5 2.3 6C53 6 50 6 49 4c0 2-1 3-3 3"
        }
    }

    val (basePath, bounds) = remember(dynamicPathString) {
        val path = PathParser().parsePathString(dynamicPathString).toPath()
        path to path.getBounds()
    }

    val computedBoxWidth = (bounds.width * scaleMultiplier).dp
    val computedBoxHeight = (bounds.height * scaleMultiplier).dp

    // 5. DYNAMIC SPACING & ALIGNMENT CALCULATIONS
    val topPadding = remember(bubbleType) {
        if (bubbleType == BubbleType.START || bubbleType == BubbleType.SINGLE) 10.dp else 1.5.dp
    }

    val tailAdjustment = remember(bubbleType, msgType) {
        if (msgType == MSG_SINGLE_EMOJI) {
            0.dp
        } else if (bubbleType == BubbleType.START || bubbleType == BubbleType.MID) {
            6.dp
        } else {
            0.dp
        }
    }

    val finalXOffset = remember(isSelf, outerEdgeOffset, tailAdjustment) {
        if (isSelf) -outerEdgeOffset - tailAdjustment else outerEdgeOffset + tailAdjustment
    }

    // 6. LINK ANNOTATION PARSING ENGINE
    val annotatedText = remember(data, textColor, msgType) {
        if (msgType != MSG_LINK) buildAnnotatedString { append(data) }
        else buildAnnotatedString {
            val matcher = CHAT_URL_PATTERN.matcher(data)
            var lastIndex = 0

            while (matcher.find()) {
                val matchStart = matcher.start()
                val matchEnd = matcher.end()
                val match = data.substring(matchStart, matchEnd)

                append(data.substring(lastIndex, matchStart))

                pushStringAnnotation(tag = "LINK", annotation = match)
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF1E88E5),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match)
                }
                pop()

                lastIndex = matchEnd
            }

            if (lastIndex < data.length) {
                append(data.substring(lastIndex))
            }
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }

val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 1.5.dp),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {
        if (replyOriginalMessage != null) {
            Box(modifier = Modifier.padding(start = 10.dp , end = 10.dp, bottom = 2.dp)) {
                ReplyBubble(
                    replyOriginalMessage,
                    isSelf,
                    onReplyClick,
                    dootawasColors.replyBackground,
                    dootawasColors.replyText
                )
            }
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        finalXOffset.roundToPx() + offsetX.roundToInt(),
                        0
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount > 0 || offsetX > 0f) {
                                offsetX = (offsetX + dragAmount).coerceIn(0f, maxSwipe)
                            }
                        },
                        onDragEnd = {
                            if (offsetX > trigger) {
                                onReplySlide()
                            }
                            offsetX = 0f
                        }
                    )
                }
                .width(computedBoxWidth)
                .height(computedBoxHeight),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val finalPath = Path()
                        if (msgType != MSG_SINGLE_EMOJI && size.width > 0 && size.height > 0) {
                            val scaleX = size.width / bounds.width
                            val scaleY = size.height / bounds.height

                            finalPath.apply {
                                addPath(basePath)
                                transform(
                                    matrix = androidx.compose.ui.graphics.Matrix().apply {
                                        scale(scaleX, scaleY)
                                        translate(-bounds.left, -bounds.top)
                                    }
                                )
                            }
                        }

                        val fillPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.FILL
                            color = messageBackgroundColor.toArgb()
                        }

                        val strokePaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.5.dp.toPx()
                            color = borderColor.toArgb()
                        }

                        val shadowLayers = listOf(
                            2.5.dp.toPx() to 5,
                            1.8.dp.toPx() to 7,
                            1.dp.toPx() to 9
                        ).map { (width, alpha) ->
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL_AND_STROKE
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                strokeWidth = width
                                color = android.graphics.Color.argb(alpha, 0, 0, 0)
                            }
                        }
                        val shadowOffsetPx = 0.5.dp.toPx()
                        onDrawBehind {
                            if (msgType != MSG_SINGLE_EMOJI) {
                                drawIntoCanvas { canvas -> canvas.nativeCanvas.save()
                                    if (!isSelf) {
                                        canvas.nativeCanvas.scale(-1f, 1f, size.width / 2f, size.height / 2f)
                                    }

                                    canvas.nativeCanvas.save()
                                    canvas.nativeCanvas.translate(shadowOffsetPx, shadowOffsetPx)
                                    shadowLayers.forEach { canvas.nativeCanvas.drawPath(finalPath.asAndroidPath(), it) }
                                    canvas.nativeCanvas.restore()

                                    canvas.nativeCanvas.drawPath(finalPath.asAndroidPath(), fillPaint)
                                    canvas.nativeCanvas.drawPath(finalPath.asAndroidPath(), strokePaint)
                                    canvas.nativeCanvas.restore()
                                }
                            }
                        }
                    }
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
                        },
                        onLongClick = onLongPress
                    )
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (msgType == MSG_SINGLE_EMOJI) {
                    Column(
                        modifier = Modifier
                            .align(if (isSelf) Alignment.CenterEnd else Alignment.CenterStart)
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = data,
                            fontSize = (fontSize * 2.5).sp,
                            lineHeight = (fontSize * 2.5).sp,
                            style = LocalTextStyle.current.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = timestampText,
                            style = timeStyle,
                            inlineContent = inlineContent,
                            modifier = Modifier.clickable {
                                if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = actualPaddingStart,
                                end = actualPaddingEnd,
                                top = bubblePadV,
                                bottom = if (msgType == MSG_LINK) 14.dp else 0.dp
                            )
                    ) {
                        when (msgType) {
                            MSG_TEXT -> {
                                Text(
                                    text = data,
                                    style = textStyle
                                )
                            }
                            MSG_LINK -> {
                                Column {
                                    linkUri?.let { imagePath ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = (6 / scaleMultiplier).dp)
                                                .clickable {
                                                    val matcher = CHAT_URL_PATTERN.matcher(data)
                                                    if (matcher.find()) {
                                                        val link = matcher.group().trim()
                                                        val uri = when {
                                                            link.matches(Regex("\\+?\\d{10,13}")) -> "tel:$link"
                                                            link.startsWith("http", true) -> link
                                                            else -> "http://$link"
                                                        }
                                                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                                            uri.toUri()))
                                                    }
                                                },
                                            elevation = CardDefaults.cardElevation(2.dp)
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = imagePath,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp),
                                                    onSuccess = { linkImageLoaded = true },
                                                    onError = { linkImageLoaded = false }
                                                )
                                                linkTitle?.let { title ->
                                                    Text(
                                                        text = title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ClickableText(
                                        text = annotatedText,
                                        style = textStyle,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) { offset ->
                                        if (isSelectionMode) {
                                            onToggleSelection()
                                            return@ClickableText
                                        }

                                        annotatedText.getStringAnnotations(
                                            tag = "LINK",
                                            start = offset,
                                            end = offset
                                        ).firstOrNull()?.let { annotation ->
                                            val link = annotation.item.trim()
                                            val uri = when {
                                                link.matches(Regex("\\+?\\d{10,13}")) -> "tel:$link"
                                                link.startsWith("http", true) -> link
                                                else -> "http://$link"
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = timestampText,
                        style = timeStyle,
                        inlineContent = inlineContent,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = timePaddingEnd,
                                bottom = bubblePadV
                            )
                            .clickable {
                                if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
                            }
                    )
                }
            }
        }
    }
}



enum class BorderMode {
    NONE,
    SEMI,
    FULL
}

fun Modifier.messageBorder(
    mode: BorderMode,
    color: Color
): Modifier = when (mode) {

    BorderMode.NONE -> this

    BorderMode.FULL -> this.border(
        width = 1.dp,
        color = color,
        shape = RoundedCornerShape(8.dp)
    )

    BorderMode.SEMI -> this.drawBehind {
        val strokeWidth = 1.5.dp.toPx()
        val radius = 8.dp.toPx()
        val halfStroke = strokeWidth / 2
        val w = size.width
        val h = size.height

        // Left edge
        drawLine(
            color,
            Offset(halfStroke, radius),
            Offset(halfStroke, h - radius),
            strokeWidth
        )

        // Right edge
        drawLine(
            color,
            Offset(w - halfStroke, radius),
            Offset(w - halfStroke, h - radius),
            strokeWidth
        )

        // Bottom edge
        drawLine(
            color,
            Offset(radius, h - halfStroke),
            Offset(w - radius, h - halfStroke),
            strokeWidth
        )

        // Bottom-right rounded corner
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w - 2 * radius, h - 2 * radius),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(strokeWidth)
        )

        // Bottom-left rounded corner
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(0f, h - 2 * radius),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(strokeWidth)
        )
    }
}









