package com.senghol.knotx.utils.objects

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import java.io.File
object FileResolver {

    data class FileResult(
        val file: File?,
        val isTemp: Boolean
    )

    fun getFile(context: Context, filePath: String?): FileResult {
        if (filePath.isNullOrBlank()) {
            Log.e("FileResolver", "Path is null or blank")
            return FileResult(null, false)
        }

        return try {
            val uri = filePath.toUri()

            when (uri.scheme) {

                // ✅ file:// or direct path
                "file", null -> {
                    val cleanPath = uri.path ?: filePath
                    val file = File(cleanPath)

                    if (file.exists()) {
                        FileResult(file, false)
                    } else {
                        Log.e("FileResolver", "File not found: $filePath")
                        FileResult(null, false)
                    }
                }

                // ✅ content:// → copy to temp file
                "content" -> {
                    val inputStream = context.contentResolver.openInputStream(uri)

                    if (inputStream == null) {
                        Log.e("FileResolver", "Unable to open content URI")
                        return FileResult(null, false)
                    }

                    val tempFile = File.createTempFile(
                        "upload_",
                        ".tmp",
                        context.cacheDir
                    )

                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    FileResult(tempFile, true)
                }

                else -> {
                    Log.e("FileResolver", "Unsupported URI: $filePath")
                    FileResult(null, false)
                }
            }

        } catch (e: Exception) {
            Log.e("FileResolver", "Error resolving file: $filePath", e)
            FileResult(null, false)
        }
    }
}