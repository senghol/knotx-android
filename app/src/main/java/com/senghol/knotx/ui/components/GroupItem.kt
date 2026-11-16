package com.senghol.knotx.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults.flingBehavior
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Groups3
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.senghol.knotx.R
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.utils.data.GroupUiItem
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.math.roundToInt


@Composable
fun GroupItem(
    groupItem: GroupUiItem,
    isTyping: Boolean,
    onClick: (GroupUiItem) -> Unit,
    onLongClick: (GroupUiItem) -> Unit,
    onSwipeRight: (GroupUiItem) -> Unit,
    onSwipeLeft: (GroupUiItem) -> Unit,
    dootawasColors: DootawasColors
) {
    val videoCallAction = SwipeAction(
        icon = {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = "Video call",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = Color(0xFF43A047),
        onSwipe = { onSwipeRight(groupItem) }
    )

    val audioCallAction = SwipeAction(
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.call_24px),
                contentDescription = "Audio Call",
                tint = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        },
        background = Color(0xFF1E88E5),
        onSwipe = { onSwipeLeft(groupItem) }
    )

    SwipeableActionsBox(
        startActions = listOf(videoCallAction),
        endActions = listOf(audioCallAction),
        swipeThreshold = 120.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main Content (Draggable)
        val colorScheme = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(dootawasColors.homeChatBackground)
                .combinedClickable(
                    onClick = { onClick(groupItem) },
                    onLongClick = { onLongClick(groupItem) }
                )
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (groupItem.imageUrl == null) Color.DarkGray
                            else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    if (!groupItem.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = groupItem.imageUrl,
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                    } else {


                        val backgroundColor = Color(0xFF0A7152) // ✅ Your custom fallback color
                        Icon(
                            imageVector = Icons.Filled.Groups3,
                            contentDescription = "Default profile",
                            tint = Color.White,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(groupItem.groupBackgroundColor))
                                .padding(10.dp) // keeps the icon nicely inside the circle
                        )
                    }
                    if (groupItem.isPinned) {

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    colorScheme.inverseSurface.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                                .align(Alignment.TopEnd)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFFF1744),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // 🧠 User name
                    Text(
                        text = groupItem.name.takeIf { it.isNotEmpty() } ?: groupItem.id,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // 🧩 Typing or Message Section
                    if (isTyping) {
                        // 🔹 Show typing GIF only (using Coil)
                        AsyncImage(
                            model = R.drawable.typing,  // e.g., res/drawable/typing_gif.gif
                            contentDescription = "Typing...",
                            modifier = Modifier
                                .height(24.dp)
                                .width(40.dp)
                                .padding(top = 4.dp)
                        )
                    } else {
                        // 🔹 Show message row with status + message + time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // ✅ Status icon (same icon, dynamic tint)
// ✅ Show status icon only for specific stithis
                            if (groupItem.sandeshStithi == SandeshStithi.SAMARPIT ||
                                groupItem.sandeshStithi == SandeshStithi.PREKSHIT ||
                                groupItem.sandeshStithi == SandeshStithi.DHRIST
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.icon_status),
                                    contentDescription = "Message status",
                                    tint = when (groupItem.sandeshStithi) {
                                        SandeshStithi.SAMARPIT -> Color(0xFFFF9800)   // gray for sending
                                        SandeshStithi.PREKSHIT -> Color(0xFF9E9E9E)   // orange for sent
                                        SandeshStithi.DHRIST -> Color(0xFF4CAF50)     // green for read
                                        else -> Color.Unspecified                     // fallback, won't be used
                                    },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp, bottom = 2.dp)
                                )
                            }


                            // 💬 Last message
                            Text(
                                text = groupItem.lastMessage ?: "No messages yet",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                        }
                    }
                }
// RIGHT COLUMN (Mute + Unread + Time)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(start = 8.dp)
                ) {

                    // 🔕 Mute + 🔴 Badge row (TOP)
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        if (groupItem.isMuted) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Muted",
                                tint = Color(0xFF009688),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 6.dp)
                            )
                        }

                        if (groupItem.totalUnread > 0) {
                            NotificationBadge(groupItem.totalUnread)
                        }
                    }

                    // 🕒 TIME (bottom)
                    Text(
                        text = groupItem.lastMessageTime,
                        color = colorScheme.outline,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

            }
        }
    }

}
