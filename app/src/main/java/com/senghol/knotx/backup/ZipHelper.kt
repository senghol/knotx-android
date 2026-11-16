package com.senghol.knotx.backup


import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {

    fun zip(sourceDir: File, outputZip: File) {
        ZipOutputStream(outputZip.outputStream().buffered()).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = sourceDir.toURI().relativize(file.toURI()).path
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    fun unzip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { zis.copyTo(it) }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}