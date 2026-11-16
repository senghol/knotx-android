package com.senghol.knotx.ui

import android.R.attr.maxWidth
import android.text.method.Touch.scrollTo
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview


import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
@Composable
fun RadioWavesInRectangle(
    question: String = "Your question here",
    modifier: Modifier = Modifier
) {
    // Animation: slow phase shift for pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),  // one full cycle
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),  // 8 seconds per cycle
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .size(320.dp)           // ← rectangle size — change as needed
            .border(
                2.dp,
                Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Define your "levels" — amplitude & frequency (higher level = bigger/stronger wave)
            val levels = listOf(
                WaveLevel(amplitude = 0.12f, frequency = 3.5f, baseColor = Color(0xFF00E5FF)), // lvl ~2 light cyan
                WaveLevel(amplitude = 0.28f, frequency = 2.2f, baseColor = Color(0xFF00B0FF)), // lvl ~5 medium blue
                WaveLevel(amplitude = 0.45f, frequency = 1.4f, baseColor = Color(0xFF1976D2))  // lvl ~7 deep blue
            )

            levels.forEachIndexed { index, level ->
                val path = Path()

                // Start from left side
                path.moveTo(0f, centerY)

                val step = 4f  // smaller = smoother wave
                var x = 0f
                while (x <= width) {
                    // Sine wave + random phase offset per level + global animation phase
                    val randomOffset = Random(index).nextFloat() * 6f  // different per level
                    val y = centerY + level.amplitude * height *
                            sin(
                                (x / width) * level.frequency * Math.PI.toFloat() * 2f +
                                        phase + randomOffset
                            ).toFloat()

                    path.lineTo(x, y)
                    x += step
                }

                // Draw the wave line
                drawPath(
                    path = path,
                    color = level.baseColor.copy(alpha = 0.7f + 0.3f * (index / (levels.size - 1f))), // fade a bit
                    style = Stroke(
                        width = 3.5f - index * 0.4f,  // thicker for lower levels
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }

        // Your question text in the center (over the waves)
        Text(
            text = question,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// Helper data class
data class WaveLevel(
    val amplitude: Float,     // 0.0 → 0.5 range (relative to height)
    val frequency: Float,     // how many waves across width
    val baseColor: Color
)


@Preview(
    name = "Signal Bars Light",
    showBackground = true,
    backgroundColor = 0xFF121212,  // dark background to match radio vibe
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, // optional: dark mode
    device = "spec:width=360dp,height=640dp,dpi=420" // optional: phone-like size
)
@Preview(
    name = "Signal Bars Large",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 500,
    heightDp = 400
)
@Composable
fun SignalBarsInRectangle(
    question: String = "Signal Detected",
    modifier: Modifier = Modifier
) {
    // Fake "signal strength" that changes slowly (you can replace with real data)
    var fakeStrength by remember { mutableFloatStateOf(0.4f) }

    LaunchedEffect(Unit) {
        while (true) {
            fakeStrength = 0.2f + Random.nextFloat() * 0.7f
            kotlinx.coroutines.delay(2200)
        }
    }

    val animatedStrength by animateFloatAsState(
        targetValue = fakeStrength,
        animationSpec = tween(900)
    )

    Box(
        modifier = modifier
            .size(320.dp, 280.dp)
            .border(
                2.dp,
                Color(0xFF00E5FF),
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barCount = 7   // total bars (you can change)
            val barWidth = w / (barCount + 2) * 0.8f
            val spacing = (w - barCount * barWidth) / (barCount + 1)

            // Our three amplitude levels (max height for each "group")
            val levelMaxHeights = listOf(
                0.25f,   // lvl ~2
                0.55f,   // lvl ~5
                0.85f    // lvl ~7
            )

            repeat(barCount) { i ->
                val x = spacing + i * (barWidth + spacing)

                // Decide which level this bar belongs to (groups of bars)
                val levelIndex = when {
                    i < 2 -> 0    // weakest bars
                    i < 5 -> 1    // medium
                    else  -> 2    // strongest
                }

                val maxHeightFraction = levelMaxHeights[levelIndex]

                // Current height (animated + little random flicker)
                val flicker = 0.04f * sin(System.currentTimeMillis() / 180f + i * 1.3f).toFloat()
                val heightFraction = (animatedStrength * maxHeightFraction) + flicker * 0.15f
                val barHeight = (h * 0.75f) * heightFraction.coerceIn(0f, 1f)

                // Color gets stronger/brighter with height & level
                val color = when (levelIndex) {
                    0 -> Color(0xFF80DEEA)  // light cyan
                    1 -> Color(0xFF00B0FF)  // medium blue
                    else -> Color(0xFF1976D2) // deep blue
                }.copy(alpha = 0.7f + heightFraction * 0.3f)

                // Draw filled rectangle (bar)
                drawRect(
                    color = color,
                    topLeft = Offset(x, h - barHeight),
                    size = Size(barWidth, barHeight),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                // Optional: thin outline for retro look
                drawRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(x, h - barHeight),
                    size = Size(barWidth, barHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                )
            }
        }

        // Question text overlay
        Text(
            text = question,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        )
    }
}



data class WaveSample(
    val amplitude: Float,
    val timeMs: Long
)

@Composable
fun AudioSignalSimulatorEnhanced(
    micLevel: Float,
    isRecording: Boolean,
    playbackPositionMs: Long?,
    resetTrigger: Int,
    onSeek: (Long) -> Unit,
    onTimeUpdate: (Long) -> Unit,
    modifier: Modifier = Modifier
){

    

    val history = remember { mutableStateListOf<WaveSample>() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val isPlaying = playbackPositionMs != null

    val barWidthDp = 5.dp
    val gapDp = 2.5.dp
    val stepDp = barWidthDp + gapDp
    val stepPx = with(density) { stepDp.toPx() }

    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    var accumulatedTime by remember { mutableLongStateOf(0L) }
    var pauseStartTime by remember { mutableLongStateOf(0L) }
    var pointerScreenX by remember { mutableFloatStateOf(0f) }
    val barColor =  MaterialTheme.colorScheme.surfaceTint
    // 🔥 RESET
    LaunchedEffect(resetTrigger) {
        history.clear()
        scrollState.scrollTo(0)
        currentTimeMs = 0L
        recordingStartTime = 0L
        accumulatedTime = 0L
        pauseStartTime = 0L
    }

    LaunchedEffect(isRecording) {

        if (isRecording) {
            // Resume or first start
            if (recordingStartTime == 0L) {
                recordingStartTime = System.currentTimeMillis()
            }

            if (pauseStartTime != 0L) {
                accumulatedTime += System.currentTimeMillis() - pauseStartTime
                pauseStartTime = 0L
            }

        } else {
            // Recording paused
            if (recordingStartTime != 0L) {
                pauseStartTime = System.currentTimeMillis()
            }
        }
    }
    LaunchedEffect(isRecording, micLevel) {

        if (!isRecording) return@LaunchedEffect

        val now = System.currentTimeMillis()

        val elapsed =
            now - recordingStartTime - accumulatedTime

        history.add(
            WaveSample(
                amplitude = micLevel,
                timeMs = elapsed
            )
        )

        currentTimeMs = elapsed
        onTimeUpdate(elapsed)
        scrollState.scrollTo(scrollState.maxValue)
    }

    // 🔥 PLAYBACK SYNC (align bar to center pointer)
    LaunchedEffect(playbackPositionMs, pointerScreenX) {

        val position = playbackPositionMs ?: return@LaunchedEffect
        if (history.isEmpty()) return@LaunchedEffect

        currentTimeMs = position

        val index = history.indexOfLast { it.timeMs <= position }
            .coerceAtLeast(0)

        val barPositionPx = index * stepPx

        val targetScroll = (barPositionPx - pointerScreenX + stepPx / 2f)
            .coerceIn(0f, scrollState.maxValue.toFloat())
            .toInt()

        scrollState.scrollTo(targetScroll)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .onSizeChanged {
                pointerScreenX = it.width * 0.5f
            }
    ) {

        // 🔥 MANUAL SEEK (only when not recording or playing)
        LaunchedEffect(isRecording, playbackPositionMs) {

            if (isRecording || isPlaying) return@LaunchedEffect

            snapshotFlow { scrollState.value }
                .collect { scrollValue ->

                    if (history.isEmpty()) return@collect

                    val absoluteX = scrollValue + pointerScreenX

                    val index = (absoluteX / stepPx).toInt()
                        .coerceIn(0, history.lastIndex)

                    val timeMs = history[index].timeMs
                    currentTimeMs = timeMs

                    onSeek(timeMs)
                }
        }

        Row(
            modifier = Modifier
                .horizontalScroll(
                    state = scrollState,
                    enabled = !isRecording && !isPlaying
                )
                .fillMaxHeight()
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(stepDp * history.size)
            ) {

                val h = size.height
                val midY = h / 2f



                history.forEachIndexed { index, sample ->

                    val barX = index * stepPx
                    val barHalfHeight = (h * 0.45f) * sample.amplitude

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, midY - barHalfHeight),
                        size = Size(stepPx - 2.dp.toPx(), barHalfHeight * 2),
                        cornerRadius = CornerRadius(3.5.dp.toPx())
                    )
                }

                // center zero line
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    strokeWidth = 1f
                )
            }
        }

        // 🔥 POINTER + TIME (hidden while recording)
        if (!isRecording) {

            Canvas(modifier = Modifier.matchParentSize()) {

                val pointerColor =
                    if (isPlaying) Color.Green else Color.Red

                drawLine(
                    color = pointerColor,
                    start = Offset(pointerScreenX, 0f),
                    end = Offset(pointerScreenX, size.height),
                    strokeWidth = 2f
                )

                drawCircle(
                    color = pointerColor,
                    radius = 6.dp.toPx(),
                    center = Offset(pointerScreenX, 12.dp.toPx())
                )
            }

            Text(
                text = formatTimeWithMillis(currentTimeMs),
                color = MaterialTheme.colorScheme.scrim,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

private fun formatTimeWithMillis(timeMs: Long): String {
    val hours = timeMs / 3_600_000
    val minutes = (timeMs % 3_600_000) / 60_000
    val seconds = (timeMs % 60_000) / 1_000
    val millis = timeMs % 1_000

    return String.format(
        Locale.US,
        "%02d:%02d:%02d.%03d",
        hours,
        minutes,
        seconds,
        millis
    )
}
@Composable
fun AudioSignalSimulatorGlobalGradient(
    question: String = "Audio Signal • Dark → Light Green",
    boxesPerSecond: Int = 30,
    modifier: Modifier = Modifier
) {
    val history = remember { mutableStateListOf<Float>() }

    val intervalMs = (1000f / boxesPerSecond).toLong()

    LaunchedEffect(boxesPerSecond) {
        while (true) {
            val time = System.currentTimeMillis() / 1000f
            var amp = sin(time * 7f) * 0.45f + sin(time * 22f) * 0.28f
            amp += (Random.nextFloat() - 0.5f) * 0.22f

            val maxAmp = 0.98f
            amp = amp.coerceIn(-maxAmp, maxAmp)

            history.add(amp)
            if (history.size > boxesPerSecond * 3) {
                history.removeAt(0)
            }

            delay(intervalMs)
        }
    }

    Box(
        modifier = modifier
            .size(420.dp, 260.dp)
            .border(2.dp, Color(0xFF00D4FF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            val barWidth = 8.dp.toPx()   // tuned for ~30 bars
            val gap = 6.dp.toPx()
            val step = barWidth + gap
            val visibleCount = (w / step).toInt().coerceAtLeast(20)

            val startIdx = (history.size - visibleCount).coerceAtLeast(0)

            val darkGreen = Color(0xFF004d00)
            val lightGreen = Color(0xFF90ee90)

            for (i in startIdx until history.size) {
                val amp = history[i]
                val barX = (i - startIdx) * step

                val barHalfHeight = (h * 0.46f) * abs(amp)

                val barCenterX = barX + barWidth / 2f
                val fraction = (barCenterX / w).coerceIn(0f, 1f)

// Inside the loop:
                val darkGreen = Color(0xFF004d00)
                val lightGreen = Color(0xFF90ee90)

                val barGradient = Brush.horizontalGradient(
                    colors = listOf(darkGreen, lightGreen),
                    startX = barX,              // start at left edge of THIS bar
                    endX = barX + barWidth      // end at right edge of THIS bar
                )

                drawRoundRect(
                    brush = barGradient,        // ← use brush = here (not color =)
                    topLeft = Offset(barX, midY - barHalfHeight),
                    size = Size(barWidth, barHalfHeight * 2),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Fill
                )

// Outline (unchanged)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(barX, midY - barHalfHeight),
                    size = Size(barWidth, barHalfHeight * 2),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 1f)
                )
            }

            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f
            )
        }

        Text(
            text = question,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}