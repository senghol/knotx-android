package com.senghol.knotx.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senghol.knotx.editor.Adjustments
import com.senghol.knotx.editor.CropState
import com.senghol.knotx.editor.EditorState
import com.senghol.knotx.editor.FilterPreset
import com.senghol.knotx.editor.ImageExporter
import com.senghol.knotx.editor.Overlay
import com.senghol.knotx.editor.StrokeLayer
import com.senghol.knotx.editor.decodeOriented
import com.senghol.knotx.ui.contactPicker.PickedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImageEditorViewModel(app: Application) : AndroidViewModel(app) {

    data class History(val canUndo: Boolean = false, val canRedo: Boolean = false)

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _history = MutableStateFlow(History())
    val history: StateFlow<History> = _history.asStateFlow()

    /** Downsampled, EXIF-oriented preview. The only bitmap held during editing. */
    var preview by mutableStateOf<Bitmap?>(null)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    /** Message sent along with the image. Not part of undo history. */
    var caption by mutableStateOf("")

    var isExporting by mutableStateOf(false)
        private set

    private class Session(val uri: Uri) {
        var state = EditorState()
        val undo = ArrayDeque<EditorState>()
        val redo = ArrayDeque<EditorState>()
        var lastKey: String? = null
        var lastAt = 0L
    }

    private var sessions: List<Session> = emptyList()
    private var loadedKey: List<Uri>? = null
    private var previewJob: Job? = null

    var uris by mutableStateOf<List<Uri>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set

    /** Small thumbnails for the strip, one per image (null until decoded). */
    val thumbs = mutableStateListOf<Bitmap?>()

    /** One disappearing-message timer for the whole send. 0 = off. */
    var disappearSeconds by mutableIntStateOf(0)

    /** HD export on/off. Applies to the whole send. Resets to off each time the editor opens. */
    var hd by mutableStateOf(false)

    /** Who the images go to. Starts as the contacts passed in, editable through the picker. */
    var recipients by mutableStateOf<List<PickedContact>>(emptyList())

    fun removeRecipient(id: String) {
        if (recipients.size > 1) recipients = recipients.filterNot { it.userNumber == id }
    }

    /** (current, total) while exporting. */
    var exportProgress by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    /* ---------------- loading ---------------- */

    fun load(list: List<Uri>, defaultDisappearSeconds: Int, initialRecipients: List<PickedContact>) {
        if (list.isEmpty() || loadedKey == list) return
        loadedKey = list
        disappearSeconds = defaultDisappearSeconds
        recipients = initialRecipients.distinctBy { it.userNumber }
        sessions = list.map { Session(it) }
        uris = list
        index = 0
        thumbs.clear()
        repeat(list.size) { thumbs.add(null) }
        _state.value = sessions[0].state
        publishHistory()
        showPreview(0)
        viewModelScope.launch {
            list.forEachIndexed { i, u ->
                val t = withContext(Dispatchers.IO) {
                    runCatching { decodeOriented(getApplication(), u, THUMB_MAX) }.getOrNull()
                }
                if (i < thumbs.size) thumbs[i] = t
            }
        }
    }

    fun select(i: Int) {
        if (i == index || i !in sessions.indices) return
        index = i
        _state.value = sessions[i].state
        publishHistory()
        showPreview(i)
    }

    private fun showPreview(i: Int) {
        previewJob?.cancel()
        preview = null
        loadFailed = false
        val uri = sessions[i].uri
        previewJob = viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching { decodeOriented(getApplication(), uri, PREVIEW_MAX) }.getOrNull()
            }
            preview = bmp
            loadFailed = bmp == null
        }
    }

    /* ---------------- history ---------------- */

    /**
     * Applies [block] and records an undo step. Calls with the same [coalesceKey]
     * within 700 ms merge into one step, so a slider drag or a gesture is one undo.
     */
    fun edit(coalesceKey: String? = null, block: (EditorState) -> EditorState) {
        val s = sessions.getOrNull(index) ?: return
        val now = SystemClock.uptimeMillis()
        val cur = _state.value
        val merge = coalesceKey != null && coalesceKey == s.lastKey && now - s.lastAt < 700
        if (!merge) {
            s.undo.addLast(cur)
            if (s.undo.size > MAX_HISTORY) s.undo.removeFirst()
            s.redo.clear()
        }
        s.lastKey = coalesceKey
        s.lastAt = now
        val next = block(cur)
        s.state = next
        _state.value = next
        publishHistory()
    }

    fun undo() {
        val s = sessions.getOrNull(index) ?: return
        if (s.undo.isEmpty()) return
        s.redo.addLast(_state.value)
        s.state = s.undo.removeLast()
        _state.value = s.state
        s.lastKey = null
        publishHistory()
    }

    fun redo() {
        val s = sessions.getOrNull(index) ?: return
        if (s.redo.isEmpty()) return
        s.undo.addLast(_state.value)
        s.state = s.redo.removeLast()
        _state.value = s.state
        s.lastKey = null
        publishHistory()
    }

    private fun publishHistory() {
        val s = sessions.getOrNull(index)
        _history.value = History(s?.undo?.isNotEmpty() == true, s?.redo?.isNotEmpty() == true)
    }

    /* ---------------- edits ---------------- */

    fun setCrop(c: CropState) = edit { it.copy(crop = c) }

    fun setAdjust(a: Adjustments) = edit("adjust") { it.copy(adjust = a) }
    fun resetAdjust() = edit { it.copy(adjust = Adjustments()) }

    fun setFilter(f: FilterPreset) = edit { it.copy(filter = f, filterStrength = 1f) }
    fun setFilterStrength(v: Float) = edit("fstrength") { it.copy(filterStrength = v) }

    fun addStroke(s: StrokeLayer) = edit { it.copy(strokes = it.strokes + s) }

    fun addOverlay(o: Overlay) = edit { it.copy(overlays = it.overlays + o) }

    fun updateOverlay(id: String, coalesceKey: String? = "ov:$id", block: (Overlay) -> Overlay) =
        edit(coalesceKey) { s -> s.copy(overlays = s.overlays.map { if (it.id == id) block(it) else it }) }

    fun removeOverlay(id: String) = edit { s -> s.copy(overlays = s.overlays.filterNot { it.id == id }) }

    /* ---------------- export ---------------- */

    /**
     * Exports every image, one JPEG each, in order. [onResult] runs on the main thread:
     * the list of files on success, or null if any image failed (partial files are deleted).
     */
    fun exportAll(onResult: (List<Uri>?) -> Unit) {
        if (isExporting || sessions.isEmpty()) return
        isExporting = true
        val hdNow = hd
        val jobs = sessions.map { it.uri to it.state }
        viewModelScope.launch {
            val done = ArrayList<Uri>()
            var failed = false
            for ((i, job) in jobs.withIndex()) {
                exportProgress = (i + 1) to jobs.size
                val r = withContext(Dispatchers.Default) {
                    runCatching { ImageExporter.export(getApplication(), job.first, job.second, hdNow) }.getOrNull()
                }
                if (r == null) { failed = true; break }
                done.add(r)
            }
            if (failed) done.forEach { u -> u.path?.let { File(it).delete() } }
            isExporting = false
            exportProgress = null
            onResult(if (failed) null else done)
        }
    }

    private companion object {
        const val PREVIEW_MAX = 2048
        const val MAX_HISTORY = 50
        const val THUMB_MAX = 160
    }
}