package com.senghol.knotx.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import coil.size.Size
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.ui.components.getRelativeTimeSpanMinuteLevel
import com.senghol.knotx.utils.data.ContactInfo
import com.senghol.knotx.utils.data.ParsedAudioSandesh
import com.senghol.knotx.utils.data.ParsedContactSandesh
import com.senghol.knotx.utils.data.ParsedDocumentSandesh
import com.senghol.knotx.utils.data.ParsedImageSandesh
import com.senghol.knotx.utils.data.ParsedLocationSandesh
import com.senghol.knotx.utils.data.ParsedSandeshItem
import com.senghol.knotx.utils.data.ParsedVideoSandesh
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MediaViewModel(
    private val repository: SandeshRepository,
    private val targetType: SandeshType,
    private val userNumber: String
) : ViewModel() {

    val imageLoader = repository.imageLoader

    private val _parsedSandeshItems = MutableStateFlow<List<ParsedSandeshItem>>(emptyList())
    val parsedSandeshItems: StateFlow<List<ParsedSandeshItem>> = _parsedSandeshItems.asStateFlow()

    init {
        fetchMediaItems()
    }
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMMM yyyy", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("h:mm a", Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    private fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Instant.ofEpochMilli(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(timestamp))
    }
    fun preloadImages(items: List<ParsedSandeshItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            items.filterIsInstance<ParsedImageSandesh>()
                .forEach { imageItem ->
                    imageItem.displayUri?.let { uri ->
                        try {
                            val request = ImageRequest.Builder(repository.applicationContext)
                                .data(uri)
                                .size(Size.ORIGINAL) // You can limit size if needed
                                .build()

                            repository.imageLoader.execute(request)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    private fun fetchMediaItems() {
        viewModelScope.launch {
            repository.getSandeshByUser(userNumber).collect { allMessages ->
                val filtered = allMessages.filter {
                    SandeshType.fromInt(it.sandeshType) == targetType
                }

                val parsed = filtered.mapNotNull { sandesh ->
                    when (targetType) {
                        SandeshType.IMAGE,
                        SandeshType.REPLY_IMAGE -> sandesh.toParsedImageSandesh()

                        SandeshType.VIDEO,
                        SandeshType.REPLY_VIDEO -> sandesh.toParsedVideoSandesh()

                        SandeshType.AUDIO,
                        SandeshType.REPLY_AUDIO -> sandesh.toParsedAudioSandesh()

                        SandeshType.DOCUMENT,
                        SandeshType.REPLY_DOCUMENT -> sandesh.toParsedDocumentSandesh()

                        SandeshType.CONTACT,
                        SandeshType.REPLY_CONTACT -> sandesh.toParsedContactSandesh()

                        SandeshType.LOCATION,
                        SandeshType.REPLY_LOCATION -> sandesh.toParsedLocationSandesh()

                        else -> null
                    }
                    // ✅ Call preloading after setting items

                }

                _parsedSandeshItems.value = parsed
                preloadImages(parsed)
            }
        }
    }



    fun Sandesh.toParsedImageSandesh(): ParsedImageSandesh {

        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val replyOriginalMessage =
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject

        // ✅ Extract ALL images
        val dataArray = jsonObject.getAsJsonArray(DootawasMessage.ALL_DATA)

        val imageUris = dataArray?.mapNotNull {
            it.asJsonObject
                .get(DootawasMessage.ORIGINAL_URI)
                ?.asString
        } ?: emptyList()

        // ✅ First image for preview
        val firstUri = imageUris.firstOrNull()

        return ParsedImageSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedTime = formatTime( time),
            formattedDate = formatDate(time),

            imageUris = imageUris,        // ✅ full list
            displayUri = firstUri,        // ✅ preview = first image
            blurUri = firstUri,           // ✅ same as you decided

            downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0,
            downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown",
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )
    }
    fun Sandesh.toParsedVideoSandesh(): ParsedVideoSandesh {

        val jsonObject = try {
            JsonParser.parseString(samagri).asJsonObject
        } catch (e: Exception) {
            JsonObject()
        }

        val videoUris = jsonObject
            .getAsJsonArray(DootawasMessage.ALL_DATA)
            ?.mapNotNull {
                it.asJsonObject
                    .get(DootawasMessage.ORIGINAL_URI)
                    ?.asString
            } ?: emptyList()

        val previewUris = jsonObject
            .getAsJsonArray(DootawasMessage.IMAGE_PREVIEW)
            ?.mapNotNull {
                it.asString
            } ?: emptyList()

        val blurUri =
            jsonObject.get(DootawasMessage.BLUR_URI)?.asString

        val downloadPercent =
            jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0

        val downloadSize =
            jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"

        val replyOriginalMessage =
            jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject

        return ParsedVideoSandesh(
            sandeshId = sandeshId,
            isSelf = isSelf,
            sandeshType = SandeshType.fromInt(sandeshType),
            time = time,
            formattedDate = formatDate(time),
            formattedTime = formatTime(time),
            videoUris = videoUris,
            previewUris = previewUris,
            blurUri = blurUri,
            downloadPercent = downloadPercent,
            downloadSize = downloadSize,
            sandeshStithi = sandeshStithi,
            replyOriginalMessage = replyOriginalMessage
        )
    }




    private fun Sandesh.toParsedAudioSandesh(): ParsedAudioSandesh? {
        return try {
            val jsonObject = JsonParser.parseString(samagri).asJsonObject

            val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
            val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
            val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
            val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"
            val duration = jsonObject.get(DootawasMessage.DURATION)?.asLong ?: 0L

            val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
            val replyOriginalMessage = if (isReply) {
                jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
            } else null

            ParsedAudioSandesh(
                sandeshId = sandeshId,
                isSelf = isSelf,
                sandeshType = SandeshType.fromInt(sandeshType),
                time = time,
                formattedDate = formatDate(time),
                formattedTime = formatTime(time),
                savedFileUri = savedFileUri,
                duplicateUri = duplicateUri,
                downloadPercent = downloadPercent,
                downloadSize = downloadSize,
                sandeshStithi = sandeshStithi,
                replyOriginalMessage = replyOriginalMessage,
                duration = duration
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun Sandesh.toParsedDocumentSandesh(): ParsedDocumentSandesh? {
        return try {
            val jsonObject = JsonParser.parseString(samagri).asJsonObject

            val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
            val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
            val mimeType = jsonObject.get(DootawasMessage.MIME_TYPE)?.asString ?: "application/octet-stream"
            val downloadPercent = jsonObject.get(DootawasMessage.DOWNLOAD_PERCENT)?.asInt ?: 0
            val downloadSize = jsonObject.get(DootawasMessage.DOWNLOAD_SIZE)?.asString ?: "Unknown"

            val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
            val replyOriginalMessage = if (isReply) {
                jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
            } else null

            ParsedDocumentSandesh(
                sandeshId = sandeshId,
                isSelf = isSelf,
                sandeshType = SandeshType.fromInt(sandeshType),
                time = time,
                formattedDate = formatDate(time),
                formattedTime = formatTime(time),
                savedFileUri = savedFileUri,
                duplicateUri = duplicateUri,
                mimeType = mimeType,
                downloadPercent = downloadPercent,
                downloadSize = downloadSize,
                sandeshStithi = sandeshStithi,
                replyOriginalMessage = replyOriginalMessage
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun Sandesh.toParsedContactSandesh(): ParsedContactSandesh? {
        return try {
            val jsonObject = JsonParser.parseString(samagri).asJsonObject

            val contactArray = jsonObject.getAsJsonArray(DootawasMessage.CONTACTS) ?: JsonArray()
            val contacts = contactArray.mapNotNull { element ->
                try {
                    val obj = element.asJsonObject
                    val name = obj.get(DootawasMessage.CONTACT_NAME)?.asString ?: "Unknown"
                    val phone = obj.get(DootawasMessage.NUMBER)?.asString ?: "N/A"
                    ContactInfo(name, phone)
                } catch (e: Exception) {
                    null  // Skip invalid entries
                }
            }

            val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
            val replyOriginalMessage = if (isReply) {
                jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
            } else null

            ParsedContactSandesh(
                sandeshId = sandeshId,
                isSelf = isSelf,
                sandeshType = SandeshType.fromInt(sandeshType),
                time = time,
                formattedDate = formatDate(time),
                formattedTime = formatTime(time),
                contacts = contacts,
                sandeshStithi = sandeshStithi,
                replyOriginalMessage = replyOriginalMessage
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun Sandesh.toParsedLocationSandesh(): ParsedLocationSandesh? {
        return try {
            val jsonObject = JsonParser.parseString(samagri).asJsonObject

            val savedFileUri = jsonObject.get(DootawasMessage.ORIGINAL_URI)?.asString
            val duplicateUri = jsonObject.get(DootawasMessage.DUPLICATE_URI)?.asString
            val latitude = jsonObject.get(DootawasMessage.LATITUDE)?.asDouble ?: return null
            val longitude = jsonObject.get(DootawasMessage.LONGITUDE)?.asDouble ?: return null

            val isReply = jsonObject.has(DootawasMessage.ORIGINAL_MESSAGE)
            val replyOriginalMessage = if (isReply) {
                jsonObject.get(DootawasMessage.ORIGINAL_MESSAGE)?.asJsonObject
            } else null
            val isLive =
                jsonObject.get(DootawasMessage.IS_LIVE)
                    ?.asBoolean ?: false
            ParsedLocationSandesh(
                sandeshId = sandeshId,
                isSelf = isSelf,
                sandeshType = SandeshType.fromInt(sandeshType),
                time = time,
                formattedTime = formatTime(time),
                formattedDate = formatDate(time),
                savedFileUri = savedFileUri,
                duplicateUri = duplicateUri,
                latitude = latitude,
                longitude = longitude,
                sandeshStithi = sandeshStithi,
                replyOriginalMessage = replyOriginalMessage,
                isLive = isLive,
                liveUntil = 0,
                isStopped = true,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


}