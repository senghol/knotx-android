package com.senghol.knotx.ui.screens.stories


import android.content.Context

import java.io.File

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object StoryAudioDownloader {

    /**
     * Downloads an audio file from [remoteUrl] directly into context.cacheDir.
     * Returns the local Uri if already cached or after successful download.
     */
    suspend fun getCachedOrDownload(context: Context, remoteUrl: String): Uri = withContext(Dispatchers.IO) {
        val cacheFolder = File(context.cacheDir, "story_audio").apply { if (!exists()) mkdirs() }

        // Generate a unique filename based on URL MD5 hash
        val fileName = remoteUrl.toMd5Hash() + ".mp3"
        val cachedFile = File(cacheFolder, fileName)

        // 1. If file already exists locally, use it immediately
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext Uri.fromFile(cachedFile)
        }

        // 2. Otherwise, download to a temp file and rename atomically
        val tempFile = File(cacheFolder, "$fileName.tmp")
        try {
            val url = URL(remoteUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile.renameTo(cachedFile)
                return@withContext Uri.fromFile(cachedFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
        }

        // Fallback to remote URL if download fails
        return@withContext Uri.parse(remoteUrl)
    }

    private fun String.toMd5Hash(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}