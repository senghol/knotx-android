package com.senghol.knotx.editor

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.senghol.knotx.ui.contactPicker.PickedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.min

data class VideoClip(
    val uri: Uri,
    val durationMs: Long = 0L,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val muted: Boolean = false,
    val ready: Boolean = false,
    val width: Int = 0,                       // display-oriented frame size
    val height: Int = 0,
    val overlays: List<Overlay> = emptyList(),
    val strokes: List<StrokeLayer> = emptyList()
)

@UnstableApi
class VideoEditorViewModel(private val app: Application) : AndroidViewModel(app) {

    val clips = mutableStateListOf<VideoClip>()
    val thumbs = mutableStateListOf<Bitmap?>()          // first frame of each clip (for the strip)

    var index by mutableIntStateOf(0); private set
    var frames by mutableStateOf<List<Bitmap>>(emptyList()); private set   // frames for the trim bar (current clip)

    var caption by mutableStateOf("")
    var disappearSeconds by mutableIntStateOf(0)
    var recipients by mutableStateOf<List<PickedContact>>(emptyList())

    var loadFailed by mutableStateOf(false); private set
    var isExporting by mutableStateOf(false); private set
    var exportProgress by mutableStateOf<Pair<Int, Int>?>(null); private set

    private var loadedKey: List<Uri>? = null
    private val frameCache = mutableMapOf<Int, List<Bitmap>>()

    val current: VideoClip? get() = clips.getOrNull(index)

    /* ---------------------------- loading ---------------------------- */

    fun load(
        uris: List<Uri>,
        defaultDisappear: Int,
        initialRecipients: List<PickedContact>,
        maxDurationMs: Long
    ) {
        if (loadedKey == uris) return
        loadedKey = uris
        disappearSeconds = defaultDisappear
        recipients = initialRecipients
        clips.clear(); thumbs.clear(); frameCache.clear()
        frames = emptyList(); index = 0; loadFailed = false
        clips.addAll(uris.map { VideoClip(it) })
        thumbs.addAll(uris.map { null })

        viewModelScope.launch {
            uris.forEachIndexed { i, uri ->
                val info = withContext(Dispatchers.IO) { probe(uri) }
                if (info == null) { loadFailed = true; return@forEachIndexed }
                val dur = info.dur
                val thumb = info.thumb
                val end = if (maxDurationMs > 0) min(dur, maxDurationMs) else dur
                clips[i] = clips[i].copy(
                    durationMs = dur, startMs = 0L, endMs = end, ready = true,
                    width = info.w, height = info.h
                )
                thumbs[i] = thumb
                if (i == index) loadFrames(i)
            }
        }
    }

    private class Probe(val dur: Long, val w: Int, val h: Int, val thumb: Bitmap?)

    private fun probe(uri: Uri): Probe? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(app, uri)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (dur <= 0L) return null
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) { val t = w; w = h; h = t }
            if (w <= 0 || h <= 0) return null
            Probe(dur, w, h, r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.small(240))
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    private fun loadFrames(i: Int) {
        frameCache[i]?.let { frames = it; return }
        val clip = clips.getOrNull(i) ?: return
        if (!clip.ready) return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(app, clip.uri)
                    val n = 10
                    (0 until n).mapNotNull { k ->
                        val us = clip.durationMs * 1000L * k / n
                        r.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.small(120)
                    }
                } catch (e: Exception) {
                    emptyList()
                } finally {
                    runCatching { r.release() }
                }
            }
            frameCache[i] = list
            if (index == i) frames = list
        }
    }

    private fun Bitmap.small(w: Int): Bitmap {
        val h = (w * height / width.toFloat()).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, w, h, true)
    }

    /* ---------------------------- editing ---------------------------- */

    fun select(i: Int) {
        if (i !in clips.indices) return
        index = i
        frames = frameCache[i] ?: emptyList()
        loadFrames(i)
    }

    fun setTrim(startMs: Long, endMs: Long) {
        val c = clips.getOrNull(index) ?: return
        clips[index] = c.copy(startMs = startMs, endMs = endMs)
    }

    fun toggleMute() {
        val c = clips.getOrNull(index) ?: return
        clips[index] = c.copy(muted = !c.muted)
    }

    private fun updateCurrent(f: (VideoClip) -> VideoClip) {
        val c = clips.getOrNull(index) ?: return
        clips[index] = f(c)
    }

    fun addOverlay(o: Overlay) = updateCurrent { it.copy(overlays = it.overlays + o) }

    fun updateOverlay(id: String, change: (Overlay) -> Overlay) = updateCurrent { c ->
        c.copy(overlays = c.overlays.map { if (it.id == id) change(it) else it })
    }

    fun removeOverlay(id: String) = updateCurrent { c ->
        c.copy(overlays = c.overlays.filterNot { it.id == id })
    }

    fun addStroke(s: StrokeLayer) = updateCurrent { it.copy(strokes = it.strokes + s) }

    fun removeRecipient(userNumber: String) {
        if (recipients.size > 1) recipients = recipients.filter { it.userNumber != userNumber }
    }

    /* ---------------------------- export ---------------------------- */

    /** Calls back with one mp4 per clip (cacheDir), or null on failure. Caller owns and deletes the files. */
    fun exportAll(onResult: (List<Uri>?) -> Unit) {
        if (isExporting) return
        isExporting = true
        viewModelScope.launch {
            val out = mutableListOf<File>()
            var ok = true
            val list = clips.toList()
            for ((i, c) in list.withIndex()) {
                exportProgress = (i + 1) to list.size
                val f = File(app.cacheDir, "vid_${System.nanoTime()}.mp4")
                val untouched = c.startMs == 0L && c.endMs >= c.durationMs && !c.muted &&
                        c.overlays.isEmpty() && c.strokes.isEmpty()
                val overlay = if (untouched) null else withContext(Dispatchers.IO) { renderOverlayBitmap(c) }
                val done = if (untouched) withContext(Dispatchers.IO) { copy(c.uri, f) } else transform(c, f, overlay)
                if (!done) { f.delete(); ok = false; break }
                out += f
            }
            isExporting = false
            exportProgress = null
            if (ok) onResult(out.map { Uri.fromFile(it) })
            else { out.forEach { it.delete() }; onResult(null) }
        }
    }

    /** Nothing changed, so no re-encode: just copy the original. */
    private fun copy(uri: Uri, dest: File): Boolean = try {
        app.contentResolver.openInputStream(uri)?.use { i -> dest.outputStream().use { o -> i.copyTo(o) } } != null
    } catch (e: Exception) { false }

    private suspend fun transform(clip: VideoClip, dest: File, overlay: Bitmap?): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val item = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()
            val builder = EditedMediaItem.Builder(item).setRemoveAudio(clip.muted)
            if (overlay != null) {
                val effect = OverlayEffect(
                    ImmutableList.of<TextureOverlay>(BitmapOverlay.createStaticBitmapOverlay(overlay))
                )
                builder.setEffects(Effects(emptyList(), listOf(effect)))
            }
            val edited = builder.build()
            val transformer = Transformer.Builder(app)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (cont.isActive) cont.resume(false)
                    }
                })
                .build()
            transformer.start(edited, dest.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
    }
}