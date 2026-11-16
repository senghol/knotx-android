package com.senghol.knotx.ui.components
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.Player

import com.senghol.knotx.ui.viewmodel.SandeshViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File



// Keep your existing `package ...` line and your SandeshViewModel import at the top of this file.

import android.content.ContentValues

import android.media.MediaScannerConnection

import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.media3.common.C

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import android.view.View

/**
 * Full-screen video viewer (standard player controls, modern top bar).
 *
 * @param disappearSeconds  the message's disappear timer. "Save to gallery" is only shown when it is 0.
 * @param onEdit            called with the current video's Uri; null hides the Edit button.
 *                          The screen closes itself right after calling it.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoGalleryScreen(
    sandeshViewModel: SandeshViewModel,
    videoUris: List<String>,
    startIndex: Int = 0,
    disappearSeconds: Int = 0,
    onEdit: ((Uri) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (videoUris.isEmpty()) return
    val exoPlayer = sandeshViewModel.videoPreviewPlayer ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, videoUris.lastIndex)) }
    var topBarVisible by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var savedIndexes by remember { mutableStateOf(setOf<Int>()) }

    BackHandler { onDismiss() }

    LaunchedEffect(videoUris, startIndex) {
        val mediaItems = videoUris.map { MediaItem.fromUri(Uri.fromFile(File(it))) }
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = exoPlayer.currentMediaItemIndex
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    val canDownload = disappearSeconds == 0
    val savedCurrent = currentIndex in savedIndexes

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // Video + standard controls. navigationBarsPadding keeps the controller above the system nav bar.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 3000
                    setShowPreviousButton(true)
                    setShowNextButton(true)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    // Top bar fades in and out together with the controller
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            topBarVisible = visibility == View.VISIBLE
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize().navigationBarsPadding()
        )

        AnimatedVisibility(
            visible = topBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(Modifier.fillMaxWidth()) {
                // Soft scrim so the buttons stay readable on bright videos
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    GlassButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(22.dp))
                    }

                    if (videoUris.size > 1) {
                        Text(
                            text = "${currentIndex + 1} / ${videoUris.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }

                    Row(
                        Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canDownload) {
                            GlassButton(
                                onClick = {
                                    if (!saving && !savedCurrent) {
                                        saving = true
                                        val idx = currentIndex
                                        val path = videoUris[idx]
                                        scope.launch {
                                            val ok = saveVideoToGallery(context, path)
                                            saving = false
                                            if (ok) savedIndexes = savedIndexes + idx
                                            Toast.makeText(
                                                context,
                                                if (ok) "Saved to gallery" else "Couldn't save video",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                when {
                                    saving -> CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    savedCurrent -> Icon(Icons.Default.Check, "Saved", tint = Color.White, modifier = Modifier.size(22.dp))
                                    else -> Icon(Icons.Default.Download, "Save to gallery", tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                        if (onEdit != null) {
                            GlassButton(
                                onClick = {
                                    exoPlayer.pause()
                                    onEdit(Uri.fromFile(File(videoUris[currentIndex])))
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Default.Edit, "Edit", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Copies the video into Movies/Knotx so it shows up in the gallery. No permission needed on Android 10+. */
private suspend fun saveVideoToGallery(context: Context, path: String): Boolean = withContext(Dispatchers.IO) {
    val src = File(path)
    if (!src.exists()) return@withContext false

    val ext = src.extension.ifBlank { "mp4" }
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "video/mp4"
    val name = "Dootawas_${System.currentTimeMillis()}.$ext"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Knotx")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        try {
            resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                ?: throw java.io.IOException("Couldn't open output stream")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    } else {
        // Android 9 and below: needs WRITE_EXTERNAL_STORAGE
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Knotx")
                .apply { mkdirs() }
            val dest = File(dir, name)
            src.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
