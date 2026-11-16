package com.senghol.knotx.ui.components.single

import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.components.messageBorder
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.data.AiBlock
import com.senghol.knotx.utils.data.ParsedAiSandesh
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange

// ============================================================================
//  AI BUBBLE (always on the left)
// ============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiMessageBubble(
    item: ParsedAiSandesh,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isHighlighted: Boolean = false,
    fontSize: Int,
    borderMode: BorderMode,
    dootawasColors: DootawasColors,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onReplySlide: () -> Unit,
    onAddCredits: (() -> Unit)? = null      // shown when status == NO_CREDITS
) {
    val bubbleColor = when {
        isHighlighted -> Color(0xFFD50000)
        isSelected -> Color(0xFFB3E5FC)
        else -> dootawasColors.leftMessageBackground
    }
    val borderColor = if (isSelected) Color.Blue else dootawasColors.leftMessageBorderColor
    val textColor = dootawasColors.leftMessageText
    val timeColor = dootawasColors.leftTimeText

    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
    val trigger = with(LocalDensity.current) { 72.dp.toPx() }

    val onBubbleClick = {
        if (isSelectionMode) onToggleSelection() else showRelativeTime = !showRelativeTime
    }
    val timeText = if (showRelativeTime) item.formattedDate else item.formattedTime

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .combinedClickable(onClick = onBubbleClick, onLongClick = onLongPress)
                    .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
            ) {
                // ---------- Blocks ----------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.blocks.forEachIndexed { index, block ->
                        val isLast = index == item.blocks.lastIndex
                        when (block) {
                            is AiBlock.Text -> MdText(
                                text = block.text + if (item.isTyping && isLast) " ▍" else "",
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = FontWeight.Normal // Normal weight for clean regular text
                            )

                            is AiBlock.Heading -> MdText(
                                text = block.text,
                                color = textColor,
                                fontSize = fontSize + when (block.level) {
                                    1 -> 6; 2 -> 4; 3 -> 2; else -> 1
                                },
                                fontWeight = FontWeight.Bold
                            )

                            is AiBlock.Code -> AiCodeBlock(block.lang, block.code, textColor)

                            is AiBlock.Table -> AiTable(block, textColor, fontSize)

                            is AiBlock.ListBlock -> AiList(block, textColor, fontSize)

                            is AiBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(textColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                MdText(block.text, textColor.copy(alpha = 0.85f), fontSize, fontWeight = FontWeight.Normal)
                            }

                            AiBlock.Divider -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(0.5.dp)
                                    .background(textColor.copy(alpha = 0.25f))
                            )
                        }
                    }
                }

                // ---------- Out of credits action ----------
                if (item.status == "NO_CREDITS" && onAddCredits != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add credits",
                        color = Color(0xFF1E88E5),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAddCredits() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                // ---------- Time ----------
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    color = timeColor,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
//  PIECES
// ============================================================================

/** Text with inline Markdown (**bold**, *italic*, `code`, ~~strike~~, [link](url)). */
@Composable
private fun MdText(
    text: String,
    color: Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val annotated = remember(text, color) {
        inlineMarkdown(text, Color(0xFF1E88E5), color.copy(alpha = 0.12f))
    }
    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize.sp,
        lineHeight = fontSize.sp * 1.4f,
        fontWeight = fontWeight
    )
}

@Composable
private fun AiCodeBlock(lang: String, code: String, textColor: Color) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(textColor.copy(alpha = 0.06f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(textColor.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lang.ifBlank { "code" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (copied) "Copied" else "Copy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E88E5),
                modifier = Modifier
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = textColor,
            softWrap = false,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        )
    }
}

@Composable
private fun AiTable(table: AiBlock.Table, textColor: Color, fontSize: Int) {
    val lineColor = textColor.copy(alpha = 0.2f)
    val colWidth = 120.dp
    val totalWidth = colWidth * table.header.size

    Column(
        Modifier
            .horizontalScroll(rememberScrollState())
            .border(0.5.dp, lineColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
    ) {
        AiTableRow(table.header, table.align, true, textColor, fontSize, colWidth)
        table.rows.forEach { row ->
            Box(Modifier.width(totalWidth).height(0.5.dp).background(lineColor))
            AiTableRow(row, table.align, false, textColor, fontSize, colWidth)
        }
    }
}

@Composable
private fun AiTableRow(
    cells: List<String>,
    align: List<String>,
    header: Boolean,
    textColor: Color,
    fontSize: Int,
    colWidth: androidx.compose.ui.unit.Dp
) {
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .then(if (header) Modifier.background(textColor.copy(alpha = 0.08f)) else Modifier)
    ) {
        cells.forEachIndexed { i, cell ->
            Box(
                modifier = Modifier
                    .width(colWidth)
                    .fillMaxHeight()
                    .padding(8.dp),
                contentAlignment = when (align.getOrNull(i)) {
                    "right" -> Alignment.CenterEnd
                    "center" -> Alignment.Center
                    else -> Alignment.CenterStart
                }
            ) {
                MdText(
                    text = cell,
                    color = textColor,
                    fontSize = fontSize - 1,
                    fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
@Composable
private fun AiList(list: AiBlock.ListBlock, textColor: Color, fontSize: Int) {
    val markers = remember(list) {
        val counters = IntArray(10)
        list.items.map { item ->
            val lvl = item.level.coerceIn(0, 9)
            for (k in lvl + 1..9) counters[k] = 0
            counters[lvl]++
            when {
                list.ordered -> "${counters[lvl]}."
                lvl == 0 -> "•"
                else -> "–"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        list.items.forEachIndexed { i, item ->
            // Fixed: Multiply the Int values first, then convert the result to Dp
            val startPadding = (item.level.coerceIn(0, 9) * 16).dp
            Row(Modifier.padding(start = startPadding)) {
                Text(
                    text = markers[i],
                    color = textColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 20.dp)
                )
                MdText(item.text, textColor, fontSize, Modifier.weight(1f), fontWeight = FontWeight.Normal)
            }
        }
    }
}

// ============================================================================
//  INLINE MARKDOWN -> AnnotatedString
// ============================================================================
private fun inlineMarkdown(src: String, linkColor: Color, codeBg: Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        val n = src.length
        while (i < n) {
            val c = src[i]
            when {
                // **bold**
                src.startsWith("**", i) -> {
                    val end = src.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(inlineMarkdown(src.substring(i + 2, end), linkColor, codeBg))
                        }
                        i = end + 2
                    } else { append("**"); i += 2 }
                }

                // ~~strike~~
                src.startsWith("~~", i) -> {
                    val end = src.indexOf("~~", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(inlineMarkdown(src.substring(i + 2, end), linkColor, codeBg))
                        }
                        i = end + 2
                    } else { append("~~"); i += 2 }
                }

                // `code`
                c == '`' -> {
                    val end = src.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                            append(src.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(c); i++ }
                }

                // *italic* or _italic_
                (c == '*' || c == '_') &&
                        i + 1 < n && !src[i + 1].isWhitespace() &&
                        (c == '*' || i == 0 || !src[i - 1].isLetterOrDigit()) -> {
                    val end = src.indexOf(c, i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(inlineMarkdown(src.substring(i + 1, end), linkColor, codeBg))
                        }
                        i = end + 1
                    } else { append(c); i++ }
                }

                // [label](url)
                c == '[' -> {
                    val close = src.indexOf("](", i + 1)
                    val paren = if (close > 0) src.indexOf(')', close + 2) else -1
                    if (close > i && paren > close) {
                        val label = src.substring(i + 1, close)
                        val url = src.substring(close + 2, paren)
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                            )
                        ) { append(label) }
                        i = paren + 1
                    } else { append(c); i++ }
                }

                else -> { append(c); i++ }
            }
        }
    }