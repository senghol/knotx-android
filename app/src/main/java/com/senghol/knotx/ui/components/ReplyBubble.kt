package com.senghol.knotx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType

@Composable
fun ReplyBubble(
    replyJson: JsonObject,
    isSelf: Boolean,
    onReplyClick: (sandeshID: String) -> Unit,
    replyBackground: Color,
    replyText: Color
) {
    // Extract primitive values once
    val originalMessageTypeInt = remember(replyJson) {
        replyJson.get(DootawasMessage.ORIGINAL_MESSAGE_TYPE)?.asInt ?: 0
    }

    val originalMessageId = remember(replyJson) {
        replyJson.get(DootawasMessage.ORIGINAL_MESSAGE_ID)?.asString
    }

    val originalSamagriJson = remember(replyJson) {
        try {
            val json = replyJson.get(DootawasMessage.ORIGINAL_SAMAGRI)?.asString
            if (json != null) Gson().fromJson(json, JsonObject::class.java) else JsonObject()
        } catch (_: Exception) {
            JsonObject()
        }
    }

    val originalMessageType = remember(originalMessageTypeInt) {
        SandeshType.fromInt(originalMessageTypeInt)
    }

// If your parent layout is a Column, set its horizontal alignment here
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 100.dp, max = 270.dp)
                .padding(
                    bottom = 2.dp
                )
                .shadow(1.dp, RoundedCornerShape(7.dp), clip = false)   // add this
                .background(replyBackground, RoundedCornerShape(7.dp))
                .clickable {
                    originalMessageId?.let(onReplyClick)
                }
                .padding(horizontal = 10.dp, vertical = 2.dp) // Added vertical padding for breathing room
    ) {
            // Single row to keep it strictly on one line
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine what content text to show based on type
                val previewText = when (originalMessageType) {
                    SandeshType.MESSAGE,
                    SandeshType.REPLY_MESSAGE -> {
                        val rawText = originalSamagriJson.get(DootawasMessage.PAATH)?.asString ?: ""
                        // Limit text to 30 characters max
                        if (rawText.length > 30) rawText.take(30) + "..." else rawText
                    }
                    SandeshType.VIDEO,
                    SandeshType.REPLY_VIDEO -> "VIDEO"

                    SandeshType.IMAGE,
                    SandeshType.REPLY_IMAGE -> "PHOTO"

                    else -> ""
                }

                Text(
                    text = "Replied to: $previewText",
                    fontSize = 12.sp,
                    color = replyText,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

}


@Composable
private fun ImagePreview(painter: AsyncImagePainter) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}