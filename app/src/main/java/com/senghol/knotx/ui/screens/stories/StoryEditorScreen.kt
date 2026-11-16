package com.senghol.knotx.ui.screens.stories

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush as ColorBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.senghol.knotx.data.database.StoryMediaType
import com.senghol.knotx.data.database.StoryVisibility
import com.senghol.knotx.ui.screens.MediaCategory
import com.senghol.knotx.ui.screens.MediaVisibility
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.ui.screens.saveMediaToAppStorageNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.lazy.LazyColumn
/**
 * Full-screen story editor: draw, add text, drop stickers, then save
 * the flattened result to the device gallery.
 *
 * Gradle deps this file needs:
 *   implementation("io.coil-kt:coil-compose:2.6.0")
 *   implementation("androidx.compose.material:material-icons-extended") // Brush/Undo/EmojiEmotions
 *   implementation("androidx.media3:media3-exoplayer:1.4.1")            // only if you support video
 *   implementation("androidx.media3:media3-ui:1.4.1")
 */


@Composable
fun StoryEditorScreen(
    background: StoryBackground,
    onClose: () -> Unit,
    onReCropRequest: (Uri) -> Unit,
    onSaved: (SavedMedia, StoryVisibility) -> Unit, // media type/duration still come from saved.mimeType + MediaMetadataRetriever
    viewModel: StoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // `background` (the function param) is just the INITIAL value from the picker.
    // For text/gradient stories we let the user keep changing it live in the editor.
    var currentBackground by remember(background) { mutableStateOf(background) }

    // ViewModel State Observers
    val audioTracks by viewModel.audioTracks.collectAsState()
    val isLoadingMusic by viewModel.isLoadingMusic.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var activeTool by remember { mutableStateOf(EditorTool.NONE) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableFloatStateOf(12f) }

    val strokes = remember { mutableStateListOf<DrawStroke>() }
    val currentStrokePoints = remember { mutableStateListOf<Offset>() }

    val texts = remember { mutableStateListOf<TextOverlay>() }
    val stickers = remember { mutableStateListOf<StickerOverlay>() }

    var editingText by remember { mutableStateOf<TextOverlay?>(null) }
    var pendingNewText by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var saveJob by remember { mutableStateOf<Job?>(null) } // lets Cancel actually stop the export, not just hide the dialog

    // Records exactly the content Box below (background + drawing + overlays) via
    // the same hardware-accelerated path the screen already renders with — this
    // replaces view.drawToBitmap(), which was a whole-window SOFTWARE screenshot
    // and could silently drop content it shouldn't have.
    val graphicsLayer = rememberGraphicsLayer() // full flatten (background + overlays) — used for image stories
    val overlaysOnlyGraphicsLayer = rememberGraphicsLayer() // overlays ONLY, transparent bg — used to burn onto video frames
    // Only set during export, only for video — a real frame pulled from the file
    // to stand in for the live player for that one captured frame (see note below
    // on why the live video can never be captured directly).
    var capturedVideoFrameUri by remember { mutableStateOf<Uri?>(null) }

    // Video-specific audio handling: strip the original track, and/or the audio
    // picker's `selectedAudio` doubles as "replacement track" when background is video.
    var muteOriginalVideoAudio by remember { mutableStateOf(false) }
    val isVideoBackground = (currentBackground as? StoryBackground.Media)?.isVideo == true

    // Who can see this story — asked right before export so a privacy choice
    // is never skipped, and so we don't waste an export on a story the user
    // then decides not to post. Defaults to Everyone.
    var showAudienceSheet by remember { mutableStateOf(false) }
    var chosenVisibility by remember { mutableStateOf(StoryVisibility.EVERYONE) }

    // Snap-to-center guides + drag-to-delete
    var snappedX by remember { mutableStateOf(false) }
    var snappedY by remember { mutableStateOf(false) }
    var draggingOverlayId by remember { mutableStateOf<String?>(null) }
    var isOverTrash by remember { mutableStateOf(false) }
    var overlayInteractionTick by remember { mutableStateOf(0) }
    var trashZone by remember { mutableStateOf<Rect?>(null) }

    // Treat "no movement for 350ms" as the drag having ended — detectTransformGestures
    // doesn't expose a clean release callback, so this is the practical substitute.
    LaunchedEffect(overlayInteractionTick) {
        if (overlayInteractionTick > 0) {
            delay(350.milliseconds)
            if (isOverTrash) {
                val idToRemove = draggingOverlayId
                texts.removeAll { it.id == idToRemove }
                stickers.removeAll { it.id == idToRemove }
            }
            snappedX = false
            snappedY = false
            draggingOverlayId = null
            isOverTrash = false
        }
    }

    // Audio State
    var selectedAudio by remember { mutableStateOf<SelectedStoryAudio?>(null) }
    var showAudioSheet by remember { mutableStateOf(false) }

    // repeatMode is now OFF at the player level — looping the selected [start,end]
    // window is handled manually below (see effect #2), since ExoPlayer's clip
    // config would need a full reprepare every time the range changes.
    val audioPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
    }

    // 1. Load the FULL, unclipped track exactly once per track selection.
    // Deliberately does NOT depend on startOffsetMs/endOffsetMs anymore — that
    // was forcing a full reprepare on every pixel of a drag, which caused both
    // the audible stutter and the slider "jumping" (duration briefly reporting
    // -1 mid-reprepare, which jittered the range's scale under your finger).
    LaunchedEffect(selectedAudio?.audioUri) {
        val audio = selectedAudio
        if (audio != null && audio.audioUri != Uri.EMPTY) {
            val localUri = if (audio.audioUri.scheme?.startsWith("http") == true) {
                StoryAudioDownloader.getCachedOrDownload(context, audio.audioUri.toString())
            } else {
                audio.audioUri
            }
            audioPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(localUri))
            audioPlayer.prepare()
            audioPlayer.seekTo(audio.startOffsetMs)
            audioPlayer.playWhenReady = !showAudioSheet
        } else {
            audioPlayer.stop()
            audioPlayer.clearMediaItems()
        }
    }

    // 2. Enforce looping within [start,end] ourselves — a cheap position check
    // + seekTo, no reprepare, so it stays smooth even while the range is
    // actively being dragged. Reads `selectedAudio` fresh every tick so it
    // always reflects the latest range without needing to restart this effect.
    LaunchedEffect(selectedAudio?.audioUri) {
        while (isActive) {
            delay(100)
            val audio = selectedAudio ?: continue
            if (audioPlayer.isPlaying && audioPlayer.currentPosition >= audio.endOffsetMs) {
                audioPlayer.seekTo(audio.startOffsetMs)
            }
        }
    }

    // 3. THE ACTUAL FIX: audioPlayer.duration is a live player property, not
    // Compose state — reading it directly in the composable body meant its
    // value could silently flip (e.g. once ExoPlayer resolves the real
    // duration right after prepare()) on any unrelated recomposition that
    // happened to fire mid-drag. That changed the trim bar's px↔ms scale
    // WHILE your finger was moving, which is what caused the jump and the
    // eventual crash. Freeze it into real state once, and never re-read the
    // raw property for layout math again.
    var stableAudioDurationMs by remember(selectedAudio?.audioUri) { mutableLongStateOf(0L) }
    LaunchedEffect(selectedAudio?.audioUri) {
        while (isActive) {
            val d = audioPlayer.duration
            if (d > 0) {
                stableAudioDurationMs = d
                break
            }
            delay(50)
        }
    }

    // 3. Pause editor background music while Audio Picker Bottom Sheet is open
    LaunchedEffect(showAudioSheet) {
        if (showAudioSheet) {
            audioPlayer.pause()
        } else if (selectedAudio != null) {
            audioPlayer.play()
        }
    }

    // Release ExoPlayer when StoryEditorScreen leaves composition
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.release()
        }
    }

    // Placed HERE, after graphicsLayer/overlaysOnlyGraphicsLayer/selectedAudio/
    // chosenVisibility/etc are all declared — Kotlin resolves local vals strictly
    // top-to-bottom, so this must sit below everything it references or the file
    // won't compile, regardless of the lambda never running until later.
    val performExport: () -> Unit = {
        saveJob = scope.launch {
            isSaving = true
            activeTool = EditorTool.NONE

            val bg = currentBackground
            if (bg is StoryBackground.Media && bg.isVideo) {
                capturedVideoFrameUri = withContext(Dispatchers.IO) {
                    extractVideoFrame(context, bg.uri)
                }
            }

            isCapturing = true
            delay(120.milliseconds)
            val flattenedBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
            isCapturing = false
            capturedVideoFrameUri = null

            val audio = selectedAudio

            val result = try {
                withContext(Dispatchers.IO) {
                    when {
                        (bg !is StoryBackground.Media && audio != null) ||
                                (bg is StoryBackground.Media && !bg.isVideo && audio != null) -> {
                            val flattenedFile = File(context.cacheDir, "story_frame_${System.currentTimeMillis()}.jpg")
                            flattenedFile.outputStream().use {
                                flattenedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
                            }
                            val outputFile = File(context.cacheDir, "story_video_${System.currentTimeMillis()}.mp4")
                            val ok = muxImageWithAudio(
                                context = context,
                                imagePath = flattenedFile.absolutePath,
                                audioUri = audio.audioUri,
                                audioStartMs = audio.startOffsetMs,
                                audioEndMs = audio.endOffsetMs,
                                outputFile = outputFile
                            )
                            flattenedFile.delete()
                            if (!ok) return@withContext null
                            val saved = saveMediaToAppStorageNext(
                                context = context,
                                sourceUri = Uri.fromFile(outputFile),
                                storageType = MediaCategory.STORY_VIDEO,
                                visibility = MediaVisibility.PRIVATE,
                                overrideMimeType = "video/mp4"
                            )
                            outputFile.delete()
                            saved?.let { Triple(it, StoryMediaType.VIDEO, audio.endOffsetMs - audio.startOffsetMs) }
                        }

                        bg is StoryBackground.Media && bg.isVideo -> {
                            val hasOverlays = strokes.isNotEmpty() || texts.isNotEmpty() || stickers.isNotEmpty()
                            val audioMode = when {
                                audio != null -> VideoAudioMode.Replace(audio.audioUri, audio.startOffsetMs, audio.endOffsetMs)
                                muteOriginalVideoAudio -> VideoAudioMode.Mute
                                else -> VideoAudioMode.KeepOriginal
                            }

                            if (!hasOverlays && audioMode is VideoAudioMode.KeepOriginal) {
                                saveMediaToAppStorageNext(
                                    context = context,
                                    sourceUri = bg.uri,
                                    storageType = MediaCategory.STORY_VIDEO,
                                    visibility = MediaVisibility.PRIVATE,
                                    overrideMimeType = "video/mp4"
                                )?.let { Triple(it, StoryMediaType.VIDEO, null) }
                            } else {
                                var overlayPath: String? = null
                                if (hasOverlays) {
                                    val overlayBitmap = overlaysOnlyGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                    val overlayFile = File(context.cacheDir, "story_overlay_${System.currentTimeMillis()}.png")
                                    overlayFile.outputStream().use {
                                        overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                                    }
                                    overlayPath = overlayFile.absolutePath
                                }

                                val outputFile = File(context.cacheDir, "story_video_${System.currentTimeMillis()}.mp4")
                                val ok = muxVideoWithOverlayAndAudio(
                                    context = context,
                                    videoUri = bg.uri,
                                    overlayImagePath = overlayPath,
                                    audioMode = audioMode,
                                    outputFile = outputFile
                                )
                                overlayPath?.let { File(it).delete() }
                                if (!ok) return@withContext null

                                val saved = saveMediaToAppStorageNext(
                                    context = context,
                                    sourceUri = Uri.fromFile(outputFile),
                                    storageType = MediaCategory.STORY_VIDEO,
                                    visibility = MediaVisibility.PRIVATE,
                                    overrideMimeType = "video/mp4"
                                )
                                outputFile.delete()
                                saved?.let { Triple(it, StoryMediaType.VIDEO, null) }
                            }
                        }

                        else -> saveStoryBitmapToAppStorage(context, flattenedBitmap)?.let {
                            Triple(it, StoryMediaType.IMAGE, null)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("STORY_DEBUG", "onSave: export threw", e)
                null
            }

            isSaving = false
            if (result != null) {
                val (saved, _, _) = result
                onSaved(saved, chosenVisibility)
            } else {
                Log.e("STORY_DEBUG", "onSave: export failed, screen staying open so you can retry")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0E))
    ) {
        // ---------- Captured content ----------
        // Everything inside THIS Box gets recorded into graphicsLayer on save.
        // Scoping the capture this tightly (vs. the old whole-window screenshot)
        // is what fixes the background silently vanishing from exports.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }
        ) {
            // ---------- Background & Canvas Layer ----------
            StoryBackgroundLayer(
                background = currentBackground,
                isCapturing = isCapturing,
                capturedVideoFrameUri = capturedVideoFrameUri,
                muteOriginalAudio = muteOriginalVideoAudio
            )

            // Everything from here down to the closing brace marked below is recorded
            // a SECOND time, independently, into overlaysOnlyGraphicsLayer — nesting a
            // recording Box inside the outer one doesn't change how this renders on
            // screen (drawLayer just composites it inline as normal), it just ALSO lets
            // us grab "overlays only, transparent background" for video export.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        overlaysOnlyGraphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(overlaysOnlyGraphicsLayer)
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTool) {
                            if (activeTool == EditorTool.DRAW) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints.clear()
                                    currentStrokePoints.add(offset)
                                },
                                onDrag = { change, _ -> currentStrokePoints.add(change.position) },
                                onDragEnd = {
                                    if (currentStrokePoints.size > 1) {
                                        strokes.add(
                                            DrawStroke(
                                                pathFromPoints(currentStrokePoints),
                                                currentColor,
                                                strokeWidth
                                            )
                                        )
                                    }
                                    currentStrokePoints.clear()
                                }
                            )
                        }
                    }
            ) {
                strokes.forEach { stroke ->
                    drawPath(
                        stroke.path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.strokeWidthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                if (currentStrokePoints.size > 1) {
                    drawPath(
                        pathFromPoints(currentStrokePoints),
                        color = currentColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            texts.forEach { overlay ->
                Box(modifier = Modifier.align(Alignment.Center)) {
                    DraggableTextOverlay(
                        overlay = overlay,
                        interactive = activeTool != EditorTool.DRAW,
                        onTap = { editingText = overlay },
                        onTransform = { offsetDelta, scaleDelta, rotationDelta ->
                            var newOffset = overlay.offset + offsetDelta
                            val snapThreshold = 14f
                            val snapX = abs(newOffset.x) < snapThreshold
                            val snapY = abs(newOffset.y) < snapThreshold
                            if (snapX) newOffset = newOffset.copy(x = 0f)
                            if (snapY) newOffset = newOffset.copy(y = 0f)
                            overlay.offset = newOffset
                            overlay.scale = (overlay.scale * scaleDelta).coerceIn(0.4f, 4f)
                            overlay.rotation += rotationDelta
                            snappedX = snapX
                            snappedY = snapY
                            draggingOverlayId = overlay.id
                            overlayInteractionTick++
                        },
                        onPositioned = { rect ->
                            if (draggingOverlayId == overlay.id) {
                                isOverTrash = trashZone?.overlaps(rect) == true
                            }
                        }
                    )
                }
            }

            stickers.forEach { sticker ->
                Box(modifier = Modifier.align(Alignment.Center)) {
                    DraggableStickerOverlay(
                        sticker = sticker,
                        interactive = activeTool != EditorTool.DRAW,
                        onTransform = { offsetDelta, scaleDelta, rotationDelta ->
                            var newOffset = sticker.offset + offsetDelta
                            val snapThreshold = 14f
                            val snapX = abs(newOffset.x) < snapThreshold
                            val snapY = abs(newOffset.y) < snapThreshold
                            if (snapX) newOffset = newOffset.copy(x = 0f)
                            if (snapY) newOffset = newOffset.copy(y = 0f)
                            sticker.offset = newOffset
                            sticker.scale = (sticker.scale * scaleDelta).coerceIn(0.4f, 4f)
                            sticker.rotation += rotationDelta
                            snappedX = snapX
                            snappedY = snapY
                            draggingOverlayId = sticker.id
                            overlayInteractionTick++
                        },
                        onPositioned = { rect ->
                            if (draggingOverlayId == sticker.id) {
                                isOverTrash = trashZone?.overlaps(rect) == true
                            }
                        }
                    )
                }
            }
            } // closes the overlaysOnlyGraphicsLayer recording Box added above

            // Snap-to-center guide lines (only visible while actively snapped)
            if (snappedX || snappedY) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (snappedX) {
                        drawLine(
                            Color.White.copy(alpha = 0.6f),
                            Offset(size.width / 2f, 0f),
                            Offset(size.width / 2f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    if (snappedY) {
                        drawLine(
                            Color.White.copy(alpha = 0.6f),
                            Offset(0f, size.height / 2f),
                            Offset(size.width, size.height / 2f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }
        // ---------- End captured content ----------

        // UI Overlay Controls
        if (!isCapturing) {
            StoryEditorTopBar(
                background = currentBackground,
                activeTool = activeTool,
                hasAudio = selectedAudio != null,
                onToolSelected = { tool ->
                    activeTool = if (activeTool == tool) EditorTool.NONE else tool
                    if (tool == EditorTool.TEXT) pendingNewText = true
                },
                onAudioClick = { showAudioSheet = true },
                canUndo = strokes.isNotEmpty(),
                onUndo = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                onCropClick = {
                    val bg = currentBackground
                    if (bg is StoryBackground.Media && !bg.isVideo) {
                        onReCropRequest(bg.uri)
                    }
                },
                muteOriginalVideoAudio = muteOriginalVideoAudio,
                onToggleMuteOriginalAudio = { muteOriginalVideoAudio = !muteOriginalVideoAudio },
                onClose = onClose,
                isSaving = isSaving,
                onSave = { showAudienceSheet = true }
            )

            // Drag-to-delete trash zone — shows only while a text/sticker is being moved
            AnimatedVisibility(
                visible = draggingOverlayId != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isOverTrash) 64.dp else 52.dp)
                        .onGloballyPositioned { coords -> trashZone = coords.boundsInRoot() }
                        .clip(CircleShape)
                        .background(if (isOverTrash) Color(0xFFE53935) else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }

            // Drawing Toolbar
            AnimatedVisibility(
                visible = activeTool == EditorTool.DRAW,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                ColorAndWidthPicker(
                    selectedColor = currentColor,
                    onColorSelected = { currentColor = it },
                    strokeWidth = strokeWidth,
                    onStrokeWidthChange = { strokeWidth = it }
                )
            }

            // Sticker Toolbar
            AnimatedVisibility(
                visible = activeTool == EditorTool.STICKER,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                StickerPicker(
                    onStickerPicked = { emoji ->
                        stickers.add(StickerOverlay(emoji = emoji, offset = Offset.Zero))
                        activeTool = EditorTool.NONE
                    }
                )
            }

            // Background Picker — swap gradients/solids live, text-mode only
            AnimatedVisibility(
                visible = activeTool == EditorTool.BACKGROUND,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                BackgroundPicker(
                    selected = currentBackground,
                    onBackgroundPicked = { bg -> currentBackground = bg }
                )
            }

            // 15-Second Audio Trimmer Bar
// Flexible Audio Range Trimmer Bar
            if (selectedAudio != null && activeTool == EditorTool.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    StoryAudioTrimBar(
                        audioTrack = selectedAudio!!,
                        totalDurationMs = if (stableAudioDurationMs > 0) stableAudioDurationMs else selectedAudio!!.totalAudioDurationMs,
                        currentPositionProvider = { audioPlayer.currentPosition },
                        isPlaying = audioPlayer.isPlaying,
                        onScrub = { positionMs -> audioPlayer.seekTo(positionMs) }, // cheap — just a seek, no reprepare
                        onRangeChanged = { newStartMs, newEndMs ->
                            // Fires once per gesture, on release — not per drag tick
                            selectedAudio = selectedAudio?.copy(startOffsetMs = newStartMs, endOffsetMs = newEndMs)
                            audioPlayer.seekTo(newStartMs)
                            audioPlayer.play()
                        },
                        onTogglePlay = {
                            if (audioPlayer.isPlaying) audioPlayer.pause() else audioPlayer.play()
                        },
                        onRemoveAudio = { selectedAudio = null }
                    )
                }
            }
        }
    }

    // Text Editor Dialog
    if (pendingNewText || editingText != null) {
        TextEditDialog(
            initialText = editingText?.text ?: "",
            initialColor = editingText?.color ?: Color.White,
            initialFontSize = editingText?.fontSizeSp ?: 32f,
            initialFontFamily = editingText?.fontFamily ?: FontFamily.Default,
            initialBgStyle = editingText?.backgroundStyle ?: TextBackgroundStyle.TRANSLUCENT_PILL,
            onDismiss = {
                pendingNewText = false
                editingText = null
                activeTool = EditorTool.NONE
            },
            onConfirm = { text, color, fontSize, font, bgStyle ->
                if (text.isNotBlank()) {
                    val target = editingText
                    if (target != null) {
                        target.text = text
                        target.color = color
                        target.fontSizeSp = fontSize
                        target.fontFamily = font
                        target.backgroundStyle = bgStyle
                    } else {
                        texts.add(
                            TextOverlay(
                                text = text,
                                color = color,
                                fontSizeSp = fontSize,
                                fontFamily = font,
                                backgroundStyle = bgStyle,
                                offset = Offset.Zero
                            )
                        )
                    }
                }
                pendingNewText = false
                editingText = null
                activeTool = EditorTool.NONE
            }
        )
    }

    if (showAudienceSheet) {
        StoryAudienceSheet(
            initialVisibility = chosenVisibility,
            onDismiss = { showAudienceSheet = false },
            onConfirm = { visibility ->
                chosenVisibility = visibility
                showAudienceSheet = false
                performExport()
            }
        )
    }

    // Blocking "processing" dialog — shown for the whole export, un-dismissable
    // except via its own Cancel button (no tap-outside, no back-press escape).
    if (isSaving) {
        Dialog(
            onDismissRequest = { /* no-op by design — Cancel button is the only way out */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xF218181C)) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (currentBackground is StoryBackground.Media && (currentBackground as StoryBackground.Media).isVideo)
                            "Processing your video..." else "Saving your story...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(20.dp))
                    TextButton(onClick = {
                        saveJob?.cancel()
                        saveJob = null
                        isSaving = false
                        isCapturing = false
                        capturedVideoFrameUri = null
                    }) {
                        Text("Cancel", color = Color(0xFFFF512F))
                    }
                }
            }
        }
    }

    // Audio Picker Bottom Sheet Modal
    if (showAudioSheet) {
        AudioPickerBottomSheet(
            tracks = audioTracks,
            isLoading = isLoadingMusic,
            selectedCategory = selectedCategory,
            selectedAudio = selectedAudio,
            onCategorySelected = { category ->
                viewModel.fetchMusic(category)
            },
            onAudioSelected = { track ->
                selectedAudio = SelectedStoryAudio(
                    audioUri = track.uri,
                    title = track.title,
                    startOffsetMs = 0L,
                    endOffsetMs = 15_000L
                )
                showAudioSheet = false
            },
            onRemoveAudio = {
                selectedAudio = null
                showAudioSheet = false
            },
            onDismiss = { showAudioSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryAudienceSheet(
    initialVisibility: StoryVisibility,
    onDismiss: () -> Unit,
    onConfirm: (StoryVisibility) -> Unit
) {
    var visibility by remember { mutableStateOf(initialVisibility) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Share to",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Just 3 plain choices, no in-sheet picker — Contact List reuses
            // whatever's already configured in Settings server-side, so there's
            // nothing to select here beyond which mode applies.
            listOf(
                Triple(StoryVisibility.EVERYONE, "Everyone", "All your contacts can see this"),
                Triple(StoryVisibility.CUSTOM, "Contact List", "Uses the contact list from Settings"),
                Triple(StoryVisibility.PRIVATE, "Only me", "Nobody else can see this")
            ).forEach { (option, label, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { visibility = option }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = visibility == option, onClick = { visibility = option })
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onConfirm(visibility) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share")
            }
        }
    }
}

/**
 * A proper trim-window instead of a plain RangeSlider: drag either edge to trim,
 * drag the middle of the highlighted window to shift the whole clip. All drag
 * math reads live state directly (not a captured snapshot), and only commits
 * to the parent (onRangeChanged) when you release — see onDragEnd below.
 */
@SuppressLint("DefaultLocale")
@Composable
fun StoryAudioTrimBar(
    audioTrack: SelectedStoryAudio,
    totalDurationMs: Long,
    currentPositionProvider: () -> Long,
    isPlaying: Boolean,
    onScrub: (Long) -> Unit,
    onRangeChanged: (startMs: Long, endMs: Long) -> Unit,
    onTogglePlay: () -> Unit,
    onRemoveAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val safeTotalDuration = totalDurationMs.coerceAtLeast(2_000L).toFloat()
    val minDurationMs = 1000f
    val maxClipDurationMs = 60_000f // your requested 1-minute cap

    // Local + decoupled from the parent until the gesture ends — keyed only on
    // track identity, so a parent resync mid-drag can't fight the user's finger.
    var localStart by remember(audioTrack.audioUri) { mutableFloatStateOf(audioTrack.startOffsetMs.toFloat()) }
    var localEnd by remember(audioTrack.audioUri) {
        mutableFloatStateOf(audioTrack.endOffsetMs.toFloat().coerceIn(localStart + minDurationMs, safeTotalDuration))
    }

    var playheadMs by remember { mutableFloatStateOf(audioTrack.startOffsetMs.toFloat()) }
    LaunchedEffect(isPlaying, audioTrack.audioUri) {
        while (isActive) {
            playheadMs = currentPositionProvider().toFloat()
            delay(50)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xD9121212)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        audioTrack.title, color = Color.White, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePlay, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onRemoveAudio, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove music", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            val startSec = (localStart / 1000).toInt()
            val endSec = (localEnd / 1000).toInt()
            Text(
                text = String.format(
                    "%02d:%02d - %02d:%02d  •  %ds loop",
                    startSec / 60, startSec % 60, endSec / 60, endSec % 60, endSec - startSec
                ),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(Modifier.height(10.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val widthPx = with(density) { maxWidth.toPx() }
                val msToPx = widthPx / safeTotalDuration
                val handleWidthPx = with(density) { 16.dp.toPx() }
                fun commit() = onRangeChanged(localStart.toLong(), localEnd.toLong())

                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )

                val windowStartPx = localStart * msToPx
                val windowWidthPx = (localEnd - localStart) * msToPx

                // Selected window — drag the middle to shift the whole clip
                Box(
                    modifier = Modifier
                        .offset { IntOffset(windowStartPx.toInt(), 0) }
                        .width(with(density) { windowWidthPx.toDp() })
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.28f))
                        .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
                        .pointerInput(audioTrack.audioUri) {
                            detectDragGestures(onDragEnd = { commit() }, onDragCancel = { commit() }) { change, drag ->
                                change.consume()
                                // Defensive: msToPx should now be stable (see parent-screen fix),
                                // but a single glitched frame shouldn't be able to teleport the
                                // window — cap what one drag callback can move it by.
                                val deltaMs = (drag.x / msToPx).coerceIn(-widthPx, widthPx)
                                val duration = (localEnd - localStart).coerceIn(minDurationMs, maxClipDurationMs)
                                val maxStart = (safeTotalDuration - duration).coerceAtLeast(0f)
                                val newStart = (localStart + deltaMs).coerceIn(0f, maxStart)
                                localStart = newStart
                                localEnd = newStart + duration
                                onScrub(localStart.toLong())
                            }
                        }
                )

                if (playheadMs in localStart..localEnd) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((playheadMs * msToPx).toInt() - 1, 0) }
                            .width(2.dp).fillMaxHeight().background(Color.White)
                    )
                }

                // Start handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((windowStartPx - handleWidthPx / 2).toInt(), 0) }
                        .width(with(density) { handleWidthPx.toDp() })
                        .fillMaxHeight()
                        .pointerInput(audioTrack.audioUri) {
                            detectDragGestures(onDragEnd = { commit() }, onDragCancel = { commit() }) { change, drag ->
                                change.consume()
                                val deltaMs = (drag.x / msToPx).coerceIn(-widthPx, widthPx)
                                // Upper bound can't legally go below 0 even if localEnd is
                                // momentarily odd — this is exactly what crashed before.
                                val upperBound = (localEnd - minDurationMs).coerceAtLeast(0f)
                                // Also respect the 60s cap from this edge
                                val lowerBoundFromCap = (localEnd - maxClipDurationMs).coerceAtLeast(0f)
                                localStart = (localStart + deltaMs).coerceIn(lowerBoundFromCap, upperBound)
                                onScrub(localStart.toLong())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                }

                // End handle
                Box(
                    modifier = Modifier
                        .offset { IntOffset((windowStartPx + windowWidthPx - handleWidthPx / 2).toInt(), 0) }
                        .width(with(density) { handleWidthPx.toDp() })
                        .fillMaxHeight()
                        .pointerInput(audioTrack.audioUri) {
                            detectDragGestures(onDragEnd = { commit() }, onDragCancel = { commit() }) { change, drag ->
                                change.consume()
                                val deltaMs = (drag.x / msToPx).coerceIn(-widthPx, widthPx)
                                val lowerBound = localStart + minDurationMs
                                // Respect both the track's real end AND your 60s cap from this edge
                                val upperBound = min(safeTotalDuration, localStart + maxClipDurationMs).coerceAtLeast(lowerBound)
                                localEnd = (localEnd + deltaMs).coerceIn(lowerBound, upperBound)
                                onScrub((localEnd - 1000f).coerceAtLeast(localStart).toLong())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                }
            }
        }
    }
}
private fun pathFromPoints(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryAudioRangeTrimmerWidget(
    audioTrack: SelectedStoryAudio,
    totalDurationMs: Long,
    onRangeChanged: (startMs: Long, endMs: Long) -> Unit,
    onRemoveAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTotalDuration = totalDurationMs.coerceAtLeast(5_000L).toFloat()

    // Convert current ms state to slider range (0f .. safeTotalDuration)
    var sliderRange by remember(audioTrack.startOffsetMs, audioTrack.endOffsetMs) {
        mutableStateOf(
            audioTrack.startOffsetMs.toFloat()..audioTrack.endOffsetMs.toFloat().coerceAtMost(safeTotalDuration)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = audioTrack.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }

                // Selected Clip Duration Label (e.g. "00:10 - 00:50 (40s)")
                val startSec = (sliderRange.start / 1000).toInt()
                val endSec = (sliderRange.endInclusive / 1000).toInt()
                val durationSec = endSec - startSec

                Text(
                    text = String.format("%02d:%02d - %02d:%02d (%ds)", startSec / 60, startSec % 60, endSec / 60, endSec % 60, durationSec),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )

                IconButton(
                    onClick = onRemoveAudio,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove music",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Two-thumb Range Slider for Start and End Selection
            RangeSlider(
                value = sliderRange,
                onValueChange = { newRange ->
                    // Minimum 1-second selection guard
                    if (newRange.endInclusive - newRange.start >= 1000f) {
                        sliderRange = newRange
                        onRangeChanged(newRange.start.toLong(), newRange.endInclusive.toLong())
                    }
                },
                valueRange = 0f..safeTotalDuration,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E676),
                    activeTrackColor = Color(0xFF00E676),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
    }
}

/** Pulls a real still frame directly from the video file — used only to stand in
 * for the live (uncapturable) player during export. */
private fun extractVideoFrame(context: android.content.Context, uri: Uri): Uri? {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        val file = File(context.cacheDir, "video_frame_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out -> frame.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        Uri.fromFile(file)
    } catch (e: Exception) {
        Log.e("STORY_DEBUG", "extractVideoFrame failed", e)
        null
    } finally {
        retriever.release()
    }
}
@Composable
private fun StoryBackgroundLayer(
    background: StoryBackground,
    isCapturing: Boolean = false,
    capturedVideoFrameUri: Uri? = null,
    muteOriginalAudio: Boolean = false
) {
    when (background) {
        is StoryBackground.Media -> {
            if (background.isVideo) {
                if (isCapturing && capturedVideoFrameUri != null) {
                    AsyncImage(
                        model = capturedVideoFrameUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    VideoBackground(uri = background.uri, muteOriginalAudio = muteOriginalAudio)
                }
            } else {
                AsyncImage(
                    // Coil defaults to Bitmap.Config.HARDWARE on API 26+ for fast GPU-only
                    // rendering — but this screen gets flattened via view.drawToBitmap(),
                    // which draws through a SOFTWARE canvas. Software canvases can't draw
                    // hardware bitmaps at all — that's this exact crash. allowHardware(false)
                    // forces a normal ARGB_8888 bitmap for this image specifically.
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(background.uri)
                        .allowHardware(false)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
        is StoryBackground.SolidColor -> Box(Modifier.fillMaxSize().background(background.color))
        is StoryBackground.Gradient -> Box(
            Modifier.fillMaxSize().background(ColorBrush.linearGradient(background.colors))
        )
    }
}

@Composable
private fun VideoBackground(uri: Uri, muteOriginalAudio: Boolean = false) {
    val context = LocalContext.current
    // Keyed on `uri` now — previously this player was remembered ONCE regardless
    // of uri changes, so switching videos left the old player's codec threads
    // and the new uri out of sync. Likely cause of the "dead thread" MediaCodec
    // warnings when reopening a video.
    val exoPlayer = remember(uri) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    LaunchedEffect(muteOriginalAudio) {
        exoPlayer.volume = if (muteOriginalAudio) 0f else 1f
    }
    DisposableEffect(uri) { onDispose { exoPlayer.release() } } // keyed the same as above, so old player releases precisely when uri changes
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { androidx.media3.ui.PlayerView(it).apply { player = exoPlayer; useController = false } },
        update = { view -> view.player = exoPlayer },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun DraggableTextOverlay(
    overlay: TextOverlay,
    interactive: Boolean,
    onTap: () -> Unit,
    onTransform: (Offset, Float, Float) -> Unit,
    onPositioned: (Rect) -> Unit = {}
) {
    val textStyle = when (overlay.backgroundStyle) {
        TextBackgroundStyle.SOLID_PILL -> Modifier
            .background(overlay.color, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        TextBackgroundStyle.TRANSLUCENT_PILL -> Modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        TextBackgroundStyle.TRANSPARENT -> Modifier.padding(6.dp)
    }

    val textColor = if (overlay.backgroundStyle == TextBackgroundStyle.SOLID_PILL) {
        if (overlay.color == Color.White) Color.Black else Color.White
    } else overlay.color

    Box(
        modifier = Modifier
            .offset { IntOffset(overlay.offset.x.toInt(), overlay.offset.y.toInt()) }
            .graphicsLayer(
                scaleX = overlay.scale,
                scaleY = overlay.scale,
                rotationZ = overlay.rotation
            )
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .then(
                if (interactive) {
                    Modifier
                        .pointerInput(overlay.id) {
                            detectTapGestures(onTap = { onTap() })
                        }
                        .pointerInput(overlay.id) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                onTransform(pan, zoom, rotation)
                            }
                        }
                } else Modifier
            )
            .then(textStyle)
    ) {
        Text(
            text = overlay.text,
            color = textColor,
            fontSize = overlay.fontSizeSp.sp,
            fontFamily = overlay.fontFamily,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DraggableStickerOverlay(
    sticker: StickerOverlay,
    interactive: Boolean,
    onTransform: (Offset, Float, Float) -> Unit,
    onPositioned: (Rect) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(sticker.offset.x.toInt(), sticker.offset.y.toInt()) }
            .graphicsLayer(
                scaleX = sticker.scale,
                scaleY = sticker.scale,
                rotationZ = sticker.rotation
            )
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .then(
                if (interactive) {
                    Modifier.pointerInput(sticker.id) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onTransform(pan, zoom, rotation)
                        }
                    }
                } else Modifier
            )
    ) {
        Text(
            text = sticker.emoji,
            fontSize = 54.sp
        )
    }
}


@Composable
private fun StoryEditorTopBar(
    background: StoryBackground,
    activeTool: EditorTool,
    hasAudio: Boolean,
    onToolSelected: (EditorTool) -> Unit,
    onAudioClick: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onCropClick: () -> Unit,
    muteOriginalVideoAudio: Boolean,
    onToggleMuteOriginalAudio: () -> Unit,
    onClose: () -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Floating Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0x66000000), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Floating Center Tools Bar
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x66000000))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolIconButton(Icons.Default.Brush, activeTool == EditorTool.DRAW) { onToolSelected(EditorTool.DRAW) }
            ToolIconButton(Icons.Default.TextFields, activeTool == EditorTool.TEXT) { onToolSelected(EditorTool.TEXT) }
            ToolIconButton(Icons.Default.EmojiEmotions, activeTool == EditorTool.STICKER) { onToolSelected(EditorTool.STICKER) }

            // Audio Picker Tool Button (Highlights when audio is attached)
            ToolIconButton(
                icon = Icons.Default.MusicNote,
                selected = hasAudio,
                onClick = onAudioClick
            )

            // Re-crop button (Enabled only for images — video crop isn't built yet,
            // see the note in chat about why that's a separate follow-up)
            if (background is StoryBackground.Media && !background.isVideo) {
                ToolIconButton(Icons.Default.Crop, selected = false, onClick = onCropClick)
            }

            // Mute-original-audio toggle — video backgrounds only. The audio-picker
            // button above doubles as "pick a replacement track" for video too.
            if (background is StoryBackground.Media && background.isVideo) {
                ToolIconButton(
                    icon = if (muteOriginalVideoAudio) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    selected = muteOriginalVideoAudio,
                    onClick = onToggleMuteOriginalAudio
                )
            }

            // Background swatch picker — only makes sense for text/gradient stories
            if (background !is StoryBackground.Media) {
                ToolIconButton(
                    icon = Icons.Default.Palette,
                    selected = activeTool == EditorTool.BACKGROUND,
                    onClick = { onToolSelected(EditorTool.BACKGROUND) }
                )
            }

            if (canUndo) {
                IconButton(onClick = onUndo, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Action Save Button
        IconButton(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier
                .size(42.dp)
                .background(
                    brush = ColorBrush.horizontalGradient(
                        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
                    ),
                    shape = CircleShape
                )
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
@Composable
private fun ToolIconButton(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color(0xFFFFD700) else Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ColorAndWidthPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit
) {
    val palette = listOf(
        Color.White, Color.Black, Color(0xFFFF2A6D), Color(0xFFFF9F1C),
        Color(0xFFFFE600), Color(0xFF05FFA1), Color(0xFF00F5D4), Color(0xFF00BBF9), Color(0xFF9B5DE5)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xD9121212))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(20.dp)
            .navigationBarsPadding()
    ) {
        Slider(
            value = strokeWidth,
            onValueChange = onStrokeWidthChange,
            valueRange = 4f..40f,
            colors = SliderDefaults.colors(
                thumbColor = selectedColor,
                activeTrackColor = selectedColor.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(palette) { color ->
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 36.dp else 30.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickableSimple { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun BackgroundPicker(
    selected: StoryBackground,
    onBackgroundPicked: (StoryBackground) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xD9121212))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(defaultSolidBackgrounds) { color ->
            val isSelected = (selected as? StoryBackground.SolidColor)?.color == color
            Box(
                modifier = Modifier
                    .size(if (isSelected) 42.dp else 36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickableSimple { onBackgroundPicked(StoryBackground.SolidColor(color)) }
            )
        }
        items(defaultGradients) { gradient ->
            val isSelected = (selected as? StoryBackground.Gradient)?.colors == gradient
            Box(
                modifier = Modifier
                    .size(if (isSelected) 42.dp else 36.dp)
                    .clip(CircleShape)
                    .background(ColorBrush.linearGradient(gradient))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickableSimple { onBackgroundPicked(StoryBackground.Gradient(gradient)) }
            )
        }
    }
}

@Composable
private fun StickerPicker(onStickerPicked: (String) -> Unit) {
    val emojis = listOf("🔥", "❤️", "✨", "😂", "👑", "🚀", "💯", "🎉", "😎", "⚡", "🌟", "😍")
    LazyRow(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xD9121212))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(emojis) { emoji ->
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.clickableSimple { onStickerPicked(emoji) }
            )
        }
    }
}

@Composable
private fun TextEditDialog(
    initialText: String,
    initialColor: Color,
    initialFontSize: Float,
    initialFontFamily: FontFamily,
    initialBgStyle: TextBackgroundStyle,
    onDismiss: () -> Unit,
    onConfirm: (String, Color, Float, FontFamily, TextBackgroundStyle) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedFontSize by remember { mutableFloatStateOf(initialFontSize) }
    var selectedFont by remember { mutableStateOf(initialFontFamily) }
    var selectedBgStyle by remember { mutableStateOf(initialBgStyle) }

    val fonts = listOf(
        "Standard" to FontFamily.Default,
        "Serif" to FontFamily.Serif,
        "Mono" to FontFamily.Monospace,
        "Cursive" to FontFamily.Cursive
    )

    val colors = listOf(
        Color.White, Color.Black, Color(0xFFFF2A6D), Color(0xFFFF9F1C),
        Color(0xFFFFE600), Color(0xFF05FFA1), Color(0xFF00BBF9), Color(0xFF9B5DE5)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF218181C),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar: Style Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedBgStyle) {
                            TextBackgroundStyle.SOLID_PILL -> "Background: Solid"
                            TextBackgroundStyle.TRANSLUCENT_PILL -> "Background: Glass"
                            TextBackgroundStyle.TRANSPARENT -> "Background: None"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    // Button to cycle through background styles or remove background completely
                    IconButton(
                        onClick = {
                            selectedBgStyle = when (selectedBgStyle) {
                                TextBackgroundStyle.TRANSLUCENT_PILL -> TextBackgroundStyle.SOLID_PILL
                                TextBackgroundStyle.SOLID_PILL -> TextBackgroundStyle.TRANSPARENT
                                TextBackgroundStyle.TRANSPARENT -> TextBackgroundStyle.TRANSLUCENT_PILL
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Cycle Text Background Style",
                            tint = if (selectedBgStyle != TextBackgroundStyle.TRANSPARENT) Color(0xFFFFD700) else Color.White
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Text Input Field Preview
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type something...", color = Color.Gray) },
                    textStyle = TextStyle(
                        color = selectedColor,
                        fontSize = selectedFontSize.sp,
                        fontFamily = selectedFont,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFFFF512F),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // 1. Text Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.width(36.dp)
                    )
                    Slider(
                        value = selectedFontSize,
                        onValueChange = { selectedFontSize = it },
                        valueRange = 16f..56f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF512F),
                            activeTrackColor = Color(0xFFFF512F),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 2. Font Style Picker
                Text(
                    text = "Font",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(fonts) { (name, font) ->
                        val isSelected = font == selectedFont
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.1f))
                                .clickableSimple { selectedFont = font }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontFamily = font,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Text Color Palette
                Text(
                    text = "Color",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickableSimple { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Dialog Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(text, selectedColor, selectedFontSize, selectedFont, selectedBgStyle) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF512F)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}


private fun saveStoryBitmapToAppStorage(
    context: android.content.Context,
    bitmap: Bitmap
): SavedMedia? {
    val tempFile = File(
        context.cacheDir,
        "story_export_${System.currentTimeMillis()}.jpg"
    )


    return try {
        tempFile.outputStream().use { out ->
            val compressed = bitmap.compress(
                Bitmap.CompressFormat.PNG, // testing whether MediaTek's DRM hook is JPEG-specific
                100,
                out
            )

        }



        val savedMedia = saveMediaToAppStorageNext(
            context = context,
            sourceUri = Uri.fromFile(tempFile),
            storageType = MediaCategory.STORY_IMAGE,
            visibility = MediaVisibility.PRIVATE,
            overrideMimeType = "image/jpeg"
        )


        if (savedMedia != null) {
        } else {
            Log.e("STORY_DEBUG", "saveMediaToAppStorageNext() returned NULL")
        }

        savedMedia
    } catch (e: Exception) {
        Log.e("STORY_DEBUG", "FAILED saving story bitmap", e)
        null
    } finally {
        tempFile.delete()
    }
}
