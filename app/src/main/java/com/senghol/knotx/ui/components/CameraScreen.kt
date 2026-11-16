package com.senghol.knotx.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import com.senghol.knotx.R
import java.util.Locale


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTransformGestures

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy

import java.util.*

import kotlinx.coroutines.*

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalLocale

private fun getSupportedResolutions(context: Context, lensFacing: Int): List<Size> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList.firstOrNull {
        val characteristics = cameraManager.getCameraCharacteristics(it)
        characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing
    } ?: return emptyList()

    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    return map?.getOutputSizes(ImageFormat.JPEG)?.sortedByDescending { it.width * it.height } ?: emptyList()
}

@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onVideoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val hapticFeedback = LocalHapticFeedback.current

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var isPhotoMode by remember { mutableStateOf(true) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var exposureLevel by remember { mutableIntStateOf(0) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var aspectRatio by remember { mutableIntStateOf(AspectRatio.RATIO_4_3) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val preview = remember { Preview.Builder().build() }
    // Primary Controls Row (Zoom, Capture, Mode Switch)
// State for countdown
    var countdown by remember { mutableIntStateOf(0) }

    val outputDirectory = remember {
        File(context.cacheDir, "media").apply { mkdirs() }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    BackHandler { onDismiss() }

    var showResolutionDialog by remember { mutableStateOf(false) }
    var availableResolutions by remember { mutableStateOf(listOf<Size>()) }
    var selectedResolution by remember { mutableStateOf(Size(1920, 1080)) } // Default Full HD

    LaunchedEffect(Unit) {
        availableResolutions = getSupportedResolutions(context, CameraSelector.LENS_FACING_BACK)
    }

    if (showResolutionDialog) {
        Dialog(onDismissRequest = { showResolutionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select Resolution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Tap a resolution to apply",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Limit the height to 300dp
                    ) {
                        items(availableResolutions) { size ->
                            val isSelected = size == selectedResolution
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        Color(0xFF2196F3).copy(alpha = 0.2f)
                                    else
                                        Color(0xFFF5F5F5)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedResolution = size
                                        showResolutionDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${size.width} x ${size.height}",
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF2196F3) else Color.Black,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showResolutionDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "Cancel", color = Color(0xFF2196F3))
                    }
                }
            }
        }
    }

    DisposableEffect(cameraProviderFuture, cameraSelector, aspectRatio) {
        val cameraProvider = cameraProviderFuture.get()

        preview.surfaceProvider = previewView.surfaceProvider

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(selectedResolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER)
            )
            .build()

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setResolutionSelector(resolutionSelector)
            .build()



        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture, videoCapture
            )
            camera = boundCamera
            boundCamera.cameraControl.setLinearZoom((zoomRatio - 1f) / 4f) // instead of zoomRatio

            boundCamera.cameraControl.setExposureCompensationIndex(exposureLevel)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera binding failed", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            executor.shutdown()
        }
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    LaunchedEffect(zoomRatio) {
        camera?.cameraControl?.setLinearZoom((zoomRatio - 1f) / 4f) // Maps 1-5 -> 0-1

    }

    LaunchedEffect(exposureLevel) {
        camera?.cameraControl?.setExposureCompensationIndex(exposureLevel)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding() // ✅ Prevent overlap with nav & status bars
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomRatio = (zoomRatio * zoom).coerceIn(1f, 5f)
                }
                detectTapGestures { offset ->
                    focusPoint = offset
                    val meteringPoint = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    camera?.cameraControl?.startFocusAndMetering(action)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1000)
                        focusPoint = null
                    }
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Focus Indicator
        focusPoint?.let { point ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(point.x.toInt() - 24, point.y.toInt() - 24) }
                    .size(48.dp)
                    .border(2.dp, Color.Yellow, CircleShape)
                    .alpha(animateFloatAsState(targetValue = 1f).value)
            )
        }

// ✅ Countdown Overlay (Centered)
        if (countdown > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdown.toString(),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

// ✅ Bottom Control Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Recording Timer (only when recording)
                if (recording != null) {
                    Text(
                        text = String.format(
                            LocalLocale.current.platformLocale,
                            "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(recordingTime),
                            TimeUnit.MILLISECONDS.toSeconds(recordingTime) % 60
                        ),
                        color = Color.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ✅ Secondary Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Flash Button
                    val flashButtonScale by animateFloatAsState(targetValue = if (flashMode == ImageCapture.FLASH_MODE_ON) 1.1f else 1f)
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                                else -> ImageCapture.FLASH_MODE_AUTO
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .scale(flashButtonScale)
                    ) {
                        val flashIcon = when (flashMode) {
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                            ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                            else -> Icons.Filled.FlashOff
                        }
                        Icon(
                            imageVector = flashIcon,
                            contentDescription = "Flash mode",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Exposure Slider
                    var exposureSliderPosition by remember { mutableFloatStateOf(exposureLevel.toFloat()) }
                    Slider(
                        value = exposureSliderPosition,
                        onValueChange = {
                            exposureSliderPosition = it
                            exposureLevel = it.toInt().coerceIn(-2, 2)
                        },
                        valueRange = -2f..2f,
                        steps = 3,
                        modifier = Modifier.width(80.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
// Timer Button
                    val timerButtonScale by animateFloatAsState(targetValue = if (timerSeconds > 0) 1.1f else 1f)

                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            timerSeconds = when (timerSeconds) {
                                0 -> 3
                                3 -> 5
                                5 -> 10
                                else -> 0
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .scale(timerButtonScale)
                    ) {
                        if (timerSeconds == 0) {
                            Icon(
                                imageVector = Icons.Filled.TimerOff,
                                contentDescription = "Timer off",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "${timerSeconds}s",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Aspect Ratio Button
                    val aspectRatioScale by animateFloatAsState(targetValue = if (aspectRatio != AspectRatio.RATIO_4_3) 1.1f else 1f)
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            aspectRatio = when (aspectRatio) {
                                AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_16_9
                                else -> AspectRatio.RATIO_4_3
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .scale(aspectRatioScale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { showResolutionDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = "Select resolution",
                                    tint = Color.White
                                )
                            }

                        }
                    }

                    // Switch Camera Button
                    val switchCameraScale by animateFloatAsState(targetValue = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) 1.1f else 1f)
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                zoomRatio = 1f // Reset to full zoom-out
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                zoomRatio = 1f
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .scale(switchCameraScale)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = "Switch camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // ✅ Primary Controls Row (Zoom - Capture - Mode Switch)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    // Zoom Slider (Left)
                    var zoomSliderPosition by remember { mutableFloatStateOf(zoomRatio) }
                    Slider(
                        value = zoomRatio,
                        onValueChange = {
                            zoomRatio = it.coerceIn(1f, 5f)
                        },
                        valueRange = 1f..5f,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(100.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    // Capture Button (Center) ✅ same logic as yours
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                            if (isPhotoMode) {

                                val outputOptions = createImageOutputOptions(context)

                                val captureAction = {
                                    imageCapture?.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                                                val savedUri = output.savedUri

                                                if (savedUri != null) {
                                                    onPhotoCaptured(savedUri) // ✅ DIRECT MEDIA URI
                                                } else {
                                                    Log.e("CameraScreen", "Photo URI is null")
                                                }
                                            }

                                            override fun onError(exc: ImageCaptureException) {
                                                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                                            }
                                        }
                                    )
                                }

                                if (timerSeconds > 0) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        countdown = timerSeconds
                                        while (countdown > 0) {
                                            delay(1000)
                                            countdown--
                                        }
                                        captureAction()
                                    }
                                } else {
                                    captureAction()
                                }
                            }else {

                                if (recording == null) {

                                    val outputOptions = createVideoOutputOptions(context)

                                    val pendingRecording = videoCapture?.output
                                        ?.prepareRecording(context, outputOptions)
                                        ?.apply {
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                withAudioEnabled()
                                            }
                                        }

                                    val startRecording = {
                                        recording = pendingRecording?.start(ContextCompat.getMainExecutor(context)) { event ->
                                            when (event) {

                                                is VideoRecordEvent.Start -> {
                                                    recordingTime = 0L
                                                    timerJob = CoroutineScope(Dispatchers.Main).launch {
                                                        while (isActive) {
                                                            delay(1000.milliseconds)
                                                            recordingTime += 1000
                                                        }
                                                    }
                                                }

                                                is VideoRecordEvent.Finalize -> {
                                                    timerJob?.cancel()

                                                    if (!event.hasError()) {
                                                        val savedUri = event.outputResults.outputUri
                                                        if (savedUri != null) {
                                                            onVideoCaptured(savedUri) // ✅ DIRECT MEDIA URI
                                                        }
                                                    }

                                                    recording = null
                                                    recordingTime = 0L
                                                }
                                            }
                                        }
                                    }

                                    if (timerSeconds > 0) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            delay(timerSeconds * 1000L)
                                            startRecording()
                                        }
                                    } else {
                                        startRecording()
                                    }

                                } else {
                                    recording?.stop()
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (recording != null) Color.Red else Color.White,
                                CircleShape
                            )
                            .border(4.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                    )
                    {
                        val captureIcon = when {
                            recording != null -> Icons.Filled.StopCircle
                            isPhotoMode -> Icons.Filled.CameraAlt
                            else -> Icons.Filled.FiberManualRecord
                        }

                        Icon(
                            imageVector = captureIcon,
                            contentDescription = when {
                                recording != null -> "Stop recording"
                                isPhotoMode -> "Take photo"
                                else -> "Start recording"
                            },
                            tint = if (recording != null) Color.White else Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Mode Switch Button (Right)
                    val modeButtonColor by animateColorAsState(
                        targetValue = if (isPhotoMode) Color.White else Color(0xFFFF4444)
                    )
                    TextButton(
                        onClick = { isPhotoMode = !isPhotoMode },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(modeButtonColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = if (isPhotoMode) "VIDEO" else "PHOTO",
                            color = modeButtonColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Close Button (top-left corner)
        IconButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_close_24px),
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun createImageOutputOptions(context: Context): ImageCapture.OutputFileOptions {

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "Android/media/${context.packageName}/Knotx/Media/Images"
        )
    }

    val resolver = context.contentResolver

    return ImageCapture.OutputFileOptions.Builder(
        resolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
}

fun createVideoOutputOptions(context: Context): MediaStoreOutputOptions {

    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "VID_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(
            MediaStore.Video.Media.RELATIVE_PATH,
            "Android/media/${context.packageName}/Knotx/Media/Videos"
        )
    }

    return MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    )
        .setContentValues(contentValues)
        .build()
}