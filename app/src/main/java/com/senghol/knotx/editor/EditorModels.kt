package com.senghol.knotx.editor

import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal fun newId(): String = UUID.randomUUID().toString()

/* ------------------------------------------------------------------ */
/* Enums                                                               */
/* ------------------------------------------------------------------ */

enum class EditorTool { NONE, CROP, ADJUST, FILTERS, DRAW, TEXT, STICKERS }

enum class BrushKind { PEN, MARKER, ERASER }

enum class TextBg { SOLID, GLASS, NONE }

enum class EditorFont(val label: String, val family: FontFamily, private val face: String) {
    STANDARD("Standard", FontFamily.Default, "sans-serif"),
    SERIF("Serif", FontFamily.Serif, "serif"),
    MONO("Mono", FontFamily.Monospace, "monospace"),
    CURSIVE("Cursive", FontFamily.Cursive, "cursive");

    fun typeface(): Typeface = Typeface.create(face, Typeface.BOLD)
}

/* ------------------------------------------------------------------ */
/* Crop                                                                */
/* ------------------------------------------------------------------ */

/**
 * Everything the crop screen produces, resolution independent.
 *
 *  steps     : 90° clockwise rotations (0..3), applied first
 *  flipH     : horizontal flip, applied after rotation
 *  straighten: free rotation in degrees, around the (rotated+flipped) image center
 *  frameW/H  : crop frame size, in units of the BASE image width (base = after steps+flip)
 *  panX/Y    : offset of image center relative to frame center, same units
 *
 * frameW == 0 means "no crop, full image".
 */
data class CropState(
    val steps: Int = 0,
    val flipH: Boolean = false,
    val straighten: Float = 0f,
    val frameW: Float = 0f,
    val frameH: Float = 0f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

class CropMatrix(val matrix: Matrix, val outW: Float, val outH: Float)

/**
 * Matrix mapping SOURCE pixels (source = EXIF-oriented image of size sw x sh)
 * into the cropped output frame (size outW x outH, scale 1).
 * Used by the preview AND the exporter, so both always agree.
 */
fun buildCropMatrix(c: CropState, sw: Float, sh: Float): CropMatrix {
    val odd = c.steps % 2 != 0
    val bw = if (odd) sh else sw
    val bh = if (odd) sw else sh
    val hasCrop = c.frameW > 0f
    val fw = if (hasCrop) c.frameW * bw else bw
    val fh = if (hasCrop) c.frameH * bw else bh

    val m = Matrix()
    m.postTranslate(-sw / 2f, -sh / 2f)
    m.postRotate(c.steps * 90f)
    if (c.flipH) m.postScale(-1f, 1f)
    m.postRotate(c.straighten)
    m.postTranslate(c.panX * bw, c.panY * bw)
    m.postTranslate(fw / 2f, fh / 2f)
    return CropMatrix(m, fw, fh)
}

/* ------------------------------------------------------------------ */
/* Adjustments & filters                                               */
/* ------------------------------------------------------------------ */

/** All values -1..1, 0 = untouched. */
data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f
)

private fun satM(s: Float) = ColorMatrix().apply { setSaturation(s) }

private fun contrastM(c: Float): ColorMatrix {
    val t = (-0.5f * c + 0.5f) * 255f
    return ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun brightM(b: Float) = ColorMatrix(
    floatArrayOf(
        1f, 0f, 0f, 0f, b,
        0f, 1f, 0f, 0f, b,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f
    )
)

private fun warmM(w: Float) = ColorMatrix(
    floatArrayOf(
        1f + 0.2f * w, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f - 0.2f * w, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
)

private fun chain(vararg ms: ColorMatrix): ColorMatrix {
    val r = ColorMatrix()
    ms.forEach { r.postConcat(it) }
    return r
}

enum class FilterPreset(val label: String, val build: () -> ColorMatrix) {
    NONE("Original", { ColorMatrix() }),
    VIVID("Vivid", { chain(satM(1.4f), contrastM(1.1f)) }),
    WARM("Warm", { chain(warmM(0.6f), satM(1.1f)) }),
    COOL("Cool", { chain(warmM(-0.6f), satM(1.05f)) }),
    FADE("Fade", { chain(satM(0.8f), contrastM(0.85f), brightM(20f)) }),
    MONO("Mono", { satM(0f) }),
    NOIR("Noir", { chain(satM(0f), contrastM(1.35f), brightM(-10f)) }),
    SEPIA(
        "Sepia",
        {
            ColorMatrix(
                floatArrayOf(
                    .393f, .769f, .189f, 0f, 0f,
                    .349f, .686f, .168f, 0f, 0f,
                    .272f, .534f, .131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    )
}

/** Adjustments first, then the filter (blended toward identity by [strength]). */
fun buildColorMatrix(a: Adjustments, f: FilterPreset, strength: Float): ColorMatrix {
    val result = chain(
        satM(1f + a.saturation),
        contrastM(1f + a.contrast),
        brightM(a.brightness * 100f),
        warmM(a.warmth)
    )
    if (f != FilterPreset.NONE) {
        val preset = f.build().array
        val id = ColorMatrix().array
        val mixed = FloatArray(20) { id[it] + (preset[it] - id[it]) * strength }
        result.postConcat(ColorMatrix(mixed))
    }
    return result
}

/* ------------------------------------------------------------------ */
/* Layers (all positions/sizes normalized to the SOURCE image)         */
/* ------------------------------------------------------------------ */

data class StrokeLayer(
    val id: String = newId(),
    val points: List<Offset>,     // normalized 0..1 of source width/height
    val colorArgb: Int,
    val width: Float,             // fraction of source width
    val kind: BrushKind
)

/** Text and emoji stickers share one type. */
data class Overlay(
    val id: String = newId(),
    val text: String,
    val colorArgb: Int,
    val size: Float,              // font size as a fraction of source width
    val font: EditorFont = EditorFont.STANDARD,
    val bg: TextBg = TextBg.NONE,
    val x: Float,                 // normalized center, 0..1 of source width
    val y: Float,                 // normalized center, 0..1 of source height
    val rotation: Float = 0f,     // degrees, in source space
    val isSticker: Boolean = false
)

internal fun Overlay.textColorArgb(): Int =
    if (bg == TextBg.SOLID && !isSticker) {
        if (colorArgb == -1) 0xFF000000.toInt() else -1
    } else colorArgb

internal fun Overlay.bgArgb(): Int? =
    if (isSticker) null else when (bg) {
        TextBg.SOLID -> colorArgb
        TextBg.GLASS -> 0x8C000000.toInt()
        TextBg.NONE -> null
    }

/** Padding as fractions of the font size, so preview and export match at any scale. */
internal fun Overlay.pad(): Pair<Float, Float> =
    if (isSticker || bg == TextBg.NONE) 0.1f to 0.1f else 0.45f to 0.2f

/* ------------------------------------------------------------------ */
/* Whole editor state (one undo snapshot)                              */
/* ------------------------------------------------------------------ */

data class EditorState(
    val crop: CropState = CropState(),
    val adjust: Adjustments = Adjustments(),
    val filter: FilterPreset = FilterPreset.NONE,
    val filterStrength: Float = 1f,
    val strokes: List<StrokeLayer> = emptyList(),
    val overlays: List<Overlay> = emptyList()
)

/* ------------------------------------------------------------------ */
/* Source <-> view mapping                                             */
/* ------------------------------------------------------------------ */

/** Maps normalized source coordinates to view/output pixels using [matrix] (source px -> view px). */
class ViewMapper(val matrix: Matrix, val srcW: Float, val srcH: Float) {
    private val inverse = Matrix().also { matrix.invert(it) }

    /** View pixels per source pixel. */
    val unit: Float = run {
        val v = floatArrayOf(1f, 0f)
        matrix.mapVectors(v)
        hypot(v[0], v[1])
    }

    val mirrored: Boolean = run {
        val v = FloatArray(9)
        matrix.getValues(v)
        v[Matrix.MSCALE_X] * v[Matrix.MSCALE_Y] - v[Matrix.MSKEW_X] * v[Matrix.MSKEW_Y] < 0f
    }

    fun toView(nx: Float, ny: Float): Offset {
        val p = floatArrayOf(nx * srcW, ny * srcH)
        matrix.mapPoints(p)
        return Offset(p[0], p[1])
    }

    fun toNorm(o: Offset): Offset {
        val p = floatArrayOf(o.x, o.y)
        inverse.mapPoints(p)
        return Offset(p[0] / srcW, p[1] / srcH)
    }

    /** Rotation (degrees) an element with source-space rotation [deg] appears to have in the view. */
    fun angleOf(deg: Float): Float {
        val r = Math.toRadians(deg.toDouble())
        val v = floatArrayOf(cos(r).toFloat(), sin(r).toFloat())
        matrix.mapVectors(v)
        return Math.toDegrees(atan2(v[1], v[0]).toDouble()).toFloat()
    }
}