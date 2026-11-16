package com.senghol.knotx.ui.screens.stories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

/**
 * Full-screen crop tool.
 * - Locked aspect ratios: frame is fixed, pinch-zoom / drag to pan the image underneath.
 * - Free: image is static (contain-fit), the frame itself is a resizable rectangle —
 *   4 corner handles (both axes) + 4 edge handles (one axis) + drag-to-move from the middle.
 *
 * implementation("androidx.exifinterface:exifinterface:1.3.7")
 */
@Composable
fun CropScreen(
    imageUri: Uri,
    onClose: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var workingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var aspect by remember { mutableStateOf(CropAspect.FREE) }

    // Locked-ratio mode state (image pans/zooms, frame is fixed)
    var userScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Free mode state (frame itself is resizable, image is static — plus an auto
    // zoom/pan that kicks in after a resize so the selection fills the screen)
    var freeRect by remember { mutableStateOf<Rect?>(null) }
    var freeZoom by remember { mutableFloatStateOf(1f) }
    var freePan by remember { mutableStateOf(Offset.Zero) }

    var straightenDegrees by remember { mutableFloatStateOf(0f) }
    var isInteracting by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }

    // Kept in sync with layout every frame, so Done can reuse the exact numbers.
    var frameWidthPxState by remember { mutableFloatStateOf(0f) }
    var frameHeightPxState by remember { mutableFloatStateOf(0f) }
    var totalScaleState by remember { mutableFloatStateOf(1f) }
    var effectivePanState by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(imageUri) {
        workingBitmap = loadOrientedBitmap(context, imageUri)
    }

    LaunchedEffect(interactionTick) {
        if (interactionTick > 0) {
            isInteracting = true
            delay(500)
            isInteracting = false
        }
    }

    fun resetTransform() {
        userScale = 1f
        panOffset = Offset.Zero
        freeRect = null // recomputed to full-image bounds on next layout pass
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
                        val wb = workingBitmap ?: return@FilledIconButton
                        scope.launch {
                            isSaving = true
                            val uri = withContext(Dispatchers.Default) {
                                cropAndSave(
                                    context = context,
                                    bitmap = wb,
                                    frameWidthPx = frameWidthPxState,
                                    frameHeightPx = frameHeightPxState,
                                    totalScale = totalScaleState,
                                    panOffset = effectivePanState,
                                    straightenDegrees = straightenDegrees
                                )
                            }
                            isSaving = false
                            if (uri != null) onCropped(uri)
                        }
                    },
                    enabled = !isSaving && workingBitmap != null,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                    }
                }
            }
        }

        // ---- Crop area ----
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val bmp = workingBitmap
            if (bmp == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val areaWidthPx = with(density) { maxWidth.toPx() }
                    val areaHeightPx = with(density) { maxHeight.toPx() }
                    val paddingPx = with(density) { 20.dp.toPx() }
                    val availW = areaWidthPx - paddingPx * 2
                    val availH = areaHeightPx - paddingPx * 2
                    val gridAlpha by animateFloatAsState(if (isInteracting) 0.7f else 0f, label = "grid")

                    if (aspect == CropAspect.FREE) {
                        // ---- FREE: image is static apart from the auto zoom/pan below;
                        //            the frame itself is a resizable rectangle ----
                        val fitScale = min(availW / bmp.width, availH / bmp.height)
                        val displayedW = bmp.width * fitScale * freeZoom
                        val displayedH = bmp.height * fitScale * freeZoom
                        val imageCenter = Offset(areaWidthPx / 2f + freePan.x, areaHeightPx / 2f + freePan.y)
                        val imageBounds = Rect(
                            left = imageCenter.x - displayedW / 2f,
                            top = imageCenter.y - displayedH / 2f,
                            right = imageCenter.x + displayedW / 2f,
                            bottom = imageCenter.y + displayedH / 2f
                        )
                        if (freeRect == null) freeRect = imageBounds
                        val rect = freeRect ?: imageBounds
                        val minSizePx = with(density) { 48.dp.toPx() }

                        totalScaleState = fitScale * freeZoom
                        frameWidthPxState = rect.width
                        frameHeightPxState = rect.height
                        effectivePanState = imageBounds.center - rect.center

                        // Called when a handle (or the move-drag) is released: animates zoom/pan
                        // so the current selection grows or shrinks to fill the viewport — the
                        // classic "zoom to selection" behavior. Reads live state, not `rect`/
                        // `imageBounds` captured above, since it may run long after this render.
                        fun autoZoomToSelection() {
                            val current = freeRect ?: return
                            if (availW <= 0f || availH <= 0f || current.width <= 0f || current.height <= 0f) return
                            val multiplier = min(availW / current.width, availH / current.height)
                            if (abs(multiplier - 1f) < 0.01f) return // already filling the viewport

                            val zoomStart = freeZoom
                            val panStart = freePan
                            val rectStart = current

                            val zoomTarget = (freeZoom * multiplier).coerceIn(0.1f, 10f)
                            val imageCenterNow = Offset(areaWidthPx / 2f + freePan.x, areaHeightPx / 2f + freePan.y)
                            val rectCenterAfterScale = imageCenterNow + (current.center - imageCenterNow) * multiplier
                            val viewportCenter = Offset(areaWidthPx / 2f, areaHeightPx / 2f)
                            val panTarget = freePan + (viewportCenter - rectCenterAfterScale)

                            val newWidth = current.width * multiplier
                            val newHeight = current.height * multiplier
                            val rectTarget = Rect(
                                viewportCenter.x - newWidth / 2f, viewportCenter.y - newHeight / 2f,
                                viewportCenter.x + newWidth / 2f, viewportCenter.y + newHeight / 2f
                            )

                            scope.launch {
                                val anim = Animatable(0f)
                                anim.animateTo(1f, tween(250)) {
                                    val t = value
                                    freeZoom = lerp(zoomStart, zoomTarget, t)
                                    freePan = Offset(lerp(panStart.x, panTarget.x, t), lerp(panStart.y, panTarget.y, t))
                                    freeRect = Rect(
                                        lerp(rectStart.left, rectTarget.left, t),
                                        lerp(rectStart.top, rectTarget.top, t),
                                        lerp(rectStart.right, rectTarget.right, t),
                                        lerp(rectStart.bottom, rectTarget.bottom, t)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(
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

                            // Drag interior to move the whole rect
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                                    .size(
                                        with(density) { rect.width.toDp() },
                                        with(density) { rect.height.toDp() }
                                    )
                                    .pointerInput(imageBounds) {
                                        detectDragGestures(
                                            onDragEnd = { autoZoomToSelection() },
                                            onDragCancel = { autoZoomToSelection() }
                                        ) { change, drag ->
                                            change.consume()
                                            // Read the LIVE state here, not the `rect` val captured when
                                            // this composable last ran — pointerInput(imageBounds) only
                                            // launches its gesture loop once, so a captured local would
                                            // stay frozen at its very first value for the whole drag.
                                            val current = freeRect ?: imageBounds
                                            freeRect = translateRect(current, drag, imageBounds)
                                            interactionTick++
                                        }
                                    }
                            )

                            // 4 corner handles — both axes
                            listOf(
                                HandleType.TL to rect.topLeft,
                                HandleType.TR to Offset(rect.right, rect.top),
                                HandleType.BL to Offset(rect.left, rect.bottom),
                                HandleType.BR to Offset(rect.right, rect.bottom)
                            ).forEach { (type, pos) ->
                                CornerHandle(
                                    x = pos.x, y = pos.y, density = density,
                                    onDragEnd = { autoZoomToSelection() }
                                ) { drag ->
                                    // Same fix as above: read freeRect live, don't use the
                                    // captured `rect` snapshot from this composition pass.
                                    val current = freeRect ?: imageBounds
                                    freeRect = applyHandleDrag(current, type, drag, imageBounds, minSizePx)
                                    interactionTick++
                                }
                            }

                            // 4 edge handles — locked to one axis
                            EdgeHandle(rect.center.x, rect.top, vertical = false, density, onDragEnd = { autoZoomToSelection() }) { drag ->
                                val current = freeRect ?: imageBounds
                                freeRect = applyHandleDrag(current, HandleType.T, drag, imageBounds, minSizePx)
                                interactionTick++
                            }
                            EdgeHandle(rect.center.x, rect.bottom, vertical = false, density, onDragEnd = { autoZoomToSelection() }) { drag ->
                                val current = freeRect ?: imageBounds
                                freeRect = applyHandleDrag(current, HandleType.B, drag, imageBounds, minSizePx)
                                interactionTick++
                            }
                            EdgeHandle(rect.left, rect.center.y, vertical = true, density, onDragEnd = { autoZoomToSelection() }) { drag ->
                                val current = freeRect ?: imageBounds
                                freeRect = applyHandleDrag(current, HandleType.L, drag, imageBounds, minSizePx)
                                interactionTick++
                            }
                            EdgeHandle(rect.right, rect.center.y, vertical = true, density, onDragEnd = { autoZoomToSelection() }) { drag ->
                                val current = freeRect ?: imageBounds
                                freeRect = applyHandleDrag(current, HandleType.R, drag, imageBounds, minSizePx)
                                interactionTick++
                            }
                        }
                    } else {
                        // ---- LOCKED RATIO: fixed frame, pinch-zoom/pan the image ----
                        val ratio = aspect.ratio!!
                        val frameW: Float
                        val frameH: Float
                        if (availW / availH > ratio) {
                            frameH = availH; frameW = availH * ratio
                        } else {
                            frameW = availW; frameH = availW / ratio
                        }

                        val baseScale = max(frameW / bmp.width, frameH / bmp.height)
                        val compensation = straightenScaleCompensation(straightenDegrees, frameW, frameH)
                        val totalScale = userScale * baseScale * compensation

                        frameWidthPxState = frameW
                        frameHeightPxState = frameH
                        totalScaleState = totalScale

                        val displayedW = bmp.width * totalScale
                        val displayedH = bmp.height * totalScale
                        val maxOffsetX = max(0f, (displayedW - frameW) / 2f)
                        val maxOffsetY = max(0f, (displayedH - frameH) / 2f)
                        panOffset = Offset(
                            panOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            panOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                        effectivePanState = panOffset

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(bmp, aspect) {
                                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                        userScale = (userScale * zoom).coerceIn(1f, 5f)
                                        panOffset += pan
                                        interactionTick++
                                    }
                                }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(
                                        with(density) { bmp.width.toDp() },
                                        with(density) { bmp.height.toDp() }
                                    )
                                    .graphicsLayer(
                                        scaleX = totalScale,
                                        scaleY = totalScale,
                                        rotationZ = straightenDegrees,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y
                                    )
                            )
                            CropScrimAndGrid(
                                frameTopLeft = Offset((areaWidthPx - frameW) / 2f, (areaHeightPx - frameH) / 2f),
                                frameSize = Size(frameW, frameH),
                                gridAlpha = gridAlpha
                            )
                        }
                    }
                }
            }
        }

        // ---- Straighten slider (works in both modes; in Free it rotates the image
        // under your resizable frame, so keep rotation modest if the frame is close
        // to the full image size or you may see blank corners) ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${straightenDegrees.toInt()}°", color = Color.White, modifier = Modifier.width(44.dp))
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
                workingBitmap = workingBitmap?.let { rotateBitmap(it, -90f) }
                resetTransform()
            }) {
                Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = "Rotate", tint = Color.White)
            }
            IconButton(onClick = {
                workingBitmap = workingBitmap?.let { flipBitmapHorizontal(it) }
                resetTransform()
            }) {
                Icon(Icons.Default.Flip, contentDescription = "Flip", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CropAspect.values().toList()) { option ->
                    FilterChip(
                        selected = aspect == option,
                        onClick = { aspect = option; resetTransform() },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            labelColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
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

/** Corner handle: draggable on both axes. */
@Composable
private fun CornerHandle(x: Float, y: Float, density: Density, onDragEnd: () -> Unit = {}, onDrag: (Offset) -> Unit) {
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
                ) { change, drag -> change.consume(); onDrag(drag) }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(Color.White, RoundedCornerShape(3.dp))
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

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun flipBitmapHorizontal(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/** Decodes the image, downsamples if huge, and auto-rotates using its EXIF orientation. */
private suspend fun loadOrientedBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

    val maxDimension = 2048
    var sampleSize = 1
    while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, decodeOptions)
    } ?: return@withContext null

    val orientation = context.contentResolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees != 0f) rotateBitmap(bitmap, degrees) else bitmap
}

/**
 * Renders exactly what's visible inside the crop frame into a fresh bitmap and saves it to cache.
 * Works for both modes: in Free mode, `panOffset` is the offset between the static image's
 * center and the frame's center; in locked-ratio mode it's the user's pan gesture directly.
 */
private fun cropAndSave(
    context: Context,
    bitmap: Bitmap,
    frameWidthPx: Float,
    frameHeightPx: Float,
    totalScale: Float,
    panOffset: Offset,
    straightenDegrees: Float
): Uri? {
    if (frameWidthPx <= 0f || frameHeightPx <= 0f) return null

    val desiredLongSide = 1080f
    val outputW: Int
    val outputH: Int
    if (frameWidthPx >= frameHeightPx) {
        outputW = desiredLongSide.toInt()
        outputH = (desiredLongSide * frameHeightPx / frameWidthPx).toInt()
    } else {
        outputH = desiredLongSide.toInt()
        outputW = (desiredLongSide * frameWidthPx / frameHeightPx).toInt()
    }
    val outputScale = outputW / frameWidthPx

    val matrix = Matrix().apply {
        postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        postRotate(straightenDegrees)
        postScale(totalScale, totalScale)
        postTranslate(panOffset.x, panOffset.y)
        postTranslate(frameWidthPx / 2f, frameHeightPx / 2f)
        postScale(outputScale, outputScale)
    }

    val output = Bitmap.createBitmap(outputW, outputH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, matrix, paint)

    return try {
        val file = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}