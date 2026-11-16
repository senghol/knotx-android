package com.senghol.knotx.editor

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.net.Uri
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.CaptionBar
import com.senghol.knotx.ui.components.DisappearingMessagesPickerDialog
import com.senghol.knotx.ui.contactPicker.ContactPickerScreen
import com.senghol.knotx.ui.contactPicker.PickedContact
import com.senghol.knotx.ui.contactPicker.PickerSources
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Standalone video editor (trim + mute). Same send flow as ImageEditorScreen.
 *
 * @param sourceUris     videos to edit (read in place, never modified)
 * @param maxDurationMs  longest allowed clip after trimming, 0 = unlimited
 * @param onDone         one finished mp4 per input (cacheDir). The caller owns and deletes them.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    modifier: Modifier = Modifier,
    sourceUris: List<Uri>,
    initialRecipients: List<PickedContact>,
    pickerSources: PickerSources = PickerSources(numberContacts = true, groups = true, deviceContacts = true, usernameContacts = true),
    defaultDisappearSeconds: Int = 0,
    showDisappearButton: Boolean = true,
    maxDurationMs: Long = 0L,
    onDone: (
        uris: List<Uri>,
        message: String,
        disappearSeconds: Int,
        recipients: List<PickedContact>
    ) -> Unit,
    onCancel: () -> Unit
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
    val vm: VideoEditorViewModel = viewModel(
        viewModelStoreOwner = owner,
        factory = remember(context) {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    VideoEditorViewModel(context.applicationContext as Application) as T
            }
        }
    )
    LaunchedEffect(sourceUris) { vm.load(sourceUris, defaultDisappearSeconds, initialRecipients, maxDurationMs) }

    val clip = vm.current
    var showDisappearPicker by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    var tool by remember { mutableStateOf(EditorTool.NONE) }
    var brush by remember { mutableStateOf(BrushKind.PEN) }
    var brushColor by remember { mutableIntStateOf(-1) }
    var brushSize by remember { mutableFloatStateOf(0.012f) }
    var showTextDialog by remember { mutableStateOf(false) }
    var editingOverlay by remember { mutableStateOf<Overlay?>(null) }

    BackHandler { if (tool != EditorTool.NONE) tool = EditorTool.NONE else onCancel() }

    /* ---------------- player ---------------- */
    val player = remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF } }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }

    LaunchedEffect(clip?.uri) {
        val c = clip ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(c.uri))
        player.prepare()
        player.playWhenReady = true
    }
    LaunchedEffect(clip?.muted) { player.volume = if (clip?.muted == true) 0f else 1f }

    // Keeps playback inside the trimmed range (loops from the start handle)
    LaunchedEffect(clip?.uri, clip?.ready, clip?.startMs, clip?.endMs) {
        val c = clip ?: return@LaunchedEffect
        if (!c.ready) return@LaunchedEffect
        while (true) {
            val p = player.currentPosition
            position = p
            val ended = player.playbackState == Player.STATE_ENDED
            if (ended || (player.isPlaying && p >= c.endMs)) player.seekTo(c.startMs)
            delay(50)
        }
    }

    fun send() {
        focusManager.clearFocus()
        if (vm.recipients.isEmpty()) { showPicker = true; return }
        player.pause()
        vm.exportAll { files ->
            if (files != null) onDone(files, vm.caption.trim(), vm.disappearSeconds, vm.recipients)
            else Toast.makeText(context, "Couldn't export the videos", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(ui().bg)
            .pointerInput(Unit) { detectTapGestures { } }   // blocks taps from reaching the screen below
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundIcon(Icons.Default.Close, "Close", true, onCancel)
                RoundIcon(
                    if (clip?.muted == true) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    if (clip?.muted == true) "Unmute" else "Mute",
                    clip?.ready == true,
                    vm::toggleMute
                )
            }

            // Video
            // Video
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                when {
                    clip != null && clip.ready -> VideoCanvas(
                        player = player,
                        clip = clip,
                        tool = tool,
                        isPlaying = isPlaying,
                        brush = brush,
                        brushColor = brushColor,
                        brushSize = brushSize,
                        vm = vm,
                        onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
                        onEditText = { editingOverlay = it }
                    )
                    vm.loadFailed -> Text("Couldn't open this video", color = ui().text)
                    else -> CircularProgressIndicator(color = ui().text)
                }
            }

            if (vm.clips.size > 1) {
                ImageStrip(thumbs = vm.thumbs, selected = vm.index, onSelect = vm::select)
            }

            Column(Modifier.navigationBarsPadding().imePadding()) {
                if (clip != null && clip.ready) {
                    when (tool) {
                        EditorTool.DRAW -> DrawPanel(
                            brush = brush,
                            onBrush = { brush = it },
                            color = brushColor,
                            onColor = { brushColor = it },
                            size = brushSize,
                            onSize = { brushSize = it }
                        )
                        EditorTool.STICKERS -> StickerPanel { emoji ->
                            vm.addOverlay(
                                Overlay(
                                    text = emoji, colorArgb = -1, size = 0.14f,
                                    x = 0.5f, y = 0.5f, isSticker = true
                                )
                            )
                            tool = EditorTool.NONE
                        }
                        else -> if (tool == EditorTool.CROP) TrimPanel(
                            clip = clip,
                            frames = vm.frames,
                            position = position,
                            maxMs = if (maxDurationMs > 0) maxDurationMs else Long.MAX_VALUE,
                            onChange = { s, e, handleMs ->
                                vm.setTrim(s, e)
                                player.seekTo(handleMs)
                            }
                        )
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
                    enabled = !vm.isExporting && clip?.ready == true,
                    onSend = ::send,
                    showTimer = showDisappearButton,
                    timerActive = vm.disappearSeconds > 0,
                    onTimer = { showDisappearPicker = true }
                )

                VideoToolDock(selected = tool) { t ->
                    when (t) {
                        EditorTool.TEXT -> { tool = EditorTool.NONE; showTextDialog = true }
                        else -> tool = if (tool == t) EditorTool.NONE else t
                    }
                }
            }
        }

        if (showTextDialog || editingOverlay != null) {
            TextEditDialog(
                initial = editingOverlay,
                onDismiss = { showTextDialog = false; editingOverlay = null },
                onConfirm = { text, color, size, font, bg ->
                    val target = editingOverlay
                    if (text.isNotBlank()) {
                        if (target != null) {
                            vm.updateOverlay(target.id) {
                                it.copy(text = text, colorArgb = color, size = size, font = font, bg = bg)
                            }
                        } else {
                            vm.addOverlay(
                                Overlay(text = text, colorArgb = color, size = size, font = font, bg = bg, x = 0.5f, y = 0.5f)
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
                        if (p != null && p.second > 1) "Preparing video ${p.first} of ${p.second}…" else "Preparing video…",
                        color = Color.White
                    )
                }
            }
        }
    }
}

/* ================================================================== */
/* Trim                                                                */
/* ================================================================== */

private const val MIN_TRIM_MS = 1000L

private fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun TrimPanel(
    clip: VideoClip,
    frames: List<Bitmap>,
    position: Long,
    maxMs: Long,
    onChange: (startMs: Long, endMs: Long, handleMs: Long) -> Unit
) {
    PanelSurface {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trim", color = ui().text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "${fmt(clip.startMs)} – ${fmt(clip.endMs)}  •  ${fmt(clip.endMs - clip.startMs)}",
                color = ui().text.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        TrimBar(
            frames = frames,
            durationMs = clip.durationMs,
            startMs = clip.startMs,
            endMs = clip.endMs,
            playheadMs = position,
            maxMs = maxMs,
            onChange = onChange
        )
    }
}

@Composable
private fun TrimBar(
    frames: List<Bitmap>,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    maxMs: Long,
    onChange: (Long, Long, Long) -> Unit
) {
    val density = LocalDensity.current
    val handleW = 14.dp
    val barH = 54.dp
    val hwPx = with(density) { handleW.toPx() }

    val start by rememberUpdatedState(startMs)
    val end by rememberUpdatedState(endMs)
    val change by rememberUpdatedState(onChange)

    BoxWithConstraints(Modifier.fillMaxWidth().height(barH)) {
        val totalPx = with(density) { maxWidth.toPx() }
        val trackPx = (totalPx - 2 * hwPx).coerceAtLeast(1f)
        val msPerPx = durationMs / trackPx

        val startX = startMs.toFloat() / durationMs * trackPx            // left edge of start handle
        val endX = hwPx + endMs.toFloat() / durationMs * trackPx         // left edge of end handle

        // Frames
        Row(Modifier.padding(horizontal = handleW).fillMaxSize().clip(RoundedCornerShape(4.dp))) {
            if (frames.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(ui().text.copy(alpha = 0.1f)))
            } else {
                frames.forEach { f ->
                    Image(
                        bitmap = f.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        // Dim outside the selection
        Box(
            Modifier
                .offset { IntOffset(hwPx.roundToInt(), 0) }
                .width(with(density) { startX.toDp() })
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.6f))
        )
        Box(
            Modifier
                .offset { IntOffset((endX + hwPx).roundToInt(), 0) }
                .width(with(density) { (totalPx - endX - hwPx).coerceAtLeast(0f).toDp() })
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Selection frame (top and bottom lines)
        Box(
            Modifier
                .offset { IntOffset((startX + hwPx).roundToInt(), 0) }
                .width(with(density) { (endX - startX - hwPx).coerceAtLeast(0f).toDp() })
                .fillMaxHeight()
                .border(2.dp, Accent)
        )

        // Playhead
        if (playheadMs in startMs..endMs) {
            val px = hwPx + playheadMs.toFloat() / durationMs * trackPx
            Box(
                Modifier
                    .offset { IntOffset(px.roundToInt() - 1, 0) }
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        // Start handle
        TrimHandle(
            Modifier
                .offset { IntOffset(startX.roundToInt(), 0) }
                .pointerInput(durationMs, trackPx, maxMs) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val lo = max(0L, if (maxMs == Long.MAX_VALUE) 0L else end - maxMs)
                        val hi = end - MIN_TRIM_MS
                        val ns = (start + (drag.x * msPerPx).toLong()).coerceIn(lo, max(lo, hi))
                        change(ns, end, ns)
                    }
                }
        )

        // End handle
        TrimHandle(
            Modifier
                .offset { IntOffset(endX.roundToInt(), 0) }
                .pointerInput(durationMs, trackPx, maxMs) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val lo = start + MIN_TRIM_MS
                        val hi = min(durationMs, if (maxMs == Long.MAX_VALUE) durationMs else start + maxMs)
                        val ne = (end + (drag.x * msPerPx).toLong()).coerceIn(min(lo, hi), hi)
                        change(start, ne, ne)
                    }
                }
        )
    }
}

@Composable
private fun TrimHandle(modifier: Modifier) {
    Box(
        modifier
            .width(14.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(Accent),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(2.dp).height(18.dp).background(Color.White, RoundedCornerShape(1.dp)))
    }
}