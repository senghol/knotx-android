package com.senghol.knotx.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws a clip's strokes + text/stickers into ONE transparent bitmap the size of the video frame.
 * Media3 lays it over every frame during export. Returns null when there is nothing to draw.
 *
 * Coordinates are normalized (0..1 of frame width/height); sizes are fractions of frame width,
 * exactly like the preview canvas, so preview and export match.
 */
internal fun renderOverlayBitmap(clip: VideoClip): Bitmap? {
    if (clip.strokes.isEmpty() && clip.overlays.isEmpty()) return null
    val w = clip.width
    val h = clip.height
    if (w <= 0 || h <= 0) return null

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val sw = w.toFloat()
    val sh = h.toFloat()

    // 1. Strokes (the eraser only clears strokes, because text is drawn afterwards)
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    clip.strokes.forEach { s ->
        if (s.points.size < 2) return@forEach
        val path = Path()
        s.points.forEachIndexed { i, p ->
            val x = p.x * sw
            val y = p.y * sh
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        strokePaint.strokeWidth = s.width * sw
        when (s.kind) {
            BrushKind.ERASER -> {
                strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                strokePaint.color = Color.BLACK
            }
            BrushKind.MARKER -> {
                strokePaint.xfermode = null
                strokePaint.color = s.colorArgb
                strokePaint.alpha = 115 // 0.45
            }
            BrushKind.PEN -> {
                strokePaint.xfermode = null
                strokePaint.color = s.colorArgb
            }
        }
        canvas.drawPath(path, strokePaint)
    }

    // 2. Text + stickers
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    clip.overlays.forEach { o ->
        val fontPx = o.size * sw
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontPx
            color = o.textColorArgb()
            // If your EditorFont has a native Typeface, use it here so export matches preview exactly.
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val (fx, fy) = o.pad()
        val padX = fx * fontPx
        val padY = fy * fontPx
        val maxW = max(1, (sw * 0.9f).toInt())

        val layout = StaticLayout.Builder
            .obtain(o.text, 0, o.text.length, tp, maxW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        var tw = 0f
        for (i in 0 until layout.lineCount) tw = max(tw, layout.getLineWidth(i))
        tw = ceil(tw)
        val th = layout.height.toFloat()
        val boxW = tw + 2 * padX
        val boxH = th + 2 * padY

        canvas.save()
        canvas.translate(o.x * sw, o.y * sh)
        canvas.rotate(o.rotation)
        val bgArgb = o.bgArgb()
        if (bgArgb != null) {
            bgPaint.color = bgArgb
            val r = 0.4f * fontPx
            canvas.drawRoundRect(RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f), r, r, bgPaint)
        }
        canvas.translate(-tw / 2f, -th / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    return bmp
}