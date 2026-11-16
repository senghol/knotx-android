package com.senghol.knotx.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale





//fun getRelativeTimeSpanMinuteLevel(timestamp: Long): String {
//    val now = System.currentTimeMillis()
//    val diff = now - timestamp
//
//    val hours = diff / 3_600_000
//    val days = diff / 86_400_000
//
//    val dateFormatter = DateTimeFormatter.ofPattern("EE, MMM d")
//        .withLocale(Locale.ENGLISH)
//        .withZone(ZoneId.systemDefault())
//
//    val dateText = dateFormatter.format(Instant.ofEpochMilli(timestamp))
//
//    val relative = when {
//        hours < 1 -> "1h"
//        hours < 24 -> "${hours}h"
////        days == 1L -> "Yesterday"
//        else -> "${days}d"
//    }
//
//    return "$dateText ($relative)"
//}

fun getRelativeTimeSpanMinuteLevel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = diff / 60_000
    val hours   = diff / 3_600_000
    val days    = diff / 86_400_000

    val relative = when {
        minutes < 1  -> "now"
        minutes < 60 -> "${minutes}m"
        hours   < 24 -> "${hours}h"
        else         -> "${days}d"          // ← 1d, 2d, 3d, … never "Yesterday"
    }

    val dateText = DateTimeFormatter.ofPattern("EE, MMM d")
        .withLocale(Locale.ENGLISH)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))

    return "$dateText ($relative)"
}




    fun generateAndSaveThumbnailInternal(context: Context, videoUri: Uri): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)

            val bitmap = retriever.getFrameAtTime(1_000_000) // 1 second mark
            retriever.release()

            if (bitmap == null) {
                
                return ""
            }

            val previewDir = File(context.filesDir, "preview")
            if (!previewDir.exists() && !previewDir.mkdirs()) {
                
                return ""
            }

            val fileName = "preview_${System.currentTimeMillis()}.webp"
            val imageFile = File(previewDir, fileName)

            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP
            }

            FileOutputStream(imageFile).use { outputStream ->
                val success = bitmap.compress(format, 100, outputStream)
                if (!success) {
                    
                    return ""
                }
            }

            
            imageFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

