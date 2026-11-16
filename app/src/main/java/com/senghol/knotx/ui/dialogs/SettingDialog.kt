package com.senghol.knotx.ui.dialogs


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.Preferences
import coil.compose.rememberAsyncImagePainter
import com.senghol.knotx.R
import com.senghol.knotx.utils.enums.SettingPrefKeys
import androidx.core.net.toUri
import java.util.Locale

import android.graphics.Bitmap

import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call

import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite

import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

import com.senghol.knotx.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date

import com.senghol.knotx.backup.BackupScheduler
import com.senghol.knotx.backup.DriveBackupItem
import com.senghol.knotx.backup.DriveHelper
import com.senghol.knotx.backup.GoogleAuthHelper
import com.senghol.knotx.backup.RestoreManager
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.data.database.TransactionEntity
import com.senghol.knotx.ui.components.ContactSelectorScreen
import com.senghol.knotx.ui.dialogs.AppDialogs.DeleteProfileDialog
import com.senghol.knotx.ui.screens.ThemeEditorScreen
import com.senghol.knotx.utils.LocaleHelper
import com.senghol.knotx.utils.enums.PrivacyType
import com.senghol.knotx.utils.objects.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.senghol.knotx.data.database.Upbhogta
import com.senghol.knotx.utils.data.UserNameIdCheckState
import com.senghol.knotx.utils.helperclass.QrPrintAdapter
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.senghol.knotx.editor.CropState
import com.senghol.knotx.editor.EditorCropScreen
import com.senghol.knotx.editor.loadOrientedBitmap
import com.senghol.knotx.editor.renderCrop

import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.lerp as lerpColor
import kotlin.math.roundToInt


enum class SettingsScreen {
    HOME,
    ACCOUNT,
    NOTIFICATIONS,
    CHATS,
    CALLS,
    PRIVACY,
    STORAGE_DATA,
    DATA_USAGE,
    APPEARANCE,
    LANGUAGE,
    HELP_SUPPORT,
    BACKUP,
    BLOCK_LIST,
    CONTRIBUTIONS,
    SOS_CONTACTS,
    IMAGE_PREVIEW,
    CONTACT_SELECTOR   // new
}

private val CollapsedWidth = 300.dp
private val CollapsedHeight = 400.dp
private val CollapsedMargin = 8.dp
private val CollapsedRadius = 20.dp
private val CollapsedElevation = 12.dp

/** Pages that take over the whole screen and bring their own top bar. */
private fun SettingsScreen.isFullScreenPage() =
    this == SettingsScreen.APPEARANCE || this == SettingsScreen.CONTACT_SELECTOR

/**
 * Drop-in replacement for SettingsDialog. Place as the LAST child of your root Box.
 *
 * Smoothness: a single `progress` (0 = card, 1 = fullscreen) drives everything, and it is only
 * READ inside layout / draw lambdas. That means the animation never recomposes the settings
 * content, it only re-measures and redraws.
 */
@Composable
fun SettingsOverlay(
    knotx: Knotx,
    onDismiss: () -> Unit,
    initialScreen: SettingsScreen = SettingsScreen.HOME
) {
    var currentScreen by rememberSaveable { mutableStateOf(initialScreen) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        when {
            currentScreen == SettingsScreen.CONTACT_SELECTOR -> currentScreen = SettingsScreen.PRIVACY
            currentScreen != SettingsScreen.HOME -> currentScreen = SettingsScreen.HOME
            isExpanded -> isExpanded = false
            else -> onDismiss()
        }
    }

    if (currentScreen.isFullScreenPage()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            when (currentScreen) {
                SettingsScreen.APPEARANCE -> ThemeEditorScreen(
                    viewModel = knotx,
                    isDark = isSystemInDarkTheme(),
                    onClose = { currentScreen = SettingsScreen.HOME }
                )
                SettingsScreen.CONTACT_SELECTOR -> ContactSelectorScreen(
                    onBack = { currentScreen = SettingsScreen.PRIVACY }
                )
                else -> Unit
            }
        }
        return
    }

    // 0f = small card, 1f = fullscreen. Passed around as State so it is only read in layout/draw.
    val progress = animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "settingsExpand"
    )

    val surface = MaterialTheme.colorScheme.surface
    val bars = WindowInsets.systemBars

    Box(Modifier.fillMaxSize()) {

        // Scrim: tap outside to dismiss. Alpha is read in the draw phase only.
        Box(
            Modifier
                .fillMaxSize()

                .drawBehind { drawRect(Color.Black.copy(alpha = 0.32f * (1f - progress.value))) }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .union(WindowInsets(bottom = 12.dp))        // minimum bottom space
                )
                // margin shrinks to 0 as we expand
                .offset {
                    val m = lerp(CollapsedMargin.toPx(), 0f, progress.value).roundToInt()
                    IntOffset(-m, -m)
                }
                // size grows from card to full parent size
                .layout { measurable, constraints ->
                    val p = progress.value
                    val w = lerp(CollapsedWidth.roundToPx(), constraints.maxWidth, p)
                    val h = lerp(CollapsedHeight.roundToPx(), constraints.maxHeight, p)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) { placeable.place(0, 0) }
                }
                // rounded corners + shadow animate on the GPU layer, no recomposition
                .graphicsLayer {
                    val p = progress.value
                    shadowElevation = lerp(CollapsedElevation.toPx(), 0f, p)
                    shape = RoundedCornerShape(lerp(CollapsedRadius.toPx(), 0f, p))
                    clip = true
                }
                .drawBehind { drawRect(surface) }
                // swallow taps so they don't reach the scrim
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(
                Modifier
                    // system bar padding fades in smoothly instead of popping
                    .layout { measurable, constraints ->
                        val p = progress.value
                        val l = (bars.getLeft(this, layoutDirection) * p).roundToInt()
                        val t = (bars.getTop(this) * p).roundToInt()
                        val r = (bars.getRight(this, layoutDirection) * p).roundToInt()
                        val b = (bars.getBottom(this) * p).roundToInt()
                        val placeable = measurable.measure(constraints.offset(-(l + r), -(t + b)))
                        layout(placeable.width + l + r, placeable.height + t + b) {
                            placeable.place(l, t)
                        }
                    }
                    .imePadding()
                    .dottedBackground(progress)
            ) {
                SettingsHeader(
                    title = stringResource(screenTitleRes(currentScreen)),
                    showBack = currentScreen != SettingsScreen.HOME,
                    isExpanded = isExpanded,
                    progress = progress,
                    onBack = { currentScreen = SettingsScreen.HOME },
                    onToggleExpand = { isExpanded = !isExpanded }
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp, bottom = 8.dp)
                ) {
                    SettingsContent(
                        screen = currentScreen,
                        knotx = knotx,
                        onNavigate = { currentScreen = it },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    screen: SettingsScreen,
    knotx: Knotx,
    onNavigate: (SettingsScreen) -> Unit,
    onDismiss: () -> Unit
) {
    when (screen) {
        SettingsScreen.HOME -> DefaultView(
            viewModel = knotx,
            onThemeEditor = { onNavigate(SettingsScreen.APPEARANCE) },
            onNavigate = onNavigate
        )
        SettingsScreen.ACCOUNT -> AccountScreen(knotx, onNavigate = onNavigate)
        SettingsScreen.NOTIFICATIONS -> NotificationScreen(knotx)
        SettingsScreen.CHATS -> ChatsScreen(viewModel = knotx)
        SettingsScreen.CALLS -> CallsScreen(knotx)
        SettingsScreen.PRIVACY -> PrivacyScreen(
            viewModel = knotx,
            onOpenContactSelector = { onNavigate(SettingsScreen.CONTACT_SELECTOR) },
            onOpenBlockList = { onNavigate(SettingsScreen.BLOCK_LIST) }
        )
        SettingsScreen.STORAGE_DATA -> StorageDataScreen(knotx, onNavigate = onNavigate)
        SettingsScreen.DATA_USAGE -> DataUsageScreen(knotx)
        SettingsScreen.LANGUAGE -> LanguageScreen(knotx)
        SettingsScreen.HELP_SUPPORT -> HelpSupportScreen()
        SettingsScreen.BACKUP -> BackupScreen(viewModel = knotx)
        SettingsScreen.BLOCK_LIST -> BlockListScreen(viewModel = knotx)
        SettingsScreen.CONTRIBUTIONS -> ContributionScreen(viewModel = knotx)
        SettingsScreen.SOS_CONTACTS -> SosContactScreen(viewModel = knotx)
        SettingsScreen.IMAGE_PREVIEW -> {
            val path by knotx.profileImagePathFlow.collectAsState(initial = null)
            ImagePreviewDialog(path, viewModel = knotx, onDismiss = onDismiss)
        }
        SettingsScreen.APPEARANCE, SettingsScreen.CONTACT_SELECTOR -> Unit
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    showBack: Boolean,
    isExpanded: Boolean,
    progress: State<Float>,
    onBack: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primary = MaterialTheme.colorScheme.primary
    val collapsedBg = MaterialTheme.colorScheme.surfaceVariant
    val expandedBg = MaterialTheme.colorScheme.surface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val p = progress.value
                // background cross-fades instead of switching instantly
                drawRect(lerpColor(collapsedBg, expandedBg, p))
                if (p < 1f) {
                    val center = Offset(30f, -10f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(onPrimary.copy(alpha = 0.14f * (1f - p)), Color.Transparent),
                            center = center,
                            radius = 260f
                        ),
                        radius = 260f,
                        center = center
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(onPrimary.copy(alpha = 0.16f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
        }

        Text(text = title, color = primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))

        IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                contentDescription = if (isExpanded) "Collapse settings" else "Expand settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Faint dotted texture that fades out from the top centre.
 * Fades away with `progress` and only ever draws inside the collapsed card area,
 * so it stays cheap while the card is growing.
 */
private fun Modifier.dottedBackground(progress: State<Float>): Modifier = drawBehind {
    val fade = 1f - progress.value
    if (fade <= 0.01f) return@drawBehind

    val spacing = 14.dp.toPx()
    val dotRadius = 1.dp.toPx()
    val maxW = minOf(size.width, CollapsedWidth.toPx())
    val maxH = minOf(size.height, CollapsedHeight.toPx())
    val fadeCenter = Offset(maxW / 2f, 0f)
    val fadeRadius = maxH * 0.9f
    val baseAlpha = 0.25f * fade

    var y = 0f
    while (y < maxH) {
        var x = 0f
        while (x < maxW) {
            val a = (1f - (Offset(x, y) - fadeCenter).getDistance() / fadeRadius).coerceIn(0f, 1f)
            if (a > 0f) {
                drawCircle(Color.Gray.copy(alpha = baseAlpha * a), dotRadius, Offset(x, y))
            }
            x += spacing
        }
        y += spacing
    }
}

// ---- Usage ----
// if (showSettings) SettingsOverlay(knotx = vm, onDismiss = { showSettings = false })

// ---- Usage ----
// Before: if (showSettings) SettingsDialog(onDismiss = { showSettings = false }, knotx = vm)
// After (as last child of your root Box):
// if (showSettings) SettingsOverlay(knotx = vm, onDismiss = { showSettings = false })
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    knotx: Knotx,
    initialScreen: SettingsScreen = SettingsScreen.HOME
) {
//    var currentScreen by remember { mutableIntStateOf(0) }

    var currentScreen by remember {
        mutableStateOf(initialScreen)
    }

    var showContactSelector by remember { mutableStateOf(false) }

    // 🔥 selector OUTSIDE dialog
    // 🔥 Show selector INSTEAD of dialog
    if (showContactSelector) {
        ContactSelectorScreen(
            onBack = { showContactSelector = false }
        )
        return
    }
    var showThemeEditor by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 20.dp,
        animationSpec = tween(220),
        label = "settingsCornerRadius"
    )
    if (showThemeEditor) {
        val isDark = isSystemInDarkTheme()
        ThemeEditorScreen(
            viewModel = knotx,
            isDark = isDark,
            onClose = { showThemeEditor = false }
        )
        return
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window

//        SideEffect {
//            dialogWindow?.setDimAmount(0f)
//        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
        ) {
//            Card(
//                shape = RoundedCornerShape(cornerRadius),
//                modifier = Modifier
//                    .then(
//                        if (isExpanded) {
//                            Modifier.fillMaxSize()
//                        } else {
//                            Modifier
//                                .size(width = 330.dp, height = 460.dp)
//                                .align(Alignment.BottomEnd)
//                        }
//                    )
//                    .graphicsLayer {
//                        transformOrigin = TransformOrigin(1f, 1f)
//                    }
//                    .clickable(enabled = false) {},
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.surface
//                ),
//                elevation = CardDefaults.cardElevation(
//                    defaultElevation = if (isExpanded) 0.dp else 12.dp
//                )
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .clip(RoundedCornerShape(cornerRadius))
//                        .background(MaterialTheme.colorScheme.surface)
//                        .drawBehind {
//                            if (!isExpanded) {
//                                val dotColor = Color.Gray.copy(alpha = 0.25f)
//                                val spacing = 14.dp.toPx()
//                                val dotRadius = 1.dp.toPx()
//                                val fadeCenter = Offset(size.width / 2f, 0f)
//                                val fadeRadius = size.height * 0.9f
//
//                                var y = 0f
//                                while (y < size.height) {
//                                    var x = 0f
//                                    while (x < size.width) {
//                                        val dist = (Offset(x, y) - fadeCenter).getDistance()
//                                        val alpha = (1f - (dist / fadeRadius)).coerceIn(0f, 1f)
//                                        if (alpha > 0f) {
//                                            drawCircle(
//                                                color = dotColor.copy(alpha = dotColor.alpha * alpha),
//                                                radius = dotRadius,
//                                                center = Offset(x, y)
//                                            )
//                                        }
//                                        x += spacing
//                                    }
//                                    y += spacing
//                                }
//                            }
//                        }
//                ) {
//                    // HEADER
//                    val onPrimary = MaterialTheme.colorScheme.onPrimary
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .shadow(
//                                elevation = if (isExpanded) 4.dp else 0.dp,
//                                shape = RectangleShape
//                            )
//                            .background(
//                                if (isExpanded) MaterialTheme.colorScheme.surface
//                                else MaterialTheme.colorScheme.surfaceVariant
//                            )
//                            .drawBehind {
//                                if (!isExpanded) {
//                                    drawCircle(
//                                        brush = Brush.radialGradient(
//                                            colors = listOf(
//                                                onPrimary.copy(alpha = 0.14f),
//                                                Color.Transparent
//                                            ),
//                                            center = Offset(x = 30f, y = -10f),
//                                            radius = 260f
//                                        ),
//                                        radius = 260f,
//                                        center = Offset(x = 30f, y = -10f)
//                                    )
//                                }
//                            }
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 14.dp, vertical = 10.dp)
//                        ) {
//
//
//                            if (currentScreen != SettingsScreen.HOME) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(32.dp)
//                                        .clip(RoundedCornerShape(10.dp))
//                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f))
//                                        .clickable { currentScreen = SettingsScreen.HOME },
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                        contentDescription = stringResource(R.string.back),
//                                        tint = MaterialTheme.colorScheme.primary,
//                                        modifier = Modifier.size(16.dp)
//                                    )
//                                }
//                                Spacer(modifier = Modifier.width(10.dp))
//                            }
//
//                            Text(
//                                text = stringResource(screenTitleRes(currentScreen)),
//                                color = MaterialTheme.colorScheme.primary,
//                                style = MaterialTheme.typography.titleMedium
//                            )
//                            Spacer(modifier = Modifier.weight(1f))
//
//                            Box(
//                                modifier = Modifier
//                                    .size(32.dp)
//                                    .clickable { isExpanded = !isExpanded },
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Icon(
//                                    imageVector = if (isExpanded) {
//                                        Icons.Default.CloseFullscreen
//                                    } else {
//                                        Icons.Default.OpenInFull
//                                    },
//                                    contentDescription = if (isExpanded) {
//                                        "Collapse settings"
//                                    } else {
//                                        "Expand settings"
//                                    },
//                                    tint = MaterialTheme.colorScheme.onSurface,
//                                    modifier = Modifier.size(16.dp)
//                                )
//                            }
//                        }
//                    }
//
//                    // BODY
//// BODY: fills the remaining space below the header
//                    Box(
//                        modifier = Modifier
//                            .weight(1f)
//                            .fillMaxWidth()
//                            .padding(8.dp)
//                    ) {
//                        when (currentScreen) {
//                            SettingsScreen.HOME ->
//                                DefaultView(
//                                    viewModel = knotx,
//                                    onThemeEditor = { showThemeEditor = true },
//                                    onNavigate = { currentScreen = it }
//                                )
//
//                            SettingsScreen.ACCOUNT ->
//                                AccountScreen(
//                                    knotx,
//                                    onNavigate = { currentScreen = it }
//                                )
//
//                            SettingsScreen.NOTIFICATIONS ->
//                                NotificationScreen(knotx)
//
//                            SettingsScreen.CHATS ->
//                                ChatsScreen(
//                                    viewModel = knotx,
//                                )
//
//                            SettingsScreen.CALLS ->
//                                CallsScreen(knotx)
//
//                            SettingsScreen.PRIVACY ->
//                                PrivacyScreen(
//                                    viewModel = knotx,
//                                    onOpenContactSelector = { showContactSelector = true },
//                                    onOpenBlockList = {
//                                        currentScreen = SettingsScreen.BLOCK_LIST
//                                    }
//                                )
//
//                            SettingsScreen.STORAGE_DATA ->
//                                StorageDataScreen(
//                                    knotx,
//                                    onNavigate = { currentScreen = it }
//                                )
//
//                            SettingsScreen.DATA_USAGE ->
//                                DataUsageScreen(knotx)
//
//                            SettingsScreen.APPEARANCE -> {
//                                showThemeEditor = true
//                            }
//
//                            SettingsScreen.LANGUAGE ->
//                                LanguageScreen(knotx)
//
//                            SettingsScreen.HELP_SUPPORT ->
//                                HelpSupportScreen()
//
//                            SettingsScreen.BACKUP ->
//                                BackupScreen(viewModel = knotx)
//
//                            SettingsScreen.BLOCK_LIST ->
//                                BlockListScreen(viewModel = knotx)
//
//                            SettingsScreen.CONTRIBUTIONS ->
//                                ContributionScreen(viewModel = knotx)
//
//                            SettingsScreen.SOS_CONTACTS ->
//                                SosContactScreen(viewModel = knotx)
//
//                            SettingsScreen.IMAGE_PREVIEW->{
//                                val profileImagePath by knotx.profileImagePathFlow.collectAsState(initial = null)
//                                ImagePreviewDialog(profileImagePath, viewModel = knotx,onDismiss = onDismiss)
//                            }
//
//                        }
//                    }
//                }
//            }
        }
    }
}
@StringRes
fun screenTitleRes(screen: SettingsScreen): Int = when (screen) {

    SettingsScreen.HOME ->
        R.string.settings

    SettingsScreen.ACCOUNT ->
        R.string.account_settings

    SettingsScreen.NOTIFICATIONS ->
        R.string.notifications

    SettingsScreen.CHATS ->
        R.string.chats

    SettingsScreen.CALLS ->
        R.string.calls

    SettingsScreen.PRIVACY ->
        R.string.privacy

    SettingsScreen.STORAGE_DATA ->
        R.string.storage_data

    SettingsScreen.DATA_USAGE ->
        R.string.data_usage

    SettingsScreen.APPEARANCE ->
        R.string.appearance

    SettingsScreen.LANGUAGE ->
        R.string.language

    SettingsScreen.HELP_SUPPORT ->
        R.string.help_support

    SettingsScreen.BACKUP ->
        R.string.backup

    SettingsScreen.BLOCK_LIST ->
        R.string.block_list

    SettingsScreen.CONTRIBUTIONS ->
        R.string.contribution

    SettingsScreen.SOS_CONTACTS ->
        R.string.sos_contacts

    SettingsScreen.IMAGE_PREVIEW ->
        R.string.profile_photo

    SettingsScreen.CONTACT_SELECTOR ->
        R.string.contact_support
}


@Composable
fun SettingItem(
    icon: Any,
    text: String,
    iconColor: Color = Color.Unspecified,
    textColor: Color = MaterialTheme.colorScheme.onSurface, // <-- Add this
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        when (icon) {
            is Int -> Image( // Drawable Resource
                painter = painterResource(id = icon),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                colorFilter = if (iconColor != Color.Unspecified) ColorFilter.tint(iconColor) else null
            )

            is ImageVector -> Icon( // Material Icon
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = iconColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = textColor, // <-- Apply the text color here
            style = MaterialTheme.typography.bodyLarge
        )
    }

}


// ---- Reusable pieces ----

@Composable
fun IconChip(icon: Any, tint: Color, bg: Color, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        when (icon) {
            is ImageVector -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            is Int -> Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ModernSettingItem(
    icon: Any,
    iconTint: Color,
    iconBg: Color,
    text: String,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, tint = iconTint, bg = iconBg)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Inset divider — starts after the icon column so it doesn't cut across the whole row
@Composable
fun InsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}

@Composable
fun ModernSettingGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}
// ---- Screen ----

@Composable
fun DefaultView(
    viewModel: Knotx,
    onThemeEditor: () -> Unit,
    onNavigate: (SettingsScreen) -> Unit
) {
    var showImagePreview by remember { mutableStateOf(false) }
    val profileImagePath by viewModel.profileImagePathFlow.collectAsState(initial = null)
//    var showDialog by remember { mutableStateOf(false) }
    var showContributeDialog by remember { mutableStateOf(false) }
    val userName by viewModel.userNameKhud.collectAsState(initial = "")

    // Category tints — reuse across items so the palette reads as intentional, not random
    val greenTint = Color(0xFF0F6E56); val greenBg = Color(0xFF0F6E56).copy(alpha = 0.12f)
    val purpleTint = Color(0xFF7F77DD); val purpleBg = Color(0xFF7F77DD).copy(alpha = 0.14f)
    val amberTint = Color(0xFF854F0B); val amberBg = Color(0xFFFAEEDA)
    val blueTint = MaterialTheme.colorScheme.primary; val blueBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize().padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

//            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(SettingsScreen.ACCOUNT) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {

                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { showImagePreview = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        tonalElevation = 0.dp
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                profileImagePath ?: R.drawable.bell_logo
                            ),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        userName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Tap to edit profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSettingGroup {
                ModernSettingItem(
                    R.drawable.icon_chat,
                    greenTint,
                    greenBg,
                    stringResource(R.string.chats)
                ) {
                    onNavigate(SettingsScreen.CHATS)
                }
                InsetDivider()
                ModernSettingItem(
                    Icons.Default.Notifications,
                    blueTint,
                    blueBg,
                    stringResource(R.string.notifications)
                ) {
                    onNavigate(SettingsScreen.NOTIFICATIONS)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSettingGroup {
                ModernSettingItem(
                    Icons.Filled.Call,
                    greenTint,
                    greenBg,
                    stringResource(R.string.calls)
                ) {
                    onNavigate(SettingsScreen.CALLS)
                }
                InsetDivider()
                ModernSettingItem(
                    Icons.Filled.PrivacyTip,
                    greenTint,
                    greenBg,
                    stringResource(R.string.privacy)
                ) {
                    onNavigate(SettingsScreen.PRIVACY)
                }
                InsetDivider()
                ModernSettingItem(
                    Icons.Filled.Storage,
                    greenTint,
                    greenBg,
                    stringResource(R.string.storage_data)
                ) {
                    onNavigate(SettingsScreen.STORAGE_DATA)
                }
                InsetDivider()
                ModernSettingItem(
                    Icons.Filled.Palette,
                    purpleTint,
                    purpleBg,
                    stringResource(R.string.appearance)
                ) {
                    onThemeEditor()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSettingGroup {
                ModernSettingItem(
                    Icons.Filled.Language,
                    greenTint,
                    greenBg,
                    stringResource(R.string.language)
                ) {
                    onNavigate(SettingsScreen.LANGUAGE)
                }
                InsetDivider()
                ModernSettingItem(
                    Icons.AutoMirrored.Filled.Help,
                    amberTint,
                    amberBg,
                    stringResource(R.string.help_support),
                    showChevron = true
                ) {
                    onNavigate(SettingsScreen.HELP_SUPPORT)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contribute banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE24B4A).copy(alpha = 0.10f))
                    .clickable { showContributeDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFA32D2D),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contribute",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFA32D2D),
                    modifier = Modifier.weight(1f)
                )

            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Version: ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "*d = under development",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }


    if (showContributeDialog) {
        ContributeDialog(
            onDismiss = { showContributeDialog = false },
            onContribute = { amount ->
                viewModel.createOrderID(amount)
                showContributeDialog = false
            }
        )
    }
    if (showImagePreview) {
        ImagePreviewDialog(
            profileImagePath = profileImagePath,
            viewModel = viewModel,
            onDismiss = { showImagePreview = false }
        )
    }
}


@Composable
fun ZoomableImageBox(
    imagePainter: Painter,
    modifier: Modifier = Modifier,
    boxSize: Dp = 300.dp
) {
    val boxPx = with(LocalDensity.current) { boxSize.toPx() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color.Gray, RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Image(
            painter = imagePainter,
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 3f)
                        val scaledPan = pan * scale // ✅ Increase pan speed with scale
                        val newOffset = offset + scaledPan


                        // Clamp offset to prevent showing outside the image
                        val maxX = (boxPx * (newScale - 1f)) / 2f
                        val maxY = (boxPx * (newScale - 1f)) / 2f
                        val clampedOffset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )

                        scale = newScale
                        offset = clampedOffset
                    }
                }
        )

        IconButton(
            onClick = {
                scale = 1f
                offset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp) // ⬅️ More distance from top/right
                .size(20.dp) // 🟢 Compact but tappable
                .background(
                    color = Color.White.copy(alpha = 0.15f), // 🔵 Subtle almost-invisible background
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.ZoomOut,
                contentDescription = "Reset zoom",
                tint = Color.Black.copy(alpha = 0.8f), // soft tint to blend with light UI
                modifier = Modifier.size(20.dp) // keeps icon balanced in smaller button
            )
        }
    }
}

@Composable
fun ImagePreviewDialog(
    profileImagePath: String?,
    viewModel: Knotx,
    onDismiss: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedUri = uri }

    LaunchedEffect(pickedUri) {
        val uri = pickedUri ?: return@LaunchedEffect
        sourceBitmap = withContext(Dispatchers.IO) { loadOrientedBitmap(context, uri, 2048) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ZoomableImageBox(
                imagePainter = rememberAsyncImagePainter(
                    model = profileImagePath?.takeIf { it.isNotEmpty() }
                        ?: R.drawable.bell_logo
                )
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    // ✏️ Edit (crop)
                    IconButton(onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF0a7171)
                        )
                    }

                    // 🗑️ Delete
                    IconButton(onClick = {
                        if (profileImagePath != null) showConfirmDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF0a7171)
                    )
                }
            }
        }
    )

    sourceBitmap?.let { bmp ->
        Dialog(
            onDismissRequest = { sourceBitmap = null; pickedUri = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(Modifier
                .fillMaxSize()
                .background(Color.Black)) {
                EditorCropScreen(
                    source = bmp,
                    initial = CropState(),
                    squareOnly = true,
                    onClose = { sourceBitmap = null; pickedUri = null },
                    onApply = { state ->
                        scope.launch {
                            val uriString = withContext(Dispatchers.IO) {
                                val out = renderCrop(bmp, state)
                                val file = File(
                                    context.filesDir,
                                    "profile_${System.currentTimeMillis()}.jpg"
                                )
                                FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                Uri.fromFile(file).toString()
                            }
                            viewModel.setProfileImageToStore(uriString)
                            viewModel.setProfileImage(uriString)
                            sourceBitmap = null
                            pickedUri = null
                        }
                    }
                )
            }
        }
    }

    if (showConfirmDialog) {
        DeleteProfileDialog(
            onConfirm = {
                viewModel.setProfileImageToStore(null)
                viewModel.setProfileImage(null)
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}



//region  Account


@Composable
fun EditableNameSetting(
    currentName: String,
    onNameChanged: (String) -> Unit,
    onContributionsClick: () -> Unit // 👈 NEW
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(currentName) }

    // Update tempName if currentName changes from outside (important when ViewModel updates)
    LaunchedEffect(currentName) {
        if (!isEditing) {
            tempName = currentName
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.name),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
            color = Color.Gray
        )
        if (isEditing)
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(1.dp)
            ) {
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 1.dp),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (tempName.isNotBlank()) {
                        onNameChanged(tempName.trim())
                        isEditing = false
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Update",
                        tint = Color.Green
                    )
                }
                IconButton(onClick = {
                    tempName = currentName // Reset edits
                    isEditing = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.Red
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditing = true }
//                    .padding(4.dp)
            ) {
                Text(
                    text = currentName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                // ❤️ Contributions icon
                IconButton(
                    onClick = { onContributionsClick() } // 👈 add this param
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Contributions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ✏️ Edit icon
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.Gray
                )
            }
        }
    }
}


@Composable
fun QrCodeDialog(
    content: String,
    onDismiss: () -> Unit,
    onSavePdf: (Bitmap) -> Unit = {}
) {
    val bitmap = remember(content) {
        generateQrCodeBitmap(content)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(stringResource(R.string.scan_to_share))
        },

        text = {
            if (bitmap != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_code),
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectionContainer {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(stringResource(R.string.qr_error))
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },

        confirmButton = {
            bitmap?.let { qrBitmap ->
                FilledTonalButton(
                    onClick = {
                        onSavePdf(qrBitmap)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = "Save PDF"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Save PDF")
                }
            }
        }
    )
}
fun generateQrCodeBitmap(text: String): Bitmap? {
    return try {
        val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = matrix.width
        val height = matrix.height
        val bmp = createBitmap(width, height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp[x, y] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ─────────────────────────────────────────────────────────────
// ASSUMED ViewModel additions (add these to Knotx if not present)
// ─────────────────────────────────────────────────────────────
//

//

//
// NOTE: The "@" is purely a DISPLAY/UI convention. Store userNameId in the DB and in
// SignalProtocolAddress lookups WITHOUT the @ — never persist or search on "@alice_92",
// always on "alice_92". The @ is prepended only when rendering to the user.

@Composable
fun AccountScreen(
    viewModel: Knotx,
    onNavigate: (SettingsScreen) -> Unit
) {
    val userName by viewModel.userNameKhud.collectAsState(initial = "")
    val phoneNumber by viewModel.userNumberKhud.collectAsState(initial = "")
    val userNameId by viewModel.userNameIdKhud.collectAsState(initial = "")
    val userNameIdCheckState by viewModel.userNameIdCheckState.collectAsState()
    val userNameIdCooldownUntil by viewModel.userNameIdCooldownUntil.collectAsState(initial = null)

    var showQrDialog by remember { mutableStateOf(false) }
    var showUserNameIdQrDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val about by viewModel.aboutKhud.collectAsState(initial = "")
    val isProcessing by viewModel.isProcessing.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())) {

            val coroutineScope = rememberCoroutineScope()
            AccountFieldsSection(
                userName = userName,
                onNameChanged = { newName ->
                    coroutineScope.launch {
                        viewModel.updateUserName(newName)
                    }
                },

                about = about,
                onAboutChanged = { newAbout ->
                    coroutineScope.launch {
                        viewModel.updateAbout(newAbout)
                    }
                },
                onContributionsClick = { onNavigate(SettingsScreen.CONTRIBUTIONS) },

                userNameId = userNameId,
                userNameIdCheckState = userNameIdCheckState,
                onUserNameIdCandidateChanged = { candidate ->
                    viewModel.checkUserNameIdAvailability(candidate)
                },
                onUserNameIdResetCheck = { candidate ->
                    viewModel.resetUserNameIdCheck(candidate) // 👈 now passes candidate through
                },
                onUserNameIdApply = { newVal ->
                    coroutineScope.launch {
                        viewModel.updateUserNameId(newVal)
                    }
                },
                onUserNameIdQrClick = { showUserNameIdQrDialog = true },

                phoneNumber = phoneNumber,
                onPhoneQrClick = { showQrDialog = true },
                onUserNameIdLocalValidationFailed = { candidate, reason ->
                    viewModel.setUserNameIdInvalid(reason, candidate) // 👈 uncommented, matches new signature
                },
                debounceMillis = 400L,
                userNameIdCooldownUntil = userNameIdCooldownUntil,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.security),
                modifier = Modifier.padding( bottom = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            val isPasscodeEnabled by viewModel.isPasscodeEnabledFlow.collectAsState(initial = false)
            val storedPasscode by viewModel.getPasscode().collectAsState(initial = "")

            PasscodeLockSetting(
                isPasscodeEnabled = isPasscodeEnabled,
                onEnablePasscode = { newPasscode ->
                    viewModel.setPasscode(newPasscode)
                },
                onDisablePasscode = {
                    viewModel.removePasscode()
                },
                getStoredPasscode = { storedPasscode ?: "" }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = stringResource(R.string.sos_contacts),
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(SettingsScreen.SOS_CONTACTS) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sos,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.sos_contacts),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Button(
                enabled = !isProcessing,
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(R.string.logout),
                    color = Color.White
                )
            }

            OutlinedButton(
                enabled = !isProcessing,
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_account))
            }
        }
    }
    val context = LocalContext.current
    if (showQrDialog) {
        QrCodeDialog(
            content = phoneNumber.toString(),
            onDismiss = { showQrDialog = false },
            onSavePdf = { bitmap ->

                val printManager =
                    context.getSystemService(Context.PRINT_SERVICE) as PrintManager

                printManager.print(
                    "Knotx QR",
                    QrPrintAdapter(
                        bitmap = bitmap,
                        text = phoneNumber.toString()
                    ),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
            }
        )
    }

    // ── NEW: QR dialog for the userNameId (deep link uses raw id, no @) ──
    if (showUserNameIdQrDialog) {
        QrCodeDialog(
            content = userNameId.toString(),
            onDismiss = { showUserNameIdQrDialog = false },
            onSavePdf = { bitmap ->

                val printManager =
                    context.getSystemService(Context.PRINT_SERVICE) as PrintManager

                printManager.print(
                    "Knotx QR",
                    QrPrintAdapter(
                        bitmap = bitmap,
                        text = userNameId.toString()
                    ),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.fullLocalCleanup()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showDeleteDialog) {
        var confirmText by remember { mutableStateOf("") }
        val confirmKeyword = stringResource(R.string.confirm_keyword)

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_warning))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        label = { Text(stringResource(R.string.type_confirm_label)) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = confirmText == confirmKeyword,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    }
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            showDeleteDialog = false
            showLogoutDialog = false
        }
    }

    if (isProcessing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.cleaning_up))
            }
        }
    }
}
@Composable
fun AccountFieldsSection(
    userName: String?,
    onNameChanged: (String) -> Unit,
    about: String?,
    onAboutChanged: (String) -> Unit,
    onContributionsClick: () -> Unit,

    userNameId: String?,
    userNameIdCheckState: UserNameIdCheckState,
    onUserNameIdCandidateChanged: (String) -> Unit,
    onUserNameIdResetCheck: (String) -> Unit,
    onUserNameIdApply: (String) -> Unit,
    onUserNameIdQrClick: () -> Unit,
    onUserNameIdLocalValidationFailed: (candidate: String, reason: String) -> Unit,

    phoneNumber: String?,
    onPhoneQrClick: () -> Unit,
    debounceMillis: Long = 400L,
    userNameIdCooldownUntil: Long?,
) {
    val context = LocalContext.current

    // ── state for Name row ──
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val isUserNameIdOnCooldown = userNameIdCooldownUntil != null && userNameIdCooldownUntil > System.currentTimeMillis()

    var isEditingAbout by remember { mutableStateOf(false) }
    var tempAbout by remember { mutableStateOf(about ?: "") }

    LaunchedEffect(about) {
        if (!isEditingAbout) {
            tempAbout = about ?: ""
        }
    }
    LaunchedEffect(userName) {
        if (!isEditingName) {
            tempName = userName ?: ""
            nameError = null
        }
    }

    LaunchedEffect(nameError) {
        if (nameError != null) {
            Toast.makeText(context, "Improper words are not allowed", Toast.LENGTH_SHORT).show()
        }
    }

    // ── state for Username row ──
    var isEditingUserNameId by remember { mutableStateOf(false) }

    var tempUserNameId by remember(userNameId) {
        mutableStateOf(
            TextFieldValue(
                text = userNameId?.ifBlank { "@" } ?: "@",
                selection = TextRange((userNameId?.length ?: 1))
            )
        )
    }

    LaunchedEffect(userNameId) {
        if (!isEditingUserNameId) {
            tempUserNameId = TextFieldValue(
                text = userNameId?.ifBlank { "@" } ?: "@",
                selection = TextRange((userNameId?.length ?: 1))
            )
        }
    }

    // 👈 NEW — fires SYNCHRONOUSLY on every keystroke, before the debounced network check.
    // This is what actually disables the apply button immediately, since userNameIdCheckState
    // won't reflect "Checking" until after `delay(debounceMillis)` below.
    LaunchedEffect(tempUserNameId.text) {
        if (isEditingUserNameId) {
            onUserNameIdResetCheck(tempUserNameId.text) // clears Available immediately, claims latestCandidate
        }
    }

    LaunchedEffect(tempUserNameId.text, isEditingUserNameId) {
        if (!isEditingUserNameId) return@LaunchedEffect
        val candidate = tempUserNameId.text

        if (candidate == (userNameId ?: "") || candidate.removePrefix("@").isBlank()) {
            onUserNameIdResetCheck(candidate)
            return@LaunchedEffect
        }

        if (containsBadWord(candidate.removePrefix("@"))) {
            Toast.makeText(context, "Improper words are not allowed", Toast.LENGTH_SHORT).show()
            onUserNameIdLocalValidationFailed(candidate, "Username not allowed")
            return@LaunchedEffect
        }

        delay(debounceMillis.milliseconds)
        onUserNameIdCandidateChanged(candidate)
    }

    Column(modifier = Modifier.fillMaxWidth(),verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ───────────────────────── NAME ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(horizontal = 8.dp)
        ) {
            if (isEditingName) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                ) {
                    TextField(
                        value = tempName,
                        onValueChange = { input ->
                            tempName = input
                            nameError = if (containsBadWord(input)) "Name not allowed" else null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 1.dp),
                        singleLine = true,
                        isError = nameError != null
                    )
                    IconButton(onClick = {
                        if (tempName.isNotBlank() && nameError == null) {
                            onNameChanged(tempName.trim())
                            isEditingName = false
                        }
                    }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Update",
                            tint = if (nameError == null && tempName.isNotBlank()) Color.Green else Color.Gray
                        )
                    }
                    IconButton(onClick = {
                        tempName = userName ?: ""
                        nameError = null
                        isEditingName = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                    }
                }
                nameError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingName = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary                        )

                        Text(
                            text = userName ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    IconButton(onClick = onContributionsClick) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = "Contributions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.Gray
                    )
                }            }
        }

        // ───────────────────────── USERNAME ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(horizontal = 8.dp)
        ) {

            if (isEditingUserNameId) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                ) {
                    TextField(
                        value = tempUserNameId,
                        onValueChange = { newValue ->
                            val text = newValue.text
                            tempUserNameId = if (!text.startsWith("@")) {
                                val fixed = "@$text"
                                newValue.copy(
                                    text = fixed,
                                    selection = TextRange((newValue.selection.start + 1).coerceIn(1, fixed.length))
                                )
                            } else {
                                newValue
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 1.dp),
                        singleLine = true,
                        trailingIcon = {
                            when (userNameIdCheckState) {
                                is UserNameIdCheckState.Checking ->
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                is UserNameIdCheckState.Available ->
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = Color.Green)
                                is UserNameIdCheckState.Taken ->
                                    Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = Color.Red)
                                is UserNameIdCheckState.Invalid ->
                                    Icon(Icons.Default.ErrorOutline, contentDescription = userNameIdCheckState.reason, tint = Color.Red)
                                is UserNameIdCheckState.Error ->
                                    Icon(Icons.Default.ErrorOutline, contentDescription = userNameIdCheckState.message, tint = Color.Red)
                                UserNameIdCheckState.Idle -> {}
                            }
                        },
                        isError = userNameIdCheckState is UserNameIdCheckState.Taken ||
                                userNameIdCheckState is UserNameIdCheckState.Invalid ||
                                userNameIdCheckState is UserNameIdCheckState.Error
                    )
                    // 👈 apply button — enabled ONLY when checkState is Available for the CURRENT text.
                    // Since onUserNameIdResetCheck fires synchronously on every keystroke (above),
                    // this can never stay stuck green from a stale/previous Available state.
                    IconButton(
                        onClick = {
                            if (userNameIdCheckState is UserNameIdCheckState.Available) {
                                onUserNameIdApply(tempUserNameId.text)
                                isEditingUserNameId = false
                            }
                        },
                        enabled = userNameIdCheckState is UserNameIdCheckState.Available
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Update",
                            tint = if (userNameIdCheckState is UserNameIdCheckState.Available) Color.Green else Color.Gray
                        )
                    }
                    IconButton(onClick = {
                        val resetValue = TextFieldValue(userNameId ?: "@", selection = TextRange((userNameId?.length ?: 1)))
                        tempUserNameId = resetValue
                        onUserNameIdResetCheck(resetValue.text)
                        isEditingUserNameId = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                    }
                }

                val helperText = when (userNameIdCheckState) {
                    is UserNameIdCheckState.Taken -> stringResource(R.string.username_taken)
                    is UserNameIdCheckState.Invalid -> userNameIdCheckState.reason
                    is UserNameIdCheckState.Error -> userNameIdCheckState.message
                    is UserNameIdCheckState.Available -> stringResource(R.string.username_available)
                    else -> null
                }
                helperText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (userNameIdCheckState is UserNameIdCheckState.Available) Color(0xFF2E7D32) else Color.Red,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isUserNameIdOnCooldown) {
                            isEditingUserNameId = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.username),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = (userNameId ?: "").ifBlank { "Set username" },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    IconButton(
                        onClick = onUserNameIdQrClick,
                        enabled = !userNameId.isNullOrBlank()
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = if (isUserNameIdOnCooldown) Color.LightGray else Color.Gray
                    )
                }
                if (isUserNameIdOnCooldown) {
                    val daysLeft = ((userNameIdCooldownUntil!! - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)) + 1
                    Text(
                        text = "You can change your username again in $daysLeft day${if (daysLeft != 1L) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(horizontal = 8.dp)
        ) {
            if (isEditingAbout) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                ) {
                    TextField(
                        value = tempAbout,
                        onValueChange = { tempAbout = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 1.dp),
                        singleLine = true,
                        placeholder = { Text("About") }
                    )

                    IconButton(onClick = {
                        onAboutChanged(tempAbout.trim())
                        isEditingAbout = false
                    }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Update",
                            tint = if (tempAbout.isNotBlank()) Color.Green else Color.Gray
                        )
                    }

                    IconButton(onClick = {
                        tempAbout = about ?: ""
                        isEditingAbout = false
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.Red
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditingAbout = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = about ?: "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.Gray
                    )
                }
            }
        }

        // ───────────────────────── PHONE (not editable) ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.phone_number),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = phoneNumber ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                IconButton(onClick = onPhoneQrClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.qr_code_scanner_24px),
                        contentDescription = "Show QR Code",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

    }
}
@Composable
fun EditableSettingItem(
    label: String,
    currentValue: String,
    isEditable: Boolean = true,
    prefix: String = "",                          // e.g. "@" for username, "" for name/phone
    checkState: UserNameIdCheckState? = null,      // null = no live availability check (name/phone)
    onValueChanged: ((String) -> Unit)? = null,    // used when checkState == null (simple fields like name)
    onCandidateChanged: ((String) -> Unit)? = null,// used when checkState != null (debounced check)
    onResetCheck: (() -> Unit)? = null,
    secondaryIcon: ImageVector? = null,            // e.g. Icons.Default.AccountBalanceWallet for "Contributions"
    onSecondaryIconClick: (() -> Unit)? = null,
    onQrClick: (() -> Unit)? = null,               // null = no QR icon shown
    debounceMillis: Long = 400L
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(currentValue) }

    // Sync from outside (ViewModel updates) while not actively editing
    LaunchedEffect(currentValue) {
        if (!isEditing) tempValue = currentValue
    }

    // Only runs the debounced check if this field actually uses one (i.e. username)
    LaunchedEffect(tempValue, isEditing) {
        if (!isEditing || checkState == null || onCandidateChanged == null) return@LaunchedEffect
        if (tempValue == currentValue || tempValue.removePrefix(prefix).isBlank()) {
            onResetCheck?.invoke()
            return@LaunchedEffect
        }
        delay(debounceMillis)
        onCandidateChanged(tempValue)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
            color = Color.Gray
        )

        if (isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(1.dp)
            ) {
                TextField(
                    value = tempValue,
                    onValueChange = { input ->
                        tempValue = if (prefix.isNotEmpty() && !input.startsWith(prefix))
                            "$prefix$input" else input
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 1.dp),
                    singleLine = true,
                    trailingIcon = checkState?.let {
                        {
                            when (it) {
                                is UserNameIdCheckState.Checking ->
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                is UserNameIdCheckState.Available ->
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = Color.Green)
                                is UserNameIdCheckState.Taken ->
                                    Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = Color.Red)
                                is UserNameIdCheckState.Invalid ->
                                    Icon(Icons.Default.ErrorOutline, contentDescription = it.reason, tint = Color.Red)
                                is UserNameIdCheckState.Error ->
                                    Icon(Icons.Default.ErrorOutline, contentDescription = it.message, tint = Color.Red)
                                UserNameIdCheckState.Idle -> {}
                            }
                        }
                    }
                )
                IconButton(onClick = {
                    val canApply = checkState == null || checkState is UserNameIdCheckState.Available
                    if (canApply && tempValue.removePrefix(prefix).isNotBlank()) {
                        if (checkState == null) onValueChanged?.invoke(tempValue.trim())
                        else onValueChanged?.invoke(tempValue) // username path also routes through onValueChanged as "apply"
                        isEditing = false
                    }
                }) {
                    val enabled = checkState == null || checkState is UserNameIdCheckState.Available
                    Icon(Icons.Default.Check, contentDescription = "Update", tint = if (enabled) Color.Green else Color.Gray)
                }
                IconButton(onClick = {
                    tempValue = currentValue
                    onResetCheck?.invoke()
                    isEditing = false
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isEditable) Modifier.clickable { isEditing = true } else Modifier)
            ) {
                Text(
                    text = currentValue.ifBlank { "Set $label" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = if (!isEditable) 4.dp else 0.dp)
                )

                if (secondaryIcon != null && onSecondaryIconClick != null) {
                    IconButton(onClick = onSecondaryIconClick) {
                        Icon(secondaryIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (onQrClick != null) {
                    IconButton(onClick = onQrClick) {
                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isEditable) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NEW composable: editable userNameId row — always displayed with @ prefix
// ─────────────────────────────────────────────────────────────
@Composable
fun EditableUserNameIdSetting(
    currentUserNameId: String,
    checkState: UserNameIdCheckState,
    onCandidateChanged: (String) -> Unit,
    onResetCheck: () -> Unit,
    onApply: (String) -> Unit,
    onQrClick: () -> Unit,
    debounceMillis: Long = 400L
) {
    var isEditing by remember { mutableStateOf(false) }
    // draft NEVER contains "@" — it's stripped on input so the DB/session lookups stay clean
    var draft by remember(currentUserNameId) { mutableStateOf(currentUserNameId) }

    LaunchedEffect(draft, isEditing) {
        if (!isEditing) return@LaunchedEffect
        if (draft == currentUserNameId || draft.isBlank()) {
            onResetCheck()
            return@LaunchedEffect
        }
        delay(debounceMillis)
        onCandidateChanged(draft)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.username), // label can stay "Username" for humans
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (isEditing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { input ->
                        // strip any "@" the user types themselves — we render it ourselves
                        draft = input.removePrefix("@").trim()
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("@") }, // always-visible, non-editable "@" in front
                    trailingIcon = {
                        when (checkState) {
                            is UserNameIdCheckState.Checking ->
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            is UserNameIdCheckState.Available ->
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.username_available),
                                    tint = Color(0xFF2E7D32) // green check
                                )
                            is UserNameIdCheckState.Taken ->
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = stringResource(R.string.username_taken),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            is UserNameIdCheckState.Invalid ->
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = checkState.reason,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            is UserNameIdCheckState.Error ->
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = checkState.message,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            UserNameIdCheckState.Idle -> {}
                        }
                    },
                    isError = checkState is UserNameIdCheckState.Taken ||
                            checkState is UserNameIdCheckState.Invalid ||
                            checkState is UserNameIdCheckState.Error
                )

                val helperText = when (checkState) {
                    is UserNameIdCheckState.Taken -> stringResource(R.string.username_taken)
                    is UserNameIdCheckState.Invalid -> checkState.reason
                    is UserNameIdCheckState.Error -> checkState.message
                    is UserNameIdCheckState.Available -> stringResource(R.string.username_available)
                    else -> null
                }
                helperText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checkState is UserNameIdCheckState.Available)
                            Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = {
                        isEditing = false
                        draft = currentUserNameId
                        onResetCheck()
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = checkState is UserNameIdCheckState.Available,
                        onClick = {
                            onApply(draft) // apply the raw id, WITHOUT "@"
                            isEditing = false
                        }
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            } else {
                Text(
                    text = if (currentUserNameId.isBlank()) stringResource(R.string.set_username)
                    else "@$currentUserNameId", // always rendered with @ in front
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (!isEditing) {
            IconButton(onClick = { isEditing = true; draft = currentUserNameId }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onQrClick, enabled = currentUserNameId.isNotBlank()) {
                Icon(Icons.Default.QrCode, contentDescription = stringResource(R.string.qr_code))
            }
        }
    }
}

//endregion
@Composable
fun ContributionItem(transaction: TransactionEntity) {

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier
                .clickable { expanded = !expanded } // 👈 whole card clickable
                .padding(12.dp)
                .animateContentSize() // 👈 smooth expand animation
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "₹${transaction.amount / 100}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = transaction.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (transaction.status) {
                            "SUCCESS" -> Color(0xFF2E7D32) // green
                            "FAILED" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // 🔽 Replace ugly arrows with icon
                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            // 🔽 Expanded Details
            if (expanded) {

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow("Payment ID", transaction.paymentId)
                DetailRow("Order ID", transaction.orderId)
                DetailRow("Method", transaction.method)
                DetailRow("Currency", transaction.currency)
                DetailRow("Date", formatDate(transaction.createdAt))
            }
        }
    }
}
@Composable
fun DetailRow(label: String, value: String?) {
    if (!value.isNullOrEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatDate(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

@Composable
fun ContributionScreen(
    viewModel: Knotx
) {

    var contributions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        contributions = viewModel.getAllTransactionsOnce()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            if (contributions.isEmpty()) {
                Text("No contributions Yet")
            } else {
                contributions.forEach { transaction ->
                    ContributionItem(transaction)
                }
            }
        }
    }
}

//endregion
@Composable
fun LEDIndicatorSetting(viewModel: Knotx) {

    val ledOptions = listOf(
        "OFF",
        "RED",
        "GREEN",
        "BLUE",
        "YELLOW",
        "PURPLE"
    )

    val settings by viewModel.settingsFlow.collectAsState()

    // ✅ Use stable default key (uppercase)
    val currentLED =
        settings[SettingPrefKeys.NOTIFICATION_LED] as? String ?: "OFF"

    var selectedLED by remember(currentLED) {
        mutableStateOf(currentLED)
    }

    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        ) {

            Text(
                text = stringResource(R.string.led_indicator),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(getStringResForLED(selectedLED)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.select_led_color),
                tint = Color.Gray
            )
        }

        if (showDialog) {

            AlertDialog(
                onDismissRequest = { showDialog = false },

                title = {
                    Text(stringResource(R.string.select_led_color))
                },

                text = {
                    Column {

                        ledOptions.forEach { option ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLED = option
                                        viewModel.updateStringSetting(
                                            SettingPrefKeys.NOTIFICATION_LED,
                                            option
                                        )
                                        showDialog = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            getColorForLED(option),
                                            shape = CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            Color.Gray,
                                            CircleShape
                                        )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = stringResource(
                                        getStringResForLED(option)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },

                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun getColorForLED(option: String): Color {
    return when (option) {
        "Red" -> Color.Red
        "Green" -> Color.Green
        "Blue" -> Color.Blue
        "Yellow" -> Color.Yellow
        "Purple" -> Color(0xFF800080) // Purple color
        else -> Color.Transparent // "Off" or default
    }
}
// Helper function to get LED Color
@StringRes
fun getStringResForLED(value: String): Int {
    return when (value) {
        "RED" -> R.string.led_red
        "GREEN" -> R.string.led_green
        "BLUE" -> R.string.led_blue
        "YELLOW" -> R.string.led_yellow
        "PURPLE" -> R.string.led_purple
        else -> R.string.led_off
    }
}
@Composable
fun NotificationScreen(viewModel: Knotx) {

    val settings by viewModel.settingsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        ) {

            Text(
                text = stringResource(R.string.global),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingItemWithToggle(
                label = stringResource(R.string.enable_all_notifications),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_ENABLE_ALL] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_ENABLE_ALL,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.new_message_sound),
                isChecked = settings[SettingPrefKeys.MESSAGE_SOUND] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.MESSAGE_SOUND,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.vibrate_on_message),
                isChecked = settings[SettingPrefKeys.MESSAGE_VIBRATION] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.MESSAGE_VIBRATION,
                    isEnabled
                )
            }

            LEDIndicatorSetting(viewModel)

            // 🔹 MESSAGE NOTIFICATIONS
            HorizontalDivider()

            Text(
                text = stringResource(R.string.message_notifications),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingItemWithToggle(
                label = stringResource(R.string.new_messages),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_NEW_MESSAGES] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_NEW_MESSAGES,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.group_chats),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_GROUP_CHATS] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_GROUP_CHATS,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.mentions),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_MENTIONS] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_MENTIONS,
                    isEnabled
                )
            }

            // 🔹 CALL NOTIFICATIONS
            HorizontalDivider()

            Text(
                text = stringResource(R.string.call_notifications),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingItemWithToggle(
                label = stringResource(R.string.incoming_calls),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_INCOMING_CALLS] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_INCOMING_CALLS,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.missed_call_alerts),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_MISSED_CALL_ALERTS] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_MISSED_CALL_ALERTS,
                    isEnabled
                )
            }

            // 🔹 IN-APP NOTIFICATIONS
            HorizontalDivider()

            Text(
                text = stringResource(R.string.in_app_notifications),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingItemWithToggle(
                label = stringResource(R.string.popup_previews),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_POPUP_PREVIEWS] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_POPUP_PREVIEWS,
                    isEnabled
                )
            }

            SettingItemWithToggle(
                label = stringResource(R.string.badge_count),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_BADGE_COUNT] as? Boolean ?: true
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_BADGE_COUNT,
                    isEnabled
                )
            }

            // 🔹 DO NOT DISTURB
            HorizontalDivider()

            Text(
                text = stringResource(R.string.do_not_disturb),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SettingItemWithToggle(
                label = stringResource(R.string.enable_dnd),
                isChecked = settings[SettingPrefKeys.NOTIFICATION_DND_ENABLE] as? Boolean ?: false
            ) { isEnabled ->
                viewModel.updateBooleanSetting(
                    SettingPrefKeys.NOTIFICATION_DND_ENABLE,
                    isEnabled
                )
            }
        }
    }
}


@Composable
fun SettingItemWithToggle(
    label: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)        // 🔥 This fixes wrapping
                .padding(end = 16.dp)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.Gray.copy(alpha = 0.5f),
                checkedTrackColor = Color(0xFFD81B60),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}


@Composable
fun SettingItemWithSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Label text for the slider
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding( top = 8.dp)
        )

        // Slider for font size or other values
        Slider(
            value = value,
            onValueChange = { newValue -> onValueChange(newValue) },
            valueRange = 10f..30f, // Adjust the range based on your font size needs
            steps = 20, // This determines the steps between the min and max range values
            modifier = Modifier
        )

        // Displaying the current value next to the slider (optional)
        Text(
            text = stringResource(
                R.string.font_size_value,
                value.toInt()
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

//region   CHAT
@Composable
fun ChatsScreen(
    viewModel: Knotx,
) {
    val settings by viewModel.settingsFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Keep all your existing settings items here
            SettingItemWithSlider(
                label = stringResource(R.string.font_size),
                value = (settings[SettingPrefKeys.CHAT_CHAT_FONT_SIZE] as? Int)?.toFloat() ?: 16f

            ) { newValue ->
                viewModel.updateIntSetting(SettingPrefKeys.CHAT_CHAT_FONT_SIZE, newValue.toInt())

            }

            SettingItemWithToggle(
                label = stringResource(R.string.enter_key_is_send),
                isChecked = settings[SettingPrefKeys.CHAT_CHAT_ENTER_KEY_BEHAVIOR] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_CHAT_ENTER_KEY_BEHAVIOR, newValue)
            }

            // Messages Settings Section
            SettingItemWithToggle(
                label = stringResource(R.string.read_receipts),
                isChecked = settings[SettingPrefKeys.CHAT_MESSAGE_READ_RECEIPTS] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MESSAGE_READ_RECEIPTS, newValue)
            }

            SettingItemWithToggle(
                label = stringResource(R.string.typing_indicators),
                isChecked = settings[SettingPrefKeys.CHAT_MESSAGE_TYPING_INDICATORS] as? Boolean
                        == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MESSAGE_TYPING_INDICATORS, newValue)
            }


            // Media Settings Section
            SettingItemWithToggle(
                label = stringResource(R.string.auto_download_photos),
                isChecked = settings[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_PHOTOS] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_PHOTOS, newValue)
            }

            SettingItemWithToggle(
                label = stringResource(R.string.auto_download_videos),
                isChecked = settings[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VIDEOS] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VIDEOS, newValue)
            }

            SettingItemWithToggle(
                label = stringResource(R.string.auto_download_voice),
                isChecked = settings[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VOICE] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_VOICE, newValue)
            }

            SettingItemWithToggle(
                label = stringResource(R.string.auto_download_files),
                isChecked = settings[SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_FILES] as? Boolean
                        == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MEDIA_AUTO_DOWNLOAD_FILES, newValue)
            }


            SettingItemWithToggle(
                label = stringResource(R.string.save_to_gallery),
                isChecked = settings[SettingPrefKeys.CHAT_MEDIA_SAVE_TO_GALLERY] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_MEDIA_SAVE_TO_GALLERY, newValue)
            }

            // Group Chats Settings Section
            SettingItemWithToggle(
                label = stringResource(R.string.mute_all_groups),
                isChecked = settings[SettingPrefKeys.CHAT_GROUP_CHAT_MUTE_ALL] as? Boolean == true
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CHAT_GROUP_CHAT_MUTE_ALL, newValue)
            }
        }
    }
}


@Composable
fun SettingItemWithText(
    label: String,
    value: String,
    onQrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .padding(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .weight(1f)
            )
            IconButton(onClick = onQrClick) {
                Icon(
                    painter = painterResource(id = R.drawable.qr_code_scanner_24px),
                    contentDescription = "Show QR Code",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}



@Composable
fun PasscodeLockSetting(
    isPasscodeEnabled: Boolean,
    onEnablePasscode: (String) -> Unit,
    onDisablePasscode: (String) -> Unit, // 👈 Require current passcode to disable
    getStoredPasscode: () -> String // 👈 Lambda to get the stored passcode
) {
    var isSettingPasscode by remember { mutableStateOf(false) }
    var isVerifyingCurrentPasscode by remember { mutableStateOf(false) }
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var currentPasscode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Toggle Switch
        Row(
            modifier = Modifier
                .fillMaxWidth(),
//                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_passcode),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 16.dp)
            )
            Switch(
                checked = isPasscodeEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        isSettingPasscode = true
                        isVerifyingCurrentPasscode = false
                    } else {
                        isVerifyingCurrentPasscode = true // 👈 Start verifying to disable
                        isSettingPasscode = false
                    }
                }
            )
        }

        // UI for setting a new passcode
        if (isSettingPasscode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.enter_4_digit_passcode), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                TextField(
                    value = passcode,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 4) passcode = it
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.confirm_passcode), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                TextField(
                    value = confirmPasscode,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 4) confirmPasscode = it
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { resId ->
                    Text(
                        text = stringResource(resId),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }


                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(
                        onClick = {
                            passcode = ""
                            confirmPasscode = ""
                            errorMessage = null
                            isSettingPasscode = false
                        }
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (passcode.length < 4) {
                                errorMessage = R.string.passcode_must_be_4_digits
                                return@Button
                            }
                            if (passcode == confirmPasscode) {
                                onEnablePasscode(passcode)
                                isSettingPasscode = false
                                errorMessage = null
                            } else {
                                errorMessage = R.string.passcodes_do_not_match
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }

            }
        }

        // UI for verifying current passcode to disable
        if (isVerifyingCurrentPasscode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.enter_current_passcode), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                TextField(
                    value = currentPasscode,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 4) currentPasscode = it
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { resId ->
                    Text(
                        text = stringResource(resId),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        currentPasscode = ""
                        errorMessage = null
                        isVerifyingCurrentPasscode = false
                    }) {
                        Text(stringResource(R.string.cancel), color = Color.Red)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        if (currentPasscode == getStoredPasscode()) {
                            onDisablePasscode(currentPasscode)
                            isVerifyingCurrentPasscode = false
                            errorMessage = null
                        } else {
                            errorMessage = R.string.incorrect_passcode
                        }
                    }) {
                        Text(stringResource(R.string.disable))
                    }
                }
            }
        }
    }
}
//endregion
//region CALLs


@Composable
fun RingtoneSelector(label: String, selectedRingtone: String?, onRingtoneSelected: (Uri) -> Unit) {
    val context = LocalContext.current
    var ringtoneTitle by remember { mutableStateOf(selectedRingtone ?: "Default") }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                (result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI))
            }

            uri?.let {
                ringtoneTitle =
                    RingtoneManager.getRingtone(context, it)?.getTitle(context) ?: "Custom Ringtone"
                onRingtoneSelected(it)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                }
                ringtonePickerLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(ringtoneTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItemWithDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Using ExposedDropdownMenuBox is the standard M3 way for selections
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedOption,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}


@Composable
fun CallsScreen(viewModel: Knotx) {
    // Collect settings as State from the DataStore Flow
    val settings by viewModel.settingsFlow.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
//            HorizontalDivider()

            // Ringtone Selection
            RingtoneSelector(
                label = stringResource(R.string.ringtone),
                selectedRingtone = settings[SettingPrefKeys.CALL_RINGTONE] as? String,
                onRingtoneSelected = { newValue ->
                    viewModel.updateStringSetting(SettingPrefKeys.CALL_RINGTONE,
                        newValue.toString()
                    )
                }
            )

            // Video Call Quality Selection
            SettingItemWithDropdown(
                label = stringResource(R.string.video_call_quality),
                options = listOf("Low", "Standard", "HD"),
                selectedOption = settings[SettingPrefKeys.CALL_VIDEO_QUALITY] as? String ?: "Standard"
            ) { newValue ->
                viewModel.updateStringSetting(SettingPrefKeys.CALL_VIDEO_QUALITY, newValue)
            }

            // Speaker by Default Toggle
            SettingItemWithToggle(
                label = stringResource(R.string.speaker_by_default),
                isChecked = settings[SettingPrefKeys.CALL_SPEAKER_DEFAULT] as? Boolean == false
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.CALL_SPEAKER_DEFAULT, newValue)
            }

        }
    }
}

//endregion


//region PRIVACY

@StringRes
fun privacyLabelRes(value: Int): Int = when (value) {
    0 -> R.string.everyone
    1 -> R.string.contact_list
    2 -> R.string.nobody
    else -> R.string.everyone
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingItemWithDropdown(
    @StringRes labelRes: Int,
    options: List<Int>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        Surface(
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            onClick = { expanded = true }
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(privacyLabelRes(selectedOption)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            options.forEach { value ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(privacyLabelRes(value)),
                            fontWeight =
                                if (value == selectedOption)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}


fun privacyLabelFromValue(value: Int): String = when (value) {
    0    -> "Everyone"
    1    -> "Contact List"
    2    -> "Nobody"
    else -> "Everyone"
}
@Composable
fun PrivacyScreen(viewModel: Knotx,
                  onOpenContactSelector: () -> Unit,
                  onOpenBlockList: () -> Unit) {
    // Collect settings as State from the DataStore Flow
    val settings by viewModel.settingsFlow.collectAsState()
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Header Section
//            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable {
                        onOpenContactSelector()
                    }
            ) {
                Text(stringResource(R.string.contact_list), modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "List"
                )
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable {
                        onOpenBlockList()   // you create this callback
                    }
            ) {
                Text(stringResource(R.string.blocked_contacts), modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Blocked Contacts"
                )
            }

            HorizontalDivider()
            // Visibility Settings
            SettingItemWithDropdown(
                labelRes = R.string.last_seen,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_LAST_SEEN] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.LAST_SEEN,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_LAST_SEEN,
                    newValue
                )
            }


            SettingItemWithDropdown(
                labelRes = R.string.online_status,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_ONLINE_STATUS] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.ONLINE_STATUS,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_ONLINE_STATUS,
                    newValue
                )
            }


            SettingItemWithDropdown(
                labelRes = R.string.profile_photo,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_PROFILE_PHOTO] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.PROFILE_PHOTO,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_PROFILE_PHOTO,
                    newValue
                )
            }


            SettingItemWithDropdown(
                labelRes = R.string.bio_about,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_BIO] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.BIO,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_BIO,
                    newValue
                )
            }

// Messaging Privacy
            SettingItemWithDropdown(
                labelRes = R.string.who_can_message_me,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_MESSAGES] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.MESSAGES,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_MESSAGES,
                    newValue
                )
            }

            SettingItemWithDropdown(
                labelRes = R.string.group_invites,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.PRIVACY_GROUP_INVITES] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.GROUP_INVITES,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.PRIVACY_GROUP_INVITES,
                    newValue
                )
            }

            SettingItemWithDropdown(
                labelRes = R.string.allow_calls_from,
                options = listOf(0, 1, 2),
                selectedOption = (settings[SettingPrefKeys.CALL_ALLOW_FROM] as? Int) ?: 0
            ) { newValue ->

                viewModel.updateServerPrivacySetting(
                    PrivacyType.CALLS,
                    newValue
                )

                viewModel.updateIntSetting(
                    SettingPrefKeys.CALL_ALLOW_FROM,
                    newValue
                )
            }


            //todo complete them later
            // Security Alerts
            SettingItemWithToggle(
                label = stringResource( R.string.notify_new_device),
                isChecked = settings[SettingPrefKeys.PRIVACY_LOGIN_ALERT] as? Boolean == false
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.PRIVACY_LOGIN_ALERT, newValue)
            }

            //todo complete them later
            SettingItemWithToggle(
                label = stringResource( R.string.suspicious_activity_alerts),
                isChecked = settings[SettingPrefKeys.PRIVACY_SUSPICIOUS_ACTIVITY] as? Boolean == false
            ) { newValue ->
                viewModel.updateBooleanSetting(SettingPrefKeys.PRIVACY_SUSPICIOUS_ACTIVITY, newValue)
            }
        }
    }
}
//endregion
//region STORAGE & DATA

fun formatBytes(bytes: Int): String {
    return when {
        bytes >= 1_000_000 -> String.format(Locale.US, "%.2f MB", bytes / 1_000_000f)
        bytes >= 1_000 -> String.format(Locale.US, "%.1f KB", bytes / 1_000f)
        else -> "$bytes B"
    }
}


@Composable
fun DataUsageScreen(viewModel: Knotx) {
    val settings by viewModel.settingsFlow.collectAsState()
    fun getInt(key: Preferences.Key<Int>): Int = settings[key] as? Int ?: 0

    // Mobile Data Usage
    val mobileMessageSent = getInt(SettingPrefKeys.DATA_USAGE_MOBILE_MESSAGE_SENT)
    val mobileMessageReceived = getInt(SettingPrefKeys.DATA_USAGE_MOBILE_MESSAGE_RECEIVED)
    val mobileMediaSent = getInt(SettingPrefKeys.DATA_USAGE_MOBILE_MEDIA_SENT)
    val mobileMediaReceived = getInt(SettingPrefKeys.DATA_USAGE_MOBILE_MEDIA_RECEIVED)

    // Wi-Fi Data Usage
    val wifiMessageSent = getInt(SettingPrefKeys.DATA_USAGE_WIFI_MESSAGE_SENT)
    val wifiMessageReceived = getInt(SettingPrefKeys.DATA_USAGE_WIFI_MESSAGE_RECEIVED)
    val wifiMediaSent = getInt(SettingPrefKeys.DATA_USAGE_WIFI_MEDIA_SENT)
    val wifiMediaReceived = getInt(SettingPrefKeys.DATA_USAGE_WIFI_MEDIA_RECEIVED)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.data_usage),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DataUsageChart(
                title = stringResource(R.string.mobile_data_title),
                sentMessage = mobileMessageSent,
                receivedMessage = mobileMessageReceived,
                sentMedia = mobileMediaSent,
                receivedMedia = mobileMediaReceived
            )

            Spacer(modifier = Modifier.height(24.dp))

            DataUsageChart(
                title = stringResource(R.string.wifi_title),
                sentMessage = wifiMessageSent,
                receivedMessage = wifiMessageReceived,
                sentMedia = wifiMediaSent,
                receivedMedia = wifiMediaReceived
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Reset all 8 values
                    listOf(
                        SettingPrefKeys.DATA_USAGE_MOBILE_MESSAGE_SENT,
                        SettingPrefKeys.DATA_USAGE_MOBILE_MESSAGE_RECEIVED,
                        SettingPrefKeys.DATA_USAGE_MOBILE_MEDIA_SENT,
                        SettingPrefKeys.DATA_USAGE_MOBILE_MEDIA_RECEIVED,
                        SettingPrefKeys.DATA_USAGE_WIFI_MESSAGE_SENT,
                        SettingPrefKeys.DATA_USAGE_WIFI_MESSAGE_RECEIVED,
                        SettingPrefKeys.DATA_USAGE_WIFI_MEDIA_SENT,
                        SettingPrefKeys.DATA_USAGE_WIFI_MEDIA_RECEIVED
                    ).forEach {
                        viewModel.updateIntSetting(it, 0)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.reset_all_data_usage))
            }
        }
    }
}

@Composable
fun RatioBar(
    sent: Int,
    received: Int,
    modifier: Modifier = Modifier,
    sentColor: Color = Color(0xFF1976D2),
    receivedColor: Color = Color(0xFF4CAF50),
    height: Dp = 16.dp,
    maxWidth: Dp = 150.dp
) {
    val total = (sent + received).coerceAtLeast(1) // avoid div by zero
    var sentFraction = sent.toFloat() / total
    var receivedFraction = received.toFloat() / total

    // Prevent zero weight by setting a minimum fraction if > 0
    val minFraction = 0.01f

    if (sent > 0 && sentFraction < minFraction) sentFraction = minFraction
    if (received > 0 && receivedFraction < minFraction) receivedFraction = minFraction

    // Normalize fractions if needed to ensure sum is 1f
    val sumFractions = sentFraction + receivedFraction
    sentFraction /= sumFractions
    receivedFraction /= sumFractions

    Row(modifier = modifier
        .width(maxWidth)
        .height(height)) {
        if (sent > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(sentFraction)
                    .background(sentColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            )
        }
        if (received > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(receivedFraction)
                    .background(receivedColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            )
        }
    }
}

@Composable
fun DataUsageChart(
    title: String,
    sentMessage: Int,
    receivedMessage: Int,
    sentMedia: Int,
    receivedMedia: Int
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Messages Ratio Bar and labels in one row
        Text(stringResource(R.string.messages))
        RatioBar(
            sent = sentMessage,
            received = receivedMessage,
            sentColor = Color(0xFF1976D2),
            receivedColor = Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Text("${formatBytes(sentMessage)} sent", style = MaterialTheme.typography.bodySmall)
            Text(
                "${formatBytes(sentMessage)} ${stringResource(R.string.sent)}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "${formatBytes(receivedMessage)} ${stringResource(R.string.received)}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Ratio Bar and labels in one row
        Text(stringResource(R.string.media))
        RatioBar(
            sent = sentMedia,
            received = receivedMedia,
            sentColor = Color(0xFF42A5F5),
            receivedColor = Color(0xFF66BB6A),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
//            Text("${formatBytes(sentMedia)} sent", style = MaterialTheme.typography.bodySmall)
            Text(
                "${formatBytes(sentMedia)} ${stringResource(R.string.sent)}",
                style = MaterialTheme.typography.bodySmall
            )
//            Text("${formatBytes(receivedMedia)} received", style = MaterialTheme.typography.bodySmall)
            Text(
                "${formatBytes(receivedMedia)} ${stringResource(R.string.received)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}



@Composable
fun StorageDataScreen(
    viewModel: Knotx,
    onNavigate: (SettingsScreen) -> Unit
) {


    val settings by viewModel.settingsFlow.collectAsState()

    val mobileLimit =
        settings[SettingPrefKeys.MOBILE_DATA_LIMIT] as? String ?: "UNLIMITED"

    val wifiUsage =
        settings[SettingPrefKeys.WIFI_USAGE] as? String ?: "UNLIMITED"

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
//
//            // 🔹 Title
//            Text(
//                text = stringResource(R.string.storage_data),
//                style = MaterialTheme.typography.headlineSmall,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            HorizontalDivider()

            // 🔹 Data Usage Section
            Text(
                text = stringResource(R.string.data_usage),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // 📱 Mobile Data Limit
            SettingItemWithDropdown(
                label = stringResource(R.string.mobile_data_limit),
                options = listOf(
                    "100MB",
                    "500MB",
                    "1GB",
                    stringResource(R.string.unlimited)
                ),
                selectedOption = mobileLimit
            ) { newValue ->
                viewModel.updateStringSetting(
                    SettingPrefKeys.MOBILE_DATA_LIMIT,
                    newValue
                )
            }

            // 📶 Wi-Fi Usage
            SettingItemWithDropdown(
                label = stringResource(R.string.wifi_usage),
                options = listOf(
                    stringResource(R.string.unlimited),
                    stringResource(R.string.restricted)
                ),
                selectedOption = wifiUsage
            ) { newValue ->
                viewModel.updateStringSetting(
                    SettingPrefKeys.WIFI_USAGE,
                    newValue
                )
            }

            // 📊 Data Usage Screen
            SettingItemWithNavigation(
                stringResource(R.string.data_usage)
            ) {
                onNavigate(SettingsScreen.DATA_USAGE)
            }

            HorizontalDivider()

            // 🔹 Storage Management Section
            Text(
                text = stringResource(R.string.storage_management),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            //todo complete it later and uncomment
//            SettingItemWithNavigation(
//                stringResource(R.string.view_storage_usage)
//            ) {
//                // TODO: open detailed breakdown
//            }

            HorizontalDivider()

            SettingItemWithNavigation(
                stringResource(R.string.backup_restore)
            ) {
                onNavigate(SettingsScreen.BACKUP)
            }
        }
    }
}




    @Composable
    fun BackupScreen(viewModel: Knotx) {
        val settingsMap by viewModel.settingsFlow.collectAsState()
        val enableBackup = settingsMap[SettingPrefKeys.ENABLE_BACKUP] as? Boolean ?: false
        val includeMedia = settingsMap[SettingPrefKeys.INCLUDE_MEDIA] as? Boolean ?: false
        val backupFrequency = settingsMap[SettingPrefKeys.BACKUP_FREQUENCY_DAYS] as? Int ?: 7
        val lastBackupTime = settingsMap[SettingPrefKeys.LAST_BACKUP_TIME] as? Long ?: 0L
        val retentionCount = settingsMap[SettingPrefKeys.BACKUP_RETENTION_COUNT] as? Int ?: 7
        val mediaRetentionMonths = settingsMap[SettingPrefKeys.MEDIA_RETENTION_MONTHS] as? Int ?: -1
        val wifiOnly = settingsMap[SettingPrefKeys.BACKUP_WIFI_ONLY] as? Boolean ?: true
        var restoreProgress by remember { mutableStateOf(0) }
        var restoreMessage by remember { mutableStateOf("") }
        var isRestoring by remember { mutableStateOf(false) }


        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        fun Context.findActivity(): Activity {
            var ctx = this
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                ctx = ctx.baseContext
            }
            error("No Activity found")
        }

        val activity = context.findActivity()

        // State
        var accountEmail by remember { mutableStateOf<String?>(null) }
        var backupList by remember { mutableStateOf<List<DriveBackupItem>>(emptyList()) }
        var showRestoreDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var isSigningIn by remember { mutableStateOf(false) }
        var driveStorageUsed by remember { mutableStateOf(0L) }

        // Strings
        val backupStarted = stringResource(R.string.backup_started)
        val restoreStarted = stringResource(R.string.restore_started)
        val signingFailed = "Sign-in failed"


        val authorizationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val authResult = GoogleAuthHelper.handleAuthorizationResult(context, result.data)
                if (authResult != null) {
                    val email = GoogleAuthHelper.getSavedEmail(context)
                    if (email != null) {
                        accountEmail = email
                        scope.launch {
                            backupList = DriveHelper.listBackups(context, email)
                            driveStorageUsed = DriveHelper.getDriveStorageUsed(context, email)
    //                        if (lastBackupTime == 0L) {
    //                            isLoading = true
    //                            val success = BackupManager.startBackup(
    //                                context, email, includeMedia, retentionCount, mediaRetentionMonths
    //                            )
    //                            if (success) {
    //                                viewModel.updateLongSetting(
    //                                    SettingPrefKeys.LAST_BACKUP_TIME,
    //                                    System.currentTimeMillis()
    //                                )
    //                                backupList = DriveHelper.listBackups(context, email)
    //                                driveStorageUsed = DriveHelper.getDriveStorageUsed(context, email)
    //                            }
    //                            isLoading = false
    //                        }
                        }
                    }
                }
            }
            isSigningIn = false
        }

        LaunchedEffect(Unit) {
            val email = GoogleAuthHelper.getSavedEmail(context)
            accountEmail = email
            if (email != null) {
                backupList = DriveHelper.listBackups(context, email)
                driveStorageUsed = DriveHelper.getDriveStorageUsed(context, email)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                // ── HEADER ────────────────────────────────────────────────────────────
                Text(
                    "Google Drive Backup",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )


                // ── ACCOUNT SECTION ───────────────────────────────────────────────────
                if (accountEmail == null) {
                    // Connect button
                    Button(
                        onClick = {
                            isSigningIn = true
                            scope.launch {
                                // Step 1: Sign in with Credential Manager
                                val credential = GoogleAuthHelper.signIn(activity)
                                if (credential != null) {
                                    val email = credential.id
                                    GoogleAuthHelper.saveEmail(context, email)
                                    // Step 2: Request Drive authorization
                                    GoogleAuthHelper.requestDriveAuthorization(
                                        activity,
                                        authorizationLauncher,
                                        onAlreadyGranted = {
                                            accountEmail = email
                                            scope.launch {
                                                backupList = DriveHelper.listBackups(context, email)
                                                driveStorageUsed =
                                                    DriveHelper.getDriveStorageUsed(context, email)
                                                //                                        if (lastBackupTime == 0L) {
                                                //                                            isLoading = true
                                                //                                            val success = BackupManager.startBackup(
                                                //                                                context, email, includeMedia, retentionCount, mediaRetentionMonths
                                                //                                            )
                                                //                                            if (success) {
                                                //                                                viewModel.updateLongSetting(
                                                //                                                    SettingPrefKeys.LAST_BACKUP_TIME,
                                                //                                                    System.currentTimeMillis()
                                                //                                                )
                                                //                                                backupList = DriveHelper.listBackups(context, email)
                                                //                                                driveStorageUsed = DriveHelper.getDriveStorageUsed(context, email)
                                                //                                            }
                                                //                                            isLoading = false
                                                //                                        }
                                            }
                                            isSigningIn = false
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, signingFailed, Toast.LENGTH_SHORT)
                                        .show()
                                    isSigningIn = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (isSigningIn) "Connecting..." else "Connect Google Drive")
                    }
                } else {
                    // Account card
                    ElevatedCard(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Connected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    accountEmail!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = {
                                    GoogleAuthHelper.signOut(context) {
                                        accountEmail = null
                                        backupList = emptyList()
                                        driveStorageUsed = 0L
                                        BackupScheduler.cancel(context)
                                        viewModel.updateBooleanSetting(
                                            SettingPrefKeys.ENABLE_BACKUP,
                                            false
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))


// ── SETTINGS SECTION ──────────────────────────────────────────────
                    ElevatedCard(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            // Include media
                            SettingRow(label = stringResource(R.string.include_media)) {
                                Switch(
                                    checked = includeMedia,
                                    onCheckedChange = {
                                        viewModel.updateBooleanSetting(
                                            SettingPrefKeys.INCLUDE_MEDIA,
                                            it
                                        )
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Wi-Fi only
                            SettingRow(label = "Wi-Fi only") {
                                Switch(
                                    checked = wifiOnly,
                                    onCheckedChange = {
                                        viewModel.updateBooleanSetting(
                                            SettingPrefKeys.BACKUP_WIFI_ONLY,
                                            it
                                        )
                                        if (accountEmail != null && lastBackupTime > 0L) {
                                            BackupScheduler.schedule(
                                                context,
                                                backupFrequency.toLong(),
                                                accountEmail!!,
                                                includeMedia
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── SCHEDULE & RETENTION ──────────────────────────────────────────
                    ElevatedCard(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                            // Frequency
                            var freqExpanded by remember { mutableStateOf(false) }
                            val freqOptions = listOf(1, 3, 7, 15, 30)
                            val daysText = stringResource(R.string.days)
                            fun freqLabel(v: Int) = "$v $daysText"
                            SettingRow(label = stringResource(R.string.auto_backup_every)) {
                                Box {
                                    OutlinedButton(
                                        onClick = { freqExpanded = true },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            freqLabel(backupFrequency),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = freqExpanded,
                                        onDismissRequest = { freqExpanded = false }) {
                                        freqOptions.forEach { day ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        freqLabel(day),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.updateIntSetting(
                                                        SettingPrefKeys.BACKUP_FREQUENCY_DAYS,
                                                        day
                                                    )
                                                    freqExpanded = false
                                                    if (accountEmail != null && lastBackupTime > 0L) {
                                                        BackupScheduler.schedule(
                                                            context,
                                                            day.toLong(),
                                                            accountEmail!!,
                                                            includeMedia
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // DB retention
                            var retExpanded by remember { mutableStateOf(false) }
                            val retOptions = listOf(7, 14, 30, -1)
                            fun retLabel(v: Int) = if (v == -1) "Forever" else "$v backups"

                            SettingRow(label = "Keep DB backups") {
                                Box {
                                    OutlinedButton(
                                        onClick = { retExpanded = true },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            retLabel(retentionCount),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = retExpanded,
                                        onDismissRequest = { retExpanded = false }) {
                                        retOptions.forEach { v ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        retLabel(v),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.updateIntSetting(
                                                        SettingPrefKeys.BACKUP_RETENTION_COUNT,
                                                        v
                                                    )
                                                    retExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Media retention
                            var mediaRetExpanded by remember { mutableStateOf(false) }
                            val mediaRetOptions = listOf(3, 6, 12, -1)
                            fun mediaRetLabel(v: Int) = if (v == -1) "Forever" else "$v months"

                            SettingRow(label = "Keep media for") {
                                Box {
                                    OutlinedButton(
                                        onClick = { mediaRetExpanded = true },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text(
                                            mediaRetLabel(mediaRetentionMonths),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = mediaRetExpanded,
                                        onDismissRequest = { mediaRetExpanded = false }) {
                                        mediaRetOptions.forEach { v ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        mediaRetLabel(v),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.updateIntSetting(
                                                        SettingPrefKeys.MEDIA_RETENTION_MONTHS,
                                                        v
                                                    )
                                                    mediaRetExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(8.dp))
                    // ── STATUS ────────────────────────────────────────────────────────
                    ElevatedCard(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            val fmt = SimpleDateFormat(
                                "dd MMM yyyy, HH:mm",
                                LocalLocale.current.platformLocale
                            )

                            if (lastBackupTime > 0) {
                                StatusRow(
                                    label = stringResource(R.string.last_backup),
                                    value = fmt.format(Date(lastBackupTime))
                                )
                                val nextTime =
                                    lastBackupTime + backupFrequency * 24L * 60 * 60 * 1000
                                StatusRow(
                                    label = stringResource(R.string.next_backup),
                                    value = fmt.format(Date(nextTime))
                                )
                            } else {
                                Text(
                                    stringResource(R.string.no_backups_yet),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (driveStorageUsed > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                StatusRow(
                                    label = "Drive used",
                                    value = "%.1f MB".format(driveStorageUsed / 1024f / 1024f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // ── ACTION BUTTONS ────────────────────────────────────────────────
                    if (lastBackupTime == 0L) {
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.startBackup(
                                    context,
                                    accountEmail!!,
                                    includeMedia,
                                    retentionCount,
                                    mediaRetentionMonths,
                                    onSuccess = {
                                        isLoading = false
                                        viewModel.updateBooleanSetting(
                                            SettingPrefKeys.ENABLE_BACKUP,
                                            true
                                        )
                                        viewModel.updateLongSetting(
                                            SettingPrefKeys.LAST_BACKUP_TIME,
                                            System.currentTimeMillis()
                                        )
                                        scope.launch {
                                            backupList =
                                                DriveHelper.listBackups(context, accountEmail!!)
                                            driveStorageUsed = DriveHelper.getDriveStorageUsed(
                                                context,
                                                accountEmail!!
                                            )
                                        }
                                        BackupScheduler.schedule(
                                            context,
                                            backupFrequency.toLong(),
                                            accountEmail!!,
                                            includeMedia
                                        )
                                        Toast.makeText(context, backupStarted, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    onFailure = {
                                        isLoading = false
                                        Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isLoading) "Starting..." else "Start Backup")
                        }
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                viewModel.startBackup(
                                    context,
                                    accountEmail!!,
                                    includeMedia,
                                    retentionCount,
                                    mediaRetentionMonths,
                                    onSuccess = {
                                        isLoading = false
                                        viewModel.updateLongSetting(
                                            SettingPrefKeys.LAST_BACKUP_TIME,
                                            System.currentTimeMillis()
                                        )
                                        scope.launch {
                                            backupList =
                                                DriveHelper.listBackups(context, accountEmail!!)
                                            driveStorageUsed = DriveHelper.getDriveStorageUsed(
                                                context,
                                                accountEmail!!
                                            )
                                        }
                                        BackupScheduler.schedule(
                                            context,
                                            backupFrequency.toLong(),
                                            accountEmail!!,
                                            includeMedia
                                        )
                                        Toast.makeText(context, backupStarted, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    onFailure = {
                                        isLoading = false
                                        Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (isLoading) "Backing up..." else stringResource(R.string.backup_now))
                        }
                    }
// Restore button
                    Button(
                        onClick = {
                            scope.launch {
                                backupList = DriveHelper.listBackups(context, accountEmail!!)
                                showRestoreDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(stringResource(R.string.restore_from_backup_file))
                    }

// Stop Backup button — only show if backup is active
                    if (enableBackup && lastBackupTime > 0L) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.updateBooleanSetting(SettingPrefKeys.ENABLE_BACKUP, false)
                                viewModel.updateLongSetting(SettingPrefKeys.LAST_BACKUP_TIME, 0L)
                                BackupScheduler.cancel(context)
                                backupList = emptyList()
                                driveStorageUsed = 0L
                                Toast.makeText(context, "Backup stopped", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "Stop Backup",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                }
            }
        }

        // ── RESTORE DIALOG ────────────────────────────────────────────────────────
    // Replace restore dialog with this:
        var restoreMediaChoice by remember { mutableStateOf(false) }
        var selectedBackupItem by remember { mutableStateOf<DriveBackupItem?>(null) }
        var showRestoreConfirm by remember { mutableStateOf(false) }

    // List dialog — pick backup
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Backup", style = MaterialTheme.typography.titleSmall) },
                text = {
                    if (backupList.isEmpty()) {
                        Text("No backups found.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn {
                            items(backupList) { item ->
                                val date = SimpleDateFormat("dd MMM yyyy, HH:mm", LocalLocale.current.platformLocale)
                                    .format(Date(item.createdTime))
                                val sizeMb = "%.1f MB".format(item.size / 1024f / 1024f)
                                TextButton(
                                    onClick = {
                                        selectedBackupItem = item
                                        showRestoreDialog = false
                                        showRestoreConfirm = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        date,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        }

    // Restart dialog
        var showRestartDialog by remember { mutableStateOf(false) }
    // Confirm dialog — warning + media choice
        if (showRestoreConfirm && selectedBackupItem != null) {
            val item = selectedBackupItem!!
            val date = SimpleDateFormat("dd MMM yyyy, HH:mm", LocalLocale.current.platformLocale)
                .format(Date(item.createdTime))

            AlertDialog(
                onDismissRequest = { showRestoreConfirm = false },
                title = { Text("Restore?", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Warning
                        ElevatedCard(
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "⚠️ Current messages will be replaced with backup from $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        // Media choice
                        if (includeMedia) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Restore media too?",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Switch(
                                    checked = restoreMediaChoice,
                                    onCheckedChange = { restoreMediaChoice = it }
                                )
                            }
                        }
                    }
                },
// Replace confirm button onClick:
                confirmButton = {
                    Button(
                        onClick = {
                            showRestoreConfirm = false
                            isRestoring = true
                            restoreProgress = 0
                            restoreMessage = "Preparing..."



                            scope.launch(Dispatchers.IO) {

                                viewModel.prepareForRestore() // ← add this to ViewModel
                                val success = RestoreManager.startRestore(
                                    context,
                                    accountEmail!!,
                                    item.id,
                                    restoreMediaChoice,
                                    onProgress = { progress, message ->
                                        scope.launch(Dispatchers.Main) {
                                            restoreProgress = progress
                                            restoreMessage = message
                                        }
                                    }
                                )
                                withContext(Dispatchers.Main) {
                                    isRestoring = false
                                    if (success) {

                                        viewModel.updateBooleanSetting(
                                            SettingPrefKeys.PENDING_RESTORE_CLEANUP,
                                            true
                                        )

                                        showRestartDialog = true

                                    } else {

                                        Toast.makeText(
                                            context,
                                            "Restore failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirm = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Restore Complete ✅", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Text(
                        "App needs to restart to load restored data.",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showRestartDialog = false
                        RestoreManager.restartApp(context)
                    }) {
                        Text("Restart Now")
                    }
                },
                dismissButton = {}
            )
        }
        // Restoring in-progress dialog — non-dismissable
        if (isRestoring) {
            AlertDialog(
                onDismissRequest = { /* cannot dismiss */ },
                title = {
                    Text("Restoring Backup", style = MaterialTheme.typography.titleSmall)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { restoreProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Percentage
                        Text(
                            "$restoreProgress%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Current step message
                        Text(
                            restoreMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Warning
                        ElevatedCard(
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "⚠️ Do not close the app. Restart required after restore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
    }

// ── HELPER COMPOSABLES ────────────────────────────────────────────────────────

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        control()
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosContactScreen(
    viewModel: Knotx,
) {
    val upbhogtas by viewModel
        .getSelectableSosContacts()
        .collectAsState(initial = emptyList())

    val sosContacts by viewModel.sosContactsFlow.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val sosNumbers = remember(sosContacts) {
        sosContacts.map { it.id }.toSet()
    }

    val filteredContacts = remember(upbhogtas, searchQuery) {
        if (searchQuery.isBlank()) {
            upbhogtas
        } else {
            val query = searchQuery.trim().lowercase()

            upbhogtas.filter {
                (it.contactName?.contains(query, ignoreCase = true) == true) ||
                        (it.onlineName?.contains(query, ignoreCase = true) == true) ||
                        it.userNumber.contains(query)
            }
        }
    }

    val sortedContacts = remember(filteredContacts, sosNumbers) {
        filteredContacts.sortedWith(
            compareByDescending<Upbhogta> { it.userNumber in sosNumbers }
                .thenBy {
                    (it.contactName ?: it.onlineName ?: it.userNumber).lowercase()
                }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider()


            if (sortedContacts.isEmpty()) {

                Text(
                    text = stringResource(R.string.no_sos_contacts),
                    modifier = Modifier.padding(24.dp)
                )

            } else {

                sortedContacts.forEach { contact ->

                    val number = contact.userNumber
                    val isChecked = number in sosNumbers
                    val displayName = contact.contactName
                        ?.takeIf { it.isNotBlank() }
                        ?: contact.onlineName
                            ?.takeIf { it.isNotBlank() }
                        ?: contact.userNumber

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    if (isChecked) {
                                        viewModel.removeSosContactNumber(number)
                                    } else {
                                        viewModel.addSosContactNumber(number)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

//                        if (displayName != number) {
                            Text(
                                text = number,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
//                        }
                        }

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    if (checked) {
                                        viewModel.addSosContactNumber(number)
                                    } else {
                                        viewModel.removeSosContactNumber(number)
                                    }
                                }
                            }
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockListScreen(
    viewModel: Knotx,
) {

    val blockedUsers by viewModel
        .observeSetting(
            SettingPrefKeys.SERVER_BLOCKED_USERS,
            emptySet()
        )
        .collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {


            if (blockedUsers.isEmpty()) {

                Text(
                    text = stringResource(R.string.no_blocked_contacts),
                    modifier = Modifier.padding(24.dp)
                )

            } else {

                // 🔥 DO NOT use weight
                // 🔥 DO NOT use fillMaxSize
                // 🔥 Let parent scroll handle scrolling

                blockedUsers.forEach { number ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = number,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        IconButton(
                            onClick = {
                                viewModel.blockOrUnblockUser(number, false)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Unblock",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}




//endregion
@Composable
fun SettingItemWithSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0a7171),
                uncheckedThumbColor = Color.Gray
            )
        )
    }
}

//region HELP & SUPPORT


@Composable
fun HelpSupportScreen() {
    val context = LocalContext.current

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    fun openEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$email".toUri()
        }
        context.startActivity(intent)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
//            Text(
//                stringResource(R.string.help_support),
//                style = MaterialTheme.typography.headlineSmall,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            HorizontalDivider()


            SettingItemWithNavigation(stringResource(R.string.help_center)) {
                openUrl("https://www.knotx.com/support")
            }

            //todo update this later not aws
            SettingItemWithNavigation(stringResource(R.string.contact_support)) {
                openEmail("aws@senghol.com")
            }


            SettingItemWithNavigation(stringResource(R.string.terms_conditions)) {
                openUrl("https://www.knotx.com/terms")
            }

            SettingItemWithNavigation(stringResource(R.string.privacy_policy)) {
                openUrl("https://www.knotx.com/privacy")
            }

            SettingItemWithNavigation(stringResource(R.string.report_issue)) {
                openUrl("https://www.knotx.com/report")
            }
        }
    }
}
//endregion

//region Language & Region

@Composable
fun LanguageScreen(knotx: Knotx) {

    val context = LocalContext.current

    var useSystem by remember {
        mutableStateOf(PrefsHelper.getUseSystemLanguage(context))
    }

    var selectedLanguage by remember {
        mutableStateOf(
            PrefsHelper.getAppLanguage(context)
                ?: Locale.getDefault().language
        )
    }

    fun applyLanguage(languageCode: String) {
        PrefsHelper.setAppLanguage(context, languageCode)
        LocaleHelper.setLocale(context, languageCode)
        knotx.restartApp()
//        activity?.recreate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
//
//            Spacer(modifier = Modifier.height(8.dp))
//            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))


            SettingItemWithSwitch(
                label = stringResource(R.string.use_system_language),
                checked = useSystem
            ) { enabled ->

                if (useSystem == enabled) return@SettingItemWithSwitch

                useSystem = enabled

                PrefsHelper.setUseSystemLanguage(context, enabled)

                knotx.restartApp()
            }


            Spacer(modifier = Modifier.height(16.dp))

            // 🌍 Manual Language List
            if (!useSystem) {

                val languages = listOf(
                    // English
                    Triple("en", "English", "अंग्रेज़ी"),

                    // 22 Scheduled Indian Languages
                    Triple("as", "অসমীয়া", "असमिया"),
                    Triple("bn", "বাংলা", "बांग्ला"),
                    Triple("brx", "बड़ो", "बोडो"),
                    Triple("doi", "डोगरी", "डोगरी"),
                    Triple("gu", "ગુજરાતી", "गुजराती"),
                    Triple("hi", "हिन्दी", "हिन्दी"),
                    Triple("kn", "ಕನ್ನಡ", "कन्नड़"),
                    Triple("ks", "کٲشُر", "कश्मीरी"),
                    Triple("kok", "कोंकणी", "कोंकणी"),
                    Triple("mai", "मैथिली", "मैथिली"),
                    Triple("ml", "മലയാളം", "मलयालम"),
                    Triple("mni", "ꯃꯩꯇꯩꯂꯣꯟ", "मणिपुरी"),
                    Triple("mr", "मराठी", "मराठी"),
                    Triple("ne", "नेपाली", "नेपाली"),
                    Triple("or", "ଓଡ଼ିଆ", "ओड़िया"),
                    Triple("pa", "ਪੰਜਾਬੀ", "पंजाबी"),
                    Triple("sa", "संस्कृतम्", "संस्कृत"),
                    Triple("sat", "ᱥᱟᱱᱛᱟᱲᱤ", "संथाली"),
                    Triple("sd", "سنڌي", "सिंधी"),
                    Triple("ta", "தமிழ்", "तमिल"),
                    Triple("te", "తెలుగు", "तेलुगु"),
                    Triple("ur", "اردو", "उर्दू")
                )

                languages.forEach { (code, nativeName, hindiName) ->

                    Column {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {

                                    if (selectedLanguage == code) return@clickable

                                    selectedLanguage = code
                                    applyLanguage(code)
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected = selectedLanguage == code,
                                onClick = null
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = nativeName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = hindiName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}


//endregion


@Composable
fun SettingItemWithNavigation(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() }
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Navigate")
    }
    HorizontalDivider()
}



