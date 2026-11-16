package com.senghol.knotx.calling.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.MediaStream

@Composable
fun RemoteVideoView(
    stream: MediaStream?,
    eglBaseContext: EglBase.Context,
    modifier: Modifier = Modifier
) {
    val videoTrack = stream?.videoTracks?.firstOrNull()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoRendererView(
                context = context,
                eglBaseContext = eglBaseContext,
                mirror = false
            )
        },
        update = { view ->
            view.setTrack(videoTrack)
        },
        onRelease = { view ->
            view.release()
        }
    )
}