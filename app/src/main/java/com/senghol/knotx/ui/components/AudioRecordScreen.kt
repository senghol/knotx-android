package com.senghol.knotx.ui.components
import android.Manifest
import android.content.ContentValues
import android.content.Context
import com.senghol.knotx.R

import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.foundation.background

import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource

import android.media.MediaPlayer

import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

import androidx.activity.compose.BackHandler

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.geometry.Offset

import androidx.core.app.ActivityCompat
import androidx.core.net.toUri

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.senghol.knotx.ui.AudioSignalSimulatorEnhanced
import com.senghol.knotx.ui.screens.MediaCategory
import com.senghol.knotx.ui.screens.MediaVisibility
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.ui.screens.saveMediaToAppStorageNext
import kotlinx.coroutines.withContext
import java.util.Locale

fun saveRecordedAudioToMedia(context: Context, file: File): Uri? {

    val resolver = context.contentResolver

    val fileName = "AUD_${System.currentTimeMillis()}.m4a"

    val contentValues = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(
            MediaStore.Audio.Media.RELATIVE_PATH,
            "Android/media/${context.packageName}/Knotx/Media/Audio"
        )
        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }

    val collection =
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val newUri = resolver.insert(collection, contentValues) ?: return null

    return try {

        file.inputStream().use { input ->
            resolver.openOutputStream(newUri)?.use { output ->
                input.copyTo(output)
            }
        }

        // mark complete
        contentValues.clear()
        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(newUri, contentValues, null, null)

        newUri

    } catch (e: Exception) {
        resolver.delete(newUri, null, null)
        e.printStackTrace()
        null
    }
}
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecorderScreen(
    // Update onSend signature to include duration
    onSend: (savedMedia: SavedMedia, durationMs: Long) -> Unit,

    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val coroutineScope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var runSimulator by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var micLevel by remember { mutableFloatStateOf(0f) }

    var outputFile by remember { mutableStateOf<File?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var micJob: Job? by remember { mutableStateOf(null) }

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }
    var wasStopped by remember { mutableStateOf(false) }
    // Start playing GIF immediately
    var playGif by remember { mutableStateOf(true) }

    val progressColor = if (isPaused) Color.Gray else Color.Red
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    // --- Permission ---
    LaunchedEffect(Unit) {
        if (!permission.status.isGranted) permission.launchPermissionRequest()
    }



    fun trackMicLevel() {
        micJob?.cancel()

        micJob = coroutineScope.launch(Dispatchers.Default) {

            var smoothedLevel = 0f

            while (isRecording) {

                if (!isPaused) {

                    val amplitude = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }

                    val db = if (amplitude > 0) {
                        20 * kotlin.math.log10(amplitude / 32767f)
                    } else {
                        -60f
                    }

                    val normalized = ((db + 60f) / 60f)
                        .coerceIn(0f, 1f)

                    smoothedLevel =
                        smoothedLevel * 0.75f + normalized * 0.25f

                    withContext(Dispatchers.Main) {
                        micLevel = smoothedLevel
                    }
                }

                delay(50)
            }

            withContext(Dispatchers.Main) {
                micLevel = 0f
            }
        }

    }
    fun startRecording() {

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inCallMode = audioManager.mode

        if (inCallMode == AudioManager.MODE_IN_CALL ||
            inCallMode == AudioManager.MODE_IN_COMMUNICATION) {

            // Show nice feedback
            Toast.makeText(
                context,
                "Cannot record — microphone is in use by a call",
                Toast.LENGTH_LONG
            ).show()

            // Optionally: also call onDismiss() or just return
            onDismiss()
            return
        }
        micJob?.cancel()
        // Stop previous recording safely
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }

        mediaRecorder = null
        isRecording = false
        isPaused = false
        micLevel = 0f
        playGif = true
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        outputFile = File(recordingsDir, "recording_${System.currentTimeMillis()}.m4a")

        // Use MediaRecorder(context) for API 31+ to enable NoiseSuppressor
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()

            }
        } else {
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
                // No audioSessionId here — cannot enable NoiseSuppressor
            }
        }

        runSimulator = true
        isRecording = true
        isPaused = false
        trackMicLevel()
    }


    fun pauseRecording() {
        mediaRecorder?.pause()
        isPaused = true
        runSimulator = false
    }

    fun resumeRecording() {
        mediaRecorder?.resume()
        runSimulator = true
        isPaused = false
        trackMicLevel()
    }

    fun stopRecording() {
        micJob?.cancel()
        micJob = null
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null
        isRecording = false
        runSimulator = false
        isPaused = false
        micLevel = 0f
    }

    fun startPlaybackTracking() {
        playbackJob?.cancel()

        playbackJob = coroutineScope.launch {
            while (isPlaying && mediaPlayer != null) {
                playbackPositionMs =
                    mediaPlayer?.currentPosition?.toLong() ?: 0L
                delay(50)
            }
        }
    }
    fun togglePlay() {

        // 🔥 First time start
        if (mediaPlayer == null) {

            if (!wasStopped) {
                stopRecording()
                wasStopped = true
                playGif = false
            }

            outputFile?.takeIf { it.exists() }?.let { file ->

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    seekTo(playbackPositionMs.toInt()) // resume support
                    start()

                    setOnCompletionListener {
                        isPlaying = false
                        playbackJob?.cancel()
                        playbackJob = null
                        playbackPositionMs = 0L
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }

                isPlaying = true
                startPlaybackTracking()
            }

            return
        }

        // 🔥 If already created

        if (isPlaying) {
            // PAUSE
            mediaPlayer?.pause()
            isPlaying = false
            playbackJob?.cancel()
            playbackJob = null
        } else {
            // RESUME
            mediaPlayer?.start()
            isPlaying = true
            startPlaybackTracking()
        }
    }

    fun sendRecording() {
        isSent = true
        stopRecording()
        isSending = true

        outputFile?.takeIf { it.exists() }?.let { file ->

            // 🔥 Get duration
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationMs =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong() ?: 0L
            retriever.release()

            val savedMedia = saveMediaToAppStorageNext(
                context = context,
                sourceUri = file.toUri(),
                storageType = MediaCategory.AUDIO,
                visibility = MediaVisibility.PRIVATE,
                overrideMimeType = "audio/mp4"
            )

            if (savedMedia != null) {

                file.delete()

                isSending = false

                onSend(savedMedia, durationMs)

            } else {

                isSending = false

                Log.e("Recorder", "Failed to save recording")
            }
        }
    }

    // --- Back Handler ---
    BackHandler { onDismiss() }

    DisposableEffect(Unit) {
        onDispose {
            
//
            stopRecording()
            mediaPlayer?.release()
            mediaPlayer = null
            if (isSent) return@onDispose
            outputFile?.takeIf { it.exists() }?.delete()
        }
    }
    var resetTrigger by remember { mutableIntStateOf(0) }
    // --- UI ---
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)                     // slightly taller for better breathing room
            .background(
                color = MaterialTheme.colorScheme.inverseOnSurface,
                shape = RoundedCornerShape(6.dp)
            )
//            .padding(horizontal = 16.dp)
//            .padding(top = 16.dp, bottom = 20.dp)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Timer + Play button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⏱ ${formatHMS(elapsedMs)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 32.sp
                    ),
                    color = if (isRecording) MaterialTheme.colorScheme.error
//                    else if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = { togglePlay() },
                    enabled = outputFile != null,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(28.dp),
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AudioSignalSimulatorEnhanced(
                micLevel = micLevel,
                isRecording = runSimulator,
                playbackPositionMs = if (isPlaying) playbackPositionMs else null,
                resetTrigger = resetTrigger,
                onTimeUpdate = { ms -> elapsedMs = ms },
                onSeek = { ms -> mediaPlayer?.seekTo(ms.toInt()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)          // give waveform more vertical space
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop / Restart
                IconButton(
                    onClick = {
                        if (!wasStopped) {
                            stopRecording()
                            wasStopped = true
                            playGif = false
                        } else {
                            resetTrigger++
                            startRecording()
                            wasStopped = false
                            playGif = true
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (!wasStopped) R.drawable.icon_stop_circle_24px
                            else R.drawable.restart_alt_24px
                        ),
                        contentDescription = if (!wasStopped) "Stop" else "Restart",
                        modifier = Modifier.size(32.dp),
                        tint = if (!wasStopped) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                // Mic – bigger & centered
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(enabled = isRecording) {
                            playGif = !playGif
                            if (isRecording) {
                                if (isPaused) resumeRecording() else pauseRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val gifPainter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(R.drawable.recording)
                            .decoderFactory(GifDecoder.Factory())
                            .size(Size.ORIGINAL)
                            .apply {
                                if (!playGif) {
                                    setParameter("coil#animation", false)
                                }
                            }
                            .build()
                    )

                    if (playGif) {
                        Image(
                            painter = gifPainter,
                            contentDescription = "Animated mic",
                            modifier = Modifier.fillMaxSize(0.9f)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.mic_24px),
                            contentDescription = "Static mic",
                            tint = if (isRecording) MaterialTheme.colorScheme.error else Color(0xFF757575),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Send / Progress
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                } else {
                    IconButton(
                        onClick = { sendRecording() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF69F0AE),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }    // --- Auto-start if permission granted ---
    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted && !isRecording) startRecording()
    }
}

fun formatHMS(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        hours,
        minutes,
        seconds
    )
}

fun getAudioDurationMs(
    context: Context,
    pathOrUri: String
): Long? {

    return try {

        val retriever = MediaMetadataRetriever()

        if (pathOrUri.startsWith("/")) {

            retriever.setDataSource(pathOrUri)

        } else {

            retriever.setDataSource(
                context,
                pathOrUri.toUri()
            )
        }

        val durationStr = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )

        retriever.release()

        durationStr?.toLongOrNull()

    } catch (e: Exception) {

        e.printStackTrace()

        null
    }
}