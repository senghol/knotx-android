
package com.senghol.knotx.calling.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.VideoTrack

@SuppressLint("ViewConstructor")
class VideoRendererView(
    context: Context,
    eglBaseContext: EglBase.Context,
    mirror: Boolean
) : FrameLayout(context), TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context)
    private val renderer = EglRenderer("VideoRendererView")
    private val drawer = GlRectDrawer()

    private var attachedTrack: VideoTrack? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var released = false

    init {
        textureView.surfaceTextureListener = this

        addView(
            textureView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        renderer.init(
            eglBaseContext,
            EglBase.CONFIG_PLAIN,
            drawer
        )

        renderer.setMirror(mirror)


        if (textureView.isAvailable) {
            surfaceTexture = textureView.surfaceTexture
            renderer.createEglSurface(surfaceTexture)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLayoutAspectRatio(w, h)
    }

    // Without this the frame is stretched to whatever shape the view has.
    // With it, the frame is center-cropped to fill the view (no distortion).
    private fun updateLayoutAspectRatio(width: Int, height: Int) {
        if (released || width <= 0 || height <= 0) return
        renderer.setLayoutAspectRatio(width.toFloat() / height.toFloat())
    }

    fun setMirror(mirror: Boolean) {
        if (!released) {
            renderer.setMirror(mirror)
        }
    }

    fun setTrack(track: VideoTrack?) {
        if (released || attachedTrack === track) return


        attachedTrack?.removeSink(renderer)
        attachedTrack = track
        attachedTrack?.addSink(renderer)
    }

    fun release() {
        if (released) return

        released = true

        attachedTrack?.removeSink(renderer)
        attachedTrack = null

        renderer.releaseEglSurface {
            renderer.release()
        }
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        if (released) return

        surfaceTexture = surface
        updateLayoutAspectRatio(width, height)
        renderer.createEglSurface(surface)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {

        surfaceTexture = null

        if (!released) {
            renderer.releaseEglSurface {
            }
        }

        return true
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        // EglRenderer handles the surface/layout changes.
        updateLayoutAspectRatio(width, height)
    }

    override fun onSurfaceTextureUpdated(
        surface: SurfaceTexture
    ) {
        // Nothing required.
    }
}