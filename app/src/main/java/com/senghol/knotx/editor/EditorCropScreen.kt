package com.senghol.knotx.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.ui.draw.clipToBounds

import android.util.DisplayMetrics

import android.view.WindowManager
import androidx.compose.foundation.shape.CircleShape
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalView

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.shadow
import kotlin.math.cos
import kotlin.math.sin
/** Ratio = width / height. Null means freeform — a fully resizable rectangle. */
enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    LANDSCAPE_5_4("5:4", 5f / 4f),
    STORY_9_16("9:16", 9f / 16f),
    WIDE_16_9("16:9", 16f / 9f),
}

private enum class HandleType { TL, TR, BL, BR, T, B, L, R }

/** Screen width / height, excluding the navigation bar (and any side nav bar). */
fun deviceWallpaperRatio(context: Context, view: View): Float {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val fullW: Int
    val fullH: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val b = wm.maximumWindowMetrics.bounds
        fullW = b.width(); fullH = b.height()
    } else {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        fullW = dm.widthPixels; fullH = dm.heightPixels
    }
    val nav = ViewCompat.getRootWindowInsets(view)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
    val w = (fullW - (nav?.left ?: 0) - (nav?.right ?: 0)).coerceAtLeast(1)
    val h = (fullH - (nav?.bottom ?: 0)).coerceAtLeast(1)
    return w.toFloat() / h.toFloat()
}
/**
 * Full-screen crop tool.
 * - Locked aspect ratios: frame is fixed, pinch-zoom / drag to pan the image underneath.
 * - Free: image is static (contain-fit), the frame itself is a resizable rectangle —
 *   4 corner handles (both axes) + 4 edge handles (one axis) + drag-to-move from the middle.
 *
 * implementation("androidx.exifinterface:exifinterface:1.3.7")
 */


@Composable
fun EditorCropScreen(
    source: Bitmap,
    initial: CropState,
    squareOnly: Boolean = false,
    lockedRatio: Float? = null,   // if set, this exact ratio is forced and the chips are hidden
    onClose: () -> Unit,
    onApply: (CropState) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val squareAspect = remember { CropAspect.entries.first { it.ratio == 1f } }
    var aspect by remember { mutableStateOf(if (squareOnly) squareAspect else CropAspect.FREE) }
    var customRatio by remember { mutableStateOf<Float?>(null) }
    var customLabel by remember { mutableStateOf<String?>(null) }
    var showCustomDialog by remember { mutableStateOf(false) }
    val activeRatio: Float? = lockedRatio ?: customRatio ?: aspect.ratio   // null = free

    var freeRect by remember { mutableStateOf<Rect?>(null) }
    var freeZoom by remember { mutableFloatStateOf(1f) }
    var freePan by remember { mutableStateOf(Offset.Zero) }

    var isInteracting by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    var frameWidthPxState by remember { mutableFloatStateOf(0f) }
    var frameHeightPxState by remember { mutableFloatStateOf(0f) }
    var totalScaleState by remember { mutableFloatStateOf(1f) }
    var effectivePanState by remember { mutableStateOf(Offset.Zero) }

    var steps by remember { mutableIntStateOf(initial.steps) }
    var flipH by remember { mutableStateOf(initial.flipH) }
    val workingBitmap: Bitmap = remember(source, steps, flipH) { transformBase(source, steps, flipH) }

    var straightenDegrees by remember { mutableFloatStateOf(initial.straighten) }

    LaunchedEffect(interactionTick) {
        if (interactionTick > 0) {
            isInteracting = true
            delay(500)
            isInteracting = false
        }
    }

    fun resetTransform() {
        freeRect = null          // re-fitted on next layout pass
        freeZoom = 1f
        freePan = Offset.Zero
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ---- Top bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text("Crop", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(onClick = { resetTransform(); straightenDegrees = 0f }) {
                    Text("Reset", color = Color.White)
                }
                FilledIconButton(
                    onClick = {
                        val unit = workingBitmap.width.toFloat()
                        val k = totalScaleState
                        if (k > 0f && frameWidthPxState > 0f) {
                            onApply(
                                CropState(
                                    steps = steps,
                                    flipH = flipH,
                                    straighten = straightenDegrees,
                                    frameW = frameWidthPxState / k / unit,
                                    frameH = frameHeightPxState / k / unit,
                                    panX = effectivePanState.x / k / unit,
                                    panY = effectivePanState.y / k / unit
                                )
                            )
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                }
            }
        }

        // ---- Crop area (clipped so the rotated image never spills out) ----
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val bmp = workingBitmap
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val areaWidthPx = with(density) { maxWidth.toPx() }
                val areaHeightPx = with(density) { maxHeight.toPx() }
                val paddingPx = with(density) { 20.dp.toPx() }
                val availW = areaWidthPx - paddingPx * 2
                val availH = areaHeightPx - paddingPx * 2
                val fillFraction = 0.8f                 // lower = more spare image around the frame
                val targetW = availW * fillFraction
                val targetH = availH * fillFraction
                val viewportCenter = Offset(areaWidthPx / 2f, areaHeightPx / 2f)
                val viewportRect = Rect(0f, 0f, areaWidthPx, areaHeightPx)
                val fitScale = min(availW / bmp.width, availH / bmp.height)
                val minSizePx = with(density) { 48.dp.toPx() }
                val gridAlpha by animateFloatAsState(if (isInteracting) 0.7f else 0f, label = "grid")

                // Layout size can change after the first frame (dialog insets settling).
                // Re-fit when it does, otherwise the frame is stuck outside the valid area.
                var lastArea by remember { mutableStateOf(Offset.Zero) }
                if (lastArea != Offset(areaWidthPx, areaHeightPx)) {
                    lastArea = Offset(areaWidthPx, areaHeightPx)
                    freeRect = null
                    freeZoom = 1f
                    freePan = Offset.Zero
                }

                // ---- Initial / reset frame: biggest frame of the active ratio that fits
                //      inside the (rotated) image, then zoomed to fill the viewport ----
                if (freeRect == null) {
                    val w = bmp.width * fitScale
                    val h = bmp.height * fitScale
                    val a = Math.toRadians(straightenDegrees.toDouble())
                    val ac = abs(cos(a)).toFloat()
                    val asn = abs(sin(a)).toFloat()
                    val r = activeRatio ?: (w / h)
                    val w0 = min(w / (ac + asn / r), h / (asn + ac / r))
                    val h0 = w0 / r
                    val m = min(targetW / w0, targetH / h0).coerceIn(0.1f, 10f)
                    freeZoom = m
                    freePan = Offset.Zero
                    freeRect = Rect(
                        viewportCenter.x - w0 * m / 2f, viewportCenter.y - h0 * m / 2f,
                        viewportCenter.x + w0 * m / 2f, viewportCenter.y + h0 * m / 2f
                    )
                }
                val rect = freeRect ?: return@BoxWithConstraints
                val imageCenter = viewportCenter + freePan

                totalScaleState = fitScale * freeZoom
                frameWidthPxState = rect.width
                frameHeightPxState = rect.height
                effectivePanState = imageCenter - rect.center

                // True if all 4 corners of r lie inside the ROTATED image.
                fun frameFits(r: Rect, zoom: Float, pan: Offset, deg: Float): Boolean {
                    val k = fitScale * zoom
                    val halfW = bmp.width * k / 2f + 1.5f
                    val halfH = bmp.height * k / 2f + 1.5f
                    val a = Math.toRadians(deg.toDouble())
                    val c = cos(a).toFloat()
                    val s = sin(a).toFloat()
                    val center = viewportCenter + pan
                    return listOf(r.topLeft, r.topRight, r.bottomLeft, r.bottomRight).all { p ->
                        val dx = p.x - center.x
                        val dy = p.y - center.y
                        abs(dx * c + dy * s) <= halfW && abs(-dx * s + dy * c) <= halfH
                    }
                }

                // Inside rotated image AND inside the visible area. Reads live state.
                fun frameOk(r: Rect): Boolean =
                    frameFits(r, freeZoom, freePan, straightenDegrees) &&
                            r.left >= -1.5f && r.top >= -1.5f &&
                            r.right <= areaWidthPx + 1.5f && r.bottom <= areaHeightPx + 1.5f

                fun inViewport(r: Rect) =
                    r.left >= -1.5f && r.top >= -1.5f &&
                            r.right <= areaWidthPx + 1.5f && r.bottom <= areaHeightPx + 1.5f

                fun frameOkFor(r: Rect, strictViewport: Boolean): Boolean =
                    frameFits(r, freeZoom, freePan, straightenDegrees) && (!strictViewport || inViewport(r))

                // Moves from current toward proposed as far as it stays valid.
                fun constrainFrame(current: Rect, proposed: Rect): Rect {
                    // After a free-look pan the frame may sit partly off-screen; only enforce
                    // the viewport limit if it is currently inside it.
                    val strict = inViewport(current)
                    if (frameOkFor(proposed, strict)) return proposed
                    var lo = 0f
                    var hi = 1f
                    repeat(10) {
                        val mid = (lo + hi) / 2f
                        if (frameOkFor(lerpRect(current, proposed, mid), strict)) lo = mid else hi = mid
                    }
                    return lerpRect(current, proposed, lo)
                }

                // After release: zoom so the selection fills the viewport.
                fun autoZoomToSelection() {
                    val current = freeRect ?: return
                    if (availW <= 0f || availH <= 0f || current.width <= 0f || current.height <= 0f) return
                    val zoomStart = freeZoom
                    val zoomTarget = (zoomStart * min(targetW / current.width, targetH / current.height)).coerceIn(0.1f, 10f)
                    val multiplier = zoomTarget / zoomStart
                    if (abs(multiplier - 1f) < 0.01f) return

                    val panStart = freePan
                    val imageCenterNow = viewportCenter + freePan
                    val rectCenterAfterScale = imageCenterNow + (current.center - imageCenterNow) * multiplier
                    val panTarget = freePan + (viewportCenter - rectCenterAfterScale)
                    val nw = current.width * multiplier
                    val nh = current.height * multiplier
                    val rectTarget = Rect(
                        viewportCenter.x - nw / 2f, viewportCenter.y - nh / 2f,
                        viewportCenter.x + nw / 2f, viewportCenter.y + nh / 2f
                    )
                    scope.launch {
                        Animatable(0f).animateTo(1f, tween(250)) {
                            val t = value
                            freeZoom = zoomStart + (zoomTarget - zoomStart) * t
                            freePan = Offset(
                                panStart.x + (panTarget.x - panStart.x) * t,
                                panStart.y + (panTarget.y - panStart.y) * t
                            )
                            freeRect = lerpRect(current, rectTarget, t)
                        }
                    }
                }

                // Always-latest handlers (handle composables keep their first lambda otherwise)
                val moveBy by rememberUpdatedState<(Offset) -> Unit>({ drag ->
                    freeRect?.let { cur ->
                        val x = constrainFrame(cur, cur.translate(drag.x, 0f))
                        freeRect = constrainFrame(x, x.translate(0f, drag.y))
                        interactionTick++
                    }
                })
                val resizeBy by rememberUpdatedState<(HandleType, Offset) -> Unit>({ type, drag ->
                    freeRect?.let { cur ->
                        val proposed = if (activeRatio != null)
                            resizeKeepRatio(cur, type, drag, activeRatio, minSizePx)
                        else
                            applyHandleDrag(cur, type, drag, viewportRect, minSizePx)
                        freeRect = constrainFrame(cur, proposed)
                        interactionTick++
                    }
                })
                val settle by rememberUpdatedState<() -> Unit>({ autoZoomToSelection() })

                // Free look: move image + frame together, limited by the image edges.
                val panBy by rememberUpdatedState<(Offset) -> Unit>({ d ->
                    val r = freeRect
                    if (r != null) {
                        val k = fitScale * freeZoom
                        val a = Math.toRadians(straightenDegrees.toDouble())
                        val ac = abs(cos(a)).toFloat()
                        val asn = abs(sin(a)).toFloat()
                        val halfW = bmp.width * k / 2f
                        val halfH = bmp.height * k / 2f
                        val boxHalfW = halfW * ac + halfH * asn   // rotated image bounding box
                        val boxHalfH = halfW * asn + halfH * ac
                        val center = viewportCenter + freePan

                        fun clampAxis(c: Float, half: Float, area: Float, delta: Float): Float =
                            if (half * 2f <= area) 0f
                            else (c + delta).coerceIn(area - half, half) - c

                        var dx = clampAxis(center.x, boxHalfW, areaWidthPx, d.x)
                        var dy = clampAxis(center.y, boxHalfH, areaHeightPx, d.y)
                        // keep the frame's center on screen so it can't be lost
                        dx = (r.center.x + dx).coerceIn(0f, areaWidthPx) - r.center.x
                        dy = (r.center.y + dy).coerceIn(0f, areaHeightPx) - r.center.y

                        freePan += Offset(dx, dy)
                        freeRect = r.translate(dx, dy)
                        interactionTick++
                    }
                })

                // Rotating: if the frame would show blank corners, zoom the image in just enough.
                LaunchedEffect(straightenDegrees) {
                    val r = freeRect ?: return@LaunchedEffect
                    if (frameFits(r, freeZoom, freePan, straightenDegrees)) return@LaunchedEffect
                    val z0 = freeZoom
                    val c0 = viewportCenter + freePan
                    val p = r.center
                    fun panFor(m: Float) = (p + (c0 - p) * m) - viewportCenter
                    val mMax = 10f / z0
                    if (mMax <= 1f || !frameFits(r, z0 * mMax, panFor(mMax), straightenDegrees)) return@LaunchedEffect
                    var lo = 1f
                    var hi = mMax
                    repeat(14) {
                        val mid = (lo + hi) / 2f
                        if (frameFits(r, z0 * mid, panFor(mid), straightenDegrees)) hi = mid else lo = mid
                    }
                    freeZoom = z0 * hi
                    freePan = panFor(hi)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                panBy(drag)
                            }
                        }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .requiredSize(
                                with(density) { bmp.width.toDp() },
                                with(density) { bmp.height.toDp() }
                            )
                            .graphicsLayer(
                                scaleX = fitScale * freeZoom,
                                scaleY = fitScale * freeZoom,
                                rotationZ = straightenDegrees,
                                translationX = freePan.x,
                                translationY = freePan.y
                            )
                    )

                    CropScrimAndGrid(
                        frameTopLeft = rect.topLeft,
                        frameSize = Size(rect.width, rect.height),
                        gridAlpha = gridAlpha
                    )

                    // Drag inside the frame to move it (works for every ratio)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                            .size(
                                with(density) { rect.width.toDp() },
                                with(density) { rect.height.toDp() }
                            )
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = { settle() },
                                    onDragCancel = { settle() }
                                ) { change, drag ->
                                    change.consume()
                                    moveBy(drag)
                                }
                            }
                    )

                    // Corner handles (ratio is kept when a ratio is active)
                    listOf(
                        HandleType.TL to rect.topLeft,
                        HandleType.TR to Offset(rect.right, rect.top),
                        HandleType.BL to Offset(rect.left, rect.bottom),
                        HandleType.BR to Offset(rect.right, rect.bottom)
                    ).forEach { (type, pos) ->
                        CornerHandle(
                            x = pos.x, y = pos.y, density = density,
                            onDragEnd = { settle() }
                        ) { drag -> resizeBy(type, drag) }
                    }

                    // Edge handles only make sense for Free
                    if (activeRatio == null) {
                        EdgeHandle(rect.center.x, rect.top, vertical = false, density, onDragEnd = { settle() }) { drag -> resizeBy(HandleType.T, drag) }
                        EdgeHandle(rect.center.x, rect.bottom, vertical = false, density, onDragEnd = { settle() }) { drag -> resizeBy(HandleType.B, drag) }
                        EdgeHandle(rect.left, rect.center.y, vertical = true, density, onDragEnd = { settle() }) { drag -> resizeBy(HandleType.L, drag) }
                        EdgeHandle(rect.right, rect.center.y, vertical = true, density, onDragEnd = { settle() }) { drag -> resizeBy(HandleType.R, drag) }
                    }
                }
            }
        }

        // ---- Straighten slider ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${straightenDegrees.toInt()}\u00B0", color = Color.White, modifier = Modifier.width(44.dp))
            Slider(
                value = straightenDegrees,
                onValueChange = { straightenDegrees = it; interactionTick++ },
                valueRange = -45f..45f,
                modifier = Modifier.weight(1f)
            )
        }

        // ---- Rotate / flip / aspect ratio controls ----
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                steps = if (flipH) (steps + 1) % 4 else (steps + 3) % 4
                resetTransform()
            }) { Icon(Icons.Default.Rotate90DegreesCcw, "Rotate", tint = Color.White) }

            IconButton(onClick = { flipH = !flipH; resetTransform() }) {
                Icon(Icons.Default.Flip, "Flip", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))

            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = Color.White.copy(alpha = 0.12f),
                labelColor = Color.White,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!squareOnly && lockedRatio == null) {
                    items(CropAspect.entries) { option ->
                        FilterChip(
                            selected = customRatio == null && aspect == option,
                            onClick = {
                                customRatio = null
                                customLabel = null
                                aspect = option
                                resetTransform()
                            },
                            label = { Text(option.label) },
                            colors = chipColors
                        )
                    }
                    item {
                        FilterChip(
                            selected = customRatio != null,
                            onClick = { showCustomDialog = true },
                            label = { Text(customLabel ?: "Custom") },
                            colors = chipColors
                        )
                    }
                }
            }
        }
    }

    // ---- Custom ratio dialog ----
    if (showCustomDialog) {
        var wTxt by remember { mutableStateOf("") }
        var hTxt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom ratio") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = wTxt,
                        onValueChange = { wTxt = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("W") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Text("  :  ")
                    OutlinedTextField(
                        value = hTxt,
                        onValueChange = { hTxt = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("H") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = wTxt.toFloatOrNull()
                    val h = hTxt.toFloatOrNull()
                    if (w != null && h != null && w > 0f && h > 0f) {
                        customRatio = (w / h).coerceIn(0.005f, 200f)
                        customLabel = "$wTxt:$hTxt"
                        resetTransform()
                        showCustomDialog = false
                    }
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun lerpRect(a: Rect, b: Rect, t: Float) = Rect(
    a.left + (b.left - a.left) * t,
    a.top + (b.top - a.top) * t,
    a.right + (b.right - a.right) * t,
    a.bottom + (b.bottom - a.bottom) * t
)

/** Corner drag that keeps width/height == ratio. The opposite corner stays anchored. */
private fun resizeKeepRatio(
    r: Rect, type: HandleType, drag: Offset, ratio: Float, minSide: Float
): Rect {
    val anchor = when (type) {
        HandleType.TL -> r.bottomRight
        HandleType.TR -> r.bottomLeft
        HandleType.BL -> r.topRight
        HandleType.BR -> r.topLeft
        else -> return r            // edge handles are hidden in ratio mode
    }
    val moving = when (type) {
        HandleType.TL -> r.topLeft
        HandleType.TR -> r.topRight
        HandleType.BL -> r.bottomLeft
        else -> r.bottomRight
    } + drag

    var w = max(abs(moving.x - anchor.x), abs(moving.y - anchor.y) * ratio)
    val minW = if (ratio >= 1f) minSide else minSide * ratio
    if (w < minW) w = minW
    val h = w / ratio

    val sx = if (type == HandleType.TL || type == HandleType.BL) -1f else 1f
    val sy = if (type == HandleType.TL || type == HandleType.TR) -1f else 1f
    val x2 = anchor.x + sx * w
    val y2 = anchor.y + sy * h
    return Rect(min(anchor.x, x2), min(anchor.y, y2), max(anchor.x, x2), max(anchor.y, y2))
}

/** Dims everything outside the frame, draws its border, and a fading rule-of-thirds grid. */
@Composable
private fun CropScrimAndGrid(
    frameTopLeft: Offset,
    frameSize: Size,
    gridAlpha: Float
) {
    ComposeCanvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.99f)) {
        drawRect(color = Color.Black.copy(alpha = 0.6f))
        drawRect(color = Color.Transparent, topLeft = frameTopLeft, size = frameSize, blendMode = BlendMode.Clear)
        drawRect(color = Color.White, topLeft = frameTopLeft, size = frameSize, style = Stroke(width = 2.dp.toPx()))
        if (gridAlpha > 0f) {
            val gridColor = Color.White.copy(alpha = gridAlpha)
            for (i in 1..2) {
                val x = frameTopLeft.x + frameSize.width * i / 3f
                drawLine(gridColor, Offset(x, frameTopLeft.y), Offset(x, frameTopLeft.y + frameSize.height), strokeWidth = 1.dp.toPx())
                val y = frameTopLeft.y + frameSize.height * i / 3f
                drawLine(gridColor, Offset(frameTopLeft.x, y), Offset(frameTopLeft.x + frameSize.width, y), strokeWidth = 1.dp.toPx())
            }
        }
    }
}
@Composable
fun CornerHandle(
    x: Float,
    y: Float,
    density: Density,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    val touch = 44.dp
    val touchPx = with(density) { touch.toPx() }
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .offset { IntOffset((x - touchPx / 2f).roundToInt(), (y - touchPx / 2f).roundToInt()) }
            .size(touch)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { latestEnd() },
                    onDragCancel = { latestEnd() }
                ) { change, drag ->
                    change.consume()
                    latestDrag(drag)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(14.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

/** Edge-midpoint handle: draggable on a single axis (vertical=true means a left/right edge, horizontal movement). */
@Composable
private fun EdgeHandle(x: Float, y: Float, vertical: Boolean, density: Density, onDragEnd: () -> Unit = {}, onDrag: (Offset) -> Unit) {
    val touchDp = 44.dp
    val touchPx = with(density) { touchDp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((x - touchPx / 2).toInt(), (y - touchPx / 2).toInt()) }
            .size(touchDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                ) { change, drag ->
                    change.consume()
                    onDrag(if (vertical) Offset(drag.x, 0f) else Offset(0f, drag.y))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (vertical) {
            Box(Modifier.width(4.dp).height(20.dp).background(Color.White, RoundedCornerShape(2.dp)))
        } else {
            Box(Modifier.width(20.dp).height(4.dp).background(Color.White, RoundedCornerShape(2.dp)))
        }
    }
}

private fun translateRect(rect: Rect, delta: Offset, bounds: Rect): Rect {
    val left = (rect.left + delta.x).coerceIn(bounds.left, bounds.right - rect.width)
    val top = (rect.top + delta.y).coerceIn(bounds.top, bounds.bottom - rect.height)
    return Rect(left, top, left + rect.width, top + rect.height)
}

private fun applyHandleDrag(rect: Rect, handle: HandleType, delta: Offset, bounds: Rect, minSize: Float): Rect {
    var left = rect.left
    var top = rect.top
    var right = rect.right
    var bottom = rect.bottom
    when (handle) {
        HandleType.TL -> { left += delta.x; top += delta.y }
        HandleType.TR -> { right += delta.x; top += delta.y }
        HandleType.BL -> { left += delta.x; bottom += delta.y }
        HandleType.BR -> { right += delta.x; bottom += delta.y }
        HandleType.T -> top += delta.y
        HandleType.B -> bottom += delta.y
        HandleType.L -> left += delta.x
        HandleType.R -> right += delta.x
    }
    left = left.coerceIn(bounds.left, right - minSize)
    top = top.coerceIn(bounds.top, bottom - minSize)
    right = right.coerceIn(left + minSize, bounds.right)
    bottom = bottom.coerceIn(top + minSize, bounds.bottom)
    return Rect(left, top, right, bottom)
}

/** How much extra zoom is needed so a rotated image still fully covers the frame (no gaps at corners). */
private fun straightenScaleCompensation(angleDegrees: Float, frameWidth: Float, frameHeight: Float): Float {
    if (angleDegrees == 0f) return 1f
    val angle = Math.toRadians(angleDegrees.toDouble())
    val cosA = abs(cos(angle))
    val sinA = abs(sin(angle))
    val scaleForWidth = (frameWidth * cosA + frameHeight * sinA) / frameWidth
    val scaleForHeight = (frameWidth * sinA + frameHeight * cosA) / frameHeight
    return max(scaleForWidth, scaleForHeight).toFloat()
}




private fun transformBase(src: Bitmap, steps: Int, flipH: Boolean): Bitmap {
    if (steps == 0 && !flipH) return src
    val m = Matrix().apply {
        postRotate(steps * 90f)
        if (flipH) postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}
/** Decode with EXIF rotation applied and downsampled so the longest side <= maxSide. */
fun loadOrientedBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        else -> return decoded
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
}

/**
 * Renders the final crop from a CropState. Mirrors the screen math:
 * values are normalized by workingBitmap.width and already divided by the display scale,
 * so they are directly in working-bitmap pixels.
 */
fun renderCrop(source: Bitmap, state: CropState): Bitmap {
    val working = transformBase(source, state.steps, state.flipH)
    val unit = working.width.toFloat()

    val outW = (state.frameW * unit).roundToInt().coerceAtLeast(1)
    val outH = (state.frameH * unit).roundToInt().coerceAtLeast(1)
    val panX = state.panX * unit
    val panY = state.panY * unit

    val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    canvas.translate(outW / 2f + panX, outH / 2f + panY) // image center = frame center + pan
    canvas.rotate(state.straighten)                       // straighten about image center
    canvas.drawBitmap(working, -working.width / 2f, -working.height / 2f, paint)
    return result
}