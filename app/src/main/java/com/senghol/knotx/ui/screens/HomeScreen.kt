package com.senghol.knotx.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

import android.graphics.BlurMaskFilter
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource


import com.senghol.knotx.ui.components.UpbhogtaItem
import com.senghol.knotx.ui.dialogs.UpbhogtaOptionsDialog
import com.senghol.knotx.ui.viewmodel.HomeViewModel

import com.senghol.knotx.ui.components.BottomActionBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.focusable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ListAlt

import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble

import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Group

import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.MoreVert

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip

import androidx.compose.ui.text.font.FontWeight

import com.senghol.knotx.ui.dialogs.UpdateRequiredDialog

import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import com.senghol.knotx.ui.dialogs.OptionalUpdateDialog
import com.senghol.knotx.utils.enums.SettingPrefKeys
import com.senghol.knotx.utils.settingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.sp

import com.google.gson.JsonObject

import com.senghol.knotx.connection.Knotx
//import com.senghol.knotx.ui.calling.CallActivity2
import com.senghol.knotx.ui.components.AskDonationReminderCard
import com.senghol.knotx.ui.components.BackupReminderCard
import com.senghol.knotx.ui.components.BatteryOptimizationWarning

import com.senghol.knotx.ui.components.GroupItem
import com.senghol.knotx.ui.components.ListScreen
import com.senghol.knotx.ui.components.MiuiPermissionCard

import com.senghol.knotx.ui.dialogs.CreateGroupDialogScreen
import com.senghol.knotx.ui.dialogs.GroupOptionsDialog
import com.senghol.knotx.ui.dialogs.SettingsScreen
import com.senghol.knotx.ui.theme.DootawasColors
import com.senghol.knotx.ui.theme.LocalDootawasTheme

import com.senghol.knotx.utils.data.GroupUiItem
import com.senghol.knotx.utils.data.UpbhogtaUiItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

import kotlin.math.roundToInt


import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange

import androidx.compose.ui.text.font.Font
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.senghol.knotx.KnotxApp
import com.senghol.knotx.calling.domain.model.CallParticipant
import com.senghol.knotx.calling.domain.model.CallType
import com.senghol.knotx.calling.domain.model.ParticipantConnectionState
import com.senghol.knotx.calling.ui.CallActivity
import com.senghol.knotx.calling.ui.CallViewModel
import com.senghol.knotx.calling.ui.CallViewModelFactory
import com.senghol.knotx.data.database.StoryEntity
import com.senghol.knotx.ui.components.CameraScreen
import com.senghol.knotx.ui.components.ListEditorState
import com.senghol.knotx.ui.components.PermissionHandler
import com.senghol.knotx.ui.components.SosQuickShareCard
import com.senghol.knotx.ui.components.StoriesTabContent
import com.senghol.knotx.ui.components.StoryPlayerScreen
import com.senghol.knotx.ui.components.home.CallHistoryRow
import com.senghol.knotx.ui.components.home.ListSummaryRow
import com.senghol.knotx.ui.components.home.SwipeToRevealListActions
import com.senghol.knotx.ui.components.home.PhoneNumberScannerScreen
import com.senghol.knotx.ui.components.home.SwipeTutorialOverlay
import com.senghol.knotx.ui.components.rememberShineBrushSearch
import com.senghol.knotx.ui.contactPicker.ContactPickerScreen
import com.senghol.knotx.ui.contactPicker.PickerSources
import com.senghol.knotx.ui.dialogs.SettingsOverlay
import com.senghol.knotx.ui.screens.stories.StoryBackground
import com.senghol.knotx.ui.screens.stories.StoryCreationSheet
import com.senghol.knotx.ui.screens.stories.StoryEditorScreen
import com.senghol.knotx.utils.data.HomeTab
import java.io.File
import java.util.UUID
import kotlin.collections.isNotEmpty
import kotlin.time.Duration.Companion.milliseconds
import com.senghol.knotx.R
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import com.senghol.knotx.utils.objects.ReviewHelper

fun forceUpdateNow(context: Context) {
    val packageName = context.packageName
    val intent = Intent(
        Intent.ACTION_VIEW,
        "market://details?id=$packageName".toUri()
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        )
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(webIntent)
    }

    // ✅ Mark that user was forced to update
    CoroutineScope(Dispatchers.IO).launch {
        context.settingStore.edit {
            it[SettingPrefKeys.OPTIONAL_UPDATE_SHOWN_VERSION] = ""
        }
    }
}
fun Modifier.innerShadow(
    shape: Shape,
    color: Color = Color.Black,
    blur: Dp = 8.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 2.dp,
    strokeWidthDp: Dp = 12.dp
) = drawWithContent {
    drawContent()

    drawIntoCanvas { canvas ->
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }

        canvas.save()
        canvas.clipPath(path)

        val paint = Paint().apply {
            this.color = color
            this.style = PaintingStyle.Stroke
            this.strokeWidth = strokeWidthDp.toPx() * 2 // half gets clipped away by the clip path
        }
        paint.asFrameworkPaint().apply {
            maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
        }

        canvas.translate(offsetX.toPx(), offsetY.toPx())
        canvas.drawPath(path, paint)

        canvas.restore()
    }
}




@Composable
fun DootawasSearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    colors: DootawasColors,
    onSearch: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val shape = RoundedCornerShape(28.dp)
    val (shineBrush, sizeModifier) = rememberShineBrushSearch(colors.searchBarBackground)

    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .then(sizeModifier)
            .background(shineBrush)
            .then(
                if (isDark) {
                    Modifier.innerShadow(
                        shape = shape,
                        color = Color.Black.copy(alpha = if (focused) 0.65f else 0.65f),
                        blur = 0.8.dp,
                        offsetX = 0.dp,
                        offsetY = 1.2.dp,
                        strokeWidthDp = 1.2.dp
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (focused)
                    colors.searchBarFocusedIndicator
                else
                    colors.searchBarUnfocusedIndicator,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusable(interactionSource = interactionSource),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = colors.searchBarText,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colors.searchBarFocusedIndicator),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                decorationBox = { innerTextField ->
                    if (value.text.isEmpty()) {
                        AnimatedContent(
                            targetState = placeholder,
                            transitionSpec = {
                                (slideInVertically(tween(350)) { it / 2 } + fadeIn(tween(350)))
                                    .togetherWith(
                                        slideOutVertically(tween(350)) { -it / 2 } + fadeOut(tween(250))
                                    )
                                    .using(SizeTransform(clip = true))
                            },
                            label = "placeholderHint"
                        ) { text ->
                            Text(
                                text = text,
                                color = colors.searchBarText.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    innerTextField()
                },            )

            AnimatedVisibility(
                visible = value.text.isNotEmpty()
            ) {
                IconButton(
                    onClick = { onValueChange(TextFieldValue()) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = colors.searchBarText.copy(alpha = .75f)
                    )
                }
            }
        }
    }
}

// Reusable bottom-only shadow modifier — Inkscape-style: independent blur + offset control
fun Modifier.bottomShadow(
    color: Color = Color.Black.copy(alpha = 0.50f),
    cornerRadius: Dp = 10.dp,
    blurRadius: Dp = 10.dp,
    offsetY: Dp = 5.dp
): Modifier = composed {
    val shadowPaint = remember {
        android.graphics.Paint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            isAntiAlias = true
        }
    }

    this.drawBehind {
        shadowPaint.setShadowLayer(
            blurRadius.toPx(),
            0f,
            offsetY.toPx(),
            color.toArgb()
        )
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadius.toPx(), cornerRadius.toPx(),
                shadowPaint
            )
        }
    }
}
@Composable
fun HomeScreen(homeViewModel: HomeViewModel, navController: NavController, knotx: Knotx, onSendSos: (JsonObject, List<String>) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val upbhogtas by homeViewModel.filteredUpbhogtaItems.collectAsStateWithLifecycle()
    val groups by homeViewModel.filteredGroupItems.collectAsStateWithLifecycle()
    val typingUsers by homeViewModel.typingUsers.collectAsStateWithLifecycle()
    val isConnected by homeViewModel.connectionStatus.collectAsStateWithLifecycle()

    val callHistory by homeViewModel.history.collectAsStateWithLifecycle()
    val lists by homeViewModel.lists.collectAsStateWithLifecycle()

    // --- Stories State Streams & Active Player Dialog State ---
    val myStories by homeViewModel.myStories.collectAsStateWithLifecycle()
    val feedStoryGroups by homeViewModel.feedStoryGroups.collectAsStateWithLifecycle()
    val currentUserId by homeViewModel.currentUserIdFlow.collectAsStateWithLifecycle(initialValue = null)

    var activeStoryPlayerPlaylist by remember { mutableStateOf<List<StoryEntity>?>(null) }
    var activeStoryPlayerIndex by remember { mutableIntStateOf(0) }


    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val searchQuery by homeViewModel.userSearchQuery.collectAsStateWithLifecycle()
    var searchFieldValue by remember {
        mutableStateOf(TextFieldValue(searchQuery))
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var settingsInitialScreen by rememberSaveable { mutableStateOf(SettingsScreen.HOME) }
    var selectedUpbhogta by remember { mutableStateOf<UpbhogtaUiItem?>(null) }
    var selectedGroupUpbhogta by remember { mutableStateOf<GroupUiItem?>(null) }

    val theme = LocalDootawasTheme.current
    val dootawasColors = theme.colors
    val forceUpdateInfo by homeViewModel.forceUpdateVersionInfo.collectAsStateWithLifecycle()
    val optionalUpdateInfo by homeViewModel.optionalUpdateDialogState.collectAsStateWithLifecycle()

    val comingSoon = stringResource(R.string.coming_soon)
    val typingUserSet = remember(typingUsers) { typingUsers.toSet() }
    val listState = rememberLazyListState()
    val tutorialShown by homeViewModel.swipeTutorialShown.collectAsStateWithLifecycle()
    var showTutorial by remember { mutableStateOf(false) }
    var showSosCard by remember { mutableStateOf(false) }

    var showScanner by remember { mutableStateOf(false) }
    var showStoriesScreen by remember { mutableStateOf(false) }
    var scannedNumber by remember { mutableStateOf<String?>(null) }

    val sosContacts by homeViewModel.sosContactsFlow.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var discardAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }

    var listEditorState by remember { mutableStateOf<ListEditorState?>(null) }
    val callViewModel: CallViewModel = viewModel(
        factory = CallViewModelFactory(
            callContainer = (context.applicationContext as KnotxApp).callContainer
        )
    )
    LaunchedEffect(listState, selectedTab, upbhogtas, groups) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { visibleIndexes ->
                when (selectedTab) {
                    0 -> {
                        val visibleNumbers = visibleIndexes.mapNotNull {
                            upbhogtas.getOrNull(it)?.userNumber
                        }
                        homeViewModel.updateVisibleUserNumbers(visibleNumbers)
                    }
                    1 -> {
                        val visibleGroupIds = visibleIndexes.mapNotNull {
                            groups.getOrNull(it)?.id
                        }
                        homeViewModel.updateVisibleGroupIds(visibleGroupIds)
                    }
                }
            }
    }

    LaunchedEffect(searchQuery) {
        if (searchFieldValue.text != searchQuery) {
            searchFieldValue = searchFieldValue.copy(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }


    val callPermissionRequest by callViewModel.callPermissionRequest.collectAsStateWithLifecycle()
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        callViewModel.onCallPermissionResult(
            granted = result.values.all { it }
        )
    }

    val profilePic by homeViewModel.currentUserProfilePicFlow.collectAsStateWithLifecycle(initialValue = null)
    var groupCallTarget by remember { mutableStateOf<Pair<GroupUiItem, CallType>?>(null) }
    LaunchedEffect(callPermissionRequest) {
        callPermissionRequest?.let { request ->
            callPermissionLauncher.launch(
                request.permissions.toTypedArray()
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(30_000) // let the user settle in first
        if (homeViewModel.shouldAskForReview()) {
            context.findActivity()?.let { activity ->
                ReviewHelper.askForReview(activity)
                homeViewModel.markReviewAsked()
            }
        }
    }
    LaunchedEffect(Unit) {
        callViewModel.openCallScreen.collect{
            context.startActivity(
                Intent(context, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
    }


    fun startCall(selected: UpbhogtaUiItem, callType: CallType) {
        if (currentUserId == selected.userNumber || selected.userNumber == "+919999912345") {
            Toast.makeText(
                context,
                "You can't call yourself",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        callViewModel.startCallWithPermission(
            participants = listOf(
                CallParticipant(
                    participantId = selected.userNumber,
                    userNumber = selected.userNumber,
                    userNameId = null,
                    displayName = null,
                    profileUrl = null,
                    connectionState = ParticipantConnectionState.INVITED
                )
            ),
            callType = callType
        )
    }

    val customTabFontFamily = FontFamily(
        Font(R.font.roboto_regular, FontWeight.Normal),
        Font(R.font.roboto_bold, FontWeight.Medium)
    )
    // Show force update dialog
    forceUpdateInfo?.let { (required, current) ->
        UpdateRequiredDialog(
            minimumVersion = required,
            currentVersion = current,
            onConfirm = {
                forceUpdateNow(context)
            },
            onCancel = {
                homeViewModel.clearForceUpdateRequest()
            }
        )
    }

    // Show optional update dialog
    optionalUpdateInfo?.let { (latest, current) ->
        OptionalUpdateDialog(
            latestVersion = latest,
            currentVersion = current,
            onConfirm = {
                forceUpdateNow(context)
                homeViewModel.clearOptionalUpdateRequest(latest)
            },
            onCancel = {
                homeViewModel.clearOptionalUpdateRequest(latest)
            }
        )
    }

    LaunchedEffect(listState, selectedTab, upbhogtas, groups) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .distinctUntilChanged()
            .collect { visibleIndexes ->
                when (selectedTab) {
                    0 -> {
                        val visibleNumbers = visibleIndexes.mapNotNull {
                            upbhogtas.getOrNull(it)?.userNumber
                        }
                        homeViewModel.updateVisibleUserNumbers(visibleNumbers)
                    }
                    1 -> {
                        val visibleGroupIds = visibleIndexes.mapNotNull {
                            groups.getOrNull(it)?.id
                        }
                        homeViewModel.updateVisibleGroupIds(visibleGroupIds)
                    }
                }
            }
    }

    // Show force update dialog
    forceUpdateInfo?.let { (required, current) ->
        UpdateRequiredDialog(
            minimumVersion = required,
            currentVersion = current,
            onConfirm = {
                forceUpdateNow(context)
            },
            onCancel = {
                homeViewModel.clearForceUpdateRequest()
            }
        )
    }

    // Show optional update dialog
    optionalUpdateInfo?.let { (latest, current) ->
        OptionalUpdateDialog(
            latestVersion = latest,
            currentVersion = current,
            onConfirm = {
                forceUpdateNow(context)
                homeViewModel.clearOptionalUpdateRequest(latest)
            },
            onCancel = {
                homeViewModel.clearOptionalUpdateRequest(latest)
            }
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        val baseColor = dootawasColors.homeBackground
        val isDark = isSystemInDarkTheme()
        val gradientColors = if (isDark) {
            listOf(
                lerp(baseColor, Color.Black, 0.04f),
                lerp(baseColor, Color.Black, 0.02f),
                lerp(baseColor, Color.Black, 0.00f)
            )
        } else {
            listOf(
                lerp(baseColor, Color.White, 0.08f),
                lerp(baseColor, Color.White, 0.16f),
                lerp(baseColor, Color.White, 0.24f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .union(WindowInsets(bottom = 12.dp))        // minimum bottom space
                )
                .blur(if (showSettings) 14.dp else 0.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top row: App name, QR scanner and profile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = dootawasColors.searchBarText,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            settingsInitialScreen = SettingsScreen.IMAGE_PREVIEW
                            showSettings = true
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (!profilePic.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePic,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(
                                        elevation = 3.dp,
                                        shape = CircleShape,
                                        clip = false
                                    )
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Profile",
                                tint = dootawasColors.searchBarText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showScanner = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.qr_code_scanner_24px
                            ),
                            contentDescription = "Scan",
                            tint = dootawasColors.homeHeaderSelectedText,
                            modifier = Modifier.size(24.dp)
                        )
                    }


                    IconButton(
                        onClick = {
                            settingsInitialScreen = SettingsScreen.HOME
                            showSettings = true
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More options",
                            tint = dootawasColors.searchBarText,
                            modifier = Modifier.size(24.dp)
                        )
                    }


                }

                // Second row: Search bar
                DootawasSearchBar(
                    value = searchFieldValue,
                    onValueChange = {

                        if (selectedTab == 0) {
                            homeViewModel.searchUpbhogtas(it.text)
                        } else if (selectedTab == 1) {
                            homeViewModel.searchGroups(it.text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.search),
                    colors = dootawasColors,
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )


            }

            // Tabs with Icons + Badges
            val totalUnreadChats = upbhogtas.sumOf { it.totalUnread }
            val totalUnreadGroups = groups.sumOf { it.totalUnread }
            val unreadStoriesCount = feedStoryGroups.count { it.hasUnseenStories }

            val chatsText = stringResource(R.string.tab_chats)
            val groupsText = stringResource(R.string.tab_groups)
            val callsText = stringResource(R.string.tab_calls)
            val storiesText = stringResource(R.string.tab_stories)
            val listsText = stringResource(R.string.tab_lists)

            val tabData = listOf(
                HomeTab(chatsText, Icons.Default.ChatBubble, totalUnreadChats),
                HomeTab(groupsText, Icons.Default.Group, totalUnreadGroups),
                HomeTab(storiesText, Icons.Default.PlayCircle, unreadStoriesCount),
                HomeTab(callsText, Icons.Default.Call, 0),
                HomeTab(listsText, Icons.AutoMirrored.Filled.ListAlt, 0)
            )

            val visibleTabs = 3
            val horizontalPadding = 8.dp
            val innerPadding = 4.dp
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val viewportWidth = screenWidth - (horizontalPadding * 2) - (innerPadding * 2)
            val tabWidth = viewportWidth / visibleTabs
            val scrollState = rememberScrollState()

            val pillOffset = tabWidth * selectedTab
// Usage in your header

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dootawasColors.homeHeaderBackground,
                                dootawasColors.homeHeaderBackground
                            )
                        )
                    )
                    .horizontalScroll(scrollState)
                    .padding(4.dp)
            ) {
                Card(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth - 8.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
//                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = dootawasColors.homeHeaderSelectedBackground
                    )
                ) {}

                Row {
                    tabData.forEachIndexed { index, (title, icon, unreadCount) ->
                        val isSelected = selectedTab == index
                        val interactionSource = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    selectedTab = index
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) dootawasColors.homeHeaderIcon else dootawasColors.homeHeaderUnSelectedText,
                                modifier = Modifier.size(15.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = title,
                                color = if (isSelected) dootawasColors.homeHeaderSelectedText else dootawasColors.homeHeaderUnSelectedText,
                                fontFamily = customTabFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = if (isSelected) 16.sp else 15.sp,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (unreadCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = if (isSelected) dootawasColors.homeHeaderSelectedText.copy(alpha = .85f) else dootawasColors.homeHeaderUnSelectedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            val density = LocalDensity.current

            LaunchedEffect(selectedTab) {
                if (selectedTab == 2) {
                    homeViewModel.getAllContactStories()
                }

                val tabWidthPx = with(density) { tabWidth.toPx() }
                val maxWindow = (tabData.size - visibleTabs).coerceAtLeast(0)

                val window = when {
                    selectedTab <= 1 -> 0
                    selectedTab >= tabData.lastIndex - 1 -> maxWindow
                    else -> selectedTab - 1
                }

                val target = (window * tabWidthPx)
                    .roundToInt()
                    .coerceIn(0, scrollState.maxValue)

                scrollState.animateScrollTo(target)
            }


            val sortedUpbhogtas = remember(upbhogtas) {
                upbhogtas.sortedWith(
                    compareByDescending<UpbhogtaUiItem> { it.isPinned }
                        .thenByDescending { it.totalUnread }
                )
            }


            LaunchedEffect(tutorialShown, sortedUpbhogtas.size) {
                if (!tutorialShown && sortedUpbhogtas.size > 2) {
                    showTutorial = true
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.2.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f))
            ) {
            }
            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> { // Chats tab
                        itemsIndexed(
                            items = sortedUpbhogtas,
                            key = { _, upbhogta -> upbhogta.userNumber }
                        ) { index, upbhogta ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                UpbhogtaItem(
                                    upbhogta = upbhogta,
                                    index = index,
                                    lastIndex = sortedUpbhogtas.lastIndex,
                                    isTyping = upbhogta.name in typingUserSet,
                                    onClick = { selected ->
                                        homeViewModel.searchUpbhogtas("")
                                        navController.navigate("sandesh_screen/${selected.userNumber}")
                                    },
                                    onLongClick = { selectedUpbhogta = it },
                                    onSwipeRight = { selected ->
                                        startCall(selected, CallType.VIDEO)
                                    },
                                    onSwipeLeft = { selected ->
                                        startCall(selected, CallType.VOICE)
                                    },
                                    dootawasColors = dootawasColors
                                )
                                if (index < sortedUpbhogtas.lastIndex) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .fillMaxHeight()
                                                .background(dootawasColors.homeChatBackground)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Groups tab
                        val sortedGroups = groups.sortedWith(
                            compareByDescending<GroupUiItem> { it.isPinned }
                                .thenByDescending { it.totalUnread }
                        )

                        itemsIndexed(
                            items = sortedGroups,
                            key = { _, group -> group.id }
                        ) {index, group ->
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GroupItem(
                                    groupItem = group,
                                    isTyping = group.id in typingUserSet,
                                    onClick = { selected ->
                                        navController.navigate("group_screen/${group.id}")
                                    },
                                    onLongClick = { selectedGroupUpbhogta = it },
                                    onSwipeRight = { groupCallTarget = group to CallType.VIDEO },
                                    onSwipeLeft = { groupCallTarget = group to CallType.VOICE },
                                    dootawasColors = dootawasColors
                                )

                                if (index < sortedGroups.lastIndex) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .fillMaxHeight()
                                                .background(dootawasColors.homeChatBackground)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(dootawasColors.lastMessageColor.copy(alpha = 0.15f))
                                        )
                                    }
                                }

                            }
                        }
                    }

                    2 -> { // Stories tab
                        item {
                            StoriesTabContent(
                                myStories = myStories,
                                feedGroups = feedStoryGroups,
                                onAddStoryClick = { showStoriesScreen = true },
                                onMyStoryClick = {
                                    if (myStories.isNotEmpty()) {
                                        activeStoryPlayerPlaylist = myStories
                                        activeStoryPlayerIndex = 0
                                    } else {
                                        showStoriesScreen = true
                                    }
                                },
                                onUserGroupClick = { group ->
                                    if (group.stories.isNotEmpty()) {
                                        activeStoryPlayerPlaylist = group.stories
                                        activeStoryPlayerIndex = 0
                                    }
                                }
                            )
                        }
                    }


                    3 -> { // Calls tab
                        itemsIndexed(
                            items = callHistory,
                            key = { _, call -> call.callId }
                        ) { index, call ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CallHistoryRow(
                                    item = call,
                                    modifier = Modifier.fillMaxWidth(),
                                    dootawasColors = dootawasColors,
                                    onClick = {
                                        // Open call details later
                                    }
                                )

                                if (index < callHistory.lastIndex) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .fillMaxHeight()
                                                .background(dootawasColors.homeChatBackground)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    dootawasColors.lastMessageColor.copy(alpha = 0.15f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }


                    4 -> { // Lists tab
                        items(
                            lists,
                            key = { it.sandeshId }
                        ) { item ->
                            SwipeToRevealListActions(
                                onDelete = {
                                    homeViewModel.deleteList(item.sandeshId)
                                },
                                onForward = {
                                    // forward logic
                                },
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(2.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    ListSummaryRow(
                                        listData = item.listData,
                                        onClick = {
                                            listEditorState = ListEditorState(
                                                sandeshId = item.sandeshId,
                                                initialData = item.listData
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }


        // Overlays sitting on top
        if (showTutorial) {
            SwipeTutorialOverlay(
                onDismiss = {
                    showTutorial = false
                    homeViewModel.markSwipeTutorialShown()
                }
            )
        }
        BatteryOptimizationWarning()
        BackupReminderCard(
            onOpenBackup = {
                settingsInitialScreen = SettingsScreen.BACKUP
                showSettings = true
            }
        )

        AskDonationReminderCard(knotx = knotx)

        activity?.let {
            MiuiPermissionCard(activity = it)
        }
        if (showSosCard) {
            SosQuickShareCard(
                sosContacts = sosContacts,
                onNavigateToAddSosContacts = {
                    showSosCard = false
                    settingsInitialScreen = SettingsScreen.SOS_CONTACTS
                    showSettings = true
                },
                onSend = { message, recipients ->
                    showSosCard = false
                    onSendSos(message, recipients)
                },
                onDismiss = { showSosCard = false }
            )
        }


        groupCallTarget?.let { (group, callType) ->
            ContactPickerScreen(
                sources = PickerSources(
                    numberContacts = false,
                    usernameContacts = false,
                    groups = false,
                    deviceContacts = false,
                    groupMembers = true,
                    groupId = group.id
                ),
                title = if (callType == CallType.VIDEO) "Group video call" else "Group voice call",
                confirmLabel = "Call",
                onBack = { groupCallTarget = null },
                onConfirm = { picked ->
                    val others = picked
                        .filter { !it.isGroup && it.userNumber != currentUserId }
                        .distinctBy { it.userNumber }

                    when {
                        others.isEmpty() ->
                            Toast.makeText(context, "Select at least one person", Toast.LENGTH_SHORT).show()

                        others.size > 3 ->
                            Toast.makeText(
                                context,
                                "You can call up to 3 people at once",
                                Toast.LENGTH_SHORT
                            ).show()

                        else -> {
                            groupCallTarget = null
                            callViewModel.startCallWithPermission(
                                participants = others.map {
                                    CallParticipant(
                                        participantId = it.userNumber,
                                        userNumber = it.userNumber,
                                        userNameId = null,
                                        displayName = null,
                                        profileUrl = null,
                                        connectionState = ParticipantConnectionState.INVITED
                                    )
                                },
                                callType = callType
                            )
                        }
                    }
                }
            )
        }

    }

    selectedUpbhogta?.let {
        UpbhogtaOptionsDialog(
            upbhogta = it,
            homeViewModel = homeViewModel,
            onDismiss = { selectedUpbhogta = null }
        )
    }

    selectedGroupUpbhogta?.let {
        GroupOptionsDialog(
            group = it,
            homeViewModel = homeViewModel,
            onDismiss = { selectedGroupUpbhogta = null }
        )
    }

    scannedNumber?.let {
        Text(
            text = stringResource(R.string.scanned_phone_number, it),
            modifier = Modifier.padding(16.dp)
        )
    }


    val userNameId by homeViewModel.userNameIdKhud.collectAsState(initial = null)

    LaunchedEffect(showSettings, userNameId) {//todo delete this later after 6 months in feb 2027
        delay(1500.milliseconds)
        if (!showSettings && userNameId.isNullOrBlank()) {
            Toast.makeText(
                context,
                "Please set your username to continue — it's required",
                Toast.LENGTH_LONG
            ).show()
            settingsInitialScreen = SettingsScreen.ACCOUNT
            showSettings = true
        }
    }
    BottomActionBar(
        isConnected = isConnected,
        actionType = selectedTab,
        onCreateList = {
            listEditorState = ListEditorState(sandeshId = UUID.randomUUID().toString(), initialData = null)
        },
        onCreateGroup = { showGroupDialog = true },
        onLinkDevice = {
            Toast.makeText(context, "CS", Toast.LENGTH_SHORT).show()
        },
        onStories = { showStoriesScreen = true },
        onMenuClick = {
            settingsInitialScreen = SettingsScreen.HOME
            showSettings = true
        },
        onContactsClick = { navController.navigate("contacts") },
        sosContacts = emptyList(),
        sosTrigger = { showSosCard = true },
        dootawasColors = dootawasColors
    )

    if (showGroupDialog) {
        CreateGroupDialogScreen(
            onGroupCreated = { name, desc ->
                knotx.createGroup(name, desc, false)
                showGroupDialog = false
            },
            onDismiss = { showGroupDialog = false }
        )
    }
    if (showSettings) {
        SettingsOverlay(
            onDismiss = { showSettings = false },
            knotx = knotx,
            initialScreen = settingsInitialScreen
        )
    }
    if (showScanner) {
        PhoneNumberScannerScreen(
            viewModel = homeViewModel,
            onCancel = { showScanner = false }
        )
    }

    var showCamera by remember { mutableStateOf(false) }
    var croppedBackground by remember { mutableStateOf<StoryBackground?>(null) }
    var showStoryEditor by remember { mutableStateOf(false) }

    // 1. Bottom Sheet for Creation
    if (showStoriesScreen) {
        StoryCreationSheet(
            onDismiss = { showStoriesScreen = false },
            onCameraClick = {
                showStoriesScreen = false
                showCamera = true
            },
            onBackgroundReady = { bg ->
                showStoriesScreen = false
                croppedBackground = bg
                showStoryEditor = true
            }
        )
    }

    // 2. Camera Screen
    if (showCamera) {
        PermissionHandler(
            onAllPermissionsGranted = { },
            onPermissionsDenied = { _ -> showCamera = false }
        )

        CameraScreen(
            onPhotoCaptured = { uriImage ->
                showCamera = false
                croppedBackground = StoryBackground.Media(uriImage, isVideo = false)
                showStoryEditor = true
            },
            onVideoCaptured = { uriVideo ->
                showCamera = false
                croppedBackground = StoryBackground.Media(uriVideo, isVideo = true)
                showStoryEditor = true
            },
            onDismiss = { showCamera = false }
        )
    }

    // 3. Editor Screen
    croppedBackground?.let { background ->
        if (showStoryEditor) {
            StoryEditorScreen(
                background = background,
                onClose = {
                    showStoryEditor = false
                    croppedBackground = null
                },
                onReCropRequest = { _ -> },
                onSaved = { saved, visibility ->
                    val savedUri = Uri.fromFile(File(saved.localPath))
                    val isVideo = saved.mimeType.startsWith("video/")
                    val durationMs = if (isVideo) {
                        try {
                            MediaMetadataRetriever().use { retriever ->
                                retriever.setDataSource(context, savedUri)
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                            }
                        } catch (e: Exception) { null }
                    } else null
                    currentUserId?.let { userId ->
                        homeViewModel.createAndPublishStory(
                            ownerId = userId,
                            background = StoryBackground.Media(uri = savedUri, isVideo = isVideo),
                            visibility = visibility,
                            durationMs = durationMs
                        )
                    }
                    showStoryEditor = false
                    croppedBackground = null
                }
            )
        }
    }

    // 4. Story Player Overlay / Screen
    activeStoryPlayerPlaylist?.let { stories ->
        if (stories.isNotEmpty()) {
            StoryPlayerScreen(
                stories = stories,
                initialIndex = activeStoryPlayerIndex,
                currentUserId = currentUserId.orEmpty(),
                observeStoryComments = { storyId ->
                    homeViewModel.observeStoryComments(storyId)
                },
                observeStoryViewers = { storyId ->
                    homeViewModel.observeStoryViewers(storyId)
                },
                observeStoryLikers = { storyId ->
                    homeViewModel.observeStoryLikers(storyId)
                },
                onStoryViewed = { storyId, ownerId ->
                    currentUserId?.let { homeViewModel.onStoryViewed(storyId, it, ownerId) }
                },
                onToggleLike = { storyId ->
                    currentUserId?.let { homeViewModel.onToggleStoryLike(storyId, it) }
                },
                onAddComment = { storyId, text ->
                    currentUserId?.let { homeViewModel.onAddComment(storyId, it, text) }
                },
                onDeleteComment = { storyId, commentId ->
                    currentUserId?.let { homeViewModel.onDeleteComment(storyId, commentId, it) }
                },
                onUpdateVisibility = { storyId, visibility ->
                    currentUserId?.let {
                        homeViewModel.onUpdateStoryVisibility(
                            storyId,
                            it,
                            visibility
                        )
                    }
                },
                onDeleteStory = { storyId ->
                    homeViewModel.onDeleteStory(storyId)
                },
                onClose = { activeStoryPlayerPlaylist = null },
                onSetCommentsPrivate = { storyId , isPrivate ->
                    currentUserId?.let {
                        homeViewModel.setCommentPrivate(storyId, isPrivate,it)
                    }
                }
            )
        }
    }

    listEditorState?.let { state ->
        ListScreen(
            initialData = state.initialData,
            currentUserId = currentUserId ?: "676",
            onSave = { listData ->
                homeViewModel.saveList(listData, state.sandeshId)
            },
            onBack = { listEditorState = null },
            onDiscardConfirmNeeded = { confirmDiscard ->
                discardAction = confirmDiscard
                showDiscardDialog = true
            },
            onSendUpdate = { }
        )
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    discardAction?.invoke()
                    showDiscardDialog = false
                    listEditorState = null
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") }
        )
    }

    LaunchedEffect(Unit) {
        knotx.paymentListener = object : PaymentListener {
            override fun onOrderCreated(orderId: String, key: String, number: String, amount: Int) {
                Handler(Looper.getMainLooper()).post {
                    activity?.let { startPaymentRazor(it, orderId, key, number, amount) }
                }
            }
            override fun onOrderFailed(message: String) {
                Log.e("PAYMENT", message)
            }
        }
    }
}







