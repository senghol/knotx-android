package com.senghol.knotx.ui.screens.stories

// TODO: change the package above to match your project, e.g. com.yourapp.sandesh.stories

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import java.util.UUID
/**
 * What sits behind everything else in a story: a photo, a video,
 * or for text-only stories — a solid color / gradient.
 */
sealed class StoryBackground {
    data class Media(val uri: Uri, val isVideo: Boolean) : StoryBackground()
    data class SolidColor(val color: Color) : StoryBackground()
    data class Gradient(val colors: List<Color>) : StoryBackground()
}

/** Styling options for the text overlay background */
enum class TextBackgroundStyle {
    TRANSPARENT,
    SOLID_PILL,
    TRANSLUCENT_PILL
}

/**
 * A single draggable / pinch-resizable / rotatable text layer.
 */
class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    text: String,
    color: Color = Color.White,
    fontSizeSp: Float = 32f,
    offset: Offset = Offset.Zero,
    scale: Float = 1f,
    rotation: Float = 0f,
    fontFamily: FontFamily = FontFamily.Default,
    backgroundStyle: TextBackgroundStyle = TextBackgroundStyle.TRANSLUCENT_PILL
) {
    var text by mutableStateOf(text)
    var color by mutableStateOf(color)
    var fontSizeSp by mutableStateOf(fontSizeSp)
    var offset by mutableStateOf(offset)
    var scale by mutableStateOf(scale)
    var rotation by mutableStateOf(rotation)
    var fontFamily by mutableStateOf(fontFamily)
    var backgroundStyle by mutableStateOf(backgroundStyle)
}

/** A single draggable / resizable / rotatable emoji sticker layer. */
class StickerOverlay(
    val id: String = UUID.randomUUID().toString(),
    emoji: String,
    offset: Offset = Offset.Zero,
    scale: Float = 1f,
    rotation: Float = 0f
) {
    var emoji by mutableStateOf(emoji)
    var offset by mutableStateOf(offset)
    var scale by mutableStateOf(scale)
    var rotation by mutableStateOf(rotation)
}

/** One completed freehand stroke on the drawing layer. Immutable — safe in a plain list. */
data class DrawStroke(
    val path: Path,
    val color: Color,
    val strokeWidthPx: Float
)


data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val uri: Uri,
    val durationMs: Long
)

data class SelectedStoryAudio(
    val audioUri: Uri,
    val title: String,
    val startOffsetMs: Long = 0L,
    val endOffsetMs: Long = 15_000L, // Default to first 15s, but user can expand/contract
    val totalAudioDurationMs: Long = 60_000L
) {
    val durationMs: Long
        get() = (endOffsetMs - startOffsetMs).coerceAtLeast(1_000L) // At least 1 second clip
}
enum class EditorTool { NONE, DRAW, TEXT, STICKER, BACKGROUND }

/** A handful of ready-made gradients for text-only stories. */
/** Ready-made gradients for text-only stories — expanded from the original 6. */
val defaultGradients = listOf(
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)), // violet -> blue
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)), // sunset
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // emerald
    listOf(Color(0xFF232526), Color(0xFF414345)), // midnight
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)), // gold
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // purple haze
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)), // rose
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), // ocean
    listOf(Color(0xFFF953C6), Color(0xFFB91D73)), // magenta
    listOf(Color(0xFF00B09B), Color(0xFF96C93D)), // mint lime
    listOf(Color(0xFFDA22FF), Color(0xFF9733EE)), // orchid
    listOf(Color(0xFFFC5C7D), Color(0xFF6A82FB)), // coral blue
    listOf(Color(0xFF16222A), Color(0xFF3A6073)), // slate
    listOf(Color(0xFFFFAFBD), Color(0xFFFFC3A0)), // peach
    listOf(Color(0xFF2C3E50), Color(0xFFFD746C)), // dusk
    listOf(Color(0xFF659999), Color(0xFFF4791F)), // forest fire
    listOf(Color(0xFF1D2B64), Color(0xFFF8CDDA)), // night bloom
    listOf(Color(0xFFEB3349), Color(0xFFF45C43)), // crimson
    listOf(Color(0xFF4CA1AF), Color(0xFFC4E0E5)), // sky mist
    listOf(Color(0xFF603813), Color(0xFFB29F94)), // mocha
    listOf(Color(0xFF360033), Color(0xFF0B8793)), // deep sea
    listOf(Color(0xFFF00000), Color(0xFFDC281E)), // fire red
    listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)), // sunset trio
    listOf(Color(0xFF000000), Color(0xFF434343)), // pure black fade
)

/** Solid color swatches offered alongside the gradients. */
val defaultSolidBackgrounds = listOf(
    Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFE53935), Color(0xFF1E88E5),
    Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF8E24AA), Color(0xFFFF7043)
)