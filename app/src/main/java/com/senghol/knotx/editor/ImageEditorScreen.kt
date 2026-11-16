package com.senghol.knotx.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
// also import your DisappearingMessagesPickerDialog here
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.senghol.knotx.ui.components.DisappearingMessagesPickerDialog
import com.senghol.knotx.ui.contactPicker.ContactPickerScreen
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.contactPicker.PickerSources
import com.senghol.knotx.ui.theme.LocalDootawasTheme

class EditorPalette(val bg: Color, val text: Color) {
    val panel: Color = lerp(bg, text, 0.07f)   // cards and sheets: background nudged toward the text color
}

@Composable
fun ui(): EditorPalette {
    val a = LocalDootawasTheme.current
    val c = a.colors
    return remember(c.homeBackground, c.homeText) {
        EditorPalette(c.homeBackground, c.homeText)
    }
}
val Accent = Color(0xFF5266FF) // single accent color, change here

val Palette = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF2A6D, 0xFFFF9F1C, 0xFFFFE600,
    0xFF05FFA1, 0xFF00BBF9, 0xFF9B5DE5
).map { it.toInt() }

val Emojis = listOf(
    "😀", "😂", "😍", "🔥", "❤️", "👍", "🎉", "😎",
    "✨", "💯", "🙏", "😢", "🥳", "🤔", "😡", "👀"
)

/**
 * Standalone image editor.
 *
 * @param sourceUri image to edit (read in place, never modified or copied)
 * @param onDone    called after export with ONE finished jpeg (cacheDir) and the caption (may be empty).
 *                  The caller owns that file and deletes it when finished.
 * @param onCancel  user closed the editor
 */
@Composable
fun ImageEditorScreen(
    modifier: Modifier = Modifier,
    sourceUris: List<Uri>,
    initialRecipients: List<PickedContact>,
    pickerSources: PickerSources = PickerSources(numberContacts = true, groups = true, deviceContacts = true, usernameContacts = true),
    defaultDisappearSeconds: Int = 0,
    showDisappearButton: Boolean = true,
    onDone: (
        uris: List<Uri>,
        message: String,
        disappearSeconds: Int,
        recipients: List<PickedContact>
    ) -> Unit,
    onCancel: () -> Unit,
    vm: ImageEditorViewModel = viewModel(key = sourceUris.joinToString("|"))
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // The editor owns its ViewModel, so every opening starts clean.
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val vm: ImageEditorViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ImageEditorViewModel(context.applicationContext as Application) as T
            }
        }
    )
    LaunchedEffect(sourceUris) { vm.load(sourceUris, defaultDisappearSeconds, initialRecipients) }

    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    val preview = vm.preview

    var tool by remember { mutableStateOf(EditorTool.NONE) }
    var showOriginal by remember { mutableStateOf(false) }
    var brush by remember { mutableStateOf(BrushKind.PEN) }
    var brushColor by remember { mutableIntStateOf(-1) }
    var brushSize by remember { mutableFloatStateOf(0.012f) }
    var showTextDialog by remember { mutableStateOf(false) }
    var editingOverlay by remember { mutableStateOf<Overlay?>(null) }
    var lastPanel by remember { mutableStateOf(EditorTool.ADJUST) }
    var showDisappearPicker by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val isPanelTool = tool == EditorTool.ADJUST || tool == EditorTool.FILTERS ||
            tool == EditorTool.DRAW || tool == EditorTool.STICKERS
    if (isPanelTool) lastPanel = tool

    BackHandler {
        if (tool != EditorTool.NONE) tool = EditorTool.NONE else onCancel()
    }

    // Center of the visible crop frame, in normalized source coordinates (where new text/stickers land)
    fun frameCenter(): Offset {
        val bmp = preview ?: return Offset(0.5f, 0.5f)
        val sw = bmp.width.toFloat()
        val sh = bmp.height.toFloat()
        val cm = buildCropMatrix(state.crop, sw, sh)
        return ViewMapper(cm.matrix, sw, sh).toNorm(Offset(cm.outW / 2f, cm.outH / 2f))
    }

    fun send() {
        focusManager.clearFocus()
        if (vm.recipients.isEmpty()) { showPicker = true; return }
        vm.exportAll { files ->
            if (files != null) onDone(files, vm.caption.trim(), vm.disappearSeconds, vm.recipients)
            else Toast.makeText(context, "Couldn't export the images", Toast.LENGTH_SHORT).show()
        }
    }

    val thumb = remember(preview) {
        preview?.let { b ->
            val w = 160
            b.scale(w, (w * b.height / b.width.toFloat()).roundToInt().coerceAtLeast(1))
        }
    }

    Box(modifier.fillMaxSize().background(ui().bg).pointerInput(Unit) { detectTapGestures { } } ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            EditorTopBar(
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                onClose = onCancel,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onCompare = { showOriginal = it }
            )

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    preview != null -> EditorCanvas(
                        bitmap = preview,
                        state = state,
                        tool = tool,
                        showOriginal = showOriginal,
                        brush = brush,
                        brushColor = brushColor,
                        brushSize = brushSize,
                        vm = vm,
                        onEditText = { editingOverlay = it }
                    )
                    vm.loadFailed -> Text("Couldn't open this image", color = ui().text)
                    else -> CircularProgressIndicator(color = ui().text)
                }
            }

            if (vm.uris.size > 1) {
                ImageStrip(thumbs = vm.thumbs, selected = vm.index, onSelect = vm::select)
            }

            Column(Modifier.navigationBarsPadding().imePadding()) {
                AnimatedVisibility(
                    visible = isPanelTool,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    when (lastPanel) {
                        EditorTool.ADJUST -> AdjustPanel(
                            a = state.adjust,
                            onChange = vm::setAdjust,
                            onReset = vm::resetAdjust
                        )
                        EditorTool.FILTERS -> FiltersPanel(
                            thumb = thumb,
                            selected = state.filter,
                            strength = state.filterStrength,
                            onSelect = vm::setFilter,
                            onStrength = vm::setFilterStrength
                        )
                        EditorTool.DRAW -> DrawPanel(
                            brush = brush,
                            onBrush = { brush = it },
                            color = brushColor,
                            onColor = { brushColor = it },
                            size = brushSize,
                            onSize = { brushSize = it }
                        )
                        EditorTool.STICKERS -> StickerPanel { emoji ->
                            val c = frameCenter()
                            vm.addOverlay(
                                Overlay(
                                    text = emoji, colorArgb = -1, size = 0.14f,
                                    x = c.x, y = c.y, isSticker = true
                                )
                            )
                            tool = EditorTool.NONE
                        }
                        else -> {}
                    }
                }

                RecipientsBar(
                    recipients = vm.recipients,
                    onOpen = { showPicker = true },
                    onRemove = vm::removeRecipient
                )

                CaptionBar(
                    caption = vm.caption,
                    onCaption = { vm.caption = it },
                    enabled = !vm.isExporting && preview != null,
                    onSend = ::send,
                    showTimer = showDisappearButton,
                    timerActive = vm.disappearSeconds > 0,
                    onTimer = { showDisappearPicker = true },
                    hd = vm.hd,
                    onHd = { vm.hd = !vm.hd }
                )

                ToolDock(
                    selected = tool,
                    onSelect = { t ->
                        when (t) {
                            EditorTool.TEXT -> { tool = EditorTool.NONE; showTextDialog = true }
                            else -> tool = if (tool == t) EditorTool.NONE else t
                        }
                    }
                )
            }
        }

        if (tool == EditorTool.CROP && preview != null) {
            // Your adapted CropScreen (see CropScreen_PATCH.md)
            EditorCropScreen(
                source = preview,
                initial = state.crop,
                onClose = { tool = EditorTool.NONE },
                onApply = { vm.setCrop(it); tool = EditorTool.NONE }
            )
        }

        if (showTextDialog || editingOverlay != null) {
            TextEditDialog(
                initial = editingOverlay,
                onDismiss = { showTextDialog = false; editingOverlay = null },
                onConfirm = { text, color, size, font, bg ->
                    val target = editingOverlay
                    if (text.isNotBlank()) {
                        if (target != null) {
                            vm.updateOverlay(target.id, coalesceKey = null) {
                                it.copy(text = text, colorArgb = color, size = size, font = font, bg = bg)
                            }
                        } else {
                            val c = frameCenter()
                            vm.addOverlay(
                                Overlay(text = text, colorArgb = color, size = size, font = font, bg = bg, x = c.x, y = c.y)
                            )
                        }
                    }
                    showTextDialog = false
                    editingOverlay = null
                }
            )
        }

        if (showPicker) {
            ContactPickerScreen(
                modifier = Modifier.fillMaxSize(),
                sources = pickerSources,
                initialItems = vm.recipients,
                title = "Send to",
                onConfirm = { picked ->
                    if (picked.isNotEmpty()) vm.recipients = picked   // never end up with nobody
                    showPicker = false
                },
                onBack = { showPicker = false }
            )
        }

        if (showDisappearPicker) {
            DisappearingMessagesPickerDialog(
                currentSeconds = vm.disappearSeconds,
                onDismiss = { showDisappearPicker = false },
                onSelected = { seconds ->
                    vm.disappearSeconds = seconds
                    showDisappearPicker = false
                }
            )
        }

        if (vm.isExporting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xB3000000))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    val p = vm.exportProgress
                    Text(
                        if (p != null && p.second > 1) "Preparing image ${p.first} of ${p.second}…" else "Preparing image…",
                        color = Color.White
                    )
                }
            }
        }
    }
}

/* ================================================================== */
/* Canvas                                                              */
/* ================================================================== */

@Composable
private fun EditorCanvas(
    bitmap: Bitmap,
    state: EditorState,
    tool: EditorTool,
    showOriginal: Boolean,
    brush: BrushKind,
    brushColor: Int,
    brushSize: Float,
    vm: ImageEditorViewModel,
    onEditText: (Overlay) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val sw = bitmap.width.toFloat()
    val sh = bitmap.height.toFloat()

    val cm = remember(state.crop, bitmap) { buildCropMatrix(state.crop, sw, sh) }
    val colorFilter = remember(state.adjust, state.filter, state.filterStrength, showOriginal) {
        if (showOriginal) null
        else ColorMatrixColorFilter(buildColorMatrix(state.adjust, state.filter, state.filterStrength))
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val live = remember { mutableStateListOf<Offset>() }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var raw by remember { mutableStateOf(Offset.Zero) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }
    var overTrash by remember { mutableStateOf(false) }
    var trashRect by remember { mutableStateOf<Rect?>(null) }
    val rootCenters = remember { mutableMapOf<String, Offset>() }

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }
        val s = min(availW / cm.outW, availH / cm.outH)
        val viewW = cm.outW * s
        val viewH = cm.outH * s
        val mapper = remember(cm, s) {
            ViewMapper(Matrix(cm.matrix).apply { postScale(s, s) }, sw, sh)
        }

        Box(
            Modifier
                .size(with(density) { viewW.toDp() }, with(density) { viewH.toDp() })
                .align(Alignment.Center)
                .clipToBounds()
        ) {
            // 1. Photo (cropped + color adjusted)
            Canvas(Modifier.fillMaxSize()) {
                paint.colorFilter = colorFilter
                drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, mapper.matrix, paint) }
            }

            // 2. Drawing layer (offscreen so the eraser only erases strokes, not the photo)
            if (!showOriginal) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .pointerInput(tool, brush, brushColor, brushSize, mapper) {
                            if (tool == EditorTool.DRAW) {
                                detectDragGestures(
                                    onDragStart = { live.clear(); live.add(mapper.toNorm(it)) },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        live.add(mapper.toNorm(change.position))
                                    },
                                    onDragEnd = {
                                        if (live.size > 1) {
                                            val w = when (brush) {
                                                BrushKind.PEN -> brushSize
                                                BrushKind.MARKER -> brushSize * 2.2f
                                                BrushKind.ERASER -> brushSize * 1.6f
                                            }
                                            vm.addStroke(
                                                StrokeLayer(points = live.toList(), colorArgb = brushColor, width = w, kind = brush)
                                            )
                                        }
                                        live.clear()
                                    },
                                    onDragCancel = { live.clear() }
                                )
                            }
                        }
                ) {
                    state.strokes.forEach { drawEditorStroke(it.points, it.colorArgb, it.width, it.kind, mapper) }
                    if (live.size > 1) {
                        val w = when (brush) {
                            BrushKind.PEN -> brushSize
                            BrushKind.MARKER -> brushSize * 2.2f
                            BrushKind.ERASER -> brushSize * 1.6f
                        }
                        drawEditorStroke(live.toList(), brushColor, w, brush, mapper)
                    }
                }
            }

            // 3. Text + stickers
            if (!showOriginal) {
                val interactive = tool != EditorTool.DRAW && tool != EditorTool.CROP
                state.overlays.forEach { o ->
                    key(o.id) {
                        OverlayItem(
                            o = o,
                            mapper = mapper,
                            frameW = viewW,
                            interactive = interactive,
                            onStart = {
                                draggingId = o.id
                                raw = mapper.toView(o.x, o.y)
                            },
                            onMove = { pan, zoom, rot ->
                                raw += pan
                                var c = raw
                                val th = 10f
                                val sx = abs(c.x - viewW / 2f) < th
                                val sy = abs(c.y - viewH / 2f) < th
                                if (sx) c = c.copy(x = viewW / 2f)
                                if (sy) c = c.copy(y = viewH / 2f)
                                if ((sx && !snapX) || (sy && !snapY)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                snapX = sx
                                snapY = sy
                                val n = mapper.toNorm(c)
                                vm.updateOverlay(o.id) {
                                    it.copy(
                                        x = n.x,
                                        y = n.y,
                                        size = (it.size * zoom).coerceIn(0.02f, 0.6f),
                                        rotation = it.rotation + if (mapper.mirrored) -rot else rot
                                    )
                                }
                                val rc = rootCenters[o.id]
                                overTrash = rc != null && trashRect?.contains(rc) == true
                            },
                            onEnd = {
                                if (overTrash) vm.removeOverlay(o.id)
                                draggingId = null
                                overTrash = false
                                snapX = false
                                snapY = false
                            },
                            onTap = { if (!o.isSticker) onEditText(o) },
                            onBounds = { rootCenters[o.id] = it.center }
                        )
                    }
                }
            }

            // 4. Snap guides
            if (snapX || snapY) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = Color.White.copy(alpha = 0.6f)
                    if (snapX) drawLine(c, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
                    if (snapY) drawLine(c, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.dp.toPx())
                }
            }
        }

        // Drag-to-delete zone
        AnimatedVisibility(
            visible = draggingId != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        ) {
            Box(
                Modifier
                    .size(if (overTrash) 64.dp else 52.dp)
                    .onGloballyPositioned { trashRect = it.boundsInRoot() }
                    .clip(CircleShape)
                    .background(if (overTrash) Color(0xFFE53935) else Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }
    }
}

fun DrawScope.drawEditorStroke(
    points: List<Offset>,
    argb: Int,
    width: Float,
    kind: BrushKind,
    mapper: ViewMapper
) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { i, p ->
        val o = mapper.toView(p.x, p.y)
        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    val color = when (kind) {
        BrushKind.ERASER -> Color.Black
        BrushKind.MARKER -> Color(argb).copy(alpha = 0.45f)
        BrushKind.PEN -> Color(argb)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width * mapper.srcW * mapper.unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (kind == BrushKind.ERASER) BlendMode.Clear else BlendMode.SrcOver
    )
}

/* ================================================================== */
/* Text / sticker overlay                                              */
/* ================================================================== */

@Composable
fun OverlayItem(
    o: Overlay,
    mapper: ViewMapper,
    frameW: Float,
    interactive: Boolean,
    onStart: () -> Unit,
    onMove: (pan: Offset, zoom: Float, rot: Float) -> Unit,
    onEnd: () -> Unit,
    onTap: () -> Unit,
    onBounds: (Rect) -> Unit
) {
    val density = LocalDensity.current
    val center = mapper.toView(o.x, o.y)
    val fontPx = o.size * mapper.srcW * mapper.unit
    val (fx, fy) = o.pad()
    val shape = RoundedCornerShape(with(density) { (0.4f * fontPx).toDp() })
    val angle = mapper.angleOf(o.rotation)

    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    val tap by rememberUpdatedState(onTap)

    val bgArgb = o.bgArgb()
    val bgModifier = if (bgArgb != null) Modifier.background(Color(bgArgb), shape) else Modifier

    Box(
        Modifier
            .layout { measurable, c ->
                val p = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) {
                    p.place(
                        (center.x - p.width / 2f).roundToInt(),
                        (center.y - p.height / 2f).roundToInt()
                    )
                }
            }
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .then(
                if (interactive) Modifier.pointerInput(o.id) {
                    overlayGestures(
                        onStart = { start() },
                        onTransform = { pan, zoom, rot -> move(pan, zoom, rot) },
                        onEnd = { end() },
                        onTap = { tap() }
                    )
                } else Modifier
            )
            .graphicsLayer { rotationZ = angle }
            .then(bgModifier)
            .padding(
                horizontal = with(density) { (fx * fontPx).toDp() },
                vertical = with(density) { (fy * fontPx).toDp() }
            )
    ) {
        Text(
            text = o.text,
            color = Color(o.textColorArgb()),
            fontSize = with(density) { fontPx.toSp() },
            fontFamily = o.font.family,
            fontWeight = FontWeight.Bold,
            style = TextStyle(),
            modifier = Modifier.widthIn(max = with(density) { (frameW * 0.9f).toDp() })
        )
    }
}

/** One-finger drag + two-finger pinch/rotate, with proper start/end callbacks and tap detection. */
private suspend fun PointerInputScope.overlayGestures(
    onStart: () -> Unit,
    onTransform: (pan: Offset, zoom: Float, rotation: Float) -> Unit,
    onEnd: () -> Unit,
    onTap: () -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onStart()
        var moved = 0f
        do {
            val event = awaitPointerEvent()
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            val rot = event.calculateRotation()
            if (pan != Offset.Zero || zoom != 1f || rot != 0f) {
                moved += pan.getDistance()
                onTransform(pan, zoom, rot)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (moved < viewConfiguration.touchSlop) onTap()
        onEnd()
    }
}

/* ================================================================== */
/* Top bar, dock, caption                                              */
/* ================================================================== */

@Composable
private fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCompare: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoundIcon(Icons.Default.Close, "Close", true, onClose)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.Undo, "Undo", canUndo, onUndo)
            RoundIcon(Icons.AutoMirrored.Filled.Redo, "Redo", canRedo, onRedo)
            // Hold to compare with the original (no color changes, no overlays)
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ui().text.copy(alpha = 0.08f))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            onCompare(true)
                            tryAwaitRelease()
                            onCompare(false)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Compare, "Hold to compare", tint = ui().text, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.size(42.dp)) // balances the close button so undo/redo stay centered
    }
}

@Composable
fun RoundIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp).clip(CircleShape).background(ui().text.copy(alpha = 0.08f))
    ) {
        Icon(
            icon, desc,
            tint = ui().text.copy(alpha = if (enabled) 1f else 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CaptionBar(
    caption: String,
    onCaption: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
    showTimer: Boolean,
    timerActive: Boolean,
    onTimer: () -> Unit,
    showHd: Boolean = false,
    hd: Boolean = false,
    onHd: () -> Unit = {}
) {
    val fieldBg = ui().text.copy(alpha = 0.08f)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            value = caption,
            onValueChange = onCaption,
            placeholder = { Text("Add a caption…") },
            maxLines = 4,
            shape = RoundedCornerShape(26.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = ui().text,
                unfocusedTextColor = ui().text,
                cursorColor = Accent,
                focusedPlaceholderColor = ui().text.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = ui().text.copy(alpha = 0.5f)
            ),
            modifier = Modifier.weight(1f)
        )
        if (showHd) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (hd) Accent.copy(alpha = 0.25f) else ui().text.copy(alpha = 0.08f))
                    .clickable(onClick = onHd),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "HD",
                    color = if (hd) Accent else ui().text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        if (showTimer) {
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onTimer,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (timerActive) Accent.copy(alpha = 0.25f) else ui().text.copy(alpha = 0.08f))
            ) {
                Icon(
                    Icons.Outlined.AvTimer, "Disappearing messages",
                    tint = if (timerActive) Accent else ui().text
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier.size(54.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}

private data class DockItem(val tool: EditorTool, val label: String, val icon: ImageVector)

private val DockItems = listOf(
    DockItem(EditorTool.CROP, "Crop", Icons.Default.Crop),
    DockItem(EditorTool.ADJUST, "Adjust", Icons.Default.Tune),
    DockItem(EditorTool.FILTERS, "Filters", Icons.Default.PhotoFilter),
    DockItem(EditorTool.DRAW, "Draw", Icons.Default.Brush),
    DockItem(EditorTool.TEXT, "Text", Icons.Default.TextFields),
    DockItem(EditorTool.STICKERS, "Stickers", Icons.Default.EmojiEmotions)
)

@Composable
private fun ToolDock(selected: EditorTool, onSelect: (EditorTool) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DockItems.forEach { item ->
            val active = selected == item.tool
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onSelect(item.tool)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, item.label, tint = if (active) Accent else ui().text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(2.dp))
                Text(item.label, color = if (active) Accent else ui().text.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

/* ================================================================== */
/* Tool panels                                                         */
/* ================================================================== */

@Composable
fun PanelSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ui().panel)
            .border(1.dp, ui().text.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Accent,
    activeTrackColor = Accent,
    inactiveTrackColor = ui().text.copy(alpha = 0.2f)
)

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Accent else ui().text.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = ui().text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ColorRow(selected: Int, onPick: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        items(Palette) { argb ->
            val isSel = argb == selected
            Box(
                Modifier
                    .size(if (isSel) 34.dp else 28.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) ui().text else ui().text.copy(alpha = 0.3f), CircleShape)
                    .clickable { onPick(argb) }
            )
        }
    }
}

@Composable
private fun AdjustPanel(a: Adjustments, onChange: (Adjustments) -> Unit, onReset: () -> Unit) {
    val names = listOf("Brightness", "Contrast", "Saturation", "Warmth")
    var idx by remember { mutableIntStateOf(0) }
    val value = when (idx) { 0 -> a.brightness; 1 -> a.contrast; 2 -> a.saturation; else -> a.warmth }
    PanelSurface {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(names.indices.toList()) { i -> Pill(names[i], idx == i) { idx = i } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = value,
                onValueChange = { v ->
                    onChange(
                        when (idx) {
                            0 -> a.copy(brightness = v)
                            1 -> a.copy(contrast = v)
                            2 -> a.copy(saturation = v)
                            else -> a.copy(warmth = v)
                        }
                    )
                },
                valueRange = -1f..1f,
                colors = sliderColors(),
                modifier = Modifier.weight(1f)
            )
            Text("${(value * 100).roundToInt()}", color = ui().text, modifier = Modifier.width(40.dp))
            TextButton(onClick = onReset) { Text("Reset", color = Accent) }
        }
    }
}

@Composable
private fun FiltersPanel(
    thumb: Bitmap?,
    selected: FilterPreset,
    strength: Float,
    onSelect: (FilterPreset) -> Unit,
    onStrength: (Float) -> Unit
) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    PanelSurface {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(FilterPreset.entries.toList()) { preset ->
                val isSel = preset == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelect(preset) }
                ) {
                    Canvas(
                        Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(if (isSel) 2.dp else 1.dp, if (isSel) Accent else ui().text.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        if (thumb != null) {
                            val k = maxOf(size.width / thumb.width, size.height / thumb.height)
                            val m = Matrix().apply {
                                postScale(k, k)
                                postTranslate((size.width - thumb.width * k) / 2f, (size.height - thumb.height * k) / 2f)
                            }
                            paint.colorFilter = ColorMatrixColorFilter(buildColorMatrix(Adjustments(), preset, 1f))
                            drawIntoCanvas { it.nativeCanvas.drawBitmap(thumb, m, paint) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(preset.label, color = if (isSel) Accent else ui().text.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        if (selected != FilterPreset.NONE) {
            Slider(
                value = strength,
                onValueChange = onStrength,
                valueRange = 0f..1f,
                colors = sliderColors()
            )
        }
    }
}

@Composable
fun DrawPanel(
    brush: BrushKind,
    onBrush: (BrushKind) -> Unit,
    color: Int,
    onColor: (Int) -> Unit,
    size: Float,
    onSize: (Float) -> Unit
) {
    PanelSurface {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Pen", brush == BrushKind.PEN) { onBrush(BrushKind.PEN) }
            Pill("Marker", brush == BrushKind.MARKER) { onBrush(BrushKind.MARKER) }
            Pill("Eraser", brush == BrushKind.ERASER) { onBrush(BrushKind.ERASER) }
        }
        Slider(value = size, onValueChange = onSize, valueRange = 0.004f..0.04f, colors = sliderColors())
        if (brush != BrushKind.ERASER) ColorRow(color, onColor)
    }
}

@Composable
fun StickerPanel(onPick: (String) -> Unit) {
    PanelSurface {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            items(Emojis) { e ->
                Text(e, fontSize = 32.sp, modifier = Modifier.clickable { onPick(e) })
            }
        }
    }
}

/* ================================================================== */
/* Text dialog                                                         */
/* ================================================================== */

@Composable
fun TextEditDialog(
    initial: Overlay?,
    onDismiss: () -> Unit,
    onConfirm: (text: String, color: Int, size: Float, font: EditorFont, bg: TextBg) -> Unit
) {
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var color by remember { mutableIntStateOf(initial?.colorArgb ?: -1) }
    var size by remember { mutableFloatStateOf(initial?.size ?: 0.07f) }
    var font by remember { mutableStateOf(initial?.font ?: EditorFont.STANDARD) }
    var bg by remember { mutableStateOf(initial?.bg ?: TextBg.GLASS) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ui().panel,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (bg) {
                            TextBg.SOLID -> "Background: Solid"
                            TextBg.GLASS -> "Background: Glass"
                            TextBg.NONE -> "Background: None"
                        },
                        color = ui().text.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    IconButton(
                        onClick = {
                            bg = when (bg) {
                                TextBg.GLASS -> TextBg.SOLID
                                TextBg.SOLID -> TextBg.NONE
                                TextBg.NONE -> TextBg.GLASS
                            }
                        },
                        modifier = Modifier.clip(CircleShape).background(ui().text.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.TextFields, "Cycle background",
                            tint = if (bg != TextBg.NONE) Color(0xFFFFD700) else ui().text
                        )
                    }
                }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type something…", color = Color.Gray) },
                    textStyle = TextStyle(
                        color = Color(color),
                        fontSize = (size * 320f).sp,
                        fontFamily = font.family,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3A3A40),
                        unfocusedContainerColor = Color(0xFF3A3A40),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Accent
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", color = ui().text.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Slider(
                        value = size,
                        onValueChange = { size = it },
                        valueRange = 0.03f..0.15f,
                        colors = sliderColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EditorFont.entries.toList()) { f -> Pill(f.label, f == font) { font = f } }
                }
                Spacer(Modifier.height(12.dp))
                ColorRow(color) { color = it }
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = ui().text.copy(alpha = 0.7f)) }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { onConfirm(text, color, size, font, bg) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent),
                        modifier = Modifier.width(88.dp).height(40.dp)
                    ) { Text("Done", color = Color.White) }
                }
            }
        }
    }
}
@Composable
fun ImageStrip(thumbs: List<Bitmap?>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(thumbs) { i, bmp ->
            val sel = i == selected
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ui().text.copy(alpha = 0.08f))
                    .border(
                        if (sel) 2.dp else 1.dp,
                        if (sel) Accent else ui().text.copy(alpha = 0.2f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(i) }
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun RecipientsBar(
    recipients: List<PickedContact>,
    onOpen: () -> Unit,
    onRemove: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("To", color = ui().text.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(recipients, key = { it.userNumber }) { r ->
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ui().text.copy(alpha = 0.1f))
                        .clickable(onClick = onOpen)
                        .padding(start = 12.dp, end = if (recipients.size > 1) 4.dp else 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        r.displayName, color = ui().text, fontSize = 13.sp,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    if (recipients.size > 1) {
                        Icon(
                            Icons.Default.Close, "Remove ${r.displayName}",
                            tint = ui().text.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onRemove(r.userNumber) }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.Add, "Add recipients", tint = Accent)
        }
    }
}