package com.senghol.knotx.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.senghol.knotx.utils.enums.SandeshType
import com.senghol.knotx.R
import com.senghol.knotx.editor.CropState
import com.senghol.knotx.editor.EditorCropScreen
import com.senghol.knotx.editor.deviceWallpaperRatio
import com.senghol.knotx.editor.loadOrientedBitmap
import com.senghol.knotx.editor.renderCrop
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.objects.DLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsModal(
    onDismiss: () -> Unit,
    userNumber: String,
    wallpaperUri: String?,
    bakaya: Double? = 0.0,
    userName: String,
    about: String?,
    onMediaSelected: (SandeshType) -> Unit,
    onWallpaperSelected: (Uri) -> Unit,
    onRemoveWallpaper: () -> Unit,
    onAppearanceClick: () -> Unit,
    onRefreshBalance: () -> Unit,
    revealSender: Boolean,
    onRevealPhoneNumber: () -> Unit,
    autoDisappearMsg: Int,
    onDisappearingDurationSelected: (Int) -> Unit,
    onHideChatWithPasskey: (passkey: String, isMuted: Boolean) -> Unit,
    dootawasColors: DootawasColors
) {
    DLog.i("USER SETTINGS MODAL -> number='$userNumber', name='$userName', about='$about'")
    var resultText by remember { mutableStateOf<String?>(null) }
    var showDeleteWallpaperDialog by remember { mutableStateOf(false) }
    var showRevealConfirmDialog by remember { mutableStateOf(false) }
    var showDisappearingPicker by remember { mutableStateOf(false) }

    var showHideChatDialog by remember { mutableStateOf(false) }
    var passkeyInput by remember { mutableStateOf("") }
    var confirmPasskeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    val savedToContactsText = stringResource(R.string.saved_to_contacts)
    val cancelledText = stringResource(R.string.cancelled)
    val unknownResultText = stringResource(R.string.unknown_result)

    val globalTheme =
        LocalDootawasTheme.current
    val contactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        resultText = when (result.resultCode) {
            Activity.RESULT_OK -> savedToContactsText
            Activity.RESULT_CANCELED -> cancelledText
            else -> unknownResultText
        }
    }

    val mediaItems = listOf(
        stringResource(R.string.media_photo) to SandeshType.IMAGE,
        stringResource(R.string.media_video) to SandeshType.VIDEO,
        stringResource(R.string.media_audio) to SandeshType.AUDIO,
        stringResource(R.string.media_document) to SandeshType.DOCUMENT,
        stringResource(R.string.media_contact) to SandeshType.CONTACT,
        stringResource(R.string.media_location) to SandeshType.LOCATION
    )

    val rootView = LocalView.current
    val wallpaperRatio = remember { deviceWallpaperRatio(context, rootView) }
    val scope = rememberCoroutineScope()
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedUri = uri }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        sourceBitmap = withContext(Dispatchers.IO) { loadOrientedBitmap(context, uri, 2048) }
    }

    fun closeCrop() {
        sourceBitmap = null
        pickedUri = null
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blockSheetDrag = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                available
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle()},
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = globalTheme.colors.chatScreenBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(blockSheetDrag)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            sourceBitmap?.let { bmp ->
                Dialog(
                    onDismissRequest = { closeCrop() },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        EditorCropScreen(
                            source = bmp,
                            initial = CropState(),
                            lockedRatio = wallpaperRatio,
                            onClose = { closeCrop() },
                            onApply = { state ->
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        val out = renderCrop(bmp, state)
                                        File(
                                            context.filesDir,
                                            "wallpaper_${System.currentTimeMillis()}.jpg"
                                        ).also { f ->
                                            FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                        }
                                    }
                                    onWallpaperSelected(Uri.fromFile(file))
                                    closeCrop()
                                }
                            }
                        )
                    }
                }
            }

            // Modal Title / Header
            Text(
                text = stringResource(R.string.user_settings_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Details & Balance Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = globalTheme.colors.userHeaderBackground,
                shadowElevation = 1.dp, // Adds a physical drop shadow beneath the surface
                tonalElevation = 1.dp,  // Optional: applies a subtle surface color overlay
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userName.isEmpty()) stringResource(R.string.name_not_set) else stringResource(R.string.name_label, userName),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.phone_label, userNumber),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        val currentBakaya = bakaya ?: 0.0

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentBakaya != 0.0) {
                                    Text(
                                        text = "₹%.2f".format(currentBakaya),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentBakaya > 0) Color(0xFF4CAF50) else Color(
                                                0xFFE53935
                                            )
                                        )
                                    )
                                }

                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = onRefreshBalance,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.refresh_balance),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                    }

                    if (!about.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = about,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (userName.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                    type = ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(ContactsContract.Intents.Insert.PHONE, userNumber)
                                }
                                contactLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save_to_contacts))
                        }
                    }

                    resultText?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Privacy & Security Section Header
            Text(
                text = stringResource(R.string.privacy_security_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = globalTheme.colors.userHeaderBackground,
                shadowElevation = 1.dp, // Adds a physical drop shadow beneath the surface
                tonalElevation = 1.dp,  // Optional: applies a subtle surface color overlay
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // E2E Encryption Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.e2e_encryption_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = stringResource(R.string.e2e_always_enabled),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(checked = true, onCheckedChange = null, enabled = false)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Reveal Phone Number Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reveal_phone_number_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (revealSender) stringResource(R.string.reveal_phone_number_on_description)
                                else stringResource(R.string.reveal_phone_number_off_description),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Switch(
                            checked = revealSender,
                            onCheckedChange = { checked ->
                                if (checked && !revealSender) {
                                    showRevealConfirmDialog = true
                                }
                            },
                            enabled = !revealSender
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Disappearing Messages Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDisappearingPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.disappearing_messages_row_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = formatDisappearingDuration(autoDisappearMsg),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Hide Chat Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHideChatDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.hide_chat_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = stringResource(R.string.hide_chat_subtitle),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Chat Settings Section Header
            Text(
                text = stringResource(R.string.chat_settings_title),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = globalTheme.colors.userHeaderBackground,
                shadowElevation = 1.dp, // Adds a physical drop shadow beneath the surface
                tonalElevation = 1.dp,  // Optional: applies a subtle surface color overlay
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Wallpaper Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.wallpaper_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (wallpaperUri != null) {
                                IconButton(onClick = { showDeleteWallpaperDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_wallpaper),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Appearance Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppearanceClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.appearance_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Shared Media Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.media_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(mediaItems) { (label, type) ->
                        MediaButton(
                            label = label,
                            type = type,
                            onClick = { onMediaSelected(type) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Delete Wallpaper Dialog
    if (showDeleteWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWallpaperDialog = false },
            title = { Text(stringResource(R.string.remove_wallpaper_title)) },
            text = { Text(stringResource(R.string.remove_wallpaper_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteWallpaperDialog = false
                    onRemoveWallpaper()
                }) {
                    Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWallpaperDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Reveal Confirm Dialog
    if (showRevealConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRevealConfirmDialog = false },
            title = { Text(stringResource(R.string.reveal_confirm_title)) },
            text = { Text(stringResource(R.string.reveal_confirm_description)) },
            confirmButton = {
                TextButton(onClick = {
                    showRevealConfirmDialog = false
                    onRevealPhoneNumber()
                }) {
                    Text(stringResource(R.string.reveal), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevealConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Disappearing Messages Picker
    if (showDisappearingPicker) {
        DisappearingMessagesPickerDialog(
            currentSeconds = autoDisappearMsg,
            onDismiss = { showDisappearingPicker = false },
            onSelected = { seconds ->
                showDisappearingPicker = false
                onDisappearingDurationSelected(seconds)
            }
        )
    }

    // Hide Chat / Passkey Dialog
    if (showHideChatDialog) {
        var isMuted by remember { mutableStateOf(false) }
        var passkeyVisible by remember { mutableStateOf(false) }
        var confirmPasskeyVisible by remember { mutableStateOf(false) }

        val passwordsMatch = passkeyInput.isNotBlank() && passkeyInput == confirmPasskeyInput
        val hasMismatchError = confirmPasskeyInput.isNotBlank() && !passwordsMatch

        AlertDialog(
            onDismissRequest = {
                showHideChatDialog = false
                passkeyInput = ""
                confirmPasskeyInput = ""
                isMuted = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.hide_chat_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.hide_chat_dialog_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Passkey Field
                    OutlinedTextField(
                        value = passkeyInput,
                        onValueChange = { passkeyInput = it },
                        label = { Text(stringResource(R.string.passkey_label)) },
                        singleLine = true,
                        isError = hasMismatchError,
                        visualTransformation = if (passkeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passkeyVisible = !passkeyVisible }) {
                                Icon(
                                    imageVector = if (passkeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passkeyVisible) "Hide passkey" else "Show passkey"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Confirm Passkey Field
                    OutlinedTextField(
                        value = confirmPasskeyInput,
                        onValueChange = { confirmPasskeyInput = it },
                        label = { Text(stringResource(R.string.confirm_passkey_label)) },
                        singleLine = true,
                        isError = hasMismatchError,
                        visualTransformation = if (confirmPasskeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasskeyVisible = !confirmPasskeyVisible }) {
                                Icon(
                                    imageVector = if (confirmPasskeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasskeyVisible) "Hide passkey" else "Show passkey"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (hasMismatchError) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.passkeys_do_not_match),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Mute Notifications Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isMuted = !isMuted }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isMuted,
                            onCheckedChange = { isMuted = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mute notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Silence alerts from this chat while hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Optional "Remove Key" Action
                    TextButton(
                        onClick = {
                            onHideChatWithPasskey("", false)
                            showHideChatDialog = false
                            passkeyInput = ""
                            confirmPasskeyInput = ""
                            isMuted = false
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.remove_hide_key),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordsMatch) {
                            onHideChatWithPasskey(passkeyInput, isMuted)
                            showHideChatDialog = false
                            passkeyInput = ""
                            confirmPasskeyInput = ""
                            isMuted = false
                        }
                    },
                    enabled = passwordsMatch
                ) {
                    Text(stringResource(R.string.hide_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showHideChatDialog = false
                        passkeyInput = ""
                        confirmPasskeyInput = ""
                        isMuted = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}




@Composable
fun DisappearingMessagesPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    var selectedDays by remember { mutableIntStateOf(currentSeconds / 86400) }
    var selectedHours by remember { mutableIntStateOf((currentSeconds % 86400) / 3600) }
    var selectedMinutes by remember { mutableIntStateOf((currentSeconds % 3600) / 60) }
    var selectedSecs by remember { mutableIntStateOf(currentSeconds % 60) }

    val presets = listOf(
        stringResource(R.string.duration_preset_10s) to 10,
        stringResource(R.string.duration_preset_30s) to 30,
        stringResource(R.string.duration_preset_1m) to 60,
        stringResource(R.string.duration_preset_5m) to 300,
        stringResource(R.string.duration_preset_1h) to 3600,
        stringResource(R.string.duration_preset_24h) to 86400,
        stringResource(R.string.duration_preset_7d) to 604800
    )

    val daysLabel = stringResource(R.string.duration_picker_days_label)
    val hoursLabel = stringResource(R.string.duration_picker_hours_label)
    val minutesLabel = stringResource(R.string.duration_picker_minutes_label)
    val secondsLabel = stringResource(R.string.duration_picker_seconds_label)

    // Custom solid dark UI colors (Independent of Material theme system)
    val dialogBg = Color(0xFF141414)
    val cardBg = Color(0xFF1E1E1E)
    val activeBoxBg = Color(0xFF484848)
    val accentCyan = Color(0xFFCEC0A4)
    val textWhite = Color(0xFFFFFFFF)
    val textGray = Color(0xFFA0A0A0)
    val strokeColor = Color(0xFF333333)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = dialogBg,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.duration_picker_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.duration_picker_subtitle),
                        fontSize = 14.sp,
                        color = textGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, strokeColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmoothSwipeColumn(
                                label = daysLabel,
                                value = selectedDays,
                                range = 0..7,
                                accentColor = accentCyan,
                                textColor = textWhite,
                                mutedColor = textGray,
                                activeBg = activeBoxBg
                            ) { selectedDays = it }

                            SmoothSwipeColumn(
                                label = hoursLabel,
                                value = selectedHours,
                                range = 0..23,
                                accentColor = accentCyan,
                                textColor = textWhite,
                                mutedColor = textGray,
                                activeBg = activeBoxBg
                            ) { selectedHours = it }

                            SmoothSwipeColumn(
                                label = minutesLabel,
                                value = selectedMinutes,
                                range = 0..59,
                                accentColor = accentCyan,
                                textColor = textWhite,
                                mutedColor = textGray,
                                activeBg = activeBoxBg
                            ) { selectedMinutes = it }

                            SmoothSwipeColumn(
                                label = secondsLabel,
                                value = selectedSecs,
                                range = 0..59,
                                accentColor = accentCyan,
                                textColor = textWhite,
                                mutedColor = textGray,
                                activeBg = activeBoxBg
                            ) { selectedSecs = it }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, start = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.duration_picker_quick_presets_label),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCyan
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { preset ->
                            val currentTotal = (selectedDays * 86400) + (selectedHours * 3600) + (selectedMinutes * 60) + selectedSecs
                            val isSelected = currentTotal == preset.second

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        val total = preset.second
                                        selectedDays = total / 86400
                                        selectedHours = (total % 86400) / 3600
                                        selectedMinutes = (total % 3600) / 60
                                        selectedSecs = total % 60
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accentCyan else cardBg,
                                border = BorderStroke(1.dp, if (isSelected) accentCyan else strokeColor)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else textWhite,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, strokeColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textWhite)
                        ) {
                            Text(
                                stringResource(R.string.cancel),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val totalSeconds = (selectedDays * 86400) +
                                        (selectedHours * 3600) +
                                        (selectedMinutes * 60) +
                                        selectedSecs
                                onSelected(totalSeconds)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                stringResource(R.string.duration_picker_save_button),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmoothSwipeColumn(
    label: String,
    value: Int,
    range: IntRange,
    accentColor: Color,
    textColor: Color,
    mutedColor: Color,
    activeBg: Color,
    onValueChanged: (Int) -> Unit
) {
    val currentValue = rememberUpdatedState(value)
    var totalDragPx by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableIntStateOf(value) }

    val stepHeightPx = 40f
    val rangeSize = range.last - range.first + 1

    val prevValue = if (value - 1 < range.first) range.last else value - 1
    val nextValue = if (value + 1 > range.last) range.first else value + 1

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(66.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDragPx = 0f
                        dragStartValue = currentValue.value
                    },
                    onDragEnd = { totalDragPx = 0f },
                    onDragCancel = { totalDragPx = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    totalDragPx += dragAmount

                    val steps = (totalDragPx / stepHeightPx).toInt()

                    var newValue = (dragStartValue - steps - range.first) % rangeSize
                    if (newValue < 0) newValue += rangeSize
                    newValue += range.first

                    if (newValue != value) {
                        onValueChanged(newValue)
                    }
                }
            }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = mutedColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = String.format("%02d", prevValue),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = mutedColor.copy(alpha = 0.35f),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onValueChanged(prevValue) }
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(activeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = String.format("%02d", nextValue),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = mutedColor.copy(alpha = 0.35f),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onValueChanged(nextValue) }
        )
    }
}

/**
 * Interactive Column supporting **True Vertical Drag/Slide up and down**
 * Swipe UP increases the value; Swipe DOWN decreases the value smoothly.
 */
/** Formats a disappearing-message duration (seconds, 0 = off) for display. */
@Composable
fun formatDisappearingDuration(seconds: Int): String {
    if (seconds <= 0) return stringResource(R.string.disappearing_off)

    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    val parts = mutableListOf<String>()
    if (days > 0) parts.add(pluralStringResource(R.plurals.disappearing_days, days, days))
    if (hours > 0) parts.add(pluralStringResource(R.plurals.disappearing_hours, hours, hours))
    if (minutes > 0) parts.add(pluralStringResource(R.plurals.disappearing_minutes, minutes, minutes))
    if (secs > 0) parts.add(pluralStringResource(R.plurals.disappearing_seconds, secs, secs))

    return parts.joinToString(" ")
}