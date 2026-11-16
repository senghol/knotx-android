package com.senghol.knotx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senghol.knotx.data.database.Sandesh
import com.senghol.knotx.utils.enums.DootawasMessage
import com.senghol.knotx.utils.enums.SandeshType


@Composable
fun ReplyBanner(
    sandesh: Sandesh,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jsonObject = remember(sandesh.sandeshId) {
        // ✅ remember-keyed so it only re-parses when the selected message actually changes
        try { JsonParser.parseString(sandesh.samagri).asJsonObject }
        catch (e: Exception) { JsonObject() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Reply",
            tint = Color.Gray,
            modifier = Modifier.padding(end = 8.dp)
        )

        when (SandeshType.fromInt(sandesh.sandeshType)) {
            SandeshType.MESSAGE, SandeshType.REPLY_MESSAGE -> {
                val text = jsonObject.get(DootawasMessage.PAATH)?.asString.orEmpty()
                Text(
                    text = text.take(30) + if (text.length > 30) "..." else "",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
            // ... other types same as before
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
        }
    }
}