package com.senghol.knotx.calling.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.webrtc.EglBase
import org.webrtc.VideoTrack

@Composable
fun LocalVideoPreview(
    videoTrack: VideoTrack?,
    videoEglContext: EglBase.Context,
    isCameraEnabled: Boolean,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isCameraEnabled || videoTrack == null) return

    LocalVideoView(
        videoTrack = videoTrack,
        eglBaseContext = videoEglContext,
        modifier = modifier
            .padding(16.dp)
            .size(
                width = 120.dp,
                height = 180.dp
            ),
        mirror = isFrontCamera
    )
}