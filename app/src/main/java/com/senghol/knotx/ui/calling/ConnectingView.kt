package com.senghol.knotx.ui.calling
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConnectingView(
    isVideoCall: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isVideoCall) "Connecting video call..." else "Connecting audio call...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}
