package com.senghol.knotx.ui.components.group

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.R
import com.senghol.knotx.ui.components.ReplyBubble
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.ContactInfo
import com.senghol.knotx.utils.data.GroupParsedContactSandesh
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
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
fun GroupContactBubble(
    sandeshId: String,
    userNumber: String,
    userColor: Int,
    userImageUri: Uri?,
    isSelf: Boolean,
    sandeshType: SandeshType,
    timeLong: Long,
    formattedTime: String,
    formattedDate: String,
    contacts: List<ContactInfo>,
    sandeshStithi: SandeshStithi,
    replyOriginalMessage: JsonObject?,
    isSelected: Boolean = false,
    isHighlighted: Boolean,
    isFirstInGroup: Boolean = true,   // false for 2nd, 3rd... message in a run from the same person
    onLongPress: () -> Unit,
    onToggleSelection: (String?) -> Unit,
    onReplyClick: (sandeshID: String) -> Unit,
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
    val textColor = if (isSelf) dootawasColors.rightMessageText else dootawasColors.leftMessageText
    val timeColor = if (isSelf) dootawasColors.rightTimeText else dootawasColors.leftTimeText
    val mutedText = textColor.copy(alpha = 0.7f)

    // ---------- State ----------
    var showRelativeTime by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipe = with(LocalDensity.current) { 90.dp.toPx() }
    val trigger = with(LocalDensity.current) { 72.dp.toPx() }
    val context = LocalContext.current

    val timeText = if (showRelativeTime) getRelativeTimeSpanMinuteLevel(timeLong) else formattedTime
    val onBubbleClick = { onToggleSelection(null) }

    val statusColor: Color? = if (isSelf) {
        when (sandeshStithi) {
            SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)
            SandeshStithi.PREKSHIT -> Color.Gray
            SandeshStithi.DHRIST -> Color(0xFF00C853)
            else -> null
        }
    } else null

    fun dial(phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .padding(top = if (isFirstInGroup) 5.dp else 1.dp, bottom = 1.dp),
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

            // Avatar OUTSIDE the bubble, top-left. Only on the first message of a run;
            // later messages keep an empty slot so all bubbles stay aligned.
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
                    .widthIn(min = 220.dp, max = 270.dp)
                    .shadow(1.dp, RoundedCornerShape(10.dp), clip = false)
                    .background(bubbleColor, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onBubbleClick, onLongClick = onLongPress)
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 5.dp)
            ) {

                // Username INSIDE the bubble (first message of a run only)
                if (!isSelf && isFirstInGroup && userNumber.isNotBlank()) {
                    Text(
                        text = userNumber.removePrefix("+"),
                        color = Color(userColor),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        style = GcbNoFontPadding,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // ----- Contacts -----
                contacts.forEachIndexed { index, contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar: first letter of the name
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(textColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = contact.name.trim().firstOrNull()?.uppercaseChar()
                            if (initial != null) {
                                Text(
                                    text = initial.toString(),
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Name + number
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = GcbNoFontPadding,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = contact.phone,
                                color = mutedText,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                style = GcbNoFontPadding,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Call button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { dial(contact.phone) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = mutedText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Divider between contacts (inset past the avatar)
                    if (index < contacts.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp, top = 6.dp, bottom = 6.dp),
                            thickness = 0.5.dp,
                            color = textColor.copy(alpha = 0.15f)
                        )
                    }
                }

                // ----- Time, bottom-right -----
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                        .clickable { showRelativeTime = !showRelativeTime },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeText,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        color = timeColor,
                        style = GcbNoFontPadding
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

private val GcbNoFontPadding = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)