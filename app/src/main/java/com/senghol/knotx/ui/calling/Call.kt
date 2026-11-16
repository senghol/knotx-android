package com.senghol.knotx.ui.calling

import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView


@Composable
fun CallScreenBlurredBackground(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Vibrant gradient base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6C5CE7), // Purple
                            Color(0xFFA29BFE),
                            Color(0xFFFD79A8), // Pink
                        )
                    )
                )
        )

        // 2. Large soft blurred glows (glassmorphism style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .size(600.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-150).dp, y = (-150).dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .blur(120.dp)
            )
            Box(
                modifier = Modifier
                    .size(700.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 100.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .blur(140.dp)
            )
        }

        // 3. Subtle dark overlay for better contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)) // Adjust for readability
        )

        // Your actual content on top
        content()
    }
}
@Composable
fun SamsungCallScreenBackground(
    content: @Composable () -> Unit
) {
    val randomColors = remember {
        List(4) {
            Color(
                red = (50..255).random() / 255f,
                green = (50..255).random() / 255f,
                blue = (50..255).random() / 255f,
                alpha = 1f
            ).copy(alpha = 0.35f + (0..10).random() / 100f)
        }
    }

    // Subtle animated shift (Samsung-like motion)
    val infinite = rememberInfiniteTransition()
    val animatedRadius by infinite.animateFloat(
        initialValue = 800f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ⭐ Your custom background (replaces system wallpaper)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = randomColors,
                        radius = animatedRadius
                    )
                )
        )

        // ⭐ Dynamic colorful blur layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(110.dp)
        )

        // ⭐ White glowing blobs (Samsung signature)
        Box(Modifier.fillMaxSize()) {
            GlowCircle(
                size = 600.dp,
                offsetY = (-180).dp,
                alpha = 0.18f,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            GlowCircle(
                size = 720.dp,
                offsetY = 160.dp,
                alpha = 0.14f,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ⭐ Slight dark scrim for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )

        content()
    }
}

@Composable
private fun GlowCircle(
    size: Dp,
    offsetY: Dp,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .offset(y = offsetY)
            .background(Color.White.copy(alpha = alpha), CircleShape)
            .blur(140.dp)
    )
}
