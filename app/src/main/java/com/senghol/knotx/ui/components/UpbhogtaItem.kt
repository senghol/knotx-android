package com.senghol.knotx.ui.components

import com.senghol.knotx.R
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.senghol.knotx.connection.Knotx.Companion.QUICK_TIPS_NUMBER
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.UpbhogtaUiItem
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.delay
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun UpbhogtaItem(
    upbhogta: UpbhogtaUiItem,
    index: Int,
    lastIndex: Int,
    isTyping: Boolean,
    onClick: (UpbhogtaUiItem) -> Unit,
    onLongClick: (UpbhogtaUiItem) -> Unit,
    onSwipeRight: (UpbhogtaUiItem) -> Unit,
    onSwipeLeft: (UpbhogtaUiItem) -> Unit,
    dootawasColors: DootawasColors
) {
//    val theme = LocalDootawasTheme.current
//    val dootawasColors = theme.colors

    // Swipe right → video call (green)
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
        onSwipe = { onSwipeRight(upbhogta) }
    )

    // Swipe left → audio call (blue)
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
        onSwipe = { onSwipeLeft(upbhogta) }
    )

    SwipeableActionsBox(
        startActions = listOf(videoCallAction),  // swipe right
        endActions = listOf(audioCallAction),    // swipe left
        swipeThreshold = 100.dp,                 // must drag 80dp before it triggers — no more misfires
        modifier = Modifier.fillMaxWidth()
    ) {
        // ✅ Your entire existing content Box goes here unchanged
        val colorScheme = MaterialTheme.colorScheme
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dootawasColors.homeChatBackground,
                                dootawasColors.homeChatBackground
                            )
                        )
                    )
                    .combinedClickable(
                        onClick = { onClick(upbhogta) },
                        onLongClick = { onLongClick(upbhogta) }
                    )
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var showPreview by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                shadowElevation = 8.dp.toPx()
                                shape = CircleShape
                                clip = false
                            }
                            .clip(CircleShape)
                            .background(
                                color = if (upbhogta.imageUrl == null) Color(upbhogta.background)
                                else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(enabled = upbhogta.imageUrl != null) { showPreview = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val isQuickTips = upbhogta.userNumber == QUICK_TIPS_NUMBER

                        when {
                            isQuickTips -> {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_quick_tips2),
                                    contentDescription = "Quick Tips",
                                    modifier = Modifier.size(46.dp).clip(CircleShape)
                                )
                            }

                            !upbhogta.imageUrl.isNullOrBlank() -> {
                                AsyncImage(
                                    model = upbhogta.imageUrl,
                                    contentDescription = "Profile Image",
                                    modifier = Modifier.size(46.dp).clip(CircleShape)
                                )
                            }

                            else -> {
                                val backgroundColor =
                                    Color(upbhogta.background).takeIf { upbhogta.background != 0 }
                                        ?: MaterialTheme.colorScheme.primary
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(backgroundColor)
                                        .padding(10.dp)
                                )
                            }
                        }

                        if (upbhogta.isPinned) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(color = Color(0xFF051304), shape = CircleShape)
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

                        if (showPreview && upbhogta.imageUrl != null) {
                            ProfileImagePreviewDialog(
                                imageUrl = upbhogta.imageUrl,
                                onDismiss = { showPreview = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text(
                            text = upbhogta.name.takeIf { it.isNotEmpty() } ?: upbhogta.userNumber,
                            color = dootawasColors.homeText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isTyping) {
                            Text(
                                text = "typing...",
                                color = Color(0xFF4CAF50),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (upbhogta.sandeshStithi == SandeshStithi.SAMARPIT ||
                                    upbhogta.sandeshStithi == SandeshStithi.PREKSHIT ||
                                    upbhogta.sandeshStithi == SandeshStithi.DHRIST
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_status),
                                        contentDescription = "Message status",
                                        tint = when (upbhogta.sandeshStithi) {
                                            SandeshStithi.SAMARPIT -> Color(0xFFFF9800)
                                            SandeshStithi.PREKSHIT -> Color(0xFF9E9E9E)
                                            SandeshStithi.DHRIST -> Color(0xFF4CAF50)
                                            else -> Color.Unspecified
                                        },
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                }

                                Text(
                                    text = buildAnnotatedString {
                                        val message = upbhogta.lastMessage
                                            ?: stringResource(R.string.no_messages_yet)

                                        if (message.startsWith("Draft:")) {
                                            withStyle(
                                                SpanStyle(
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ) {
                                                append("Draft:")
                                            }
                                            append(message.removePrefix("Draft:"))
                                        } else {
                                            append(message)
                                        }
                                    },
                                    color = dootawasColors.lastMessageColor,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.wrapContentWidth().padding(start = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (upbhogta.isMuted) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Muted",
                                    tint = Color(0xFF009688),
                                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                                )
                            }
                            if (upbhogta.totalUnread > 0) {
                                NotificationBadge(upbhogta.totalUnread)
                            }
                        }
                        Text(
                            text = upbhogta.lastMessageTime,
                            color = dootawasColors.lastMessageColor,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

            }

        }

    }
}


@Composable
fun NotificationBadge(
    totalUnread: Int,
    modifier: Modifier = Modifier // Default Modifier if none is passed
) {

        val theme = LocalDootawasTheme.current
    val dootawasColors = theme.colors
        // Animation state for scale effect
        var animate by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (animate) 1.1f else 1f,
            animationSpec = tween(durationMillis = 300)
        )

        // Trigger animation on change in totalUnread
        LaunchedEffect(totalUnread) {
            animate = true
            delay(300.milliseconds)
            animate = false
        }

        Box(
            modifier = modifier
                .size(if (totalUnread > 99) 28.dp else 22.dp) // Larger size for 3+ digits
                .scale(scale) // Apply scale animation
                .bottomShadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    cornerRadius = 10.dp,
                    blurRadius = 3.dp,
                    offsetY = 2.dp
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            dootawasColors.unreadBadgeColor.copy(alpha = 0.75f),
                            dootawasColors.unreadBadgeColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (totalUnread > 99) "99+" else totalUnread.toString(),
                color = Color.White, // White for better contrast
                fontSize = if (totalUnread > 99) 10.sp else 12.sp, // Adjust font size for 99+
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }

}