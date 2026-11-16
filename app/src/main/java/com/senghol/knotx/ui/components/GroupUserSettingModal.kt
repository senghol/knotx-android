package com.senghol.knotx.ui.components


import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.senghol.knotx.R
import com.senghol.knotx.editor.CropState
import com.senghol.knotx.editor.EditorCropScreen
import com.senghol.knotx.editor.deviceWallpaperRatio
import com.senghol.knotx.editor.loadOrientedBitmap
import com.senghol.knotx.editor.renderCrop
import com.senghol.knotx.ui.dialogs.AppDialogs.DeleteProfileDialog
import com.senghol.knotx.ui.dialogs.ConfirmationDialogA
import com.senghol.knotx.utils.data.GroupUserProfile
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ConfirmRemoveDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    memberName: String
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Remove Member", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to remove $memberName from the group? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Yes, Remove", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupUserSettingsModal(
    isGroupActive: Boolean,
    onDismiss: () -> Unit,
    userNumber: String,
    members: List<GroupUserProfile>,
    onMediaSelected: (SandeshType) -> Unit,
    onAddMembersClicked: () -> Unit,
    onPermissionChanged: (GroupUserProfile, Map<String, Boolean>) -> Unit,
    onRemoveMember: (GroupUserProfile) -> Unit,
    onProfileImageUpdated: (String) -> Unit,
    onLeaveGroup:()-> Unit,
    onDeleteGroup:()-> Unit,
    onDeleteGroupProfile:()-> Unit,
    wallpaperUri: String?,
    onWallpaperSelected: (Uri) -> Unit,
    onRemoveWallpaper: () -> Unit

) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val wallpaperRatio = remember { deviceWallpaperRatio(context, rootView) }
    val currentUser = members.find { it.userNumber == userNumber }
    val currentUserIsSwami = currentUser?.isSwami ?: false
    val currentUserCanManageMembers = currentUserIsSwami || (currentUser?.canModifyMembers == true)
    // ? Determine if Update Profile Picture should be visible
    val canUpdateProfilePic = (currentUser?.canUpdateProfile == true) || currentUserIsSwami
    var showConfirmation by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isWallpaperCrop by remember {
        mutableStateOf(false)
    }
    var showDeleteWallpaperDialog by remember {
        mutableStateOf(false)
    }

    val onlySwamiText = stringResource(R.string.only_swami_can_change_permissions)
    val msgPermissionText = stringResource(R.string.permission_message)
    val callPermissionText = stringResource(R.string.permission_call)
    val addMembersPermissionText = stringResource(R.string.permission_add_members)
    val updateProfilePermissionText = stringResource(R.string.permission_update_profile)
// ? Launcher for crop result
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
        isWallpaperCrop = false
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxHeight(1.0f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
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
                            squareOnly = !isWallpaperCrop,                              // profile = 1:1
                            lockedRatio = if (isWallpaperCrop) wallpaperRatio else null, // wallpaper = device ratio
                            onClose = { closeCrop() },
                            onApply = { state ->
                                val forWallpaper = isWallpaperCrop
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        val out = renderCrop(bmp, state)
                                        val name = if (forWallpaper) "wallpaper" else "group_profile"
                                        File(
                                            context.filesDir,
                                            "${name}_${System.currentTimeMillis()}.jpg"
                                        ).also { f ->
                                            FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                        }
                                    }
                                    if (forWallpaper) {
                                        onWallpaperSelected(Uri.fromFile(file))
                                    } else {
                                        onProfileImageUpdated(Uri.fromFile(file).toString())
                                    }
                                    closeCrop()
                                }
                            }
                        )
                    }
                }
            }

            Text(stringResource(R.string.group_settings_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)

            Spacer(Modifier.height(16.dp))


            // ? Show only if user has permission
            if (isGroupActive && canUpdateProfilePic) {

                // ? Update Profile Picture Section
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side: Title
                        Column {
                            Text(
                                stringResource(R.string.profile_picture_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        // Right side: Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            // Edit icon
                            IconButton(onClick = {
                                isWallpaperCrop = false
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_profile_picture_desc),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = {
                                showDeleteDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_profile_picture_desc),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {

                        isWallpaperCrop = true
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = stringResource(R.string.wallpaper_title),
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (wallpaperUri != null) {

                        IconButton(
                            onClick = {
                                showDeleteWallpaperDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.delete_wallpaper),
                                tint = Color.Red
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            if (isGroupActive && members.isNotEmpty()) {
                Text(stringResource(R.string.members_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)

                members.forEach { member ->
                    var expanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    buildAnnotatedString {

                                        if (member.userName.isNotBlank()) {

                                            // ? Name (bold)
                                            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                append(member.userName)
                                            }

                                            append(" • ")

                                            // ? Number (small + faded)
                                            withStyle(
                                                style = SpanStyle(
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                append(member.userNumber)
                                            }

                                        } else {

                                            // ? Number (bold if no name)
                                            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                append(member.userNumber)
                                            }
                                        }
                                    }
                                )
                                if (member.isSwami) {
                                    Text(stringResource(R.string.swami_label), fontSize = 12.sp)
                                }
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (expanded) stringResource(R.string.collapse_desc)
                                else stringResource(R.string.expand_desc)
                            )
                        }


                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Text(stringResource(R.string.permissions_title), fontSize = 12.sp, color = Color(0xFF040821))
                                Spacer(Modifier.height(4.dp))

                                var localPermissions by remember(member) {
                                    mutableStateOf(
                                        mapOf(
                                            "canMessage" to member.canMessage,
                                            "canUpdateProfile" to member.canUpdateProfile,
                                            "canModifyMembers" to member.canModifyMembers,
                                            "canStartCall" to member.canStartCall
                                        )
                                    )
                                }
                                var hasChanges by remember { mutableStateOf(false) }

                                // Reset permissions whenever member data updates from DB
                                LaunchedEffect(member) {
                                    localPermissions = mapOf(
                                        "canMessage" to member.canMessage,
                                        "canUpdateProfile" to member.canUpdateProfile,
                                        "canModifyMembers" to member.canModifyMembers,
                                        "canStartCall" to member.canStartCall
                                    )
                                    hasChanges = false
                                }

                                // Helper function to compare current toggles with DB
                                fun hasPermissionChanged(local: Map<String, Boolean>, m: GroupUserProfile): Boolean {
                                    return local["canMessage"] != m.canMessage ||
                                            local["canUpdateProfile"] != m.canUpdateProfile ||
                                            local["canModifyMembers"] != m.canModifyMembers ||
                                            local["canStartCall"] != m.canStartCall
                                }

                                val isSwamiSelf = currentUserIsSwami && member.userNumber == userNumber

                                Row {
                                    Column(modifier = Modifier.weight(1f)) {
                                        PermissionToggle(
                                            msgPermissionText,
                                            localPermissions["canMessage"] ?: false
                                        ) { newValue ->
                                            if (currentUserIsSwami && !isSwamiSelf) {
                                                localPermissions = localPermissions.toMutableMap()
                                                    .also { it["canMessage"] = newValue }
                                                hasChanges = hasPermissionChanged(localPermissions, member)
                                            } else {
                                                Toast.makeText(context, onlySwamiText, Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        PermissionToggle(
                                            callPermissionText,
                                            localPermissions["canStartCall"] ?: false
                                        ) { newValue ->
                                            if (currentUserIsSwami && !isSwamiSelf) {
                                                localPermissions = localPermissions.toMutableMap()
                                                    .also { it["canStartCall"] = newValue }
                                                hasChanges = hasPermissionChanged(localPermissions, member)
                                            } else {
                                                Toast.makeText(context, onlySwamiText, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(32.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        PermissionToggle(
                                            addMembersPermissionText,
                                            localPermissions["canModifyMembers"] ?: false
                                        ) { newValue ->
                                            if (currentUserIsSwami && !isSwamiSelf) {
                                                localPermissions = localPermissions.toMutableMap()
                                                    .also { it["canModifyMembers"] = newValue }
                                                hasChanges = hasPermissionChanged(localPermissions, member)
                                            } else {
                                                Toast.makeText(context, onlySwamiText, Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        PermissionToggle(
                                            updateProfilePermissionText,
                                            localPermissions["canUpdateProfile"] ?: false
                                        ) { newValue ->
                                            if (currentUserIsSwami && !isSwamiSelf) {
                                                localPermissions = localPermissions.toMutableMap()
                                                    .also { it["canUpdateProfile"] = newValue }
                                                hasChanges = hasPermissionChanged(localPermissions, member)
                                            } else {
                                                Toast.makeText(context, onlySwamiText, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                if (hasChanges) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onPermissionChanged(member, localPermissions)

                                            hasChanges = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(stringResource(R.string.update_permissions_button))
                                    }
                                }

                                if (isGroupActive && currentUserCanManageMembers) {
                                    Spacer(Modifier.height(8.dp))

                                    if (member.userNumber != userNumber) {
                                        var showRemoveDialog by remember { mutableStateOf(false) }
                                        Button(
                                            onClick = { showRemoveDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.remove_member_button), color = Color.White)
                                        }

                                        ConfirmRemoveDialog(
                                            showDialog = showRemoveDialog,
                                            onDismiss = { showRemoveDialog = false },
                                            onConfirm = {
                                                onRemoveMember(member)
                                                expanded = false
                                            },
                                            memberName = member.userNumber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (isGroupActive) {

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // ? Add Members (Left)
                    if (currentUserCanManageMembers) {
                        Button(
                            onClick = onAddMembersClicked,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.add_members_button),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // ? Delete / Leave (Right)
                    Button(
                        onClick = {
                            showConfirmation = {
                                if (currentUserIsSwami){
                                    onDeleteGroup()
                                } else {
                                    onLeaveGroup()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(
                            if (currentUserIsSwami) stringResource(R.string.delete_group_button) else stringResource(R.string.leave_group_button),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 4.dp)
            ) {
                // Title with modern typography
                Text(
                    text = stringResource(R.string.media_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

// 1. Define your strings at the top of the Composable function
                val photoLabel = stringResource(R.string.media_photo)
                val videoLabel = stringResource(R.string.media_video)
                val audioLabel = stringResource(R.string.media_audio)
                val documentLabel = stringResource(R.string.media_document)
                val contactLabel = stringResource(R.string.media_contact)
                val locationLabel = stringResource(R.string.media_location)

// 2. Use them safely inside your LazyRow list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(
                        listOf(
                            photoLabel to SandeshType.IMAGE,
                            videoLabel to SandeshType.VIDEO,
                            audioLabel to SandeshType.AUDIO,
                            documentLabel to SandeshType.DOCUMENT,
                            contactLabel to SandeshType.CONTACT,
                            locationLabel to SandeshType.LOCATION
                        )
                    ) { (label, type) ->
                        MediaButton(
                            label = label,
                            type = type,
                            onClick = { onMediaSelected(type) }

                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showDeleteWallpaperDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteWallpaperDialog = false
            },

            title = {
                Text(stringResource(R.string.remove_wallpaper_title))
            },

            text = {
                Text(stringResource(R.string.remove_wallpaper_confirmation))
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showDeleteWallpaperDialog = false

                        onRemoveWallpaper()
                    }
                ) {
                    Text(stringResource(R.string.remove), color = Color.Red)
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteWallpaperDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteProfileDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteGroupProfile() // ? actual delete call
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
    showConfirmation?.let { action ->
        ConfirmationDialogA(
            message = stringResource(R.string.are_you_sure_message),
            onConfirm = {
                action()
                showConfirmation = null
            },
            onDismiss = { showConfirmation = null }
        )
    }

}

@Composable
fun MediaButton(
    label: String,
    type: SandeshType,
    onClick: () -> Unit,
) {
    // Card with elevation and Material3 ripple
    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true) // Updated to Material3 ripple
            ) { onClick() },
        shape = MaterialTheme.shapes.medium, // Material3 shape
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon for each media type
            Icon(
                imageVector = when (type) {
                    SandeshType.IMAGE -> Icons.Default.Image
                    SandeshType.VIDEO -> Icons.Default.Videocam
                    SandeshType.AUDIO -> Icons.Default.Mic
                    SandeshType.DOCUMENT -> Icons.Default.Description
                    SandeshType.CONTACT -> Icons.Default.Person
                    SandeshType.LOCATION -> Icons.Default.LocationOn
                    else -> Icons.AutoMirrored.Filled.HelpOutline

                },
                contentDescription = "$label icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Label text
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
