package com.senghol.knotx.ui.dialogs
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import com.senghol.knotx.R
import com.senghol.knotx.ui.viewmodel.HomeViewModel
import com.senghol.knotx.utils.data.GroupUiItem

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.HorizontalDivider

import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.senghol.knotx.utils.data.UpbhogtaUiItem


//todo there are some texts which not create languages
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun UpbhogtaOptionsDialog(
        upbhogta: UpbhogtaUiItem,
        homeViewModel: HomeViewModel,
        onDismiss: () -> Unit
    ) {
        var isBlocked by remember { mutableStateOf(upbhogta.isBlocked) }
        var isPinned by remember { mutableStateOf(upbhogta.isPinned) }
        var showConfirmation by remember { mutableStateOf<(() -> Unit)?>(null) }
        var isMuted by remember { mutableStateOf(upbhogta.isMuted) }  // ← new state
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Smaller dialog, 80% of screen width
                    .wrapContentHeight()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp), // Reduced padding
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header
                    Text(
                        text = "Options for ${upbhogta.name}",
                        style = MaterialTheme.typography.titleSmall, // Smaller typography
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

// Options
                    OptionItem(
                        icon = if (isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block,
                        text = if (isBlocked) "Unblock" else "Block",
                        onClick = {
                            showConfirmation = {
                                isBlocked = !isBlocked
                                homeViewModel.updateBlockedStatus(upbhogta.name, upbhogta.userNumber, isBlocked)
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    OptionItem(
                        icon = if (isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        text = if (isPinned) "Unpin Chat" else "Pin Chat",
                        onClick = {
                            isPinned = !isPinned
                            homeViewModel.updatePinnedStatus(upbhogta.userNumber, isPinned)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    // Mute / Unmute   ← NEW OPTION
                    OptionItem(
                        icon = if (isMuted)  Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        text = if (isMuted) "Unmute Notifications" else "Mute Notifications",
                        onClick = {
                            isMuted = !isMuted
                            homeViewModel.updateMutedStatus(
                                upbhogta.userNumber,
                                isMuted
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    OptionItem(
                        icon = Icons.Filled.DeleteSweep,
                        text = "Clear Chat",
                        onClick = {
                            showConfirmation = {
                                homeViewModel.clearChat(upbhogta.userNumber)
                                onDismiss()
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    OptionItem(
                        icon = Icons.Default.Delete,
                        text = "Delete Chat",
                        onClick = {
                            showConfirmation = {
                                homeViewModel.deleteUpbhogta(upbhogta.userNumber)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }

        showConfirmation?.let { action ->
            ConfirmationDialogA(
                message = "Are you sure you want to proceed?",
                onConfirm = {
                    action()
                    showConfirmation = null
                },
                onDismiss = { showConfirmation = null }
            )
        }
    }

@Composable
fun OptionItem(
    icon: Any,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) onClick()
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            when (icon) {
                is Int -> Icon(
                    painter = painterResource(id = icon),
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                is ImageVector -> Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun OptionIconButton(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOptionsDialog(
    group: GroupUiItem,
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var isPinned by remember { mutableStateOf(group.isPinned) }
    var isMuted by remember { mutableStateOf(group.isMuted) }
    var showConfirmation by remember { mutableStateOf<(() -> Unit)?>(null) }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {


                // Header
                Text(
                    text = "Options for ${group.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

// Pin / Unpin
                OptionItem(
                    icon = if (isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                    text = if (isPinned) "Unpin Group" else "Pin Group",
                    onClick = {
                        isPinned = !isPinned
                        homeViewModel.updateGroupPinnedStatus(
                            group.id,
                            isPinned
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                // Mute / Unmute ✅
                OptionItem(
                    icon = if (isMuted)
                        Icons.AutoMirrored.Filled.VolumeOff
                    else
                        Icons.AutoMirrored.Filled.VolumeUp,
                    text = if (isMuted) "Unmute Notifications" else "Mute Notifications",
                    onClick = {
                        isMuted = !isMuted
                        homeViewModel.updateGroupMutedStatus(
                            group.id,
                            isMuted
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                // Clear Chat
                OptionItem(
                    icon = Icons.Filled.DeleteSweep,
                    text = "Clear Chat",
                    onClick = {
                        showConfirmation = {
                            homeViewModel.clearGroupChat(group.id)
                            onDismiss()
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                // Exit / Delete Group
                OptionItem(
                    icon = Icons.Default.Delete,
                    text = "Delete / Exit Group",
                    onClick = {
                        showConfirmation = {
                            homeViewModel.deleteGroup(group.id)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }

    // Confirmation Dialog
    showConfirmation?.let { action ->
        ConfirmationDialogA(
            message = "Are you sure you want to proceed?",
            onConfirm = {
                action()
                showConfirmation = null
            },
            onDismiss = { showConfirmation = null }
        )
    }
}

@Composable
fun DialogOption(
    icon: Any,
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (icon) {
            is Int -> Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            is ImageVector -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
