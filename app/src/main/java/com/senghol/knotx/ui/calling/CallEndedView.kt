package com.senghol.knotx.ui.calling
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.senghol.knotx.R
@Composable
fun CallEndedView(
    modifier: Modifier = Modifier,
    isVideoCall: Boolean,
    callDuration: Long? = null,
    userName: String?,
    userNumber: String,
    userImageUrl: String? = null,
    onAudioRecall: () -> Unit,
    onVideoRecall: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = modifier
            .widthIn(max = 360.dp)   // ✅ dialog width control
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // 🧑 Profile
            if (!userImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.DarkGray, CircleShape)
                        .padding(20.dp),
                    tint = Color.White
                )
            }

            // 👤 Name / number
            Text(
                text = userName ?: userNumber,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1
            )

            // 📞 Status + duration
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isVideoCall) "Video call ended" else "Audio call ended",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                callDuration?.let {
                    Text(
                        text = formatCallDuration(it),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))


        }
    }
}

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1E1E1E), CircleShape)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
//        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}


@Preview(
    showSystemUi = true,
    showBackground = true,
    backgroundColor = 0xFF000000 // Black background
)
@Composable
fun CallEndedViewPreview() {
    CallEndedView(
        isVideoCall = false,
        callDuration = 3723L, // Example: 1hr 2min 3sec
        userName = "Vikash Dagar",
        userNumber = "+91 9876543210",
        userImageUrl = null, // Or provide a mock image URL
        onAudioRecall = {},
        onVideoRecall = {},
        onBack = {}
    )
}
