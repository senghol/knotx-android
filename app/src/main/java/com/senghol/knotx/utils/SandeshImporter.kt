package com.senghol.knotx.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.ui.screens.MediaCategory
import com.senghol.knotx.ui.screens.MediaVisibility
import com.senghol.knotx.ui.screens.SavedMedia
import com.senghol.knotx.ui.screens.saveMediaToAppStorageNext
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshStithi
import com.senghol.knotx.utils.enums.SandeshType

import net.lingala.zip4j.ZipFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern


/**
 * SandeshImporter
 *
 * Imports a WhatsApp exported chat (.zip) into the Knotx database.
 *
 * @param context         App context (needed for MediaStore / file ops)
 * @param sandeshDao      DAO to insert messages
 * @param userNumber      The conversation partner's phone number (used as userNumber for all messages)
 * @param zipPath         Absolute path to the WhatsApp export .zip file
 * @param partnerName     The display name of the OTHER person in the chat (e.g. "Monu").
 *                        If sender matches this → isSelf = false, else isSelf = true
 * @param showInGallery   If true, media is also inserted into MediaStore so it appears in gallery
 */
class SandeshImporter(
    private val context: Context,
    private val sandeshDao: SandeshDao,
    private val userNumber: String,
    val extractedDir: File,
    private val partnerName: String,
    private val showInGallery: Boolean,
    val onProgress: (Int) -> Unit = {} // 0-100
) {

    // ─────────────────────────────────────────────
    // Regex to parse WhatsApp export lines
    // Format: DD/MM/YY, HH:MM - Sender: message
    // ─────────────────────────────────────────────
    private val linePattern = Pattern.compile(
        """^(\d{2}/\d{2}/\d{2}),\s(\d{2}:\d{2})\s-\s([^:]+):\s(.*)$"""
    )

    private val dateFormat = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault())

    // Matches http/https URLs and bare domains like senghol.com, maps.google.com etc.
    private val urlPattern = Pattern.compile(
        """(https?://[^\s]+|(?<!\w)[a-zA-Z0-9][a-zA-Z0-9\-]*\.[a-zA-Z]{2,}(?:/[^\s]*)?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Matches phone numbers:
    // +91 85060 97312 | +919999999999 | 9958022501 | 07015009826
    private val phonePattern = Pattern.compile(
        """(\+?\d[\d\s\-]{8,}\d)"""
    )

    // ─────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────
    suspend fun import() {
        val txtFile = extractedDir.walkTopDown()
            .firstOrNull { it.extension == "txt" }
            ?: throw IllegalStateException("No .txt file found")

        val lines = txtFile.readLines()
        val rawMessages = parseRawMessages(lines)

        val imageGroups = groupImagesBySameTime(rawMessages)
        val processedImageKeys = mutableSetOf<String>()

        val total = rawMessages.size

        rawMessages.forEachIndexed { index, raw ->
            if (isSystemMessage(raw)) return@forEachIndexed

            if (isImageFile(raw.body)) {
                val key = "${raw.sender}_${raw.time}"
                if (processedImageKeys.contains(key)) return@forEachIndexed
                processedImageKeys.add(key)
                val group = imageGroups[key] ?: return@forEachIndexed
                val sandesh = buildGroupedImageSandesh(group, extractedDir) ?: return@forEachIndexed
                sandeshDao.insertMessage(sandesh)

                val caption = group.firstNotNullOfOrNull { extractCaption(it.body) }
                if (caption != null) {
                    val isSelf = raw.sender.trim() != partnerName.trim()
                    val captionRaw = RawMessage(sender = raw.sender, body = caption, time = raw.time, uniqueTime = raw.uniqueTime + 1)
                    buildTextSandesh(captionRaw, isSelf)?.let { sandeshDao.insertMessage(it) }
                }

            } else {
                val sandesh = buildSandesh(raw, extractedDir) ?: return@forEachIndexed
                sandeshDao.insertMessage(sandesh)

                val caption = extractCaption(raw.body)
                if (caption != null) {
                    val isSelf = raw.sender.trim() != partnerName.trim()
                    val captionRaw = RawMessage(sender = raw.sender, body = caption, time = raw.time, uniqueTime = raw.uniqueTime + 1)
                    buildTextSandesh(captionRaw, isSelf)?.let { sandeshDao.insertMessage(it) }
                }
            }

            // report progress after each message
            onProgress(((index + 1) * 100) / total)
        }

    }
    private fun groupImagesBySameTime(messages: List<RawMessage>): Map<String, List<RawMessage>> {
        return messages
            .filter { isImageFile(it.body) }
            .groupBy { "${it.sender}_${it.time}" } // same sender + same minute
    }

    private var offset = 0L
    // ─────────────────────────────────────────────
    // Step 1: Parse raw lines into RawMessage list
    // handles multi-line messages
    // ─────────────────────────────────────────────
    private fun parseRawMessages(lines: List<String>): List<RawMessage> {
        val result = mutableListOf<RawMessage>()
        var current: RawMessage? = null

        for (line in lines) {
            val matcher = linePattern.matcher(line)
            if (matcher.matches()) {
                current?.let { result.add(it) }
                val dateStr = "${matcher.group(1)}, ${matcher.group(2)}"
                val sender = matcher.group(3)?.trim() ?: ""
                val body = matcher.group(4) ?: ""
                val time = runCatching {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                }.getOrDefault(System.currentTimeMillis())

                current = RawMessage(sender = sender, body = body, time = time,uniqueTime = time + offset++)
            } else {
                // Multi-line: append to current message body
                current?.let {
                    current = it.copy(body = it.body + "\n" + line)
                }
            }
        }
        current?.let { result.add(it) }
        return result
    }

    // ─────────────────────────────────────────────
    // Step 2: Convert RawMessage → Sandesh entity
    // ─────────────────────────────────────────────


    private fun buildSandesh(raw: RawMessage, extractedDir: File): Sandesh? {
        // Skip system messages (no real sender, e.g. encryption notice)
        if (isSystemMessage(raw)) return null

        val isSelf = raw.sender.trim() != partnerName.trim()

        return when {
            raw.body == "<Media omitted>" -> null // skip, no file

            raw.body == "This message was deleted" -> buildDeleteSandesh(raw, isSelf)

            raw.body == "live location shared" -> buildLiveLocationSandesh(raw, isSelf)

            raw.body.startsWith("location: http") -> buildStaticLocationSandesh(raw, isSelf)

            isVideoFile(raw.body) -> buildVideoSandesh(raw, isSelf, extractedDir)

            isAudioFile(raw.body) -> buildAudioSandesh(raw, isSelf, extractedDir)

            isVcfFile(raw.body) -> buildContactSandesh(raw, isSelf, extractedDir)

            isDocumentFile(raw.body) -> buildDocumentSandesh(raw, isSelf, extractedDir)

            else -> buildTextSandesh(raw, isSelf)
        }
    }

    // ─────────────────────────────────────────────
    // TEXT MESSAGE  (type 0)
    // ─────────────────────────────────────────────
    private fun buildTextSandesh(raw: RawMessage, isSelf: Boolean): Sandesh? {
        if (raw.body.isBlank()) {
            return null
        }

        val urlMatcher = urlPattern.matcher(raw.body)
        val hasUrl = urlMatcher.find()
        val linkUri = if (hasUrl) urlMatcher.group() else null

        val phoneMatcher = phonePattern.matcher(raw.body)
        val hasPhone = phoneMatcher.find()
        val phoneNumber = if (hasPhone) phoneMatcher.group()?.trim() else null

        // is_link = true if message has a URL/domain OR a phone number
        val hasLink = hasUrl || hasPhone

        val json = JsonObject().apply {
            addProperty(DootawasMessage.PAATH, raw.body)
            addProperty(DootawasMessage.IS_LINK, hasLink)
            if (linkUri != null) {
                addProperty(DootawasMessage.LINK_URI, linkUri)
                addProperty(DootawasMessage.LINK_TITLE, raw.body)
            }
            if (phoneNumber != null) {
                addProperty(DootawasMessage.LINK_URI, phoneNumber)
                addProperty(DootawasMessage.LINK_TITLE, raw.body)
            }
            // original_message skipped intentionally
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.MESSAGE.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // IMAGE MESSAGE  (type 1)
    // Multiple images sent in the same second by same sender
    // are each stored as individual Sandesh rows (each with all_data array of 1 item)
    // ─────────────────────────────────────────────
    private fun buildGroupedImageSandesh(group: List<RawMessage>, dir: File): Sandesh? {
        val allData = JsonArray()
        group.forEach { raw ->
            val fileName = extractFileName(raw.body) ?: return@forEach
            val savedUri = saveMediaFile(dir, fileName) ?: return@forEach
            allData.add(JsonObject().apply {
                addProperty(DootawasMessage.ORIGINAL_URI, savedUri.localPath.toString())
                addProperty(DootawasMessage.MIME_TYPE, getMimeType(fileName) ?: "image/jpeg")
            })
        }

        if (allData.size() == 0) return null

        val first = group.first()
        val isSelf = first.sender.trim() != partnerName.trim()
        val caption = group.firstNotNullOfOrNull { extractCaption(it.body) }

        val json = JsonObject().apply {
            addProperty(DootawasMessage.DATA_COUNT, allData.size())
            add(DootawasMessage.ALL_DATA, allData)
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.IMAGE.value)
            addProperty(DootawasMessage.DOWNLOAD_PERCENT, 100)
            addProperty(DootawasMessage.DOWNLOAD_SIZE, "100kb")
            if (caption != null) {
                addProperty(DootawasMessage.PAATH, caption)
            }
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = first.uniqueTime,
            sandeshType = SandeshType.IMAGE.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    private fun extractCaption(body: String): String? {
        val captionLines = body.lines().drop(1).filter { it.isNotBlank() }
        return if (captionLines.isEmpty()) null else captionLines.joinToString("\n")
    }    // ─────────────────────────────────────────────
    // VIDEO MESSAGE  (type 2)
    // ─────────────────────────────────────────────
    private fun buildVideoSandesh(
        raw: RawMessage,
        isSelf: Boolean,
        dir: File
    ): Sandesh? {

        val fileName = extractFileName(raw.body) ?: return null

        val savedMedia =
            saveMediaFile(
                dir,
                fileName
            ) ?: return null

        var thumbnailUri: String? = null

        try {

            val file =
                File(savedMedia.localPath)

            if (file.exists() && file.length() > 0) {

                val thumbnail =
                    ThumbnailUtils.createVideoThumbnail(
                        file,
                        Size(320, 180),
                        null
                    )

                if (thumbnail != null) {

                    thumbnailUri =
                        saveThumbnail(
                            fileName,
                            thumbnail
                        )?.toString()
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }

        val allData = JsonArray().apply {

            add(
                JsonObject().apply {

                    addProperty(
                        DootawasMessage.ORIGINAL_URI,
                        savedMedia.localPath
                    )

                    addProperty(
                        DootawasMessage.MIME_TYPE,
                        savedMedia.mimeType
                    )
                }
            )
        }

        val previews = JsonArray()

        thumbnailUri?.let {
            previews.add(it)
        }

        val json = JsonObject().apply {

            addProperty(
                DootawasMessage.DATA_COUNT,
                1
            )

            add(
                DootawasMessage.ALL_DATA,
                allData
            )

            add(
                DootawasMessage.IMAGE_PREVIEW,
                previews
            )

            addProperty(
                DootawasMessage.DOWNLOAD_PERCENT,
                100
            )

            addProperty(
                DootawasMessage.DOWNLOAD_SIZE,
                savedMedia.fileSize
            )

            addProperty(
                DootawasMessage.SANDESH_TYPE,
                SandeshType.VIDEO.value
            )
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,
            sandeshType = SandeshType.VIDEO.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }
    private fun saveThumbnail(
        videoFileName: String,
        bitmap: Bitmap
    ): Uri? {

        return try {

            // ------------------------------------------------
            // Temp cache file
            // ------------------------------------------------
            val tempThumbFile = File.createTempFile(
                "thumb_temp",
                ".jpg",
                context.cacheDir
            )

            tempThumbFile.outputStream().use { out ->

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    80,
                    out
                )
            }

            // ------------------------------------------------
            // Save using unified storage system
            // ------------------------------------------------
            val saved =
                saveMediaToAppStorageNext(
                    context = context,
                    sourceUri = tempThumbFile.toUri(),
                    storageType = MediaCategory.PREVIEW,
                    visibility = MediaVisibility.PRIVATE,
                    overrideMimeType = "image/jpeg"
                )

            tempThumbFile.delete()

            saved?.localPath?.toUri()

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }    // ─────────────────────────────────────────────
    // AUDIO MESSAGE  (type 4)
    // ─────────────────────────────────────────────
    private fun buildAudioSandesh(raw: RawMessage, isSelf: Boolean, dir: File): Sandesh? {
        val fileName = extractFileName(raw.body) ?: return null
        val savedUri = saveMediaFile(dir, fileName) ?: return null

        val json = JsonObject().apply {
            addProperty(DootawasMessage.ORIGINAL_URI, savedUri.localPath.toString())
            addProperty(DootawasMessage.DUPLICATE_URI, "")
            addProperty(DootawasMessage.DURATION, 0L)
            addProperty(DootawasMessage.DOWNLOAD_PERCENT, 100)
            addProperty(DootawasMessage.DOWNLOAD_SIZE, "100kb")
            // original_message skipped intentionally
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.AUDIO.value) // add this
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.AUDIO.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // DOCUMENT MESSAGE  (type 5)
    // ─────────────────────────────────────────────
    private fun buildDocumentSandesh(raw: RawMessage, isSelf: Boolean, dir: File): Sandesh? {
        val fileName = extractFileName(raw.body) ?: return null
        val savedUri = saveMediaFile(dir, fileName) ?: return null
        val mimeType = getMimeType(fileName) ?: "application/octet-stream"

        val json = JsonObject().apply {
            addProperty(DootawasMessage.ORIGINAL_URI, savedUri.localPath.toString())
            addProperty(DootawasMessage.DUPLICATE_URI, "")
            addProperty(DootawasMessage.MIME_TYPE, mimeType)
            addProperty(DootawasMessage.DOWNLOAD_PERCENT, 100)
            addProperty(DootawasMessage.DOWNLOAD_SIZE, "100kb")
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.DOCUMENT.value) // add this
            // original_message skipped intentionally
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.DOCUMENT.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // CONTACT MESSAGE  (type 6)
    // TODO: Parse actual .vcf file content to extract
    //       contact_name and number when Knotx
    //       adds VCF export support. For now we store
    //       the filename as contact_name with empty number.
    // ─────────────────────────────────────────────
    private fun buildContactSandesh(raw: RawMessage, isSelf: Boolean, dir: File): Sandesh {
        val fileName = extractFileName(raw.body) ?: raw.body
        val contactName = fileName.removeSuffix(".vcf").trim()

        val contactJson = JsonObject().apply {
            addProperty(DootawasMessage.CONTACT_NAME, contactName)
            addProperty(DootawasMessage.NUMBER, "")
        }

        val contacts = JsonArray().apply { add(contactJson) }

        val json = JsonObject().apply {
            add(DootawasMessage.CONTACTS, contacts)
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.CONTACT.value) // add this
            // original_message skipped intentionally
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.CONTACT.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // STATIC LOCATION  (type 7)
    // ─────────────────────────────────────────────
    private fun buildStaticLocationSandesh(raw: RawMessage, isSelf: Boolean): Sandesh {
        val (lat, lng) = extractLatLng(raw.body)

        val json = JsonObject().apply {
            addProperty(DootawasMessage.LATITUDE, lat)
            addProperty(DootawasMessage.LONGITUDE, lng)
            addProperty(DootawasMessage.IS_LIVE, false)
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LOCATION.value) // add this
            // original_message skipped intentionally
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.LOCATION.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // LIVE LOCATION  (type 7 with is_live = true)
    // No coordinates available in export text
    // ─────────────────────────────────────────────
    private fun buildLiveLocationSandesh(raw: RawMessage, isSelf: Boolean): Sandesh {
        val json = JsonObject().apply {
            addProperty(DootawasMessage.LATITUDE, 0.0)
            addProperty(DootawasMessage.LONGITUDE, 0.0)
            addProperty(DootawasMessage.IS_LIVE, true)
            addProperty(DootawasMessage.LIVE_UNTIL, 0L)
            addProperty(DootawasMessage.IS_STOPPED, true)
            addProperty(DootawasMessage.SANDESH_TYPE, SandeshType.LOCATION.value) // add this
            // original_message skipped intentionally
        }

        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = json.toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.LOCATION.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // DELETE MESSAGE  (type 8)
    // samagri is empty json — nothing to show
    // ─────────────────────────────────────────────
    private fun buildDeleteSandesh(raw: RawMessage, isSelf: Boolean): Sandesh {
        return Sandesh(
            sandeshId = UUID.randomUUID().toString(),
            userNumber = userNumber,
            samagri = JsonObject().toString(),
            isSelf = isSelf,
            time = raw.uniqueTime,  // ← was raw.time,
            sandeshType = SandeshType.DELETE.value,
            sandeshStithi = SandeshStithi.DHRIST
        )
    }

    // ─────────────────────────────────────────────
    // MEDIA SAVING
    // Saves file from extracted zip dir to app media storage.
    // If showInGallery = true → uses MediaStore (visible in gallery)
    // If showInGallery = false → saves to app-private internal path
    // ─────────────────────────────────────────────



    private fun saveMediaFile(
        extractedDir: File,
        fileName: String
    ): SavedMedia? {

        val sourceFile = File(
            extractedDir,
            fileName
        )

        if (!sourceFile.exists()) {


            return null
        }

        val mimeType =
            getMimeType(fileName)
                ?: "application/octet-stream"

        val storageType = when {

            mimeType.startsWith("image/") ->
                MediaCategory.IMAGE

            mimeType.startsWith("video/") ->
                MediaCategory.VIDEO

            mimeType.startsWith("audio/") ->
                MediaCategory.AUDIO

            else ->
                MediaCategory.DOCUMENT
        }

        return try {

            val saved =
                saveMediaToAppStorageNext(
                    context = context,
                    sourceUri = sourceFile.toUri(),
                    storageType = storageType,
                    visibility = if (showInGallery) {
                        MediaVisibility.PUBLIC
                    } else {
                        MediaVisibility.PRIVATE
                    },
                    overrideMimeType = mimeType
                )

            if (saved == null) {


                null

            } else {

                SavedMedia(
                    localPath = saved.localPath,
                    mimeType = saved.mimeType,
                    publicUri = saved.publicUri,
                    fileName = saved.fileName,
                    fileSize = saved.fileSize
                )
            }

        } catch (e: Exception) {



            e.printStackTrace()

            null
        }
    }
    /**
     * Saves media to MediaStore — appears in gallery / Files app.
     * Used when showInGallery = true for images and videos.
     */

    /**
     * Saves media to app-private internal storage under:
     * /data/data/<package>/files/Knotx/Media/<Type>/
     * Not visible in gallery.
     */

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private fun isSystemMessage(raw: RawMessage): Boolean {
        // System messages have no real sender (e.g. encryption notice lines
        // are parsed by linePattern but sender is a long string with no colon before message)
        // We treat sender == "" or body starting with "Messages and calls" as system
        return raw.sender.isBlank() ||
                raw.body.startsWith("Messages and calls are end-to-end encrypted")
    }

    private fun extractFileName(body: String): String? {
        return body.lines().first().removeSuffix(" (file attached)").trim().takeIf { it.isNotBlank() }
    }

    private fun isImageFile(body: String): Boolean {
        val name = body.lines().first().removeSuffix(" (file attached)").lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")
    }
    private fun isVideoFile(body: String): Boolean {
        val name = body.lines().first().removeSuffix(" (file attached)").lowercase()
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")
    }

    private fun isAudioFile(body: String): Boolean {
        val name = body.lines().first().removeSuffix(" (file attached)").lowercase()
        return name.endsWith(".mp3") || name.endsWith(".opus") ||
                name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".aac")
    }

    private fun isVcfFile(body: String): Boolean {
        return body.lines().first().removeSuffix(" (file attached)").lowercase().endsWith(".vcf")
    }

    private fun isDocumentFile(body: String): Boolean {
        val name = body.lines().first().removeSuffix(" (file attached)").lowercase()
        return name.endsWith(".pdf") || name.endsWith(".doc") ||
                name.endsWith(".docx") || name.endsWith(".xls") ||
                name.endsWith(".xlsx") || name.endsWith(".txt")
    }

    private fun extractLatLng(body: String): Pair<Double, Double> {
        // e.g. "location: https://maps.google.com/?q=28.611937,77.354415"
        val pattern = Pattern.compile("""q=([-\d.]+),([-\d.]+)""")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            Pair(
                matcher.group(1)?.toDoubleOrNull() ?: 0.0,
                matcher.group(2)?.toDoubleOrNull() ?: 0.0
            )
        } else {
            Pair(0.0, 0.0)
        }
    }

    private fun getMimeType(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    // ─────────────────────────────────────────────
    // Internal data class for parsed lines
    // ─────────────────────────────────────────────
    private data class RawMessage(
        val sender: String,
        val body: String,
        val time: Long,
        val uniqueTime: Long = time  // ← add this
    )
}