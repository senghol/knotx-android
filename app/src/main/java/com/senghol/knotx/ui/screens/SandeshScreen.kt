package com.senghol.knotx.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.senghol.knotx.ui.components.MessageBubble
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.res.Configuration
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.util.Linkify
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.senghol.knotx.ui.components.single.AudioBubble
import com.senghol.knotx.ui.components.ContactBubble
import com.senghol.knotx.ui.components.DocumentBubble
import com.senghol.knotx.ui.components.ImageBubble
import com.senghol.knotx.ui.components.LocationBubble
import com.senghol.knotx.ui.components.UserSettingsModal
import com.senghol.knotx.ui.components.VideoBubble
import com.senghol.knotx.utils.data.ParsedAudioSandesh
import com.senghol.knotx.utils.data.ParsedContactSandesh
import com.senghol.knotx.utils.data.ParsedDocumentSandesh
import com.senghol.knotx.utils.data.ParsedImageSandesh
import com.senghol.knotx.utils.data.ParsedLocationSandesh
import com.senghol.knotx.utils.data.ParsedTextSandesh
import com.senghol.knotx.utils.data.ParsedVideoSandesh
import com.senghol.knotx.ui.components.ContactPickerWithHomeScreen
import com.senghol.knotx.ui.components.SearchBox
import com.senghol.knotx.ui.components.UserHeaderLandscape
import com.senghol.knotx.ui.components.UserHeaderPortrait
import com.senghol.knotx.utils.enums.SandeshStithi
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.senghol.knotx.ui.components.BillPreviewScreen
import com.senghol.knotx.ui.components.CreateBillScreen
import com.senghol.knotx.ui.components.EmojiPickerBottomSheet
import com.senghol.knotx.ui.components.EmojiReactionItem
import com.senghol.knotx.ui.components.HorizontalAttachmentMenu
import com.senghol.knotx.ui.components.LedgerBubble
import com.senghol.knotx.ui.components.ListBubble
import com.senghol.knotx.ui.components.ListData
import com.senghol.knotx.ui.components.ListMessage
import com.senghol.knotx.ui.components.ListMessageType
import com.senghol.knotx.ui.components.ListScreen
import com.senghol.knotx.ui.components.LiveLocationScreen
import com.senghol.knotx.ui.components.MessageInputBar
import com.senghol.knotx.ui.components.MessageSelectionBar
import com.senghol.knotx.ui.components.ModernMapPicker
import com.senghol.knotx.ui.components.ModernMessageBubble
import com.senghol.knotx.ui.components.SettlementSheet
import com.senghol.knotx.ui.components.SingleMessageInfoDialog
import com.senghol.knotx.ui.components.VideoGalleryScreen
import com.senghol.knotx.ui.components.getAudioDurationMs
import com.senghol.knotx.ui.theme.LocalDootawasTheme
import com.senghol.knotx.utils.data.ParsedLedgerSandesh
import com.senghol.knotx.utils.data.ParsedListSandesh
import com.senghol.knotx.utils.data.ParsedUniversalSandesh
import com.senghol.knotx.utils.data.SingleMessageStatus
import kotlinx.coroutines.FlowPreview
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.senghol.knotx.editor.ImageEditorScreen
import com.senghol.knotx.editor.VideoEditorScreen
import com.senghol.knotx.ui.components.AiUsageDialog
import com.senghol.knotx.ui.components.single.AiMessageBubble
import com.senghol.knotx.ui.components.single.DateHeader
import com.senghol.knotx.ui.components.single.DisappearingRequestBubble
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.dukaan.DukaanScreen
import com.senghol.knotx.utils.data.ParsedAiSandesh
import com.senghol.knotx.utils.data.ParsedDateHeaderSandesh
import com.senghol.knotx.utils.data.ParsedDisappearingSandesh
import com.senghol.knotx.utils.enums.PurchaseOrderType
import kotlin.time.Duration.Companion.milliseconds


tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
//@OptIn(FlowPreview::class)
@SuppressLint("ContextCastToActivity")
@OptIn(FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun SandeshScreen(
    navController: NavController,
//    upbhogta: Upbhogta,
    sandeshViewModel: SandeshViewModel,
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

    val globalTheme =
        LocalDootawasTheme.current

    val isDark =
        isSystemInDarkTheme()

    val lightColors by
    sandeshViewModel.lightColors.collectAsState()

    val darkColors by
    sandeshViewModel.darkColors.collectAsState()

    val userBorderMode by
    sandeshViewModel.borderMode.collectAsState()

    val userUseModernStyle by
    sandeshViewModel.useModernStyle.collectAsState()

    val upbhogta by sandeshViewModel.upbhogta.collectAsState()
// FINAL COLORS
    val dootawasColors =
        if (upbhogta?.customColors != null) {

            if (isDark)
                darkColors
            else
                lightColors

        } else {

            globalTheme.colors
        }

// FINAL BORDER MODE
    val borderMode =
        if (upbhogta?.customColors != null) {
            userBorderMode
        } else {
            globalTheme.borderMode
        }

    // 🌟 FINAL BUBBLE DESIGN MODE
    val useModernStyle =
        if (upbhogta?.customColors != null) {
            userUseModernStyle // 👈 From user-specific StateFlow
        } else {
            globalTheme.useModernStyle // 👈 Fallback to global application configuration
        }

        val fontSize by sandeshViewModel.fontSizeFlow.collectAsState()
        val enterToSendEnabled by sandeshViewModel.chatBehaviourFlow.collectAsState()

    // 👇 THIS is now live from DB

    val userName = upbhogta?.contactName?.takeIf { it.isNotBlank() }
        ?: upbhogta?.onlineName?.takeIf { it.isNotBlank() }
        ?: ""
    val userAbout = upbhogta?.about ?: ""
    val isAi = sandeshViewModel.isAi
        val userNumber = upbhogta?.userNumber ?: ""
        val photoUrl = upbhogta?.photoUrl

        val wallpaperUrl = upbhogta?.wallpaperImage
        val context = LocalContext.current


    //todo insteaad of this selectedMessages we should add isSelected in DataClass
        val selectedMessages = remember { mutableStateSetOf<String>() }
            val parsedList by sandeshViewModel.parsedList.collectAsStateWithLifecycle()
        var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }

    var showAiUsage by remember {
        mutableStateOf(false)
    }
        val walletBalance by sandeshViewModel.aiWalletBalance.collectAsStateWithLifecycle()

        var showInfoDialog by remember { mutableStateOf(false) }
        var selectedStatusInfo by remember { mutableStateOf<SingleMessageStatus?>(null) }
        var billSourceList by remember { mutableStateOf<ListData?>(null) }

        var replyMessage by remember { mutableStateOf(false) }
        var hideOnSend by remember { mutableStateOf(false) }
        val singleSelectedSandesh by sandeshViewModel.selectedSandesh.collectAsState()
        var isSelectionMode by remember { mutableStateOf(false) }
        var selectedLedger by remember { mutableStateOf<ParsedLedgerSandesh?>(null) }
        var showBillPreview by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }
        var showRecorder by remember { mutableStateOf(false) }
        var mimeType by remember { mutableStateOf<String?>(null) }
        val selectedImageUris = remember { mutableStateListOf<Uri>() }
        var selectedDocumentUri by remember { mutableStateOf<Uri?>(null) }
    // ✅ Add this near selectedImageUris
        val selectedVideoUris = remember { mutableStateListOf<Uri>() }
        var showVideoPreview by remember { mutableStateOf(false) }
        var videoPreviewIndex by remember { mutableIntStateOf(0) }
        var videoPreviewUris by remember { mutableStateOf<List<String>>(emptyList()) }

        var showImagePreview by remember { mutableStateOf(false) }
        var previewIndex by remember { mutableIntStateOf(0) }
        var showCamera by remember { mutableStateOf(false) }
        var messageText by remember { mutableStateOf("") }
        val isBlocked = upbhogta?.isBlocked == true

        var showEmojiPicker by remember { mutableStateOf(false) }
        var showEmojiPickerFirst by remember { mutableStateOf(true) }
        var showSheet by remember { mutableStateOf(false) }
        var showSettlement by remember { mutableStateOf(false) }
        var showCreateBill by remember { mutableStateOf(false) }
        var showContactPicker by remember { mutableStateOf(false) }
        var showForward by remember { mutableStateOf(false) }
    // Track both the target message transaction ID and the actual list document structure
    // The Sandesh ID is always present, but ListData is null for new creations
    var activeListForEdit by remember { mutableStateOf<Pair<String, ListData?>?>(null) }
        var liveLocationSandeshId by remember { mutableStateOf<String?>(null) }
        val firstVisibleIndex by remember {
            derivedStateOf { listState.firstVisibleItemIndex }
        }
        var showThemeEditor by remember {
            mutableStateOf(false)
        }
        var selectedImageVisibility by remember {
            mutableStateOf(MediaVisibility.PRIVATE)
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
        var isSearchMode by remember { mutableStateOf(false) }
        var searchText by remember { mutableStateOf("") }

        val validUserName = userName.takeIf {
            it.isNotBlank() && it.lowercase() != "null"
        }

        // 👉 Place this new header block HERE
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val latestSandeshId by sandeshViewModel.latestSandeshId.collectAsStateWithLifecycle()

        val currentTime by produceState(
            initialValue = System.currentTimeMillis()
        ) {

            while (true) {

                value = System.currentTimeMillis()

                delay(60_000.milliseconds)
            }
        }

    var showSwipeDialog by remember { mutableStateOf(false) }

    BackHandler {
        navController.popBackStack()
    }
// Swipe hint (see the note below about a "loaded" flag)
    LaunchedEffect(parsedList) {
        delay(500.milliseconds)
        showSwipeDialog = parsedList.isEmpty()
    }
// Scroll to newest message, but not if the user is reading older ones
    LaunchedEffect(latestSandeshId) {
        if (latestSandeshId != null && listState.firstVisibleItemIndex <= 3) {
            listState.animateScrollToItem(0)
        }
    }
// Navigation guard
    LaunchedEffect(userNumber) {
        if (userNumber.isBlank()) {
            delay(500.milliseconds)
            if (userNumber.isBlank()) navController.popBackStack()
        }
    }


// Active user: runs on open and every screen-on; cleans up on screen-off or leave
    LifecycleStartEffect(userNumber) {
        if (userNumber.isNotBlank() && !isAi) {
            sandeshViewModel.setAsActiveUser(userNumber)
            sandeshViewModel.checkSingleProfile()
            sandeshViewModel.cleanNotification()
        }
        onStopOrDispose {
            sandeshViewModel.clearActiveUser()
            sandeshViewModel.pauseAllPlayback()   // or voicePlayer.pause()
        }
    }

// Refresh: every time the chat becomes interactive
    LifecycleResumeEffect(userNumber) {
        if (userNumber.isNotBlank() && !isAi) {
            sandeshViewModel.checkSingleProfile()
            sandeshViewModel.cleanNotification()
        }
        onPauseOrDispose { }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    val unseenIds by remember {
        derivedStateOf {
            parsedList
                .filter { it.sandeshStithi != SandeshStithi.DHRIST && !it.isSelf }
                .map { it.sandeshId }
        }
    }

    LaunchedEffect(unseenIds, lifecycleOwner) {
        if (unseenIds.isEmpty()  && isAi) return@LaunchedEffect

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            sandeshViewModel.processDhrist()
        }
    }


    LaunchedEffect(Unit) {
        sandeshViewModel.initPlayer(context)
        sandeshViewModel.initVideoPreviewPlayer(context)
    }


// Screenshot protection
    val isScreenshotProtected = (upbhogta?.autoDisappearMsg ?: 0) != 0
    val window = remember(context) { context.findActivity()?.window }

    DisposableEffect(isScreenshotProtected, window) {
        if (isScreenshotProtected) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (isScreenshotProtected) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
//    LaunchedEffect(upbhogta?.userNumber) {
//
//
//        delay(15000) // 10 ms delay between messages
//            val phoneUtil = PhoneNumberUtil.getInstance()
//
//            val randomMessages = listOf(
//                "Hello world",
//                "Check https://google.com",
//                "Visit github.com",
//                "Call me +919876543210",
//                "Nice day today",
//                "Openai.com is cool",
//                "Random message",
//                "www.youtube.com",
//                "😀",
//                "Testing message"
//            )
//
//            for (i in 2051 until 2099) {
//
//                val messageText = "${randomMessages.random()} #$i"
//
//                val spannable = SpannableString(messageText)
//
//                val hasUrl = LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
//
//                val hasPhone =
//                    phoneUtil.findNumbers(messageText, "IN")
//                        .iterator()
//                        .hasNext()
//
//                val isLink = hasUrl || hasPhone
//
//                val messageSamagri = JsonObject().apply {
//                    addProperty(DootawasMessage.PAATH, messageText)
//                    addProperty(DootawasMessage.IS_LINK, isLink)
//                }
//
//                sandeshViewModel.sendMessage(
//                    userNumbers = listOf(userNumber),
//                    message = messageSamagri
//                )
//
//                delay(10) // 10 ms delay between messages
//            }
//
//    }
// 👇 your existing one — unchanged


        fun globalScroll() {
            coroutineScope.launch {
                if (parsedList.isNotEmpty()) {
                    delay(150.milliseconds)
                    
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
                    val previewUri = sandeshViewModel.saveFirstPageAsImage(context, docUri)

                    val savedMedia = saveMediaToAppStorageNext(
                        context,
                        docUri,
                        MediaCategory.DOCUMENT,
                        selectedImageVisibility
                    ) ?: return@let

                    val docName = getFileName(context, docUri)
                    


                    if (replyMessage) {
                        if (singleSelectedSandesh != null) {
                            val replyJson = JsonObject().apply {
                                addProperty(
                                    DootawasMessage.ORIGINAL_SAMAGRI, singleSelectedSandesh!!.samagri
                                )  // Original message text
                                addProperty(
                                    DootawasMessage.ORIGINAL_MESSAGE_ID, singleSelectedSandesh!!.sandeshId
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
                                addProperty(DootawasMessage.MIME_TYPE, savedMedia.mimeType)
                                addProperty(DootawasMessage.DUPLICATE_URI, previewUri.toString())
                                addProperty(DootawasMessage.FILE_NAME, docName)
                                addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.localPath)

                                add(
                                    DootawasMessage.ORIGINAL_MESSAGE, replyJson
                                )  // Add the replyJson under the "REPLY" key
                            }
                            sandeshViewModel.sendReplyDocument(
                                userNumber = userNumber,
                                message = messageSamagri
                            )
                            // Reset selection
                            replyMessage = false
                            sandeshViewModel.clearReply()
                        }
                    } else {
                        val messageSamagri = JsonObject()
                        messageSamagri.addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                        messageSamagri.addProperty(DootawasMessage.MIME_TYPE, savedMedia.mimeType)
                        messageSamagri.addProperty(DootawasMessage.DUPLICATE_URI, previewUri.toString())
                        messageSamagri.addProperty(DootawasMessage.FILE_NAME, docName)
                        messageSamagri.addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.localPath)
                        sandeshViewModel.sendDocument(
                            userNumbers = listOf(userNumber),
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
            .background(dootawasColors.userHeaderBackground)   // background stays outside the padding
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
                if (!isAi) {
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
            }

            val insets = WindowInsets.navigationBars.asPaddingValues()
            val leftInset = insets.calculateLeftPadding(LayoutDirection.Ltr)
            val rightInset = insets.calculateRightPadding(LayoutDirection.Ltr)

            val navBarOnRight = rightInset > leftInset
            if (!isSearchMode) {

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
                    isNavBarOnRight = navBarOnRight,
                    modifier = Modifier
                        .align(alignment)
                        .height(headerHeight)
                        .navigationBarsPadding(),
                    onBackClick = {
                        navController.popBackStack()
                    },
                    isAi = sandeshViewModel.isAi,
                    onShowAiUsage = {showAiUsage = true},
                    userHeaderTextColor = dootawasColors.userHeaderText
                )
            } else {
                UserHeaderPortrait(
                    photoUrl = photoUrl,
                    validUserName = validUserName,
                    userNumber = userNumber,
                    onShowUserSettings = { showUserSettings = true },
                    onToggleSearch = { isSearchMode = true },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    dootawasColors = dootawasColors,
                    isAi = sandeshViewModel.isAi,
                    onShowAiUsage = {showAiUsage = true}
                )
            }
        }

            val sidePadding =if (isSearchMode) 6.dp else 58.dp

            val columnModifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)     // handles notch + status bar
                .windowInsetsPadding(WindowInsets.ime)             // ONLY when keyboard is open

            Column(modifier = columnModifier) {


//             🌟 Separate SearchBox below or anywhere you like
                SearchBox(
                    isSearchMode = isSearchMode,
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    onCloseSearch = { isSearchMode = false },
                    sandeshViewModel = sandeshViewModel,
                    containerColor = dootawasColors.userHeaderBackground
                )

                if (showUserSettings) {

                // Bottom sheet with user settings
                UserSettingsModal(
                    onDismiss = { showUserSettings = false },
                    userNumber = userNumber,
                    wallpaperUri = wallpaperUrl,
                    bakaya = upbhogta?.bakaya,
                    userName = userName,
                    about = userAbout,
                    onMediaSelected = { selectedType ->
                        navController.navigate("media_screen/$userNumber/${selectedType.value}")
                    },
                    onWallpaperSelected = { uri ->
                        sandeshViewModel.updateWallpaper(uri)
                    },
                    onRemoveWallpaper = {
                        sandeshViewModel.updateWallpaper(null)
                    },
                    onAppearanceClick = {
                        showUserSettings = false
                        showThemeEditor = true
                    },
                    onRefreshBalance = {
                        sandeshViewModel.fetchUserBalance()
                    },
                    revealSender = upbhogta?.revealSender ?: false,
                    onRevealPhoneNumber = {
                        sandeshViewModel.revealPhoneNumber()
                    },
                    autoDisappearMsg = upbhogta?.autoDisappearMsg ?: 0,
                    onDisappearingDurationSelected = { duration ->
                        sandeshViewModel.requestDisappearingMsg(duration)
                    },
                    onHideChatWithPasskey = { passkey,isMuted ->
                        // Call your ViewModel or repository function here
                        sandeshViewModel.updateHideKeyForUser(userNumber, passkey,isMuted)
                    }, dootawasColors = dootawasColors
                )
                }

                // ✅ Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isLandscape) {
                                if (navBarOnRight) Modifier.padding(
                                    end = sidePadding,
                                    start = 32.dp
                                )
                                else Modifier.padding(start = sidePadding, end = 32.dp)
                            } else {
                                Modifier.padding(top = sidePadding)
                            }
                        )
                        .padding(horizontal = 4.dp), // your usual inner horizontal padding
                    reverseLayout = true,
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(
                        items = parsedList,
                        key = { it.sandeshId },
                        contentType = { item ->
                            when (item) {
                                is ParsedTextSandesh -> "text"
                                is ParsedImageSandesh -> "image"
                                is ParsedAudioSandesh -> "audio"
                                is ParsedContactSandesh -> "contact"
                                is ParsedDocumentSandesh -> "document"
                                is ParsedLocationSandesh -> "location"
                                is ParsedVideoSandesh -> "video"
                                is ParsedUniversalSandesh -> "universal"
                                is ParsedLedgerSandesh -> "ledger"
                                is ParsedListSandesh -> "list"
                                is ParsedDisappearingSandesh -> "disappearing"
                                is ParsedDateHeaderSandesh -> "date_header"   // NEW
                                is ParsedAiSandesh -> "ai_sandesh"
                            }
                        }
                    ) { parsedItem ->
                        val isSelectionModeActive by remember {
                            derivedStateOf { isSelectionMode }
                        }

                        when (parsedItem) {
                            is ParsedTextSandesh -> {
                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }

                                // 🌟 Switch layout dynamically based on preferences
                                if (useModernStyle && !isAi) {
                                    ModernMessageBubble(
                                        isSelf = parsedItem.isSelf,
                                        data = parsedItem.paath,
                                        formattedTime = parsedItem.formattedTime,
                                        timeLong = parsedItem.time,
                                        formattedDate = parsedItem.formattedDate,
                                        msgType = parsedItem.msgType,
                                        linkUri = parsedItem.linkUri,
                                        linkTitle = parsedItem.linkTitle,
                                        replyOriginalMessage = parsedItem.replyOriginalMessage,
                                        isSelected = isSelectedItem,
                                        isSelectionMode = isSelectionModeActive,
                                        sandeshStithi = parsedItem.sandeshStithi,
                                        fontSize = fontSize,
                                        isHighlighted = isHighlightedItem,
                                        onLongPress = {
                                            if (!isSelectionMode) isSelectionMode = true
                                            selectedMessages.add(sandeshId)
                                        },
                                        onToggleSelection = {
                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }
                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        },
                                        onReplyClick = { id -> scrollToMessage(id) },
                                        borderMode = borderMode,
                                        dootawasColors = dootawasColors,
                                        bubbleType = parsedItem.bubbleType,
                                        onReplySlide = {
                                            sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                            replyMessage = true
                                            isSelectionMode = false
                                            selectedMessages.clear()
                                        }
                                    )
                                } else {
                                    // 🌟 Fallback to your classic/original bubble composable
                                    MessageBubble(
                                        isSelf = parsedItem.isSelf,
                                        data = parsedItem.paath,
                                        formattedTime = parsedItem.formattedTime,
                                        formattedDate = parsedItem.formattedDate,
                                        msgType = parsedItem.msgType,
                                        linkUri = parsedItem.linkUri,
                                        linkTitle = parsedItem.linkTitle,
                                        replyOriginalMessage = parsedItem.replyOriginalMessage,
                                        isSelected = isSelectedItem,
                                        isSelectionMode = isSelectionModeActive,
                                        sandeshStithi = parsedItem.sandeshStithi,
                                        fontSize = fontSize,
                                        isHighlighted = isHighlightedItem,
                                        onLongPress = {
                                            if (!isSelectionMode) isSelectionMode = true
                                            selectedMessages.add(sandeshId)
                                        },
                                        onToggleSelection = {
                                            if (selectedMessages.contains(sandeshId)) {
                                                selectedMessages.remove(sandeshId)
                                            } else {
                                                selectedMessages.add(sandeshId)
                                            }
                                            if (selectedMessages.isEmpty()) {
                                                isSelectionMode = false
                                            }
                                        },
                                        onReplyClick = { id -> scrollToMessage(id) },
                                        borderMode = borderMode,
                                        dootawasColors = dootawasColors,
                                        onReplySlide = {
                                            sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                            replyMessage = true
                                            isSelectionMode = false
                                            selectedMessages.clear()
                                        },
                                        // Note: If your old MessageBubble doesn't accept 'bubbleType' (the tail positioning),
                                        // you can safely leave it out here!
                                    )
                                }
                            }
                            is ParsedImageSandesh -> {
                                val sandeshId = parsedItem.sandeshId  // ✅ ADD this first
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }
                                    ImageBubble(
                                        isSelf = parsedItem.isSelf,
                                        imageUris = parsedItem.imageUris,
                                        blurFileUri = parsedItem.blurUri,
                                        downloadPercent = parsedItem.downloadPercent,
                                        downloadSize = parsedItem.downloadSize,
                                        paath = parsedItem.paath,
                                        timeLong = parsedItem.time,
                                        formattedTime = parsedItem.formattedTime,
                                        isSelected = isSelectedItem,  // ✅ change from selectedMessages.contains(parsedItem.sandeshId)
                                        isHighlighted = isHighlightedItem,  // ✅ change from parsedItem.sandeshId == highlightedMessageId
                                        onLongPress = {
                                            if (!isSelectionMode) isSelectionMode = true
                                            selectedMessages.add(parsedItem.sandeshId)
                                        },
                                        onImageClick = { uris, clickedIndex ->

                                            // 🚫 Ignore preview during selection mode
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
                                            
                                            sandeshViewModel.downloadFile(
                                                sandeshId = parsedItem.sandeshId,
                                                userNumber = userNumber
                                            )

                                        },
                                        onCancelClick = {
                                            sandeshViewModel.resetDownloadPercentage(parsedItem.sandeshId)
                                        },
                                        onReplyClick = { messageIdReply ->
                                            
                                            scrollToMessage(messageIdReply)
                                        },
                                        sandeshStithi = parsedItem.sandeshStithi,
                                        replyOriginalMessage = parsedItem.replyOriginalMessage,
                                        imageLoader = sandeshViewModel.imageLoader,
                                        dootawasColors = dootawasColors,
                                        onReplySlide = {
                                            sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                            replyMessage = true
                                            isSelectionMode = false
                                            selectedMessages.clear()
                                        }
                                    )

                            }

                            is ParsedVideoSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }

                                VideoBubble(
                                    sandeshId = sandeshId,
                                    isSelf = parsedItem.isSelf,
                                    sandeshType = parsedItem.sandeshType,
                                    paath = parsedItem.paath,
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
                                        sandeshViewModel.downloadFile(
                                            sandeshId = sandeshId,
                                            userNumber = userNumber
                                        )
                                    },

                                    onReplyClick = { id ->
                                        scrollToMessage(id)
                                    },

                                    onCancelDownload = {
                                        sandeshViewModel.resetDownloadPercentage(sandeshId)
                                    },
                                    dootawasColors = dootawasColors,
                                    videoUris = parsedItem.videoUris,
                                    previewUris = parsedItem.previewUris,
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }
                            is ParsedAudioSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val savedFileUri = parsedItem.savedFileUri
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }
                                AudioBubble(
                                    sandeshId = sandeshId,
                                    savedFileUri = savedFileUri,
                                    isPlaying = sandeshViewModel.isPlaying,
                                    duration = parsedItem.duration,
                                    downloadPercent = parsedItem.downloadPercent,
                                    downloadSize = parsedItem.downloadSize,
                                    isSelf = parsedItem.isSelf,
                                    sandeshStithi = parsedItem.sandeshStithi,
                                    timeLong = parsedItem.time,
                                    formattedTime = parsedItem.formattedTime,
                                    replyOriginalMessage = parsedItem.replyOriginalMessage,

                                    exoPlayer = sandeshViewModel.exoPlayer,
                                    currentlyPlayingId = sandeshViewModel.currentlyPlayingId,

                                    onPlayClick = {
                                        savedFileUri?.let {
                                            sandeshViewModel.togglePlay(sandeshId, it)
                                        }
                                    },

                                    isSelectionMode = isSelectionModeActive,  // ✅ derived read
                                    isSelected = isSelectedItem,

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
                                        sandeshViewModel.downloadFile(
                                            sandeshId = sandeshId,
                                            userNumber = userNumber
                                        )
                                    },

                                    onReplyClick = { id ->
                                        scrollToMessage(id)
                                    },

                                    isHighlighted = isHighlightedItem,
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }
                            is ParsedDocumentSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }
                                DocumentBubble(
                                    sandeshId = sandeshId,
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

                                    isSelectionMode = isSelectionModeActive,  // ✅ derived read
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
                                        sandeshViewModel.downloadFile(
                                            sandeshId = sandeshId,
                                            userNumber = userNumber
                                        )
                                    },

                                    onReplyClick = { id ->
                                        scrollToMessage(id)
                                    },

                                    onCancelDownload = {
                                        sandeshViewModel.resetDownloadPercentage(sandeshId)
                                    },
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }
                            is ParsedContactSandesh -> {

                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }
                                ContactBubble(
                                    sandeshId = sandeshId,
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
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }

                            is ParsedLocationSandesh -> {
                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }

                                LocationBubble(
                                    sandeshId = sandeshId,
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

                                    isSelectionMode = isSelectionModeActive,  // ✅ derived read
                                    isSelected = isSelectedItem,
                                    isHighlighted = isHighlightedItem,

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
                                        sandeshViewModel.downloadFile(
                                            sandeshId = sandeshId,
                                            userNumber = userNumber
                                        )
                                    },

                                    onReplyClick = { id ->
                                        scrollToMessage(id)
                                    },
                                    isLive = parsedItem.isLive,
                                    onStopLiveLocation = {
                                        sandeshViewModel.stopLiveLocationSharing(
                                            context = context,
                                            sandeshId = parsedItem.sandeshId,

                                        )
                                    },
                                    liveUntil = parsedItem.liveUntil,
                                    isStopped = parsedItem.isStopped,
                                    currentTime = currentTime,
                                    dootawasColors = dootawasColors,
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }

                            is ParsedUniversalSandesh -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.85f
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Icon(
                                            imageVector = parsedItem.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = parsedItem.paath,
                                            fontSize = fontSize.sp,
                                            lineHeight = fontSize.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is ParsedLedgerSandesh -> {
                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }

                                LedgerBubble(
                                    item = parsedItem,
                                    dootawasColors = dootawasColors,
                                    currentBalance = upbhogta?.bakaya ?: 0.0,
                                    isDark = isDark,
                                    isSelected = isSelectedItem,
                                    isHighlighted = isHighlightedItem,
                                    isSelectionMode = isSelectionModeActive,
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
                                    onAccept = { id, resolved ->
                                        sandeshViewModel.acceptLedger(id,resolved)
                                    },
                                    onReject = { id,resolved ->
                                        sandeshViewModel.rejectLedger(id,resolved)
                                    },
                                    onClick = { parsed ->
                                        selectedLedger = parsed
                                        showBillPreview = true
                                    },
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    }
                                )
                            }
                            is ParsedListSandesh -> {
                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }

                                ListBubble(
                                    data = parsedItem,
                                    replyOriginalMessage = parsedItem.listMessage.current.let {
                                        // Replace with your system's extraction logic if you attach replyJson to ParsedListSandesh
                                        null
                                    },
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
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    },
                                    onAcceptClick = { id ->
                                        // Trigger your update verification hook
                                        sandeshViewModel.acceptProposedListUpdate(id, userNumber)
                                    },
                                    onRejectClick = { id ->
                                        // Trigger your update rejection hook
                                        sandeshViewModel.rejectProposedListUpdate(id, userNumber)
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
                                            activeListForEdit = Pair(sandeshId, parsedItem.displayList)
                                            // Open full dashboard view screen
//                                            navController.navigate("list_detail_screen/${sandeshId}")
                                        }
                                    }
                                )
                            }

                            is ParsedDisappearingSandesh -> {
                                DisappearingRequestBubble(
                                    title = parsedItem.title,
                                    message = parsedItem.message,
                                    formattedTime = parsedItem.formattedTime,
                                    isSelf = parsedItem.isSelf,
                                    accepted = parsedItem.accepted,
                                    onAccept = {
                                        sandeshViewModel.respondToDisappearingRequest(
                                            sandeshId = parsedItem.sandeshId,
                                            accepted = true,
                                            duration = parsedItem.duration
                                        )
                                    },
                                    onReject = {
                                        sandeshViewModel.respondToDisappearingRequest(
                                            sandeshId = parsedItem.sandeshId,
                                            accepted = false,
                                            duration = parsedItem.duration
                                        )
                                    }
                                )
                            }
                            is ParsedDateHeaderSandesh -> {
                                DateHeader(item = parsedItem,dootawasColors = dootawasColors)
                            }

                            is ParsedAiSandesh -> {
                                val sandeshId = parsedItem.sandeshId
                                val isSelectedItem by remember(sandeshId) {
                                    derivedStateOf { selectedMessages.contains(sandeshId) }
                                }
                                val isHighlightedItem by remember(sandeshId) {
                                    derivedStateOf { sandeshId == highlightedMessageId }
                                }


                                AiMessageBubble(
                                    item = parsedItem,
                                    isSelected = isSelectedItem,
                                    isSelectionMode = isSelectionModeActive,
                                    isHighlighted = isHighlightedItem,
                                    fontSize = fontSize,
                                    borderMode = borderMode,
                                    dootawasColors = dootawasColors,
                                    onLongPress = {
                                        if (!isSelectionMode) isSelectionMode = true
                                        selectedMessages.add(sandeshId)
                                    },
                                    onToggleSelection = {
                                        if (selectedMessages.contains(sandeshId)) {
                                            selectedMessages.remove(sandeshId)
                                        } else {
                                            selectedMessages.add(sandeshId)
                                        }
                                        if (selectedMessages.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    },
                                    onReplySlide = {
                                        sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                        replyMessage = true
                                        isSelectionMode = false
                                        selectedMessages.clear()
                                    },
                                    onAddCredits = {
                                        // open your recharge screen here, for example:
                                        // navController.navigate("ai_recharge")
                                    }
                                )
                            }
                        }
                    }
                }


                val isTyping by sandeshViewModel.isTyping

                if (isTyping) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
//                            .padding(start = 12.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(R.drawable.typing)
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = "Typing indicator",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .height(14.dp)     // control height only
                                .width(60.dp)
                                .clip(RectangleShape)
                        )

                    }
                }

//                if (replyMessage) {
//
//                    val jsonObject = try {
//                        JsonParser.parseString(singleSelectedSandesh!!.samagri).asJsonObject
//                    } catch (e: Exception) {
//                        JsonObject() // Handle invalid JSON safely
//                    }
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color(0xFFEEEEEE)) // Light gray background
//                            .padding(8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            Icons.Default.Info,
//                            contentDescription = "Reply",
//                            tint = Color.Gray,
//                            modifier = Modifier.padding(end = 8.dp)
//                        )
//
//                        when (singleSelectedSandesh?.let { SandeshType.fromInt(it.sandeshType) }) {
//                            SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE -> {
//                                val replyText =
//                                    jsonObject.get(DootawasMessage.PAATH)?.asString.orEmpty()
//                                Text(
//                                    text = replyText.take(30) + if (replyText.length > 40) "..." else "",
//                                    modifier = Modifier.weight(1f),
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis,
//                                    fontSize = 14.sp,
//                                    color = Color.Black
//                                )
//                            }
//
//                            SandeshType.IMAGE, SandeshType.REPLY_IMAGE -> {
//                                val replyImage =
//                                    jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
//                                replyImage?.let { imageUrl ->
//                                    AsyncImage(
//                                        model = imageUrl,
//                                        contentDescription = "Reply Image",
//                                        modifier = Modifier
//                                            .size(40.dp)
//                                            .clip(RoundedCornerShape(8.dp)) // Clip first
//                                            .border(
//                                                1.dp,
//                                                Color.Gray,
//                                                RoundedCornerShape(8.dp)
//                                            ) // Add border with same shape
//                                            .background(Color.LightGray),
//                                        contentScale = ContentScale.Crop
//                                    )
//                                }
//                            }
//
//                            SandeshType.VIDEO, SandeshType.REPLY_VIDEO -> {
//                                val replyImage =
//                                    jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
//                                replyImage?.let { imageUrl ->
//
//                                    Box(
//                                        modifier = Modifier
//                                            .size(40.dp)
//                                            .clip(RoundedCornerShape(8.dp))
//                                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
//                                    ) {
//                                        AsyncImage(
//                                            model = imageUrl,
//                                            contentDescription = "Reply Image",
//                                            modifier = Modifier
//                                                .matchParentSize()
//                                                .background(Color.LightGray),
//                                            contentScale = ContentScale.Crop
//                                        )
//                                        Icon(
//                                            imageVector = Icons.Default.PlayArrow,
//                                            contentDescription = "Reply Video",
//                                            modifier = Modifier
//                                                .align(Alignment.Center) // Center the icon on the image
//                                                .size(20.dp),
//                                            tint = Color.White // White for better visibility
//                                        )
//                                    }
//                                }
//                            }
//
//                            SandeshType.AUDIO -> {
//                                Icon(
//                                    imageVector = Icons.Default.AddCircle,
//                                    contentDescription = "Reply Audio",
//                                    modifier = Modifier.size(24.dp),
//                                    tint = Color.Black
//                                )
//                            }
//
//                            else -> {}
//                        }
//                        Spacer(modifier = Modifier.weight(1f)) // Pushes close button to the end
//                        IconButton(onClick = { replyMessage = false }) {
//                            Icon(
//                                Icons.Default.Close,
//                                contentDescription = "Cancel Reply",
//                                tint = Color.Red
//                            )
//                        }
//                    }
//                }
                if (isSelectionMode) {

                    //For forward we check showContactPicker
                    if (!showForward) {

                        val clipboard = LocalClipboard.current
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()

                        MessageSelectionBar(
                            selectedCount = selectedMessages.size,
                            onCopy = {
                                val selectedId = selectedMessages.firstOrNull() ?: return@MessageSelectionBar

                                scope.launch {
                                    // 1. Get the raw JSON string (samagri)
                                    val samagriJsonString = sandeshViewModel.getSingleMessage(selectedId, userNumber)

                                    if (!samagriJsonString.isNullOrEmpty()) {

                                        // 2. Parse the JSON string and extract the "paath" key
                                        val textToCopy = try {
                                            JsonParser.parseString(samagriJsonString)
                                                .asJsonObject
                                                .get("paath") // Get the "paath" JsonElement
                                                .asString      // Convert it to a String
                                        } catch (e: Exception) {
                                            // Log or handle parsing errors (e.g., if 'paath' is missing or not a string)
                                            Log.e("CopyError", "Failed to parse samagri JSON: ${e.message}")
                                            return@launch // Stop execution if parsing failed
                                        }

                                        // 3. Copy the extracted "paath" text using the modern API
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("Copied Paath Text", textToCopy))
                                        )

                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }

                                    isSelectionMode = false
                                    selectedMessages.clear()
                                }
                            },
                            onInfo = {
                                val selectedId = selectedMessages.firstOrNull() ?: return@MessageSelectionBar

                                scope.launch {
                                    // Call VM suspend method to get parsed JSON
                                    val status = sandeshViewModel.getSingleMessageStatusInfo(
                                        userNumber = userNumber,
                                        sandeshId = selectedId
                                    )

                                    selectedStatusInfo = status
                                    showInfoDialog = true
                                    isSelectionMode = false

                                }
                            }
                            ,
                            onReply = {
                                val selectedId = selectedMessages.firstOrNull()
                                val sandeshId = parsedList.find { it.sandeshId == selectedId }?.sandeshId
                                if (sandeshId != null) {
                                    sandeshViewModel.fetchSingleSandesh(sandeshId, userNumber)
                                    replyMessage = true
                                    isSelectionMode = false
                                    selectedMessages.clear()
                                }
                            },
                            onForward = {
                                if (isScreenshotProtected) {
                                    Toast.makeText(context, "Forward protected", Toast.LENGTH_SHORT).show()
                                } else {
                                    showForward = true
                                    showEmojiPickerFirst = false
                                }
                            },
                            onDelete = {
                                showDialog = true
                                isSelectionMode = false},
                            showReactionBar = selectedMessages.size == 1 && showEmojiPickerFirst,

                            quickReactions = quickReactions.take(maxVisibleEmojis),

                            onReactionSelected = { emoji ->
                                val selectedId = selectedMessages.firstOrNull()
                                val sandeshId = parsedList.find {
                                    it.sandeshId == selectedId
                                }?.sandeshId

                                if (sandeshId != null) {
                                    sandeshViewModel.reactToMsg(emoji, sandeshId)
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

                    } else{
//                        showOverlay.value = false
                        ContactPickerWithHomeScreen(
                            onContactsSelected = { numbers ->
                                sandeshViewModel.processForward(selectedMessages.toList(), numbers)
                                selectedMessages.clear()
                                showContactPicker = false
                                isSelectionMode = false
                                showForward = false
                                showEmojiPickerFirst = true
                            },
                            onBack = {
                                selectedMessages.clear()
                                showContactPicker = false
                                isSelectionMode = false
                                showForward = false
                                showEmojiPickerFirst = true
                            }
                        )
                    }
                } else {
                    MessageInputBar(
                        isBlocked = isBlocked,
                        replyMessage = replyMessage,
                        singleSelectedSandesh = singleSelectedSandesh,
                        enterToSendEnabled = enterToSendEnabled,
                        dootawasColors = dootawasColors,
                        onSendText = { text,duration ->
                            // your existing sendMessage() logic — but text is passed in now
                            val spannable = SpannableString(text)
                            val hasUrl = LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
                            val hasPhone = PhoneNumberUtil.getInstance()
                                .findNumbers(text, "IN").iterator().hasNext()
                            val messageSamagri = JsonObject().apply {
                                addProperty(DootawasMessage.PAATH, text)
                                addProperty(DootawasMessage.IS_LINK, hasUrl || hasPhone)
                                addProperty(DootawasMessage.DISAPPEAR_DURATION, duration)
                            }
                            sandeshViewModel.sendMessage(
                                userNumbers = listOf(userNumber),
                                message = messageSamagri
                            )
                        },
                        onSendReply = { text,duration ->
                            if (singleSelectedSandesh != null) {
                                val selectedSamagriJson = Gson().fromJson(
                                    singleSelectedSandesh!!.samagri,
                                    JsonObject::class.java
                                )

                                selectedSamagriJson.remove(DootawasMessage.ORIGINAL_MESSAGE)

                                val replyJson = JsonObject().apply {
                                    addProperty(
                                        DootawasMessage.ORIGINAL_SAMAGRI,
                                        selectedSamagriJson.toString()
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

                                val spannable = SpannableString(text)
                                val hasUrl = LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
                                val hasPhone = PhoneNumberUtil.getInstance()
                                    .findNumbers(text, "IN").iterator().hasNext()

                                val messageSamagri = JsonObject().apply {
                                    addProperty(DootawasMessage.PAATH, text)
                                    addProperty(DootawasMessage.IS_LINK, hasUrl || hasPhone)
                                    add(DootawasMessage.ORIGINAL_MESSAGE, replyJson)
                                    addProperty(DootawasMessage.DISAPPEAR_DURATION, duration)
                                }

                                sandeshViewModel.replyMessage(
                                    userNumber = userNumber,
                                    message = messageSamagri
                                )

                                replyMessage = false
                                sandeshViewModel.clearReply()
                            }
                        },
                        onCameraClick = { if (!isAi)  showCamera = true },
                        onRecorderClick = { showRecorder = true },
                        onAttachClick = { showSheet = true },
                        onCancelReply = {
                            replyMessage = false
                            sandeshViewModel.clearReply()
                        },
                        onTyping = {
                            sandeshViewModel.sendTyping(userNumber)
                        },
                        defaultDisappearSeconds = upbhogta?.autoDisappearMsg ?: 0,
                        sandeshViewModel = sandeshViewModel,
                        draftMessage = upbhogta?.draftMessage,
                        userNumber = userNumber,
                        lastOnline = upbhogta?.lastOnline ?: 0L,
                    )
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(dootawasColors.userHeaderBackground)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = overlayDate!!,
                        color = dootawasColors.userHeaderText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            val audioPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { pickedUri ->
                    val mime = context.contentResolver.getType(pickedUri) ?: ""
                    if (!mime.startsWith("audio/")) {
                        Toast.makeText(context, "Selected file is not audio", Toast.LENGTH_LONG).show()
                        return@let
                    }

                    val savedMedia = saveMediaToAppStorageNext(
                        context,
                        pickedUri,
                        MediaCategory.AUDIO,
                        selectedImageVisibility
                    ) ?: return@let


                    val durationMs = getAudioDurationMs(context, savedMedia.localPath) ?: 0L
                    // Now send — same logic as recorder
                    

                    if (replyMessage) {
                        if (singleSelectedSandesh != null) {
                            val replyJson = JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_SAMAGRI, singleSelectedSandesh!!.samagri)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, singleSelectedSandesh!!.sandeshId)
                                addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, singleSelectedSandesh!!.sandeshType)
                            }

                            val messageSamagri = JsonObject().apply {
                                addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                                addProperty(DootawasMessage.MIME_TYPE, ".m4a")  // or detect real mime
                                add(DootawasMessage.ORIGINAL_MESSAGE, replyJson)
                                addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                                addProperty(DootawasMessage.DURATION, durationMs)
                                addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                                addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.fileSize)
                            }

                            sandeshViewModel.sendReplyAudio(
                                userNumber = userNumber,
                                message = messageSamagri
                            )

                            // Reset
                            replyMessage = false
                            sandeshViewModel.clearReply()
                        }
                    } else {
                        val messageSamagri = JsonObject().apply {
                            addProperty(DootawasMessage.ORIGINAL_URI, savedMedia.localPath)
                            addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                            addProperty(DootawasMessage.DURATION, durationMs)
                            addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                            addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                            addProperty(DootawasMessage.DOWNLOAD_SIZE,savedMedia.fileSize)
                        }

                        sandeshViewModel.sendAudio(
                            userNumbers = listOf(userNumber),
                            message = messageSamagri
                        )
                    }
                }
            }


            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia(10) // ✅ limit = 10
            ) { uris: List<Uri> ->

                if (uris.isNotEmpty()) {

                    selectedImageVisibility =
                        MediaVisibility.PRIVATE
                    selectedImageUris.clear() // reset old selection
                    selectedImageUris.addAll(uris)

                    uris.forEach { uri ->
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {}
                    }
                }
            }

            if (selectedImageUris.isNotEmpty()) {


                ImageEditorScreen(
                    initialRecipients = remember(userNumber) {
                        listOf(
                            PickedContact(
                                userNumber = userNumber,
                                userName = upbhogta?.contactName ?: upbhogta?.onlineName
                                ?: userNumber, // adjust to your UI item's fields
                                profileUri = upbhogta?.photoUrl,
                                isGroup = false // true when this chat is a group, and pass showDisappearButton = false
                            )
                        )
                    },
                    sourceUris =  selectedImageUris.toList(),
                    defaultDisappearSeconds = upbhogta?.autoDisappearMsg ?: 0,
                    showDisappearButton = true, // false in groups, as your old dialog did

                    onCancel = {
                        selectedImageUris.clear()
                    },

                    onDone = { files, message, disappearDuration, recipients ->
                        sandeshViewModel.sendImages(
                            context = context.applicationContext,
                            files = files,
                            caption = message,
                            disappearSeconds = disappearDuration,
                            recipients = recipients,
                            visibility = selectedImageVisibility,
                            currentChatId = userNumber,
                            currentChatIsGroup = false,
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
                            sandeshViewModel.clearReply()
                        }
                        selectedImageUris.clear()
                        globalScroll()
                    }
                )
            }

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
                        initialRecipients = remember(userNumber) {
                            listOf(
                                PickedContact(
                                    userNumber = userNumber,
                                    userName = upbhogta?.contactName ?: upbhogta?.onlineName ?: userNumber,
                                    profileUri = upbhogta?.photoUrl,
                                    isGroup = false
                                )
                            )
                        },
                        defaultDisappearSeconds = upbhogta?.autoDisappearMsg ?: 0,
                        showDisappearButton = true,
                        maxDurationMs = 0L,
                        onCancel = { selectedVideoUris.clear() },
                        onDone = { files, message, disappearSeconds, recipients ->
                            sandeshViewModel.sendVideos(
                                context = context.applicationContext,
                                files = files,
                                caption = message,
                                disappearSeconds = disappearSeconds,
                                recipients = recipients,
                                visibility = selectedImageVisibility,
                                currentChatId = userNumber,
                                currentChatIsGroup = false,
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
                                sandeshViewModel.clearReply()
                            }
                            selectedVideoUris.clear()
                            globalScroll()
                        }
                    )
                }
            }

            var showLocationScreen by remember { mutableStateOf(false) }
            if (showSheet && !isBlocked) {
                if (isAi) {
                    Toast.makeText(
                        context,
                        "AI features unlocking soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                    showSheet = false
                }else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) { detectTapGestures { showSheet = false } }
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

                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onVideoClick = {
                                showSheet = false
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )
                            },
                            onAudioClick = {
                                audioPickerLauncher.launch("audio/*")
                                showSheet = false
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
                            onBillClick = {
                                showSheet = false
                                showSettlement = true   // step 1 — open settlement sheet
                            },
                            onListClick = {
                                showSheet = false
                                activeListForEdit = Pair(UUID.randomUUID().toString(), null)
                            },
                            userHeaderBackground = dootawasColors.userHeaderBackground,
                        )
                    }
                }

            }

            if (showSettlement) {
                SettlementSheet(
                    contactId = userNumber,
                    contactName = userName.takeUnless { it.isBlank() } ?: userNumber,
                    currentBalance = upbhogta?.bakaya ?: 0.0,
                    darkMode = true,
                    onDismiss = { showSettlement = false },
                    onCreateBill = {
                        showSettlement = false
                        showCreateBill = true
                    },
                    onSettled = { ledgerJson ->
                        showSettlement = false
                        sandeshViewModel.sendLedgerSandesh(listOf(userNumber), ledgerJson)
                    }
                )
            }

            if (showBillPreview && selectedLedger != null) {
                BillPreviewScreen(
                    ledgerJson = selectedLedger!!.dataJson,
                    darkMode = isDark,
                    onBack = { showBillPreview = false },
                    onShare = { text ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Bill"))
                    }
                )
            }

            if (showLocationScreen) {
                ModernMapPicker(
                    onLocationSelected = { location, savedMedia, isLive, expiryTime ->


                        val mediaPath =
                            savedMedia?.localPath ?: ""

                        val mediaSize =
                            savedMedia?.fileSize ?: 0L

                        val mimeType =
                            savedMedia?.mimeType ?: "image/jpeg"

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

                                val messageSamagri =
                                    JsonObject().apply {



                                        location.let { latLng ->

                                            addProperty(
                                                DootawasMessage.LATITUDE,
                                                latLng.latitude
                                            )

                                            addProperty(
                                                DootawasMessage.LONGITUDE,
                                                latLng.longitude
                                            )
                                        }
                                        add(
                                            DootawasMessage.ORIGINAL_MESSAGE,
                                            replyJson
                                        )
                                        addProperty(
                                            DootawasMessage.IS_LIVE,
                                            isLive
                                        )
                                        addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                                        addProperty(
                                            DootawasMessage.ORIGINAL_URI,
                                            mediaPath
                                        )
                                        addProperty(
                                            DootawasMessage.DOWNLOAD_SIZE,
                                            mediaSize
                                        )
                                        addProperty(
                                            DootawasMessage.MIME_TYPE,
                                            mimeType
                                        )
                                        if (expiryTime != null) {
                                            addProperty(
                                                DootawasMessage.LIVE_UNTIL,
                                                expiryTime
                                            )
                                        } else {
                                            addProperty(
                                                DootawasMessage.DOWNLOAD_SIZE,
                                                ""
                                            )
                                        }
                                    }
                                sandeshViewModel.replyLocation(
                                    userNumber,
                                    messageSamagri,
                                    context
                                )
                            }

                            replyMessage = false
                            sandeshViewModel.clearReply()

                        } else {

                            val messageSamagri =
                                JsonObject().apply {

                                    addProperty(
                                        DootawasMessage.ORIGINAL_URI,
                                        mediaPath
                                    )
                                    addProperty(
                                        DootawasMessage.DOWNLOAD_SIZE,
                                        mediaSize
                                    )
                                    addProperty(
                                        DootawasMessage.MIME_TYPE,
                                        mimeType
                                    )
                                    addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                                    location.let { latLng ->

                                        addProperty(
                                            DootawasMessage.LATITUDE,
                                            latLng.latitude
                                        )

                                        addProperty(
                                            DootawasMessage.LONGITUDE,
                                            latLng.longitude
                                        )
                                    }

                                    addProperty(
                                        DootawasMessage.IS_LIVE,
                                        isLive
                                    )

                                    if (expiryTime != null) {

                                        addProperty(
                                            DootawasMessage.LIVE_UNTIL,
                                            expiryTime
                                        )

                                    } else {

                                        addProperty(
                                            DootawasMessage.LIVE_UNTIL,
                                            ""
                                        )
                                    }

                                }

                            sandeshViewModel.sendLocation(
                                userNumbers = listOf(userNumber),
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

            if (showInfoDialog && selectedStatusInfo != null) {
                SingleMessageInfoDialog (
                    status = selectedStatusInfo!!,
                    messageId = selectedMessages.firstOrNull() ?: return,
                    sandeshViewModel = sandeshViewModel,
                    onDismiss = { showInfoDialog = false
                        isSelectionMode = false
                        selectedMessages.clear()
                    },
                    formatDateTime = { t -> sandeshViewModel.formatDateTime(t) }
                )
            }

            if (showContactPicker) {
                ContactPickerScreen(
                    onContactsSelected = { contactsJson ->
                         // <- got JSON string of selected contacts
                        contactsJson.addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
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
                                sandeshViewModel.sendReplyContact(userNumber, contactsJson)
                                // Reset selection
                                replyMessage = false
                                sandeshViewModel.clearReply()
                            }
                        } else {
                            sandeshViewModel.sendContact(
                                userNumbers = listOf(userNumber),
                                contactsJson
                            )
                        }
                        showContactPicker = false
                    },
                    onBack = {
                        showContactPicker = false
                    }
                )
            }

            if (showDialog) {
                ConfirmationDialog(
                    message = "Are you sure you want to delete this message?",
                    onConfirm = { deleteBoth ->
                        if (selectedMessages.isNotEmpty()) {
                            val messagesToDelete = selectedMessages.toSet()

                            sandeshViewModel.deleteSelectedMessages(
                                messagesToDelete,
                                userNumber,
                                deleteBoth
                            )
                            selectedMessages.clear()
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
                    onDismiss = { showImagePreview = false },
                    disappearSeconds = upbhogta?.autoDisappearMsg ?: 0,
                    onEdit = { uri ->
                        selectedImageUris.clear()
                        selectedImageUris.add(uri)
                        showImagePreview = false      // preview closes, the editor opens
                    }
                )            }

            if (showRecorder) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
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
                                        addProperty(DootawasMessage.ORIGINAL_URI, audioUri.localPath)
                                        addProperty(DootawasMessage.DOWNLOAD_SIZE, audioUri.fileSize)
                                        addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                                        add(
                                            DootawasMessage.ORIGINAL_MESSAGE, replyJson
                                        )  // Add the replyJson under the "REPLY" key
                                        addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                                        addProperty(DootawasMessage.DURATION, durationMs)
                                        addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                                    }
                                    sandeshViewModel.sendReplyAudio(
                                        userNumber = userNumber,
                                        message = messageSamagri
                                    )
                                    // Reset selection
                                    replyMessage = false
                                    sandeshViewModel.clearReply()
                                }
                            } else {
                                val messageSamagri = JsonObject()
                                messageSamagri.addProperty(
                                    DootawasMessage.ORIGINAL_URI,
                                    audioUri.localPath
                                )
                                messageSamagri.addProperty(DootawasMessage.MIME_TYPE, ".m4a")
                                messageSamagri.addProperty(DootawasMessage.DURATION, durationMs)
                                messageSamagri.addProperty(DootawasMessage.DOWNLOAD_PERCENT, 0)
                                messageSamagri.addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                                messageSamagri.addProperty(DootawasMessage.DOWNLOAD_SIZE, audioUri.fileSize)
                                sandeshViewModel.sendAudio(
                                    userNumbers = listOf(userNumber),
                                    message = messageSamagri
                                )
                            }
                        },
                        onDismiss = { showRecorder = false },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }



//
            if (showCamera) {
                // Separate permission state from the showCamera toggle
                var isPermissionGranted by remember { mutableStateOf(false) }

                PermissionHandler(
                    onAllPermissionsGranted = {
                        isPermissionGranted = true
                    },
                    onPermissionsDenied = { deniedPermissions ->
                        showCamera = false
                    }
                )

                // Mount CameraScreen ONLY when permissions are explicitly confirmed
                if (isPermissionGranted) {
                    CameraScreen(
                        onPhotoCaptured = { uriImage ->
                            selectedImageVisibility = MediaVisibility.PUBLIC
                            selectedImageUris.clear()
                            selectedImageUris.add(uriImage)
                            hideOnSend = false
                            showCamera = false
                        },

                        onVideoCaptured = { uri ->
                            selectedImageVisibility = MediaVisibility.PUBLIC
                            selectedVideoUris.clear()
                            selectedVideoUris.add(uri)
                            showCamera = false
                        },

                        onDismiss = { showCamera = false }
                    )
                }
            }

            // In SandeshScreen, inside the root Box, at the very bottom:
            // paste this just before the closing } of your root Box
//            if (isSelectionMode && selectedMessages.size == 1 && showEmojiPickerFirst) {
//
//
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .align(Alignment.BottomCenter)
//                        .padding(bottom = 150.dp) // height of action row
//                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 8.dp, horizontal = 12.dp)
//                            .horizontalScroll(rememberScrollState()),
//                        horizontalArrangement = Arrangement.spacedBy(10.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = selectedMessages.size.toString(),
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 20.sp
//                        )
//
//
//                        quickReactions.take(maxVisibleEmojis).forEach { emoji ->
//                            EmojiReactionItem(
//                                emoji = emoji,
//                                onClick = {
//                                    val selectedId = selectedMessages.firstOrNull()
//                                    val sandeshId = parsedList.find { it.sandeshId == selectedId }?.sandeshId
//                                    if (sandeshId != null) {
//                                        sandeshViewModel.reactToMsg(emoji, sandeshId)
//                                        selectedMessages.clear()
//                                        isSelectionMode = false
//                                    }
//                                }
//                            )
//                        }
//
//                        Box(
//                            modifier = Modifier
//                                .clip(CircleShape)
//                                .background(MaterialTheme.colorScheme.primaryContainer)
//                                .clickable { showEmojiPicker = true }
//                                .padding(10.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "😀 +",
//                                fontSize = 20.sp,
//                                color = MaterialTheme.colorScheme.primary
//                            )
//                        }
//                    }
//
//                    HorizontalDivider(
//                        thickness = 0.7.dp,
//                        color = MaterialTheme.colorScheme.outlineVariant
//                    )
//                }
//            }

        }

    liveLocationSandeshId?.let { sandeshId ->


        LiveLocationScreen(
            sandeshId = sandeshId,
            sandeshViewModel = sandeshViewModel,
            onBack = {
                liveLocationSandeshId = null
            }
        )
    }
    if (showAiUsage && isAi) {
        AiUsageDialog(
            balance = walletBalance,
            onDismiss = {
                showAiUsage = false
            },
            onBuy = { pack ->
                showAiUsage = false

                // Price in INR → paise, with AI purchase type
                    sandeshViewModel.createOrderID(
                    amount = pack.priceInr * 100,
                    orderType = PurchaseOrderType.AI
                )
                // Connect your Razorpay recharge flow here
            },
//            dootawasColors = dootawasColors,
            onRefresh = {
                sandeshViewModel.fetchAiTokens()
            }
        )
    }
    if (showEmojiPicker) {

        EmojiPickerBottomSheet(
            onDismiss = { showEmojiPicker = false },
            onEmojiPicked = { emoji ->
                showEmojiPicker = false
                val selectedId = selectedMessages.firstOrNull()
                val sandeshId = parsedList.find { it.sandeshId == selectedId }?.sandeshId
                if (sandeshId != null) {
                    sandeshViewModel.reactToMsg(emoji, sandeshId)
                    selectedMessages.clear()
                    isSelectionMode = false
                }
            }
        )
    }

    if (showThemeEditor) {

        UserThemeEditorScreen(

            initialTheme = dootawasColors,

            initialBorderMode = borderMode,

            isDark = isDark,
            useModernStyle = useModernStyle,
            onApply = { updatedTheme, updatedBorderMode, useModernStyle -> // 👈 Added parameter here

                sandeshViewModel.saveCurrentTheme(
                    isDark = isDark,
                    colors = updatedTheme,
                    borderMode = updatedBorderMode,
                    useModernStyle = useModernStyle // 👈 Forward the bubble layout choice
                )

                showThemeEditor = false
            },

            onClose = {
                showThemeEditor = false
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
            currentUserId = userNumber,
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
                    serializedMessageJson.addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                if (isNewCreation) {
                    sandeshViewModel.sendList(
                        targetSandeshId = targetSandeshId,
                        userNumbers = listOf(userNumber),
                        message = serializedMessageJson
                    )
                } else {
                    sandeshViewModel.modifyListLocally(
                        targetSandeshId = targetSandeshId,
                        userNumber = userNumber,
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
                serializedMessageJson.addProperty(DootawasMessage.DISAPPEAR_DURATION,upbhogta?.autoDisappearMsg ?: 0)
                // Condition 3: Send Update -> Push out onto wire network queue only
                sandeshViewModel.sendListUpdateOnly(
                    targetSandeshId = UUID.randomUUID().toString(),
                    userNumbers = listOf(userNumber),
                    message = serializedMessageJson
                )
                activeListForEdit = null
            },
            onDiscardConfirmNeeded = { confirmDiscard ->
                discardAction = confirmDiscard
                showDiscardDialog = true
            },
            // In your ListScreen call:
            onCreateBill = { list ->
                billSourceList = list
                showCreateBill = true
            },
        )
    }


    var showDukaan by remember { mutableStateOf(false) }

    if (showCreateBill) {
        CreateBillScreen(
            contactId = userNumber,
            contactName = userName.takeUnless { it.isBlank() } ?: userNumber,
            previousBalance = upbhogta?.bakaya ?: 0.0,
            initialList = billSourceList,
            onOpenDukaan = { showDukaan = true },
            onBillCreated = { ledgerSandesh ->
                showCreateBill = false
                billSourceList = null
                sandeshViewModel.sendLedgerSandesh(listOf(userNumber), ledgerSandesh)
            },
            onBack = {
                billSourceList = null
                showCreateBill = false
            }
        )
    }


    // Put this AFTER the bill block so it draws on top of it.
    if (showDukaan) {
        DukaanScreen(onBack = { showDukaan = false })   // the screen we build next
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
            sandeshViewModel = sandeshViewModel,

            videoUris = videoPreviewUris,
            startIndex = videoPreviewIndex,
            disappearSeconds = upbhogta?.autoDisappearMsg ?: 0,
            onEdit = { uri ->
                selectedVideoUris.clear()
                selectedVideoUris.add(uri)
            },
            onDismiss = {
                showVideoPreview = false
            }
        )
    }


// Check if not already revealed before showing floating reminder card
    val isAlreadyRevealed = upbhogta?.revealSender == true
    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(
        visible = !isAlreadyRevealed && showSwipeDialog,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(end = 12.dp, bottom = 130.dp)
                .widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Header with Title & Close Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reveal phone number",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showSwipeDialog = false },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "Swipe to share your phone number, or dismiss to use your username.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                // Swipe Component
                SwipeToRevealButton(
                    onConfirmed = {
                        showSwipeDialog = false
                        sandeshViewModel.revealPhoneNumber()
                    }
                )
            }
        }
    }
    }

}

@Composable
fun SwipeToRevealButton(
    onConfirmed: () -> Unit
) {
    val thumbSize = 52.dp
    val trackHeight = 60.dp
    val density = LocalDensity.current

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val maxOffsetPx = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)

    val offsetAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var confirmed by remember { mutableStateOf(false) }

    // Calculate progress (0f to 1f) without recomposing the entire layout on every drag frame
    val progress = remember(offsetAnim.value, maxOffsetPx) {
        if (maxOffsetPx > 0f) (offsetAnim.value / maxOffsetPx).coerceIn(0f, 1f) else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Swipe to reveal number",
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = 1f - progress },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .padding(4.dp)
                .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                .size(thumbSize - 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(maxOffsetPx, confirmed) {
                    if (confirmed) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!confirmed) {
                                if (offsetAnim.value >= maxOffsetPx * 0.85f) {
                                    scope.launch {
                                        offsetAnim.animateTo(maxOffsetPx, spring())
                                        confirmed = true
                                        onConfirmed()
                                    }
                                } else {
                                    scope.launch {
                                        offsetAnim.animateTo(
                                            0f,
                                            spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset =
                                    (offsetAnim.value + dragAmount).coerceIn(0f, maxOffsetPx)
                                offsetAnim.snapTo(newOffset)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Swipe",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {

    var fileName = "document_${System.currentTimeMillis()}"

    if (uri.scheme == "content") {

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->

            if (cursor.moveToFirst()) {

                val nameIndex =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) {
                        fileName = name
                    }
                }
            }
        }

    } else {
        uri.lastPathSegment?.let {
            fileName = it
        }
    }

    return fileName
}




fun openDocument(context: Context, uriString: String, mimeType: String) {
    try {
        val uri = if (uriString.startsWith("/")) {
            // Raw file path → convert to FileProvider URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                File(uriString)
            )
        } else {
            // Already content:// or file:// → use as is
            uriString.toUri()
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}


enum class MediaCategory(
    val folder: String,
    val prefix: String,
    val defaultExtension: String
) {
    IMAGE("userdata/Images", "IMG", "jpg"),
    VIDEO("userdata/Videos", "VID", "mp4"),
    AUDIO("userdata/Audio", "AUD", "m4a"),
    DOCUMENT("userdata/Documents", "DOC", "dat"),
    PREVIEW("userdata/Preview", "PREVIEW", "jpg"),
    PROFILE("userdata/Profiles", "PROFILE", "jpg"),
    DRAFT("userdata/Drafts", "DRAFT", "dat"),

    STORY_IMAGE("userdata/Stories/Images", "STORY_IMG", "jpg"),
    STORY_VIDEO("userdata/Stories/Videos", "STORY_VID", "mp4")
}
enum class MediaVisibility {
    PRIVATE,
    PUBLIC
}

data class SavedMedia(
    val localPath: String,
    val mimeType: String,
    val publicUri: Uri? = null,
    val fileName: String,
    val fileSize: Long
)

fun MediaCategory.isPublicExportSupported(): Boolean {

    return when (this) {

        MediaCategory.IMAGE,
        MediaCategory.VIDEO,
        MediaCategory.AUDIO,
        MediaCategory.DOCUMENT -> true

        else -> false
    }
}

fun saveMediaToAppStorageNext(
    context: Context,
    sourceUri: Uri,
    storageType: MediaCategory,
    visibility: MediaVisibility = MediaVisibility.PRIVATE,
    overrideMimeType: String? = null
): SavedMedia? {

    val resolver = context.contentResolver

    // ------------------------------------------------
    // Resolve MIME type
    // ------------------------------------------------
    val mimeType =
        overrideMimeType
            ?: resolver.getType(sourceUri)
                ?.takeIf {
                    it != "application/octet-stream"
                }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(
                        sourceUri.toString()
                    )?.lowercase()
                )
            ?: "application/octet-stream"

    // ------------------------------------------------
    // Resolve extension
    // ------------------------------------------------
    val extension =
        MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: storageType.defaultExtension

    val fileName =
        "${storageType.prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"

    // ------------------------------------------------
    // Internal storage directory
    // ------------------------------------------------
    val internalDir = File(
        context.filesDir,
        storageType.folder
    )

    if (!internalDir.exists()) {
        internalDir.mkdirs()
    }

    // ------------------------------------------------
    // Prevent media scanning
    // ------------------------------------------------
    if (
        storageType == MediaCategory.IMAGE ||
        storageType == MediaCategory.VIDEO ||
        storageType == MediaCategory.AUDIO ||
        storageType == MediaCategory.PREVIEW ||
        storageType == MediaCategory.PROFILE ||
        storageType == MediaCategory.STORY_IMAGE ||
        storageType == MediaCategory.STORY_VIDEO
    ) {
        File(internalDir, ".nomedia").apply {
            if (!exists()) {
                createNewFile()
            }
        }
    }

    val internalFile = File(
        internalDir,
        fileName
    )

    return try {

        // ------------------------------------------------
        // Copy picked file into INTERNAL STORAGE
        // ------------------------------------------------
        val inputStream = when (sourceUri.scheme) {

            "content" -> {
                resolver.openInputStream(sourceUri)
            }

            "file", null -> {
                sourceUri.path?.let {
                    File(it).inputStream()
                }
            }

            else -> {
                resolver.openInputStream(sourceUri)
            }
        } ?: throw Exception(
            "Unable to open input stream"
        )

        inputStream.use { input ->

            internalFile.outputStream().use { output ->

                input.copyTo(output)
            }
        }

        // ------------------------------------------------
        // OPTIONAL PUBLIC EXPORT
        // ------------------------------------------------
        val publicUri = if (

            visibility == MediaVisibility.PUBLIC &&
            storageType.isPublicExportSupported()

        ) {

            exportFileToMediaStore(
                context = context,
                file = internalFile,
                fileName = fileName,
                mimeType = mimeType,
                category = storageType
            )

        } else {
            null
        }

        SavedMedia(
            localPath = internalFile.absolutePath,
            mimeType = mimeType,
            publicUri = publicUri,
            fileName = fileName,
            fileSize = internalFile.length()
        )

    } catch (e: Exception) {

        e.printStackTrace()

        if (internalFile.exists()) {
            internalFile.delete()
        }

        null
    }
}

fun exportFileToMediaStore(
    context: Context,
    file: File,
    fileName: String,
    mimeType: String,
    category: MediaCategory
): Uri? {

    if (!category.isPublicExportSupported()) {
        return null
    }

    val resolver = context.contentResolver

    val relativePath = when (category) {

        MediaCategory.IMAGE ->
            "${Environment.DIRECTORY_PICTURES}/Knotx"

        MediaCategory.VIDEO ->
            "${Environment.DIRECTORY_MOVIES}/Knotx"

        MediaCategory.AUDIO ->
            "${Environment.DIRECTORY_MUSIC}/Knotx"

        MediaCategory.DOCUMENT ->
            "${Environment.DIRECTORY_DOCUMENTS}/Knotx"

        else -> return null
    }

    val contentValues = ContentValues().apply {

        put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            fileName
        )

        put(
            MediaStore.MediaColumns.MIME_TYPE,
            mimeType
        )

        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            relativePath
        )

        put(
            MediaStore.MediaColumns.IS_PENDING,
            1
        )
    }

    val collection = when (category) {

        MediaCategory.IMAGE ->
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

        MediaCategory.VIDEO ->
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

        MediaCategory.AUDIO ->
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

        MediaCategory.DOCUMENT ->
            MediaStore.Files.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )

        else -> return null
    }

    val mediaStoreUri =
        resolver.insert(
            collection,
            contentValues
        ) ?: return null

    return try {

        resolver.openOutputStream(
            mediaStoreUri
        )?.use { output ->

            file.inputStream().use { input ->

                input.copyTo(output)
            }
        }

        contentValues.clear()

        contentValues.put(
            MediaStore.MediaColumns.IS_PENDING,
            0
        )

        resolver.update(
            mediaStoreUri,
            contentValues,
            null,
            null
        )

        mediaStoreUri

    } catch (e: Exception) {

        e.printStackTrace()

        resolver.delete(
            mediaStoreUri,
            null,
            null
        )

        null
    }
}