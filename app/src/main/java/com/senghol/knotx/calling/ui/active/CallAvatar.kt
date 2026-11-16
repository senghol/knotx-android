package com.senghol.knotx.calling.ui.active

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Use the SAME AsyncImage import as your IncomingCallScreen
// (coil.compose.AsyncImage for Coil 2, coil3.compose.AsyncImage for Coil 3).
import coil.compose.AsyncImage

/**
 * Round contact photo with an initial-letter fallback. Same look as the
 * incoming-call screen, so every call screen can share it.
 */
@Composable
fun CallAvatar(
    name: String,
    profileUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color(0xFF232836),
        border = BorderStroke(
            width = 2.dp,
            color = Color.White.copy(alpha = 0.2f)
        ),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!profileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profileUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}