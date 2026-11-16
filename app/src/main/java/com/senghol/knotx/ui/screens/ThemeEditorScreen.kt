package com.senghol.knotx.ui.screens
// Compose core
import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// Layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState

// Shapes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayCircle

// Material
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset

// UI
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Dialog
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.google.gson.JsonObject
import com.senghol.knotx.connection.Knotx
import com.senghol.knotx.ui.components.MessageBubble
import com.senghol.knotx.ui.theme.DootawasColors

import com.senghol.knotx.ui.theme.toCompose
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.R
import com.senghol.knotx.ui.components.BorderMode
import com.senghol.knotx.ui.components.BottomActionBar
import com.senghol.knotx.ui.components.BubbleType
import com.senghol.knotx.ui.components.ModernMessageBubble
import com.senghol.knotx.ui.components.UpbhogtaItem
import com.senghol.knotx.ui.components.UserHeaderPortrait
import com.senghol.knotx.ui.components.home.StoriesLockedMessage
import com.senghol.knotx.ui.theme.DarkDootawasColors
import com.senghol.knotx.ui.theme.LightDootawasColors
import com.senghol.knotx.ui.theme.defaultGroupBundle
import com.senghol.knotx.ui.theme.toChatColors
import com.senghol.knotx.ui.theme.toGroupColors
import com.senghol.knotx.utils.data.UpbhogtaUiItem
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Color Picker (Skydoves)



@Composable
fun UserThemeEditorScreen(
    initialTheme: DootawasColors,
    initialBorderMode: BorderMode,
    isDark: Boolean,
    useModernStyle: Boolean,
    onApply: (
        DootawasColors,
        BorderMode,
        Boolean
    ) -> Unit,
    onClose: () -> Unit
) {
    var showCoach by remember { mutableStateOf(false) }
    var coachAnchor by remember { mutableStateOf(Offset.Zero) }
    var borderMode by remember(initialBorderMode) {
        mutableStateOf(initialBorderMode)
    }
    var selectedField by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var theme by remember(initialTheme) { mutableStateOf(initialTheme) }

    // 👇 FIX #1: seed local state from the incoming parameter, don't shadow with a fresh false
    var localUseModernStyle by remember(useModernStyle) { mutableStateOf(useModernStyle) }


    var flashColor by remember { mutableStateOf<Color?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(flashTick) {
        if (flashTick == 0) return@LaunchedEffect
        try {
            flashColor = Color.Red
            delay(200.milliseconds)
            flashColor = null          // original
            delay(150.milliseconds)
            flashColor = Color.Red
            delay(200.milliseconds)
        } finally {
            flashColor = null          // always end on original
        }
    }
    val previewTheme = flashColor
        ?.let { c -> selectedField?.let { theme.withField(it, c) } }
        ?: theme
    BackHandler {
        onClose()
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            showCoach = false
                        }
                    }
                }
            }
    ) {
        Column {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Back", modifier = Modifier.clickable { onClose() })
                Text(if (isDark) "Dark Chat Theme" else "Light Chat Theme")
                Text("Apply", modifier = Modifier.clickable {
                    onApply(theme, borderMode, localUseModernStyle)
                })
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    // FIX #3: use `surface`, not `onSurface`, as a background color
//                    .background(colorScheme.onSurface)
                    .verticalScroll(rememberScrollState())
            ) {
                key(previewTheme, localUseModernStyle) {
                    ChatPreview(
                        theme = previewTheme,
                        isDark = isDark,
                        borderMode = borderMode,
                        useModernStyle = localUseModernStyle
                    )
                }

                Spacer(Modifier.height(16.dp))

                ColorGrid16(
                    theme = theme,
                    onColorClick = { field, color, pos ->
                        selectedField = field
                        selectedColor = color
                        coachAnchor = pos
                        showCoach = true
                        flashTick++
                    }
                )

                Spacer(Modifier.height(24.dp))

                BubbleStyleSelectorRow(
                    useModernStyle = localUseModernStyle,
                    isDark = !isDark, // 👈 fixed — was `!isDark`, which flipped contrast for the wrong mode
                    onStyleChange = { selectedStyle ->
                        localUseModernStyle = selectedStyle

                        // 👇 load that style's default bubble colors, same as ThemeEditorScreen
                        val defaults = if (selectedStyle) {
                            DootawasStyleDefaults.modern(isDark)
                        } else {
                            DootawasStyleDefaults.classic(isDark)
                        }

                        theme = theme.applyBubbleDefaults(defaults)
                    }
                )

                Spacer(Modifier.height(16.dp))

                if (!localUseModernStyle) {
                    BorderToggle(borderMode) {
                        borderMode = it
                    }
                }
            }
        }
    }

    if (
        showCoach &&
        selectedField != null &&
        selectedColor != null
    ) {
        CoachBubble(
            anchor = coachAnchor,
            colorName = selectedField ?: "Color",
            color = selectedColor ?: Color.White,
            onDismiss = { showCoach = false },
            onEdit = {
                showCoach = false
                showPicker = true
            }
        )
    }

    // PICKER
    if (showPicker) {

        AdvancedColorPickerScreen(
            initialColor =
                selectedColor ?: Color.White,

            onDismiss = {
                showPicker = false
            },

            onColorSelected = { newColor ->

                selectedField?.let { field ->
                    theme = theme.withField(field, newColor)
                }
                showPicker = false
            }
        )
    }
}
@Composable
fun AppIconSelector(
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    val icons = listOf(
        Triple("default", "Default", R.drawable.ic_launcher),
        Triple("green", "Green", R.drawable.ic_launcher_green),
        Triple("cyan", "Cyan", R.drawable.ic_launcher_cyan),
        Triple("blue", "Blue", R.drawable.ic_launcher_blue),
        Triple("slate", "Slate", R.drawable.ic_launcher_slate),
        Triple("orange", "Orange", R.drawable.ic_launcher_orange)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "App Icon",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            "Choose the launcher icon displayed on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        icons.chunked(3).forEach { rowIcons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowIcons.forEach { (id, name, iconRes) ->
                    val isSelected = selectedIcon == id

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onIconSelected(id) }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box {
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = "$name app icon",
                                modifier = Modifier.size(64.dp)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Fill remaining space in the last row
                repeat(3 - rowIcons.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ThemeEditorScreen(
    viewModel: Knotx,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val bundle = viewModel.themeBundle.collectAsState().value ?: return
    val groupBundle = viewModel.groupThemeBundle.collectAsState().value ?: defaultGroupBundle()

    var showCoach by remember { mutableStateOf(false) }
    var coachAnchor by remember { mutableStateOf(Offset.Zero) }

    var editorMode by remember { mutableStateOf("home") }   // "chat" | "group" | "home"
    val isGroup = editorMode == "group"

    // CHAT state
    var chatBorderMode by remember(bundle) { mutableStateOf(BorderMode.entries[bundle.borderMode]) }
    var chatModern by remember(bundle) { mutableStateOf(bundle.useModernStyle) }

    // GROUP state
    var groupBorderMode by remember(groupBundle) {
        mutableStateOf(BorderMode.entries.getOrElse(groupBundle.borderMode) { BorderMode.SEMI })
    }
    val groupModern = false   // group is classic only

    // Active values for the current tab
    val borderMode = if (isGroup) groupBorderMode else chatBorderMode
    val useModernStyle = if (isGroup) groupModern else chatModern

    var selectedField by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val chatTheme = if (isDark) bundle.dark.toCompose() else bundle.light.toCompose()
    val groupColors = if (isDark) groupBundle.dark.toCompose() else groupBundle.light.toCompose()

    // Preview/grid always receive a DootawasColors
    val theme = if (isGroup) groupColors.toChatColors(chatTheme) else chatTheme

    var flashColor by remember { mutableStateOf<Color?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(flashTick) {
        if (flashTick == 0) return@LaunchedEffect
        try {
            flashColor = Color.Red
            delay(200.milliseconds)
            flashColor = null          // original
            delay(150.milliseconds)
            flashColor = Color.Red
            delay(200.milliseconds)
        } finally {
            flashColor = null          // always end on original
        }
    }
    val previewTheme = flashColor
        ?.let { c -> selectedField?.let { theme.withField(it, c) } }
        ?: theme

    BackHandler { onClose() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            showCoach = false
                        }
                    }
                }
            }
    ) {
        Column {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Back", modifier = Modifier.clickable { onClose() })
                Text(
                    (if (isGroup) "Group " else "") + (if (isDark) "Dark Theme" else "Light Theme")
                )
                Text("Reset", modifier = Modifier.clickable { showResetDialog = true })
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ToggleChip(text = "Chat", selected = editorMode == "chat") { editorMode = "chat" }
                Spacer(Modifier.width(8.dp))
                ToggleChip(text = "Group", selected = editorMode == "group") { editorMode = "group" }
                Spacer(Modifier.width(8.dp))
                ToggleChip(text = "Home", selected = editorMode == "home") { editorMode = "home" }
                Spacer(Modifier.width(8.dp))
                ToggleChip(text = "Icon", selected = editorMode == "icon") { editorMode = "icon" }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
//                    .background(colorScheme.onSurface)
                    .verticalScroll(rememberScrollState())
            ) {
                    when (editorMode) {
                        "icon" -> {
                            val selectedIcon by viewModel.selectedAppIcon.collectAsState()
                            val context = LocalContext.current

                            AppIconSelector(
                                selectedIcon = selectedIcon,
                                onIconSelected = { icon ->
                                    viewModel.saveAppIcon(context, icon)
                                }
                            )
                        }

                        "chat", "group" -> {
                            key(previewTheme, useModernStyle, editorMode) {
                                ChatPreview(
                                    theme = previewTheme,
                                    isDark = isDark,
                                    borderMode = borderMode,
                                    useModernStyle = useModernStyle
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            ColorGrid16(
                                theme = theme,
                                onColorClick = { field, color, pos ->
                                    selectedField = field
                                    selectedColor = color
                                    coachAnchor = pos
                                    showCoach = true
                                    flashTick++
                                }
                            )

                            if (!isGroup) {
                                Spacer(Modifier.height(16.dp))

                                BubbleStyleSelectorRow(
                                    useModernStyle = useModernStyle,
                                    isDark = !isDark,
                                    onStyleChange = { selectedStyle ->
                                        val defaults = if (selectedStyle) {
                                            DootawasStyleDefaults.modern(isDark)
                                        } else {
                                            DootawasStyleDefaults.classic(isDark)
                                        }
                                        chatModern = selectedStyle
                                        viewModel.updateSingleColor(
                                            isDark = isDark,
                                            borderMode = chatBorderMode,
                                            useModernStyle = selectedStyle
                                        ) { current -> current.applyBubbleDefaults(defaults) }
                                    }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            BorderToggle(borderMode) { mode ->
                                if (isGroup) {
                                    groupBorderMode = mode
                                    viewModel.updateGroupColor(
                                        isDark = isDark,
                                        borderMode = mode,
                                        useModernStyle = groupModern
                                    ) { it }
                                } else {
                                    chatBorderMode = mode
                                    viewModel.updateSingleColor(
                                        isDark = isDark,
                                        borderMode = mode,
                                        useModernStyle = chatModern
                                    ) { current -> current }
                                }
                            }

                        }

                        else -> {
                            Spacer(Modifier.height(24.dp))
                            key(previewTheme) {
                                HomePreview(previewTheme, isDark)
                            }
                            Spacer(Modifier.height(16.dp))
                            ColorRow(
                                items = listOf(
                                    "homeBg" to theme.homeBackground,
                                    "homeChatBg" to theme.homeChatBackground,
                                    "homeText" to theme.homeText,
                                    "homeHeaderBg" to theme.homeHeaderBackground,
                                    "homeHeaderSelectedText" to theme.homeHeaderSelectedText,
                                    "homeHeaderUnselectedText" to theme.homeHeaderUnSelectedText,
                                    "homeHeaderIcon" to theme.homeHeaderIcon,
                                    "lastMsgColor" to theme.lastMessageColor
                                ),
                                onColorClick = { field, color, pos ->
                                    selectedField = field
                                    selectedColor = color
                                    coachAnchor = pos
                                    showCoach = true
                                    flashTick++
                                }
                            )

                            Spacer(Modifier.height(16.dp))

                            ColorRow(
                                items = listOf(
                                    "homeHeaderSelectedBackground" to theme.homeHeaderSelectedBackground,
                                    "searchBg" to theme.searchBarBackground,
                                    "searchText" to theme.searchBarText,
                                    "searchFocused" to theme.searchBarFocusedIndicator,
                                    "searchUnfocused" to theme.searchBarUnfocusedIndicator,
                                    "buttonBg" to theme.buttonBackground,
                                    "buttonText" to theme.buttonText,
                                    "unread" to theme.unreadBadgeColor,
                                ),
                                onColorClick = { field, color, pos ->
                                    selectedField = field
                                    selectedColor = color
                                    coachAnchor = pos
                                    showCoach = true
                                    flashTick++
                                }
                            )
                        }
                    }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showCoach && selectedField != null && selectedColor != null) {
        CoachBubble(
            anchor = coachAnchor,
            colorName = selectedField ?: "Color",
            color = selectedColor ?: Color.White,
            onDismiss = { showCoach = false },
            onEdit = {
                showCoach = false
                showPicker = true
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (isGroup) "Reset Group Theme?" else "Reset Theme?") },
            text = {
                Text(
                    if (isGroup) "This will restore the default group colors."
                    else "This will remove all custom colors and restore default theme."
                )
            },
            confirmButton = {
                Text("Reset", modifier = Modifier.clickable {
                    if (isGroup) {
                        viewModel.resetGroupTheme()
                        groupBorderMode = BorderMode.SEMI
                    } else {
                        viewModel.resetTheme()
                        chatModern = true
                        chatBorderMode = BorderMode.entries[0]
                    }
                    showResetDialog = false
                })
            },
            dismissButton = {
                Text("Cancel", modifier = Modifier.clickable { showResetDialog = false })
            }
        )
    }

    if (showPicker) {
        AdvancedColorPickerScreen(
            initialColor = selectedColor ?: Color.White,
            onDismiss = { showPicker = false },
            onColorSelected = { newColor ->
                selectedField?.let { field ->
                    if (isGroup) {
                        viewModel.updateGroupColor(
                            isDark = isDark,
                            borderMode = groupBorderMode,
                            useModernStyle = groupModern
                        ) { g ->
                            g.toChatColors(chatTheme).withField(field, newColor).toGroupColors()
                        }
                    } else {
                        viewModel.updateSingleColor(
                            isDark = isDark,
                            borderMode = chatBorderMode,
                            useModernStyle = chatModern
                        ) { t ->
                            t.withField(field, newColor)
                        }
                    }
                }
                showPicker = false
            }
        )
    }
}

@Composable
fun OnboardingThemeEditorScreen(
    viewModel: Knotx,
    isDark: Boolean,
    onComplete: () -> Unit
) {
    val bundle = viewModel.themeBundle.collectAsState().value

    if (bundle == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 🌟 Seed Modern's default bubble colors exactly once, since useModernStyle
    // is already true here but the bundle's colors are just the neutral base —
    // never actually stamped with the Modern palette.
    var hasSeededDefaults by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (bundle.useModernStyle) {
            viewModel.updateSingleColor(
                isDark = isDark,
                borderMode = BorderMode.entries[bundle.borderMode],
                useModernStyle = true
            ) { current -> current.applyBubbleDefaults(DootawasStyleDefaults.modern(isDark)) }
        }
        hasSeededDefaults = true
    }

    // Wait for seeding to land before reading anything else off the bundle —
    // otherwise our "initial" snapshot below would capture pre-seed colors,
    // and Continue/Skip would flicker to Continue the instant seeding applies.
    if (!hasSeededDefaults) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val seededBundle = viewModel.themeBundle.collectAsState().value ?: bundle

    // 🌟 Captured ONCE, after seeding — this is the correct baseline for hasChanges.
    val initialBundleSnapshot = remember { seededBundle }

    var showCoach by remember { mutableStateOf(false) }
    var coachAnchor by remember { mutableStateOf(Offset.Zero) }
    var borderMode by remember(seededBundle) {
        mutableStateOf(BorderMode.entries[seededBundle.borderMode])
    }
    var useModernStyle by remember(seededBundle) {
        mutableStateOf(seededBundle.useModernStyle)
    }

    var selectedField by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var editorMode by remember { mutableStateOf("home") }

    val theme = if (isDark) bundle.dark.toCompose() else bundle.light.toCompose()

    // Derived once from the frozen snapshot — these never move again.
    val initialTheme = remember {
        if (isDark) initialBundleSnapshot.dark.toCompose() else initialBundleSnapshot.light.toCompose()
    }
    val initialBorderMode = remember { BorderMode.entries[initialBundleSnapshot.borderMode] }
    val initialUseModernStyle = remember { initialBundleSnapshot.useModernStyle }

    val hasChanges = theme != initialTheme ||
            borderMode != initialBorderMode ||
            useModernStyle != initialUseModernStyle

    // Onboarding: back is fully disabled — swallow it, no navigation.
    BackHandler(enabled = true) { /* intentionally no-op during onboarding */ }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                            showCoach = false
                        }
                    }
                }
            }
    ) {
        Column {
            // TOP BAR — no Back button; Reset only shows once something's changed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(1.dp)) // keeps the title visually centered

                Text(if (isDark) "Dark Theme" else "Light Theme")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasChanges) {
                        Text(
                            "Reset",
                            modifier = Modifier
                                .clickable {
                                    viewModel.resetTheme()
                                    borderMode = initialBorderMode
                                    useModernStyle = initialUseModernStyle
                                }
                                .padding(end = 16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ToggleChip(text = "Chat", selected = editorMode == "chat") { editorMode = "chat" }
                Spacer(Modifier.width(8.dp))
                ToggleChip(text = "Home", selected = editorMode == "home") { editorMode = "home" }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(colorScheme.onSurface)
                    .verticalScroll(rememberScrollState())
            ) {
                if (editorMode == "chat") {
                    key(theme, useModernStyle) {
                        ChatPreview(
                            theme = theme,
                            isDark = !isDark,
                            borderMode = borderMode,
                            useModernStyle = useModernStyle
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    ColorGrid16(
                        theme = theme,
                        onColorClick = { field, color, pos ->
                            selectedField = field
                            selectedColor = color
                            coachAnchor = pos
                            showCoach = true
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    BubbleStyleSelectorRow(
                        useModernStyle = useModernStyle,
                        isDark = !isDark,
                        onStyleChange = { selectedStyle ->
                            useModernStyle = selectedStyle

                            val defaults = if (selectedStyle) {
                                DootawasStyleDefaults.modern(isDark)
                            } else {
                                DootawasStyleDefaults.classic(isDark)
                            }

                            viewModel.updateSingleColor(
                                isDark = isDark,
                                borderMode = borderMode,
                                useModernStyle = selectedStyle
                            ) { current -> current.applyBubbleDefaults(defaults) }
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    if (!useModernStyle) {
                        BorderToggle(borderMode) { mode ->
                            borderMode = mode
                            viewModel.updateSingleColor(
                                isDark = isDark,
                                borderMode = mode,
                                useModernStyle = useModernStyle
                            ) { current -> current }
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    HomePreview(theme, isDark)
                    Spacer(Modifier.height(16.dp))
                    ColorRow(
                        items = listOf(
                            "homeBg" to theme.homeBackground,
                            "homeChatBg" to theme.homeChatBackground,
                            "homeText" to theme.homeText,
                            "homeHeaderBg" to theme.homeHeaderBackground,
                            "homeHeaderSelectedText" to theme.homeHeaderSelectedText,
                            "homeHeaderUnselectedText" to theme.homeHeaderUnSelectedText,
                            "homeHeaderIcon" to theme.homeHeaderIcon,
                            "lastMsgColor" to theme.lastMessageColor
                        ),
                        onColorClick = { field, color, pos ->
                            selectedField = field
                            selectedColor = color
                            coachAnchor = pos
                            showCoach = true
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ColorRow(
                        items = listOf(
                            "searchBg" to theme.searchBarBackground,
                            "searchText" to theme.searchBarText,
                            "searchFocused" to theme.searchBarFocusedIndicator,
                            "searchUnfocused" to theme.searchBarUnfocusedIndicator,
                            "buttonBg" to theme.buttonBackground,
                            "buttonText" to theme.buttonText,
                            "unread" to theme.unreadBadgeColor,
                        ),
                        onColorClick = { field, color, pos ->
                            selectedField = field
                            selectedColor = color
                            coachAnchor = pos
                            showCoach = true
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Persistent bottom action, in case the top-right link is missed on first run.
                Button(
                    onClick = { onComplete() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(if (hasChanges) "Continue" else "Skip")
                }
            }
        }
    }

    if (showCoach && selectedField != null && selectedColor != null) {
        CoachBubble(
            anchor = coachAnchor,
            colorName = selectedField ?: "Color",
            color = selectedColor ?: Color.White,
            onDismiss = { showCoach = false },
            onEdit = {
                showCoach = false
                showPicker = true
            }
        )
    }

    if (showPicker) {
        AdvancedColorPickerScreen(
            initialColor = selectedColor ?: Color.White,
            onDismiss = { showPicker = false },
            onColorSelected = { newColor ->
                selectedField?.let { field ->
                    viewModel.updateSingleColor(
                        isDark = isDark,
                        borderMode = borderMode,
                        useModernStyle = useModernStyle
                    ) { t -> t.applyFieldUpdate(field, newColor) }
                }
                showPicker = false
            }
        )
    }
}

fun DootawasColors.applyFieldUpdate(field: String, newColor: Color): DootawasColors = when (field) {
    "chatBackground" -> copy(chatScreenBackground = newColor)
    "chatWallpaper" -> copy(chatScreenWallpaper = newColor)
    "wallpaperOverlay" -> copy(wallpaperOverlay = newColor)
    "leftMsgBg" -> copy(leftMessageBackground = newColor)
    "rightMsgBg" -> copy(rightMessageBackground = newColor)
    "leftText" -> copy(leftMessageText = newColor)
    "rightText" -> copy(rightMessageText = newColor)
    "leftTimeBg" -> copy(leftTimeBackGround = newColor)
    "rightTimeBg" -> copy(rightTimeBackGround = newColor)
    "leftTimeText" -> copy(leftTimeText = newColor)
    "rightTimeText" -> copy(rightTimeText = newColor)
    "leftBorder" -> copy(leftMessageBorderColor = newColor)
    "rightBorder" -> copy(rightMessageBorderColor = newColor)
    "reply" -> copy(replyBackground = newColor)
    "glass" -> copy(glassColor = newColor)
    "searchBg" -> copy(searchBarBackground = newColor)
    "searchText" -> copy(searchBarText = newColor)
    "searchFocused" -> copy(searchBarFocusedIndicator = newColor)
    "searchUnfocused" -> copy(searchBarUnfocusedIndicator = newColor)
    "userHeaderBg" -> copy(userHeaderBackground = newColor)
    "userHeaderText" -> copy(userHeaderText = newColor)
    "userHeaderIcon" -> copy(userHeaderIcon = newColor)
    "homeBg" -> copy(homeBackground = newColor)
    "homeChatBg" -> copy(homeChatBackground = newColor)
    "homeText" -> copy(homeText = newColor)
    "homeHeaderBg" -> copy(homeHeaderBackground = newColor)
    "homeHeaderSelectedText" -> copy(homeHeaderSelectedText = newColor)
    "homeHeaderUnselectedText" -> copy(homeHeaderUnSelectedText = newColor)
    "homeHeaderSelectedBackground" -> copy(homeHeaderSelectedBackground =  newColor)
    "homeHeaderIcon" -> copy(homeHeaderIcon = newColor)
    "buttonBg" -> copy(buttonBackground = newColor)
    "buttonText" -> copy(buttonText = newColor)
    "header" -> copy(dialogHeader = newColor)
    "unread" -> copy(unreadBadgeColor = newColor)
    "lastMsgColor" -> copy(lastMessageColor = newColor)
    "replyText" -> copy(replyText = newColor)
    else -> this
}
@Composable
fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) colorScheme.primary
                else colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White
            else colorScheme.onSurface
        )
    }
}
@Composable
fun ChatPreview(
    theme: DootawasColors,
    isDark: Boolean,
    borderMode: BorderMode,
    useModernStyle: Boolean // 👈 Added flag to control layout variation
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(theme.chatScreenBackground)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        val backgroundPainter = painterResource(R.drawable.portrait)

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = backgroundPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = theme.chatScreenWallpaper,
                blendMode = BlendMode.SrcIn
            )
        )
        Column(
            modifier = Modifier.matchParentSize()
        ) {
            UserHeaderPortrait(
                photoUrl = null,
                validUserName = "User Name",
                userNumber = "9999999999",
                onShowUserSettings = {},
                onToggleSearch = {},
                onBackClick = {},
                dootawasColors = theme,
                onShowAiUsage = {}
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                val replyJson = JsonObject().apply {
                    addProperty(DootawasMessage.ORIGINAL_MESSAGE_TYPE, SandeshType.MESSAGE.value)
                    addProperty(DootawasMessage.ORIGINAL_MESSAGE_ID, "msg_123")
                    val inner = JsonObject().apply {
                        addProperty(DootawasMessage.PAATH, "This is original message")
                    }
                    addProperty(DootawasMessage.ORIGINAL_SAMAGRI, inner.toString())
                }

                if (useModernStyle) {
                    // 🌟 MODERN PATH PREVIEWS
                    ModernMessageBubble(
                        isSelf = false,
                        data = "Hello 👋",
                        formattedTime = "10:00",
                        formattedDate = "",
                        msgType = 0, // MSG_TEXT
                        bubbleType = BubbleType.SINGLE,
                        onLongPress = {},
                        onToggleSelection = {},
                        onReplyClick = {},
                        isHighlighted = false,
                        sandeshStithi = SandeshStithi.PREKSHIT,
                        fontSize = 14,
                        replyOriginalMessage = replyJson,
                        borderMode = borderMode,
                        dootawasColors = theme,
                        onReplySlide = {},
                        timeLong = 99
                    )
                    ModernMessageBubble(
                        isSelf = true,
                        data = "Hi bro 🚀",
                        formattedTime = "10:01",
                        formattedDate = "",
                        msgType = 0, // MSG_TEXT
                        bubbleType = BubbleType.SINGLE,
                        onLongPress = {},
                        onToggleSelection = {},
                        onReplyClick = {},
                        isHighlighted = false,
                        sandeshStithi = SandeshStithi.SAMARPIT,
                        fontSize = 14,
                        borderMode = borderMode,
                        dootawasColors = theme,
                        onReplySlide = {},
                        timeLong = 99
                    )
                } else {
                    // 🏛️ CLASSIC BUBBLE PREVIEWS
                    MessageBubble(
                        isSelf = false,
                        data = "Hello 👋",
                        formattedTime = "10:00",
                        formattedDate = "",
                        msgType = 0,
                        onLongPress = {},
                        onToggleSelection = {},
                        onReplyClick = {},
                        isHighlighted = false,
                        sandeshStithi = SandeshStithi.PREKSHIT,
                        fontSize = 14,
                        replyOriginalMessage = replyJson,
                        borderMode = borderMode,
                        dootawasColors = theme,
                        onReplySlide = {},
                    )
                    MessageBubble(
                        isSelf = true,
                        data = "Hi bro 🚀",
                        formattedTime = "10:01",
                        formattedDate = "",
                        msgType = 0,
                        onLongPress = {},
                        onToggleSelection = {},
                        onReplyClick = {},
                        isHighlighted = false,
                        sandeshStithi = SandeshStithi.SAMARPIT,
                        fontSize = 14,
                        borderMode = borderMode,
                        dootawasColors = theme,
                        onReplySlide = {},
                    )
                }
            }
        }
    }
}


// Only the fields that visually differ between Classic and Modern bubble styles.
// Everything else (home screen, search bar, dialogs, buttons) stays as-is regardless of style.
data class BubbleColorSet(
    val chatScreenBackground: Color,
    val chatScreenWallpaper: Color,
    val wallpaperOverlay: Color,
    val leftMessageBackground: Color,
    val rightMessageBackground: Color,
    val leftMessageText: Color,
    val rightMessageText: Color,
    val leftTimeBackGround: Color,
    val rightTimeBackGround: Color,
    val leftTimeText: Color,
    val rightTimeText: Color,
    val leftMessageBorderColor: Color,
    val rightMessageBorderColor: Color,
    val replyBackground: Color,
    val replyText: Color,
    val glassColor: Color,
    val userHeaderBackground: Color,
    val userHeaderText: Color
)

object DootawasStyleDefaults {

    private fun base(isDark: Boolean): DootawasColors =
        if (isDark) DarkDootawasColors else LightDootawasColors
    // MODERN — your current defaults, kept as their own explicit set
    // MODERN: everything comes from DootawasColors
    fun modern(isDark: Boolean): BubbleColorSet {
        val c = base(isDark)
        return BubbleColorSet(
            chatScreenBackground = c.chatScreenBackground,
            chatScreenWallpaper = c.chatScreenWallpaper,
            wallpaperOverlay = c.wallpaperOverlay,
            leftMessageBackground = c.leftMessageBackground,
            rightMessageBackground = c.rightMessageBackground,
            leftMessageText = c.leftMessageText,
            rightMessageText = c.rightMessageText,
            leftTimeBackGround = c.leftTimeBackGround,
            rightTimeBackGround = c.rightTimeBackGround,
            leftTimeText = c.leftTimeText,
            rightTimeText = c.rightTimeText,
            leftMessageBorderColor = c.leftMessageBorderColor,
            rightMessageBorderColor = c.rightMessageBorderColor,
            replyBackground = c.replyBackground,
            glassColor = c.glassColor,
            replyText = c.replyText,
            userHeaderBackground = c.userHeaderBackground,
            userHeaderText = c.userHeaderText
        )
    }

    // CLASSIC — tweaked from Modern: flatter/fully-opaque fills (no glass translucency,
    // since standard boxes don't float over wallpaper the same way), slightly deeper
    // border/background tones for a more solid "box" feel.
    fun classic(isDark: Boolean): BubbleColorSet {
        val c = base(isDark)
        return BubbleColorSet(
            chatScreenBackground = c.chatScreenBackground,
            chatScreenWallpaper = c.chatScreenWallpaper,
            wallpaperOverlay = c.wallpaperOverlay,
            leftMessageBackground = c.leftMessageBackground,
            rightMessageBackground = c.rightMessageBackground,
            leftMessageText = c.leftMessageText,
            rightMessageText = c.rightMessageText,
            leftTimeBackGround = c.leftTimeBackGround,
            rightTimeBackGround = c.rightTimeBackGround,
            leftTimeText = c.leftTimeText,
            rightTimeText = c.rightTimeText,
            leftMessageBorderColor = c.leftMessageBorderColor,
            rightMessageBorderColor = c.rightMessageBorderColor,
            replyBackground = c.replyBackground,
            glassColor = c.glassColor,
            replyText = c.replyText,
            userHeaderBackground = c.userHeaderBackground,
            userHeaderText = c.userHeaderText
        )
    }
}

fun DootawasColors.applyBubbleDefaults(set: BubbleColorSet): DootawasColors = copy(
    chatScreenBackground = set.chatScreenBackground,
    chatScreenWallpaper = set.chatScreenWallpaper,
    wallpaperOverlay = set.wallpaperOverlay,
    leftMessageBackground = set.leftMessageBackground,
    rightMessageBackground = set.rightMessageBackground,
    leftMessageText = set.leftMessageText,
    rightMessageText = set.rightMessageText,
    leftTimeBackGround = set.leftTimeBackGround,
    rightTimeBackGround = set.rightTimeBackGround,
    leftTimeText = set.leftTimeText,
    rightTimeText = set.rightTimeText,
    leftMessageBorderColor = set.leftMessageBorderColor,
    rightMessageBorderColor = set.rightMessageBorderColor,
    replyBackground = set.replyBackground,
    replyText = set.replyText,
    glassColor = set.glassColor,
    userHeaderBackground = set.userHeaderBackground,
    userHeaderText = set.userHeaderText
)
@Composable
fun BubbleStyleSelectorRow(
    useModernStyle: Boolean,
    isDark: Boolean,
    onStyleChange: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 👇 Derived directly from isDark instead of ambient colorScheme,
    // so this always matches the theme being edited, not the system/app shell theme.
    val contentColor = if (!isDark) Color.White else Color.Black
    val containerColor = if (!isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Message Bubble Design",
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow")

            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                StyleSelectionItem(
                    title = "Classic Layout (Standard Boxes)",
                    isSelected = !useModernStyle,
                    isDark = isDark,
                    onClick = { onStyleChange(false) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                StyleSelectionItem(
                    title = "Modern Vector Layout (Dynamic Tails)",
                    isSelected = useModernStyle,
                    isDark = isDark,
                    onClick = { onStyleChange(true) }
                )
            }
        }
    }
}

@Composable
private fun StyleSelectionItem(
    title: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (!isDark) {
        Color.White.copy(alpha = 0.85f)
    } else {
        Color.Black.copy(alpha = 0.85f)
    }
    val unselectedBorderColor = if (!isDark) {
        Color.White.copy(alpha = 0.4f)
    } else {
        Color.Black.copy(alpha = 0.3f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp
        )

        // Custom Rounded Checkbox Indicator
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFF00C853) else unselectedBorderColor,
                    shape = CircleShape
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF00C853), CircleShape)
                )
            }
        }
    }
}

@Composable
fun HomePreview(dootawasColors: DootawasColors, isDark: Boolean) {

    val totalUnreadChats = 5
    val totalUnreadGroups = 6
    val totalUnreadStories = 0 // Replace with your actual StateFlow or value
    val chatsText = stringResource(R.string.tab_chats)
    val groupsText = stringResource(R.string.tab_groups)
    val callsText = stringResource(R.string.tab_calls)
    val storiesText = stringResource(R.string.tab_stories)
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val tabData = remember(totalUnreadChats, totalUnreadGroups, totalUnreadStories) {
        listOf(
            Triple(chatsText, Icons.Default.ChatBubble, totalUnreadChats),
            Triple(groupsText, Icons.Default.Group, totalUnreadGroups),
            Triple(callsText, Icons.Default.Call, 0),
            Triple(storiesText, Icons.Default.PlayCircle, totalUnreadStories)
        )
    }


    val focusManager = LocalFocusManager.current

    val visibleTabs = 3
    val horizontalPadding = 8.dp
    val innerPadding = 4.dp

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val viewportWidth =
        screenWidth - (horizontalPadding * 2) - (innerPadding * 2)

    val tabWidth = viewportWidth / visibleTabs

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dootawasColors.homeBackground)
                .windowInsetsPadding(WindowInsets.systemBars)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {


            DootawasSearchBar(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it

//                    if (selectedTab == 0) {
//                        homeViewModel.searchUpbhogtas(it.text)
//                    } else {
//                        homeViewModel.searchGroups(it.text)
//                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(R.string.search),
                colors = dootawasColors,
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )

            val customTabFontFamily = FontFamily(
                Font(R.font.roboto_regular, FontWeight.Normal),
                Font(R.font.roboto_bold, FontWeight.Medium)
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
//                    .bottomShadow(
//                        color = Color.Black.copy(alpha = 0.05f),
//                        cornerRadius = 10.dp,
//                        blurRadius = 2.dp,
//                        offsetY = 3.dp
//                    )
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
                                fontSize = 15.sp,
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







            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dootawasColors.lastMessageColor.copy(alpha = 0.15f))
            ) {
            }
            Spacer(modifier = Modifier.height(4.dp))

            Spacer(Modifier.height(12.dp)) // keeps the title visually centered
            val upbhogtas = listOf(
                UpbhogtaUiItem(
                    userNumber = "1",
                    name = "Upbhogta",
                    lastMessage = "Hello 👋",
                    totalUnread = 3,
                    background = 0,
                    lastMessageTime = "10:45 AM",
                    sandeshStithi = SandeshStithi.PREKSHIT
                ),
                UpbhogtaUiItem(
                    userNumber = "2",
                    name = "Rahul",
                    lastMessage = "Kahan ho bhai.",
                    totalUnread = 2,
                    isTyping = true,
                    background = 0,
                    lastMessageTime = "Now",
                    sandeshStithi = SandeshStithi.SAMARPIT
                )
            )
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                when (selectedTab) {

                    0 -> {
                        val sortedUpbhogtas = upbhogtas.sortedWith(
                            compareByDescending<UpbhogtaUiItem> { it.isPinned }
                                .thenByDescending { it.totalUnread }
                        )

                        itemsIndexed(
                            items = sortedUpbhogtas,
                            key = { _, upbhogta -> upbhogta.userNumber }
                        ) { index, upbhogta ->
                            UpbhogtaItem(
                                upbhogta = upbhogta,
                                index = index,
                                lastIndex = sortedUpbhogtas.lastIndex,
                                isTyping = false,
                                onClick = { },
                                onLongClick = { },
                                onSwipeRight = { },
                                onSwipeLeft = { },
                                dootawasColors = dootawasColors
                            )
                        }
                    }


                    1 -> { // 🔸 Group tab
//                        val sortedGroups = groups.sortedWith(
//                            compareByDescending<GroupUiItem> { it.isPinned }
//                                .thenByDescending { it.totalUnread }
//                        )
//
//
//                        items(sortedGroups) { group ->
//                            GroupItem(
//                                groupItem = group,
//                                isTyping = typingUsers.contains(group.id),
//                                onClick = { selected ->
//                                    navController.navigate("group_screen/${group.id}")
//                                },
//                                onLongClick = { selectedGroupUpbhogta = it },
//                                onSwipeRight = {
//                                    Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
//                                },
//                                onSwipeLeft = {
//                                    Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
//                                }
//                            )
//                        }
                    }


                    2 -> { // 🔸 Calls tab
//                        items(callSessions) { session ->
//                            CallHistoryItem(session = session)
//                        }
                    }
                    3 -> { // Stories tab index
                        item {
                            StoriesLockedMessage()
                        }
                    }
                }
            }
        }
        BottomActionBar(
            isConnected = true,
            actionType = 2,
            sosContacts = emptyList(),
            onCreateList = {
            },
            onCreateGroup = { },
            onLinkDevice = { },
            onStories = { },
            onMenuClick = { },
            onContactsClick = { },
            sosTrigger = {},
            dootawasColors = dootawasColors
        )
    }
}




@Composable
fun ColorGrid16(
    theme: DootawasColors,
    onColorClick: (String, Color, Offset) -> Unit
) {

    val leftItems = listOf(
        "leftMsgBg" to theme.leftMessageBackground,
        "leftText" to theme.leftMessageText,
        "leftTimeBg" to theme.leftTimeBackGround,
        "leftTimeText" to theme.leftTimeText,
        "leftBorder" to theme.leftMessageBorderColor,
        "reply" to theme.replyBackground
    )

    val rightItems = listOf(
        "rightMsgBg" to theme.rightMessageBackground,
        "rightText" to theme.rightMessageText,
        "rightTimeBg" to theme.rightTimeBackGround,
        "rightTimeText" to theme.rightTimeText,
        "rightBorder" to theme.rightMessageBorderColor,
        "replyText" to theme.replyText
    )

    val midItems = listOf(
        "chatBackground" to theme.chatScreenBackground,
        "userHeaderBg" to theme.userHeaderBackground,
        "userHeaderText" to theme.userHeaderText,
        "chatWallpaper" to theme.chatScreenWallpaper
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

        // ⬅️ LEFT
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leftItems.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (key, color) ->
                        SmallColorBox(key, color, onColorClick)
                    }
                }
            }
        }

        // 🎯 MID
        Column(
            modifier = Modifier.wrapContentWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            midItems.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (key, color) ->
                        SmallColorBox(key, color, onColorClick)
                    }
                }
            }
        }

        // ➡️ RIGHT
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            rightItems.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    row.forEach { (key, color) ->
                        SmallColorBox(key, color, onColorClick)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorRow(
    items: List<Pair<String, Color>>,
    onColorClick: (String, Color, Offset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (key, color) ->
            ColorBoxWithCoachmark(
                key = key,
                color = color,
                onClick = onColorClick
            )
        }
    }
}

@Composable
fun ColorBoxWithCoachmark(
    key: String,
    color: Color,
    onClick: (String, Color, Offset) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .size(32.dp)
            .onGloballyPositioned {
                offset = it.positionInRoot()
            }
            .clip(shape) // 🔥 ensures border + bg match
            .background(color)
            .border(
                0.5.dp,
                Color.Red.copy(alpha = 0.6f), // cleaner red
                RoundedCornerShape(6.dp)
            )
            .clickable {
                onClick(key, color, offset)
            }
    )
}
@Composable
fun SmallColorBox(
    key: String,
    color: Color,
    onClick: (String, Color, Offset) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .size(32.dp)
            .onGloballyPositioned {
                offset = it.positionInRoot()
            }
            .clip(shape)
            .background(color)
            .border(1.dp, Color.Red.copy(0.6f), shape)
            .clickable {
                onClick(key, color, offset) // 🔥 send key + color + position
            }
    )
}

@Composable
fun CoachBubble(
    anchor: Offset,
    colorName: String,
    color: Color,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val hex = remember(color) { color.toHex() }
    LaunchedEffect(copied) {
        if (copied) { delay(1200.milliseconds); copied = false }
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val bubbleWidth = 140.dp
    val bubbleHeight = 80.dp
    val arrowSize = 12.dp
    val margin = 16.dp

    val bubbleColor = Color(0xFF312B34)

    val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
    val bubbleHeightPx = with(density) { bubbleHeight.toPx() }
    val arrowPx = with(density) { arrowSize.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }

    val offset = IntOffset(
        x = (anchor.x - bubbleWidthPx / 2)
            .coerceIn(marginPx, screenWidthPx - bubbleWidthPx - marginPx)
            .toInt(),
        y = (anchor.y - bubbleHeightPx - arrowPx - marginPx).toInt()
    )

    Box(modifier = Modifier.fillMaxSize()) {


        Card(
            modifier = Modifier.offset { offset },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(bubbleWidth)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = colorName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // COPY
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy color",
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("color", hex))
                                    )
                                    copied = true
                                }
                            }
                            .padding(8.dp)
                            .size(20.dp)
                    )

                    // EDIT
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit color",
                        tint = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onEdit() }
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }
}

fun DootawasColors.withField(field: String, c: Color): DootawasColors = when (field) {
    "chatBackground" -> copy(chatScreenBackground = c)
    "chatWallpaper" -> copy(chatScreenWallpaper = c)
    "wallpaperOverlay" -> copy(wallpaperOverlay = c)
    "leftMsgBg" -> copy(leftMessageBackground = c)
    "rightMsgBg" -> copy(rightMessageBackground = c)
    "leftText" -> copy(leftMessageText = c)
    "rightText" -> copy(rightMessageText = c)
    "leftTimeBg" -> copy(leftTimeBackGround = c)
    "rightTimeBg" -> copy(rightTimeBackGround = c)
    "leftTimeText" -> copy(leftTimeText = c)
    "rightTimeText" -> copy(rightTimeText = c)
    "leftBorder" -> copy(leftMessageBorderColor = c)
    "rightBorder" -> copy(rightMessageBorderColor = c)
    "reply" -> copy(replyBackground = c)
    "replyText" -> copy(replyText = c)
    "glass" -> copy(glassColor = c)
    "userHeaderBg" -> copy(userHeaderBackground = c)
    "userHeaderText" -> copy(userHeaderText = c)
    "userHeaderIcon" -> copy(userHeaderIcon = c)
    "header" -> copy(dialogHeader = c)
    "searchBg" -> copy(searchBarBackground = c)
    "searchText" -> copy(searchBarText = c)
    "searchFocused" -> copy(searchBarFocusedIndicator = c)
    "searchUnfocused" -> copy(searchBarUnfocusedIndicator = c)
    "homeBg" -> copy(homeBackground = c)
    "homeChatBg" -> copy(homeChatBackground = c)
    "homeText" -> copy(homeText = c)
    "homeHeaderBg" -> copy(homeHeaderBackground = c)
    "homeHeaderSelectedText" -> copy(homeHeaderSelectedText = c)
    "homeHeaderUnselectedText" -> copy(homeHeaderUnSelectedText = c)
    "homeHeaderSelectedBackground" -> copy(homeHeaderSelectedBackground = c)
    "homeHeaderIcon" -> copy(homeHeaderIcon = c)
    "buttonBg" -> copy(buttonBackground = c)
    "buttonText" -> copy(buttonText = c)
    "unread" -> copy(unreadBadgeColor = c)
    "lastMsgColor" -> copy(lastMessageColor = c)
    else -> this
}

fun Color.toHex(): String = String.format("#%08X", toArgb())   // #AARRGGBB
@Composable
fun BorderToggle(
    mode: BorderMode,
    onChange: (BorderMode) -> Unit
) {
    val text = when (mode) {
        BorderMode.NONE -> "No Border"
        BorderMode.SEMI -> "Semi Border"
        BorderMode.FULL -> "Full Border"
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .background(colorScheme.primary, RoundedCornerShape(14.dp))
            .clickable {
                val next = when (mode) {
                    BorderMode.NONE -> BorderMode.SEMI
                    BorderMode.SEMI -> BorderMode.FULL
                    BorderMode.FULL -> BorderMode.NONE
                }
                onChange(next) // ✅ enum, not Int
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
fun ColorSelector(
    label: String,
    color: Color,
    onClick: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(color) }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)

        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color, CircleShape)
        )
    }
}
@Composable
fun PresetColorRow(onColorSelected: (Color) -> Unit) {

    val presets = listOf(
        Color(0xFF000000),
        Color(0xFFFFFFFF),
        Color(0xFFFF5252),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFFFC107),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        presets.forEach { color ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(1.dp, Color.Gray, CircleShape)
                    .clickable { onColorSelected(color) }
            )
        }
    }
}


@Composable
fun AdvancedColorPickerScreen(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {

    var isHexEditing by remember { mutableStateOf(false) }

    val controller = rememberColorPickerController()

    var color by remember(initialColor) { mutableStateOf(initialColor) }
    var hex by remember(initialColor) { mutableStateOf(colorToHex(initialColor)) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(initialColor) {

        controller.selectByColor(initialColor, true)

        delay(50)

        controller.selectByColor(initialColor, true)

        isInitialized = true
    }
    BackHandler {
        onDismiss()
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = colorScheme.background
    ) {

        Column {

            // 🔝 Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }

                Text("Pick Color", style = MaterialTheme.typography.titleMedium)

                TextButton(onClick = { onColorSelected(color) }) {
                    Text("Apply")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {

// 🎨 Picker
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    controller = controller,
                    initialColor = initialColor,
                    onColorChanged = {

                        if (!isInitialized) {
                            return@HsvColorPicker
                        }


                        color = it.color.copy(alpha = color.alpha)
                        if (!isHexEditing) {
                            hex = colorToHex(color)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))

// 🌑 Brightness (FIXED POSITION)
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    controller = controller
                )

                Spacer(Modifier.height(14.dp))

                // 🔢 HEX + Preview
                Row(verticalAlignment = Alignment.CenterVertically) {

                    OutlinedTextField(
                        value = hex,
                        onValueChange = {
                            isHexEditing = true
                            hex = it

                            hexToColorOrNull(it)?.let { parsed ->
                                color = parsed
                                controller.selectByColor(parsed, true)
                            }
                        },
                        label = { Text("HEX") },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged {
                                if (!it.isFocused) isHexEditing = false
                            }
                    )

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(color, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 🎯 Presets
                PresetColorRow {
                    color = it
                    controller.selectByColor(it, true)
                    hex = colorToHex(it)
                }

                Spacer(Modifier.height(16.dp))

                // 🎛 RGB
                RGBSliders(color) { updated ->
                    color = updated
                    controller.selectByColor(updated, true)
                    hex = colorToHex(updated)
                }

                Spacer(Modifier.height(16.dp))

                // 🌈 Alpha
                AlphaSlider(color) { updated ->
                    color = updated
                    hex = colorToHex(updated)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun RGBSliders(color: Color, onChange: (Color) -> Unit) {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()

    SliderRow("R", r) { onChange(color.copy(red = it / 255f)) }
    SliderRow("G", g) { onChange(color.copy(green = it / 255f)) }
    SliderRow("B", b) { onChange(color.copy(blue = it / 255f)) }
}

@Composable
fun AlphaSlider(color: Color, onChange: (Color) -> Unit) {
    val alpha = (color.alpha * 255).toInt()

    SliderRow("A", alpha) {
        onChange(color.copy(alpha = it / 255f))
    }
}

@Composable
fun SliderRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value")
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f
        )
    }
}

fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

fun hexToColorOrNull(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        val value = clean.toLong(16).toInt()
        Color(value)
    } catch (e: Exception) {
        null
    }
}