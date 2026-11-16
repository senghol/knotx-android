package com.senghol.knotx.editor

import android.graphics.Matrix
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.abs
import kotlin.math.min

/**
 * Video preview + drawing / text / sticker layers.
 * The frame is drawn at exactly the video's aspect ratio, so normalized overlay coordinates
 * map 1:1 onto the exported frame (see renderOverlayBitmap).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun VideoCanvas(
    player: ExoPlayer,
    clip: VideoClip,
    tool: EditorTool,
    isPlaying: Boolean,
    brush: BrushKind,
    brushColor: Int,
    brushSize: Float,
    vm: VideoEditorViewModel,
    onTogglePlay: () -> Unit,
    onEditText: (Overlay) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val exo = player
    val sw = clip.width.toFloat().coerceAtLeast(1f)
    val sh = clip.height.toFloat().coerceAtLeast(1f)

    val live = remember { mutableStateListOf<Offset>() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var raw by remember { mutableStateOf(Offset.Zero) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }
    var overTrash by remember { mutableStateOf(false) }
    var trashRect by remember { mutableStateOf<Rect?>(null) }
    val rootCenters = remember { mutableMapOf<String, Offset>() }

    fun widthOf(kind: BrushKind) = when (kind) {
        BrushKind.PEN -> brushSize
        BrushKind.MARKER -> brushSize * 2.2f
        BrushKind.ERASER -> brushSize * 1.6f
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availW = with(density) { maxWidth.toPx() }
        val availH = with(density) { maxHeight.toPx() }
        val s = min(availW / sw, availH / sh)
        val viewW = sw * s
        val viewH = sh * s
        // No crop in the video editor, so the "crop matrix" is just identity scaled to the view.
        val mapper = remember(s, sw, sh) { ViewMapper(Matrix().apply { postScale(s, s) }, sw, sh) }

        Box(
            Modifier
                .size(with(density) { viewW.toDp() }, with(density) { viewH.toDp() })
                .align(Alignment.Center)
                .clipToBounds()
        ) {
            // 1. Video
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        this.player = exo
                    }
                }
            )

            // Tap = play / pause (not while drawing; sits below the layers that need touches)
            if (tool != EditorTool.DRAW) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onTogglePlay()
                        }
                )
                if (!isPlaying) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // 2. Drawing layer (offscreen so the eraser only erases strokes)
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .then(
                        if (tool == EditorTool.DRAW) Modifier.pointerInput(brush, brushColor, brushSize, mapper) {
                            detectDragGestures(
                                onDragStart = { live.clear(); live.add(mapper.toNorm(it)) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    live.add(mapper.toNorm(change.position))
                                },
                                onDragEnd = {
                                    if (live.size > 1) {
                                        vm.addStroke(
                                            StrokeLayer(
                                                points = live.toList(),
                                                colorArgb = brushColor,
                                                width = widthOf(brush),
                                                kind = brush
                                            )
                                        )
                                    }
                                    live.clear()
                                },
                                onDragCancel = { live.clear() }
                            )
                        } else Modifier
                    )
            ) {
                clip.strokes.forEach { drawEditorStroke(it.points, it.colorArgb, it.width, it.kind, mapper) }
                if (live.size > 1) {
                    drawEditorStroke(live.toList(), brushColor, widthOf(brush), brush, mapper)
                }
            }

            // 3. Text + stickers
            val interactive = tool != EditorTool.DRAW
            clip.overlays.forEach { o ->
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

/* ================================================================== */
/* Tool dock                                                           */
/* ================================================================== */

private data class VDockItem(val tool: EditorTool, val label: String, val icon: ImageVector)

private val VDockItems = listOf(
    VDockItem(EditorTool.CROP, "Trim", Icons.Default.ContentCut),   // CROP slot is used as "Trim" here
    VDockItem(EditorTool.DRAW, "Draw", Icons.Default.Brush),
    VDockItem(EditorTool.TEXT, "Text", Icons.Default.TextFields),
    VDockItem(EditorTool.STICKERS, "Stickers", Icons.Default.EmojiEmotions)
)

@Composable
internal fun VideoToolDock(selected: EditorTool, onSelect: (EditorTool) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        VDockItems.forEach { item ->
            val active = selected == item.tool
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onSelect(item.tool)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(item.icon, item.label, tint = if (active) Accent else ui().text, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(2.dp))
                Text(item.label, color = if (active) Accent else ui().text.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}