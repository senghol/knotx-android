package com.senghol.knotx.calling.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.VideoTrack

@Composable
fun LocalVideoView(
    videoTrack: VideoTrack?,
    eglBaseContext: EglBase.Context,
    modifier: Modifier = Modifier,
    mirror: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoRendererView(
                context = context,
                eglBaseContext = eglBaseContext,
                mirror = mirror
            )
        },
        update = { view ->
            view.setMirror(mirror)
            view.setTrack(videoTrack)
        },
        onRelease = { view ->
            view.release()
        }
    )
}