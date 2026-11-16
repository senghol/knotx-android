package com.senghol.knotx.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import java.util.UUID

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

// NUMBER = auto index prefix ("1.", "2." ...). COUNTDOWN = live "2d 4h left"
// text computed from item.dueDate (shares the same date as the DATE field -
// you can enable either or both independently).
// CHECKBOX is kept only so old persisted lists still deserialize.

data class ListEditorState(val sandeshId: String, val initialData: ListData?)

// Only two screens: VIEW (the list, and where Save / Send happen) and EDIT.
// A brand-new list simply starts in EDIT.
enum class ListScreenMode { VIEW, EDIT }

enum class ListField {
    CHECKBOX,
    NUMBER,
    QTY,
    PRICE,
    DATE,
    COUNTDOWN
}

enum class ListMessageType {
    CREATE,
    UPDATE
}

/**
 * Per-item color tag. Only the NAME is persisted, so the same tag renders with
 * a light-mode shade in light theme and a dark-mode shade in dark theme (see
 * [ListColors.palette]). Null = no color (default surface).
 */
enum class ItemColor { PURPLE, BLUE, GREEN, ORANGE, PINK }

@Immutable
data class ListItem(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("text")
    val text: String = "",

    @SerializedName("checked")
    val checked: Boolean = false,

    @SerializedName("price")
    val price: Double? = null,

    @SerializedName("qty")
    val qty: String? = null,

    @SerializedName("dueDate")
    val dueDate: Long? = null,

    @SerializedName("addedBy")
    val addedBy: String? = null,

    @SerializedName("addedAt")
    val addedAt: Long? = null,

    // Old persisted lists have no "color" key -> deserializes as null (no color).
    @SerializedName("color")
    val color: ItemColor? = null,
)

@Immutable
data class ListData(

    // Version of THIS document.
    @SerializedName("version")
    val version: Long = 1,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("enabledFields")
    val enabledFields: Set<ListField> = emptySet(),

    @SerializedName("items")
    val items: List<ListItem> = emptyList(),

    @SerializedName("createdBy")
    val createdBy: String? = null,

    @SerializedName("createdAt")
    val createdAt: Long? = null,

    @SerializedName("updatedBy")
    val updatedBy: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: Long? = null,
)

@Immutable
data class ListMessage(
    @SerializedName("messageType")
    val messageType: ListMessageType = ListMessageType.CREATE,

    // Version this update is based on.
    // Ignored for CREATE.
    @SerializedName("baseVersion")
    val baseVersion: Long = 0,

    // Latest version accepted by the user.
    @SerializedName("currentVersion")
    val currentVersion: Long = 0,

    // Current accepted list.
    @SerializedName("current")
    val current: ListData,

    // Proposed update.
    // Null after Accept/Reject.
    @SerializedName("updated")
    val updated: ListData? = null,

    @SerializedName("listId")
    val listId: String? = null,
)

// ---------------------------------------------------------------------------
// Colors
// ---------------------------------------------------------------------------

/** accent = stripe / checkbox / dot; container = tinted card background. */
@Immutable
data class ItemPalette(val accent: Color, val container: Color)

@Immutable
data class ListColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val outline: Color,
    val checkedStrike: Color,
    val danger: Color,
    // Distinct accent for money so it doesn't get lost among plain text.
    val price: Color,
    // Same 5 hues in both themes, just tuned shades per theme.
    val palette: Map<ItemColor, ItemPalette>,
)

private val LightItemPalette = mapOf(
    ItemColor.PURPLE to ItemPalette(Color(0xFF6750A4), Color(0xFFE9DDFF)),
    ItemColor.BLUE to ItemPalette(Color(0xFF1E6FD9), Color(0xFFD6E6FF)),
    ItemColor.GREEN to ItemPalette(Color(0xFF2E7D32), Color(0xFFD3F0D5)),
    ItemColor.ORANGE to ItemPalette(Color(0xFFE65100), Color(0xFFFFE0CC)),
    ItemColor.PINK to ItemPalette(Color(0xFFC2185B), Color(0xFFFFD9E6)),
)

private val DarkItemPalette = mapOf(
    ItemColor.PURPLE to ItemPalette(Color(0xFFD0BCFF), Color(0xFF4A3F6B)),
    ItemColor.BLUE to ItemPalette(Color(0xFF90CAF9), Color(0xFF1F3B5C)),
    ItemColor.GREEN to ItemPalette(Color(0xFF81C784), Color(0xFF24432A)),
    ItemColor.ORANGE to ItemPalette(Color(0xFFFFB074), Color(0xFF5A3418)),
    ItemColor.PINK to ItemPalette(Color(0xFFF48FB1), Color(0xFF5E2740)),
)

private val LightListColors = ListColors(
    background = Color(0xFFFFFAF5),
    surface = Color(0xFFFAEEDA),
    surfaceVariant = Color(0xFFEBE4F0),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF57505E),
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    outline = Color(0xFF79747E),
    checkedStrike = Color(0xFF8D8791),
    danger = Color(0xFFB3261E),
    price = Color(0xFF2E7D32),
    palette = LightItemPalette,
)

private val DarkListColors = ListColors(
    background = Color(0xFF333333),
    surface = Color(0xFF504D4D),
    surfaceVariant = Color(0xFF362F40),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    outline = Color(0xFF938F99),
    checkedStrike = Color(0xFF7C7680),
    danger = Color(0xFFF2B8B5),
    price = Color(0xFF81C784),
    palette = DarkItemPalette,
)

@Composable
fun rememberListColors(): ListColors {
    val dark = isSystemInDarkTheme()
    return remember(dark) { if (dark) DarkListColors else LightListColors }
}

@Composable
private fun SaveActionButton(
    label: String,
    enabled: Boolean,
    colors: ListColors,
    icon: ImageVector = Icons.Default.Check,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (enabled) colors.primary else colors.surfaceVariant,
        animationSpec = tween(200),
        label = "save_btn_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (enabled) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = tween(200),
        label = "save_btn_fg",
    )
    Row(
        modifier = Modifier
            .shadow(if (enabled) 3.dp else 0.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/**
 * Top-level stateful entry point for the List feature - just TWO screens:
 *
 *  VIEW - the list exactly as it will be saved. Checkbox taps, swipe-deletes and
 *         anything coming back from EDIT are held as unsaved changes; when there
 *         are any, a bottom bar appears with "Save" and "Save & Send".
 *  EDIT - change title, columns, items and colors. "Done" goes back to VIEW
 *         (it does NOT save).
 *
 * [initialData] null -> a new list, which starts in EDIT.
 *
 * [onSave]       persists the list (called by Save and Save & Send).
 * [onSendUpdate] is called by "Save & Send" (after onSave) and by "Push" (nothing pending).
 * [onCreateBill] is called with the list exactly as shown in VIEW (incl. unsaved changes)
 *               when the user taps the bill icon; the host opens the bill screen.
 * [onDiscardConfirmNeeded] is invoked when leaving with unsaved changes.
 */
@Composable
fun ListScreen(
    initialData: ListData?,
    currentUserId: String,
    onSave: (ListData) -> Unit,
    onBack: () -> Unit,
    onDiscardConfirmNeeded: (onConfirmDiscard: () -> Unit) -> Unit,
    onSendUpdate: (ListData) -> Unit = {},
    onCreateBill: (ListData) -> Unit = {},
) {
    val colors = rememberListColors()

    var savedData by remember {
        mutableStateOf(initialData ?: ListData(enabledFields = setOf(ListField.CHECKBOX)))
    }
    var mode by remember {
        mutableStateOf(if (initialData == null) ListScreenMode.EDIT else ListScreenMode.VIEW)
    }
    // What VIEW shows: saved data + any unsaved changes (checks, deletes, edits).
    var pendingViewData by remember { mutableStateOf(savedData) }
    // Working copy while in EDIT, and what it looked like when EDIT was opened.
    var draft by remember { mutableStateOf(savedData) }
    var editBaseline by remember { mutableStateOf(savedData) }
    var showFieldsDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    // VIEW is locked by default so qty / price / color can't be changed by a stray tap.
    var unlocked by remember { mutableStateOf(false) }

    val hasUnsavedChanges = pendingViewData != savedData
    val doneEnabled = savedData.createdAt != null || draft.title.isNotBlank() || draft.items.isNotEmpty()

    fun enterEdit() {
        editBaseline = pendingViewData
        draft = pendingViewData
        unlocked = false
        mode = ListScreenMode.EDIT
    }

    // Done: hand the draft to VIEW as unsaved changes. Nothing is persisted yet.
    fun finishEdit() {
        pendingViewData = draft
        unlocked = false
        mode = ListScreenMode.VIEW
    }

    // Leave EDIT without applying the draft.
    fun leaveEdit() {
        if (savedData.createdAt == null && editBaseline == savedData) {
            onBack() // brand-new list, never got past the first edit -> nothing to go back to
        } else {
            draft = editBaseline
            mode = ListScreenMode.VIEW
        }
    }

    /** Persists whatever VIEW is showing and returns what was saved. */
    fun commitPending(): ListData {
        val now = System.currentTimeMillis()
        val base = pendingViewData.copy(title = pendingViewData.title.ifBlank { "New List" })
        val toSave = if (savedData.createdAt == null) {
            // First-ever save - this list doesn't exist yet.
            base.copy(
                version = 1,
                createdBy = currentUserId,
                createdAt = now,
                updatedBy = null,
                updatedAt = null,
            )
        } else {
            // Editing something that already exists - bump the version.
            base.copy(
                version = savedData.version + 1,
                updatedBy = currentUserId,
                updatedAt = now,
            )
        }
        savedData = toSave
        pendingViewData = toSave
        draft = toSave
        onSave(toSave)
        unlocked = false // re-lock after saving
        return toSave
    }

    fun requestBack() {
        when (mode) {
            ListScreenMode.EDIT ->
                if (draft != editBaseline) onDiscardConfirmNeeded { leaveEdit() } else leaveEdit()
            ListScreenMode.VIEW ->
                if (hasUnsavedChanges) onDiscardConfirmNeeded { pendingViewData = savedData; onBack() } else onBack()
        }
    }

    // Checkbox / qty / price / color edits made directly in VIEW (unsaved until Save).
    fun changeItemInView(updated: ListItem) {
        pendingViewData = pendingViewData.copy(
            items = pendingViewData.items.map { if (it.id == updated.id) updated else it }
        )
    }

    fun deleteItemInView(itemId: String) {
        pendingViewData = pendingViewData.copy(
            items = pendingViewData.items.filterNot { it.id == itemId }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            ListTopBar(
                mode = mode,
                title = if (mode == ListScreenMode.VIEW) pendingViewData.title else draft.title,
                hasUnsavedChanges = hasUnsavedChanges,
                unlocked = unlocked,
                doneEnabled = doneEnabled,
                colors = colors,
                onTitleChange = { draft = draft.copy(title = it) },
                onBack = { requestBack() },
                canCreateBill = pendingViewData.items.isNotEmpty(),
                onCreateBillClick = { onCreateBill(pendingViewData) },
                onToggleLock = { unlocked = !unlocked },
                onEditClick = { enterEdit() },
                onDoneClick = { finishEdit() },
                onColumnsClick = { showFieldsDialog = true },
                onPushClick = { showSendDialog = true },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = mode == ListScreenMode.VIEW && hasUnsavedChanges,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                SaveActionBar(
                    colors = colors,
                    onSave = { commitPending() },
                    onSaveAndSend = { onSendUpdate(commitPending()) },
                )
            }
        }
    ) { padding ->
        // Cross-fade + slight scale between the two screens instead of a hard cut.
        AnimatedContent(
            targetState = mode,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 60)) +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(220, delayMillis = 60)))
                    .togetherWith(fadeOut(animationSpec = tween(150)))
            },
            label = "list_mode_transition",
        ) { targetMode ->
            when (targetMode) {
                ListScreenMode.VIEW -> ListViewMode(
                    data = pendingViewData,
                    colors = colors,
                    unlocked = unlocked,
                    onItemChange = ::changeItemInView,
                    onDeleteItem = ::deleteItemInView,
                )
                ListScreenMode.EDIT -> ListEditMode(
                    data = draft,
                    colors = colors,
                    currentUserId = currentUserId,
                    onDataChange = { draft = it },
                )
            }
        }
    }

    if (showFieldsDialog) {
        FieldsSelectionDialog(
            enabledFields = draft.enabledFields,
            colors = colors,
            onToggle = { field ->
                val next = draft.enabledFields.toMutableSet()
                if (field in next) next.remove(field) else next.add(field)
                draft = draft.copy(enabledFields = next)
            },
            onDismiss = { showFieldsDialog = false },
        )
    }

    if (showSendDialog) {
        AlertDialog(
            onDismissRequest = { showSendDialog = false },
            title = { Text("Send updates?") },
            text = { Text("This will send the current version of this list back to the other person.") },
            confirmButton = {
                TextButton(onClick = {
                    onSendUpdate(savedData)
                    showSendDialog = false
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { showSendDialog = false }) { Text("Cancel") }
            },
            containerColor = colors.surface,
        )
    }
}

// ---------------------------------------------------------------------------
// Top bar - title lives here and IS the editor (no separate name field
// anywhere else), plus the "Columns" entry point for the fields dialog.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(
    mode: ListScreenMode,
    title: String,
    hasUnsavedChanges: Boolean,
    unlocked: Boolean,
    doneEnabled: Boolean,
    colors: ListColors,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    canCreateBill: Boolean,
    onCreateBillClick: () -> Unit,
    onToggleLock: () -> Unit,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onColumnsClick: () -> Unit,
    onPushClick: () -> Unit,
) {
    val editing = mode == ListScreenMode.EDIT

    TopAppBar(
        title = {
            if (editing) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text("New list", color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                            }
                            inner()
                        }
                    },
                )
            } else {
                Text(
                    text = title.ifBlank { "New list" },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (editing) {
                IconButton(onClick = onColumnsClick) {
                    Icon(Icons.Default.ViewColumn, contentDescription = "Columns", tint = colors.primary)
                }
                SaveActionButton(label = "Done", enabled = doneEnabled, colors = colors, onClick = onDoneClick)
                Spacer(Modifier.width(8.dp))
            } else {
                IconButton(onClick = onToggleLock) {
                    Icon(
                        imageVector = if (unlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = if (unlocked) "Lock" else "Unlock to edit qty, price and color",
                        tint = if (unlocked) colors.primary else colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCreateBillClick, enabled = canCreateBill) {
                    CreateBillIcon(
                        colors = colors,
                        tint = if (canCreateBill) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                // Push only makes sense when what's on screen is what was saved.
                if (!hasUnsavedChanges) {
                    TextButton(onClick = onPushClick) {
                        Text("Push", color = colors.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onEditClick) { Text("Edit") }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.onSurface,
            navigationIconContentColor = colors.onSurface,
            actionIconContentColor = colors.primary,
        )
    )
}

// ---------------------------------------------------------------------------
// "Create bill" icon - Material has no list+rupee glyph, so it is composed:
// a list icon with a small rupee badge on its corner (no text involved).
// ---------------------------------------------------------------------------

@Composable
private fun CreateBillIcon(
    colors: ListColors,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(28.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ListAlt,
            contentDescription = "Create bill",
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopStart),
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CurrencyRupee,
                contentDescription = null,
                tint = colors.background,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Columns dialog - every option shown with an icon in an active / inactive
// state, tapping toggles it straight into enabledFields.
// ---------------------------------------------------------------------------

private data class FieldSpec(val field: ListField, val label: String, val icon: ImageVector)

private val fieldSpecs = listOf(
    FieldSpec(ListField.CHECKBOX, "Checkbox", Icons.Default.CheckBox),
    FieldSpec(ListField.NUMBER, "Number", Icons.Default.FormatListNumbered),
    FieldSpec(ListField.QTY, "Quantity", Icons.Default.Inventory2),
    FieldSpec(ListField.PRICE, "Price", Icons.Default.AttachMoney),
    FieldSpec(ListField.DATE, "Date", Icons.Default.CalendarToday),
    FieldSpec(ListField.COUNTDOWN, "Countdown", Icons.Default.HourglassBottom),
)

@Composable
private fun FieldsSelectionDialog(
    enabledFields: Set<ListField>,
    colors: ListColors,
    onToggle: (ListField) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Columns", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fieldSpecs.forEach { spec ->
                    ColumnOptionRow(
                        spec = spec,
                        active = spec.field in enabledFields,
                        colors = colors,
                        onClick = { onToggle(spec.field) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        containerColor = colors.surface,
    )
}

@Composable
private fun ColumnOptionRow(
    spec: FieldSpec,
    active: Boolean,
    colors: ListColors,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (active) colors.primary else colors.surfaceVariant, tween(200), label = "col_bg")
    val fg by animateColorAsState(if (active) colors.onPrimary else colors.onSurfaceVariant, tween(200), label = "col_fg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(spec.icon, contentDescription = null, tint = fg)
        Spacer(Modifier.width(12.dp))
        Text(spec.label, color = fg, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        AnimatedVisibility(visible = active) {
            Icon(Icons.Default.Check, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Item color picker pieces
// ---------------------------------------------------------------------------

@Composable
private fun SwatchCircle(
    fill: Color,
    selected: Boolean,
    colors: ListColors,
    contentDescription: String,
    icon: ImageVector?,
    iconTint: Color,
    onClick: () -> Unit,
) {
    val ring by animateColorAsState(
        targetValue = if (selected) colors.onSurface else Color.Transparent,
        animationSpec = tween(150),
        label = "swatch_ring",
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(2.dp, ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(14.dp))
        }
    }
}

/** "No color" + the 5 colors. Selecting the active one again is a no-op; use the first circle to clear. */
@Composable
private fun ColorSwatchRow(
    selected: ItemColor?,
    colors: ListColors,
    onSelect: (ItemColor?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwatchCircle(
            fill = colors.surfaceVariant,
            selected = selected == null,
            colors = colors,
            contentDescription = "No color",
            icon = Icons.Default.Close,
            iconTint = colors.onSurfaceVariant,
            onClick = { onSelect(null) },
        )
        ItemColor.values().forEach { c ->
            val p = colors.palette.getValue(c)
            SwatchCircle(
                fill = p.accent,
                selected = selected == c,
                colors = colors,
                contentDescription = c.name.lowercase(),
                icon = if (selected == c) Icons.Default.Check else null,
                iconTint = colors.onPrimary,
                onClick = { onSelect(c) },
            )
        }
    }
}

/** Small round button that shows the current color (or a palette icon) and toggles the swatch row. */
@Composable
private fun ColorToggleDot(
    color: ItemColor?,
    colors: ListColors,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 30.dp,
) {
    val fill by animateColorAsState(
        targetValue = color?.let { colors.palette.getValue(it).accent } ?: colors.surfaceVariant,
        animationSpec = tween(200),
        label = "color_dot_fill",
    )
    Box(
        modifier = Modifier
            .size(size)
            .shadow(2.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == null) {
            Icon(
                Icons.Default.Palette,
                contentDescription = "Item color",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Swipe-to-reveal delete - swipe a row left to reveal a delete button behind
// it; tapping the button deletes. Swiping back closes it.
// ---------------------------------------------------------------------------

@Composable
private fun SwipeToRevealDelete(
    colors: ListColors,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    addedAtLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidth = 72.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(colors.danger)
                    .clickable {
                        onDelete()
                        scope.launch { offsetX.snapTo(0f) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    if (addedAtLabel != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = addedAtLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val target = if (offsetX.value < -actionWidthPx / 2) -actionWidthPx else 0f
                            scope.launch { offsetX.animateTo(target, spring(dampingRatio = Spring.DampingRatioNoBouncy)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newVal = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)
                            scope.launch { offsetX.snapTo(newVal) }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// VIEW MODE - read-only except the checkbox (pending until Save) and
// swipe-delete. Elevated cards, no borders anywhere.
// ---------------------------------------------------------------------------

@Composable
private fun ListColumnHeader(
    enabledFields: Set<ListField>,
    colors: ListColors,
) {
    val showQty = ListField.QTY in enabledFields
    val showPrice = ListField.PRICE in enabledFields
    if (!showQty && !showPrice) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Spacer(Modifier.weight(1f))
        if (showQty) {
            Text(
                text = "Qty",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (showPrice) {
            Spacer(Modifier.width(60.dp)) // roughly lines up with number + checkbox
            Text(
                text = "Price",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.width(50.dp)) // roughly lines up with number + checkbox
    }
}

@Composable
private fun ListViewMode(
    data: ListData,
    colors: ListColors,
    unlocked: Boolean,
    onItemChange: (ListItem) -> Unit,
    onDeleteItem: (itemId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val total = data.items.sumOf { it.price ?: 0.0 }
        if (ListField.PRICE in data.enabledFields && total > 0.0) {
            item {
                Text(
                    text = "Total ${formatPrice(total)}",
                    color = colors.price,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        itemsIndexed(data.items, key = { _, it -> it.id }) { index, listItem ->
            SwipeToRevealDelete(
                colors = colors,
                onDelete = { onDeleteItem(listItem.id) },
                modifier = Modifier.animateItem(),
                addedAtLabel = listItem.addedAt?.let { formatShortDate(it) },
            ) {
                ViewModeItemRow(
                    item = listItem,
                    index = index,
                    enabledFields = data.enabledFields,
                    colors = colors,
                    unlocked = unlocked,
                    onChange = onItemChange,
                )
            }
        }
        if (data.items.isEmpty()) {
            item {
                Text(
                    text = "No items yet",
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

/**
 * VIEW-mode card. Checkbox, quantity, price and color are editable right here
 * (only for columns that are enabled). Adding / removing columns and adding
 * items stay in EDIT. All changes are unsaved until Save.
 */
@Composable
private fun ViewModeItemRow(
    item: ListItem,
    index: Int,
    enabledFields: Set<ListField>,
    colors: ListColors,
    unlocked: Boolean,
    onChange: (ListItem) -> Unit,
) {
    val elevation by animateDpAsState(
        targetValue = if (item.checked) 1.dp else 4.dp,
        animationSpec = tween(250),
        label = "item_elevation",
    )
    val contentColor by animateColorAsState(
        targetValue = if (item.checked) colors.checkedStrike else colors.onSurface,
        animationSpec = tween(250),
        label = "item_text_color",
    )
    val palette = item.color?.let { colors.palette.getValue(it) }
    val containerColor by animateColorAsState(
        targetValue = palette?.container ?: colors.surface,
        animationSpec = tween(250),
        label = "item_container",
    )

    var showColors by remember(item.id) { mutableStateOf(false) }
    // Local text buffer so partial input like "12." isn't snapped back while typing.
    var priceText by remember(item.id) { mutableStateOf(item.price?.let(::formatPriceInput) ?: "") }

    val showDateRow = (ListField.DATE in enabledFields || ListField.COUNTDOWN in enabledFields) && item.dueDate != null
    val toggleChecked = { onChange(item.copy(checked = !item.checked)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(16.dp), clip = false)
            .clickable(enabled = ListField.CHECKBOX in enabledFields) { toggleChecked() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Colored accent stripe on the leading edge (clipped by the card shape).
            if (palette != null) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(palette.accent)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Main row: number, checkbox, name, qty, price, color dot.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ListField.NUMBER in enabledFields) {
                        Text(
                            text = "${index + 1}.",
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    if (ListField.CHECKBOX in enabledFields) {
                        AnimatedCheckBadge(
                            checked = item.checked,
                            colors = colors,
                            accent = palette?.accent ?: colors.primary,
                            onClick = toggleChecked,
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = item.text,
                        color = contentColor,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (ListField.QTY in enabledFields) {
                        if (unlocked) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                            ) {
                                CompactNumericField(
                                    value = item.qty ?: "",
                                    onValueChange = { onChange(item.copy(qty = it.ifBlank { null })) },
                                    placeholder = "Qty",
                                    colors = colors,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else if (!item.qty.isNullOrBlank()) {
                            Text(
                                text = item.qty,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    if (ListField.PRICE in enabledFields) {
                        if (unlocked) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .width(76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                            ) {
                                CompactNumericField(
                                    value = priceText,
                                    onValueChange = { filtered ->
                                        priceText = filtered
                                        if (filtered.isBlank()) {
                                            onChange(item.copy(price = null))
                                        } else {
                                            filtered.toDoubleOrNull()?.let { onChange(item.copy(price = it)) }
                                        }
                                    },
                                    placeholder = "Price",
                                    colors = colors,
                                    textWeight = FontWeight.SemiBold,
                                    textColor = colors.price,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else if (item.price != null) {
                            Text(
                                text = formatPrice(item.price),
                                color = colors.price,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    if (unlocked) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Item color",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
//                        ColorToggleDot(
//                            color = item.color,
//                            colors = colors,
//                            onClick = { showColors = !showColors },
//                            size = 24.dp,
//                        )
                    }
                }

                // Second line: date on the left, countdown on the right (read-only).
                AnimatedVisibility(visible = showDateRow) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        if (ListField.DATE in enabledFields && item.dueDate != null) {
                            Text(
                                text = formatShortDate(item.dueDate),
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                        if (ListField.COUNTDOWN in enabledFields && item.dueDate != null) {
                            Text(
                                text = formatCountdown(item.dueDate),
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showColors && unlocked,
                    enter = expandVertically(tween(200)) + fadeIn(),
                    exit = shrinkVertically(tween(150)) + fadeOut(),
                ) {
                    ColorSwatchRow(
                        selected = item.color,
                        colors = colors,
                        onSelect = { onChange(item.copy(color = it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedCheckBadge(
    checked: Boolean,
    colors: ListColors,
    accent: Color = colors.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (checked) accent else colors.surface,
        animationSpec = tween(200),
        label = "check_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accent else colors.outline,
        animationSpec = tween(200),
        label = "check_border",
    )
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check_scale",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(if (checked) 3.dp else 0.dp, RoundedCornerShape(8.dp), clip = false)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Checked",
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom action bar - only shown in VIEW when there are unsaved changes.
// ---------------------------------------------------------------------------

@Composable
private fun SaveActionBar(
    colors: ListColors,
    onSave: () -> Unit,
    onSaveAndSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.onSurface,
                ),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onSaveAndSend,
                modifier = Modifier.weight(1.4f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save & Send", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// EDIT MODE - filled, borderless inputs floating on soft-shadowed
// surfaces. Items scroll; the add-item bar is pinned to the bottom.
// ---------------------------------------------------------------------------

@Composable
private fun ListEditMode(
    data: ListData,
    colors: ListColors,
    currentUserId: String,
    onDataChange: (ListData) -> Unit,
) {
    var newItemText by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemDueDate by remember { mutableStateOf<Long?>(null) }
    var newItemColor by remember { mutableStateOf<ItemColor?>(null) }
    var showAddDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ListColumnHeader(enabledFields = data.enabledFields, colors = colors)
            }
            itemsIndexed(data.items, key = { _, it -> it.id }) { index, listItem ->
                SwipeToRevealDelete(
                    colors = colors,
                    onDelete = { onDataChange(data.copy(items = data.items.filterNot { it.id == listItem.id })) },
                    modifier = Modifier.animateItem(),
                    addedAtLabel = listItem.addedAt?.let(::formatAddedCaption),
                ) {
                    EditModeItemRow(
                        item = listItem,
                        index = index,
                        enabledFields = data.enabledFields,
                        colors = colors,
                        onChange = { updated ->
                            onDataChange(data.copy(items = data.items.map { if (it.id == updated.id) updated else it }))
                        },
                    )
                }
            }
        }

        AddItemBar(
            text = newItemText,
            qty = newItemQty,
            price = newItemPrice,
            dueDate = newItemDueDate,
            color = newItemColor,
            enabledFields = data.enabledFields,
            colors = colors,
            onTextChange = { newItemText = it },
            onQtyChange = { newItemQty = it },
            onPriceChange = { newItemPrice = it },
            onColorChange = { newItemColor = it },
            onPickDate = { showAddDatePicker = true },
            onClearDate = { newItemDueDate = null },
            onAdd = {
                if (newItemText.isNotBlank()) {
                    onDataChange(
                        data.copy(
                            items = data.items + ListItem(
                                text = newItemText.trim(),
                                qty = newItemQty.ifBlank { null },
                                price = newItemPrice.toDoubleOrNull(),
                                dueDate = newItemDueDate,
                                color = newItemColor,
                                addedBy = currentUserId,
                                addedAt = System.currentTimeMillis(),
                            )
                        )
                    )
                    newItemText = ""
                    newItemQty = ""
                    newItemPrice = ""
                    newItemDueDate = null
                    newItemColor = null
                }
            },
        )
    }

    if (showAddDatePicker) {
        ListDatePickerDialog(
            initialMillis = newItemDueDate,
            onDismiss = { showAddDatePicker = false },
            onConfirm = { millis -> newItemDueDate = millis; showAddDatePicker = false },
            onClear = { newItemDueDate = null; showAddDatePicker = false },
        )
    }
}

/**
 * A soft, shadowed, filled surface that stands in for an "input box" without
 * ever drawing a stroked outline.
 */
@Composable
private fun SoftField(
    modifier: Modifier = Modifier,
    colors: ListColors,
    elevation: androidx.compose.ui.unit.Dp = 3.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(14.dp), clip = false)
            .background(colors.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun BorderlessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: ListColors,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    textWeight: FontWeight = FontWeight.Normal,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = textWeight),
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = colors.primary,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
        ),
    )
}

// Raw numeric text for editing (no currency symbol) - kept separate from the
// display-formatted `formatPrice` used in read-only contexts.
private fun formatPriceInput(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun filterDecimal(input: String): String {
    var seenDot = false
    return input.filter { ch ->
        when {
            ch.isDigit() -> true
            ch == '.' && !seenDot -> { seenDot = true; true }
            else -> false
        }
    }
}

@Composable
private fun EditModeItemRow(
    item: ListItem,
    index: Int,
    enabledFields: Set<ListField>,
    colors: ListColors,
    onChange: (ListItem) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }

    // Local text buffer is the source of truth for what's *displayed* in the
    // price field, so partial input like "12." doesn't snap back. We only push
    // a parsed Double up when it parses cleanly, and null when emptied.
    var priceText by remember(item.id) { mutableStateOf(item.price?.let(::formatPriceInput) ?: "") }

    val palette = item.color?.let { colors.palette.getValue(it) }
    val cardColor by animateColorAsState(
        targetValue = palette?.container ?: colors.surface,
        animationSpec = tween(250),
        label = "edit_item_container",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), clip = false)
            .background(cardColor, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // Main row: number, checkbox, item text, quantity ... price (end).
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ListField.NUMBER in enabledFields) {
                Text(
                    text = "${index + 1}.",
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            if (ListField.CHECKBOX in enabledFields) {
                AnimatedCheckBadge(
                    checked = item.checked,
                    colors = colors,
                    accent = palette?.accent ?: colors.primary,
                ) { onChange(item.copy(checked = !item.checked)) }
            }
            BorderlessTextField(
                value = item.text,
                onValueChange = { onChange(item.copy(text = it)) },
                placeholder = "Item",
                colors = colors,
                modifier = Modifier.weight(1f),
            )
            if (ListField.QTY in enabledFields) {
                BorderlessTextField(
                    value = item.qty ?: "",
                    onValueChange = { onChange(item.copy(qty = filterDecimal(it).ifBlank { null })) },
                    placeholder = "Qty",
                    colors = colors,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.width(90.dp),
                )
            }
            if (ListField.PRICE in enabledFields) {
                BorderlessTextField(
                    value = priceText,
                    onValueChange = { raw ->
                        val filtered = filterDecimal(raw)
                        priceText = filtered
                        when {
                            filtered.isBlank() -> onChange(item.copy(price = null))
                            else -> filtered.toDoubleOrNull()?.let { onChange(item.copy(price = it)) }
                        }
                    },
                    placeholder = "Price",
                    colors = colors,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.width(90.dp),
                    textWeight = FontWeight.SemiBold,
                )
            }
        }

        // Second row: date / countdown chips on the left, color dot always at the end.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ListField.DATE in enabledFields) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(item.dueDate?.let { formatShortDate(it) } ?: "Set date") },
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(containerColor = colors.surface),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
            if (ListField.COUNTDOWN in enabledFields && item.dueDate != null) {
                Spacer(Modifier.width(6.dp))
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(formatCountdown(item.dueDate)) },
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(containerColor = colors.surface),
                    leadingIcon = {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
            Spacer(Modifier.weight(1f))
//            ColorToggleDot(
//                color = item.color,
//                colors = colors,
//                onClick = { showColors = !showColors },
//            )
            Icon(
                Icons.Default.Palette,
                contentDescription = "Item color",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = showColors,
            enter = expandVertically(tween(200)) + fadeIn(),
            exit = shrinkVertically(tween(150)) + fadeOut(),
        ) {
            ColorSwatchRow(
                selected = item.color,
                colors = colors,
                onSelect = { onChange(item.copy(color = it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        ListDatePickerDialog(
            initialMillis = item.dueDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis -> onChange(item.copy(dueDate = millis)); showDatePicker = false },
            onClear = { onChange(item.copy(dueDate = null)); showDatePicker = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Sticky add-item bar - pinned to the bottom of edit/create mode.
//
// The item-name input and the add button always get their own row; qty /
// price / date only appear in a row above - and only if enabled. A color dot
// next to the add button reveals the swatch row for the new item.
// ---------------------------------------------------------------------------

@Composable
private fun CompactNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: ListColors,
    modifier: Modifier = Modifier,
    textWeight: FontWeight = FontWeight.Normal,
    textColor: Color = colors.onSurface,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(filterDecimal(it)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor, fontWeight = textWeight),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun AddItemBar(
    text: String,
    qty: String,
    price: String,
    dueDate: Long?,
    color: ItemColor?,
    enabledFields: Set<ListField>,
    colors: ListColors,
    onTextChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onColorChange: (ItemColor?) -> Unit,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit,
    onAdd: () -> Unit,
) {
    val showDateAffordance = ListField.DATE in enabledFields || ListField.COUNTDOWN in enabledFields
    val showSecondaryRow = ListField.QTY in enabledFields || ListField.PRICE in enabledFields || showDateAffordance
    var showColors by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Color swatches for the item being added (toggled by the dot below).
            AnimatedVisibility(
                visible = showColors,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(150)) + fadeOut(),
            ) {
                ColorSwatchRow(
                    selected = color,
                    colors = colors,
                    onSelect = onColorChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Row 1: Qty / Price / Date / Countdown
            AnimatedVisibility(visible = showSecondaryRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (ListField.QTY in enabledFields) {
                        SoftField(
                            colors = colors,
                            elevation = 1.dp,
                            modifier = Modifier
                                .width(92.dp)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            CompactNumericField(
                                value = qty,
                                onValueChange = onQtyChange,
                                placeholder = "Qty",
                                colors = colors,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (ListField.PRICE in enabledFields) {
                        SoftField(
                            colors = colors,
                            elevation = 1.dp,
                            modifier = Modifier
                                .width(108.dp)
                                .fillMaxHeight()
                        ) {
                            Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            CompactNumericField(
                                value = price,
                                onValueChange = onPriceChange,
                                placeholder = "Price",
                                colors = colors,
                                textWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (ListField.DATE in enabledFields) {
                        AssistChip(
                            onClick = onPickDate,
                            label = { Text(dueDate?.let { formatShortDate(it) } ?: "Date") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            trailingIcon = if (dueDate != null) {
                                {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear date",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onClearDate() },
                                    )
                                }
                            } else null,
                            border = null,
                            elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp),
                            colors = AssistChipDefaults.assistChipColors(containerColor = colors.surfaceVariant),
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }

                    if (ListField.COUNTDOWN in enabledFields) {
                        AssistChip(
                            onClick = onPickDate,
                            label = { Text(dueDate?.let { formatCountdown(it) } ?: "Countdown") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            border = null,
                            elevation = AssistChipDefaults.assistChipElevation(elevation = 1.dp),
                            colors = AssistChipDefaults.assistChipColors(containerColor = colors.surfaceVariant),
                            modifier = Modifier.fillMaxHeight(),
                        )
                    }

                    Spacer(Modifier.weight(1f))
                }
            }

            // Row 2: Name input, color dot, Add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BorderlessTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = "Add an item",
                    colors = colors,
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(16.dp), clip = false)
                        .background(colors.surfaceVariant, RoundedCornerShape(16.dp)),
                )

                ColorToggleDot(
                    color = color,
                    colors = colors,
                    onClick = { showColors = !showColors },
                    size = 36.dp,
                )

                val canAdd = text.isNotBlank()
                val scale by animateFloatAsState(
                    targetValue = if (canAdd) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "add_btn_scale",
                )

                FilledIconButton(
                    onClick = onAdd,
                    enabled = canAdd,
                    modifier = Modifier
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add item",
                        tint = colors.onPrimary
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date picker - native Material3 dialog, used both for existing items and
// for a new item that hasn't been added yet.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListDatePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onConfirm) }, enabled = state.selectedDateMillis != null) { Text("Set") }
        },
        dismissButton = {
            Row {
                if (initialMillis != null) TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

private val shortDateFormat = SimpleDateFormat("d MMM", Locale.getDefault())

private fun formatShortDate(millis: Long): String = shortDateFormat.format(Date(millis))

fun formatPrice(value: Double): String =
    if (value == value.toLong().toDouble()) "₹${value.toLong()}" else "₹${"%.2f".format(value)}"

private fun formatCountdown(dueMillis: Long): String {
    val diff = dueMillis - System.currentTimeMillis()
    if (diff <= 0) return "Overdue"
    val days = diff / (24 * 60 * 60 * 1000)
    val hours = (diff / (60 * 60 * 1000)) % 24
    return when {
        days > 0 -> "${days}d ${hours}h left"
        hours > 0 -> "${hours}h left"
        else -> "<1h left"
    }
}

private fun formatAddedCaption(addedAtMillis: Long): String {
    val diff = System.currentTimeMillis() - addedAtMillis
    if (diff < 0) return "Added just now"
    val days = diff / (24 * 60 * 60 * 1000)
    val hours = (diff / (60 * 60 * 1000)) % 24
    return when {
        days > 0 -> "Added ${days}d ago"
        hours > 0 -> "Added ${hours}h ago"
        else -> "Added just now"
    }
}