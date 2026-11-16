package com.senghol.knotx.ui.calling
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.senghol.knotx.R
import com.senghol.knotx.calling.domain.model.CallStatus

import com.senghol.knotx.utils.data.CallEvent
import com.senghol.knotx.utils.data.CallUiState
import com.senghol.knotx.utils.enums.CallResult
import com.senghol.knotx.utils.enums.CallState
import kotlinx.coroutines.delay

@Composable
fun CallingDots() {
    val dots = listOf(".   ", "..  ", "... ", "....")
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            index = (index + 1) % dots.size
        }
    }

    Text(
        text = dots[index],
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFF44336)
    )
}

@Composable
fun OutgoingCallView(
    uiState: CallUiState,
    status: CallStatus,
    isVideoCall: Boolean,
    onEvent: (CallEvent) -> Unit
) {

    // ⏳ 40 sec timeout
    LaunchedEffect(uiState.callState) {
        delay(40_000L)
        if (uiState.callState == CallState.Calling) {
            onEvent(CallEvent.EndCall(CallResult.NO_ANSWER))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        // 👤 TOP-LEFT: Profile image
        AsyncImage(
            model = uiState.remoteUserProfileUrl,
            contentDescription = "User Image",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 20.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop,
//            placeholder = painterResource(R.drawable.person_24px),
//            error = painterResource(R.drawable.person_24px)
        )

        // 🔝 TOP-CENTER: Calling + dots
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val textCall = when (status) {
                CallStatus.CONNECTING -> "Connecting"
                CallStatus.RINGING -> "Ringing"
                else -> {""}
            }
            Text(
                text = textCall,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF44336)
            )
            Spacer(Modifier.width(6.dp))
            CallingDots()
        }

        // 🎯 CENTER: Name & number (bigger)
// 🧑 MID-TOP: Name & number (between top and center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp)   // 🔽 controls vertical position
                .offset(y = 24.dp),      // fine-tune if needed
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val name = uiState.remoteUserName?.trim()
            val number = uiState.remoteUserNumber?.trim()

            Text(
                text = name ?: number ?: "Unknown",
                style = MaterialTheme.typography.headlineMedium, // ⬆ bigger
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (!number.isNullOrEmpty() && number != name) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // ☎️ BOTTOM: Controls
        CallControls(
            uiState = uiState,
            isInPipMode = false,
            isVideo = isVideoCall,
            onEvent = onEvent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
