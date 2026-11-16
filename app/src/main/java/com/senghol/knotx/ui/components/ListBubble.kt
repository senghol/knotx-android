package com.senghol.knotx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.data.ParsedListSandesh
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import com.senghol.knotx.R


@Composable
fun ListBubble(
    data: ParsedListSandesh,
    replyOriginalMessage: JsonObject?,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    dootawasColors: DootawasColors,
    onLongPress: () -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
    onReplySlide: () -> Unit,
    onAcceptClick: (sandeshId: String) -> Unit,
    onRejectClick: (sandeshId: String) -> Unit,
    onBubbleClick: () -> Unit
) {
    var showRelativeTime by remember { mutableStateOf(false) }
    val timeBackgroundColor = remember(data.isSelf) { if (data.isSelf) dootawasColors.rightTimeBackGround else dootawasColors.leftTimeBackGround }
    val bubbleBgColor = if (data.isSelf) dootawasColors.rightMessageBackground else dootawasColors.leftMessageBackground
    val timeTextColor = remember(data.isSelf) { if (data.isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText}

    val borderColor = remember(isHighlighted, isSelected) {
        when {
            isHighlighted -> Color(0xFFD50000)
            isSelected -> Color.Blue
            else -> Color.Transparent
        }
    }

    val displayedTime = remember(showRelativeTime, data.time, data.formattedTime) {
        if (showRelativeTime) getRelativeTimeSpanMinuteLevel(data.time) else data.formattedTime
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp), // Thinner vertical margin
        horizontalAlignment = if (data.isSelf) Alignment.End else Alignment.Start
    ) {
        if (replyOriginalMessage != null) {
            ReplyBubble(replyOriginalMessage, data.isSelf, onReplyClick,dootawasColors.replyBackground,dootawasColors.replyText)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
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
                // Compact Sizing Boundaries (-20% width profile)
                .widthIn(min = 220.dp, max = 250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleBgColor)
                .border(if (borderColor != Color.Transparent) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(12.dp))
                // Clean dual-gesture handler for click + long press anywhere on the bubble background
                .combinedClickable(
                    onClick = { onBubbleClick() },
                    onLongClick = { onLongPress() }
                )
                .padding(8.dp) // Tight inner padding
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // --- Top Header Details Area ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Premium Tinted Document Icon Container
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(dootawasColors.chatScreenBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = "List Document",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF7C4DFF)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = data.title.ifEmpty { "Untitled List" },
                            fontSize = 13.sp, // Slightly more compact typography profile
                            fontWeight = FontWeight.Bold,
                            color = if (data.isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText,
                            maxLines = 1
                        )
                        Text(
                            text = "${data.completed}/${data.total} completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (data.isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- Animated Document Progress Engine ---
                val animatedProgress by animateFloatAsState(
                    targetValue = data.progress,
                    animationSpec = tween(durationMillis = 500),
                    label = "docProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp) // Thinner progress lane
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedProgress)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Checklist Subview Pod (Dashboard View) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(dootawasColors.chatScreenBackground)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp) // Tight vertical stack
                ) {
                    data.previewItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp) // Scaled down circular radio check ring
                                    .border(
                                        width = 1.dp,
                                        color = if (item.checked) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.checked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFF00C853)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = item.text.ifEmpty { "Item" },
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (data.remainingItems > 0) {
                        Text(
                            text = "+${data.remainingItems} more items",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 1.dp)
                        )
                    }
                }

                // --- Simple Clean Action Pipeline Injection ---
                if (data.isUpdate) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("By ")
                            append(data.displayList.updatedBy ?: "User")
                            append(" • v${data.listMessage.baseVersion}→v${data.displayList.version}")
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRejectClick(data.sandeshId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp), // Slim buttons
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onAcceptClick(data.sandeshId) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- Bottom Info / Status Matrix ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append(displayedTime)
                            if (data.sandeshStithi != SandeshStithi.LIKHIT && data.isSelf) {
                                append(" ")
                                appendInlineContent("icon")
                            }
                        },
                        fontSize = 10.sp,
                        color = timeTextColor,
                        modifier = Modifier
                            .align(if (data.isSelf) Alignment.BottomEnd else Alignment.BottomStart)
                            .padding(
                                start = if (!data.isSelf) 4.dp else 0.dp,
                                end = if (data.isSelf) 4.dp else 0.dp
                            )
                            .background(timeBackgroundColor, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                            .clickable { showRelativeTime = !showRelativeTime },
                        inlineContent = if (data.isSelf) {
                            mapOf("icon" to InlineTextContent(
                                placeholder = Placeholder(
                                    width = 12.sp,
                                    height = 12.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                val iconRes = when (data.sandeshStithi) {
                                    SandeshStithi.PREKSHIT, SandeshStithi.SAMARPIT, SandeshStithi.DHRIST -> R.drawable.icon_status
                                    else -> null
                                }
                                val iconColor = when (data.sandeshStithi) {
                                    SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
                                    SandeshStithi.PREKSHIT -> Color.Gray
                                    SandeshStithi.DHRIST -> Color(0xFF00C853)
                                    else -> Color.Unspecified
                                }
                                iconRes?.let {
                                    Icon(
                                        painter = painterResource(id = it),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = iconColor
                                    )
                                }
                            })
                        } else emptyMap()
                    )
                }
            }
        }
    }
}
