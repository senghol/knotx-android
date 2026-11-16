package com.senghol.knotx.calling.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CallControls(
    isMicrophoneEnabled: Boolean,
    isCameraEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    showCamera: Boolean,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onToggleMicrophone
        ) {
            Text(
                if (isMicrophoneEnabled) "Mute"
                else "Unmute"
            )
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Button(
            onClick = onToggleSpeaker
        ) {
            Text(
                if (isSpeakerEnabled) "Earpiece"
                else "Speaker"
            )
        }

        if (showCamera) {
            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Button(
                onClick = onToggleCamera
            ) {
                Text(
                    if (isCameraEnabled) "Camera off"
                    else "Camera on"
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Button(
                onClick = onSwitchCamera
            ) {
                Text("Flip")
            }
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Button(
            onClick = onEndCall
        ) {
            Text("End")
        }
    }
}