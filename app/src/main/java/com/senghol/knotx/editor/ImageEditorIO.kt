package com.senghol.knotx.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.scale

/* ------------------------------------------------------------------ */
/* Decoding                                                            */
/* ------------------------------------------------------------------ */

/**
 * Decodes [uri] so its long side is at most [maxDim], with EXIF orientation applied.
 * Reads straight from the uri; nothing is copied or written.
 */
internal fun decodeOriented(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    val cr = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

    val long = max(bmp.width, bmp.height)
    if (long > maxDim) {
        val k = maxDim.toFloat() / long
        val scaled = bmp.scale(
            (bmp.width * k).roundToInt().coerceAtLeast(1),
            (bmp.height * k).roundToInt().coerceAtLeast(1)
        )
        if (scaled !== bmp) bmp.recycle()
        bmp = scaled
    }

    val orientation = try {
        cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val m = exifMatrix(orientation) ?: return bmp
    val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    if (rotated !== bmp) bmp.recycle()
    return rotated
}

private fun exifMatrix(orientation: Int): Matrix? {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(-90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(-90f)
        else -> return null
    }
    return m
}

/* ------------------------------------------------------------------ */
/* Native overlay drawing (export)                                     */
/* ------------------------------------------------------------------ */

internal object OverlayRenderer {

    fun drawStrokes(canvas: Canvas, strokes: List<StrokeLayer>, mapper: ViewMapper) {
        if (strokes.isEmpty()) return
        val layer = canvas.saveLayer(null, null)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokes.forEach { st ->
            if (st.points.size < 2) return@forEach
            val path = Path()
            st.points.forEachIndexed { i, p ->
                val o = mapper.toView(p.x, p.y)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            paint.strokeWidth = st.width * mapper.srcW * mapper.unit
            if (st.kind == BrushKind.ERASER) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                paint.color = android.graphics.Color.BLACK
            } else {
                paint.xfermode = null
                paint.color = st.colorArgb
                if (st.kind == BrushKind.MARKER) paint.alpha = 115 // 0.45
            }
            canvas.drawPath(path, paint)
        }
        canvas.restoreToCount(layer)
    }

    fun drawOverlays(canvas: Canvas, overlays: List<Overlay>, mapper: ViewMapper, frameW: Float) {
        val maxW = (frameW * 0.9f).toInt().coerceAtLeast(1)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        overlays.forEach { o ->
            if (o.text.isEmpty()) return@forEach
            val fontPx = o.size * mapper.srcW * mapper.unit
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontPx
                color = o.textColorArgb()
                typeface = o.font.typeface()
            }
            var natural = 0f
            o.text.split("\n").forEach { natural = max(natural, tp.measureText(it)) }
            val width = min(ceil(natural).toInt().coerceAtLeast(1), maxW)
            val layout = StaticLayout.Builder
                .obtain(o.text, 0, o.text.length, tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
            var lineMax = 0f
            for (i in 0 until layout.lineCount) lineMax = max(lineMax, layout.getLineWidth(i))

            val (fx, fy) = o.pad()
            val padX = fx * fontPx
            val padY = fy * fontPx
            val boxW = lineMax + 2 * padX
            val boxH = layout.height + 2 * padY
            val c = mapper.toView(o.x, o.y)

            canvas.save()
            canvas.translate(c.x, c.y)
            canvas.rotate(mapper.angleOf(o.rotation))
            o.bgArgb()?.let { argb ->
                bgPaint.color = argb
                canvas.drawRoundRect(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f, 0.4f * fontPx, 0.4f * fontPx, bgPaint)
            }
            canvas.translate(-lineMax / 2f, -layout.height / 2f)
            layout.draw(canvas)
            canvas.restore()
        }
    }
}

/* ------------------------------------------------------------------ */
/* Export: decode -> crop/color -> overlays -> ONE jpeg in cacheDir    */
/* ------------------------------------------------------------------ */

object ImageExporter {
    private const val STANDARD_SIDE = 1600
    private const val HD_SIDE = 4096
    private const val STANDARD_QUALITY = 80
    private const val HD_QUALITY = 92

    /** Returns the uri of the single finished file, or null on failure. Caller owns (and deletes) it. */
    fun export(context: Context, sourceUri: Uri, s: EditorState, hd: Boolean): Uri? {
        val maxSide = if (hd) HD_SIDE else STANDARD_SIDE
        val quality = if (hd) HD_QUALITY else STANDARD_QUALITY
        val src = decodeOriented(context, sourceUri, maxSide) ?: return null
        var out: Bitmap? = null
        return try {
            val sw = src.width.toFloat()
            val sh = src.height.toFloat()
            val cm = buildCropMatrix(s.crop, sw, sh)
            val k = min(1f, maxSide / max(cm.outW, cm.outH))
            cm.matrix.postScale(k, k)
            val ow = (cm.outW * k).roundToInt().coerceAtLeast(1)
            val oh = (cm.outH * k).roundToInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
            out = bitmap
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.BLACK)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(buildColorMatrix(s.adjust, s.filter, s.filterStrength))
            }
            canvas.drawBitmap(src, cm.matrix, paint)

            val mapper = ViewMapper(cm.matrix, sw, sh)
            OverlayRenderer.drawStrokes(canvas, s.strokes, mapper)
            OverlayRenderer.drawOverlays(canvas, s.overlays, mapper, ow.toFloat())

            val file = File.createTempFile("image_edit_", ".jpg", context.cacheDir)
            val ok = FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            if (ok) Uri.fromFile(file) else { file.delete(); null }
        } finally {
            src.recycle()
            out?.recycle()
        }
    }
}