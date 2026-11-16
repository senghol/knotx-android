package com.senghol.knotx.ui.screens.stories


// TODO: adjust package to wherever your other story data-layer files live

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** How the original video's audio should be handled during a video export. */
sealed class VideoAudioMode {
    object KeepOriginal : VideoAudioMode()
    object Mute : VideoAudioMode()
    data class Replace(val uri: Uri, val startMs: Long, val endMs: Long) : VideoAudioMode()
}
/**
 * A single static bitmap composited over every frame for the whole video —
 * exactly what we need since story text/stickers/drawing don't move or animate
 * over time, they're just baked on once.
 *
 * @OptIn(UnstableApi::class) because androidx.media3.effect is still an
 * unstable-API package in 1.11 — same annotation you're likely already using
 * elsewhere (StoryPlayerScreen imports UnstableApi too).
 */
@UnstableApi
private class StaticBitmapOverlay(private val bitmap: Bitmap) : BitmapOverlay() {
    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap
}

private data class VideoQualityInfo(val width: Int, val height: Int, val bitrateBps: Int)

/** Reads the SOURCE video's real resolution/bitrate so we only compress when it's actually needed. */
private fun probeVideoQuality(context: Context, uri: Uri): VideoQualityInfo? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, uri, null)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 0
                val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 0
                val bitrate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) format.getInteger(MediaFormat.KEY_BIT_RATE) else -1
                if (width > 0 && height > 0) return VideoQualityInfo(width, height, bitrate)
            }
        }
        null
    } catch (e: Exception) {
        Log.e("STORY_DEBUG", "probeVideoQuality failed", e)
        null
    } finally {
        extractor.release()
    }
}

private const val GOOD_ENOUGH_BITRATE_BPS = 3_000_000 // ~3 Mbps — treat anything at/under this as already fine
private const val GOOD_ENOUGH_SHORT_SIDE = 720         // e.g. 720x1280 vertical is already story-appropriate
private const val TARGET_BITRATE_BPS = 2_500_000       // what we compress DOWN to when a clip actually needs it
private const val TARGET_SHORT_SIDE = 720

private data class EncodeDecision(val downscaleToShortSide: Int?, val targetBitrateBps: Int)

/**
 * null downscale = leave resolution alone. The bitrate is ALWAYS capped (even
 * on the "already good" path) so nothing sneaks through above the ceiling —
 * but on that path the cap is the ORIGINAL bitrate if it's already lower,
 * meaning an already-efficient file barely gets touched.
 */
private fun decideEncoding(quality: VideoQualityInfo?): EncodeDecision {
    if (quality == null) {
        // Couldn't read real numbers — safe moderate default, not a guess in either extreme.
        return EncodeDecision(TARGET_SHORT_SIDE, TARGET_BITRATE_BPS)
    }
    val shortSide = minOf(quality.width, quality.height)
    val bitrateKnown = quality.bitrateBps > 0
    val alreadyGood = shortSide <= GOOD_ENOUGH_SHORT_SIDE && (!bitrateKnown || quality.bitrateBps <= GOOD_ENOUGH_BITRATE_BPS)

    return if (alreadyGood) {
        val cappedBitrate = if (bitrateKnown) minOf(quality.bitrateBps, GOOD_ENOUGH_BITRATE_BPS) else TARGET_BITRATE_BPS
        EncodeDecision(downscaleToShortSide = null, targetBitrateBps = cappedBitrate)
    } else {
        EncodeDecision(downscaleToShortSide = TARGET_SHORT_SIDE, targetBitrateBps = TARGET_BITRATE_BPS)
    }
}


/**
 * The unified video-export path: keeps/mutes/replaces audio AND, if you pass an
 * overlayImagePath (a transparent PNG containing just text/stickers/drawing),
 * burns it onto every frame via a Media3 OverlayEffect.
 *
 * UNTESTED BY ME — androidx.media3.effect's exact API shape (OverlayEffect's
 * constructor, BitmapOverlay's required overrides, Effects' constructor args)
 * is the newest, most version-sensitive part of everything we've built in this
 * thread. If this throws or fails to compile, the Media3 Transformer demo
 * app's "overlay" sample is the authoritative reference for your exact
 * media3-effect version — the shape here (one video EditedMediaItem with
 * .setEffects(...), plus an optional second audio-only sequence) is right,
 * only exact method names might need a small adjustment.
 */
@OptIn(UnstableApi::class)
suspend fun muxVideoWithOverlayAndAudio(
    context: Context,
    videoUri: Uri,
    overlayImagePath: String?,
    audioMode: VideoAudioMode,
    outputFile: File
): Boolean = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { cont ->
        val removeOriginalAudio = audioMode !is VideoAudioMode.KeepOriginal
        val quality = probeVideoQuality(context, videoUri)
        val decision = decideEncoding(quality)

        val videoItemBuilder = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
            .setRemoveAudio(removeOriginalAudio)

        val videoEffects = mutableListOf<androidx.media3.common.Effect>()

        if (overlayImagePath != null) {
            val rawOverlayBitmap = BitmapFactory.decodeFile(overlayImagePath)
            if (rawOverlayBitmap != null) {
                // The overlay was captured over the WHOLE editor screen, but the live
                // preview shows the video letterboxed (RESIZE_MODE_FIT) inside that
                // screen if its aspect ratio doesn't match — so only part of the
                // captured overlay corresponds to actual video content. Compositing
                // it directly onto the raw video file (no letterbox baked in) is what
                // was causing drawings to appear stretched/zoomed. This crops out the
                // letterbox margins and rescales to the video's real pixel size first.
                val alignedOverlay = if (quality != null) {
                    alignOverlayToVideoFrame(rawOverlayBitmap, quality.width, quality.height)
                } else {
                    rawOverlayBitmap
                }
                videoEffects.add(OverlayEffect(ImmutableList.of<TextureOverlay>(StaticBitmapOverlay(alignedOverlay))))
            } else {
                Log.e("STORY_DEBUG", "muxVideoWithOverlayAndAudio: overlay bitmap failed to decode from $overlayImagePath — continuing without it")
            }
        }

        decision.downscaleToShortSide?.let { shortSide ->
            videoEffects.add(Presentation.createForShortSide(shortSide))
        }

        if (videoEffects.isNotEmpty()) {
            videoItemBuilder.setEffects(Effects(/* audioProcessors = */ emptyList(), videoEffects))
        }

        val sequences = mutableListOf(EditedMediaItemSequence.Builder(videoItemBuilder.build()).build())

        if (audioMode is VideoAudioMode.Replace) {
            val audioItem = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(audioMode.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(audioMode.startMs)
                            .setEndPositionMs(audioMode.endMs)
                            .build()
                    )
                    .build()
            ).build()
            // Looping the audio sequence stops Transformer from ending the whole export
            // early when the replacement clip is shorter than the video (it otherwise
            // stops at the shortest sequence). Verify setIsLooping() exists under this
            // exact name on your media3-transformer version — it has moved before.
            sequences.add(
                EditedMediaItemSequence.Builder(audioItem)
                    .setIsLooping(true)
                    .build()
            )
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(context.applicationContext)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder()
                            .setBitrate(decision.targetBitrateBps)
                            .build()
                    )
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    Log.e("STORY_DEBUG", "muxVideoWithOverlayAndAudio failed", exportException)
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)
        cont.invokeOnCancellation { transformer.cancel() }
    }
}

/**
 * Crops the letterbox margins out of a full-screen-captured overlay (from
 * RESIZE_MODE_FIT preview) and rescales what's left to match the video's real
 * pixel dimensions, so overlay pixels line up 1:1 with video content instead
 * of being stretched across a differently-proportioned frame.
 */
private fun alignOverlayToVideoFrame(overlay: Bitmap, videoWidth: Int, videoHeight: Int): Bitmap {
    val screenW = overlay.width.toFloat()
    val screenH = overlay.height.toFloat()
    val screenAspect = screenW / screenH
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()

    // Same "contain" math as RESIZE_MODE_FIT: figure out where the video's
    // actual content rectangle sat within the full-screen preview.
    val fitW: Float
    val fitH: Float
    if (videoAspect > screenAspect) {
        fitW = screenW
        fitH = screenW / videoAspect
    } else {
        fitH = screenH
        fitW = screenH * videoAspect
    }
    val fitLeft = ((screenW - fitW) / 2f).toInt().coerceAtLeast(0)
    val fitTop = ((screenH - fitH) / 2f).toInt().coerceAtLeast(0)
    val cropW = fitW.toInt().coerceAtMost(overlay.width - fitLeft)
    val cropH = fitH.toInt().coerceAtMost(overlay.height - fitTop)

    if (cropW <= 0 || cropH <= 0) return overlay // degenerate case, don't crop into nothing

    val cropped = Bitmap.createBitmap(overlay, fitLeft, fitTop, cropW, cropH)
    return Bitmap.createScaledBitmap(cropped, videoWidth, videoHeight, true)
}
/**
 * Combines a still image with a trimmed audio clip into an MP4 "video story."
 *
 * CAVEAT: Media3 Transformer's image-input API (treating a still image as a video
 * track via setDurationUs/setFrameRate) moved around across 1.3 -> 1.5. If this
 * doesn't build/behave as-is on your exact media3-transformer version, check the
 * Media3 Transformer demo app's image-sequence sample for the current signature —
 * the *shape* of this (EditedMediaItemSequence for image + one for audio, combined
 * into a Composition) is stable; only the exact image-duration API has shifted.
 */
@OptIn(UnstableApi::class)
suspend fun muxImageWithAudio(
    context: Context,
    imagePath: String,
    audioUri: Uri,
    audioStartMs: Long,
    audioEndMs: Long,
    outputFile: File
): Boolean = withContext(Dispatchers.Main) { // Transformer must be built+started on a thread with a Looper — Main, specifically the same thread throughout
    suspendCancellableCoroutine { cont ->
        val clipDurationMs = (audioEndMs - audioStartMs).coerceAtLeast(1_000L)

        // Two things an image MediaItem needs before Transformer will accept it:
        // (1) an explicit MIME type, so the asset-loader routing recognizes it as an
        // image rather than falling through to the video/audio player path, and
        // (2) setImageDurationMs() — this is the actual current API for declaring
        // how long a still image should play for; EditedMediaItem.setDurationUs()
        // (what I had before) doesn't exist for this purpose on this Media3 version.
        val imageMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(imagePath)))
            .setMimeType(MimeTypes.IMAGE_JPEG)
            .setImageDurationMs(clipDurationMs)
            .build()

        val imageItem = EditedMediaItem.Builder(imageMediaItem)
            .setFrameRate(30)
            .build()

        val audioItem = EditedMediaItem.Builder(
            MediaItem.Builder()
                .setUri(audioUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(audioStartMs)
                        .setEndPositionMs(audioEndMs)
                        .build()
                )
                .build()
        ).build()

        val composition = Composition.Builder(
            EditedMediaItemSequence.Builder(imageItem).build(),
            EditedMediaItemSequence.Builder(audioItem).build()
        ).build()

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(
                // A still image barely changes frame-to-frame, so 1.5 Mbps is
                // already generous — no need to probe quality here like the real
                // video path does, since there's no "original bitrate" to respect.
                DefaultEncoderFactory.Builder(context.applicationContext)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(1_500_000).build()
                    )
                    .build()
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)
        cont.invokeOnCancellation { transformer.cancel() }
    }
}

/**
 * Re-muxes a video, either stripping its original audio (newAudioUri = null) or
 * replacing it with a trimmed clip from a different track.
 *
 * KNOWN GAP TO TEST ON YOUR END: if the new audio clip is shorter than the video,
 * Transformer's default behavior is to end the output when the shortest sequence
 * ends — meaning your video could get cut short to match a short audio clip. If
 * you hit that, the fix is either looping the audio sequence
 * (EditedMediaItemSequence.Builder(audioItem).setIsLooping(true) — check the
 * exact flag name on your version) or trimming/repeating on your side before
 * calling this. Untested here since it depends on real device output, not
 * something I can verify without running it.
 */
@OptIn(UnstableApi::class)
suspend fun replaceVideoAudio(
    context: Context,
    videoUri: Uri,
    newAudioUri: Uri?,
    audioStartMs: Long,
    audioEndMs: Long,
    outputFile: File
): Boolean = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { cont ->
        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
            .setRemoveAudio(true)
            .build()

        val sequences = mutableListOf(EditedMediaItemSequence.Builder(videoItem).build())

        if (newAudioUri != null) {
            val audioItem = EditedMediaItem.Builder(
                MediaItem.Builder()
                    .setUri(newAudioUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(audioStartMs)
                            .setEndPositionMs(audioEndMs)
                            .build()
                    )
                    .build()
            ).build()
            sequences.add(EditedMediaItemSequence.Builder(audioItem).build())
        }

        val composition = Composition.Builder(sequences).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    if (cont.isActive) cont.resume(false)
                }
            })
            .build()

        transformer.start(composition, outputFile.absolutePath)
        cont.invokeOnCancellation { transformer.cancel() }
    }
}