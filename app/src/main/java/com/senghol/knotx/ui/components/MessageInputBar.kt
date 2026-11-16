package com.senghol.knotx.ui.components
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.R
import com.senghol.knotx.data.database.GroupSandesh
import com.senghol.knotx.ui.theme.GroupDootawasColors
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// 👇 replace with your actual package paths

fun formatLastSeen(
    lastOnline: Long,
    longAgoText: String,
    minFormat: String,  // e.g., "Active %dm ago"
    hourFormat: String, // e.g., "Active %dh ago"
    dayFormat: String   // e.g., "Active %dd ago"
): String {

    if (lastOnline <= 0) return longAgoText

    val timestamp = if (lastOnline < 1_000_000_000_000L) {
        lastOnline * 1000
    } else {
        lastOnline
    }

    val now = System.currentTimeMillis()
    val diffMillis = now - timestamp

    if (diffMillis <= 0) return longAgoText

    val minutes = diffMillis / 60000
    val days = minutes / 1440

    if (days >= 365) return longAgoText

    return when {
        minutes < 60 -> String.format(minFormat, minutes.coerceAtLeast(1))
        minutes < 1440 -> String.format(hourFormat, minutes / 60)
        else -> String.format(dayFormat, days)
    }
}

@Composable
fun MessageInputBar(
    isBlocked: Boolean,
    replyMessage: Boolean,
    singleSelectedSandesh: Sandesh?,
    enterToSendEnabled: Boolean,
    dootawasColors: DootawasColors,
    defaultDisappearSeconds: Int,               // 👈 NEW — from upbhogta.autoDisappearMsg, the standing default
    onSendText: (String, Int) -> Unit,          // 👈 CHANGED — now also carries the effective disappear duration
    onSendReply: (String, Int) -> Unit,         // 👈 CHANGED — same
    onCameraClick: () -> Unit,
    onRecorderClick: () -> Unit,
    onAttachClick: () -> Unit,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier,
    onTyping: () -> Unit,
    sandeshViewModel: SandeshViewModel,
    draftMessage: String?,
    userNumber: String,
    lastOnline: Long,

) {
    var messageText by rememberSaveable { mutableStateOf("") }
    val currentText by rememberUpdatedState(messageText)

    val replyText = remember(singleSelectedSandesh?.sandeshId) {
        val json = try { JsonParser.parseString(singleSelectedSandesh?.samagri).asJsonObject }
        catch (e: Exception) { JsonObject() }
        when (singleSelectedSandesh?.let { SandeshType.fromInt(it.sandeshType) }) {
            SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE ->
                json.get(DootawasMessage.PAATH)?.asString.orEmpty()
            else -> "" // other types same as before
        }
    }
    val presenceState by sandeshViewModel
        .observePresence(userNumber)
        .collectAsState()

    val defaultTypeMessage = stringResource(R.string.type_a_message)
    val blockedMessage = stringResource(R.string.you_blocked_this_contact)
    val onlineMessage = stringResource(R.string.status_online)

    val longAgoText = stringResource(R.string.status_long_ago)
    val minFormat = stringResource(R.string.active_minutes_ago)
    val hourFormat = stringResource(R.string.active_hours_ago)
    val dayFormat = stringResource(R.string.active_days_ago)

    var placeholderText by remember(userNumber) {
        mutableStateOf(defaultTypeMessage)
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // lastOnline only matters while the user is offline
    val statusKey = if (presenceState == false) lastOnline else 0L

    LaunchedEffect(userNumber, isBlocked, presenceState, statusKey) {
        if (isBlocked) {
            placeholderText = blockedMessage
            return@LaunchedEffect
        }

        if (presenceState == null) {
            placeholderText = defaultTypeMessage
            return@LaunchedEffect
        }

        placeholderText = defaultTypeMessage
        delay(2000.milliseconds)

        placeholderText = if (presenceState == true) {
            onlineMessage
        } else {
            formatLastSeen(lastOnline, longAgoText, minFormat, hourFormat, dayFormat)
        }

        delay(2000.milliseconds)
        placeholderText = defaultTypeMessage
    }
    // ── NEW: per-message override, resets to the upbhogta default after every send ──
    var overrideDisappearSeconds by remember { mutableStateOf<Int?>(null) }
    val effectiveDisappearSeconds = overrideDisappearSeconds ?: defaultDisappearSeconds
    var showDisappearPicker by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()
    val iconTransition = updateTransition(messageText.isNotBlank(), label = "icon")
    val iconScale by iconTransition.animateFloat(
        transitionSpec = { tween(200, easing = FastOutSlowInEasing) },
        label = "iconScale"
    ) { if (it) 1f else 0.9f }

    fun sendCurrentText() {
        val text = messageText.trimEnd('\n')
        if (replyMessage) onSendReply(text, effectiveDisappearSeconds)
        else onSendText(text, effectiveDisappearSeconds)
        messageText = ""
        overrideDisappearSeconds = null // 👈 revert to the default for the NEXT message
    }

    // Restore the draft once, when it has actually loaded
    var draftRestored by rememberSaveable(userNumber) { mutableStateOf(false) }

    LaunchedEffect(draftMessage) {
        if (!draftRestored && draftMessage != null) {
            if (draftMessage.isNotEmpty() && messageText.isEmpty()) {
                messageText = draftMessage
            }
            draftRestored = true
        }
    }

    // Save the draft on screen-off or leave (an empty string clears it)
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            sandeshViewModel.saveDraft(draft = currentText.trim())
            sandeshViewModel.resetUnread()
        }
    }
    Column(modifier = modifier) {

        Row(
            modifier = Modifier
                .imePadding()
                .background(dootawasColors.chatScreenBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(dootawasColors.userHeaderBackground)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(iconScale)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(24.dp))
                    .background(
                        dootawasColors.userHeaderBackground,
                        RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .animateContentSize()
            ) {
                // ── Reply row (separate row, same column) ──
                if (replyMessage && singleSelectedSandesh != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = replyText,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = onCancelReply,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // ── Text row (your original Box, now a child of the Column) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 120.dp)
                        .verticalScroll(scrollState)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    BasicTextField(
                    value = messageText,
                    onValueChange = {
                        if (!isBlocked) messageText = it
                        if (it.isNotBlank()) onTyping()
                    },
                    enabled = !isBlocked,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (enterToSendEnabled) ImeAction.Send else ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isBlocked && enterToSendEnabled && messageText.isNotBlank()) {
                                sendCurrentText()
                                //todo keyboard close after send
//                                keyboardController?.hide()
//                                focusManager.clearFocus()
                            }
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(modifier = Modifier.weight(1f)) {

                                if (messageText.isEmpty()) {
                                    Text(
                                        text = if (isBlocked)
                                            stringResource(R.string.blocked_contact_placeholder)
                                        else
                                            placeholderText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp
                                    )
                                }

                                innerTextField()
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { showDisappearPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AvTimer,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (effectiveDisappearSeconds > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                    )
                }   // closes the text Box
            }       // closes the Column (pill)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(dootawasColors.userHeaderBackground, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = {
                            if (!isBlocked) {
                                if (messageText.isNotBlank()) {
                                    sendCurrentText()
                                } else {
                                    onCameraClick()
                                }
                            }
                        },
                        onLongClick = {
                            if (messageText.isBlank() && !isBlocked) onRecorderClick()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = messageText.isNotBlank(), animationSpec = tween(200)) { isSend ->
                    if (isSend) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(iconScale)
                        )
                    } else {

                        Icon(
                            painter = painterResource(R.drawable.ic_camera_modern),   // or Icons.Outlined.PhotoCamera
                            contentDescription = stringResource(R.string.camera),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.scale(iconScale)
                        )
                    }
                }
            }
        }
    }

    // ── NEW: override picker — reuses the same swipe-column dialog from Settings ──
    if (showDisappearPicker) {
        DisappearingMessagesPickerDialog(
            currentSeconds = effectiveDisappearSeconds,
            onDismiss = { showDisappearPicker = false },
            onSelected = { seconds ->
                overrideDisappearSeconds = seconds
                showDisappearPicker = false
            }
        )
    }
}


@Composable
fun GroupMessageInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    isGroupActive: Boolean?,
    enterToSendEnabled: Boolean,
    dootawasColors: GroupDootawasColors,
    onAttachClick: () -> Unit,
    onSendMessage: () -> Unit,
    onCameraClick: () -> Unit,
    onRecorderClick: () -> Unit,
    onTyping: () -> Unit,
    replyMessage: Boolean,
    singleSelectedSandesh: GroupSandesh?,
    onCancelReply: () -> Unit = {}
) {


    val scrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    val iconTransition = updateTransition(
        targetState = messageText.isNotBlank(),
        label = "groupInputIconTransition"
    )
    val iconScale by iconTransition.animateFloat(
        transitionSpec = { tween(200, easing = FastOutSlowInEasing) },
        label = "groupInputIconScale"
    ) { if (it) 1f else 0.9f }

    val replyText = remember(replyMessage, singleSelectedSandesh?.sandeshId) {
        if (!replyMessage || singleSelectedSandesh == null) return@remember ""

        val json = try {
            JsonParser.parseString(singleSelectedSandesh.samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        when (SandeshType.fromInt(singleSelectedSandesh.sandeshType)) {
            SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE ->
                json.get(DootawasMessage.PAATH)?.asString.orEmpty()
            else -> ""
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .imePadding()
            .fillMaxWidth()
            .background(dootawasColors.chatScreenBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Attachment button
        IconButton(
            onClick = onAttachClick,
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.scale(iconScale)
            )
        }

        // Reply preview and message input share the same rounded container
        Column(
            modifier = Modifier
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
                .animateContentSize()
        ) {
            if (replyMessage && singleSelectedSandesh != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                    )

                    Text(
                        text = replyText,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanding, scrollable text input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 120.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.type_a_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                BasicTextField(
                    value = messageText,
                    enabled = isGroupActive == true,
                    onValueChange = {
                        onMessageChange(it)
                        if (it.isNotBlank()) onTyping()
                    },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (enterToSendEnabled) {
                            ImeAction.Send
                        } else {
                            ImeAction.Default
                        }
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (
                                enterToSendEnabled &&
                                isGroupActive == true &&
                                messageText.isNotBlank()
                            ) {
                                onSendMessage()
                                //todo if want to close keyboard after send
//                                keyboardController?.hide()
//                                focusManager.clearFocus()
                            }
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )
            }
        }

        // Send / camera button; long press opens recorder
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        when {
                            messageText.isNotBlank() && isGroupActive == true ->
                                onSendMessage()
                            messageText.isBlank() ->
                                onCameraClick()
                        }
                    },
                    onLongClick = {
                        if (messageText.isBlank() && isGroupActive == true) {
                            onRecorderClick()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = messageText.isNotBlank(),
                animationSpec = tween(200),
                label = "groupSendCameraTransition"
            ) { isSend ->
                if (isSend) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(iconScale)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_camera_modern),
                        contentDescription = stringResource(R.string.camera),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(iconScale)
                    )
                }
            }
        }
    }
}
