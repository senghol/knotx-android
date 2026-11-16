package com.senghol.knotx.ui.components


import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import com.senghol.knotx.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp

import androidx.compose.material3.ripple
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.Column

import androidx.compose.material3.HorizontalDivider

// Utility function to make copying plain text easy
import android.content.ClipData
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import com.senghol.knotx.utils.data.ReactionUi
import com.senghol.knotx.utils.data.SandeshStatusInfo
import com.senghol.knotx.utils.data.SingleMessageStatus
import com.senghol.knotx.utils.enums.SandeshStithi


import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme

import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds



import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*

suspend fun Clipboard.setText(text: String) {
    this.setClipEntry(
        ClipEntry(ClipData.newPlainText("Copied Text", text))
    )
}
// OR, if you need to support AnnotatedString:
suspend fun Clipboard.setText(annotatedString: AnnotatedString) {
    this.setClipEntry(
        ClipEntry(ClipData.newPlainText("Copied Text", annotatedString.text))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSelectionBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onInfo: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    showReactionBar: Boolean = false,
    quickReactions: List<String> = emptyList(),
    onReactionSelected: (String) -> Unit = {},
    onExpandEmojiPicker: () -> Unit = {},
    surfaceColor: Color,
    contentColor: Color = LocalContentColor.current
) {
    val isSingleSelected = selectedCount == 1

    Surface(
        modifier = Modifier.fillMaxWidth(), // no animateContentSize -> fixes the jump
        color = surfaceColor,
        contentColor = contentColor,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp)
        ) {
            // Quick reaction row
            AnimatedVisibility(
                visible = showReactionBar && isSingleSelected,
                enter = fadeIn(tween(200)) + expandVertically(
                    animationSpec = tween(250),
                    expandFrom = Alignment.Top
                ),
                exit = fadeOut(tween(150)) + shrinkVertically(
                    animationSpec = tween(250),
                    shrinkTowards = Alignment.Top
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickReactions.forEach { emoji ->
                            EmojiReactionItem(
                                emoji = emoji,
                                onClick = { onReactionSelected(emoji) }
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onExpandEmojiPicker() },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "😀", fontSize = 18.sp)
                                Text(
                                    text = "+",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Bottom action row: every visible item gets an equal-width slot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Counter badge
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = contentColor.copy(alpha = 0.12f),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = contentColor
                            )
                        }
                    }
                }

                // 2. Single-item actions
                if (isSingleSelected) {
                    ActionSlot {
                        IconBtn(Icons.Default.ContentCopy, "Copy", onCopy, tint = contentColor)
                    }
                    ActionSlot {
                        IconBtn(Icons.Default.Info, "Info", onInfo, tint = contentColor)
                    }
                    ActionSlot {
                        IconBtn(
                            Icons.AutoMirrored.Filled.Reply,
                            "Reply",
                            onReply,
                            tint = contentColor
                        )
                    }
                }

                // 3. Forward
                if (selectedCount <= 10) {
                    ActionSlot {
                        IconBtn(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Forward",
                            onClick = onForward,
                            tint = contentColor,
                            modifier = Modifier.graphicsLayer { scaleX = -1f }
                        )
                    }
                }

                // 4. Delete
                if (selectedCount <= 50) {
                    ActionSlot {
                        IconBtn(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            onClick = onDelete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
@Composable
fun EmojiPickerBottomSheet(
    onDismiss: () -> Unit,
    onEmojiPicked: (String) -> Unit
) {

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.85f
    val minHeight = 280.dp

    var sheetHeight by remember { mutableStateOf(minHeight) }
    val animatedHeight by animateDpAsState(sheetHeight)

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
            .padding(WindowInsets.systemBars.asPaddingValues()) // safe area
    ) {

        // 👇 Bottom anchored sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(animatedHeight)
                .background(
                    MaterialTheme.colorScheme.onSurface,
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {

            // 🔥 Drag handle (only this area resizes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val newHeight = sheetHeight - dragAmount.dp
                            sheetHeight = newHeight.coerceIn(minHeight, maxHeight)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Blue, RoundedCornerShape(50))
                )
            }

            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        emojiGridColumns = 7
                        isNestedScrollingEnabled = true

                        setOnEmojiPickedListener {
                            onEmojiPicked(it.emoji)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
// ---- emoji item ----

@Composable
fun MessageInputArea(
    onEmojiSelected: (String) -> Unit
) {
    var isPickerVisible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current // For controlling the keyboard
    var message by remember { mutableStateOf("") } // The text field content

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Your Message") },
            trailingIcon = {
                IconButton(onClick = {
                    isPickerVisible = !isPickerVisible
                    // Optional: Control keyboard when toggling picker
                    if (isPickerVisible) {
                        keyboardController?.hide()
                    } else {
                        keyboardController?.show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Favorite, // Placeholder for an emoji icon
                        contentDescription = "Toggle Emoji Picker"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // The Jetpack Emoji Picker integrated into Compose
        if (isPickerVisible) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp), // Define a height for the picker
                factory = { context ->
                    EmojiPickerView(context).apply {
                        // Set the listener to pass the selected emoji up to the Composable
                        setOnEmojiPickedListener { emojiItem ->
                            onEmojiSelected(emojiItem.emoji)
                        }
                        emojiGridColumns = 9 // Customization for more columns in Compose
                    }
                },
                update = { view ->
                    // Handle View updates if needed, e.g., theme changes
                }
            )
        }
    }
}

@Composable
 fun EmojiReactionItem(emoji: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = ""
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(120)
            pressed = false
        }
    }

    Text(
        text = emoji,
        fontSize = 26.sp,
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = { pressed = true; onClick() }
            )
            .padding(10.dp)
    )
}

// ---- icon btn ----
@Composable
private fun IconBtn(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
            .padding(12.dp)
    )
}
@Composable
fun WrittenRow(time: Long?, formatDateTime: (Long) -> String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (time == null || time == 0L) "Not yet" else formatDateTime(time),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (time == null || time == 0L)
                Color.Red.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun SentRow(
    time: Long?,
    formatDateTime: (Long) -> String,
    stithi: SandeshStithi = SandeshStithi.PREKSHIT
) {
    val ui = getStithiUI(stithi)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, bottom = 10.dp),  // <— added start padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        ui.icon?.let { iconRes ->
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = ui.color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        // Time
        Text(
            text = if (time == null || time == 0L) "Not yet" else formatDateTime(time),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (time == null || time == 0L)
                Color.Red.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun GroupMessageInfoDialog(
    status: SandeshStatusInfo,
    messageId: String,
    sandeshViewModel: SandeshViewModel,
    onDismiss: () -> Unit,
    formatDateTime: (Long) -> String
) {
    LaunchedEffect(messageId) {
        sandeshViewModel.loadGroupReactions(
            sandeshId = messageId,
        )
    }

    val reactions by sandeshViewModel.groupReactionsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp) // 🔥 space from top & bottom
                .heightIn(max = 600.dp)    // 🔥 prevent full screen stretch
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()) // 🔥 SCROLLABLE
                    .padding(18.dp)
            ) {

                Text("Message Info", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("ID: $messageId", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                WrittenRow(
                    time = status.likhit,
                    formatDateTime = formatDateTime
                )

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                SentRow(
                    time = status.prekshit,
                    formatDateTime = formatDateTime
                )

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                GroupDeliveryStatusSection(
                    stithi = SandeshStithi.SAMARPIT,
                    userStatusMap = status.samarpit,
                    formatDateTime = formatDateTime
                )

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                GroupDeliveryStatusSection(
                    stithi = SandeshStithi.DHRIST,
                    userStatusMap = status.dhrist,
                    formatDateTime = formatDateTime
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Reactions",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(8.dp))

                if (reactions.isEmpty()) {
                    Text("No reactions", color = MaterialTheme.colorScheme.outline)
                } else {
                    reactions.forEach { reaction ->
                        GroupReactionRow(
                            reaction = reaction,
                            isSelf = reaction.userId == "self",
                            onDelete = {
                                sandeshViewModel.reactToMsg(
                                    emoji = "",
                                    msgId = messageId
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
@Composable
fun GroupReactionRow(
    reaction: ReactionUi,
    isSelf: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = reaction.emoji,
            fontSize = 22.sp
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (reaction.userId == "self") {
                "You"
            } else {
                reaction.userId
            },
            modifier = Modifier.weight(1f)
        )

        if (isSelf) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove reaction"
                )
            }
        }
    }
}
@Composable
fun GroupDeliveryStatusSection(
    stithi: SandeshStithi,                      // SAMARPIT or DHRIST
    userStatusMap: Map<String, Long?>,          // number -> timestamp or null/0
    formatDateTime: (Long) -> String            // your formatter
) {
    val ui = getStithiUI(stithi)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {

        // ICON COLUMN
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ui.icon?.let { iconRes ->
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = ui.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // RIGHT COLUMN (multi-user)
        Column(modifier = Modifier.weight(1f)) {

            userStatusMap.entries.forEachIndexed { index, entry ->

                val number = entry.key
                val time = entry.value

                val display = when {
                    time == null || time == 0L -> "Not yet"
                    else -> formatDateTime(time)
                }

                Column(modifier = Modifier.padding(vertical = 4.dp)) {

                    // PHONE NUMBER
                    Text(
                        text = number,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // TIME OR NOT YET
                    Text(
                        text = display,
                        fontSize = 13.sp,
                        color = if (time == null || time == 0L)
                            Color.Red.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }

                if (index < userStatusMap.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
@Composable
fun DeliveredRow(time: Long?, formatDateTime: (Long) -> String) {
    val ui = getStithiUI(SandeshStithi.SAMARPIT)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),

                verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = ui.icon!!),
            contentDescription = null,
            tint = ui.color,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = if (time == null) "Not yet" else formatDateTime(time),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (time == null)
                Color.Red.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SeenRow(time: Long?, formatDateTime: (Long) -> String) {
    val ui = getStithiUI(SandeshStithi.DHRIST)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = ui.icon!!),
            contentDescription = null,
            tint = ui.color,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = if (time == null) "Not yet" else formatDateTime(time),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (time == null)
                Color.Red.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SingleMessageInfoDialog(
    status: SingleMessageStatus,
    messageId: String,
    sandeshViewModel: SandeshViewModel,
    onDismiss: () -> Unit,
    formatDateTime: (Long) -> String
) {
    LaunchedEffect(messageId) {
        sandeshViewModel.loadReactions(
            sandeshId = messageId
        )
    }
    val reactions by sandeshViewModel.reactionsFlow.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                Text("Message Info", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text("ID: $messageId", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Sent (icon + time)
                SentRow(status.prekshit, formatDateTime)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Delivered (single)
                DeliveredRow(status.samarpit, formatDateTime)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Seen (single)
                SeenRow(status.dhrist, formatDateTime)


                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Reactions",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(8.dp))



                if (reactions.isEmpty()) {
                    Text("No reactions")
                } else {
                    reactions.forEach { reaction ->

                        ReactionRow(
                            reaction = reaction,
                            isSelf = reaction.userId == "self",
                            onDelete = {
                                sandeshViewModel.reactToMsg(
                                    msgId = messageId,
                                    emoji = ""
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ReactionRow(
    reaction: ReactionUi,
    isSelf: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = reaction.emoji,
            fontSize = 22.sp
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (reaction.userId == "self") "You" else "Other"
        )

        Spacer(Modifier.weight(1f))

        if (isSelf) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove reaction"
                )
            }
        }
    }
}
data class StithiUI(
    @param:DrawableRes val icon: Int?,
    val color: Color
)


fun getStithiUI(stithi: SandeshStithi): StithiUI {
    val icon = when (stithi) {
        SandeshStithi.PREKSHIT,
        SandeshStithi.SAMARPIT,
        SandeshStithi.DHRIST -> R.drawable.icon_status
        else -> null
    }

    val color = when (stithi) {
        SandeshStithi.SAMARPIT -> Color(0xFFFF6D00)  // orange (delivered)
        SandeshStithi.PREKSHIT -> Color.Gray         // sent
        SandeshStithi.DHRIST -> Color(0xFF00C853)    // green (seen)
        else -> Color.Unspecified
    }

    return StithiUI(icon, color)
}
@Composable
fun GlassAttachmentPanel(
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onDocumentClick: () -> Unit = {}

) {
    // Controls the animation progress (0f → 1f)
    var animateIn by remember { mutableStateOf(false) }

    // Trigger animation once when panel appears
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val theme = LocalDootawasTheme.current
    val dootawasColors = theme.colors
    Box(
        modifier = modifier
            .wrapContentSize()   // 🔥 instead of filling width
            .clip(RoundedCornerShape(6.dp))
            .background(dootawasColors.glassColor)  // same glass look as before
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Row 1
            Row(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp
                ),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIcon(
                    icon = Icons.Filled.Image,
                    desc = "Image",
                    color = Color(0xFFFF9800),  // Orange for photos
                    animateIn = animateIn,
                    index = 0,
                    onClick = onImageClick
                )
                AnimatedIcon(
                    icon = Icons.Filled.Videocam,
                    desc = "Video",
                    color = Color(0xFFE53935),  // Red
                    animateIn = animateIn,
                    index = 1,
                    onClick = onVideoClick
                )
            }

            // Row 2
            Row(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp
                ),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIcon(
                    icon = Icons.Filled.Headset,
                    desc = "Audio",
                    color = Color(0xFFCDDC39),  // Purple
                    animateIn = animateIn,
                    index = 2,
                    onClick = onAudioClick
                )
                AnimatedIcon(
                    icon = Icons.Filled.LocationOn,
                    desc = "Location",
                    color = Color(0xFF00BFA5),  // Teal
                    animateIn = animateIn,
                    index = 3,
                    onClick = onLocationClick
                )
            }

            // Row 3
            Row(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp
                ),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedIcon(
                    icon = Icons.Filled.Person,
                    desc = "Contact",
                    color = Color(0xFF42A5F5),  // Blue for people/contact
                    animateIn = animateIn,
                    index = 4,
                    onClick = onContactClick
                )
                AnimatedIcon(
                    icon = Icons.Filled.Description,
                    desc = "Document",
                    color = Color(0xFF5C6BC0),  // Indigo
                    animateIn = animateIn,
                    index = 5,
                    onClick = onDocumentClick
                )
            }
        }
    }
}
@Composable
fun RadialAttachmentMenu(
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onDocumentClick: () -> Unit = {},
    onBillClick: () -> Unit = {}
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    data class RadialItem(
        val icon: ImageVector,
        val color: Color,
        val angle: Float,
        val onClick: () -> Unit,
        val isBill: Boolean = false
    )

    val items = listOf(
        RadialItem(Icons.Filled.Receipt,     Color(0xFFEB5B00), 90f,  onBillClick, isBill = true),
        RadialItem(Icons.Filled.Image,       Color(0xFFFF9800), 60f,  onImageClick),
        RadialItem(Icons.Filled.Videocam,    Color(0xFFE53935), 30f,  onVideoClick),
        RadialItem(Icons.Filled.Headset,     Color(0xFFCDDC39), 0f,   onAudioClick),
        RadialItem(Icons.Filled.LocationOn,  Color(0xFF00BFA5), -30f, onLocationClick),
        RadialItem(Icons.Filled.Person,      Color(0xFF42A5F5), -60f, onContactClick),
        RadialItem(Icons.Filled.Description, Color(0xFF5C6BC0), -90f, onDocumentClick),
    )

    val radius = 90.dp

    Box(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val angleRad = Math.toRadians(item.angle.toDouble())
            val targetX = (cos(angleRad) * radius.value).dp
            val targetY = (-sin(angleRad) * radius.value).dp
            val delayMs = index * 35

            val scale by animateFloatAsState(
                targetValue = if (animateIn) 1f else 0.3f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 320f,
                    visibilityThreshold = 0.001f
                ),
                label = "s$index"
            )
            val alpha by animateFloatAsState(
                targetValue = if (animateIn) 1f else 0f,
                animationSpec = tween(180, delayMs),
                label = "a$index"
            )
            val offsetX by animateDpAsState(
                targetValue = if (animateIn) targetX else 0.dp,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 320f
                ),
                label = "x$index"
            )
            val offsetY by animateDpAsState(
                targetValue = if (animateIn) targetY else 0.dp,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 320f
                ),
                label = "y$index"
            )

            Box(
                modifier = Modifier
                    .offset(x = offsetX - 23.dp, y = offsetY - 23.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(item.color, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { item.onClick() }
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (item.isBill) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(15.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, item.color, CircleShape)
                    ) {
                        Text(
                            "₹",
                            color = item.color,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalAttachmentMenu(
    modifier: Modifier = Modifier,
    onListClick: () -> Unit = {},
    onImageClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onDocumentClick: () -> Unit = {},
    onBillClick: () -> Unit = {},
//    dootawasColors: DootawasColors,
    userHeaderBackground: Color,
    showBillAndList: Boolean = true
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    data class MenuItem(
        val label: String,
        val icon: ImageVector,
        val color: Color,
        val onClick: () -> Unit,
        val isBill: Boolean = false
    )

    val items = buildList {
        if (showBillAndList) {
            add(MenuItem(stringResource(R.string.menu_bill), Icons.Filled.Receipt, Color(0xFFEB5B00), onBillClick, isBill = true))
        }

        add(MenuItem(stringResource(R.string.menu_gallery), Icons.Filled.Image, Color(0xFFFF9800), onImageClick))
        add(MenuItem(stringResource(R.string.menu_video), Icons.Filled.Videocam, Color(0xFFE53935), onVideoClick))
        add(MenuItem(stringResource(R.string.menu_audio), Icons.Filled.Headset, Color(0xFFCDDC39), onAudioClick))
        if (showBillAndList) {
            add(
                MenuItem(
                    stringResource(R.string.menu_list),
                    Icons.AutoMirrored.Filled.Assignment,
                    Color(0xFF7C4DFF),
                    onListClick
                )
            )

        }
        add(MenuItem(stringResource(R.string.menu_location), Icons.Filled.LocationOn, Color(0xFF00BFA5), onLocationClick))
        add(MenuItem(stringResource(R.string.menu_contact), Icons.Filled.Person, Color(0xFF42A5F5), onContactClick))
        add(MenuItem(stringResource(R.string.menu_document), Icons.Filled.Description, Color(0xFF5C6BC0), onDocumentClick))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = userHeaderBackground.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (showBillAndList) 4 else 3),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items) { index, item ->
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(animateIn) {
                    if (animateIn) {
                        delay((index * 35L).milliseconds)
                        itemVisible = true
                    }
                }

                val scale by animateFloatAsState(
                    targetValue = if (itemVisible) 1f else 0.4f,
                    animationSpec = tween(durationMillis = 250),
                    label = "scale_$index"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (itemVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "alpha_$index"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { item.onClick() }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(46.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(item.color, CircleShape)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (item.isBill) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(15.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, item.color, CircleShape)
                            ) {
                                Text(
                                    text = "₹",
                                    color = item.color,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 8.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedIcon(
    icon: ImageVector,
    desc: String,
    color: Color,
    animateIn: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    // Stagger delay: each icon starts a bit later (50ms per index)
    val delay = index * 60

    // Progress of animation (0f at center → 1f at final position)
    val progress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = delay)
    )

    // Calculate offset from center to final position (approximate for 2-col grid)
    // Adjust these values based on your spacing / icon size if needed
    val targetOffsetX = when (index % 2) {
        0 -> -50f   // left column
        else -> 50f // right column
    }
    val targetOffsetY = when (index / 2) {
        0 -> -45f   // top row
        1 -> 0f     // middle row
        else -> 45f // bottom row
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .offset {
                // Start at center (0,0), move to target position
                val dx = (targetOffsetX * progress).toInt()
                val dy = (targetOffsetY * progress).toInt()
                IntOffset(dx, dy)
            }
            .graphicsLayer {
                // Slight scale-up during animation for "pop" feel
                scaleX = 0.7f + (0.3f * progress)
                scaleY = scaleX
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        radius = 32.dp,
                        color = color.copy(alpha = 0.3f)
                    ),
                    onClick = onClick
                )
                .padding(6.dp)
        )
    }
}
// ────────────────────────────────────────────────
// Reusable colored icon with ripple
// ────────────────────────────────────────────────
@Composable
private fun ColoredIcon(
    icon: ImageVector,
    contentDesc: String,
    color: Color,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDesc,
        tint = color,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    radius = 32.dp,
                    color = color.copy(alpha = 0.3f)
                ),
                onClick = onClick
            )
            .padding(6.dp)
    )
}