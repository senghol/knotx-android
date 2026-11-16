package com.senghol.knotx.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.senghol.knotx.R
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.ui.components.AudioRecorderScreen
import com.senghol.knotx.ui.components.CameraScreen
import com.senghol.knotx.ui.components.PermissionHandler
import com.senghol.knotx.ui.dialogs.ConfirmationDialog
import com.senghol.knotx.ui.dialogs.ImagePreviewScreen
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.res.Configuration
import android.text.SpannableString
import android.text.util.Linkify
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.google.gson.JsonArray
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.senghol.knotx.editor.ImageEditorScreen
import com.senghol.knotx.editor.VideoEditorScreen

import com.senghol.knotx.ui.components.AddMemberContactPicker
import com.senghol.knotx.ui.components.ContactPickerWithHomeScreen
import com.senghol.knotx.ui.components.EmojiPickerBottomSheet
import com.senghol.knotx.ui.components.EmojiReactionItem

import com.senghol.knotx.ui.components.GroupSearchBox
import com.senghol.knotx.ui.components.GroupUserSettingsModal
import com.senghol.knotx.ui.components.GroupMessageInfoDialog
import com.senghol.knotx.ui.components.GroupMessageInputBar
import com.senghol.knotx.ui.components.HorizontalAttachmentMenu
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.ListMessage
import com.senghol.knotx.ui.components.ListMessageType
import com.senghol.knotx.ui.components.ListScreen
import com.senghol.knotx.ui.components.LiveLocationScreen
import com.senghol.knotx.ui.components.MessageSelectionBar
import com.senghol.knotx.ui.components.ModernMapPicker
import com.senghol.knotx.ui.components.UserGroupHeaderPortrait
import com.senghol.knotx.ui.components.UserHeaderLandscape
import com.senghol.knotx.ui.components.VideoGalleryScreen
import com.senghol.knotx.ui.components.getAudioDurationMs
import com.senghol.knotx.ui.components.group.GroupAudioBubble
import com.senghol.knotx.ui.components.group.GroupContactBubble
import com.senghol.knotx.ui.components.group.GroupDateHeader
import com.senghol.knotx.ui.components.group.GroupDocumentBubble
import com.senghol.knotx.ui.components.group.GroupImageBubble
import com.senghol.knotx.ui.components.group.GroupListBubble
import com.senghol.knotx.ui.components.group.GroupLocationBubble
import com.senghol.knotx.ui.components.group.GroupMessageBubble
import com.senghol.knotx.ui.components.group.GroupVideoBubble
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import com.senghol.knotx.utils.data.GroupParsedAudioSandesh
import com.senghol.knotx.utils.data.GroupParsedContactSandesh
import com.senghol.knotx.utils.data.GroupParsedDateHeaderSandesh
import com.senghol.knotx.utils.data.GroupParsedDocumentSandesh
import com.senghol.knotx.utils.data.GroupParsedImageSandesh
import com.senghol.knotx.utils.data.GroupParsedListSandesh
import com.senghol.knotx.utils.data.GroupParsedLocationSandesh
import com.senghol.knotx.utils.data.GroupParsedTextSandesh
import com.senghol.knotx.utils.data.GroupParsedUniversalSandesh
import com.senghol.knotx.utils.data.GroupParsedVideoSandesh
import com.senghol.knotx.utils.data.SandeshStatusInfo
import com.senghol.knotx.utils.enums.GroupSandeshType
import com.senghol.knotx.utils.enums.JsonKeys
import com.senghol.knotx.utils.enums.SandeshStithi
import kotlinx.coroutines.FlowPreview
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(
    navController: NavController,
    groupViewModel: SandeshViewModel,
) {


    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val itemWidthDp = with(density) { 28.sp.toDp() + 8.dp * 2 }
    val maxVisibleEmojis = remember(screenWidthDp) {
        (screenWidthDp / itemWidthDp).toInt().coerceAtLeast(6)
    }
    val quickReactions = remember {
        listOf("😍", "❤️", "😂", "😢", "😡", "👍", "👎", "🔥", "🎉", "🚀")
    }


    val theme = LocalDootawasTheme.current
    val dootawasColors = theme.groupColors
    val fontSize by groupViewModel.fontSizeFlow.collectAsState()
    val enterToSendEnabled by groupViewModel.chatBehaviourFlow.collectAsState()

    val groupEntity by groupViewModel.groupEntity.collectAsState()
    val userName = groupEntity?.groupName ?: ""
    val groupId = groupEntity?.groupId ?: ""
    val photoUrl = groupEntity?.groupImageUrl
    val wallpaperUrl = groupEntity?.wallpaperImage
    val isGroupActive = groupEntity?.isGroupActive

    val context = LocalContext.current
    val selectedMessages = remember { mutableStateSetOf<String>() }
    val parsedList by groupViewModel.groupParsedList.collectAsStateWithLifecycle()
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var replyMessage by remember { mutableStateOf(false) }
    var hideOnSend by remember { mutableStateOf(false) }
    val singleSelectedSandesh by groupViewModel.groupSelectedSandesh.collectAsState()
    var isSelectionMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }
    var mimeType by remember { mutableStateOf<String?>(null) }
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    var selectedDocumentUri by remember { mutableStateOf<Uri?>(null) }

    val selectedVideoUris = remember { mutableStateListOf<Uri>() }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewIndex by remember { mutableIntStateOf(0) }
    var showCamera by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val borderMode = theme.groupBorderMode
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedStatusInfo by remember { mutableStateOf<SandeshStatusInfo?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showLocationScreen by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()


    val firstVisibleIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    val overlayDate by remember {
        derivedStateOf {
            parsedList
                .getOrNull(firstVisibleIndex)
                ?.formattedDate
        }
    }

    val showOverlay by remember {
        derivedStateOf {
            firstVisibleIndex != 0
        }
    }
    var showUserSettings by remember { mutableStateOf(false) }
    var activeListForEdit by remember { mutableStateOf<Pair<String, ListData?>?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val members by groupViewModel.groupMembers.collectAsState()
    var showForward by remember { mutableStateOf(false) }
    val validUserName = userName.takeIf {
        it.isNotBlank() && it.lowercase() != "null"
    }
    var showVideoPreview by remember { mutableStateOf(false) }
    var videoPreviewIndex by remember { mutableIntStateOf(0) }
    var videoPreviewUris by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedImageVisibility by remember {
        mutableStateOf(MediaVisibility.PRIVATE)
    }
    // 👉 Place this new header block HERE
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var liveLocationSandeshId by remember { mutableStateOf<String?>(null) }
    val latestSandeshId by groupViewModel.latestSandeshId.collectAsStateWithLifecycle()
    // Alternative (slightly more precise – only restart when the actual unseen list changes)
    val unseenIds by remember {
        derivedStateOf {
            parsedList
                .filter { it.sandeshStithi != SandeshStithi.DHRIST && !it.isSelf }
                .map { it.sandeshId }
        }
    }

    val currentTime by produceState(
        initialValue = System.currentTimeMillis()
    ) {

        while (true) {

            value = System.currentTimeMillis()

            delay(60_000.milliseconds)
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    val currentMessageText by rememberUpdatedState(messageText)

// 1. Read receipts: only while the screen is resumed
    LaunchedEffect(unseenIds, lifecycleOwner) {
        if (unseenIds.isEmpty()) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            groupViewModel.processGroupDhrist()
        }
    }

// 2. Auto-scroll to the newest message, but not if the user is reading older ones
    LaunchedEffect(latestSandeshId) {
        if (latestSandeshId != null && listState.firstVisibleItemIndex <= 3) {
            listState.animateScrollToItem(0)
        }
    }

// 3. Active group + notifications + draft saving
//    Runs on first open and on every screen-on; cleans up on screen-off or leaving
    LifecycleStartEffect(groupId) {
        if (groupId.isNotBlank()) {
            groupViewModel.setAsActiveUser(groupId)
            groupViewModel.cleanNotification()
        }

        onStopOrDispose {
            groupViewModel.saveDraft(draft = currentMessageText.trim())  // saves "" when empty, clearing the draft
            groupViewModel.clearActiveUser()   // only clears if it is still this group
            groupViewModel.resetUnread()
        }
    }

// 4. Restore the draft once, when the group entity has actually loaded
    var draftRestored by rememberSaveable(groupId) { mutableStateOf(false) }

    LaunchedEffect(groupEntity) {
        val entity = groupEntity ?: return@LaunchedEffect
        if (!draftRestored) {
            val draft = entity.draftMessage
            if (!draft.isNullOrEmpty() && messageText.isEmpty()) {
                messageText = draft
            }
            draftRestored = true
        }
    }

// 5. Players (see the note below)
    LaunchedEffect(Unit) {
        groupViewModel.initPlayer(context)
        groupViewModel.initVideoPreviewPlayer(context)
    }

// 6. Visibility check when the screen first appears
    LaunchedEffect(groupId) {
        delay(500.milliseconds)   // let members load
        groupViewModel.onCheckVisibilityRequested()
    }

    //todo why this global scroll is not used its user in sandesh screen
    fun globalScroll() {
        coroutineScope.launch {
            if (parsedList.isNotEmpty()) {
                delay(150)
                
//                                            listState.animateScrollToItem(0) // Scroll to the first message

                val visibleItem =
                    listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }

                if (visibleItem != null) {
                    // Already visible — scroll by an offset to create smooth effect
                    val viewportHeight =
                        listState.layoutInfo.viewportEndOffset
                    val itemCenter =
                        visibleItem.offset + visibleItem.size / 2
                    val scrollBy = itemCenter - viewportHeight / 2

                    if (scrollBy != 0) {
                        listState.animateScrollBy(scrollBy.toFloat())
                    }
                } else {
                    // Not visible — scroll normally
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    val onlyOneMember by groupViewModel.onlyOneMember.collectAsState()

    fun sendMessage() {
        if (singleSelectedSandesh != null) {
            // Parse samagri as JSON
            val selectedSamagriJson = Gson().fromJson(
                singleSelectedSandesh!!.samagri,
                JsonObject::class.java
            )

            // Remove nested "originalMessage" if exists
            selectedSamagriJson.remove(DootawasMessage.ORIGINAL_MESSAGE)

            // Create a new reply JSON
            val replyJson = JsonObject().apply {
                addProperty(
                    DootawasMessage.ORIGINAL_SAMAGRI,
                    selectedSamagriJson.toString()
                ) // Cleaned original message
                addProperty(
                    DootawasMessage.ORIGINAL_MESSAGE_ID,
                    singleSelectedSandesh!!.sandeshId
                )  // Original message ID
                addProperty(
                    DootawasMessage.ORIGINAL_MESSAGE_TYPE,
                    singleSelectedSandesh!!.sandeshType
                )  // Original message type
            }

            // Create new message JSON
            // Normal message
            val spannable = SpannableString(messageText)
            val hasUrl =
                LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)

            val phoneUtil = PhoneNumberUtil.getInstance()
            val hasPhone =
                phoneUtil.findNumbers(messageText, "IN").iterator()
                    .hasNext()

            val isLink = hasUrl || hasPhone

            val messageSamagri = JsonObject().apply {
                addProperty(DootawasMessage.PAATH, messageText)
                addProperty(DootawasMessage.IS_LINK, isLink)
                add(DootawasMessage.ORIGINAL_MESSAGE, replyJson)
            }

            //todo
            groupViewModel.replyToGroups(
                groupIds = listOf(groupId),
                message = messageSamagri
            )

            // Reset selection
            replyMessage = false
            groupViewModel.clearReply()
        } else {
            val startTime = System.currentTimeMillis()

            // Normal message
            val spannable = SpannableString(messageText)
            val hasUrl =
                LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)

            val phoneUtil = PhoneNumberUtil.getInstance()
            val hasPhone =
                phoneUtil.findNumbers(messageText, "IN").iterator()
                    .hasNext()

            val isLink = hasUrl || hasPhone

            val messageSamagri = JsonObject().apply {
                addProperty(DootawasMessage.PAATH, messageText)
                addProperty(DootawasMessage.IS_LINK, isLink)
            }

            if (onlyOneMember) {
                Toast.makeText(context, "Add Members Before Messaging", Toast.LENGTH_SHORT).show()
                return
            } else {
                groupViewModel.sendMessageToGroups(
                    groupIds = listOf(groupId),
                    message = messageSamagri
                )
            }

            

        }
        messageText = ""
//            globalScroll()
    }

    //todo disable in production
//    LaunchedEffect(Unit) {
//
//        delay(5000) // 🔥 keep this
//
//        repeat(50) { index ->
//
//            val count = index + 1
//            messageText = "test${count.toString().padStart(2, '0')}" // test01, test02...
//
//            sendMessage()
//
//            delay(100) // 🔥 keep this
//        }
//    }


    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument() // ✅ Supports persistable permissions
    ) { uri: Uri? ->
        uri?.let { docUri ->
            selectedDocumentUri = docUri
            

            try {
                context.contentResolver.takePersistableUriPermission(
                    docUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                

                // Get the MIME type
                mimeType = context.contentResolver.getType(docUri) // ✅ Retrieve the MIME type
                

                // Generate preview
                val previewUri = groupViewModel.saveFirstPageAsImage(context, docUri)
                
                val docName = getFileName(context, docUri)
                val savedMedia = saveMediaToAppStorageNext(
                    context,
                    docUri,
                    MediaCategory.DOCUMENT,
                    selectedImageVisibility
                ) ?: return@let



                if (replyMessage) {
                    if (singleSelectedSandesh != null) {
                        val replyJson = JsonObject().apply {
                            addProperty(
                                DootawasMessage.ORIGINAL_SAMAGRI, singleSelectedSandesh!!.samagri
                            )  // Original message text
                            addProperty(
                                DootawasMessage.ORIGINAL_MESSAGE_ID,
                                singleSelectedSandesh!!.sandeshId
                            )  // Original message ID
                            addProperty(
                                DootawasMessage.ORIGINAL_MESSAGE_TYPE,
                                singleSelectedSandesh!!.sandeshType
                            )  // Original message type
                        }
                        val messageSamagri = JsonObject().apply {
                            addProperty(
                                DootawasMessage.ORIGINAL_URI,
                                savedMedia.localPath
                            )

                            addProperty(DootawasMessage.MIME_TYPE, mimeType)
                            addProperty(DootawasMessage.DUPLICATE_URI, previewUri.toString())
                            addProperty(DootawasMessage.FILE_NAME, docName)
                            addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.fileSize)
                            add(
                                DootawasMessage.ORIGINAL_MESSAGE, replyJson
                            )  // Add the replyJson under the "REPLY" key
                        }
                        //todo
                        groupViewModel.replyDocumentToGroups(
                            groupIds = listOf(groupId),
                            message = messageSamagri
                        )
                        replyMessage = false
                        groupViewModel.clearReply()
                    }
                } else {
                    val messageSamagri = JsonObject()
                    messageSamagri.addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                    messageSamagri.addProperty(DootawasMessage.MIME_TYPE, savedMedia.mimeType)
                    messageSamagri.addProperty(DootawasMessage.DUPLICATE_URI, previewUri.toString())
                    messageSamagri.addProperty(DootawasMessage.FILE_NAME, docName)
                    messageSamagri.addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.fileSize)
                    //todo
                    groupViewModel.sendDocumentToGroups(
                        groupIds = listOf(groupId),
                        message = messageSamagri
                    )
                }
                selectedImageUris.clear()
            } catch (e: SecurityException) {
                
            }
        }
    }

    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    fun scrollToMessage(messageId: String) {
        coroutineScope.launch {
            val index = parsedList.indexOfFirst { it.sandeshId == messageId }
            if (index == -1) return@launch

            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@launch

            val firstVisible = visible.first().index
            val lastVisible = visible.last().index
            val jumpGap = 4 // how many items before the target we land, then animate

            // 1. Jump close to the target, on the side we are coming from
            when {
                index < firstVisible - jumpGap -> {
                    // target has a smaller index than what we see -> land just past it
                    listState.scrollToItem((index + jumpGap).coerceAtMost(parsedList.lastIndex))
                }
                index > lastVisible + jumpGap -> {
                    // target has a bigger index than what we see -> land just before it
                    listState.scrollToItem((index - jumpGap).coerceAtLeast(0))
                }
            }

            // 2. ONE animation straight to the centered position
            val layout = listState.layoutInfo
            val viewportHeight = layout.viewportEndOffset - layout.viewportStartOffset
            val avgItemSize = layout.visibleItemsInfo
                .map { it.size }
                .average()
                .toInt()
            val centerOffset = -(viewportHeight - avgItemSize) / 2

            listState.animateScrollToItem(index, centerOffset)

            // 3. Blink highlight
            repeat(2) {
                highlightedMessageId = messageId
                delay(250.milliseconds)
                highlightedMessageId = null
                delay(250.milliseconds)
            }
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    fun scrollToBottom() {
        coroutineScope.launch {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dootawasColors.userHeaderBackground)
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .union(WindowInsets(bottom = 12.dp))        // minimum bottom space
            )
    ) {

        val isDefaultWallpaper = wallpaperUrl.isNullOrEmpty()

        val backgroundPainter = if (isDefaultWallpaper) {
            if (isLandscape) {
                painterResource(id = R.drawable.landscape)
            } else {
                painterResource(id = R.drawable.portrait)
            }
        } else {

            rememberAsyncImagePainter(
                model = wallpaperUrl,
                placeholder = if (isLandscape)
                    painterResource(id = R.drawable.landscape)
                else
                    painterResource(id = R.drawable.portrait)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dootawasColors.chatScreenBackground)
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (isDefaultWallpaper) {
                    ColorFilter.tint(
                        color = dootawasColors.chatScreenWallpaper,
                        blendMode = BlendMode.SrcIn
                    )
                } else {
                    null
                }
            )
        }
        val insets = WindowInsets.navigationBars.asPaddingValues()
        val leftInset = insets.calculateLeftPadding(LayoutDirection.Ltr)
        val rightInset = insets.calculateRightPadding(LayoutDirection.Ltr)

        val navBarOnRight = rightInset > leftInset

        if (isLandscape) {
            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

            val headerHeight by animateDpAsState(if (imeVisible) 80.dp else 295.dp)

            val alignment = if (navBarOnRight) Alignment.TopEnd else Alignment.TopStart

            UserHeaderLandscape(
                photoUrl = photoUrl,
                validUserName = validUserName,
                userNumber = "",
                onShowUserSettings = { showUserSettings = true },
                onToggleSearch = { isSearchMode = true },
                isGroup = true,
                isNavBarOnRight = navBarOnRight,
                modifier = Modifier
                    .align(alignment)
                    .height(headerHeight)
                    .navigationBarsPadding(),
                onBackClick = {
                    navController.popBackStack()
                },
                onShowAiUsage = {},
                userHeaderTextColor = dootawasColors.userHeaderText
            )
        } else {
            UserGroupHeaderPortrait(
                photoUrl = photoUrl,
                validUserName = validUserName,
                userNumber = "",
                onShowUserSettings = { showUserSettings = true },
                onToggleSearch = { isSearchMode = true },
                onBackClick = {
                    navController.popBackStack()
                },
                groupColors = dootawasColors
            )
        }
        val sidePadding =if (isSearchMode) 6.dp else 64.dp

        val columnModifier = Modifier
            .fillMaxSize()
            .imePadding()

        Column(modifier = columnModifier) {


//             🌟 Separate SearchBox below or anywhere you like
            GroupSearchBox(
                isSearchMode = isSearchMode,
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                onCloseSearch = { isSearchMode = false },
                sandeshViewModel = groupViewModel
            )

            var selectToAddMembers by remember { mutableStateOf(false) }

            if (showUserSettings) {
                GroupUserSettingsModal(
                    isGroupActive = isGroupActive == true,
                    onDismiss = { showUserSettings = false },
                    userNumber = groupViewModel.userNumberKhud,
                    members = members,
                    onMediaSelected = { selectedType ->
                        navController.navigate("media_screen/$groupId/${selectedType.value}")
                    },
                    onAddMembersClicked = {
                        selectToAddMembers = true
                    },
                    onPermissionChanged = { member, updatedPermissions ->
                        groupViewModel.updateMemberPermissionsBulk(
                            groupId = groupId,
                            userNumber = member.userNumber,
                            permissions = updatedPermissions
                        )
                    },
                    onRemoveMember = { member ->
                        val notifyUserJson = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(
                                DootawasMessage.SANDESH_TYPE,
                                SandeshType.MEMBERS_UPDATED.value
                            )

                            val userNumbersArray = JsonArray().apply {
                                add(member.userNumber)
                            }
                            add(JsonKeys.USER_NUMBER, userNumbersArray)
                        }
                        val jsonObject = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(JsonKeys.USER_NUMBER, member.userNumber)
                            addProperty(DootawasMessage.SANDESH_TYPE, GroupSandeshType.ACTION_REMOVE_MEMBER.value)
                            add(JsonKeys.SANDESH, notifyUserJson)
                        }

                        groupViewModel.groupAction(jsonObject)
                    },
                    onProfileImageUpdated = { path ->
                        groupViewModel.setProfileImage(path)
                    },
                    onLeaveGroup = {
                        val notifyUserJson = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.MEMBERS_UPDATED.value)
                            addProperty(JsonKeys.USER_NUMBER, groupViewModel.userNumberKhud)
                        }
                        val jsonObject = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            // Create a JsonArray and add your single user number to it
                            val userNumbersArray = JsonArray().apply {
                                add(groupViewModel.userNumberKhud)
                            }
                            add(
                                JsonKeys.USER_NUMBER,
                                userNumbersArray
                            ) // Use .add() for JsonElements like arrays


                            addProperty(DootawasMessage.SANDESH_TYPE, GroupSandeshType.ACTION_LEAVE_GROUP.value)
                            add(JsonKeys.SANDESH, notifyUserJson)
                        }
                        groupViewModel.groupAction(jsonObject)
                        groupViewModel.updateGroupActive(true)
                    },
                    onDeleteGroup = {
                        val notifyUserJson = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(
                                DootawasMessage.SANDESH_TYPE,
                                SandeshType.GROUP_UPDATED.value
                            )
                            // Create a JsonArray and add your single user number to it
                            val userNumbersArray = JsonArray().apply {
                                add(groupViewModel.userNumberKhud)
                            }
                            add(
                                JsonKeys.USER_NUMBER,
                                userNumbersArray
                            ) // Use .add() for JsonElements like arrays

                            addProperty(
                                DootawasMessage.SANDESH_TYPE,
                                SandeshType.GROUP_UPDATED.value
                            )
                        }
                        val jsonObject = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(DootawasMessage.SANDESH_TYPE, GroupSandeshType.ACTION_DELETE_GROUP.value)
                            add(JsonKeys.SANDESH, notifyUserJson)
                        }

                        groupViewModel.groupAction(jsonObject)
                        groupViewModel.updateGroupActive(false)
                        showUserSettings = false
                    },
                    onDeleteGroupProfile = {
                        val notifyUserJson = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(
                                DootawasMessage.SANDESH_TYPE,
                                SandeshType.MEMBERS_UPDATED.value
                            )
                            addProperty(JsonKeys.USER_NUMBER, groupViewModel.userNumberKhud)
                            addProperty(
                                DootawasMessage.SANDESH_TYPE,
                                SandeshType.MEMBERS_UPDATED.value
                            )
                        }
                        val jsonObject = JsonObject().apply {
                            addProperty(JsonKeys.GROUP_ID, groupId)
                            addProperty(DootawasMessage.SANDESH_TYPE, GroupSandeshType.ACTION_DELETE_PROFILE.value)
                            add(JsonKeys.SANDESH, notifyUserJson)
                        }
                        groupViewModel.groupAction(jsonObject)
                    },
                    wallpaperUri = wallpaperUrl,
                    onWallpaperSelected = { uri ->
                        groupViewModel.updateWallpaper(uri)
                    },
                    onRemoveWallpaper = {
                        groupViewModel.updateWallpaper(null)
                    }
                )
            }


            if (selectToAddMembers) {
                Dialog(onDismissRequest = { selectToAddMembers = false }) {
                    AddMemberContactPicker(
                        onContactsSelected = { numbers ->
                            groupViewModel.addMembersToGroup(numbers)
                            selectToAddMembers = false
                        },
                        onBack = { selectToAddMembers = false },
                        existingMembers = members.map { it.userNumber }
                    )
                }
            }

            // ✅ Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isLandscape) {
                            if (navBarOnRight) Modifier.padding(end = sidePadding)
                            else Modifier.padding(start = sidePadding)
                        } else {
                            Modifier.padding(top = sidePadding)
                        }
                    )
                    .padding(horizontal = 2.dp), // your usual inner horizontal padding
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(
                    items = parsedList,
                    key = { it.sandeshId },
                    contentType = { item ->
                        when (item) {
                            is GroupParsedTextSandesh -> "text"
                            is GroupParsedAudioSandesh -> "audio"
                            is GroupParsedContactSandesh -> "contact"
                            is GroupParsedDocumentSandesh -> "document"
                            is GroupParsedImageSandesh -> "image"
                            is GroupParsedLocationSandesh -> "location"
                            is GroupParsedVideoSandesh -> "video"
                            is GroupParsedUniversalSandesh -> "universal"
                            is GroupParsedListSandesh -> "list"
                            is GroupParsedDateHeaderSandesh -> "date_header"
                        }
                    }

                ) { parsedItem ->



                    when (parsedItem) {
                        is GroupParsedTextSandesh -> {
                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId
                            GroupMessageBubble(
                                userNumber = parsedItem.displayNameOrNumber,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderProfile,
                                isSelf = parsedItem.isSelf,
                                timeLong = parsedItem.time,
                                data = parsedItem.paath,
                                formattedTime = parsedItem.formattedTime,
                                msgType = parsedItem.msgType,
                                linkUri = parsedItem.linkUri,
                                linkTitle = parsedItem.linkTitle,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,  // ✅ Pass reply JSON
                                isSelected = isSelectedItem,
                                isSelectionMode = isSelectionMode,
                                isFirstInGroup = parsedItem.isFirstInGroup,
                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(parsedItem.sandeshId)
                                },
                                onToggleSelection = {
                                    if (selectedMessages.contains(parsedItem.sandeshId)) {
                                        selectedMessages.remove(parsedItem.sandeshId)
                                    } else {
                                        selectedMessages.add(parsedItem.sandeshId)
                                    }
                                    if (selectedMessages.isEmpty()) isSelectionMode = false
                                },
                                onReplyClick = { sandeshId ->
                                    
                                    // You can handle reply actions here (e.g., highlight original)
                                    scrollToMessage(sandeshId)
                                },
                                isHighlighted = isHighlightedItem,
                                sandeshStithi = parsedItem.sandeshStithi,
                                fontSize = fontSize,
                                borderMode = borderMode,
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },
                                dootawasColors = dootawasColors
                            )


                        }

                        is GroupParsedImageSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId
                                GroupImageBubble(
                                    userNumber = parsedItem.displayNameOrNumber,
                                    userColor = if (isDark) {
                                        parsedItem.senderUserDarkColor
                                    } else {
                                        parsedItem.senderUserLightColor
                                    },
                                    userImageUri = parsedItem.senderUserProfile,
                                    isSelf = parsedItem.isSelf,
                                    imageUris = parsedItem.imageUris,
                                    blurFileUri = parsedItem.blurUri,
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    isSelected = isSelectedItem,
                                    paath = parsedItem.paath,
                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(parsedItem.sandeshId)
                                    },


                                    onImageClick = { uris, clickedIndex ->

                                        if (isSelectionMode) {

                                            if (selectedMessages.contains(parsedItem.sandeshId)) {
                                                selectedMessages.remove(parsedItem.sandeshId)
                                            } else {
                                                selectedMessages.add(parsedItem.sandeshId)
                                            }

                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }

                                        } else {

                                            previewImages = uris
                                            previewIndex = clickedIndex
                                            showImagePreview = true
                                        }
                                    },
                                    onDownloadClick = {
                                        
                                        groupViewModel.downloadGroupFile(
                                            sandeshId = parsedItem.sandeshId,
                                            groupId = groupId
                                        )

                                    },
                                    onCancelClick = {

                                    },
                                    onReplyClick = { messageIdReply ->
                                        
                                        scrollToMessage(messageIdReply)
                                    },
                                    isHighlighted = isHighlightedItem,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,
                                    imageLoader = groupViewModel.imageLoader,
                                    onReplySlide = {
                                        groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    },dootawasColors = dootawasColors
                                )
                        }

                        is GroupParsedVideoSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId

                            GroupVideoBubble(
                                sandeshId = sandeshId,
                                userNumber = parsedItem.displayNameOrNumber,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderUserProfile,
                                isSelf = parsedItem.isSelf,
                                sandeshType = parsedItem.sandeshType,
                                timeLong = parsedItem.time,
                                formattedTime = parsedItem.formattedTime,
                                formattedDate = parsedItem.formattedDate,
                                blurUri = parsedItem.blurUri,
                                downloadPercent = parsedItem.downloadPercent,
                                downloadSize = parsedItem.downloadSize,
                                sandeshStithi = parsedItem.sandeshStithi,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,

                                isSelected = isSelectedItem,
                                isHighlighted = isHighlightedItem,
                                paath = parsedItem.paath,

                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },

                                onToggleSelection = { videoURI ->

                                    if (isSelectionMode) {

                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }

                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }

                                    } else {
                                        val index = parsedItem.videoUris.indexOf(videoURI)
                                        if (index >= 0) {
                                            videoPreviewUris = parsedItem.videoUris
                                            videoPreviewIndex = index
                                            showVideoPreview = true
                                        }
                                    }
                                },

                                onDownloadClick = {
                                    groupViewModel.downloadGroupFile(
                                        sandeshId = sandeshId,
                                        groupId = groupId
                                    )
                                },

                                onReplyClick = { id ->
                                    scrollToMessage(id)
                                },

                                onCancelDownload = {
                                    groupViewModel.resetGroupDownloadPercentage(sandeshId)
                                },
                                videoUris = parsedItem.videoUris,
                                previewUris = parsedItem.previewUris,
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },dootawasColors = dootawasColors
                            )
                        }

                        is GroupParsedDocumentSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isHighlightedItem = sandeshId == highlightedMessageId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            GroupDocumentBubble(
                                sandeshId = sandeshId,
                                userNumber = parsedItem.displayNameOrNumber,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderUserProfile,
                                isSelf = parsedItem.isSelf,
                                sandeshType = parsedItem.sandeshType,
                                timeLong = parsedItem.time,
                                formattedTime = parsedItem.formattedTime,
                                formattedDate = parsedItem.formattedDate,
                                savedFileUri = parsedItem.savedFileUri,
                                duplicateUri = parsedItem.duplicateUri,
                                mimeType = parsedItem.mimeType,
                                downloadPercent = parsedItem.downloadPercent,
                                downloadSize = parsedItem.downloadSize,
                                sandeshStithi = parsedItem.sandeshStithi,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,

                                isSelectionMode = isSelectionMode,
                                isSelected = isSelectedItem,
                                isHighlighted = isHighlightedItem,

                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },

                                onToggleSelection = { docURI, mimeType ->

                                    if (isSelectionMode) {

                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }

                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }

                                    } else {

                                        docURI?.let { openDocument(context, it, mimeType) }

                                    }
                                },

                                onDownloadClick = {
                                    groupViewModel.downloadGroupFile(
                                        sandeshId = sandeshId,
                                        groupId = groupId
                                    )
                                },

                                onReplyClick = { id ->
                                    scrollToMessage(id)
                                },

                                onCancelDownload = {
                                    groupViewModel.resetGroupDownloadPercentage(sandeshId)
                                },
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },dootawasColors = dootawasColors
                            )
                        }

                        is GroupParsedAudioSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId

                            GroupAudioBubble(
                                sandeshId = sandeshId,
                                userNumber = parsedItem.displayNameOrNumber,
                                isPlaying = groupViewModel.isPlaying,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderUserProfile,
                                savedFileUri = parsedItem.savedFileUri,

                                exoPlayer = groupViewModel.exoPlayer,
                                currentlyPlayingId = groupViewModel.currentlyPlayingId,

                                onPlayClick = {
                                    parsedItem.savedFileUri?.let {
                                        groupViewModel.togglePlay(
                                            sandeshId = sandeshId,
                                            uri = it
                                        )
                                    }
                                },

                                downloadPercent = parsedItem.downloadPercent,
                                downloadSize = parsedItem.downloadSize,
                                isSelf = parsedItem.isSelf,
                                sandeshStithi = parsedItem.sandeshStithi,
                                timeLong = parsedItem.time,
                                formattedTime = parsedItem.formattedTime,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,

                                isSelectionMode = isSelectionMode,
                                isSelected = isSelectedItem,
                                isHighlighted = isHighlightedItem,

                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },

                                onToggleSelection = {
                                    if (isSelectionMode) {

                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }

                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    }
                                },

                                onDownloadClick = {
                                    groupViewModel.downloadGroupFile(
                                        sandeshId = sandeshId,
                                        groupId = groupId
                                    )
                                },

                                onReplyClick = { id ->
                                    scrollToMessage(id)
                                },
                                duration = parsedItem.duration,
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },dootawasColors = dootawasColors
                            )
                        }
                        is GroupParsedContactSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId

                            GroupContactBubble(
                                sandeshId = sandeshId,
                                userNumber = parsedItem.displayNameOrNumber,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderUserProfile,
                                isSelf = parsedItem.isSelf,
                                sandeshType = parsedItem.sandeshType,
                                timeLong = parsedItem.time,
                                formattedTime = parsedItem.formattedTime,
                                formattedDate = parsedItem.formattedDate,
                                contacts = parsedItem.contacts,
                                sandeshStithi = parsedItem.sandeshStithi,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,

                                isSelected = isSelectedItem,
                                isHighlighted = isHighlightedItem,

                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },

                                onToggleSelection = {
                                    if (isSelectionMode) {

                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }

                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    }
                                },

                                onReplyClick = { id ->
                                    scrollToMessage(id)
                                },
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },dootawasColors = dootawasColors
                            )
                        }
                        is GroupParsedLocationSandesh -> {

                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem = selectedMessages.contains(sandeshId)
                            val isHighlightedItem = sandeshId == highlightedMessageId

                            GroupLocationBubble(
                                sandeshId = sandeshId,
                                userNumber = parsedItem.displayNameOrNumber,
                                userColor = if (isDark) {
                                    parsedItem.senderUserDarkColor
                                } else {
                                    parsedItem.senderUserLightColor
                                },
                                userImageUri = parsedItem.senderUserProfile,
                                isSelf = parsedItem.isSelf,
                                sandeshType = parsedItem.sandeshType,
                                timeLong = parsedItem.time,
                                formattedTime = parsedItem.formattedTime,
                                formattedDate = parsedItem.formattedDate,
                                savedFileUri = parsedItem.savedFileUri,
                                duplicateUri = parsedItem.duplicateUri,
                                latitude = parsedItem.latitude,
                                longitude = parsedItem.longitude,
                                sandeshStithi = parsedItem.sandeshStithi,
                                replyOriginalMessage = parsedItem.replyOriginalMessage,
                                isSelectionMode = isSelectionMode,
                                isHighlighted = isHighlightedItem,
                                isSelected = isSelectedItem,
                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },

                                onToggleSelection = { geoUri ->

                                    if (isSelectionMode) {

                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }

                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }

                                    } else {

                                        if (parsedItem.isLive) {
                                            liveLocationSandeshId = parsedItem.sandeshId
                                        } else {
                                            geoUri?.let {
                                                val intent = Intent(Intent.ACTION_VIEW, it)
                                                intent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(intent)
                                            }
                                        }

                                    }
                                },

                                onDownloadClick = {
                                    groupViewModel.downloadGroupFile(
                                        sandeshId = sandeshId,
                                        groupId = groupId
                                    )
                                },

                                onReplyClick = { id ->
                                    scrollToMessage(id)
                                },
                                isLive = parsedItem.isLive,
                                onStopLiveLocation = {
                                    groupViewModel.stopLiveLocationSharing(
                                        context = context,
                                        sandeshId = parsedItem.sandeshId,
                                        )
                                },
                                liveUntil = parsedItem.liveUntil,
                                isStopped = parsedItem.isStopped,
                                currentTime = currentTime,
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },
                                dootawasColors = dootawasColors
                            )
                        }

                        is GroupParsedUniversalSandesh -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp), // simple spacing
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.85f
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = parsedItem.paath,
                                        fontSize = fontSize.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        is GroupParsedListSandesh -> {
                            val sandeshId = parsedItem.sandeshId
                            val isSelectedItem by remember(sandeshId) {
                                derivedStateOf { selectedMessages.contains(sandeshId) }
                            }
                            val isHighlightedItem by remember(sandeshId) {
                                derivedStateOf { sandeshId == highlightedMessageId }
                            }

                            GroupListBubble(
                                data = parsedItem,
                                replyOriginalMessage = null, // Dynamic extraction matching location bubble architecture
                                isSelected = isSelectedItem,
                                isHighlighted = isHighlightedItem,
                                dootawasColors = dootawasColors,
                                onLongPress = {
                                    if (!isSelectionMode) isSelectionMode = true
                                    selectedMessages.add(sandeshId)
                                },
                                onReplyClick = { messageIdReply ->
                                    scrollToMessage(messageIdReply)
                                },
                                onReplySlide = {
                                    groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                },
                                onAcceptClick = { id ->
                                    groupViewModel.acceptProposedListUpdate(id, groupId)
                                },
                                onRejectClick = { id ->
                                    groupViewModel.rejectProposedListUpdate(id, groupId)
                                },
                                onBubbleClick = {
                                    if (isSelectionMode) {
                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }
                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        // Matches single-chat structural state requirements
                                        activeListForEdit = Pair(sandeshId, parsedItem.displayList)
                                    }
                                }
                            )
                        }
                        is GroupParsedDateHeaderSandesh -> {
                            GroupDateHeader(
                                key = parsedItem.sandeshId,
                                shortText = parsedItem.formattedDate,
                                detailedText = parsedItem.detailedDate,
                                groupDootawasColors =  dootawasColors
                            )
                        }
                    }

                }
            }


            val isTyping by groupViewModel.isTyping


            if (isTyping) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.typing)
                        .decoderFactory(GifDecoder.Factory())
                        .build(),
                    contentDescription = "Typing indicator",
                    modifier = Modifier
//                            .padding(start = 12.dp)
                        .size(36.dp)
                )
            }

//            if (replyMessage) {
//
//                val jsonObject = try {
//                    JsonParser.parseString(singleSelectedSandesh!!.samagri).asJsonObject
//                } catch (e: Exception) {
//                    JsonObject() // Handle invalid JSON safely
//                }
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(Color(0xFFEEEEEE)) // Light gray background
//                        .padding(8.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        Icons.Default.Info,
//                        contentDescription = "Reply",
//                        tint = Color.Gray,
//                        modifier = Modifier.padding(end = 8.dp)
//                    )
//
//                    when (singleSelectedSandesh?.let { SandeshType.fromInt(it.sandeshType) }) {
//                        SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE -> {
//                            val replyText =
//                                jsonObject.get(DootawasMessage.PAATH)?.asString.orEmpty()
//                            Text(
//                                text = replyText.take(30) + if (replyText.length > 40) "..." else "",
//                                modifier = Modifier.weight(1f),
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis,
//                                fontSize = 14.sp,
//                                color = Color.Black
//                            )
//                        }
//
//                        SandeshType.IMAGE, SandeshType.REPLY_IMAGE -> {
//                            val replyImage =
//                                jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
//                            replyImage?.let { imageUrl ->
//                                AsyncImage(
//                                    model = imageUrl,
//                                    contentDescription = "Reply Image",
//                                    modifier = Modifier
//                                        .size(40.dp)
//                                        .clip(RoundedCornerShape(8.dp)) // Clip first
//                                        .border(
//                                            1.dp,
//                                            Color.Gray,
//                                            RoundedCornerShape(8.dp)
//                                        ) // Add border with same shape
//                                        .background(Color.LightGray),
//                                    contentScale = ContentScale.Crop
//                                )
//                            }
//                        }
//
//                        SandeshType.VIDEO, SandeshType.REPLY_VIDEO -> {
//                            val replyImage =
//                                jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
//                            replyImage?.let { imageUrl ->
//
//                                Box(
//                                    modifier = Modifier
//                                        .size(40.dp)
//                                        .clip(RoundedCornerShape(8.dp))
//                                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
//                                ) {
//                                    AsyncImage(
//                                        model = imageUrl,
//                                        contentDescription = "Reply Image",
//                                        modifier = Modifier
//                                            .matchParentSize()
//                                            .background(Color.LightGray),
//                                        contentScale = ContentScale.Crop
//                                    )
//                                    Icon(
//                                        imageVector = Icons.Default.PlayArrow,
//                                        contentDescription = "Reply Video",
//                                        modifier = Modifier
//                                            .align(Alignment.Center) // Center the icon on the image
//                                            .size(20.dp),
//                                        tint = Color.White // White for better visibility
//                                    )
//                                }
//                            }
//                        }
//
//                        SandeshType.AUDIO -> {
//                            Icon(
//                                imageVector = Icons.Default.AddCircle,
//                                contentDescription = "Reply Audio",
//                                modifier = Modifier.size(24.dp),
//                                tint = Color.Black
//                            )
//                        }
//
//                        else -> {}
//                    }
//                    Spacer(modifier = Modifier.weight(1f)) // Pushes close button to the end
//                    IconButton(onClick = { replyMessage = false }) {
//                        Icon(
//                            Icons.Default.Close,
//                            contentDescription = "Cancel Reply",
//                            tint = Color.Red
//                        )
//                    }
//                }
//            }

            if (isSelectionMode) {


                if (!showForward) {

                    val clipboard = LocalClipboard.current
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    MessageSelectionBar(
                        selectedCount = selectedMessages.size,


                        onCopy = {
                            val selectedId =
                                selectedMessages.firstOrNull() ?: return@MessageSelectionBar

                            scope.launch {
                                // 1. Get the raw JSON string (samagri)
                                val samagriJsonString =
                                    groupViewModel.getSingleGroupMessage(
                                        selectedId,
                                        groupId
                                    )

                                if (!samagriJsonString.isNullOrEmpty()) {

                                    // 2. Parse the JSON string and extract the "paath" key
                                    val textToCopy = try {
                                        JsonParser.parseString(samagriJsonString)
                                            .asJsonObject
                                            .get("paath") // Get the "paath" JsonElement
                                            .asString      // Convert it to a String
                                    } catch (e: Exception) {
                                        // Log or handle parsing errors (e.g., if 'paath' is missing or not a string)
                                        Log.e(
                                            "CopyError",
                                            "Failed to parse samagri JSON: ${e.message}"
                                        )
                                        return@launch // Stop execution if parsing failed
                                    }

                                    // 3. Copy the extracted "paath" text using the modern API
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText(
                                                "Copied Paath Text",
                                                textToCopy
                                            )
                                        )
                                    )

                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }

                                isSelectionMode = false
                                selectedMessages.clear()
                            }
                        },
                        onInfo = {
                            val selectedId =
                                selectedMessages.firstOrNull() ?: return@MessageSelectionBar

                            scope.launch {
                                // Call VM suspend method to get parsed JSON
                                val status = groupViewModel.getMessageStatusInfo(
                                    userNumber = groupId,
                                    sandeshId = selectedId
                                )

                                selectedStatusInfo = status
                                showInfoDialog = true


                            }
                        },
                        onReply = {
                            val selectedId = selectedMessages.firstOrNull()
                            val sandeshId =
                                parsedList.find { it.sandeshId == selectedId }?.sandeshId
                            if (sandeshId != null) {
                                groupViewModel.fetchSingleGroupSandesh(sandeshId, groupId)
                                replyMessage = true
                                isSelectionMode = false
                                selectedMessages.clear()
                            }
                        },
                        onForward = {
                            showForward = true
                        },
                        onDelete = { showDialog = true },
                        showReactionBar = selectedMessages.size == 1 && showEmojiPicker,

                        quickReactions = quickReactions.take(maxVisibleEmojis),

                        onReactionSelected = { emoji ->
                            val selectedId = selectedMessages.firstOrNull()
                            val sandeshId = parsedList.find {
                                it.sandeshId == selectedId
                            }?.sandeshId

                            if (sandeshId != null) {
                                groupViewModel.reactToMsg(emoji, sandeshId)
                                selectedMessages.clear()
                                isSelectionMode = false
                            }
                        },

                        onExpandEmojiPicker = {
                            showEmojiPicker = true
                        },
                        surfaceColor = dootawasColors.userHeaderBackground,
                        contentColor = dootawasColors.userHeaderText
                    )

                } else {
                    ContactPickerWithHomeScreen(
                        onContactsSelected = { selectedTargets ->

                            groupViewModel.processForward(
                                selectedMessages.toList(),
                                selectedTargets
                            )
                            selectedMessages.clear()
                            showContactPicker = false
                            isSelectionMode = false
                            showForward = false
                        },
                        onBack = {
                            selectedMessages.clear()
                            showContactPicker = false
                            isSelectionMode = false
                            showForward = false
                        }
                    )
                }
            } else {

                GroupMessageInputBar(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    isGroupActive = isGroupActive,
                    enterToSendEnabled = enterToSendEnabled,
                    dootawasColors = dootawasColors,
                    onAttachClick = { showSheet = true },
                    onSendMessage = {
                        messageText = messageText.trimEnd('\n')
                        sendMessage()
                    },
                    onCameraClick = { showCamera = true },
                    onRecorderClick = { showRecorder = true },
                    onTyping = { groupViewModel.sendGroupTyping(groupId) },
                    replyMessage = replyMessage,
                    singleSelectedSandesh = singleSelectedSandesh,
                    onCancelReply = {
                        replyMessage = false
                        // Clear your selected reply message here too.
                    }
                )


//                val scrollState = rememberScrollState()
//                val interactionSource = remember { MutableInteractionSource() }
//
//                // Animation for icon transition
//                val iconTransition = updateTransition(
//                    targetState = messageText.isNotBlank(),
//                    label = "iconTransition"
//                )
//                val iconScale by iconTransition.animateFloat(
//                    transitionSpec = {
//                        tween(
//                            durationMillis = 200,
//                            easing = FastOutSlowInEasing
//                        )
//                    },
//                    label = "iconScale"
//                ) { if (it) 1f else 0.9f }
//
//                Row(
//                    modifier = Modifier
//                        .imePadding()
//                        .background(dootawasColors.chatScreenBackground) // ✅ solid color
//                        .padding(horizontal = 8.dp, vertical = 8.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    // Plus icon with animation
//                    IconButton(
//                        onClick = {
////                            focusManager.clearFocus()
////                            groupViewModel.testCall()
//                            showSheet = true },
//                        modifier = Modifier
//                            .size(48.dp)
//                            .shadow(4.dp, RoundedCornerShape(12.dp))
//                            .background(MaterialTheme.colorScheme.surfaceContainer)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Add,
//                            contentDescription = "Add",
//                            tint = MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier.scale(iconScale)
//                        )
//                    }
//
//                    // Scrollable TextField
//                    Box(
//                        modifier = Modifier
//                            .weight(1f)
//                            .heightIn(min = 48.dp, max = 120.dp)
//                            .shadow(2.dp, RoundedCornerShape(24.dp))
//                            .background(
//                                MaterialTheme.colorScheme.surfaceContainer,
//                                RoundedCornerShape(24.dp)
//                            )
//                            .clip(RoundedCornerShape(24.dp))
//                            .verticalScroll(scrollState)
//                            .padding(horizontal = 16.dp, vertical = 12.dp)
//                    ) {
//                        if (messageText.isEmpty()) {
//                            Text(
//                                text = "Type a message...",
//                                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                fontSize = 16.sp,
//                                modifier = Modifier.align(Alignment.CenterStart)
//                            )
//                        }
//
//                        BasicTextField(
//                            value = messageText,
//                            enabled = isGroupActive == true,
//                            onValueChange = { newText ->
//                                messageText = newText
//                                groupViewModel.sendGroupTyping(groupId)
//                            },
//                            textStyle = LocalTextStyle.current.copy(
//                                color = MaterialTheme.colorScheme.onSurface,
//                                fontSize = 16.sp
//                            ),
//                            modifier = Modifier.fillMaxWidth(),
//                            keyboardOptions = KeyboardOptions(
//                                capitalization = KeyboardCapitalization.Sentences,
//                                imeAction = if (enterToSendEnabled) ImeAction.Send else ImeAction.Default
//                            ),
//                            keyboardActions = KeyboardActions(
//                                onSend = {
//                                    if (enterToSendEnabled && messageText.isNotBlank()) {
//                                        messageText = messageText.trimEnd('\n')
//                                        sendMessage()
//                                    }
//                                }
//                            ),
//                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
//                            interactionSource = interactionSource,
//                            decorationBox = { innerTextField -> innerTextField() }
//                        )
//                    }
//
//                    // Send or Camera Button
//                    Box(
//                        modifier = Modifier
//                            .size(48.dp)
//                            .shadow(4.dp, RoundedCornerShape(12.dp))
//                            .background(
//                                MaterialTheme.colorScheme.surfaceContainer,
//                                RoundedCornerShape(12.dp)
//                            )
//                            .clip(RoundedCornerShape(12.dp))
//                            .combinedClickable(
//                                interactionSource = interactionSource,
//                                indication = ripple(color = MaterialTheme.colorScheme.primary),
//                                onClick = {
//                                    if (messageText.isNotBlank() && isGroupActive == true) {
//                                        messageText = messageText.trimEnd('\n')
//                                        sendMessage()
//                                    } else {
//                                        showCamera = true
//
//                                    }
//                                },
//                                onLongClick = {
//                                    if (messageText.isBlank() && isGroupActive == true) {
//
//                                        showRecorder = true
//                                    }
//                                }
//                            ),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Crossfade(
//                            targetState = messageText.isNotBlank(),
//                            animationSpec = tween(200)
//                        ) { isSendIcon ->
//                            if (isSendIcon) {
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Filled.Send,
//                                    contentDescription = "Send",
//                                    tint = MaterialTheme.colorScheme.primary,
//                                    modifier = Modifier.scale(iconScale)
//                                )
//                            } else {
//                                Icon(
//                                    painter = painterResource(id = R.drawable.icon_camera_24px),
//                                    contentDescription = "Camera",
//                                    tint = MaterialTheme.colorScheme.primary,
//                                    modifier = Modifier.scale(iconScale)
//                                )
//                            }
//                        }
//                    }
//                }
//
//                    val scrollState = rememberScrollState()
//
//                    Row(
//                        modifier = Modifier
//                            .imePadding()
//                            .background(
//                                Color(0xFFF2F2F2),
//                            ),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        // Plus icon
//                        IconButton(onClick = { showSheet = true }) {
//                            Icon(
//                                imageVector = Icons.Default.Add,
//                                contentDescription = "Add",
//                                tint = Color.Gray
//                            )
//                        }
//
//                        // Scrollable + expanding TextField
//                        Box(
//                            modifier = Modifier
//                                .weight(1f)
//                                .heightIn(
//                                    min = 40.dp,
//                                    max = 120.dp
//                                ) // Grows up to 200dp then scrolls
//                                .verticalScroll(scrollState)
//                                .background(Color.White, RoundedCornerShape(24.dp))
//                                .padding(horizontal = 12.dp, vertical = 10.dp)
//                        ) {
//                            if (messageText.isEmpty()) {
//                                Text(
//                                    text = "Type a message...",
//                                    color = Color.Gray
//                                )
//                            }
//
//
//                            BasicTextField(
//                                value = messageText,
//                                onValueChange = { newText ->
//                                    messageText = newText
//                                    groupViewModel.sendTyping(groupId)
//                                },
//                                textStyle = LocalTextStyle.current.copy(color = Color.Black),
//                                modifier = Modifier.fillMaxWidth(),
//                                keyboardOptions = KeyboardOptions(
//                                    imeAction = if (enterToSendEnabled) ImeAction.Send else ImeAction.Default
//                                ),
//                                keyboardActions = KeyboardActions(
//                                    onSend = {
//                                        if (enterToSendEnabled && messageText.isNotBlank()) {
//                                            messageText = messageText.trimEnd('\n')
//                                            sendMessage()
//                                        }
//                                    }
//                                ),
//                                cursorBrush = SolidColor(Color.Black),
//                                decorationBox = { innerTextField -> innerTextField() }
//                            )
//                        }
//                        Box(
//                            modifier = Modifier
//                                .size(56.dp)
//                                .combinedClickable(onClick = {
//                                    // Short click: send message if messageText is not blank
//                                    if (messageText.isNotBlank()) {
//                                        sendMessage()
//                                    } else {
//                                        // Short click when messageText is blank - Show camera functionality
//                                        showCamera = true
//                                        
//                                    }
//                                }, onLongClick = {
//                                    if (messageText.isBlank()) {
//                                        // Long press: open recorder for mic
//                                        
//                                        showRecorder =
//                                            true // This should trigger your UI to show the recorder
//                                    }
//
//                                }),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            if (messageText.isNotBlank()) {
//                                // Show send icon if messageText is not blank
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Filled.Send,
//                                    contentDescription = "Send",
//                                    tint = Color.Blue
//                                )
//                            } else {
//                                // Show camera icon if messageText is blank
//                                val cameraIcon: Painter =
//                                    painterResource(id = R.drawable.icon_camera_24px) // Replace with your drawable name
//                                Icon(
//                                    painter = cameraIcon,
//                                    contentDescription = "Camera",
//                                    tint = Color.Blue
//                                )
//                            }
//                        }
//
//                    }
            }

        }


        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 7.dp, bottom = 68.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { scrollToBottom() },
                shape = CircleShape,
                containerColor = dootawasColors.userHeaderBackground,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom"
                )
            }
        }

        if (showOverlay && overlayDate != null && !showContactPicker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF2D3D2E), Color(0xAA000000))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = overlayDate!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

//        if (isSelectionMode && selectedMessages.size == 1) {
//
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
//                    .padding(bottom = 56.dp) // height of action row
//                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp, horizontal = 12.dp)
//                        .horizontalScroll(rememberScrollState()),
//                    horizontalArrangement = Arrangement.spacedBy(10.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = selectedMessages.size.toString(),
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 20.sp
//                    )
//
//                    quickReactions.take(maxVisibleEmojis).forEach { emoji ->
//                        EmojiReactionItem(
//                            emoji = emoji,
//                            onClick = {
//                                val selectedId = selectedMessages.firstOrNull()
//                                val sandeshId = parsedList.find { it.sandeshId == selectedId }?.sandeshId
//                                if (sandeshId != null) {
//                                    groupViewModel.reactToMsg(emoji, sandeshId)
//                                    selectedMessages.clear()
//                                    isSelectionMode = false
//                                }
//                            }
//                        )
//                    }
//
//                    Box(
//                        modifier = Modifier
//                            .clip(CircleShape)
//                            .background(MaterialTheme.colorScheme.primaryContainer)
//                            .clickable { showEmojiPicker = true }
//                            .padding(10.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "😀 +",
//                            fontSize = 20.sp,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//
//                HorizontalDivider(
//                    thickness = 0.7.dp,
//                    color = MaterialTheme.colorScheme.outlineVariant
//                )
//            }
//        }

    }


    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->

        if (uris.isNotEmpty()) {


            selectedImageVisibility =
                MediaVisibility.PRIVATE
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)

            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
            }
        }
    }

    if (selectedImageUris.isNotEmpty()) {
        ImageEditorScreen(
            initialRecipients = remember(groupId, groupEntity?.groupName) {
                listOf(
                    PickedContact(
                        userNumber = groupId,
                        userName = groupEntity?.groupName ?: "",
                        profileUri = groupEntity?.groupImageUrl,
                        isGroup = true
                    )
                )
            },
            sourceUris = selectedImageUris.toList(),
            defaultDisappearSeconds = groupEntity?.autoDisappearMsg ?: 0,
            showDisappearButton = true,
            onCancel = {
                selectedImageUris.clear()
            },
            onDone = { files, message, disappearDuration, recipients ->
                groupViewModel.sendImages(
                    context = context.applicationContext,
                    files = files,
                    caption = message,
                    disappearSeconds = disappearDuration,
                    recipients = recipients,
                    visibility = selectedImageVisibility,
                    currentChatId = groupId,
                    currentChatIsGroup = true,
                    replyOriginal = if (replyMessage) singleSelectedSandesh?.let { s ->
                        JsonObject().apply {
                            addProperty(DootawasMessage.ORIGINAL_SAMAGRI, s.samagri)
                            addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, s.sandeshId)
                            addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, s.sandeshType)
                        }
                    } else null
                )
                if (replyMessage) {
                    replyMessage = false
                    groupViewModel.clearReply()
                }
                selectedImageUris.clear()
            }
        )
    }

    val audioPickerLauncherForGroup = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            val mime = context.contentResolver.getType(pickedUri) ?: ""
            if (!mime.startsWith("audio/")) {
                Toast.makeText(context, "Not an audio file", Toast.LENGTH_LONG).show()
                return@let
            }

            val savedMedia = saveMediaToAppStorageNext(
                context,
                pickedUri,
                MediaCategory.AUDIO,
                selectedImageVisibility
            ) ?: return@let


            val durationMs = getAudioDurationMs(context, savedMedia.localPath) ?: 0L

            val messageSamagri = JsonObject().apply {
                addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                addProperty(DootawasMessage.DURATION, durationMs)
                addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.fileSize)
            }

            if (replyMessage) {
                if (singleSelectedSandesh != null) {
                    val replyJson = JsonObject().apply {
                        addProperty(DootawasMessage.ORIGINAL_SAMAGRI, singleSelectedSandesh!!.samagri)
                        addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, singleSelectedSandesh!!.sandeshId)
                        addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, singleSelectedSandesh!!.sandeshType)
                    }
                    messageSamagri.add(DootawasMessage.ORIGINAL_MESSAGE, replyJson)

                    groupViewModel.replyAudioToGroups(
                        groupIds = listOf(groupId),
                        message = messageSamagri
                    )

                    replyMessage = false
                    groupViewModel.clearReply()
                }
            } else {
                groupViewModel.sendAudioToGroups(
                    groupIds = listOf(groupId),
                    message = messageSamagri
                )
            }
        }
    }

// ✅ Video section
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->

        if (uris.isNotEmpty()) {

            selectedImageVisibility = MediaVisibility.PRIVATE

            selectedVideoUris.clear()
            selectedVideoUris.addAll(uris)

            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
            }

            hideOnSend = false
        }
    }

    if (selectedVideoUris.isNotEmpty()) {

        key(selectedVideoUris.first().toString()) {
            VideoEditorScreen(
                sourceUris = selectedVideoUris.toList(),
                initialRecipients = remember(groupId) {
                    listOf(
                        PickedContact(
                            userNumber = groupId,
                            userName = userName,          // ASSUMPTION: your group's display name variable
                            profileUri = photoUrl,    // ASSUMPTION: your group's photo variable (or null)
                            isGroup = true
                        )
                    )
                },
                defaultDisappearSeconds = 0,
                showDisappearButton = false,               // groups don't support disappearing videos yet
                maxDurationMs = 0L,
                onCancel = { selectedVideoUris.clear() },
                onDone = { files, message, disappearSeconds, recipients ->
                    groupViewModel.sendVideos(
                        context = context.applicationContext,
                        files = files,
                        caption = message,
                        disappearSeconds = disappearSeconds,
                        recipients = recipients,
                        visibility = selectedImageVisibility,
                        currentChatId = groupId,
                        currentChatIsGroup = true,
                        replyOriginal = if (replyMessage) singleSelectedSandesh?.let { s ->
                            JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_SAMAGRI, s.samagri)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, s.sandeshId)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, s.sandeshType)
                            }
                        } else null
                    )
                    if (replyMessage) {
                        replyMessage = false
                        groupViewModel.clearReply()
                    }
                    selectedVideoUris.clear()
                    globalScroll()
                }
            )
        }
    }

    if (showSheet && isGroupActive == true) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        showSheet = false
                    }
                }
        ) {


            HorizontalAttachmentMenu(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 42.dp, bottom = 56.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .pointerInput(Unit) { detectTapGestures { } },
                onImageClick = {
                    showSheet = false
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onVideoClick = {
                    showSheet = false
                    videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                onAudioClick = {
                    showSheet = false
                    audioPickerLauncherForGroup.launch("audio/*")
                },
                onLocationClick = {
                    showSheet = false
                    showLocationScreen = true
                },
                onContactClick = {
                    showSheet = false
                    showContactPicker = true
                },
                onDocumentClick = {
                    showSheet = false
                    documentPickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                onListClick = {},
                onBillClick = {},
                userHeaderBackground = dootawasColors.userHeaderBackground,
                showBillAndList = false,
            )
        }
    }

    if (showLocationScreen) {
        ModernMapPicker(
            onLocationSelected = { location, savedMedia, isLive, expiryTime ->

                val mediaPath = savedMedia?.localPath ?: ""
                val mediaSize = savedMedia?.fileSize ?: 0L
                val mimeType = savedMedia?.mimeType ?: "image/jpeg"
                val messageSamagri = JsonObject().apply {

                    addProperty(DootawasMessage.LATITUDE, location.latitude)
                    addProperty(DootawasMessage.LONGITUDE, location.longitude)
                    addProperty(DootawasMessage.IS_LIVE, isLive)
                    addProperty(DootawasMessage.ORIGINAL_URI, mediaPath)
                    addProperty(DootawasMessage.DOWNLOAD_SIZE, mediaSize)
                    addProperty(DootawasMessage.MIME_TYPE, mimeType)
                    if (expiryTime != null) {
                        addProperty(DootawasMessage.LIVE_UNTIL, expiryTime)
                    } else {
                        addProperty(DootawasMessage.LIVE_UNTIL, "")
                    }


                    if (replyMessage && singleSelectedSandesh != null) {

                        add(
                            DootawasMessage.ORIGINAL_MESSAGE,
                            JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_SAMAGRI, singleSelectedSandesh!!.samagri)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, singleSelectedSandesh!!.sandeshId)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, singleSelectedSandesh!!.sandeshType)
                            }
                        )
                    }
                }

                if (replyMessage && singleSelectedSandesh != null) {

                    groupViewModel.replyLocationToGroups(
                        groupIds = listOf(groupId),
                        message = messageSamagri,
                        context = context
                    )

                    replyMessage = false
                    groupViewModel.clearReply()

                } else {

                    groupViewModel.sendLocationToGroups(
                        groupIds = listOf(groupId),
                        message = messageSamagri,
                        context = context
                    )
                }

                showLocationScreen = false
            },

            onDismiss = {
                showLocationScreen = false
            }
        )

    }

    if (showContactPicker) {
        ContactPickerScreen(
            onContactsSelected = { contactsJson ->
                 // <- got JSON string of selected contacts
                if (replyMessage) {
                    if (singleSelectedSandesh != null) {
                        val replyJson = JsonObject().apply {
                            addProperty(
                                DootawasMessage.ORIGINAL_SAMAGRI,
                                singleSelectedSandesh!!.samagri
                            )
                            addProperty(
                                DootawasMessage.ORIGINAL_MESSAGE_ID,
                                singleSelectedSandesh!!.sandeshId
                            )
                            addProperty(
                                DootawasMessage.ORIGINAL_MESSAGE_TYPE,
                                singleSelectedSandesh!!.sandeshType
                            )
                        }
                        contactsJson.add(DootawasMessage.ORIGINAL_MESSAGE, replyJson)
                        groupViewModel.replyContactToGroups(
                            groupIds = listOf(groupId),
                            message = contactsJson
                        )
                        replyMessage = false
                        groupViewModel.clearReply()
                    }
                } else {
                    groupViewModel.sendContactToGroups(
                        groupIds = listOf(groupId),
                        message = contactsJson
                    )
                }
                showContactPicker = false
            },
            onBack = {
                showContactPicker = false
            }
        )
    }

    // ✅ Show Confirmation Dialog When Needed
    if (showDialog) {
        ConfirmationDialog(
            message = "Are you sure you want to delete this message?",
            onConfirm = { deleteBoth ->
//                val selectedIds = selectedMessages.filter { it.value }.keys

                if (selectedMessages.isNotEmpty()) {
                    groupViewModel.deleteGroupSelectedMessages(
                        selectedMessages,
                        groupId,
                        deleteBoth
                    )
                    selectedMessages.clear() // ✅ Clear selection after deletion
                    isSelectionMode = false
                }
                showDialog = false // ✅ Close dialog
            },
            onDismiss = { showDialog = false } // ✅ Close dialog
        )
    }

    if (showImagePreview) {
        
        ImagePreviewScreen(
            imageList = previewImages,
            startIndex = previewIndex,
            onDismiss = { showImagePreview = false }
        )
    }

    if (showRecorder) {
        Box(
            Modifier
                .fillMaxSize()
//                        .background(Color.Black)
                .pointerInput(Unit) {
                    // Consume all touch events
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }
        ) {
            AudioRecorderScreen(
                { audioUri, durationMs ->
                    showRecorder = false
                    
                    if (replyMessage) {
                        if (singleSelectedSandesh != null) {
                            val replyJson = JsonObject().apply {
                                addProperty(
                                    DootawasMessage.ORIGINAL_SAMAGRI,
                                    singleSelectedSandesh!!.samagri
                                )  // Original message text
                                addProperty(
                                    DootawasMessage.ORIGINAL_MESSAGE_ID,
                                    singleSelectedSandesh!!.sandeshId
                                )  // Original message ID
                                addProperty(
                                    DootawasMessage.ORIGINAL_MESSAGE_TYPE,
                                    singleSelectedSandesh!!.sandeshType
                                )  // Original message type
                            }
                            val messageSamagri = JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_URI, audioUri.toString())
                                addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                                add(
                                    DootawasMessage.ORIGINAL_MESSAGE, replyJson
                                )  // Add the replyJson under the "REPLY" key
                                addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                                addProperty(DootawasMessage.DURATION, durationMs)
                            }
                            //todo
                            groupViewModel.replyAudioToGroups(
                                groupIds = listOf(groupId),
                                message = messageSamagri
                            )

                            replyMessage = false
                            groupViewModel.clearReply()
                        }
                    } else {
                        val messageSamagri = JsonObject()
                        messageSamagri.addProperty(
                            DootawasMessage.ORIGINAL_URI,
                            audioUri.toString()
                        )
                        messageSamagri.addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                        messageSamagri.addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                        messageSamagri.addProperty(DootawasMessage.DURATION, durationMs)
                        //todo
                        groupViewModel.sendAudioToGroups(
                            groupIds = listOf(groupId),
                            message = messageSamagri
                        )
                    }
                },
                onDismiss = { showRecorder = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }


    if (showInfoDialog && selectedStatusInfo != null) {
        GroupMessageInfoDialog(
            status = selectedStatusInfo!!,
            messageId = selectedMessages.firstOrNull() ?: return,
            sandeshViewModel = groupViewModel,
            onDismiss = {
                showInfoDialog = false
                isSelectionMode = false
                selectedMessages.clear()
            },
            formatDateTime = { t -> groupViewModel.formatDateTime(t) }
        )
    }

//
    if (showCamera) {
        PermissionHandler(
            onAllPermissionsGranted = {
                showCamera = true

            },
            onPermissionsDenied = { deniedPermissions ->
                showCamera = false
            }

        )

        if (showCamera) {
            CameraScreen(
                onPhotoCaptured = { uriImage ->
                    selectedImageVisibility =
                        MediaVisibility.PUBLIC
                    selectedImageUris.clear()          // ✅ reset old selection
                    selectedImageUris.add(uriImage)    // ✅ add as single item
                    showCamera = false
                },
                onVideoCaptured = { uri ->
                    selectedImageVisibility = MediaVisibility.PUBLIC
                    hideOnSend = false
                    selectedVideoUris.clear()
                    selectedVideoUris.add(uri)
                    showCamera = false
                },
                onDismiss = { showCamera = false }
            )
        }
    }

    liveLocationSandeshId?.let { sandeshId ->


        LiveLocationScreen(
            sandeshId = sandeshId,
            sandeshViewModel = groupViewModel,
            onBack = {
                liveLocationSandeshId = null
            }
        )
    }
    if (showEmojiPicker) {

        EmojiPickerBottomSheet(
            onDismiss = { showEmojiPicker = false },
            onEmojiPicked = { emoji ->
                val selectedId = selectedMessages.firstOrNull()
                val sandeshId = parsedList.find { it.sandeshId == selectedId }?.sandeshId
                if (sandeshId != null) {
                    groupViewModel.reactToMsg(emoji, sandeshId)
                    selectedMessages.clear()
                    isSelectionMode = false
                }
            }
        )
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var discardAction by remember { mutableStateOf<(() -> Unit)?>(null) }

// --- Inline Fullscreen Dashboard Presentation ---
    if (activeListForEdit != null) {
        val (targetSandeshId, listData) = activeListForEdit!!

        ListScreen(
            initialData = listData,
            currentUserId = groupId,
            onBack = { activeListForEdit = null },
            onSave = { updatedListData ->
                val isNewCreation = (listData == null)

                val listMessageWrapper = ListMessage(
                    messageType = if (isNewCreation) ListMessageType.CREATE else ListMessageType.UPDATE,
                    baseVersion = updatedListData.version,
                    current = updatedListData,
                    updated = null
                )

                val serializedMessageJson = try {
                    val jsonString = Gson().toJson(listMessageWrapper)
                    JsonParser.parseString(jsonString).asJsonObject
                } catch (_: Exception) {
                    JsonObject()
                }

                if (isNewCreation) {
                    groupViewModel.sendList(
                        targetSandeshId = targetSandeshId,
                        userNumbers = listOf(groupId),
                        message = serializedMessageJson
                    )
                } else {
                    groupViewModel.modifyListLocally(
                        targetSandeshId = targetSandeshId,
                        userNumber = groupId,
                        message = serializedMessageJson
                    )
                }
                activeListForEdit = null
            },
            onSendUpdate = { updatedListData ->
                val listMessageWrapper = ListMessage(
                    messageType = ListMessageType.UPDATE,
                    baseVersion = updatedListData.version,
                    current = updatedListData,
                    updated = null,
                    listId = targetSandeshId
                )

                val serializedMessageJson = try {
                    val jsonString = Gson().toJson(listMessageWrapper)
                    JsonParser.parseString(jsonString).asJsonObject
                } catch (_: Exception) {
                    JsonObject()
                }

                // Condition 3: Send Update -> Push out onto wire network queue only
                groupViewModel.sendListUpdateOnly(
                    targetSandeshId = UUID.randomUUID().toString(),
                    userNumbers = listOf(groupId),
                    message = serializedMessageJson
                )
                activeListForEdit = null
            },
            onDiscardConfirmNeeded = { confirmDiscard ->
                discardAction = confirmDiscard
                showDiscardDialog = true
            }
        )
    }

    // --- Safety Intercept Discard Dialog ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardAction?.invoke()
                        activeListForEdit = null
                        showDiscardDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") }
        )
    }

    if (showVideoPreview) {

        VideoGalleryScreen(
            sandeshViewModel = groupViewModel,
            videoUris = videoPreviewUris,
            startIndex = videoPreviewIndex,
            disappearSeconds = groupEntity?.autoDisappearMsg ?: 0,
            onEdit = { uri ->
                selectedVideoUris.clear()
                selectedVideoUris.add(uri)
            },
            onDismiss = {
                showVideoPreview = false
            }
        )
    }
}

